#!/usr/bin/env python3

import math
import time
from tkinter import *
from typing import Tuple

import bresenham
import cv2
import numpy as np
from PIL import ImageTk, Image

from pclient.networking.types import Enum
from viewer.config import Config
from viewer.gui.frames.info_frame import InfoFrame
from viewer.gui.frames.main_menu import MainMenu
from viewer.gui.popups.error_popup import ErrorPopup
from viewer.gui.popups.goto_popup import GotoPopup
from viewer.gui.popups.reporter_popup import ReporterPopup
from viewer.gui.renderer import Renderer
from viewer.gui.frames.selection_frame import SelectionFrame
from viewer.gui.frames.tasks_frame import TasksFrame
from viewer.gui.widgets import ConfigButton, ConfigLabel
from viewer.util import ActiveTask, ChunkPosition, Dimension, Priority


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

    def __init__(self, master, viewer, *args, size: Tuple[int, int] = Config.VIEWER_SIZE, **kwargs) -> None:
        super().__init__(master, *args, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR, width=size[0], height=size[1],
                         **kwargs)

        self.viewer = viewer
        self.renderer = Renderer(self.viewer, self)

        self.current_dimension = Dimension.OVERWORLD

        self._exited = False

        self.size = size
        self._current_scale = [1, 1]
        self._left_offset = [self.size[0] // 2 // Config.CHUNK_SIZE[0], self.size[1] // 2 // Config.CHUNK_SIZE[1]]

        self.mouse_position = (0, 0)
        self.grabbed_position = (0, 0)
        self.last_grabbed_time = time.time()
        self.mouse_grabbed = False
        self.selection_mode = MainFrame.SelectionMode.LINE

        self._last_reporter = -1

        self._last_update = time.time()

        self.selected_chunks = []

        # for x in range(-10, 10):
        #     for y in range(-10, 10):
        #         self.selected_chunks.append(ChunkPosition(x, y))

        self.image_label = Label(self, borderwidth=Config.BORDER_WIDTH, relief=Config.RELIEF)
        self.image_label.grid(row=0, column=0, rowspan=5, columnspan=3, padx=(3, 0), pady=(3, 0))

        self._menu = MainMenu(self, self, self.viewer)
        self.master.config(menu=self._menu)

        self._reporter_button = ConfigButton(self, text="Reporter: None", command=self.do_reporter_popup)
        self._reporter_button.grid(row=0, column=3, padx=3, pady=(3, 0), sticky=NW)

        self._reporter_info = InfoFrame(self, self.viewer)
        self._reporter_info.grid(row=1, column=3, padx=3, sticky=NW)

        self._selection = SelectionFrame(self, self.viewer)
        self._selection.grid(row=2, column=3, padx=3, sticky=NW)

        self._tasks = TasksFrame(self, self, self.viewer)
        self._tasks.grid(row=3, column=3, padx=3, sticky=NW)

        self._pos_info = Frame(self, bg="#%02x%02x%02x" % Config.WINDOW_COLOUR)
        self._pos_info.grid(row=5, column=0, columnspan=2, sticky=NW)

        self._coords = ConfigButton(self._pos_info, text="Normal/chunk: (0, 0) / (0, 0)", command=self.do_goto_popup)
        self._dimension = ConfigButton(self._pos_info, text="Dim: %s." % Dimension.name_from_value(self.current_dimension),
                                       command=self.change_dimension)

        self._coords.grid(row=0, column=0, padx=3, pady=(3, 0), sticky=NW)
        self._dimension.grid(row=1, column=0, padx=3, pady=(0, 3), sticky=NW)

        self.update_image(self.convert_to_photoimage(np.zeros((self.size[1], self.size[0], 3), np.uint8)))

        self.pack()

        # self.query_selected()

        # Setup events
        self.image_label.bind("<Button>", self.on_click)
        self.image_label.bind("<MouseWheel>", self.on_click)
        self.image_label.bind("<ButtonRelease>", self.on_release)
        self.image_label.bind("<Motion>", self.on_motion)

        self.master.after(10, self.on_update)
        self.master.protocol("WM_DELETE_WINDOW", self.destroy)

    def destroy(self) -> None:
        self._exited = True
        self.viewer.on_exit()

    # ------------------------------ Utility methods ------------------------------ #

    @staticmethod
    def convert_to_photoimage(array: np.ndarray) -> PhotoImage:
        # VVV slower :/
        # return PhotoImage(master=self.screen, data=cv2.imencode(".png",array)[1].tobytes())

        image = cv2.cvtColor(array, cv2.COLOR_BGR2RGB)
        image = Image.fromarray(image)
        return ImageTk.PhotoImage(image)

    @staticmethod
    def scaled_line_width(scale_x: float, scale_y: float, factor: float = 1) -> int:
        line_width = (scale_x + scale_y) / (2 + factor)
        return max(1, round(line_width))

    def screen_to_chunk(self, x: int, y: int) -> Tuple[float, float]:
        return x / Config.CHUNK_SIZE[0] / self._current_scale[0] - self._left_offset[0], \
               y / Config.CHUNK_SIZE[1] / self._current_scale[1] - self._left_offset[1]

    # ------------------------------ Transform methods ------------------------------ #

    def scale_and_pan(self, center_x: float, center_y: float, scale_x: float, scale_y: float) -> None:
        # Center to the position of the cursor
        self._left_offset[0] -= center_x / Config.CHUNK_SIZE[0] / self._current_scale[0]
        self._left_offset[1] -= center_y / Config.CHUNK_SIZE[1] / self._current_scale[1]

        # noinspection PyTypeChecker
        self._current_scale[0] = scale_x
        # noinspection PyTypeChecker
        self._current_scale[1] = scale_y

        # Readjust to normal position, with the new scale
        self._left_offset[0] += center_x / Config.CHUNK_SIZE[0] / self._current_scale[0]
        self._left_offset[1] += center_y / Config.CHUNK_SIZE[1] / self._current_scale[1]

    # ------------------------------ Updating ------------------------------ #

    def update_mouse_select(self, new_x: int, new_y: int):
        if self.mouse_grabbed and time.time() - self.last_grabbed_time >= .5:
            selected_chunks = []

            grabbed_pos = self.screen_to_chunk(*self.grabbed_position)
            grabbed_pos = (int(math.floor(grabbed_pos[0])), int(math.floor(grabbed_pos[1])))
            old_pos = self.screen_to_chunk(*self.mouse_position)
            old_pos = (int(math.floor(old_pos[0])), int(math.floor(old_pos[1])))
            new_pos = self.screen_to_chunk(new_x, new_y)
            new_pos = (int(math.floor(new_pos[0])), int(math.floor(new_pos[1])))

            if self.selection_mode == MainFrame.SelectionMode.PENCIL:
                selected_chunks.append(old_pos)

            elif self.selection_mode == MainFrame.SelectionMode.LINE:
                selected_chunks.extend(bresenham.bresenham(*old_pos, *new_pos))

            elif self.selection_mode == MainFrame.SelectionMode.RECT:
                start_pos = (min(grabbed_pos[0], new_pos[0]), min(grabbed_pos[1], new_pos[1]))
                end_pos = (max(grabbed_pos[0], new_pos[0]), max(grabbed_pos[1], new_pos[1]))

                for x in range(end_pos[0] - start_pos[0] + 1):
                    for y in range(end_pos[1] - start_pos[1] + 1):
                        selected_chunks.append((start_pos[0] + x, start_pos[1] + y))

            for selected_chunk in selected_chunks:
                if not selected_chunk in self.selected_chunks:
                    self.selected_chunks.append(ChunkPosition(*selected_chunk))

    def update_image(self, image: PhotoImage) -> None:
        self.image_label.image = image
        self.image_label.config(image=image)

    # ------------------------------ Other ------------------------------ #

    def do_error_popup(self, message: str, error: str) -> None:
        self.after(10, lambda: ErrorPopup(message, error))

    def do_reporter_popup(self) -> None:
        self.after(10, lambda: ReporterPopup(self.viewer))

    def do_goto_popup(self) -> None:
        self.after(10, lambda: GotoPopup(self))

    def change_dimension(self) -> None:
        new_dimension = self.current_dimension + 1
        if new_dimension > Dimension.END:
            new_dimension = Dimension.NETHER

        if self.current_dimension == Dimension.NETHER and new_dimension != Dimension.NETHER:
            self._left_offset[0] -= self.size[0] / 2 / Config.CHUNK_SIZE[0] / self._current_scale[0]
            self._left_offset[1] -= self.size[1] / 2 / Config.CHUNK_SIZE[1] / self._current_scale[1]

            self._left_offset[0] *= 8
            self._left_offset[1] *= 8

            self._left_offset[0] += self.size[0] / 2 / Config.CHUNK_SIZE[0] / self._current_scale[0]
            self._left_offset[1] += self.size[1] / 2 / Config.CHUNK_SIZE[1] / self._current_scale[1]

        elif new_dimension == Dimension.NETHER and self.current_dimension != Dimension.NETHER:
            self._left_offset[0] -= self.size[0] / 2 / Config.CHUNK_SIZE[0] / self._current_scale[0]
            self._left_offset[1] -= self.size[1] / 2 / Config.CHUNK_SIZE[1] / self._current_scale[1]

            self._left_offset[0] /= 8
            self._left_offset[1] /= 8

            self._left_offset[0] += self.size[0] / 2 / Config.CHUNK_SIZE[0] / self._current_scale[0]
            self._left_offset[1] += self.size[1] / 2 / Config.CHUNK_SIZE[1] / self._current_scale[1]

        self.current_dimension = new_dimension

    def goto(self, x: int, z: int) -> None:
        self._left_offset = [
            -(x - self.size[0] / 2 / Config.CHUNK_SIZE[0] / self._current_scale[0]),
            -(z - self.size[1] / 2 / Config.CHUNK_SIZE[1] / self._current_scale[1]),
        ]

    def query_selected(self) -> None:
        if self.viewer.current_reporter == -1:
            self.do_error_popup("Couldn't query chunks:", "No current reporter.")
            return

        reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)

        try:
            task = reporter.get_registered_task("static_scan")
        except LookupError:
            self.do_error_popup("Couldn't query chunks:", "Static scan not supported.")
            return

        parameters = [
            ActiveTask.Parameter(task.get_param_description("positions"), *self.selected_chunks),
            ActiveTask.Parameter(task.get_param_description("dimension"), Dimension.value_to_mc(self.current_dimension)),
            ActiveTask.Parameter(task.get_param_description("priority"), Priority.USER),
        ]

        # for chunk_position in self.selected_chunks:
        #     if not (chunk_position, self.current_dimension) in self.waiting_chunks:
        #         self.waiting_chunks.append((chunk_position, self.current_dimension))

        self.viewer.start_task(task, parameters)
        self.selected_chunks.clear()

    def query_highways(self, max_distance: int, min_distance: int, chunk_skip: int) -> None:
        if self.viewer.current_reporter == -1:
            self.do_error_popup("Couldn't query highways:", "No current reporter.")
            return

        reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)

        try:
            task = reporter.get_registered_task("highway_scan")
        except LookupError:
            self.do_error_popup("Couldn't query highways:", "Highway scan not supported.")
            return

        parameters = [
            ActiveTask.Parameter(task.get_param_description("maxDistance"), max_distance),
            ActiveTask.Parameter(task.get_param_description("minDistance"), min_distance),
            ActiveTask.Parameter(task.get_param_description("chunkSkip"), chunk_skip),
            ActiveTask.Parameter(task.get_param_description("dimension"), self.current_dimension - 1),
            ActiveTask.Parameter(task.get_param_description("priority"), Priority.LOW),
        ]

        self.viewer.start_task(task, parameters)

    # ------------------------------ Events ------------------------------ #

    # noinspection PyUnresolvedReferences
    def on_click(self, event: Event) -> None:
        if event.num == 4 or event.delta == 120:
            self.scale_and_pan(event.x, event.y,
                               self._current_scale[0] + self._current_scale[0] / Config.SCALE_SENSITIVITY,
                               self._current_scale[1] + self._current_scale[1] / Config.SCALE_SENSITIVITY)
        elif event.num == 5 or event.delta == -120:
            self.scale_and_pan(event.x, event.y,
                               self._current_scale[0] - self._current_scale[0] / Config.SCALE_SENSITIVITY,
                               self._current_scale[1] - self._current_scale[1] / Config.SCALE_SENSITIVITY)

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
        self.update_mouse_select(event.x, event.y)
        if self.mouse_grabbed and time.time() - self.last_grabbed_time < .5:
            self.last_grabbed_time = time.time()
            self._left_offset[0] += (event.x - self.mouse_position[0]) / Config.CHUNK_SIZE[0] / self._current_scale[0]
            self._left_offset[1] += (event.y - self.mouse_position[1]) / Config.CHUNK_SIZE[1] / self._current_scale[1]
        self.mouse_position = (event.x, event.y)

    def on_update(self) -> None:
        # print(1 / (time.time() - self._last_update))
        self._last_update = time.time()

        current_reporter = None if self.viewer.current_reporter == -1 else \
            self.viewer.get_reporter(handler_id=self.viewer.current_reporter)

        if self.viewer.current_reporter != self._last_reporter:
            if current_reporter is None:
                self._reporter_button.config(text="Reporter: None")
            else:
                self._reporter_button.config(text="Reporter: %r (%i)" % (current_reporter.handler_name,
                                                                         current_reporter.handler_id))
        self._last_reporter = self.viewer.current_reporter

        self._menu.on_update()
        # self._accounts.on_update()
        self._selection.on_update()
        self._reporter_info.on_update()
        self._tasks.on_update()

        cursor_coords = self.screen_to_chunk(*self.mouse_position)
        self._coords.config(text="Normal/chunk: (%i, %i) / (%i, %i)" %
                                 (cursor_coords[0] * 16, cursor_coords[1] * 16,
                                        math.floor(cursor_coords[0]), math.floor(cursor_coords[1])))
        self._dimension.config(text="Dim: %s." % Dimension.name_from_value(self.current_dimension))

        """
        print(self._left_offset)

        print((self.mouse_position[0] - self._left_offset[0] * Config.CHUNK_SIZE[0]) // self._current_scale[0] // Config.CHUNK_SIZE[0],
              (self.mouse_position[1] - self._left_offset[1] * Config.CHUNK_SIZE[1]) // self._current_scale[1] // Config.CHUNK_SIZE[1])
        """

        self.update_image(self.convert_to_photoimage(self.renderer.render()))

        self.master.after(max(10, 10 - int(time.time() - self._last_update) * 1000), self.on_update)

    class SelectionMode(Enum):
        NONE = 0
        PENCIL = 1
        LINE = 2
        RECT = 3

    class QueryState(Enum):
        WAITING = 0
        LOADED = 1
        UNLOADED = 2
