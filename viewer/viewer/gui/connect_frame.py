#!/usr/bin/env python3

import re
import time
from tkinter import *

from viewer import YCHandler
from viewer.config import Config
from viewer.gui.main_frame import MainFrame
from pclient.impl.user import User, Group
from pclient.networking.handlers.default import DefaultHandler
from pclient.networking.handlers.handshake import HandShakeHandler


class ProgressFrame(Frame):

    def __init__(self, master, mcviewer, last_frame, *args, size=(960, 307), **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, width=size[0], height=size[1],
                         **kwargs)

        self.mcviewer = mcviewer
        self._last_frame = last_frame

        self._last_messages_length = 0

        self._login_popup = None
        self._username_var = None
        self._password_var = None
        self._ready_to_login = False

        self._done_init = False

        self._stage = "Handshake"
        self._stage_dots = 1

        self._exited = False

        Label(self, text="Connecting to server...", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
              font=Config.LARGE_FONT).pack(anchor=W)

        self._stage_label = Label(self, text="Stage: " + self._stage, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                                  font=Config.FONT)
        self._stage_label.pack(anchor=W)
        self._messages_text = Text(self, font=Config.SMALL_TYPE_FONT, width=65, height=5,
                                   borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        self._messages_text.pack(padx=3, anchor=W)

        Button(self, text="Stop.", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
               command=self._revert_to_previous, borderwidth=Config.BORDER_WIDTH,
               relief=Config.RELIEF).pack(padx=3, pady=3, side=BOTTOM, anchor=SW)

        self.pack_propagate(False)
        self.pack()

        self.after(10, self.on_update)
        self.master.protocol("WM_DELETE_WINDOW", self._revert_to_previous)

    def _revert_to_previous(self, message: str = None) -> None:
        self._exited = True

        self.destroy()
        self._last_frame.pack()

        self.master.protocol("WM_DELETE_WINDOW", self.master.destroy)

        if self.mcviewer.connection is not None:
            if self.mcviewer.connection.connected:
                self.mcviewer.connection.exit("Stopped.")
            self.mcviewer.connection = None

        if message is not None:
            self._last_frame._do_error_popup(message)

    def _do_login_popup(self) -> None:
        if self._login_popup is None:
            self._login_popup = Tk()
            self._login_popup.title("Log in to %r." % self.mcviewer.connection.server_name)

            self._username_var = StringVar(self._login_popup)
            self._password_var = StringVar(self._login_popup)

            self._login_popup.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

            # Lazy mode -> just add a space at the end
            Label(self._login_popup, text="Enter username and password for %r: " % self.mcviewer.connection.server_name,
                  bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, font=Config.LARGE_FONT).grid(row=0, column=0)

            username_frame = Frame(self._login_popup, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
            username_frame.grid(row=1, column=0, sticky=W)

            Label(username_frame, text="Username:", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                  font=Config.FONT).grid(row=0, column=0, sticky=W)

            username_entry = Entry(username_frame, textvariable=self._username_var, font=Config.TYPE_FONT, width=17)
            username_entry.grid(row=0, column=1, sticky=W)
            username_entry.focus_force()

            # TODO: Add keys later i.e. enter goes to the next entry and also exits etc for both

            Label(username_frame, text="Password:", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                  font=Config.FONT).grid(row=1, column=0, sticky=W)
            Entry(username_frame, textvariable=self._password_var, show="*", font=Config.TYPE_FONT,
                  width=17).grid(row=1, column=1, sticky=W)

            Button(self._login_popup, text="Login", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
                   command=self._login_popup.destroy, borderwidth=Config.BORDER_WIDTH,
                   relief=Config.RELIEF).grid(row=2, column=0, padx=3, pady=3, sticky=W)

            def update_login_popup() -> None:  # Need to do this here as otherwise the program will hang :/
                if self._exited:
                    self._login_popup.destroy()

                self._login_popup.after(10, update_login_popup)

            self._login_popup.after(10, update_login_popup)
            self._login_popup.mainloop()

        else:
            self._login_popup.lift()
            self._login_popup.focus_force()

    def get_username(self) -> User:
        if self._login_popup is None and not self._ready_to_login:
            self._do_login_popup()

        if self._exited or self._username_var is None or not self._username_var.get():
            return None

        if not re.fullmatch("[A-Za-z0-9_]+(@[A-Za-z0-9_]+)?", self._username_var.get()):
            raise TypeError("Username %r is not in valid username format." % self._username_var.get())

        username, group = re.split("@[A-Za-z0-9_]+", self._username_var.get())[0], \
                          re.split("^[A-Za-z0-9_]+", self._username_var.get())[1].lstrip("@")
        if not group:
            group = "local"

        return User(username, 1, 4, Group(group, 1, 4))

    def get_password(self) -> str:
        if self._login_popup is None and not self._ready_to_login:
            self._do_login_popup()

        if self._exited or self._password_var is None:
            return ""

        return self._password_var.get()

    def on_update(self) -> None:
        if self.mcviewer.connection is not None:
            if not self.mcviewer.connection.connected:
                self._revert_to_previous(self.mcviewer.connection.exit_reason)
                return

            if isinstance(self.mcviewer.connection.handler, HandShakeHandler):
                if self.mcviewer.connection.handler.state == HandShakeHandler.State.HAND_SHAKE:
                    self._stage = "Handshake"
                elif self.mcviewer.connection.handler.state == HandShakeHandler.State.ENCRYPTION:
                    self._stage = "Encrypting"
                elif self.mcviewer.connection.handler.state == HandShakeHandler.State.LOGIN:
                    self._stage = "Authenticating"
                else:
                    self._stage = "Handoff"  # This shouldn't happen btw
            elif isinstance(self.mcviewer.connection.handler, DefaultHandler):
                if not self._done_init:
                    self._done_init = True
                    self.mcviewer.init()

                self._stage = "Sync"

                if self.mcviewer.ce_handler.first_sync_done:
                    self.mcviewer.logger.info("Finished sync, starting the chunk viewer.")
                    self.destroy()
                    self._last_frame.destroy()
                    MainFrame(self.master, self.mcviewer)
                    return

        self._stage_label.config(text="Stage: " + self._stage + "." * (self._stage_dots // 50 + 1))
        self._stage_dots += 1
        if self._stage_dots > 150:
            self._stage_dots = 1

        if len(self.mcviewer.logger.messages) != self._last_messages_length:
            self._messages_text.config(state=NORMAL)
            for message in self.mcviewer.logger.messages[self._last_messages_length:]:
                self._messages_text.insert(END, "[%s] %s\n" % (message.log_level.name, message.text))
            self._messages_text.see(END)
            self._messages_text.config(state=DISABLED)
            self._last_messages_length = len(self.mcviewer.logger.messages)

        self.after(10, self.on_update)


class ConnectFrame(Frame):

    def __init__(self, master, mcviewer, *args, size=(960, 307), **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, width=size[0], height=size[1], **kwargs)

        self.mcviewer = mcviewer

        self._error_popup = None
        self._exited = False

        Label(self, text="Welcome da YesCom Viewer (smiling imp).", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
              font=Config.LARGE_FONT).pack()

        self._connect_frame = Frame(self, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        self._connect_frame.pack()

        self._ip_frame = Frame(self._connect_frame, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        self._ip_frame.grid(row=0, column=0, rowspan=3, padx=2, sticky=NW)

        Label(self._ip_frame, text="Direct IP:", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
              font=Config.FONT).grid(row=0, column=0, columnspan=3, sticky=W)

        self._server_host_var = StringVar(self, "localhost")
        self._server_port_var = IntVar(self, 5001)
        self._target_host_var = StringVar(self, "localhost")
        self._target_port_var = IntVar(self, 25565)

        server_host_entry = Entry(self._ip_frame, textvariable=self._server_host_var, width=17,
                                  font=Config.TYPE_FONT)
        server_host_entry.grid(row=1, column=0, sticky=NW)

        Label(self._ip_frame, text=":", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
              font=Config.FONT).grid(row=1, column=1, sticky=NW)

        server_port_entry = Entry(self._ip_frame, textvariable=self._server_port_var, width=8,
                                  font=Config.TYPE_FONT)
        server_port_entry.grid(row=1, column=2, sticky=NW)

        Label(self._ip_frame, text="Target IP:", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
              font=Config.FONT).grid(row=2, column=0, sticky=NW)

        target_host_entry = Entry(self._ip_frame, textvariable=self._target_host_var, width=17,
                                  font=Config.TYPE_FONT)
        target_host_entry.grid(row=3, column=0, sticky=NW)

        Label(self._ip_frame, text=":", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
              font=Config.FONT).grid(row=3, column=1, sticky=NW)

        target_port_entry = Entry(self._ip_frame, textvariable=self._target_port_var, width=8,
                                  font=Config.TYPE_FONT)
        target_port_entry.grid(row=3, column=2, sticky=NW)

        Label(self._connect_frame, text="Recent servers (0):", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
              font=Config.FONT).grid(row=0, column=6, sticky=W)

        self._recent_servers = Listbox(self._connect_frame, width=40, height=7, borderwidth=Config.BORDER_WIDTH,
                                       relief=Config.RELIEF)
        self._recent_servers.grid(row=1, column=6, rowspan=2, sticky=N)

        self._buttons_frame = Frame(self._connect_frame, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        self._buttons_frame.grid(row=4, column=0, sticky=W)

        Button(self._buttons_frame, text="Connect", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
               command=self._connect, borderwidth=Config.BORDER_WIDTH,
               relief=Config.RELIEF).grid(row=0, column=0, padx=1, sticky=W)
        Button(self._buttons_frame, text="Exit", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
               command=self.on_exit, borderwidth=Config.BORDER_WIDTH,
               relief=Config.RELIEF).grid(row=0, column=1, padx=1, sticky=W)

        self.pack_propagate(False)
        self.pack()

        self.after(10, self.on_update)

    def _do_error_popup(self, message: str) -> None:
        if self._error_popup is None:
            self._error_popup = Tk()
            self._error_popup.title("Error")

            self._error_popup.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

            Label(self._error_popup, text="An error has occurred:", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                  font=Config.LARGE_FONT).pack()
            Label(self._error_popup, text=message, fg="red", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                  font=Config.TYPE_FONT).pack()

            def do_exit() -> None:
                self._error_popup.destroy()
                self._error_popup = None

            Button(self._error_popup, text="Ok.", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
                   command=do_exit, borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF).pack(pady=3)

            # self._error_popup.resizable(0, 0)
            self._error_popup.protocol("WM_DELETE_WINDOW", do_exit)

        else:
            self._error_popup.destroy()
            self._error_popup = None
            self._do_error_popup(message)

    def _connect(self) -> None:
        self.mcviewer.logger.info("Connecting...")

        self.mcviewer.server_host = self._server_host_var.get()
        self.mcviewer.server_port = self._server_port_var.get()
        self.mcviewer.target_host = self._target_host_var.get()
        self.mcviewer.target_port = self._target_port_var.get()

        if self.mcviewer.connection is None or not self.mcviewer.connection.connected:
            progress_frame = ProgressFrame(self.master, self.mcviewer, self)

            try:
                self.mcviewer.make_connection()
                self.mcviewer.connection.set_auth_suppliers(progress_frame.get_username, progress_frame.get_password)
                self.mcviewer.connect()
            except Exception as error:
                progress_frame.pack_forget()
                self.mcviewer.logger.error(repr(error))
                self._do_error_popup(repr(error))
                self.mcviewer.connection = None
                return

        self.pack_forget()

    def on_exit(self) -> None:
        self._exited = True

        self.destroy()
        self.master.destroy()
        if self.mcviewer.connection is not None:
            self.mcviewer.connection.exit("Exited.")

    def on_update(self) -> None:
        self.after(10, self.on_update)
