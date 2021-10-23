#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config
from viewer.gui.widgets import ConfigLabel, ConfigButton


class TasksFrame(Frame):

    def __init__(self, master, main_frame, viewer, *args, **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, **kwargs)

        self.main_frame = main_frame
        self.viewer = viewer

        self._start_task_popup = None
        self._task_info_popup = None
        self._stop_task_popup = None

        self._info = ConfigLabel(self, text="Tasks (0):")
        self._listbox = Listbox(self, width=20, height=5, font=Config.FONT, selectmode=SINGLE,
                                borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        ConfigButton(self, text="Start Task.", command=self._start_task)
        self._show = ConfigButton(self, text="Show Task Info.", command=self._show_info)
        self._remove = ConfigButton(self, text="Stop Task.", command=self._stop_task)

        for child in self.winfo_children():
            child.pack(anchor=W)

    def _start_task(self) -> None:
        if self._start_task_popup is None:
            self._start_task_popup = Tk()
            self._start_task_popup.title("Start Task")

            self._start_task_popup.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

            def do_exit() -> None:
                if self._start_task_popup is not None:
                    self._start_task_popup.destroy()
                    self._start_task_popup = None

            self._start_task_popup.protocol("WM_DELETE_WINDOW", do_exit)
            self._start_task_popup.mainloop()

        else:
            self._start_task_popup.focus_force()
            self._start_task_popup.lift()

    def _show_info(self) -> None:
        ...

    def _stop_task(self) -> None:
        if self.viewer.current_reporter == -1:
            self.main_frame.do_error_popup("An error occurred while attempting to remove a task:",
                                           "No current reporter.")
            return

        for entry in self._listbox.curselection():
            formatted = self._listbox.get(entry)
            try:
                self.viewer.stop_task(int(formatted.split(" (")[1].replace(")", "")))
            except LookupError as error:
                self.viewer.logger.warn("Error while removing task:")
                self.viewer.logger.error(repr(error))

    def on_update(self) -> None:
        active_tasks = []

        if self.viewer.current_reporter != -1:
            reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)
            for active_task in reporter.active_tasks:
                active_tasks.append("%s (%i)" % (active_task.registered_task.name, active_task.task_id))

        self._info.config(text="Tasks (%i):" % len(active_tasks))

        listbox_state = self._listbox.get(0, END)
        for active_task in listbox_state:
            if not active_task in active_tasks:
                self._listbox.delete(listbox_state.index(active_task))

        listbox_state = self._listbox.get(0, END)
        for active_task in active_tasks:
            if not active_task in listbox_state:
                self._listbox.insert(END, active_task)

        if not self._listbox.curselection() and (self._show["state"] != DISABLED or self._remove["state"] != DISABLED):
            self._show.config(state=DISABLED)
            self._remove.config(state=DISABLED)
        elif self._listbox.curselection() and (self._show["state"] != NORMAL or self._remove["state"] != NORMAL):
            self._show.config(state=NORMAL)
            self._remove.config(state=NORMAL)
