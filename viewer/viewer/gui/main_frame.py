#!/usr/bin/env python3

import math
import threading
import time
from typing import Tuple

import cv2
import numpy as np

from tkinter import *

from PIL import ImageTk, Image

from viewer.config import Config
from viewer.gui.accounts_frame import AccountsFrame
from viewer.gui.main_menu import MainMenu
from viewer.gui.renderer import Renderer
from viewer.gui.selection_frame import SelectionFrame
from viewer.network.types import Dimension
from viewer.queries import IsLoadedQuery


class MainFrame(Frame):

    @property
    def exited(self) -> bool:
        return self._exited

    @property
    def scale(self) -> Tuple[float, float]:
        return self._current_scale[0], self._current_scale[1]

    @property
    def left_offset(self) -> Tuple[int, int]:
        return self._left_offset[0], self._left_offset[1]

    def __init__(self, master, mcviewer, *args, size: Tuple[int, int] = (1024, 768), **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, width=size[0], height=size[1],
                         **kwargs)

        self.mcviewer = mcviewer
        self.renderer = Renderer(self.mcviewer, self)

        self._error_popup = None

        self.current_dimension = Dimension.OVERWORLD

        self._exited = False

        self._data = {
            Dimension.OVERWORLD: {},
            Dimension.NETHER: {},
            Dimension.END: {},
        }

        self.size = size
        self._current_scale = [1, 1]
        self._left_offset = [self.size[0] // 2 // Config.CHUNK_SIZE[0], self.size[1] // 2 // Config.CHUNK_SIZE[1]]

        self.mouse_position = (0, 0)
        self.grabbed_position = (0, 0)
        self.last_grabbed_time = time.time()
        self.mouse_grabbed = False

        self._last_update = time.time()

        self.selected_chunks = []

        for x in range(-10, 10):
            for y in range(-10, 10):
                self.selected_chunks.append((x, y))

        self.image_label = Label(self, borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        self.image_label.grid(row=0, column=0, rowspan=3, columnspan=3, padx=2, pady=2)

        self._menu = MainMenu(self, self.mcviewer)
        self.master.config(menu=self._menu)

        self._selection = SelectionFrame(self, self.mcviewer)
        self._selection.grid(row=0, column=3, pady=3, sticky=NW)

        self._accounts = AccountsFrame(self, self.mcviewer)
        self._accounts.grid(row=1, column=3, sticky=NW)

        self._info = Frame(self, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        self._info.grid(row=4, column=0, columnspan=2, sticky=NW)

        self._coords_label = Label(self._info, text="Normal/chunk: (0, 0) / (0, 0)",
                                   bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, font=Config.FONT)
        self._dimension = Button(self._info, text="Dim: %s." %
                                                  Dimension.name_from_value(self.current_dimension),
                                 bg="#%02x%02x%02x" % Config.WIDGET_COLOUR,
                                 font=Config.FONT, command=self.change_dimension, borderwidth=Config.BORDER_WIDTH,
                                 relief=Config.RELIEF)

        self._coords_label.grid(row=0, column=0, padx=2, sticky=NW)
        self._dimension.grid(row=1, column=0, padx=3, pady=3, sticky=NW)

        self.update_image(self.convert_to_photoimage(np.zeros((self.size[1], self.size[0], 3), np.uint8)))

        self.pack()

        self.query_selected()

        # Setup events
        self.image_label.bind("<Button>", self.on_click)
        self.image_label.bind("<ButtonRelease>", self.on_release)
        self.image_label.bind("<Motion>", self.on_motion)

        self.master.after(10, self.on_update)
        self.master.protocol("WM_DELETE_WINDOW", self.on_exit)

    # ------------------------------ Utility methods ------------------------------ #

    @staticmethod
    def convert_to_photoimage(array: np.ndarray) -> PhotoImage:
        # VVV slower :/
        # return PhotoImage(master=self.screen,data=cv2.imencode(".png",array)[1].tobytes())

        image = cv2.cvtColor(array, cv2.COLOR_BGR2RGB)
        image = Image.fromarray(image)
        return ImageTk.PhotoImage(image)

    @staticmethod
    def scaled_line_width(scale_x: float, scale_y: float, factor: float = 1) -> int:
        line_width = (scale_x + scale_y) / (2 + factor)
        return max(1, round(line_width))

    # ------------------------------ Transform methods ------------------------------ #

    def _scale_and_pan(self, center_x: float, center_y: float, scale_x: float, scale_y: float) -> None:
        # Center to the position of the cursor
        self._left_offset[0] -= center_x / Config.CHUNK_SIZE[0] / self._current_scale[0]
        self._left_offset[1] -= center_y / Config.CHUNK_SIZE[1] / self._current_scale[1]

        # Add new scale
        self._current_scale[0] += scale_x
        self._current_scale[1] += scale_y

        # Readjust to normal position, with the new scale
        self._left_offset[0] += center_x / Config.CHUNK_SIZE[0] / self._current_scale[0]
        self._left_offset[1] += center_y / Config.CHUNK_SIZE[1] / self._current_scale[1]

    # ------------------------------ Updating ------------------------------ #

    def update_mouse_select(self):
        if self.mouse_grabbed and time.time() - self.last_grabbed_time >= .5:
            selected_chunk = (int(math.floor(self.mouse_position[0] / Config.CHUNK_SIZE[0] / self._current_scale[0] -
                                             self._left_offset[0])),
                              int(math.floor(self.mouse_position[1] / Config.CHUNK_SIZE[1] / self._current_scale[1] -
                                             self._left_offset[1])))
            if not selected_chunk in self.selected_chunks:
                self.selected_chunks.append(selected_chunk)

    def update_image(self, image: PhotoImage) -> None:
        self.image_label.image = image
        self.image_label.config(image=image)

    # ------------------------------ Other ------------------------------ #

    def _do_error_popup(self, message: str, error: str) -> None:
        if self._error_popup is None:
            self._error_popup = Tk()
            self._error_popup.title("Error")

            self._error_popup.config(bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)

            def do_exit() -> None:
                if self._error_popup is not None:
                    self._error_popup.destroy()
                    self._error_popup = None

            Label(self._error_popup, text=message, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR,
                  font=Config.LARGE_FONT).pack()
            Label(self._error_popup, text=error, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, fg="red",
                  font=Config.FONT).pack()

            Button(self._error_popup, text="Ok.", bg="#%02x%02x%02x" % Config.WIDGET_COLOUR, font=Config.FONT,
                   command=do_exit, borderwidth=Config.BORDER_WIDTH,
                   relief=Config.RELIEF).pack(pady=3)

            self._error_popup.protocol("WM_DELETE_WINDOW", do_exit)

        else:
            self._error_popup.destroy()
            self._error_popup = None
            self.do_error_popup(message, error)

    def do_error_popup(self, message: str, error: str) -> None:
        self.after(10, lambda: self._do_error_popup(message, error))

    def change_dimension(self) -> None:
        self.current_dimension += 1
        if self.current_dimension > Dimension.END:
            self.current_dimension = Dimension.NETHER

    def query_selected(self) -> None:
        curr_dimension_data = self._data[self.current_dimension]
        curr_time = time.time()
        for chunk in self.selected_chunks:
            curr_dimension_data[chunk] = (MainFrame.QueryState.WAITING, curr_time)
        self.selected_chunks.clear()

    def clear_selected(self) -> None:
        curr_dimension_data = self._data[self.current_dimension]
        for chunk in self.selected_chunks:
            if chunk in curr_dimension_data:
                del(curr_dimension_data[chunk])
            if len(self.selected_chunks) > 5000:
                time.sleep(0.0001)
        self.selected_chunks.clear()

    # ------------------------------ Events ------------------------------ #

    # noinspection PyUnresolvedReferences
    def on_click(self, event: Event) -> None:
        if event.num == 4:
            self._scale_and_pan(event.x, event.y,
                                self._current_scale[0] / Config.SCALE_SENSITIVITY,
                                self._current_scale[1] / Config.SCALE_SENSITIVITY)
        elif event.num == 5:
            self._scale_and_pan(event.x, event.y,
                                -(self._current_scale[0] / Config.SCALE_SENSITIVITY),
                                -(self._current_scale[1] / Config.SCALE_SENSITIVITY))

        elif event.num == 1:
            self.grabbed_position = (event.x, event.y)
            self.last_grabbed_time = time.time()
            self.mouse_grabbed = True
        else:
            print(event)

    # noinspection PyUnresolvedReferences
    def on_release(self, event: Event) -> None:
        if event.num == 1:
            self.mouse_grabbed = False

    # noinspection PyUnresolvedReferences
    def on_motion(self, event: Event) -> None:
        self.update_mouse_select()
        if self.mouse_grabbed and time.time() - self.last_grabbed_time < .5:
            self.last_grabbed_time = time.time()
            self._left_offset[0] += (event.x - self.mouse_position[0]) / Config.CHUNK_SIZE[0] / self._current_scale[0]
            self._left_offset[1] += (event.y - self.mouse_position[1]) / Config.CHUNK_SIZE[1] / self._current_scale[1]
        self.mouse_position = (event.x, event.y)

    def on_exit(self) -> None:
        self._exited = True
        self.mcviewer.on_exit()
        self.destroy()
        self.master.destroy()

    def on_update(self) -> None:
        # print(1 / (time.time() - self._last_update))
        self._last_update = time.time()

        self._accounts.on_update()
        self._selection.on_update()

        cursor_coords = (self.mouse_position[0] / Config.CHUNK_SIZE[0] / self._current_scale[0] - self._left_offset[0],
                         self.mouse_position[1] / Config.CHUNK_SIZE[1] / self._current_scale[1] - self._left_offset[1])
        self._coords_label.config(text="Normal/chunk: (%i, %i) / (%i, %i)" %
                                       (cursor_coords[0] * 16, cursor_coords[1] * 16,
                                        math.floor(cursor_coords[0]), math.floor(cursor_coords[1])))
        self._dimension.config(text="Dim: %s." % Dimension.name_from_value(self.current_dimension))

        """
        print(self._left_offset)

        print((self.mouse_position[0] - self._left_offset[0] * Config.CHUNK_SIZE[0]) // self._current_scale[0] // Config.CHUNK_SIZE[0],
              (self.mouse_position[1] - self._left_offset[1] * Config.CHUNK_SIZE[1]) // self._current_scale[1] // Config.CHUNK_SIZE[1])
        """

        curr_dimension_data = self._data[self.current_dimension].copy()

        latest_result = None
        while latest_result is not None:
            if isinstance(latest_result, IsLoadedQuery):
                for chunk in self._data[latest_result.dimension].copy():
                    if chunk == (latest_result.position.x // 16, latest_result.position.z // 16):
                        self._data[latest_result.dimension][chunk] = (latest_result.result, time.time())
                        break

        # print(self._current_scale)

        self.update_mouse_select()

        self.update_image(self.convert_to_photoimage(self.renderer.render()))

        self.master.after(max(10, 10 - int(time.time() - self._last_update) * 1000), self.on_update)

    class QueryState:
        WAITING = 0
        LOADED = 1
        UNLOADED = 2
