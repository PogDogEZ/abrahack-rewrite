#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config


class ConfigLabel(Label):

    def __init__(self, master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                 fg="#%02x%02x%02x" % Config.TEXT_COLOUR, font=Config.FONT, **kwargs) -> None:
        super().__init__(master, *args, bg=bg, fg=fg, font=font, **kwargs)


class ConfigButton(Button):

    def __init__(self, master, *args, bg="#%02x%02x%02x" % Config.WIDGET_COLOUR,
                 fg="#%02x%02x%02x" % Config.TEXT_COLOUR, activebackground="#%02x%02x%02x" % Config.HIGHLIGHT_COLOUR,
                 font=Config.FONT, borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF, **kwargs) -> None:
        super().__init__(master, *args, bg=bg, fg=fg, activebackground=activebackground, font=font, borderwidth=borderwidth, relief=relief, **kwargs)
        self._default_background = bg

        self.bind("<Enter>", self._on_enter)
        self.bind("<Leave>", self._on_leave)

    def _on_enter(self, event) -> None:
        self.config(bg=self["activebackground"])

    def _on_leave(self, event) -> None:
        self.config(bg=self._default_background)
        
