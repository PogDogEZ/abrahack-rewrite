#!/usr/bin/env python3
from viewer.network.handler import YCHandler
from viewer.util import Reporter
from pclient.impl.logger import Logger
from pclient.networking.connection import Connection


class Viewer:

    @property
    def name(self) -> str:  # TODO: Name
        return "test"

    @property
    def ce_handler(self) -> YCHandler:
        return self._ce_handler

    @property
    def server_host(self) -> str:
        return self._server_host

    @server_host.setter
    def server_host(self, server_host: str) -> None:
        self._server_host = server_host

    @property
    def server_port(self) -> int:
        return self._server_port

    @server_port.setter
    def server_port(self, server_port: int) -> None:
        self._server_port = server_port

    @property
    def target_host(self) -> str:
        return self._target_host

    @target_host.setter
    def target_host(self, target_host: str) -> None:
        self._target_host = target_host

    @property
    def target_port(self) -> int:
        return self._target_port

    @target_port.setter
    def target_port(self, target_port: int) -> None:
        self._target_port = target_port

    @property
    def current_reporter(self) -> Reporter:
        return self._current_reporter

    @property
    def account_action_callback(self):
        callback = self._account_action_callback
        self._account_action_callback = None
        return callback

    @property
    def performing_account_action(self) -> bool:
        return self._account_action_callback is not None

    def __init__(self, logger: Logger) -> None:
        self.logger = logger

        self.connection = None
        self._ce_handler = None

        self._server_host = "localhost"
        self._server_port = 5001

        self._target_host = "localhost"
        self._target_port = 25565

        self._account_action_callback = None

        self._current_reporter = None
        self._reporters = []

    def __repr__(self) -> str:
        return "Viewer()"

    def make_connection(self) -> None:
        self.connection = Connection(self._server_host, self._server_port, self.logger)

    def connect(self) -> None:
        if self.connection is None:
            raise Exception("No connection.")
        if self.connection.connected:
            raise Exception("Already connected.")

        self.connection.connect()
        self._ce_handler = YCHandler(self.connection, self)
        self.connection.register_handler(self._ce_handler)

    def init(self) -> None:
        if self.connection is None:
            raise Exception("No connection.")

        if self._ce_handler is None:  # I'm dumb enough to probably let this happen
            self._ce_handler = YCHandler(self.connection, self)
            self.connection.register_handler(self._ce_handler)

        if self._ce_handler.initialized:
            raise Exception("Already initialized.")

        self._ce_handler.init()

    def register_reporter(self, reporter: Reporter) -> None:
        if not reporter in self._reporters:
            self.logger.debug("Registering reporter: %r." % reporter)
            self._reporters.append(reporter)

            if self._current_reporter is None:
                self._current_reporter = reporter

    def unregister_reporter(self, reporter: Reporter) -> None:
        if reporter in self._reporters:
            self._reporters.remove(reporter)

    def get_reporter(self, name: str = None, assigned_id: int = None) -> Reporter:
        for reporter in self._reporters:
            if (name is None or reporter.name == name) and (assigned_id is None or reporter.assigned_id == assigned_id):
                return reporter

        raise LookupError("Couldn't find reporter by %r, %r." % (name, assigned_id))

    def on_exit(self) -> None:
        if self.connection is not None:
            self.connection.exit("Exited.")

    def on_update(self) -> None:
        ...
