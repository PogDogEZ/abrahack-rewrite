#!/usr/bin/env python3

import socket

from network.impl.generic import GenericSystemObject


class Server(GenericSystemObject):

    @property
    def host(self):
        return self._host

    @property
    def port(self):
        return self._port

    @property
    def max_conns(self):
        return self._max_conns

    def __init__(self, host: str, port: int, system, max_conns: int = 5,
                 family: int = socket.AF_INET, sock_type: int = socket.SOCK_STREAM) -> None:
        super().__init__(system)

        self._host = host
        self._port = port
        self._max_conns = max_conns

        self._sock = socket.socket(family, sock_type)
        self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self._sock.bind((host, port))
        self._sock.listen(max_conns)

        self._on_connect_listeners = []

        self.system.register_new_server(self)

    def __repr__(self) -> str:
        return "Server(host='%s', port=%i, max_conns=%i)" % (self.host, self.port, self.max_conns)

    # -------------------- Decorators -------------------- #

    def on_connect(self, func) -> None:
        self._on_connect_listeners.append(func)

    # -------------------- System functions -------------------- #

    def on_update(self) -> None:
        self._sock.settimeout(1)  # We can accept if we time out, otherwise server shutdown will halt because of this
        try:
            sock, (host, port) = self._sock.accept()
        except socket.timeout:
            return

        for on_connect_listener in self._on_connect_listeners:
            on_connect_listener(host, port, sock)
