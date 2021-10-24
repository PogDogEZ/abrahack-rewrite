#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config
from viewer.gui.widgets import ConfigLabel, ConfigButton


class ReporterPopup(Tk):

    INSTANCE = None

    def __init__(self, viewer) -> None:
        if ReporterPopup.INSTANCE is not None:
            ReporterPopup.INSTANCE.focus_force()
            ReporterPopup.INSTANCE.lift()
            return

        super().__init__()

        ReporterPopup.INSTANCE = self

        self.viewer = viewer

        self._last_reporter = None
        self._last_reporters = self.viewer.get_reporters()

        self.title("Select Reporter")
        self.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

        self._current_reporter_label = ConfigLabel(self, text="Current Reporter: None")
        self._current_reporter_label.grid(row=0, column=0, padx=3, pady=3, sticky=NW)

        ConfigLabel(self, text="Select reporter:").grid(row=1, column=0, padx=3, sticky=NW)

        self._reporter_listbox = Listbox(self, width=20, font=Config.FONT, selectmode=SINGLE,
                                         borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        self._reporter_listbox.grid(row=2, column=0, padx=3, sticky=NW)

        if self.viewer.current_reporter == -1 and len(self._last_reporters):
            self._reporter_listbox.selection_set(0, 0)

        for index, reporter in enumerate(self._last_reporters):
            self._reporter_listbox.insert(0, "%s (%i)" % (reporter.handler_name, reporter.handler_id))

            if reporter.handler_id == self.viewer.current_reporter:
                self._reporter_listbox.selection_set(index, index)

        buttons_frame = Frame(self, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        buttons_frame.grid(row=3, column=0, padx=3, pady=3, sticky=NW)

        ConfigButton(buttons_frame, text="Select",
                     command=self._select_reporter).grid(row=0, column=0, padx=1, sticky=NW)
        ConfigButton(buttons_frame, text="Deselect", command=self._deselect_reporter).grid(row=0, column=1)

        self.after(10, self.on_update)
        self.mainloop()

    def destroy(self) -> None:
        ReporterPopup.INSTANCE = None
        super().destroy()

    def _select_reporter(self) -> None:
        for entry in self._reporter_listbox.curselection():
            selected = self._reporter_listbox.get(entry)

            for reporter in self.viewer.get_reporters():
                if "%s (%i)" % (reporter.handler_name, reporter.handler_id) == selected:
                    self.viewer.current_reporter = reporter.handler_id
                    break

        self.destroy()

    def _deselect_reporter(self) -> None:
        self.viewer.current_reporter = -1
        self.destroy()

    def on_update(self) -> None:
        current_reporter = None if self.viewer.current_reporter == -1 else \
            self.viewer.get_reporter(handler_id=self.viewer.current_reporter)
        reporters = self.viewer.get_reporters()

        if current_reporter is None:
            self._current_reporter_label.config(text="Reporter: None")
        else:
            self._current_reporter_label.config(text="Reporter: %r (%i)" % (current_reporter.handler_name,
                                                                            current_reporter.handler_id))

        listbox_state = self._reporter_listbox.get(0, END)

        for reporter in reporters:
            if not reporter in self._last_reporters:
                self._reporter_listbox.insert(0, "%s (%i)" % (reporter.handler_name, reporter.handler_id))

        for reporter in self._last_reporters:
            if not reporter in reporters:
                self._reporter_listbox.delete(listbox_state.index("%s (%i)" % (reporter.handler_name,
                                                                               reporter.handler_id)))
        self._last_reporters = reporters

        if self.winfo_exists():
            self.after(10, self.on_update)
