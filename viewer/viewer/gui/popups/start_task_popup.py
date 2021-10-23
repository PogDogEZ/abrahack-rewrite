#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config


class StartTaskPopup(Tk):

    INSTANCE = None

    def __init__(self) -> None:  # TODO: Start task popup
        super().__init__()

        if StartTaskPopup.INSTANCE is not None:
            StartTaskPopup.INSTANCE.lift()
            StartTaskPopup.INSTANCE.focus_force()
            return

        StartTaskPopup.INSTANCE = self

        self.title("Start Task")
        self.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

        self.mainloop()

    def destroy(self) -> None:
        StartTaskPopup.INSTANCE = None
        super().destroy()