#!/usr/bin/env python3

import logging
import sys

from PyQt5.QtGui import QPalette, QColor
from PyQt5.QtWidgets import QStyleFactory, QApplication

from .pyclient.networking import packets as pyclient_packets
from .viewer.config import Config
from .viewer.gui import MainWindow
from .viewer.network import packets as viewer_packets


def main() -> None:
    logging.basicConfig(format="[%(asctime)s] [%(levelname)s] %(message)s", datefmt="%H:%M:%S", level=logging.DEBUG)

    logging.debug("Registering packets...")
    pyclient_packets.packets.extend(viewer_packets.packets)
    # pyclient.networking.packets.packets.extend(viewer.network.packets.packets)
    logging.debug("Done.")

    preferred_style = "Fusion"
    logging.debug("Current styles: %r." % QStyleFactory.keys())
    logging.debug("Starting viewer...")

    if not preferred_style in QStyleFactory.keys():
        preferred_style = QStyleFactory.keys()[0]

    app = QApplication(sys.argv)
    app.setStyle(preferred_style)
    app.setFont(Config.FONT)

    palette = app.palette()

    palette.setColor(QPalette.WindowText, QColor(*Config.TEXT_COLOUR))
    palette.setColor(QPalette.Button, QColor(*Config.BUTTON_COLOUR))
    palette.setColor(QPalette.Light, QColor(*Config.LIGHT_COLOUR))
    palette.setColor(QPalette.Dark, QColor(*Config.DARK_COLOUR))
    palette.setColor(QPalette.Mid, QColor(*Config.MID_COLOUR))
    palette.setColor(QPalette.Text, QColor(*Config.TEXT_COLOUR))
    palette.setColor(QPalette.BrightText, QColor(*Config.BRIGHT_TEXT_COLOUR))
    palette.setColor(QPalette.Base, QColor(*Config.BASE_COLOUR))
    palette.setColor(QPalette.Window, QColor(*Config.WINDOW_COLOUR))

    app.setPalette(palette)

    main_window = MainWindow()
    main_window.show()

    app.exec()


if __name__ == "__main__":
    main()
