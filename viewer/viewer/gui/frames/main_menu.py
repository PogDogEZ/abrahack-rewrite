#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config
from viewer.gui.popups.accounts_popup import AccountsPopup
from viewer.gui.popups.goto_popup import GotoPopup
from viewer.gui.popups.trackers_popup import TrackersPopup
from viewer.gui.widgets import ConfigLabel, ConfigButton


class MainMenu(Menu):

    def __init__(self, master, main_frame, viewer, *args, **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                         activebackground="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT, borderwidth=0,
                         activeborderwidth=0, relief=SOLID, **kwargs)

        self.main_frame = main_frame
        self.viewer = viewer

        self._goto_popup = None
        self._goto_x = None
        self._goto_z = None

        self._accounts_popup = None
        self._accounts_frame = None

        self._layers_popup = None

        file_menu = Menu(self, tearoff=0, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                         activebackground="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT, borderwidth=2,
                         activeborderwidth=0, relief=SOLID)
        file_menu.add_command(label="Refresh.", command=self._refresh_reporter)
        file_menu.add_command(label="Quit.", command=self.main_frame.destroy)

        self.add_cascade(label="File", menu=file_menu)

        edit_menu = Menu(self, tearoff=0, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                         activebackground="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT, borderwidth=2,
                         activeborderwidth=0, relief=SOLID)
        edit_menu.add_command(label="Goto.", command=lambda: GotoPopup(self.main_frame))

        self.add_cascade(label="Edit", menu=edit_menu)

        selection_menu = Menu(self, tearoff=0, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                              activebackground="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT, borderwidth=2,
                              activeborderwidth=0, relief=SOLID)
        selection_menu.add_command(label="Query Selected.", command=self.main_frame.query_selected)
        selection_menu.add_command(label="Clear Selection.", command=self.main_frame.selected_chunks.clear)

        self.add_cascade(label="Selection", menu=selection_menu)

        self.add_command(label="Accounts", command=lambda: AccountsPopup(self.viewer, self.main_frame))

        self.add_command(label="Trackers", command=lambda: TrackersPopup(self.viewer, self.main_frame))

        self._layers_menu = Menu(self, tearoff=0, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                                 activebackground="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
                                 borderwidth=2, activeborderwidth=0, relief=SOLID)

        for layer in self.main_frame.renderer.layers:
            self._layers_menu.add_checkbutton(label=layer.name.capitalize())

        self.add_command(label="Layers", command=self._do_layers_popup)
        # self.add_cascade(label="Layers", menu=self._layers_menu)

        connection_menu = Menu(self, tearoff=0, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                               activebackground="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT, borderwidth=2,
                               activeborderwidth=0, relief=SOLID)
        connection_menu.add_command(label="Connection Info.", command=self._do_connection_info_popup)

        self.add_cascade(label="Connection", menu=connection_menu)

        """
        test_menu = Menu(self, tearoff=0, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, font=Config.FONT, borderwidth=0,
                         relief=FLAT)
        test_menu.add_command(label="t")

        self.add_cascade(label="test", menu=test_menu)
        """

    def _refresh_reporter(self) -> None:
        self.viewer.current_reporter = self.viewer.current_reporter

    def _do_layers_popup(self) -> None:
        if self._layers_popup is None:
            self._layers_popup = Tk()
            self._layers_popup.title("Layers")

            self._layers_popup.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

            def do_exit() -> None:
                if self._layers_popup is not None:
                    self._layers_popup.destroy()
                    self._layers_popup = None

            # LayersFrame(self._layers_popup).pack()

            self._layers_popup.protocol("WM_DELETE_WINDOW", do_exit)
            self._layers_popup.mainloop()

        else:
            self._layers_popup.focus_force()
            self._layers_popup.lift()

    def _do_connection_info_popup(self) -> None:
        if self.viewer.connection is None:
            ...
        else:
            ...

    def on_update(self) -> None:
        if self._accounts_popup is not None and self._accounts_frame is not None:
            self._accounts_frame.on_update()
