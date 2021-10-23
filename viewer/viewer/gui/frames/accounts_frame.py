#!/usr/bin/env python3

import time
from tkinter import *

from viewer.config import Config
from viewer.gui.widgets import ConfigLabel, ConfigButton
from viewer.util import Player


class AccountsFrame(Frame):

    def __init__(self, master, main_frame, viewer, *args, **kwargs):
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, **kwargs)

        self.main_frame = main_frame
        self.viewer = viewer

        self._account_popup = None
        self._close_account_popup = False
        self._message = ""
        self._error = ""

        self._info_popup = None
        self._close_info_popup = False
        self._info_text = None
        self._info_player = None

        self._username_var = None
        self._password_var = None

        self._info = ConfigLabel(self, text="Players (0):")
        self._listbox = Listbox(self, width=18, height=7, font=Config.FONT, selectmode=SINGLE,
                                borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        ConfigButton(self, text="Add Account.", command=self._do_account_popup)
        self._show = ConfigButton(self, text="Show Player Info.", command=self._show_info)
        self._goto = ConfigButton(self, text="Goto Player.", command=self._goto_account)
        self._remove = ConfigButton(self, text="Remove Account.", command=self._remove_account)

        for child in self.winfo_children():
            child.pack(anchor=W)

    def _do_account_popup(self) -> None:
        if self._account_popup is None:
            if self.viewer.current_reporter == -1:
                self.main_frame.do_error_popup("An error occurred while attempting to add an account:",
                                               "No current reporter.")
                return

            self._account_popup = Tk()
            self._account_popup.title("Add Account")

            self._account_popup.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

            def do_exit() -> None:
                if self._account_popup is not None:
                    self._account_popup.destroy()
                    self._account_popup = None

            self._username_var = StringVar(self._account_popup)
            self._password_var = StringVar(self._account_popup)

            ConfigLabel(self._account_popup, text="Username:", font=Config.FONT).grid(row=0, column=0)

            username_entry = Entry(self._account_popup, textvariable=self._username_var, font=Config.TYPE_FONT,
                                   width=20)
            username_entry.grid(row=0, column=1)

            ConfigLabel(self._account_popup, text="Password:").grid(row=1, column=0)

            password_entry = Entry(self._account_popup, textvariable=self._password_var, font=Config.TYPE_FONT,
                                   width=20, show="*")
            password_entry.grid(row=1, column=1)

            username_entry.focus_force()

            ConfigButton(self._account_popup, text="Login.",
                         command=lambda: self.viewer.add_account(self._username_var.get(), self._password_var.get(),
                                                                 self._on_response)
                         ).grid(row=2, column=0, columnspan=2, pady=3)

            self._account_popup.protocol("WM_DELETE_WINDOW", do_exit)

        else:
            self._account_popup.lift()
            self._account_popup.focus_force()

    def _on_response(self, successful: bool, message: str) -> None:
        self._close_account_popup = True

        if not successful:
            self._message = "An error occurred while attempting to log in an account:"
            self._error = message
        else:
            ...

    def _do_info_popup(self, player: Player) -> None:
        if self._info_popup is None:
            self._info_player = player

            self._info_popup = Tk()
            self._info_popup.title("Player Info")

            self._info_popup.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

            def do_exit() -> None:
                self._info_player = None

                if self._info_text is not None:
                    self._info_text.destroy()
                    self._info_text = None
                if self._info_popup is not None:
                    self._info_popup.destroy()
                    self._info_popup = None

            ConfigLabel(self._info_popup, text="Player info for %r:" % player.display_name
                        ).grid(row=0, column=0, padx=3, pady=3, sticky=NW)

            self._info_text = Text(self._info_popup, font=Config.SMALL_TYPE_FONT, width=65, height=7,
                                   borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
            self._info_text.grid(row=1, column=0, padx=3, sticky=NW)

            ConfigButton(self._info_popup, text="Done.", command=do_exit).grid(row=2, column=0, padx=3, pady=3, sticky=NW)

            self._info_popup.protocol("WM_DELETE_WINDOW", do_exit)

        else:
            self._info_player = None

            if self._info_text is not None:
                self._info_text.destroy()
                self._info_text = None
            if self._info_popup is not None:
                self._info_popup.destroy()
                self._info_popup = None

    def _show_info(self) -> None:  # TODO: Show account info
        if self.viewer.current_reporter == -1:
            self.main_frame.do_error_popup("An error occurred while attempting to show info for an account:",
                                           "No current reporter.")
            return

        reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)

        for entry in self._listbox.curselection():
            display_name = self._listbox.get(entry)
            player = reporter.get_player(display_name)

            self._do_info_popup(player)

    def _goto_account(self) -> None:
        if self.viewer.current_reporter == -1:
            self.main_frame.do_error_popup("An error occurred while attempting to show info for an account:",
                                           "No current reporter.")
            return

        reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)
        players = reporter.players

        for entry in self._listbox.curselection():
            display_name = self._listbox.get(entry)
            player = reporter.get_player(display_name)

            self.main_frame.current_dimension = player.dimension + 1
            self.main_frame.goto(player.position.x / 16, player.position.z / 16)
            self.main_frame.scale_and_pan(self.main_frame.size[0] / 2, self.main_frame.size[1] / 2, 3, 3)

    def _remove_account(self) -> None:
        if self.viewer.current_reporter == -1:
            self.main_frame.do_error_popup("An error occurred while attempting to remove an account:",
                                           "No current reporter.")
            return

        reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)

        for entry in self._listbox.curselection():
            display_name = self._listbox.get(entry)
            player = reporter.get_player(display_name)

            self.viewer.remove_account(player.username)
            self._listbox.delete(entry)

    def on_update(self) -> None:
        if self._close_account_popup:
            self._close_account_popup = False

            if self._account_popup is not None:
                self._account_popup.destroy()
                self._account_popup = None

        if self._close_info_popup:
            self._close_info_popup = False

            self._info_player = None

            if self._info_text is not None:
                self._info_text.destroy()
                self._info_text = None
            if self._info_popup is not None:
                self._info_popup.destroy()
                self._info_popup = None

        if self._info_popup is not None and self._info_text is not None and self._info_player is not None:
            self._info_text.config(state=NORMAL)

            self._info_text.delete("0.0", END)
            self._info_text.insert(END, "UUID: %r\n" % self._info_player.uuid)
            self._info_text.insert(END, "Position: %r\n" % self._info_player.position)
            self._info_text.insert(END, "Angle: %r\n" % self._info_player.angle)
            self._info_text.insert(END, "Dimension: %i\n" % self._info_player.dimension)
            self._info_text.insert(END, "Health: %.1f\n" % self._info_player.health)
            self._info_text.insert(END, "Hunger: %i\n" % self._info_player.food)
            self._info_text.insert(END, "Saturation: %.1f" % self._info_player.saturation)
            self._info_text.see(END)

            self._info_text.config(state=DISABLED)

        if self._message or self._error:
            self.main_frame.do_error_popup(self._message, self._error)
            self._message = ""
            self._error = ""

        players = []

        if self.viewer.current_reporter != -1:
            reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)
            for display_name in reporter.players:
                players.append(display_name.display_name)

        listbox_state = self._listbox.get(0, END)
        for display_name in listbox_state:
            if not display_name in players and display_name in listbox_state:
                self._listbox.delete(listbox_state.index(display_name))

        listbox_state = self._listbox.get(0, END)
        for display_name in players:
            if not display_name in listbox_state:
                self._listbox.insert(END, display_name)

        self._info.config(text="Players (%i):" % len(players))
        if not self._listbox.curselection() and (self._show["state"] != DISABLED or self._goto["state"] != DISABLED or
                                                 self._remove["state"] != DISABLED):
            self._show.config(state=DISABLED)
            self._goto.config(state=DISABLED)
            self._remove.config(state=DISABLED)
        elif self._listbox.curselection() and (self._show["state"] != NORMAL or self._goto["state"] != NORMAL or
                                               self._remove["state"] != NORMAL):
            self._show.config(state=NORMAL)
            self._goto.config(state=NORMAL)
            self._remove.config(state=NORMAL)
