#!/usr/bin/env python3

import time

from typing import Any

from network.networking.handlers import Handler
from network.networking.packets import KeepAlivePacket, DisconnectPacket, KeepAliveResponsePacket, InputResponsePacket, \
    ConnectionInfoRequestPacket, Packet, Side, ConnectionInfoResponsePacket, InputPacket, PrintPacket


class DefaultHandler(Handler):

    KEEP_ALIVE_RATE = 15
    KEEP_ALIVE_TIMEOUT = 5
    INPUT_TIMEOUT = 500

    def __init__(self, system, connection) -> None:
        super().__init__(system, connection)

        self._last_keep_alive = time.time()
        self._keep_alive_id = -1

        self.ping = 0

        self._current_input = None

    def __repr__(self) -> str:
        return "DefaultHandler(connection=%r)" % self.connection

    def print(self, *message: Any, channel: int = 5, sep: str = " ", end: str = "") -> None:
        print_packet = PrintPacket()

        print_packet.channel = channel
        print_packet.message = sep.join(message) + end

        self.connection.send_packet(print_packet)

    def input(self, prompt: str) -> str:
        if self._current_input is not None:
            raise Exception("Concurrent inputs are not allowed.")

        input_packet = InputPacket()
        input_packet.prompt = prompt
        self.connection.send_packet(input_packet)

        self._current_input = [prompt, None]

        while self._current_input[1] is None:
            time.sleep(0.1)  # Be nice to other threads

        response = self._current_input[1]
        self._current_input = None
        return response

    def handle_connection_info_request(self) -> None:
        connection_info_response = ConnectionInfoResponsePacket()

        connection_info_response.ping = int(self.ping * 1000)
        if self.connection.attached_user is None:
            connection_info_response.user = "none"
        else:
            user = self.connection.attached_user
            connection_info_response.user = str(user)

        connection_info_response.host = self.connection.host
        connection_info_response.port = self.connection.port

        self.connection.send_packet(connection_info_response)

    def on_packet(self, packet: Packet) -> None:
        if packet.SIDE != self.side and packet.SIDE != Side.BOTH:
            raise Exception("Invalid packet received: target sides do not match.")

        if isinstance(packet, ConnectionInfoRequestPacket):
            self.handle_connection_info_request()

        elif isinstance(packet, InputResponsePacket):
            if self._current_input is None:
                raise Exception("Invalid InputResponsePacket, no input requested.")

            self._current_input[1] = packet.response

        elif isinstance(packet, KeepAliveResponsePacket):
            if packet.keep_alive_id != self._keep_alive_id:
                raise Exception("Invalid KeepAliveResponsePacket.")

            self.ping = (time.time() - self._last_keep_alive) / 2
            self.system.logger.debug("%r ping: %.2fms" % (self.connection, self.ping * 1000))
            self._last_keep_alive = time.time()
            self._keep_alive_id = -1

        elif isinstance(packet, DisconnectPacket):
            self.connection.exit("Client disconnect, reason: '%s'" % packet.message)

            # self.system.logger.info("Client: %r disconnect, reason: %s" % (self.connection, packet.message))

    def on_update(self) -> None:
        if self._keep_alive_id != -1 and time.time() - self._last_keep_alive > self.KEEP_ALIVE_TIMEOUT:
            raise TimeoutError("Failed to respond to keep alive packet.")
        elif self._keep_alive_id == -1 and time.time() - self._last_keep_alive > self.KEEP_ALIVE_RATE:
            self._keep_alive_id = int(time.time() * 1000)
            self._last_keep_alive = time.time()

            keep_alive_packet = KeepAlivePacket()
            keep_alive_packet.keep_alive_id = self._keep_alive_id

            self.connection.send_packet(keep_alive_packet)
