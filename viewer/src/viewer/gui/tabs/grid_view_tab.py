#!/usr/bin/env python3
import datetime
import operator
import time
from threading import RLock
from typing import List

from PyQt5.QtCore import QThread
from PyQt5.QtGui import QPixmap
from PyQt5.QtWidgets import QWidget, QHBoxLayout, QGraphicsView, QVBoxLayout, QPushButton, QGraphicsScene, QTreeWidget, \
    QTreeWidgetItem

from viewer import DataExchangePacket
from viewer.gui.renderer import Renderer
from viewer.gui.util import array_to_qt_image
from viewer.util import TrackedPlayer, Tracker, Dimension


class GridViewTab(QWidget):
    """
    An interactive grid view, which allows you to view the chunk states and tracked players.
    """

    def __init__(self, main_window) -> None:
        super().__init__()

        self.main_window = main_window

        self._renderer = Renderer(self.main_window)
        self._left_offset = (0, 0)
        self._scale = (1, 1)

        self._nether_states = {}
        self._overworld_states = {}
        self._end_states = {}
        self._tracked_players = {}

        self._render_scheduler_thread = GridViewTab.RenderSchedulerThread()

        self.setObjectName("grid_view_tab")

        self.main_layout = QHBoxLayout(self)

        self.grid_view = QGraphicsView(self)
        self.main_layout.addWidget(self.grid_view)

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

        self.trackers_tree_widget.setHeaderLabel("Tracked players (0):")
        self.goto_button.setText("Goto player")
        self.untrack_button.setText("Untrack player")

        self.main_window.reporter_selected_emitter.connect(lambda reporter: self._update_tracked_players_header())
        self.main_window.tracked_player_data_emitter.connect(self._on_tracked_player_data)
        self.main_window.tracker_added_emitter.connect(self._on_tracker_added)
        self.main_window.tracker_removed_emitter.connect(self._on_tracker_removed)
        self.main_window.tracker_updated_emitter.connect(self._on_tracker_updated)

        self._render_scheduler_thread.start()

    def _render_grid(self) -> None:
        grid_size = self.grid_view.size()
        if grid_size.width() <= 10 or grid_size.height() <= 10:
            return

        image = self._renderer.render((grid_size.width() - 10, grid_size.height() - 10), (0, 0), self._left_offset,
                                      self._scale)

        scene = QGraphicsScene()
        scene.addPixmap(QPixmap.fromImage(array_to_qt_image(image)))

        self.grid_view.setScene(scene)

    # ----------------------------- Convenience methods ----------------------------- #

    def _update_tracked_players_header(self) -> None:
        if self.main_window.viewer is None or self.main_window.viewer.current_reporter is None:
            tracked_players = 0
        else:
            tracked_players = len(self._tracked_players)

        self.trackers_tree_widget.setHeaderLabel("Tracked players (%i):" % tracked_players)

    # noinspection PyMethodMayBeStatic
    def _update_tracked_player_data(self, item_widget: QTreeWidgetItem, tracked_player: TrackedPlayer) -> QTreeWidgetItem:
        if tracked_player.get_possible_players():
            best_possible = max(tracked_player.get_possible_players().items(), key=operator.itemgetter(1))[1]
            best_possible = self.main_window.viewer.get_name_for_uuid(best_possible)
        else:
            best_possible = "Unknown"
        current_position = tracked_player.render_distance.center_position
        error_x, error_z = tracked_player.render_distance.error_x, tracked_player.render_distance.error_z

        item_widget.setText(0, "%s:%i" % (best_possible, tracked_player.tracked_player_id))

        if not item_widget.childCount():
            for index in range(6):
                item_widget.addChild(QTreeWidgetItem([]))

        item_widget.child(0).setText(0, "ID: %i" % tracked_player.tracked_player_id)
        item_widget.child(1).setText(0, "Position: %i, %i" % (current_position.x * 16, current_position.z * 16))
        item_widget.child(2).setText(0, "Error: %.1f, %.1f" % (error_x * 16, error_z * 16))
        item_widget.child(3).setText(0, "Dimension: %s" % Dimension.name_from_value(Dimension.mc_to_value(tracked_player.dimension)))
        item_widget.child(4).setText(0, "Since: %s" % datetime.datetime.utcfromtimestamp(tracked_player.found_at // 1000))
        item_widget.child(5).setText(0, "Logged out: %s" % tracked_player.logged_out)

        for index in range(item_widget.childCount()):
            item_widget.child(index).setToolTip(0, item_widget.child(index).text(0))

        # TODO: Possible players

        return item_widget

    # ----------------------------- General emitters ----------------------------- #

    def _on_tracked_player_data(self, data: List[TrackedPlayer]) -> None:
        for tracked_player in data:
            if tracked_player.tracked_player_id in self._tracked_players:
                item_widget = self._tracked_players[tracked_player.tracked_player_id][1]
            else:
                item_widget = QTreeWidgetItem([""])
                self.trackers_tree_widget.addTopLevelItem(item_widget)

            item_widget = self._update_tracked_player_data(item_widget, tracked_player)
            self._tracked_players[tracked_player.tracked_player_id] = (tracked_player, item_widget)

        self._update_tracked_players_header()

    def _on_tracker_added(self, tracker: Tracker) -> None:
        self.main_window.request_data_sync(DataExchangePacket.DataType.TRACKED_PLAYER, tracker.get_tracked_player_ids())

    def _on_tracker_removed(self, tracker: Tracker) -> None:
        ...

    def _on_tracker_updated(self, tracker: Tracker) -> None:
        self.main_window.request_data_sync(DataExchangePacket.DataType.TRACKED_PLAYER, tracker.get_tracked_player_ids())

    # ----------------------------- Widget emitters ----------------------------- #

    # ----------------------------- Threads ----------------------------- #

    class RenderSchedulerThread(QThread):

        def __init__(self) -> None:
            super().__init__()

        def run(self) -> None:
            ...
