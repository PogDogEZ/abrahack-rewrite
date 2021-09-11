#!/usr/bin/env python3

from viewer.network.packets import YCInitRequestPacket, YCInitResponsePacket
from pclient.networking.handlers import Handler
from pclient.networking.packets.packet import Packet


class YCHandler(Handler):

    @property
    def initialized(self) -> bool:
        return self._initialized

    @property
    def first_sync_done(self) -> bool:
        return self._first_sync_done

    @property
    def syncing(self) -> bool:
        return self._syncing

    def __init__(self, connection, mcviewer) -> None:
        super().__init__(connection)

        self.mcviewer = mcviewer

        self._initialized = False
        self._first_sync_done = False

        self._syncing = False

    def on_packet(self, packet: Packet) -> None:
        if isinstance(packet, YCInitResponsePacket):
            if self._initialized:
                self.connection.exit("Received ce_init_response when already initialized.")
                return

            self._initialized = True

            if packet.rejected:
                self.connection.exit("Rejected on CE init due to: %s." % packet.message)
                return
            else:
                self.mcviewer.logger.info("Successfully initialized CE connection.")

            self.mcviewer.logger.debug("Target: %s:%i" % (packet.host_name, packet.host_port))

            if packet.host_name != self.mcviewer.target_host or packet.host_port != self.mcviewer.target_port:
                self.connection.exit("Target mismatch, expected: %s:%i." % (packet.host_name, packet.host_port))

    def init(self) -> None:
        if self._initialized:
            raise Exception("Already initialized.")

        ce_init_request = YCInitRequestPacket()
        ce_init_request.handler_name = self.mcviewer.name
        ce_init_request.listening = True
        ce_init_request.reporting = False
        ce_init_request.host_name = self.mcviewer.target_host
        ce_init_request.host_port = self.mcviewer.target_port

        self.mcviewer.logger.debug("Sending ce_init_request for target: %s:%i." % (self.mcviewer.target_host,
                                                                                   self.mcviewer.target_port))

        self.connection.send_packet(ce_init_request)

        self.mcviewer.logger.debug("Waiting for server response...")
