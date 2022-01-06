#!/usr/bin/env python3

import logging
import time
from typing import List

import numpy as np
from PyQt5.QtCore import QThread, pyqtSignal
from PyQt5.QtGui import QPixmap
from PyQt5.QtWidgets import QWidget, QHBoxLayout, QVBoxLayout, QPushButton, QGraphicsScene, QTreeWidget, QLabel

from ..renderer.renderer import Renderer
from ..util import array_to_qt_image
from ...config import Config
from ...util import ChunkState, Dimension


class GridViewTab(QWidget):
    """
    The grid view tab, shows a grid (detailing loaded chunks and tracked players) and a list of trackers.
    """

    def __init__(self, main_window) -> None:
        super().__init__()

        self.main_window = main_window

        self._background_thread = GridViewTab.BackgroundThread(self.main_window, self)

        self.setObjectName("grid_view_tab")

        self.main_layout = QHBoxLayout(self)
        # self.main_layout.setContentsMargins(0, 0, 0, 0)

        self.grid_view_layout = QVBoxLayout(self)
        # self.grid_view_layout.setSpacing(2)

        # self.coords_label = QLabel(self)
        # self.grid_view_layout.addWidget(self.coords_label)

        self.grid_view = Renderer(self, self.main_window)
        self.grid_view_layout.addWidget(self.grid_view)

        self.main_layout.addLayout(self.grid_view_layout)

        self.trackers_layout = QVBoxLayout()

        self.trackers_tree_widget = QTreeWidget(self)
        self.trackers_layout.addWidget(self.trackers_tree_widget)

        self.buttons_layout = QHBoxLayout()

        self.goto_button = QPushButton(self)
        self.goto_button.setEnabled(False)
        self.buttons_layout.addWidget(self.goto_button)

        self.untrack_button = QPushButton(self)
        self.untrack_button.setEnabled(False)
        self.buttons_layout.addWidget(self.untrack_button)

        self.trackers_layout.addLayout(self.buttons_layout)

        self.main_layout.addLayout(self.trackers_layout)
        self.main_layout.setStretch(0, 1)

        # self.coords_label.setText("Coords: 0, 0")
        self.trackers_tree_widget.setHeaderLabel("Trackers (0):")
        self.goto_button.setText("Goto tracker")
        self.untrack_button.setText("Untrack tracker")

        self.main_window.connect_emitter.connect(self._reset_data)
        self.main_window.disconnect_emitter.connect(self._reset_data)
        self.main_window.reporter_selected_emitter.connect(lambda reporter: self._reset_data())
        self.main_window.resized_emitter.connect(self._render_grid)
        self.main_window.chunk_data_emitter.connect(self._on_chunk_data)

        self._background_thread.render_update_emitter.connect(self._render_grid)

        self._background_thread.start()

    def _render_grid(self) -> None:
        grid_size = self.grid_view.size()
        if grid_size.width() <= 10 or grid_size.height() <= 10:
            return

        # noinspection PyProtectedMember
        image = self.grid_view._render()

        scene = QGraphicsScene()
        scene.addPixmap(QPixmap.fromImage(array_to_qt_image(image)))

        self.grid_view.setScene(scene)

    def _reset_data(self) -> None:
        self.grid_view.states.clear()

    # ----------------------------- General emitters ----------------------------- #

    # noinspection PyTypeChecker
    def _on_chunk_data(self, states: List[ChunkState]) -> None:
        for state in states:
            region = (state.chunk_position.x // Config.REGION_SIZE[0], state.chunk_position.z // Config.REGION_SIZE[1])
            value = 255 if state.state == ChunkState.State.LOADED else 32

            dimension = Dimension.mc_to_value(state.dimension)
            if not dimension in self.grid_view.states:
                self.grid_view.states[dimension] = {}

            if not region in self.grid_view.states[dimension]:
                self.grid_view.states[dimension][region] = np.zeros((Config.REGION_SIZE[1] // Config.INTERPOLATION_SIZE[1],
                                                                     Config.REGION_SIZE[0] // Config.INTERPOLATION_SIZE[0]),
                                                                    dtype=np.uint8)
                self.grid_view.states[dimension][region][:, :] = 0

            coords = ((state.chunk_position.x % Config.REGION_SIZE[0]) // Config.INTERPOLATION_SIZE[0],
                      (state.chunk_position.z % Config.REGION_SIZE[1]) // Config.INTERPOLATION_SIZE[1])
            self.grid_view.states[dimension][region][coords[::-1]] = value

    # ----------------------------- Threads ----------------------------- #

    class BackgroundThread(QThread):

        render_update_emitter = pyqtSignal()

        def __init__(self, main_window, grid_view_tab) -> None:
            super().__init__()

            self.main_window = main_window
            self.grid_view_tab = grid_view_tab

        def run(self) -> None:
            decay_rate = 0
            while True:
                start = time.time()

                # coords = self.grid_view_tab.grid_view.cursor_coords
                # coords = (round(coords[0] * 16), round(coords[1] * 16))
                # self.grid_view_tab.coords_label.setText("Coords: %i, %i" % coords)

                # Decay the chunk states
                if Config.DECAY_RATE and decay_rate > Config.DECAY_RATE:
                    decay_rate = 0
                    try:
                        for dimension in self.grid_view_tab.grid_view.states:
                            for region_coords, region in self.grid_view_tab.grid_view.states[dimension].items():
                                # region[region < 32] += 1
                                region[region > 0] -= 1
                    except RuntimeError:  # Lazy solution
                        ...
                decay_rate += 1

                if self.main_window.main_tab_widget.currentWidget() == self.grid_view_tab:
                    self.render_update_emitter.emit()

                delta_time = time.time() - start  # FIXME: Necessary?
                if delta_time < 0.03333:
                    time.sleep(0.03333 - delta_time)
