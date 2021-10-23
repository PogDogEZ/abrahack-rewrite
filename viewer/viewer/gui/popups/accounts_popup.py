#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config
from viewer.gui.widgets import ConfigLabel, ConfigButton


class AccountsPopup(Tk):

    INSTANCE = None

    def __init__(self, viewer) -> None:
        super().__init__()

        if AccountsPopup.INSTANCE is not None:
            AccountsPopup.INSTANCE.lift()
            AccountsPopup.INSTANCE.focus_force()
            return

        AccountsPopup.INSTANCE = self

        self.viewer = viewer

        self._username_var = StringVar(self)
        self._password_var = StringVar(self)

        self.title("Accounts")
        self.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

        self._info = ConfigLabel(self, text="Players (0):")
        self._info.grid(row=0, column=0, padx=3, pady=3, sticky=W)
        self._listbox = Listbox(self, width=26, height=7, font=Config.FONT, selectmode=SINGLE,
                                borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        self._listbox.grid(row=1, column=0, padx=3, pady=3, sticky=NW)

        ConfigLabel(self, text="Selected info:").grid(row=0, column=1, padx=(0, 3), pady=3, sticky=W)
        self._info_text = Text(self, font=Config.SMALL_TYPE_FONT, width=50, height=7, borderwidth=Config.BORDER_WIDTH,
                               relief=Config.RELIEF)
        self._info_text.grid(row=1, column=1, padx=(0, 3), pady=3, sticky=NW)

        add_account_frame = Frame(self, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        add_account_frame.grid(row=2, column=0, columnspan=2, padx=3, pady=(0, 3), sticky=NW)

        ConfigLabel(add_account_frame, text="Username:", font=Config.FONT).grid(row=0, column=0, sticky=W)

        username_entry = Entry(add_account_frame, textvariable=self._username_var, font=Config.TYPE_FONT,
                               width=20)
        username_entry.grid(row=0, column=1, sticky=NW)

        ConfigLabel(add_account_frame, text="Password:").grid(row=1, column=0, sticky=W)

        password_entry = Entry(add_account_frame, textvariable=self._password_var, font=Config.TYPE_FONT,
                               width=20, show="*")
        password_entry.grid(row=1, column=1, sticky=NW)

        username_entry.focus_force()
        ConfigButton(add_account_frame, text="Login Mojang Account.").grid(row=2, column=0, columnspan=2, pady=(3, 0),
                                                                           sticky=NW)
        ConfigButton(add_account_frame, text="Login MS Account.").grid(row=3, column=0, columnspan=2, sticky=NW)

        self._goto = ConfigButton(self, text="Goto Player.")
        self._remove = ConfigButton(self, text="Remove Account.")

        self.after(10, self.on_update)
        self.mainloop()

    def destroy(self) -> None:
        AccountsPopup.INSTANCE = None
        super().destroy()

    def on_update(self) -> None:
        players = []
        current_player = None

        if self.viewer.current_reporter != -1:
            reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)
            for display_name in reporter.players:
                players.append(display_name.display_name)

            for entry in self._listbox.curselection():
                display_name = self._listbox.get(entry)
                current_player = reporter.get_player(display_name)

        self._info_text.config(state=NORMAL)

        self._info_text.delete("0.0", END)
        self._info_text.insert(END, "UUID: %r\n" % (current_player.uuid if current_player is not None else None))
        self._info_text.insert(END, "Position: %r\n" % (current_player.position if current_player is not None else None))
        self._info_text.insert(END, "Angle: %r\n" % (current_player.angle if current_player is not None else None))
        self._info_text.insert(END, "Dimension: %i\n" % (current_player.dimension if current_player is not None else 0))
        self._info_text.insert(END, "Health: %.1f\n" % (current_player.health if current_player is not None else 20))
        self._info_text.insert(END, "Hunger: %i\n" % (current_player.food if current_player is not None else 20))
        self._info_text.insert(END, "Saturation: %.1f" % (current_player.saturation if current_player is not None else 5))
        self._info_text.see(END)

        self._info_text.config(state=DISABLED)

        listbox_state = self._listbox.get(0, END)
        for display_name in listbox_state:
            if not display_name in players and display_name in listbox_state:
                self._listbox.delete(listbox_state.index(display_name))

        listbox_state = self._listbox.get(0, END)
        for display_name in players:
            if not display_name in listbox_state:
                self._listbox.insert(END, display_name)

        self._info.config(text="Players (%i):" % len(players))
        if not self._listbox.curselection() and (self._goto["state"] != DISABLED or self._remove["state"] != DISABLED):
            self._goto.config(state=DISABLED)
            self._remove.config(state=DISABLED)
        elif self._listbox.curselection() and (self._goto["state"] != NORMAL or self._remove["state"] != NORMAL):
            self._goto.config(state=NORMAL)
            self._remove.config(state=NORMAL)

        if self.winfo_exists():
            self.after(10, self.on_update)
