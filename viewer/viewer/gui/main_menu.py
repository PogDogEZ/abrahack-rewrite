#!/usr/bin/env python3

from tkinter import *

from viewer.config import Config
from pclient.networking.packets import ConnectionInfoRequestPacket
from viewer.gui.layers_frame import LayersFrame


class MainMenu(Menu):

    def __init__(self, master, mcviewer, *args, **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                         activebackground="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT, borderwidth=0,
                         activeborderwidth=0, relief=SOLID, **kwargs)

        self.mcviewer = mcviewer

        self._layers_popup = None

        file_menu = Menu(self, tearoff=0, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                         activebackground="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT, borderwidth=2,
                         activeborderwidth=0, relief=SOLID)
        file_menu.add_command(label="Quit.", command=self.master.on_exit)

        self.add_cascade(label="File", menu=file_menu)
        self.add_cascade(label="Edit")

        selection_menu = Menu(self, tearoff=0, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                              activebackground="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT, borderwidth=2,
                              activeborderwidth=0, relief=SOLID)
        selection_menu.add_command(label="Query Selected.", command=self.master.query_selected)
        selection_menu.add_command(label="Clear Selected.", command=self.master.clear_selected)
        selection_menu.add_command(label="Clear Selection.", command=self.master.selected_chunks.clear)

        self.add_cascade(label="Selection", menu=selection_menu)

        self._layers_menu = Menu(self, tearoff=0, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                                 activebackground="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
                                 borderwidth=2, activeborderwidth=0, relief=SOLID)

        for layer in self.master.renderer.layers:
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

    def _do_layers_popup(self) -> None:
        if self._layers_popup is None:
            self._layers_popup = Tk()
            self._layers_popup.title("Layers")

            self._layers_popup.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

            def do_exit() -> None:
                if self._layers_popup is not None:
                    self._layers_popup.destroy()
                    self._layers_popup = None

            LayersFrame(self._layers_popup).pack()

            self._layers_popup.protocol("WM_DELETE_WINDOW", do_exit)

        else:
            self._layers_popup.focus_force()
            self._layers_popup.lift()

    def _do_connection_info_popup(self) -> None:
        if self.mcviewer.connection is None:
            ...
        else:
            ...
