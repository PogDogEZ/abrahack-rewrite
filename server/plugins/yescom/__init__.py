#!/usr/bin/env python3

import base64
import json
import os.path
from typing import List

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives._serialization import Encoding, PublicFormat
from cryptography.hazmat.primitives.serialization import load_pem_public_key, load_der_public_key

import plugins.yescom.network.packets
from network.impl.event.network import ClientCapabilitiesEvent, PostDisconnectEvent
from network.impl.logger import Logger, LogLevel
from network.impl.plugin import BasePlugin, subscribe_event
from plugins.yescom.network.handler import YCHandler, Reporter
from plugins.yescom.network.handler.archiving import Archiver
from plugins.yescom.network.handler.listening import Listener


class YesCom(BasePlugin):

    NAME = "YesCom"
    VERSION = "1.1"

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

        self._trusted_handlers = {"48Ocl1Ohx0YAzgUe82hOemj6Sp7c7Tt4h2Fq203qLj0=": None}

        self._current_id = 0

        self._listeners = []
        self._reporters = []
        self._archivers = []

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

        if not os.path.exists("trusted_handlers") or not os.path.isdir("trusted_handlers"):
            self.logger.warn("Trusted handlers directory does not exist, creating a new one.")

            os.mkdir("trusted_handlers")

            for trusted_handler in self._trusted_handlers:
                fileobj = open(os.path.join("trusted_handlers", "%s.pem" % trusted_handler), "wb")
                public_key = self._trusted_handlers[trusted_handler]
                if public_key is not None:
                    fileobj.write(public_key.public_bytes(Encoding.PEM, PublicFormat.SubjectPublicKeyInfo))
                fileobj.close()
        else:
            self.logger.debug("Reading trusted handlers...")

            self._trusted_handlers.clear()
            for file in os.listdir("trusted_handlers"):
                fileobj = open(os.path.join("trusted_handlers", file), "rb")
                data = fileobj.read()
                if not data:
                    self._trusted_handlers[os.path.splitext(file)[0]] = None
                else:
                    self._trusted_handlers[os.path.splitext(file)[0]] = load_pem_public_key(data, default_backend())
                fileobj.close()

        self.logger.debug("Done, %i trusted handlers." % len(self._trusted_handlers))

        # self.system.register_packets(plugins.yescom.network.packets.all_packets)
        self.logger.info("Loaded %r, server: %s:%i." % (self, self._host_name, self._host_port))

    def unload(self) -> None:
        super().unload()

        if not os.path.exists("trusted_handlers") or not os.path.isdir("trusted_handlers"):
            os.mkdir("trusted_handlers")

        for trusted_handler in self._trusted_handlers:
            fileobj = open(os.path.join("trusted_handlers", "%s.pem" % trusted_handler), "wb")
            public_key = self._trusted_handlers[trusted_handler]
            if public_key is not None:
                fileobj.write(public_key.public_bytes(Encoding.PEM, PublicFormat.SubjectPublicKeyInfo))
            fileobj.close()

    # -------------------- Events -------------------- #

    @subscribe_event(events=(ClientCapabilitiesEvent,))
    def on_client_capabilities(self, event: ClientCapabilitiesEvent) -> ClientCapabilitiesEvent:
        can_listen = True
        can_report = True

        for packet in plugins.yescom.network.packets.listening_packets:
            unmapped_packet = (packet.ID, packet.NAME, packet.SIDE)

            if not unmapped_packet in event.get_unmapped_packets():
                event.add_unmapped_packet(unmapped_packet)

            if not unmapped_packet in event.get_packets():
                can_listen = False

        for packet in plugins.yescom.network.packets.reporting_packets:
            unmapped_packet = (packet.ID, packet.NAME, packet.SIDE)

            if not unmapped_packet in event.get_unmapped_packets():
                event.add_unmapped_packet(unmapped_packet)

            if not unmapped_packet in event.get_packets():
                can_report = False

        if not can_listen and not can_report:
            event.set_rejected(True)
        else:
            self.logger.debug("Register YCHandler with connection %r..." % event.get_connection())
            event.get_connection().register_handler(YCHandler(self.system, event.get_connection(), self,
                                                              self._current_id))
            event.set_rejected(False)
            self._current_id += 1

            if can_listen:
                self.logger.debug("Pushing listening capabilities for %r." % event.get_connection())
                event.get_connection().push_capabilities(plugins.yescom.network.packets.listening_packets)
            elif can_report:
                self.logger.debug("Pushing reporting capabilities for %r." % event.get_connection())
                event.get_connection().push_capabilities(plugins.yescom.network.packets.reporting_packets)

        return event

    @subscribe_event(events=(PostDisconnectEvent,))
    def on_post_disconnect(self, event: PostDisconnectEvent) -> PostDisconnectEvent:
        for listener in self._listeners:
            if listener.connection == event.get_connection():
                self.remove_listener(listener)
                break
        else:
            for reporter in self._reporters:
                if reporter.connection == event.get_connection():
                    self.remove_reporter(reporter)
                    break
            else:
                for archiver in self._archivers:
                    if archiver.connection == event.get_connection():
                        self.remove_archiver(archiver)
                        break

        return event

    def is_trusted(self, handler_hash: bytes) -> bool:
        return base64.b64encode(handler_hash).decode() in self._trusted_handlers

    def is_public_key_valid(self, handler_hash: bytes, public_key: bytes) -> bool:
        handler_hash = base64.b64encode(handler_hash).decode()
        public_key = load_der_public_key(public_key, default_backend())

        return handler_hash in self._trusted_handlers and \
               (self._trusted_handlers[handler_hash] is None or
                self._trusted_handlers[handler_hash].public_numbers() == public_key.public_numbers())

    def update_public_key(self, handler_hash: bytes, public_key: bytes) -> None:
        handler_hash = base64.b64encode(handler_hash).decode()

        if handler_hash in self._trusted_handlers and self._trusted_handlers[handler_hash] is None:
            self._trusted_handlers[handler_hash] = load_der_public_key(public_key)

    # -------------------- Data -------------------- #

    # -------------------- Listeners -------------------- #

    def add_listener(self, listener: Listener) -> None:
        if not listener in self._listeners:
            self.logger.debug("Registered listener: %r." % listener)

            self._listeners.append(listener)

    def remove_listener(self, listener: Listener) -> None:
        if listener in self._listeners:
            self.logger.debug("Unregistered listener: %r." % listener)

            self._listeners.remove(listener)

            if listener.reporter is not None:
                listener.reporter.unregister_listener(listener)

            for archiver in listener.get_archivers():
                archiver.cancel_request(listener)

    def get_listener(self, handler_name: str = None, handler_id: int = None) -> Listener:
        for listener in self._listeners:
            if (handler_name is None or listener.handler_name == handler_name) and (handler_id is None or
                                                                                    listener.handler_id == handler_id):
                return listener

        raise LookupError("Couldn't find listener by %r, %r." % (handler_name, handler_id))

    # -------------------- Reporters -------------------- #

    def add_reporter(self, reporter: Reporter) -> None:
        if not reporter in self._reporters:
            self.logger.debug("Registered reporter: %r." % reporter)

            self._reporters.append(reporter)

            for listener in self._listeners:
                listener.sync_reporter(reporter)

    def remove_reporter(self, reporter: Reporter) -> None:
        if reporter in self._reporters:
            self.logger.debug("Unregistered reporter: %r." % reporter)

            self._reporters.remove(reporter)

            for listener in self._listeners:
                listener.sync_reporter(reporter)

            for archiver in reporter.get_archivers():
                archiver.cancel_request(reporter)

    def has_reporter(self, reporter: Reporter) -> bool:
        return reporter in self._reporters

    def get_reporter(self, handler_name: str = None, handler_id: int = None) -> Reporter:
        for reporter in self._reporters:
            if (handler_name is None or reporter.handler_name == handler_name) and (handler_id is None or
                                                                                    reporter.handler_id == handler_id):
                return reporter

        raise LookupError("Couldn't find reporter by %r, %r." % (handler_name, handler_id))

    def get_reporters(self) -> List[Reporter]:
        return self._reporters.copy()

    # -------------------- Archivers -------------------- #

    def add_archiver(self, archiver: Archiver) -> None:
        if not archiver in self._archivers:
            self.logger.debug("New archiver: %r." % archiver)

            self._archivers.append(archiver)

    def remove_archiver(self, archiver: Archiver) -> None:
        ...


plugin = {"class": YesCom}
