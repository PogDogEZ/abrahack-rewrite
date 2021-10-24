#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config
from viewer.gui.widgets import ConfigLabel, ConfigButton


class GotoPopup(Tk):

    INSTANCE = None

    def __init__(self, main_frame) -> None:
        if GotoPopup.INSTANCE is not None:
            GotoPopup.INSTANCE.lift()
            GotoPopup.INSTANCE.focus_force()
            return

        super().__init__()

        GotoPopup.INSTANCE = self

        self.main_frame = main_frame

        self.title("Goto")
        self.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

        self.focus_force()
        self.lift()

        ConfigLabel(self, text="Enter coordinates to go to:").grid(row=0, column=0, padx=3, pady=3, sticky=NW)

        coordinates_frame = Frame(self, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        coordinates_frame.grid(row=1, column=0, padx=3, sticky=NW)

        ConfigLabel(coordinates_frame, text=", ").grid(row=0, column=1, sticky=NW)

        self._goto_x = Entry(coordinates_frame, font=Config.TYPE_FONT, width=9)
        self._goto_x.grid(row=0, column=0, sticky=NW)
        self._goto_x.insert(END, "0")
        self._goto_x.focus_force()
        self._goto_x.icursor(END)

        self._goto_z = Entry(coordinates_frame, font=Config.TYPE_FONT, width=9)
        self._goto_z.grid(row=0, column=2, sticky=NW)
        self._goto_z.insert(END, "0")

        buttons_frame = Frame(self, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        buttons_frame.grid(row=2, column=0, padx=3, pady=3, sticky=NW)

        ConfigButton(buttons_frame, text="Goto", command=self.do_goto).grid(row=0, column=0, sticky=NW)
        ConfigButton(buttons_frame, text="Cancel", command=self.destroy).grid(row=0, column=1, sticky=NW)

        self.after(10, self.on_update)
        self.mainloop()

    def destroy(self) -> None:
        GotoPopup.INSTANCE = None
        super().destroy()

    def do_goto(self) -> None:
        self.main_frame.goto(int(self._goto_x.get()) / 16, int(self._goto_z.get()) / 16)
        self.destroy()

    def on_update(self) -> None:
        if self.winfo_exists():
            self.after(10, self.on_update)