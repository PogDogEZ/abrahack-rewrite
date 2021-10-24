#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config
from viewer.gui.widgets import ConfigLabel, ConfigButton


class LoginPopup(Tk):

    INSTANCE = None

    @property
    def username(self) -> str:
        return "" if self._username_var is None else self._username_var.get()

    @property
    def password(self) -> str:
        return "" if self._password_var is None else self._password_var.get()

    def __init__(self, server_name: str, call_back) -> None:
        if LoginPopup.INSTANCE is not None:
            LoginPopup.INSTANCE.lift()
            LoginPopup.INSTANCE.focus_force()
            return

        super().__init__()

        LoginPopup.INSTANCE = self

        self._call_back = call_back

        self.title("Log in to %r." % server_name)
        self.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

        self._username_var = StringVar(self)
        self._password_var = StringVar(self)

        # Lazy mode -> just add a space at the end
        ConfigLabel(self, text="Enter username and password for %r: " % server_name,
                    font=Config.LARGE_FONT).grid(row=0, column=0)

        username_frame = Frame(self, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        username_frame.grid(row=1, column=0, sticky=W)

        ConfigLabel(username_frame, text="Username:").grid(row=0, column=0, sticky=W)

        username_entry = Entry(username_frame, textvariable=self._username_var, font=Config.TYPE_FONT, width=17)
        username_entry.grid(row=0, column=1, sticky=W)
        username_entry.focus_force()

        # TODO: Add keys later i.e. enter goes to the next entry and also exits etc for both

        ConfigLabel(username_frame, text="Password:").grid(row=1, column=0, sticky=W)
        Entry(username_frame, textvariable=self._password_var, show="*", font=Config.TYPE_FONT,
              width=17).grid(row=1, column=1, sticky=W)

        ConfigButton(self, text="Login", command=self.destroy).grid(row=2, column=0, padx=3, pady=3, sticky=W)

        self.mainloop()

    def destroy(self) -> None:
        if self._call_back is not None:
            self._call_back(self.username, self.password)

        LoginPopup.INSTANCE = None
        super().destroy()
