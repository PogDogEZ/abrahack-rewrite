#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config


class TaskInfoPopup(Tk):  # TODO: Task info popup

    INSTANCE = None

    def __init__(self) -> None:
        super().__init__()

        if TaskInfoPopup.INSTANCE is not None:
            TaskInfoPopup.INSTANCE.destroy()

        TaskInfoPopup.INSTANCE = self

        self.title("Task Info")
        self.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

        self.mainloop()

    def destroy(self) -> None:
        TaskInfoPopup.INSTANCE = None
        super().destroy()
