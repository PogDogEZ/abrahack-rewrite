#!/usr/bin/env python3

import colorama
from tkinter import *

import pclient.networking.packets
import viewer.network.packets

from viewer import Viewer
from viewer.gui.frames.connect import ConnectFrame
from pclient.impl.logger import Logger, LogLevel
from viewer.gui.frames.main_frame import MainFrame

colorama.init()

if __name__ == "__main__":
    logger = Logger("viewer", show_time=True, show_name=False, log_level=LogLevel.DEBUG)

    logger.debug("Registering packets...")
    pclient.networking.packets.packets.extend(viewer.network.packets.packets)
    logger.debug("Done.")

    root = Tk()
    root.title("Viewer")

    viewer = Viewer(logger)

    ConnectFrame(root, viewer)

    root.mainloop()
