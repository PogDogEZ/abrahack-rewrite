#!/usr/bin/env python3

import time
from tkinter import *

from viewer.config import Config


class AccountsFrame(Frame):

    def __init__(self, master, mcviewer, *args, **kwargs):
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, **kwargs)

        self.mcviewer = mcviewer

        self._account_popup = None
        self._close_account_popup = False
        self._message = ""
        self._error = ""

        self._username_var = None
        self._password_var = None

        self._info = Label(self, text="Accounts Online (0):", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                           font=Config.FONT)
        self._listbox = Listbox(self, width=20, font=Config.FONT, selectmode=SINGLE, borderwidth=Config.BORDER_WIDTH,
                                relief=Config.RELIEF)
        Button(self, text="Add Account.", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
               command=self._do_account_popup, borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        self._show = Button(self, text="Show Account Info.", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR,
                            font=Config.FONT, command=self.show_info, borderwidth=Config.BORDER_WIDTH,
                            relief=Config.RELIEF)
        self._remove = Button(self, text="Remove Account.", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
                              command=self.remove_account, borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)

        for child in self.winfo_children():
            child.pack(anchor=W)

    def _do_account_popup(self) -> None:
        if self._account_popup is None:
            if self.mcviewer.current_reporter is None:
                self.master.do_error_popup("An error occurred while attempting to add an account:",
                                           "No current reporter.")
                return

            self._account_popup = Tk()
            self._account_popup.title("Add Account")

            self._account_popup.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

            self._username_var = StringVar(self._account_popup)
            self._password_var = StringVar(self._account_popup)

            Label(self._account_popup, text="Username:", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                  font=Config.FONT).grid(row=0, column=0)

            username_entry = Entry(self._account_popup, textvariable=self._username_var, font=Config.TYPE_FONT,
                                   width=20)
            username_entry.grid(row=0, column=1)

            Label(self._account_popup, text="Password:", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                  font=Config.FONT).grid(row=1, column=0)

            password_entry = Entry(self._account_popup, textvariable=self._password_var, font=Config.TYPE_FONT,
                                   width=20, show="*")
            password_entry.grid(row=1, column=1)

            username_entry.focus_force()

            Button(self._account_popup, text="Login.", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
                   command=lambda: self.mcviewer.add_account(self.mcviewer.current_reporter, self._username_var.get(),
                                                             self._password_var.get(), self._on_response),
                   borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF).grid(row=2, column=0, columnspan=2, pady=3)

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

    def show_info(self) -> None:  # TODO: Show account info
        if self.mcviewer.current_reporter is None:
            self.master.do_error_popup("An error occurred while attempting to show info for an account:",
                                       "No current reporter.")
            return

        for entry in self._listbox.curselection():
            username = self._listbox.get(entry)
            for player in self.mcviewer.players:
                if player.name == username:
                    print(player)
            print(username)

    def remove_account(self) -> None:
        if self.mcviewer.current_reporter is None:
            self.master.do_error_popup("An error occurred while attempting to remove an account:",
                                       "No current reporter.")
            return

        for entry in self._listbox.curselection():
            username = self._listbox.get(entry)
            for player in self.mcviewer.players:
                if player.name == username:
                    self.mcviewer.disconnect(player)
                    self._listbox.delete(entry)
                    break

    def on_update(self) -> None:
        if self._close_account_popup:
            self._close_account_popup = False
            self._account_popup.destroy()
            self._account_popup = None

        if self._message or self._error:
            self.master.do_error_popup(self._message, self._error)
            self._message = ""
            self._error = ""

        self._info.config(text="Accounts (%i):" % 0)
        if not self._listbox.curselection():
            self._show.config(state=DISABLED)
            self._remove.config(state=DISABLED)
        else:
            self._show.config(state=NORMAL)
            self._remove.config(state=NORMAL)
