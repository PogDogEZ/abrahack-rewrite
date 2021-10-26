#!/usr/bin/env python3

import io
import socket
import threading
import time
import zlib

from collections import deque

import pclient.networking.packets

from pclient.impl.logger import Logger
from pclient.impl.user import User
from pclient.networking.handlers import Handler
from pclient.networking.handlers.handshake import HandShakeHandler
from pclient.networking.packets import DisconnectPacket
from pclient.networking.packets.packet import Packet
from pclient.networking.types.basic import Integer, UnsignedShort
from pclient.networking.types.enum import EncryptionType


class Connection(threading.Thread):

    @property
    def handler(self) -> Handler:
        return self._handler

    @property
    def host(self) -> str:
        return self._host

    @property
    def port(self) -> int:
        return self._port

    @property
    def protocol_version(self) -> int:
        return self._protocol_version

    @property
    def connected(self) -> bool:
        return self._connected

    @property
    def server_name(self) -> str:
        return self._server_name

    @server_name.setter
    def server_name(self, server_name: str) -> None:
        self._server_name = server_name

    def __init__(self, host: str, port: int, logger: Logger, protocol_version: int = 4,
                 family: int = socket.AF_INET, sock_type: int = socket.SOCK_STREAM):
        super().__init__()

        self.logger = logger

        self._host = host
        self._port = port

        self._protocol_version = protocol_version

        self._sock = socket.socket(family, sock_type)

        self._sock_file = None
        self._write_lock = threading.RLock()

        self._latest_packets = deque()
        self._packet_queue = deque()

        self._handler = None
        self._other_handlers = []

        self._user_supplier = None
        self._password_supplier = None

        self._connected = False
        self._server_name = ""

        self._encryption = False
        self._encryption_type = EncryptionType.NONE

        self._compression = False
        self._compression_threshold = 256

        self.should_exit = False
        self.exit_reason = "Generic Disconnect"

    def __repr__(self) -> str:
        return "Connection(host=%s, port=%i)" % (self._host, self._port)

    def set_auth_suppliers(self, user_supplier, password_supplier) -> None:
        self._user_supplier = user_supplier
        self._password_supplier = password_supplier

    def register_handler(self, handler: Handler) -> None:
        if not handler in self._other_handlers:
            self._other_handlers.append(handler)

    def unregister_handler(self, handler: Handler) -> None:
        self._other_handlers.remove(handler)

    def connect(self) -> None:
        self.logger.info("Connecting to %s:%i..." % (self._host, self._port))

        self._sock.connect((self._host, self._port))
        self._sock.setblocking(False)

        self._sock_file = self._sock.makefile("rb")

        self._connected = True

        self._handler = HandShakeHandler(self)

        self.logger.debug("Starting rw thread...")
        self.start()

    # -------------------- Setters and getters -------------------- #

    def get_user(self) -> User:
        return None if self._user_supplier is None else self._user_supplier()

    def get_password(self) -> str:
        return "" if self._password_supplier is None else self._password_supplier()

    def encryption_enabled(self) -> bool:
        return self._encryption

    def get_encryption_type(self) -> EncryptionType:
        return self._encryption_type

    def set_encryption(self, encryption_enabled: bool, encryption_type: EncryptionType) -> None:
        self._encryption = encryption_enabled
        self._encryption_type = encryption_type

    def compression_enabled(self) -> bool:
        return self._compression

    def get_compression_threshold(self) -> int:
        return self._compression_threshold

    def set_compression(self, compression_enabled: bool, threshold: int) -> None:
        # assert isinstance(compression_enabled, bool), "Param 'compression_enabled' must be of type bool."
        # assert isinstance(threshold, int), "Param 'threshold' must be of type int."

        self._compression = compression_enabled
        self._compression_threshold = threshold

    # -------------------- Packet read -------------------- #

    def _attempt_read_packet(self, read_timeout: float) -> Packet:
        with self._write_lock:
            try:
                self._sock.settimeout(read_timeout)
                header_bytes = self._sock.recv(7)
            except socket.timeout:
                return None

            if not len(header_bytes):
                time.sleep(0.1)  # Peer may have disconnected, let's wait to find out

                if not self.should_exit:
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

            for packet in pclient.networking.packets.packets:
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
        read_timeout = 0.001
        if len(self._packet_queue):
            read_timeout = 0.01  # We have more packets to send so no time to wait for new receives

        for index in range(300):
            try:
                packet = self._attempt_read_packet(read_timeout)
            except Exception as error:
                self.logger.fatal("An exception occurred while attempting to read a packet: '%r'" % error)
                self.exit("Exception while reading packet: %r" % error)
                return

            if packet is None:
                return

            try:
                self._handler.on_packet(packet)
                for handler in self._other_handlers:
                    handler.on_packet(packet)
            except Exception as error:
                self.exit("Exception in connection: '%r' while handling packet: %r" % (self, error))
                raise error

            self._latest_packets.append(packet)

    def get_latest_packet(self, timeout: float = 30) -> Packet:
        start = time.time()
        while time.time() - start < timeout and not len(self._latest_packets):
            time.sleep(0.001)  # Doesn't slow down other threads as much

        if not len(self._latest_packets):
            raise TimeoutError("Packet read timeout after %i millis." % round(timeout * 1000))

        return self._latest_packets.popleft()

    # -------------------- Packet write -------------------- #

    def _send_queued_packets(self) -> None:
        for index in range(min(15, len(self._packet_queue))):
            self._write_packet_instant(self._packet_queue.popleft())

    def _write_packet_instant(self, packet: Packet, no_compression: bool = False) -> None:
        with self._write_lock:
            packet_data = io.BytesIO()
            buffer = io.BytesIO()

            # print(packet)

            packet.write(packet_data)
            packet_data = packet_data.getvalue()

            flags = 0

            if self._compression and len(packet_data) > self._compression_threshold and not no_compression:
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
                if not self.should_exit:
                    self.exit(repr(error))

    def send_packet(self, packet: Packet, force: bool = False, no_compression: bool = False) -> None:
        # assert isinstance(packet, Packet), "Param 'packet' must be of type Packet."

        if not force:
            self._packet_queue.append(packet)
        else:
            self._write_packet_instant(packet, no_compression=no_compression)

    # -------------------- Thread -------------------- #

    def exit(self, exit_reason: str = "Generic Disconnect") -> None:
        if self.should_exit:
            return

        self.should_exit = True
        self.exit_reason = exit_reason

        disconnect_packet = DisconnectPacket()
        disconnect_packet.message = exit_reason
        self.send_packet(disconnect_packet)

    def run(self) -> None:
        while self._connected:
            self._read_packets()
            self._send_queued_packets()

            if self.should_exit:
                if len(self._packet_queue):
                    self._send_queued_packets()
                    time.sleep(0.1)

                self.logger.log("Client disconnect, reason: '%s'" % self.exit_reason)

                self._sock.close()
                self._sock_file.close()
                self._connected = False
            else:
                time.sleep(0.01)
