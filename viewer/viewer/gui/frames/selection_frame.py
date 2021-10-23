#!/usr/bin/env python3

import threading

from tkinter import *

from viewer.config import Config
from viewer.gui.widgets import ConfigLabel, ConfigButton


class SelectionFrame(Frame):

    def __init__(self, master, viewer, *args, **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, **kwargs)

        self.viewer = viewer

        self._highway_query_popup = None
        self._max_distance_var = None
        self._min_distance_var = None
        self._chunk_skip_var = None

        self._was_empty = False

        self._selected_chunks_label = ConfigLabel(self, text="Selected Chunks: 0.")
        # self._query_mode_button = ConfigButton(self, text="Select Mode: PENCIL.",
        #                                        command=self.master.change_selection_mode)
        self._query_selected_button = ConfigButton(self, text="Query Selected.",
                                                   command=lambda: threading.Thread(target=self.master.query_selected).start())
        ConfigButton(self, text="Query Highways.", command=self._do_highway_query_popup)
        # self._clear_selected_button = ConfigButton(self, text="Clear Selected.",
        #                                            command=lambda: threading.Thread(target=self.master.clear_selected).start())
        self._clear_selection_button = ConfigButton(self, text="Clear Selection.", command=self.master.selected_chunks.clear)

        for child in self.winfo_children():
            child.pack(anchor=W)

    def _do_highway_query_popup(self) -> None:
        if self._highway_query_popup is None:
            if self.viewer.current_reporter == -1:
                self.master.do_error_popup("Couldn't query highways:", "No current reporter.")
                return

            self._highway_query_popup = Tk()
            self._highway_query_popup.title("Highway Query")

            self._highway_query_popup.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

            self._max_distance_var = IntVar(self._highway_query_popup)
            self._max_distance_var.set(5000)
            self._min_distance_var = IntVar(self._highway_query_popup)
            self._min_distance_var.set(50)
            self._chunk_skip_var = IntVar(self._highway_query_popup)
            self._chunk_skip_var.set(12)

            def do_exit() -> None:
                if self._highway_query_popup is not None:
                    self._highway_query_popup.destroy()
                    self._highway_query_popup = None

            def do_query() -> None:
                self.master.query_highways(max(self._max_distance_var.get(), self._min_distance_var.get()),
                                           min(self._min_distance_var.get(), self._max_distance_var.get()),
                                           max(1, self._chunk_skip_var.get()))
                do_exit()

            ConfigLabel(self._highway_query_popup, text="Highway scan options:").grid(row=0, column=0, padx=3, pady=3, sticky=NW)

            options_frame = Frame(self._highway_query_popup, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
            options_frame.grid(row=1, column=0, padx=3, sticky=NW)

            ConfigLabel(options_frame, text="Max distance (chunks):").grid(row=0, column=0, sticky=NW)
            ConfigLabel(options_frame, text="Min distance (chunks):").grid(row=1, column=0, sticky=NW)
            ConfigLabel(options_frame, text="Chunk skip:").grid(row=2, column=0, sticky=NW)

            max_dist_entry = Entry(options_frame, textvariable=self._max_distance_var, font=Config.TYPE_FONT, width=10)
            max_dist_entry.grid(row=0, column=1, sticky=NW)
            max_dist_entry.focus_force()

            min_dist_entry = Entry(options_frame, textvariable=self._min_distance_var, font=Config.TYPE_FONT, width=10)
            min_dist_entry.grid(row=1, column=1, sticky=NW)

            chunk_skip_entry = Entry(options_frame, textvariable=self._chunk_skip_var, font=Config.TYPE_FONT, width=10)
            chunk_skip_entry.grid(row=2, column=1, sticky=NW)

            buttons_frame = Frame(self._highway_query_popup, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
            buttons_frame.grid(row=2, column=0, padx=3, pady=3, sticky=NW)

            ConfigButton(buttons_frame, text="Start", command=do_query).grid(row=0, column=0, sticky=NW)
            ConfigButton(buttons_frame, text="Cancel", command=do_exit).grid(row=0, column=1, sticky=NW)

            self._highway_query_popup.protocol("WM_DELETE_WINDOW", do_exit)
            self._highway_query_popup.mainloop()

        else:
            self._highway_query_popup.focus_force()
            self._highway_query_popup.lift()

    def on_update(self) -> None:
        self._selected_chunks_label.config(text="Selected Chunks: %i." % len(self.master.selected_chunks))

        # self._query_mode_button.config(text="Select Mode: %s." %
        #                                     main_frame.MainFrame.SelectionMode.name_from_value(self.master.selection_mode))

        if self.master.selected_chunks and self._was_empty:
            self._query_selected_button.config(state=NORMAL)
            # self._clear_selected_button.config(state=NORMAL)
            self._clear_selection_button.config(state=NORMAL)
        elif not self._was_empty:
            self._query_selected_button.config(state=DISABLED)
            # self._clear_selected_button.config(state=DISABLED)
            self._clear_selection_button.config(state=DISABLED)

        self._was_empty = bool(self.master.selected_chunks)
