#!/usr/bin/env python3
from typing import List
from uuid import UUID

from viewer.network.handler import YCHandler
from viewer.network.packets import SelectReporterPacket
from viewer.util import Reporter, ActiveTask, RegisteredTask
from pclient.impl.logger import Logger
from pclient.networking.connection import Connection


class Viewer:

    @property
    def name(self) -> str:  # TODO: Name
        return "test"

    @property
    def handler(self) -> YCHandler:
        return self._handler

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
    def current_reporter(self) -> int:
        return self._current_reporter

    @current_reporter.setter
    def current_reporter(self, handler_id: int) -> None:
        if self._current_reporter != -1:
            self.get_reporter(handler_id=self._current_reporter).reset()
        self._current_reporter = -1

        select_reporter = SelectReporterPacket()
        select_reporter.selected_reporter = handler_id

        self.connection.send_packet(select_reporter)

    def __init__(self, logger: Logger) -> None:
        self.logger = logger

        self.connection = None
        self._handler = None

        self._server_host = "localhost"
        self._server_port = 5001

        self._target_host = "localhost"
        self._target_port = 25565

        self._current_reporter = -1
        self._reporters = []

        self._uuid_to_username_cache = {}

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
        self._handler = YCHandler(self.connection, self)
        self.connection.register_handler(self._handler)

    def init(self) -> None:
        if self.connection is None:
            raise Exception("No connection.")

        if self._handler is None:  # I'm dumb enough to probably let this happen
            self._handler = YCHandler(self.connection, self)
            self.connection.register_handler(self._handler)

        if self._handler.initialized:
            raise Exception("Already initialized.")

        self._handler.init()

    def register_reporter(self, reporter: Reporter) -> None:
        if not reporter in self._reporters:
            self.logger.debug("Registered reporter: %r." % reporter)
            self._reporters.append(reporter)

            if self._current_reporter == -1:
                self.current_reporter = reporter.handler_id

    def unregister_reporter(self, reporter: Reporter) -> None:
        if reporter in self._reporters:
            self.logger.debug("Unregistered reporter: %r." % reporter)

            self._reporters.remove(reporter)

            if reporter.handler_id == self._current_reporter:
                self.logger.debug("Lost current reporter.")
                self._current_reporter = -1

    def get_reporter(self, handler_name: str = None, handler_id: int = None) -> Reporter:
        for reporter in self._reporters:
            if (handler_name is None or reporter.handler_name == handler_name) and (handler_id is None or
                                                                                    reporter.handler_id == handler_id):
                return reporter

        raise LookupError("Couldn't find reporter by %r, %r." % (handler_name, handler_id))

    def get_reporters(self) -> List[Reporter]:
        return self._reporters.copy()

    def start_task(self, registered_task: RegisteredTask, parameters: List[ActiveTask.Parameter]) -> None:
        if self._current_reporter != -1:
            self.logger.debug("Starting new task %r." % registered_task)
            self._handler.start_task(registered_task, parameters)

    def stop_task(self, task_id: int) -> None:
        if self._current_reporter != -1:
            self.logger.debug("Stopping task with id %i." % task_id)
            self._handler.stop_task(task_id)

    def add_account(self, username: str, password: str, callback) -> None:
        if self._current_reporter != -1:
            self._handler.add_account(username, password, callback)

    def remove_account(self, username: str) -> None:
        if self._current_reporter != -1:
            self._handler.remove_account(username)

    def set_name_for_uuid(self, uuid: UUID, name: str) -> None:
        self._uuid_to_username_cache[uuid] = name

    def get_name_for_uuid(self, uuid: UUID) -> str:
        return self._uuid_to_username_cache.get(uuid, "")

    def on_exit(self) -> None:
        if self.connection is not None:
            self.connection.exit("Exited.")

    def on_update(self) -> None:
        ...
