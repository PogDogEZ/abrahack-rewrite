#!/usr/bin/env python3

"""
Dummy server and connection. Used because of my dumb code.
"""

import socket

from network.networking.connection import ServerConnection
from network.networking.handlers import Handler
from network.networking.packets import Packet
from network.networking.server import Server


class DummyServer(Server):

    def __init__(self, system) -> None:
        super().__init__("", 0, system)
        self.system.unregister_server(self)

    def __repr__(self) -> str:
        return "DummyServer()"

    def on_connect(self, func) -> None:
        ...

    def on_update(self) -> None:
        ...


class DummyConnection(ServerConnection):

    def __init__(self, system) -> None:
        super().__init__("", 0, socket.socket(socket.AF_INET, socket.SOCK_STREAM), DummyServer(system), system)
        self.system.unregister_connection(self)

    def __repr__(self) -> str:
        return "DummyConnection()"

    def register_handler(self, handler: Handler) -> None:
        ...

    def unregister_handler(self, handler: Handler) -> None:
        ...

    def _attempt_read_packet(self, read_timeout: float) -> Packet:
        ...

    def _read_packets(self) -> None:
        ...

    def _send_queued_packets(self) -> None:
        ...

    def _write_packet_instant(self, packet: Packet, no_compression: bool = False) -> None:
        ...

    def send_packet(self, packet: Packet, force: bool = False, no_compression: bool = False) -> None:
        ...

    def get_latest_packet(self, timeout: float = 30) -> Packet:
        ...

    def exit(self, reason: str = "Generic Disconnect") -> None:
        ...

    def on_update(self) -> None:
        ...
