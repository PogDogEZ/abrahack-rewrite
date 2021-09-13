#!/usr/bin/env python3

import json
from typing import List

import plugins.yescom.network.packets

# import network.networking.packets

from network.impl.event.network import ClientCapabilitiesEvent
from network.impl.logger import Logger, LogLevel
from network.impl.plugin import BasePlugin, subscribe_event
from plugins.yescom.network.handler import YCHandler, Reporter


class YesCom(BasePlugin):

    NAME = "YesCom"
    VERSION = "1.0"

    @property
    def host_name(self) -> str:
        return self._host_name

    @property
    def host_port(self) -> int:
        return self._host_port

    @property
    def listeners(self):  # -> List[Listener]:
        return self._listeners.copy()

    @property
    def reporters(self) -> List[Reporter]:
        return self._reporters.copy()

    def __init__(self, system) -> None:
        super().__init__(system, None)

        self.logger = Logger("yescom", log_level=LogLevel.DEBUG)

        self._host_name = "localhost"
        self._host_port = 25565

        self._current_id = 0

        self._listeners = []
        self._reporters = []

    def load(self) -> None:
        super().load()

        try:
            config = json.load(open("config.json", "r"))  # FIXME: Move to actual config directory provided by server

            self._host_name = config["host_name"]
            self._host_port = config["host_port"]

        except Exception as read_error:
            self.logger.warn("Couldn't load config file:")
            self.logger.error(repr(read_error))

            self.logger.info("Creating a new one...")
            config = {
                "host_name": self._host_name,
                "host_port": self._host_port,
            }
            try:
                json.dump(config, open("config.json", "w"))
            except Exception as write_error:
                self.logger.warn("Couldn't save config file:")
                self.logger.error(repr(write_error))
                self.logger.info("Continuing anyway.")

        self.system.register_packets(plugins.yescom.network.packets.all_packets)
        self.logger.info("Loaded %r, server: %s:%i." % (self, self._host_name, self._host_port))

    @subscribe_event(events=(ClientCapabilitiesEvent,))
    def on_client_capabilities(self, event: ClientCapabilitiesEvent) -> ClientCapabilitiesEvent:
        can_listen = True
        can_report = True

        for packet in plugins.yescom.network.packets.listening_packets:
            if not packet in event.get_accepted_packets():
                can_listen = False

        for packet in plugins.yescom.network.packets.reporting_packets:
            if not packet in event.get_accepted_packets():
                can_report = False

        if not can_listen and not can_report:
            event.set_rejected(True)
        else:
            self.logger.debug("Register YCHandler with connection %r..." % event.get_connection())
            event.get_connection().register_handler(YCHandler(self.system, event.get_connection(), self,
                                                              self._current_id))
            event.set_rejected(False)
            self._current_id += 1

        return event

    """
    def add_listener(self, client: CEListener) -> None:
        if not client in self._listeners:
            self._listeners.append(client)

    def get_listener(self, name: str = "", assigned_id: int = 0) -> CEListener:
        for listener in self._reporters:
            if (name is None or listener.name == name) and (assigned_id is None or listener.assigned_id == assigned_id):
                return listener

        raise LookupError("Couldn't find listener by %r, %r." % (name, assigned_id))
    """

    def add_reporter(self, client: Reporter) -> None:
        if not client in self._reporters:
            self._reporters.append(client)

    def get_reporter(self, handler_name: str = None, handler_id: int = None) -> Reporter:
        for reporter in self._reporters:
            if (handler_name is None or reporter.name == handler_name) and (handler_id is None or
                                                                            reporter.assigned_id == handler_id):
                return reporter

        raise LookupError("Couldn't find reporter by %r, %r." % (handler_name, handler_id))


plugin = {"class": YesCom}
