#!/usr/bin/env python3

import io
import logging
import socket
import threading
import time
import zlib

from collections import deque
from typing import Tuple

from . import packets
from .handlers import Handler
from .handlers.handshake import HandShakeHandler
from .packets import Packet, DisconnectPacket
from .types.basic import Integer, UnsignedShort
from .types.enum import EncryptionType
from ..impl.user import User


class ServerInfo:
    """
    Stores information about the server.
    """

    @property
    def server_name(self) -> str:
        """
        :return: The reported name of the server.
        """

        return self._server_name

    @server_name.setter
    def server_name(self, server_name: str) -> None:
        self._server_name = server_name

    @property
    def encryption(self) -> bool:
        """
        :return: Whether or not encryption is enabled.
        """

        return self._encryption_enabled

    @encryption.setter
    def encryption(self, encryption_enabled: bool) -> None:
        self._encryption_enabled = encryption_enabled

    @property
    def encryption_type(self) -> EncryptionType:
        """
        :return: The type of encryption the server uses.
        """

        return self._encryption_type

    @encryption_type.setter
    def encryption_type(self, encryption_type: EncryptionType) -> None:
        self._encryption_type = encryption_type

    @property
    def compression(self) -> bool:
        """
        :return: Whether or not compression is enabled.
        """

        return self._compression_enabled

    @compression.setter
    def compression(self, compression_enabled: bool) -> None:
        self._encryption_enabled = compression_enabled

    @property
    def compression_threshold(self) -> int:
        """
        :return: The compression threshold.
        """

        return self._compression_threshold

    @compression_threshold.setter
    def compression_threshold(self, compression_threshold: int) -> None:
        self._compression_threshold = compression_threshold

    @property
    def authentication(self) -> bool:
        """
        :return: Whether or not authentication is enabled.
        """

        return self._authentication_enabled

    @authentication.setter
    def authentication(self, authentication_enabled: bool) -> None:
        self._authentication_enabled = authentication_enabled

    def __init__(self, server_name: str, encryption_enabled: bool, encryption_type: EncryptionType,
                 compression_enabled: bool, compression_threshold: int, authentication_enabled: bool) -> None:
        self._server_name = server_name

        self._encryption_enabled = encryption_enabled
        self._encryption_type = encryption_type

        self._compression_enabled = compression_enabled
        self._compression_threshold = compression_threshold

        self._authentication_enabled = authentication_enabled

    def __repr__(self) -> str:
        return "ServerInfo(name=%r)" % self._server_name


