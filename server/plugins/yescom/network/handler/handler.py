#!/usr/bin/env python3

import threading
from typing import List

import plugins.yescom.network.packets.listening as listening
import plugins.yescom.network.packets.reporting as reporting

from.reporting import Reporter

from network.networking.handlers import Handler
from network.networking.packets import Packet
from plugins.yescom.network.packets.shared import YCInitRequestPacket, YCInitResponsePacket


class YCHandler(Handler):

    def __init__(self, system, connection, coord_exploit, handler_id: int) -> None:
        super().__init__(system, connection)

        self.coord_exploit = coord_exploit

        self._handler_id = handler_id
        self._name = "unknown"

        self._initialized = False

    def __repr__(self) -> str:
        return "YCHandler(name=%s, id=%i, connection=%s)" % (self._name, self._handler_id, self.connection)

    def on_packet(self, packet: Packet) -> None:
        if isinstance(packet, YCInitRequestPacket):
            if self._initialized:
                raise Exception("Didn't expect YCInitRequestPacket as already initialized.")
            self._initialized = True

            self.coord_exploit.logger.debug("Got yc_init_request from %r." % self.connection)

            self._name = packet.handler_name

            rejected = False
            message = ""

            new_handler = None

            if packet.listening:
                self.coord_exploit.logger.debug("%r is listening." % self)
                # new_handler = CEListener(self.system, self.connection, self.coord_exploit, self._id, self._name)
                # self.coord_exploit.add_listener(new_handler)
                # new_handler.do_full_sync()

            else:
                if packet.host_name != self.coord_exploit.host_name:
                    self.coord_exploit.logger.warn("%r attempted to connect with different host name (%s, %s)" %
                                                   (self.connection, packet.host_name, self.coord_exploit.host_name))
                    rejected = True
                    message = "Invalid host name."

                elif packet.host_port != self.coord_exploit.host_port:
                    self.coord_exploit.logger.warn("%r attempted to connect with different host port (%i, %i)" %
                                                   (self.connection, packet.host_port, self.coord_exploit.host_port))
                    rejected = True
                    message = "Invalid host port."

                else:
                    self.coord_exploit.logger.debug("%r is reporting." % self)
                    new_handler = Reporter(self.system, self.connection, self.coord_exploit, self._handler_id, self._name)
                    self.coord_exploit.add_reporter(new_handler)

            yc_init_response = YCInitResponsePacket()
            yc_init_response.rejected = rejected
            yc_init_response.message = message
            # Send these anyway as they will be dropped if rejected when encoding
            yc_init_response.host_name = self.coord_exploit.host_name
            yc_init_response.host_port = self.coord_exploit.host_port

            self.connection.send_packet(yc_init_response)

            if not rejected:
                self.connection.unregister_handler(self)
                self.connection.register_handler(new_handler)
            else:
                self.connection.exit("Rejected: " + message)
