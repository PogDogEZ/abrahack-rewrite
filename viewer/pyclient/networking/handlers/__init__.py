#!/usr/bin/env python3

from ..packets import Packet, Side


class Handler:  # TODO: Overhaul handlers

    side = Side.SERVER

    def __init__(self, connection) -> None:
        self.connection = connection

    def __repr__(self) -> str:
        return "Handler(connection=%r)" % self.connection

    def on_packet(self, packet: Packet) -> None:
        ...

    def on_update(self) -> None:
        ...
