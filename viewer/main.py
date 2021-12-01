#!/usr/bin/env python3

import logging
import time

import pyclient.networking.packets
import viewer.network.packets
from pyclient.networking.connection import Connection
from pyclient.networking.handlers.default import DefaultHandler
from viewer import Viewer

if __name__ == "__main__" or True:
    logging.basicConfig(format="[%(asctime)s] [%(levelname)s] %(message)s", datefmt="%H:%M:%S", level=logging.DEBUG)

    logging.debug("Registering packets...")
    pyclient.networking.packets.packets.extend(viewer.network.packets.packets)
    logging.debug("Done.")

    logging.debug("Starting viewer...")
    connection = Connection("localhost", 5001, 6)
    connection.connect()

    viewer = Viewer(connection)

    while viewer.connection.connected and not isinstance(viewer.connection.handler, DefaultHandler):
        time.sleep(0.1)

    if viewer.connection.connected:
        viewer.init()
