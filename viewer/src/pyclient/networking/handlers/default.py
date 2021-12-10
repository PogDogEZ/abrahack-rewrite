#!/usr/bin/env python3
import logging
import time

from . import Handler
from ..packets import Packet, Side, KeepAlivePacket, DisconnectPacket, KeepAliveResponsePacket, \
    ConnectionInfoResponsePacket, PrintPacket


class DefaultHandler(Handler):

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

        if isinstance(packet, KeepAlivePacket):
            # logging.debug("Latest keepalive ID: %i" % packet.keep_alive_id)
            keep_alive_response = KeepAliveResponsePacket()
            keep_alive_response.keep_alive_id = packet.keep_alive_id
            self.connection.send_packet(keep_alive_response)

        if isinstance(packet, DisconnectPacket):
            self.connection.exit(packet.message)

        elif isinstance(packet, ConnectionInfoResponsePacket):
            logging.info("Connection info response:")
            logging.info("Ping: %ims." % packet.ping)
            logging.info("User: %r." % packet.user)
            logging.info("IP address: %s:%i." % (packet.host, packet.port))

            self.connection.user = packet.user

        elif isinstance(packet, PrintPacket):
            logging.info(packet.message)  # print(packet.message, channel=packet.channel)
