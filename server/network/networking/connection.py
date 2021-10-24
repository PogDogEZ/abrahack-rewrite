#!/usr/bin/env python3

import socket
import time
import io
import zlib
import threading

from collections import deque
from typing import List, Type

import network.networking.packets

from network.impl.event.network import PrePacketInEvent, PostPacketInEvent, PrePacketOutEvent, \
    PostPacketOutEvent, PreDisconnectEvent, PostDisconnectEvent, PreConnectionUpdateEvent, PostConnectionUpdateEvent
from network.impl.generic import GenericSystemObject
from network.networking.handlers import Handler
from network.networking.packets import DisconnectPacket
from network.networking.packets import Packet
from network.networking.handlers.handshake import HandShakeHandler
from network.networking.server import Server
from network.networking.types.basic import Integer, UnsignedShort


class Connection(GenericSystemObject):

    @property
    def host(self):
        return self._host

    @property
    def port(self):
        return self._port

    def __init__(self, system, host: str, port: int, sock: socket.socket) -> None:
        super().__init__(system)

        self._host = host
        self._port = port
        self._sock = sock

        self._capabilities = network.networking.packets.packets.copy()

        self.should_exit = False
        self._is_exiting = False
        self.exit_reason = "Generic Disconnect"

        self._write_lock = threading.RLock()

        self._latest_packets = deque()
        self._packet_queue = deque()

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

            for packet in self._capabilities:
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
        if len(self._packet_queue):
            read_timeout = 0.001  # We have more packets to send so no time to wait for new receives

        for index in range(100):
            try:
                packet = self._attempt_read_packet(read_timeout)
            except Exception as error:
                self.system.logger.error("Exception in connection: '%r': %r" % (self, error))
                self.exit(repr(error))
                
                return

            if packet is None or self.system.event_bus.post(PrePacketInEvent(packet, self)):
                return

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
        for index in range(min(50, len(self._packet_queue))):
            packet = self._packet_queue.popleft()
            self._write_packet_instant(packet)
            self.system.event_bus.post(PostPacketOutEvent(packet, self))

    def _write_packet_instant(self, packet: Packet, no_compression: bool = False) -> None:
        with self._write_lock:
            packet_data = io.BytesIO()
            buffer = io.BytesIO()

            # print(packet)

            packet.write(packet_data)
            packet_data = packet_data.getvalue()

            flags = 0

            if self.system.get_compression() and len(packet_data) > self.system.get_compression_threshold() and \
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
                if not self.should_exit:
                    self.exit(repr(error))  # FIXME: fuck

    def flush_packets(self) -> None:
        self._send_queued_packets()

    def send_packet(self, packet: Packet, force: bool = False, no_compression: bool = False) -> None:
        # assert isinstance(packet, Packet), "Param 'packet' must be of type Packet."

        event = self.system.event_bus.post(PrePacketOutEvent(packet, force, no_compression, self))
        if event:
            return

        force = event.get_force()
        no_compression = event.get_no_compression()

        if not force:
            self._packet_queue.append(packet)
        else:
            self._write_packet_instant(packet, no_compression=no_compression)

    def push_capabilities(self, packets: List[Type[Packet]]) -> None:
        self._capabilities.extend(packets)


