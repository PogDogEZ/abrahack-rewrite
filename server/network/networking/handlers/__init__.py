#!/usr/bin/env python3

"""
Handlers for network project. These handle incoming packets based on the current stage of operation.
"""

from network.networking.packets import Packet, Side


class Handler:

    """
    This is the side that the packets it will be processing should originate from.
    """
    side = Side.CLIENT

    def __init__(self, system, connection) -> None:
        self.system = system
        self.connection = connection

    def __repr__(self) -> str:
        return "Handler(connection=%r)" % self.connection

    def on_packet(self, packet: Packet) -> None:
        raise NotImplementedError("This function is not implemented in the base Handler class.")

    def on_update(self) -> None:
        ...
