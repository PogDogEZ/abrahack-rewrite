#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config
from viewer.gui.widgets import ConfigLabel, ConfigButton


class InfoFrame(Frame):

    def __init__(self, master, viewer, *args, **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, **kwargs)

        self.viewer = viewer

        self._last_players = 0

        self._waiting_queries = ConfigLabel(self, text="Waiting queries: 0")
        self._ticking_queries = ConfigLabel(self, text="Ticking queries: 0")

        self._tick_rate = ConfigLabel(self, text="Tickrate: 20.0tps")
        self._time_since_last_packet = ConfigLabel(self, text="TSLP: 0ms")

        self._players_online = ConfigButton(self, text="Online players: 0")

        for child in self.winfo_children():
            child.pack(anchor=W)

    def on_update(self) -> None:
        waiting_queries = 0
        ticking_queries = 0

        tick_rate = 20
        time_since_last_packet = 0

        players = 0

        if self.viewer.current_reporter != -1:
            reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)

            waiting_queries = reporter.waiting_queries
            ticking_queries = reporter.ticking_queries

            tick_rate = reporter.tick_rate
            time_since_last_packet = reporter.time_since_last_packet

            players = len(reporter.online_players)

        self._waiting_queries.config(text="Waiting queries: %i" % waiting_queries)
        self._ticking_queries.config(text="Ticking queries: %i" % ticking_queries)

        self._tick_rate.config(text="Tickrate: %.1ftps" % tick_rate)
        self._time_since_last_packet.config(text="TSLP: %ims" % time_since_last_packet)

        if self._last_players != players:
            self._players_online.config(text="Online players: %i" % players)
        self._last_players = players
