#!/usr/bin/env python3

from tkinter import *

import pclient.networking.packets
import viewer.network.packets

from viewer import Viewer
from viewer.gui.main_frame import MainFrame
from pclient.impl.logger import Logger, LogLevel

if __name__ == "__main__":
    logger = Logger("viewer", show_time=True, show_name=False, log_level=LogLevel.DEBUG)

    logger.debug("Registering packets...")
    pclient.networking.packets.packets.extend(viewer.network.packets.packets)
    logger.debug("Done.")

    root = Tk()
    root.title("YesCom Viewer")

    viewer = Viewer(logger)

    MainFrame(root, viewer)

    root.mainloop()
