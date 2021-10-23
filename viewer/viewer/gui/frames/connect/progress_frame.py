#!/usr/bin/env python3

import re
import time
from tkinter import *

from pclient.impl.user import User, Group
from pclient.networking.handlers.default import DefaultHandler
from pclient.networking.handlers.handshake import HandShakeHandler
from viewer.config import Config
from viewer.gui.frames.main_frame import MainFrame
from viewer.gui.popups.error_popup import ErrorPopup
from viewer.gui.popups.login_popup import LoginPopup
from viewer.gui.widgets import ConfigLabel, ConfigButton


class ProgressFrame(Frame):

    def __init__(self, master, mcviewer, last_frame, *args, size=(960, 307), **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, width=size[0], height=size[1],
                         **kwargs)

        self.viewer = mcviewer

        self._last_frame = last_frame

        self._last_messages_length = 0

        self._username = ""
        self._password = ""
        self._ready_to_login = False

        self._done_init = False

        self._stage = "Handshake"
        self._stage_dots = 1

        ConfigLabel(self, text="Connecting to server...", font=Config.LARGE_FONT).pack(anchor=W)

        self._stage_label = ConfigLabel(self, text="Stage: " + self._stage)
        self._stage_label.pack(anchor=W)
        self._messages_text = Text(self, font=Config.SMALL_TYPE_FONT, width=65, height=5,
                                   borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        self._messages_text.pack(padx=3, anchor=W)

        ConfigButton(self, text="Stop", command=self._revert_to_previous).pack(padx=3, pady=3, side=BOTTOM, anchor=SW)

        self.pack_propagate(False)
        self.pack()

        self.after(10, self.on_update)
        self.master.protocol("WM_DELETE_WINDOW", self._revert_to_previous)

    def destroy(self) -> None:
        self.master.protocol("WM_DELETE_WINDOW", self.master.destroy)
        super().destroy()

    def _revert_to_previous(self, message: str = None) -> None:
        self._last_frame.pack()

        if self.viewer.connection is not None:
            if self.viewer.connection.connected:
                self.viewer.connection.exit("Stopped.")
            self.viewer.connection = None

        if message is not None:
            ErrorPopup("An error has occurred in stage %s:" % self._stage, message)

        self.destroy()

    def _on_login(self, username: str, password: str) -> None:
        self._username = username
        self._password = password
        self._ready_to_login = True

    def get_user(self) -> User:
        if not self._ready_to_login:
            self.after(10, lambda: LoginPopup(self.viewer.connection.server_name, self._on_login))

            while not self._ready_to_login:
                time.sleep(0.1)

        if not self.winfo_exists():
            return None

        if not re.fullmatch("[A-Za-z0-9_]+(@[A-Za-z0-9_]+)?", self._username):
            raise TypeError("Username %r is not in valid username format." % self._username)

        username, group = re.split("@[A-Za-z0-9_]+", self._username)[0], \
                          re.split("^[A-Za-z0-9_]+", self._username)[1].lstrip("@")
        if not group:
            group = "local"

        return User(username, 1, 4, Group(group, 1, 4))

    def get_password(self) -> str:
        if not self._ready_to_login:
            self.after(10, lambda: LoginPopup(self.viewer.connection.server_name, self._on_login))

            while not self._ready_to_login:
                time.sleep(0.1)

        if not self.winfo_exists():
            return ""

        return self._password

    def on_update(self) -> None:
        if self.viewer.connection is not None:
            if not self.viewer.connection.connected:
                self._revert_to_previous(self.viewer.connection.exit_reason)
                return

            if isinstance(self.viewer.connection.handler, HandShakeHandler):
                if self.viewer.connection.handler.state == HandShakeHandler.State.HAND_SHAKE:
                    self._stage = "Handshake"
                elif self.viewer.connection.handler.state == HandShakeHandler.State.ENCRYPTION:
                    self._stage = "Encrypting"
                elif self.viewer.connection.handler.state == HandShakeHandler.State.LOGIN:
                    self._stage = "Authenticating"
                else:
                    self._stage = "Handoff"  # This shouldn't happen btw

            elif isinstance(self.viewer.connection.handler, DefaultHandler):
                if not self._done_init:
                    self._done_init = True
                    self.viewer.init()

                self._stage = "Sync"

                if self.viewer.handler.sync_done:
                    self.viewer.logger.info("Finished sync, starting the chunk viewer.")

                    self._last_frame.destroy()

                    MainFrame(self.master, self.viewer)
                    self.destroy()
                    return

        self._stage_dots += 1
        if self._stage_dots > 150:
            self._stage_dots = 1
        self._stage_label.config(text="Stage: " + self._stage + "." * (self._stage_dots // 50 + 1))

        if len(self.viewer.logger.messages) != self._last_messages_length:
            self._messages_text.config(state=NORMAL)
            for message in self.viewer.logger.messages[self._last_messages_length:]:
                self._messages_text.insert(END, "[%s] %s\n" % (message.log_level.name, message.text))
            self._messages_text.see(END)
            self._messages_text.config(state=DISABLED)
            self._last_messages_length = len(self.viewer.logger.messages)

        if self.winfo_exists():
            self.after(10, self.on_update)
