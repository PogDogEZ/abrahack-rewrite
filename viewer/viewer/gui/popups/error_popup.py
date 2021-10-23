#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config
from viewer.gui.widgets import ConfigLabel, ConfigButton


class ErrorPopup(Tk):

    INSTANCE = None

    def __init__(self, message: str, error: str) -> None:
        super().__init__()

        if ErrorPopup.INSTANCE is not None:
            ErrorPopup.INSTANCE.destroy()

        ErrorPopup.INSTANCE = self

        self.title("Error")
        self.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

        ConfigLabel(self, text=message, font=Config.LARGE_FONT).pack()
        ConfigLabel(self, text=error, fg="red").pack()

        ConfigButton(self, text="Ok", command=self.destroy).pack(pady=3)

        self.mainloop()

    def destroy(self) -> None:
        ErrorPopup.INSTANCE = None
        super().destroy()