class ServerConnection(Connection):

    def __init__(self, host: str, port: int, sock: socket.socket, server: Server, system) -> None:
        super().__init__(system, host, port, sock)

        self.server = server
        self.attached_user = None

        self._sock.setblocking(False)

        self._sock_file = self._sock.makefile("rb")

        self.handler = HandShakeHandler(self.system, self)
        self._other_handlers = []

        self.system.register_new_connection(self)

    def __repr__(self) -> str:
        return "ServerConnection(host=%r, port=%i)" % (self.host, self.port)

    # -------------------- Handler stuff -------------------- #

    def register_handler(self, handler: Handler) -> None:
        # assert isinstance(handler, Handler), "Param 'handler' must be of type Handler."

        if not handler in self._other_handlers:
            self._other_handlers.append(handler)

    def unregister_handler(self, handler: Handler) -> None:
        # assert isinstance(handler, Handler), "Param 'handler' must be of type Handler."
        assert handler in self._other_handlers, "Handler is not registered with this connection."

        self._other_handlers.remove(handler)

    # -------------------- System functions -------------------- #

    def exit(self, reason: str = "Generic Disconnect") -> None:
        if self.should_exit:
            return

        self.should_exit = True
        self.exit_reason = reason

        disconnect_packet = DisconnectPacket()
        disconnect_packet.message = reason
        self.send_packet(disconnect_packet)

    def on_update(self) -> None:  # Called every server update
        if self._is_exiting:  # Should stop disconnects from being handled twice
            return

        self.system.event_bus.post(PreConnectionUpdateEvent(self))

        self._read_packets()
        self._send_queued_packets()

        if self.should_exit:
            self._is_exiting = True

            self.system.event_bus.post(PreDisconnectEvent(self))

            if len(self._packet_queue):
                self._send_queued_packets()  # Let's be nice and try to flush the packet queue
                time.sleep(0.1)  # We should be updated asynchronously so we can do this

            self.system.unregister_connection(self)
            self._sock.close()
            self._sock_file.close()
            self.system.logger.info("Client: '%r' has been disconnected, reason: '%s'." % (self, self.exit_reason))

            if self.attached_user is not None:
                self.system.log_out_user(self.attached_user, self)

            self.system.event_bus.post(PostDisconnectEvent(self))
            return

        while len(self._latest_packets):
            packet = self._latest_packets.popleft()

            try:
                self.handler.on_packet(packet)
                for handler in self._other_handlers.copy():
                    handler.on_packet(packet)
                self.system.event_bus.post(PostPacketInEvent(packet, self))
            except Exception as error:
                self.exit("Exception in connection: '%r' while handling packet: %r" % (self, error))

        if self.handler is not None:
            try:
                self.handler.on_update()
                for handler in self._other_handlers.copy():
                    handler.on_update()
            except Exception as error:
                self.exit("Exception in connection: '%r' on handler update: %r" % (self, error))

        self.system.event_bus.post(PostConnectionUpdateEvent(self))


class ClientConnection(Connection):

    def __init__(self, system, host: str, port: int, family: int = socket.AF_INET, sock_type: int = socket.SOCK_STREAM):
        super().__init__(system, host, port, socket.socket(family, sock_type))

        self._sock.connect((host, port))
        self._sock.setblocking(False)

        self._sock_file = self._sock.makefile("rb")
        self._write_lock = threading.RLock()

        self._latest_packets = deque()
        self._packet_queue = deque()

        self._compression = False
        self._compression_threshold = 256

        self.should_exit = False
        self.exit_reason = "Generic Disconnect"

    def __repr__(self) -> str:
        return "ClientConnection(host=%s, port=%i)" % (self.host, self.port)

    # -------------------- Setters and getters -------------------- #

    def compression_enabled(self) -> bool:
        return self._compression

    def set_compression(self, compression_enabled: bool, threshold: int) -> None:
        # assert isinstance(compression_enabled, bool), "Param 'compression_enabled' must be of type bool."
        # assert isinstance(threshold, int), "Param 'threshold' must be of type int."

        self._compression = compression_enabled
        self._compression_threshold = threshold

    def get_compression_threshold(self) -> int:
        return self._compression_threshold

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
        while True:
            self._read_packets()
            self._send_queued_packets()

            if self.should_exit:
                if len(self._packet_queue):
                    self._send_queued_packets()
                    time.sleep(0.1)

                self.system.logger.log("Client disconnect, reason: '%s'" % self.exit_reason)

                self._sock.close()
                self._sock_file.close()
                break

            time.sleep(0.01)



