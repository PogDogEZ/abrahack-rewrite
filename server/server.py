#!/usr/bin/env python3

"""
Iska's Networking Program v5.
"""

import socket
import threading
import time

import colorama

from network.impl.logger import Logger, LogLevel
from network.impl.system import System
from network.impl.user.group import TestGroup
from network.networking.connection import ServerConnection
from network.networking.handlers.handshake import HandShakeHandler
from network.networking.server import Server
from network.networking.types.enum import EncryptionType

colorama.init()


PROTOCOL_VER = 4  # TODO: Move this somewhere else


def main() -> None:
    logger = Logger("system", log_level=LogLevel.DEBUG)
    syst = System(logger, login_message="Test",
                  encryption=True, encryption_type=EncryptionType.AES256,
                  compression=False, authentication=True)

    syst.default_group = TestGroup(syst.server_name, ["local"], 0, 1, syst)
    syst.add_group(syst.default_group)

    syst.load_plugins()

    host = "localhost"
    port = 5001
    max_conns = 100

    logger.info("Starting server on %r %s:%i (protocol %i), max connections: %i" % (syst.server_name, host, port,
                                                                                    PROTOCOL_VER, max_conns))

    sock_listener = Server(host, port, syst, max_conns)
    syst.async_updated(sock_listener)

    @sock_listener.on_connect
    def on_connection(host: str, port: int, conn: socket.socket) -> None:
        logger.info("New connection from %s:%i." % (host, port))
        connection = ServerConnection(host, port, conn, sock_listener, syst)
        syst.async_updated(connection)

        if isinstance(connection.handler, HandShakeHandler):
            connection.handler.start_handshake()

    while True:
        try:
            start = time.time()
            syst.update()
            time_taken = time.time() - start

            if time_taken < 0.005:
                time.sleep(0.005 - time_taken)
        except (Exception, KeyboardInterrupt) as error:
            logger.fatal("A fatal exception has occurred on system update: '%r'" % error)

            try:
                syst.shutdown()  # Yeah we don't know what caused the fatal exception and we don't want to crash
            except Exception as exc:
                logger.error("An error occurred while shutting down the server: '%r'" % exc)
                syst.shutdown(force=True)

            if len(threading.enumerate()) == 1:  # Yay! We exited correctly
                exit()

            start_timing = time.time()
            while len(threading.enumerate()) != 1:
                if time.time() - start_timing > 15:
                    logger.log("Not all threads shutdown, forcing system shutdown...")
                    time.sleep(2.5)
                    syst.shutdown(force=True)

            break


if __name__ == "__main__":
    main()
