#!/usr/bin/env python3

from tkinter import *
from typing import List

from viewer.config import Config
from viewer.gui.widgets import ConfigLabel, ConfigButton
from viewer.util import Tracker, Reporter, Dimension


class TrackersPopup(Tk):

    INSTANCE = None

    def __init__(self, viewer, main_frame) -> None:
        if TrackersPopup.INSTANCE is not None:
            TrackersPopup.INSTANCE.focus_force()
            TrackersPopup.INSTANCE.lift()
            return

        super().__init__()

        TrackersPopup.INSTANCE = self

        self.viewer = viewer
        self.main_frame = main_frame

        self.title("Trackers")
        self.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

        self._info = ConfigLabel(self, text="Trackers (0):")
        self._info.grid(row=0, column=0, padx=3, pady=3, sticky=W)
        self._listbox = Listbox(self, width=20, height=10, font=Config.FONT, selectmode=SINGLE,
                                borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        self._listbox.grid(row=1, column=0, padx=3, pady=3, sticky=NW)

        ConfigLabel(self, text="Selected info:").grid(row=0, column=1, padx=(0, 3), pady=3, sticky=W)
        self._info_text = Text(self, font=Config.SMALL_TYPE_FONT, width=50, height=7, borderwidth=Config.BORDER_WIDTH,
                               relief=Config.RELIEF)
        self._info_text.grid(row=1, column=1, padx=(0, 3), pady=3, sticky=NW)

        buttons_frame = Frame(self, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        buttons_frame.grid(row=2, column=0, columnspan=2, padx=3, pady=(0, 3), sticky=W)

        self._goto = ConfigButton(buttons_frame, text="Goto Player", command=self._goto_player)
        self._goto.grid(row=0, column=0, padx=(0, 2), sticky=W)
        # self._remove = ConfigButton(buttons_frame, text="Remove Tracker", command=self._remove_tracker)
        # self._remove.grid(row=0, column=1, padx=(0, 2), sticky=W)
        ConfigButton(buttons_frame, text="Exit", command=self.destroy).grid(row=0, column=1, sticky=W)

        self.after(10, self.on_update)
        self.mainloop()

    def destroy(self) -> None:
        TrackersPopup.INSTANCE = None
        super().destroy()

    def _get_selection(self, reporter: Reporter) -> List[Tracker]:
        trackers = []

        for entry in self._listbox.curselection():
            tracker_id = int(self._listbox.get(entry).split("tracker ")[1])
            try:
                tracker = reporter.get_tracker(tracker_id)
            except LookupError:
                continue

            if tracker is not None:
                trackers.append(tracker)

        return trackers

    def _goto_player(self) -> None:
        if self.viewer.current_reporter != -1:
            reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)

            for tracker in self._get_selection(reporter):
                self.main_frame.current_dimension = Dimension.mc_to_value(tracker.tracked_player.dimension)
                self.main_frame.goto(tracker.tracked_player.render_distance.center_position.x,
                                     tracker.tracked_player.render_distance.center_position.z)
                self.main_frame.scale_and_pan(self.main_frame.size[0] / 2, self.main_frame.size[1] / 2, 3, 3)

    def on_update(self) -> None:
        trackers = []
        current_tracker = None

        if self.viewer.current_reporter != -1:
            reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)
            for tracker in reporter.trackers:
                trackers.append("tracker %i" % tracker.tracker_id)

            for tracker in self._get_selection(reporter):
                current_tracker = tracker

        tracker_id = 0
        tracked_player_id = 0
        dimension = 0
        position = None
        most_probable = "unknown"

        if current_tracker is not None:
            tracker_id = current_tracker.tracker_id
            tracked_player_id = current_tracker.tracked_player.tracked_player_id
            dimension = current_tracker.tracked_player.dimension
            position = (current_tracker.tracked_player.render_distance.center_position.x * 16,
                        current_tracker.tracked_player.render_distance.center_position.z * 16)

            best_possible = current_tracker.tracked_player.get_best_possible_player()
            if best_possible is not None:
                most_probable = self.viewer.get_name_for_uuid(best_possible)

        self._info_text.config(state=NORMAL)

        self._info_text.delete("0.0", END)
        self._info_text.insert(END, "Tracker ID: %i\n" % tracker_id)
        self._info_text.insert(END, "Tracked player ID: %i\n" % tracked_player_id)
        self._info_text.insert(END, "Dimension: %i\n" % dimension)
        self._info_text.insert(END, "Position: %r\n" % (position,))
        self._info_text.insert(END, "Most probable: %r\n" % most_probable)

        self._info_text.config(state=DISABLED)

        listbox_state = self._listbox.get(0, END)
        for tracker in listbox_state:
            if not tracker in trackers and tracker in listbox_state:
                self._listbox.delete(listbox_state.index(tracker))

        listbox_state = self._listbox.get(0, END)
        for tracker in trackers:
            if not tracker in listbox_state:
                self._listbox.insert(END, tracker)

        self._info.config(text="Trackers (%i):" % len(trackers))
        if not self._listbox.curselection() and self._goto["state"] != DISABLED:
            self._goto.config(state=DISABLED)
        elif self._listbox.curselection() and self._goto["state"] != NORMAL:
            self._goto.config(state=NORMAL)

        if self.winfo_exists():
            self.after(10, self.on_update)
