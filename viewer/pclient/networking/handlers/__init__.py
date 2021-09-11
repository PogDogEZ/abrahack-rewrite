#!/usr/bin/env python3

"""
Handlers for network project. These handle incoming packets based on the current stage of operation.
"""

from pclient.networking.packets import Packet, Side


class Handler:

    side = Side.SERVER

    def __init__(self, connection) -> None:
        self.connection = connection

    def __repr__(self) -> str:
        return "Handler(connection=%r)" % self.connection

    def on_packet(self, packet: Packet) -> None:
        raise NotImplementedError("This function is not implemented in the base Handler class.")

    def on_update(self) -> None:  # TODO: Something with on_update?
        ...
