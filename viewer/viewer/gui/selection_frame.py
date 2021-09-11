#!/usr/bin/env python3
import math
import threading
from tkinter import *

from viewer.config import Config
from viewer.network.types import Dimension


class SelectionFrame(Frame):

    def __init__(self, master, mcviewer, *args, **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, **kwargs)

        self.mcviewer = mcviewer

        self._was_empty = False

        self._selected_chunks_label = Label(self, text="Selected Chunks: 0.", bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                                            font=Config.FONT)
        self._query_selected_button = Button(self, text="Query Selected.", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR,
                                             font=Config.FONT,
                                             command=lambda: threading.Thread(target=self.master.query_selected).start(),
                                             borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        self._clear_selected_button = Button(self, text="Clear Selected.", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR,
                                             font=Config.FONT,
                                             command=lambda: threading.Thread(target=self.master.clear_selected).start(),
                                             borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        self._clear_selection_button = Button(self, text="Clear Selection.", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR,
                                              font=Config.FONT,
                                              command=self.master.selected_chunks.clear, borderwidth=Config.BORDER_WIDTH,
                                              relief=Config.RELIEF)

        for child in self.winfo_children():
            child.pack(anchor=W)

    def on_update(self) -> None:
        self._selected_chunks_label.config(text="Selected Chunks: %i." % len(self.master.selected_chunks))

        if self.master.selected_chunks and self._was_empty:
            self._query_selected_button.config(state=NORMAL)
            self._clear_selected_button.config(state=NORMAL)
            self._clear_selection_button.config(state=NORMAL)
        elif not self._was_empty:
            self._query_selected_button.config(state=DISABLED)
            self._clear_selected_button.config(state=DISABLED)
            self._clear_selection_button.config(state=DISABLED)

        self._was_empty = bool(self.master.selected_chunks)
