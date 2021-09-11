#!/usr/bin/env python3

import time

from pclient.networking.handlers import Handler
from pclient.networking.packets import KeepAlivePacket, DisconnectPacket, KeepAliveResponsePacket, \
    ConnectionInfoResponsePacket, PrintPacket
from pclient.networking.packets.packet import Packet, Side


class DefaultHandler(Handler):

    KEEP_ALIVE_RATE = 1
    KEEP_ALIVE_TIMEOUT = 5
    INPUT_TIMEOUT = 500

    def __init__(self, connection) -> None:
        super().__init__(connection)

        self._last_keep_alive = time.time()
        self._keep_alive_id = -1

        self.ping = 0

        self._current_input = None

    def __repr__(self) -> str:
        return "DefaultHandler(connection=%r)" % self.connection

    def on_packet(self, packet: Packet) -> None:
        if packet.SIDE != self.side and packet.SIDE != Side.BOTH:
            raise Exception("Invalid packet received: target sides do not match.")

        if isinstance(packet, DisconnectPacket):
            self.connection.exit(packet.message)

        elif isinstance(packet, ConnectionInfoResponsePacket):
            self.connection.logger.info("Connection info response:")
            self.connection.logger.info("Ping: %ims." % packet.ping)
            self.connection.logger.info("User: %r." % packet.user)
            self.connection.logger.info("IP address: %s:%i." % (packet.host, packet.port))

        elif isinstance(packet, PrintPacket):
            self.connection.logger.print(packet.message, channel=packet.channel)

        elif isinstance(packet, KeepAlivePacket):
            self.connection.logger.debug("Latest keepalive ID: %i" % packet.keep_alive_id)
            keep_alive_response = KeepAliveResponsePacket()
            keep_alive_response.keep_alive_id = packet.keep_alive_id
            self.connection.send_packet(keep_alive_response)