class Connection(threading.Thread):
    """
    A client connection.
    """

    @property
    def host(self) -> str:
        """
        :return: The host address of the server.
        """

        return self._host

    @property
    def port(self) -> int:
        """
        :return: The port of the server.
        """

        return self._port

    @property
    def expecting_timeout(self) -> bool:
        return self._expecting_timeout

    @expecting_timeout.setter
    def expecting_timeout(self, expecting_timeout: bool) -> None:
        if expecting_timeout and not self._expecting_timeout:
            self._last_packet = time.time()

        self._expecting_timeout = expecting_timeout

    @property
    def protocol_version(self) -> int:
        """
        :return: The client's current protocol version.
        """

        return self._protocol_version

    @property
    def connected(self) -> bool:
        """
        :return: Whether or not the client is connected.
        """

        return self._connected

    @property
    def account(self) -> Tuple[str, str, str]:
        """
        :return: The account the client will login with.
        """

        return ("", "", "") if self._account_supplier is None else self._account_supplier()

    @property
    def user(self) -> User:
        """
        :return: The server user associated with the client.
        """

        return self._server_user

    @user.setter
    def user(self, user: User) -> None:
        self._server_user = user

    @property
    def server_info(self) -> ServerInfo:
        """
        :return: The server information.
        """

        return self._server_info

    def __init__(self, host: str, port: int, protocol_version: int = 4,
                 family: int = socket.AF_INET, sock_type: int = socket.SOCK_STREAM):
        super().__init__()

        self._host = host
        self._port = port
        self._protocol_version = protocol_version

        self._expecting_timeout = True

        self._connected = False
        self._exit = False
        self._is_exiting = False
        self.exit_reason = "Generic disconnect."

        self._last_packet = time.time()

        self._sock = socket.socket(family, sock_type)
        self._net_lock = threading.RLock()
        self._read_lock = threading.RLock()

        self._received_packets = deque()
        self._send_queue_packets = deque()

        self.handler = None
        self._other_handlers = []

        self._account_supplier = None

        self._server_info = ServerInfo("", False, EncryptionType.NONE, False, 256, False)
        self._server_user = None

    def __repr__(self) -> str:
        return "Connection(host=%s, port=%i)" % (self._host, self._port)

    def run(self) -> None:
        while not self._exit and not self._is_exiting:
            with self._read_lock:
                if self.handler is not None:
                    try:
                        self.handler.on_update()
                        for handler in self._other_handlers:
                            handler.on_update()
                    except Exception as error:
                        logging.warning("An error occurred during a handler update:")
                        logging.error(error, exc_info=True)
                        self.exit("Exception during handler update.")

            with self._net_lock:
                self._read_packets()
                self._send_packets()

            if self._expecting_timeout and time.time() - self._last_packet > 30:
                self.exit("Timed out.")

            if self._exit:
                self._is_exiting = True

                if self.handler is not None:
                    self.handler.on_exit(self.exit_reason)
                    for handler in self._other_handlers:
                        handler.on_exit(self.exit_reason)

                if len(self._send_queue_packets):
                    self._send_packets()
                    time.sleep(0.1)

                logging.info("Client disconnect, reason: %r." % self.exit_reason)

                self._connected = False
                self._sock.close()

    # -------------------- Misc connection stuff -------------------- #

    def set_account_supplier(self, account_supplier) -> None:
        """
        Set the user and password suppliers for when the server requests auth details.

        :param account_supplier: The supplier for the account details.
        """

        self._account_supplier = account_supplier

    def register_handler(self, handler: Handler) -> None:
        """
        Registers a handler to listen for packets.

        :param handler: The handler to register.
        """

        if not handler in self._other_handlers:
            self._other_handlers.append(handler)

    def unregister_handler(self, handler: Handler) -> None:
        """
        Unregisters a handler from this connection.

        :param handler: The handler to unregister.
        """

        if handler in self._other_handlers:
            self._other_handlers.remove(handler)

    def connect(self) -> None:
        """
        Connects to the server.
        """

        if self._exit:  # Don't connect if we've already exited
            return

        logging.info("Connecting to %s:%i..." % (self._host, self._port))

        self._sock.connect((self._host, self._port))
        self._sock.setblocking(False)

        self._connected = True

        self.handler = HandShakeHandler(self)

        logging.debug("Starting rw thread...")
        self.start()

    # -------------------- Packet read -------------------- #

    def _attempt_read_packet(self, read_timeout: float) -> Packet:
        try:
            self._sock.settimeout(read_timeout)
            header_bytes = self._sock.recv(7)
        except socket.timeout:
            return None

        if not len(header_bytes):
            time.sleep(0.1)  # Peer may have disconnected, let's wait to find out

            if not self._is_exiting:
                raise ConnectionAbortedError("Connection to peer has been aborted (no information).")
            else:
                return None

        self._sock.settimeout(30)

        while len(header_bytes) < 7:
            header_bytes += self._sock.recv(7 - len(header_bytes))

        header_bytes = io.BytesIO(header_bytes)

        packet_length = Integer.read(header_bytes)
        flags = header_bytes.read(1)[0]
        packet_id = UnsignedShort.read(header_bytes)

        packet_buffer = io.BytesIO()
        packet_buffer.write(self._sock.recv(packet_length))

        while packet_buffer.tell() < packet_length:  # Read excess packet data if we weren't able to read it all
            packet_buffer.write(self._sock.recv(packet_length - packet_buffer.tell()))

        for packet in packets.packets:
            if packet.ID == packet_id:

                if flags & 1:  # Packet is compressed
                    new_packet_buffer = io.BytesIO()

                    packet_buffer = zlib.decompress(packet_buffer.getvalue())
                    new_packet_buffer.write(packet_buffer)
                    packet_buffer = new_packet_buffer

                packet_buffer.seek(0)

                read_packet = packet()
                read_packet.read(packet_buffer)

                return read_packet

        return None

    def _read_packets(self) -> None:
        read_timeout = 0.01
        if len(self._send_queue_packets):
            read_timeout = 0.001  # We have more packets to send so no time to wait for new receives

        for index in range(10):
            try:
                packet = self._attempt_read_packet(read_timeout)
            except Exception as error:
                logging.fatal("An exception occurred while attempting to read a packet:")
                logging.error(error, exc_info=True)
                self.exit("Exception while reading packet: %r." % error)
                return

            if packet is None:
                return

            if self.handler is not None:
                try:
                    self.handler.on_packet(packet)
                    for handler in self._other_handlers:
                        handler.on_packet(packet)
                except Exception as error:
                    logging.warning("An error occurred while handling a packet:")
                    logging.error(error, exc_info=True)
                    self.exit("Exception while handling packet.")

            self._last_packet = time.time()
            with self._read_lock:
                self._received_packets.append(packet)

    # -------------------- Packet write -------------------- #

    def _send_packets(self) -> None:
        for index in range(min(15, len(self._send_queue_packets))):
            self._write_packet_instant(self._send_queue_packets.popleft())

    def _write_packet_instant(self, packet: Packet, no_compression: bool = False) -> None:
        packet_data = io.BytesIO()
        buffer = io.BytesIO()

        # print(packet)

        packet.write(packet_data)
        packet_data = packet_data.getvalue()

        flags = 0

        if self._server_info.compression and len(packet_data) > self._server_info.compression_threshold and \
                not no_compression:
            flags |= 1
            packet_data = zlib.compress(packet_data)

        Integer.write(len(packet_data), buffer)
        buffer.write(bytes([flags]))
        UnsignedShort.write(packet.ID, buffer)
        buffer.write(packet_data)

        try:
            self._sock.settimeout(30)
            self._sock.sendall(buffer.getvalue())
        except Exception as error:
            self.exit(repr(error))

    def send_packet(self, packet: Packet, force: bool = True, no_compression: bool = False) -> None:
        with self._net_lock:
            if not force:
                self._send_queue_packets.append(packet)
            else:
                self._write_packet_instant(packet, no_compression=no_compression)

    def flush_packets(self) -> None:
        """
        Flushes all the packets in the send queue.
        """

        # self._read_packets()
        with self._net_lock:
            self._send_packets()

    def exit(self, reason: str = "Generic disconnect.") -> None:
        """
        Disconnects from the peer, given a reason (or not).

        :param reason: The reason for exiting.
        """

        if self._exit:
            return

        self._exit = True
        self.exit_reason = reason

        self.send_packet(DisconnectPacket(reason))
