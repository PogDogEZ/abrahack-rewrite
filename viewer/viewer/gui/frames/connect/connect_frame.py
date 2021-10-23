#!/usr/bin/env python3

import json
from tkinter import *

from viewer.config import Config
from viewer.gui.frames.connect.progress_frame import ProgressFrame
from viewer.gui.popups.error_popup import ErrorPopup
from viewer.gui.widgets import ConfigLabel, ConfigButton


class ConnectFrame(Frame):

    def __init__(self, master, mcviewer, *args, **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, **kwargs)

        self.viewer = mcviewer

        self._previous_servers = [("localhost", 5001)]
        try:
            self._previous_servers = json.load(open("previous_servers.json", "r"))
        except FileNotFoundError or IsADirectoryError:
            json.dump(self._previous_servers, open("previous_servers.json", "w"))

        self._progress_frame = None

        ConfigLabel(self, text="Welcome da YesCom Viewer (smiling imp).", font=Config.LARGE_FONT).pack()

        self._connect_frame = Frame(self, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        self._connect_frame.pack()

        self._ip_frame = Frame(self._connect_frame, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        self._ip_frame.grid(row=0, column=0, rowspan=3, padx=2, sticky=NW)

        ConfigLabel(self._ip_frame, text="Direct IP:").grid(row=0, column=0, columnspan=3, sticky=W)

        self._server_host_var = StringVar(self, "localhost")
        self._server_port_var = IntVar(self, 5001)

        server_host_entry = Entry(self._ip_frame, textvariable=self._server_host_var, width=17,
                                  font=Config.TYPE_FONT)
        server_host_entry.grid(row=1, column=0, sticky=NW)

        ConfigLabel(self._ip_frame, text=":").grid(row=1, column=1, sticky=NW)

        server_port_entry = Entry(self._ip_frame, textvariable=self._server_port_var, width=8,
                                  font=Config.TYPE_FONT)
        server_port_entry.grid(row=1, column=2, sticky=NW)

        ConfigLabel(self._connect_frame, text="Recent servers (0):").grid(row=0, column=6, sticky=W)

        self._recent_servers = Listbox(self._connect_frame, width=40, height=6, font=Config.FONT, selectmode=SINGLE,
                                       borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        self._recent_servers.grid(row=1, column=6, rowspan=2, sticky=N)

        for previous_server in self._previous_servers:
            self._recent_servers.insert(END, "%s:%i" % (previous_server[0], previous_server[1]))

        self._buttons_frame = Frame(self._connect_frame, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        self._buttons_frame.grid(row=4, column=0, sticky=W)

        ConfigButton(self._buttons_frame, text="Connect", command=self._connect).grid(row=0, column=0, padx=1, sticky=W)
        ConfigButton(self._buttons_frame, text="Exit", command=self.master.destroy).grid(row=0, column=1, padx=1, sticky=W)

        self.pack(padx=3, pady=3)

    def destroy(self) -> None:
        super().destroy()

        if self.viewer.handler is not None and not self.viewer.handler.sync_done and self.viewer.connection is not None:
            self.viewer.connection.exit("Exited.")

    def _connect(self) -> None:
        self.viewer.logger.info("Connecting...")

        self.viewer.server_host = self._server_host_var.get()
        self.viewer.server_port = self._server_port_var.get()

        if self.viewer.connection is None or not self.viewer.connection.connected:
            def setup_progress_frame() -> None:
                self._progress_frame = ProgressFrame(self.master, self.viewer, self)

            try:
                self.viewer.make_connection()
                self.viewer.connection.set_auth_suppliers(lambda: self._progress_frame.get_user(),
                                                          lambda: self._progress_frame.get_password())
                self.viewer.connect()

                self.after(10, setup_progress_frame)

            except Exception as error:
                self.viewer.logger.error(repr(error))
                ErrorPopup("An error has occurred:", repr(error))
                self.viewer.connection = None
                return

        self.pack_forget()
