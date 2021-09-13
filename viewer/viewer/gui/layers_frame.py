#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config


class LayersFrame(Frame):

    def __init__(self, master, *args, **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, **kwargs)

        Label(self, text="Layers:", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, font=Config.FONT).pack()

