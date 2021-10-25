#!/usr/bin/env python3

from tkinter import *


class TrackersPopup(Tk):

    INSTANCE = None

    def __init__(self) -> None:
        if TrackersPopup.INSTANCE is not None:
            TrackersPopup.INSTANCE.focus_force()
            TrackersPopup.INSTANCE.lift()
            return

        super().__init__()

        TrackersPopup.INSTANCE = self

        self.title("Trackers")

    def destroy(self) -> None:
        TrackersPopup.INSTANCE = None
        super().destroy()