#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config


class ConfigLabel(Label):

    def __init__(self, master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, font=Config.FONT,
                 **kwargs) -> None:
        super().__init__(master, *args, bg=bg, font=font, **kwargs)


class ConfigButton(Button):

    def __init__(self, master, *args, bg="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
                 borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF, **kwargs) -> None:
        super().__init__(master, *args, bg=bg, font=font, borderwidth=borderwidth, relief=relief, **kwargs)