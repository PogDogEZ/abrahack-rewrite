#!/usr/bin/env python3

import datetime
import operator
from typing import List

from PyQt5.QtWidgets import QWidget, QVBoxLayout, QTreeWidget, QHBoxLayout, QPushButton, QTreeWidgetItem

from ...network.packets import DataExchangePacket
from ...util import TrackedPlayer, Tracker


class TrackingTab(QWidget):

    def __init__(self, main_window) -> None:
        super().__init__()

        self.main_window = main_window

        self._tracked_players = {}

        self.setObjectName("tracking_tab")

        self.main_layout = QVBoxLayout(self)

        self.tracked_players_tree_widget = QTreeWidget(self)
        self.main_layout.addWidget(self.tracked_players_tree_widget)

        self.buttons_layout = QHBoxLayout()

        self.goto_button = QPushButton(self)
        self.goto_button.setEnabled(False)
        self.buttons_layout.addWidget(self.goto_button)

        self.untrack_button = QPushButton(self)
        self.untrack_button.setEnabled(False)
        self.buttons_layout.addWidget(self.untrack_button)

        self.main_layout.addLayout(self.buttons_layout)

        self.tracked_players_tree_widget.setHeaderLabel(" Tracked players (0):")
        self.goto_button.setText("Goto player")
        self.untrack_button.setText("Untrack player")

        self.main_window.connect_emitter.connect(self._reset_tracked_players)
        self.main_window.disconnect_emitter.connect(self._reset_tracked_players)
        self.main_window.reporter_selected_emitter.connect(lambda reporter: self._reset_tracked_players())
        self.main_window.reporter_unselected_emitter.connect(self._reset_tracked_players)
        self.main_window.tracked_player_data_emitter.connect(self._on_tracked_player_data)
        self.main_window.tracker_added_emitter.connect(self._on_tracker_added)
        self.main_window.tracker_removed_emitter.connect(self._on_tracker_removed)
        self.main_window.tracker_updated_emitter.connect(self._on_tracker_updated)

        self.tracked_players_tree_widget.itemSelectionChanged.connect(self._on_item_selection_changed)

    # ----------------------------- Convenience methods ----------------------------- #

    def _get_item_widget(self, tracked_player: TrackedPlayer) -> QTreeWidgetItem:
        # noinspection PyTypeChecker
        return self._tracked_players.get(tracked_player.tracked_player_id, (None, None))[1]

    def _get_tracked_player(self, item_widget: QTreeWidgetItem) -> TrackedPlayer:
        for tracked_player_id, (tracked_player, item_widget2) in self._tracked_players.items():
            if item_widget == item_widget2:
                return tracked_player

        # noinspection PyTypeChecker
        return None

    def _update_tracked_players_header(self) -> None:
        if self.main_window.viewer is None or self.main_window.viewer.current_reporter is None:
            tracked_players = 0
        else:
            tracked_players = len(self._tracked_players)

        self.tracked_players_tree_widget.setHeaderLabel("Tracked players (%i):" % tracked_players)

    # noinspection PyMethodMayBeStatic
    def _update_tracked_player_data(self, item_widget: QTreeWidgetItem, tracked_player: TrackedPlayer) -> QTreeWidgetItem:
        if tracked_player.get_possible_players():
            best_possible = max(tracked_player.get_possible_players().items(), key=operator.itemgetter(1))[0]
            best_possible = self.main_window.viewer.get_name_for_uuid(best_possible)
        else:
            best_possible = "Unknown"
        current_position = tracked_player.render_distance.center_position
        error_x, error_z = tracked_player.render_distance.error_x, tracked_player.render_distance.error_z

        item_widget.setText(0, "\"%s\": %i" % (best_possible, tracked_player.tracked_player_id))

        if not item_widget.childCount():
            for index in range(6):
                item_widget.addChild(QTreeWidgetItem([]))

        item_widget.child(0).setText(0, "Logged in: %s" % (not tracked_player.logged_out))
        item_widget.child(1).setText(0, "Position: %i, %i" % (current_position.x * 16, current_position.z * 16))
        item_widget.child(1).setToolTip(0, "%i, %i" % (current_position.x * 16, current_position.z * 16))
        item_widget.child(2).setText(0, "Error: %.1f, %.1f" % (error_x * 16, error_z * 16))
        item_widget.child(2).setToolTip(0, "%.1f, %.1f" % (error_x * 16, error_z * 16))
        item_widget.child(3).setText(0, "Dimension: %i" % tracked_player.dimension)
        item_widget.child(4).setText(0, "Since: %s" % datetime.datetime.utcfromtimestamp(tracked_player.found_at // 1000))
        item_widget.child(4).setToolTip(0, str(datetime.datetime.utcfromtimestamp(tracked_player.found_at // 1000)))

        possible_players = tracked_player.get_possible_players()
        possible_players_item_widget = item_widget.child(5)
        possible_players_item_widget.setText(0, "Possible players: %i" % len(possible_players))
        possible_players_item_widget.setToolTip(0, "The players that might possibly be the tracked player.")

        possible_players_item_widget.takeChildren()  # TODO: Only on changes to this
        for uuid, likeliness in sorted(possible_players.items(), key=operator.itemgetter(1), reverse=True):
            if likeliness < 1:
                break
            possible_players_item_widget.addChild(QTreeWidgetItem(["\"%s\": %i" % (self.main_window.viewer.get_name_for_uuid(uuid),
                                                                                   likeliness)]))

        return item_widget

    # ----------------------------- General emitters ----------------------------- #

    def _reset_tracked_players(self) -> None:
        self._tracked_players = {}
        self.tracked_players_tree_widget.clear()
        self._update_tracked_players_header()

    def _on_tracked_player_data(self, data: List[TrackedPlayer]) -> None:
        for tracked_player in data:
            if tracked_player.tracked_player_id in self._tracked_players:
                item_widget = self._tracked_players[tracked_player.tracked_player_id][1]
            else:
                item_widget = QTreeWidgetItem([""])
                self.tracked_players_tree_widget.addTopLevelItem(item_widget)

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

    def _on_item_selection_changed(self) -> None:
        tracked_player = self._get_tracked_player(self.tracked_players_tree_widget.currentItem())
        if tracked_player is not None:
            self.goto_button.setEnabled(True)
            self.untrack_button.setEnabled(True)
        else:
            self.goto_button.setEnabled(False)
            self.untrack_button.setEnabled(False)

    def _on_goto(self, checked: bool) -> None:
        ...

    def _on_untrack(self, checked: bool) -> None:
        ...
