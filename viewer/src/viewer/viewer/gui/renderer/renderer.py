#!/usr/bin/env python3

import math
from enum import Enum
from typing import Dict, Tuple, List

import bresenham
import cv2
import numpy as np
from PIL import ImageFont
from PyQt5.QtCore import Qt, QThread, pyqtSignal
from PyQt5.QtGui import QMouseEvent, QWheelEvent, QContextMenuEvent
from PyQt5.QtWidgets import QGraphicsView, QWidget, QApplication, QMenu, QMessageBox

from ..util import get_font_paths, draw_font
from ...config import Config
from ...util import Dimension, ChunkPosition, Priority


class Renderer(QGraphicsView):
    """
    Responsible for rendering the grid in the grid view tab.
    """

    @property
    def cursor_coords(self) -> Tuple[float, float]:
        return (self.mouse_position[0] / (Config.CHUNK_SIZE[0] * self.scale[0]) - self.offset[0],
                self.mouse_position[1] / (Config.CHUNK_SIZE[1] * self.scale[1]) - self.offset[1])

    @property
    def states(self) -> Dict[Dimension, Dict[Tuple[int, int], np.ndarray]]:
        """
        :return: The chunk states for all dimensions.
        """

        return self._states

    @property
    def nether_states(self) -> Dict[Tuple[int, int], np.ndarray]:
        """
        :return: The chunk states for the nether dimension.
        """

        if not Dimension.NETHER in self._states:
            self._states[Dimension.NETHER] = {}
        return self._states[Dimension.NETHER]

    @property
    def overworld_states(self) -> Dict[Tuple[int, int], np.ndarray]:
        """
        :return: The chunk states for the overworld dimension.
        """

        if not Dimension.OVERWORLD in self._states:
            self._states[Dimension.OVERWORLD] = {}
        return self._states[Dimension.OVERWORLD]

    @property
    def end_states(self) -> Dict[Tuple[int, int], np.ndarray]:
        """
        :return: The chunk states for the end dimension.
        """

        if not Dimension.END in self._states:
            self._states[Dimension.END] = {}
        return self._states[Dimension.END]

    def __init__(self, parent: QWidget, main_window) -> None:
        super().__init__(parent)

        self.main_window = main_window
        self.setMouseTracking(True)

        self._query_selected_thread = None

        unloadable, families, accounted = get_font_paths()
        if Config.FONT.family() in families:
            self._font = ImageFont.truetype(families[Config.FONT.family()], int(Config.FONT.pointSize() * 2.4))
        else:
            self._font = ImageFont.load_default()

        self.offset = (0, 0)
        self.scale = (1, 1)

        self.mouse_grabbed = False
        self.mouse_position = (-1, -1)
        self._clicked_position = (-1, -1)
        self._double_clicked_position = (-1, -1)

        self.selecting = False
        self._selection = []

        self._render_grid = True
        self._render_highways = True
        self._selection_mode = Renderer.Selection.LINE
        self._current_dimension = Dimension.NETHER
        self._states = {}

    # ----------------------------- Events ----------------------------- #

    # noinspection PyTypeChecker
    def contextMenuEvent(self, event: QContextMenuEvent) -> None:
        main_menu = QMenu()

        def _select() -> None:
            if self._selection_mode == Renderer.Selection.NONE:
                self._selection_mode = Renderer.Selection.LINE
            self.selecting = True

        # main_menu.addAction("Select", _select)
        main_menu.addAction("Query selected", self._query_selection)
        main_menu.addAction("Clear selected", self._selection.clear)

        main_menu.addSeparator()

        # noinspection PyProtectedMember
        main_menu.addAction("New task", lambda: self.main_window.tasks_tab._on_new_task(False))

        main_menu.addSeparator()

        main_menu.addAction("Copy coords", self._copy_coords)

        main_menu.addSeparator()

        toggle_grid_action = main_menu.addAction("Toggle grid")
        toggle_grid_action.setCheckable(True)
        toggle_grid_action.setChecked(self._render_grid)
        toggle_grid_action.triggered.connect(self.toggle_grid)

        toggle_highways_action = main_menu.addAction("Toggle highways")
        toggle_highways_action.setCheckable(True)
        toggle_highways_action.setChecked(self._render_highways)
        toggle_highways_action.triggered.connect(self.toggle_highways)

        main_menu.addSeparator()

        def _set_colour_map(colour_map: int) -> None:
            Config.COLOUR_MAP = colour_map

        colour_map_menu = main_menu.addMenu("Colour map")
        # noinspection PyProtectedMember
        for name, colour_map in Config._COLOUR_MAPS.items():
            colour_map_action = colour_map_menu.addAction(name)
            colour_map_action.setCheckable(True)
            colour_map_action.setChecked(colour_map == Config.COLOUR_MAP)
            colour_map_action.triggered.connect(lambda checked, colour_map_=colour_map: _set_colour_map(colour_map_))

        selection_menu = main_menu.addMenu("Selection")

        # We don't need to set these as exclusive cos they're mutually exclusive
        for value in Renderer.Selection.__members__.values():
            selection_action = selection_menu.addAction(value.name.capitalize())
            selection_action.setCheckable(True)
            selection_action.setChecked(value == self._selection_mode)
            selection_action.triggered.connect(lambda checked, value_=value: self.set_selection_mode(value_))
            selection_menu.addAction(selection_action)

        dimension_menu = main_menu.addMenu("Dimension")

        nether_action = dimension_menu.addAction("Nether", lambda: self.set_dimension(Dimension.NETHER))
        nether_action.setCheckable(True)
        nether_action.setChecked(self._current_dimension == Dimension.NETHER)

        overworld_action = dimension_menu.addAction("Overworld", lambda: self.set_dimension(Dimension.OVERWORLD))
        overworld_action.setCheckable(True)
        overworld_action.setChecked(self._current_dimension == Dimension.OVERWORLD)

        end_action = dimension_menu.addAction("End", lambda: self.set_dimension(Dimension.END))
        end_action.setCheckable(True)
        end_action.setChecked(self._current_dimension == Dimension.END)

        main_menu.addMenu(dimension_menu)

        main_menu.exec_(event.globalPos())

    def mouseMoveEvent(self, event: QMouseEvent) -> None:
        if self.mouse_grabbed:
            if self.selecting:
                coords = self.cursor_coords
                self.mouse_position = (event.x(), event.y())
                new_coords = self.cursor_coords

                if Config.INTERPOLATION_SIZE != (1, 1):
                    coords = (coords[0] // Config.INTERPOLATION_SIZE[0], coords[1] // Config.INTERPOLATION_SIZE[1])
                    new_coords = (new_coords[0] // Config.INTERPOLATION_SIZE[0], new_coords[1] // Config.INTERPOLATION_SIZE[1])

                if self._selection_mode == Renderer.Selection.LINE:
                    for point in bresenham.bresenham(math.ceil(coords[0]), math.ceil(coords[1]),
                                                     math.floor(new_coords[0]), math.floor(new_coords[1])):
                        if Config.INTERPOLATION_SIZE != (1, 1):
                            point = (point[0] * Config.INTERPOLATION_SIZE[0], point[1] * Config.INTERPOLATION_SIZE[1])

                        if not point in self._selection:
                            self._selection.append(point)

                elif self._selection_mode == Renderer.Selection.BOX:
                    self.mouse_position = (event.x(), event.y())
                    coords = self.cursor_coords
                    coords = (math.ceil(coords[0]), math.ceil(coords[1]))

                    if len(self._selection) == 1:
                        self._selection.append(coords)
                    else:
                        self._selection[1] = coords

            else:
                if self.mouse_position == (-1, -1):  # We haven't moved yet
                    ...
                else:
                    self.offset = (
                        self.offset[0] + (event.x() - self.mouse_position[0]) / (Config.CHUNK_SIZE[0] * self.scale[0]),
                        self.offset[1] + (event.y() - self.mouse_position[1]) / (Config.CHUNK_SIZE[1] * self.scale[1]),
                    )

        self.mouse_position = (event.x(), event.y())

    def mousePressEvent(self, event: QMouseEvent) -> None:
        if event.button() == Qt.LeftButton:
            if not self.selecting:
                QApplication.setOverrideCursor(Qt.DragMoveCursor)
            self.mouse_grabbed = True

    def mouseReleaseEvent(self, event: QMouseEvent) -> None:
        if event.button() == Qt.LeftButton:
            QApplication.restoreOverrideCursor()
            self.mouse_grabbed = False
            self.selecting = False
            # self.mouse_position = (-1, -1)

    def mouseDoubleClickEvent(self, event: QMouseEvent) -> None:
        if self._selection_mode != Renderer.Selection.NONE:
            self.mouse_grabbed = True
            self.selecting = True

            if self._selection_mode == Renderer.Selection.BOX:
                self._selection.clear()
                self._selection.append((math.floor(event.x() / (Config.CHUNK_SIZE[0] * self.scale[0]) - self.offset[0]),
                                        math.floor(event.y() / (Config.CHUNK_SIZE[1] * self.scale[1]) - self.offset[1])))

    def wheelEvent(self, event: QWheelEvent) -> None:
        size = (self.size().width(), self.size().height())
        self.offset = (  # Pan to the mouse position
            self.offset[0] - event.x() / (Config.CHUNK_SIZE[0] * self.scale[0]),
            self.offset[1] - event.y() / (Config.CHUNK_SIZE[1] * self.scale[1]),
        )

        self.scale = (
            self.scale[0] * math.exp(event.angleDelta().y() / Config.SCALE_SENSITIVITY),
            self.scale[1] * math.exp(event.angleDelta().y() / Config.SCALE_SENSITIVITY),
        )

        self.offset = (  # Pan back
            self.offset[0] + event.x() / (Config.CHUNK_SIZE[0] * self.scale[0]),
            self.offset[1] + event.y() / (Config.CHUNK_SIZE[1] * self.scale[1]),
        )

    # ----------------------------- Render stuff ----------------------------- #

    def _scaled_line_width(self, factor: float = 1) -> int:
        line_width = (self.scale[0] + self.scale[1]) / (2 + factor)
        return max(1, round(line_width))

    def _render(self) -> np.ndarray:
        if self.main_window.viewer is not None:
            current_reporter = self.main_window.viewer.current_reporter
        else:
            current_reporter = None

        if self._current_dimension in self._states:
            dim_data = self._states[self._current_dimension]
        else:
            dim_data = {}

        size = (self.size().width() - 10, self.size().height() - 10)
        if size[0] <= 0 or size[1] <= 0:
            return None
        image = np.zeros((size[1], size[0], 3), dtype=np.uint8)
        image[:, :] = Config.BASE_COLOUR[:3]

        # The size of the image in chunks
        chunks_size = (size[0] / (Config.CHUNK_SIZE[0] * self.scale[0]), size[1] / (Config.CHUNK_SIZE[1] * self.scale[1]))
        # The size of a chunk on the image, yeah I know, terrible naming
        chunk_size = (Config.CHUNK_SIZE[0] * self.scale[0], Config.CHUNK_SIZE[1] * self.scale[1])

        x_range = range(math.floor(-self.offset[0] / Config.REGION_SIZE[0]),
                        math.ceil((-self.offset[0] + chunks_size[0]) / Config.REGION_SIZE[0]))
        z_range = range(math.floor(-self.offset[1] / Config.REGION_SIZE[1]),
                        math.ceil((-self.offset[1] + chunks_size[1]) / Config.REGION_SIZE[1]))

        # logging.debug(str(x_range) + ", " + str(z_range))

        # Find regions that we can blit onto the image
        for x in x_range:
            for z in z_range:
                real_region_coords = [x * Config.REGION_SIZE[0], z * Config.REGION_SIZE[1]]

                if (x, z) in dim_data:
                    region = dim_data[x, z]

                    # Ok so this is a weird place to put interpolation, since you may think it's faster to interpolate
                    # after we've cropped the region. There's a major issue preventing me from doing that, and I can't
                    # be bothered to solve it. A rundown of the issue is that we crop the region based on the
                    # interpolation size, but we have to floor divide it, so multiplying it back to the regular size
                    # loses precision, which makes the region jump around on screen. So damn, RIP optimisation.
                    if Config.INTERPOLATION_SIZE != (1, 1):
                        region = cv2.resize(region, (region.shape[1] * Config.INTERPOLATION_SIZE[0],
                                                     region.shape[0] * Config.INTERPOLATION_SIZE[1]),
                                            interpolation=Config.INTERPOLATION_TYPE)

                    # We don't want to resize a massive image if we're really zoomed in, so crop the region if needed
                    if real_region_coords[0] < -self.offset[0]:  # Crop X mins
                        x_crop = math.floor(-self.offset[0] - real_region_coords[0])
                        region = region[:, x_crop:]
                        real_region_coords[0] += x_crop
                    if real_region_coords[0] + region.shape[1] > chunks_size[0] - self.offset[0]:  # Crop X maxes
                        x_crop = math.ceil(chunks_size[0] - real_region_coords[0] - self.offset[0])
                        region = region[:, :x_crop]

                    if real_region_coords[1] < -self.offset[1]:  # Crop Z mins
                        z_crop = math.floor(-self.offset[1] - real_region_coords[1])
                        region = region[z_crop:, :]
                        real_region_coords[1] += z_crop
                    if real_region_coords[1] + region.shape[0] > chunks_size[1] - self.offset[1]:  # Crop Z maxes
                        z_crop = math.ceil(chunks_size[1] - real_region_coords[1] - self.offset[1])
                        region = region[:z_crop, :]

                    if not region.shape[0] or not region.shape[1]:  # Skip off screen regions
                        continue

                    # Resize the region to fit the image
                    region = cv2.resize(region, (math.ceil(region.shape[1] * chunk_size[0]),
                                                 math.ceil(region.shape[0] * chunk_size[1])),
                                        interpolation=cv2.INTER_NEAREST)

                    # Adjust coords to real image coordinates
                    blit_coords = [
                        math.floor((real_region_coords[0] + self.offset[0]) * chunk_size[0]),
                        math.floor((real_region_coords[1] + self.offset[1]) * chunk_size[1]),
                        math.floor((real_region_coords[0] + self.offset[0]) * chunk_size[0] + region.shape[1]),
                        math.floor((real_region_coords[1] + self.offset[1]) * chunk_size[1] + region.shape[0]),
                    ]

                    # Fit the region into the image
                    if blit_coords[0] < 0:
                        region = region[:, -blit_coords[0]:]
                        blit_coords[0] = 0
                    if blit_coords[1] < 0:
                        region = region[-blit_coords[1]:, :]
                        blit_coords[1] = 0
                    if blit_coords[2] > size[0]:
                        region = region[:, :size[0] - blit_coords[2]]
                        blit_coords[2] = size[0]
                    if blit_coords[3] > size[1]:
                        region = region[:size[1] - blit_coords[3], :]
                        blit_coords[3] = size[1]

                    if not region.shape[0] or not region.shape[1] or blit_coords[2] < 0 or blit_coords[3] < 0:
                        continue

                    if Config.COLOUR_MAP is None:
                        region_coloured = cv2.cvtColor(region, cv2.COLOR_GRAY2BGR)
                    else:
                        region_coloured = cv2.applyColorMap(region, Config.COLOUR_MAP)

                    region = cv2.cvtColor(region, cv2.COLOR_GRAY2BGR)  # Do this so we can use np.where, RIP optimisation ;-;
                    current = image[blit_coords[1]: blit_coords[3], blit_coords[0]: blit_coords[2]]
                    # Blit the region onto the image
                    image[blit_coords[1]: blit_coords[3], blit_coords[0]: blit_coords[2]] = np.where(region > 0,
                                                                                                     region_coloured,
                                                                                                     current)

        if self._render_grid:
            if self.scale[0] > Config.GRID_SCALE[0]:  # Draw the X lines on for the grid
                for x in range(int(chunks_size[0]) + 1):
                    actual_x = (x + (self.offset[0] % 1)) * chunk_size[0]
                    image = cv2.line(image, (int(actual_x), 0), (int(actual_x), size[1]), Config.DARK_COLOUR[:3], 1)

            if self.scale[1] > Config.GRID_SCALE[1]:  # Draw the Z lines on for the grid
                for z in range(int(chunks_size[1]) + 1):
                    actual_z = (z + (self.offset[1] % 1)) * chunk_size[1]
                    image = cv2.line(image, (0, int(actual_z)), (size[0], int(actual_z)), Config.DARK_COLOUR[:3], 1)

        # Draw the highways
        if self._render_highways:
            image = cv2.line(image, (math.floor((self.offset[0] + .5) * chunk_size[0]), 0),
                             (math.floor((self.offset[0] + .5) * chunk_size[0]), image.shape[0]),
                             Config.HIGHWAY_COLOUR, self._scaled_line_width(-.75), cv2.LINE_AA)
            image = cv2.line(image, (0, math.floor((self.offset[1] + .5) * chunk_size[1])),
                             (image.shape[1], math.floor((self.offset[1] + .5) * chunk_size[1])),
                             Config.HIGHWAY_COLOUR, self._scaled_line_width(-.75), cv2.LINE_AA)

        if self._selection_mode == Renderer.Selection.LINE:
            for chunk_x, chunk_z in self._selection:
                # Is the chunk on screen?
                if -self.offset[0] - Config.INTERPOLATION_SIZE[0] <= chunk_x <= chunks_size[0] - self.offset[0] and \
                        -self.offset[1] - Config.INTERPOLATION_SIZE[1] <= chunk_z <= chunks_size[1] - self.offset[1]:
                    chunk_coords = (
                        math.floor((chunk_x + self.offset[0]) * chunk_size[0]),
                        math.floor((chunk_z + self.offset[1]) * chunk_size[1]),
                    )
                    image = cv2.rectangle(image, chunk_coords,
                                          (math.floor(chunk_coords[0] + chunk_size[0] * Config.INTERPOLATION_SIZE[0]),
                                           math.floor(chunk_coords[1] + chunk_size[1] * Config.INTERPOLATION_SIZE[1])),
                                          Config.SELECTION_COLOUR, 2)

        elif self._selection_mode == Renderer.Selection.BOX:
            if len(self._selection) > 1:
                bounds = (
                    math.floor((self._selection[0][0] + self.offset[0]) * chunk_size[0]),
                    math.floor((self._selection[0][1] + self.offset[1]) * chunk_size[1]),
                    math.floor((self._selection[1][0] + self.offset[0]) * chunk_size[0]),
                    math.floor((self._selection[1][1] + self.offset[1]) * chunk_size[1]),
                )
                image = cv2.rectangle(image, bounds[:2], bounds[2:], Config.SELECTION_COLOUR, 2)

        if current_reporter is not None:
            for player in current_reporter.get_players():
                if player.dimension == Dimension.value_to_mc(self._current_dimension):
                    adjusted_coords = (int(((player.position.x / 16 + self.offset[0]) * chunk_size[0])),
                                       int(((player.position.z / 16 + self.offset[1]) * chunk_size[1])))
                    image = cv2.circle(image, adjusted_coords, round(self.scale[0] * 2), Config.PLAYER_COLOUR, -1, cv2.LINE_AA)
                    image = cv2.putText(image, player.display_name, adjusted_coords, cv2.FONT_HERSHEY_DUPLEX,
                                        self.scale[0] / 3, (255, 255, 255), round(self.scale[0] / 2), cv2.LINE_AA)

        # Draw the coords label onto the image
        coords = (round(self.cursor_coords[0] * 16), round(self.cursor_coords[1] * 16))
        text = "Coords: %i, %i" % coords
        image = draw_font(image, self._font, text, (0, image.shape[0] - self._font.getsize(text)[1]), Config.TEXT_COLOUR)

        # image = cv2.putText(image, "Coords: %i, %i" % coords, (0, image.shape[0] - 4), cv2.FONT_HERSHEY_DUPLEX, .75,
        #                     Config.TEXT_COLOUR[:3], 1, cv2.LINE_AA)
        # cv2.flip(image, 0, image)
        return image

    # ----------------------------- Other methods ----------------------------- #

    def _query_selection(self) -> None:
        if self._selection_mode == Renderer.Selection.NONE:
            QMessageBox.warning(self, "No selection", "Selection mode is currently set to \"None\".")
            return
        if self.main_window.viewer is None or self.main_window.viewer.current_reporter is None:
            QMessageBox.warning(self, "No reporter", "No reporter is currently selected.")
            return
        if not self._selection:
            QMessageBox.warning(self, "No selection", "No selection has been made.")
            return

        if self._query_selected_thread is not None and self._query_selected_thread.isRunning():
            return

        self._query_selected_thread = Renderer.QuerySelectedThread(self.main_window, self._current_dimension,
                                                                   self._selection, self._selection_mode)
        self._query_selected_thread.error_emitter.connect(lambda error: QMessageBox.warning(self, "Error", error))
        self._query_selected_thread.start()

    def _copy_coords(self) -> None:
        coords = self.cursor_coords
        QApplication.clipboard().setText("X: %i, Z: %i, dim: %s" % (coords[0] * 16, coords[1] * 16,
                                                                    Dimension.name_from_value(self._current_dimension)))

    # ----------------------------- Setters and getters ----------------------------- #

    def toggle_grid(self) -> bool:
        """
        Toggles whether or not to render the grid when you zoom in.
        :return: The new render state.
        """

        self._render_grid = not self._render_grid
        return self._render_grid

    def toggle_highways(self) -> bool:
        """
        Toggles whether or not to render the highways when you zoom in.
        :return: The new render state.
        """

        self._render_highways = not self._render_highways
        return self._render_highways

    def set_selection_mode(self, selection) -> None:
        if selection != self._selection_mode:
            self._selection.clear()
            self._selection_mode = selection

    def set_dimension(self, dimension: Dimension) -> None:
        self._current_dimension = dimension

    # ----------------------------- Classes ----------------------------- #

    class Selection(Enum):
        """
        The selection mode.
        """

        NONE = 0
        LINE = 1
        BOX = 2

    class QuerySelectedThread(QThread):

        error_emitter = pyqtSignal(str)

        def __init__(self, main_window, dimension: Dimension, selection: List[Tuple[int, int]], selection_mode) -> None:
            super().__init__()

            self.main_window = main_window
            self.dimension = dimension
            self.selection = selection
            self.selection_mode = selection_mode

        def run(self) -> None:
            try:
                if self.selection_mode == Renderer.Selection.LINE:
                    positions = []
                    for x, z in self.selection:
                        positions.append(ChunkPosition(x, z))

                    self.main_window.viewer.start_task("static_scan",
                                                       positions=positions,
                                                       dimension=Dimension.value_to_mc(self.dimension),
                                                       priority=Priority.MEDIUM)

                elif self.selection_mode == Renderer.Selection.BOX:
                    self.main_window.viewer.start_task("basic_scan",
                                                       startPos=ChunkPosition(*self.selection[0]),
                                                       endPos=ChunkPosition(*self.selection[1]),
                                                       chunkSkip=(Config.INTERPOLATION_SIZE[0] + Config.INTERPOLATION_SIZE[1]) // 2,
                                                       dimension=Dimension.value_to_mc(self.dimension),
                                                       priority=Priority.MEDIUM)

                self.selection.clear()

            except Exception as error:
                self.error_emitter.emit(repr(error))
