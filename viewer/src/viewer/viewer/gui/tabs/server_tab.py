#!/usr/bin/env python3

import math
import time
from uuid import UUID

import cv2
import numpy as np
from PyQt5.QtCore import Qt, QThread, pyqtSignal
from PyQt5.QtGui import QPixmap
from PyQt5.QtWidgets import QWidget, QVBoxLayout, QLabel, QHBoxLayout, QGridLayout, QGraphicsView, \
    QListWidget, QGraphicsScene, QListWidgetItem, QSizePolicy

from ..renderer.graph import plot_graph
from ..util import array_to_qt_image
from ...config import Config


class ServerTab(QWidget):

    def __init__(self, main_window) -> None:
        super().__init__()

        self.main_window = main_window

        self._online_players_list = []

        self.tick_data = []
        self.tslp_data = []
        self.query_data = []
        self.trackers_data = []

        self._background_thread = ServerTab.BackgroundThread(self.main_window, self)

        self.setObjectName("server_tab")

        self.main_outer_layout = QVBoxLayout(self)
        self.server_info_label = QLabel(self)
        self.server_info_label.setAlignment(Qt.AlignLeading | Qt.AlignLeft | Qt.AlignTop)

        self.main_outer_layout.addWidget(self.server_info_label)

        self.main_inner_layout = QHBoxLayout()
        self.online_players_layout = QVBoxLayout()

        self.online_players_label = QLabel(self)
        self.online_players_layout.addWidget(self.online_players_label)

        self.online_players_view = QListWidget(self)
        self.online_players_view.setFixedWidth(self.online_players_view.fontMetrics().width("M" * 16))
        self.online_players_view.setSizePolicy(QSizePolicy.Expanding, QSizePolicy.Expanding)
        self.online_players_layout.addWidget(self.online_players_view)

        self.main_inner_layout.addLayout(self.online_players_layout)

        self.graphs_layout = QGridLayout()

        self.tickrate_layout = QVBoxLayout()
        self.tickrate_label = QLabel(self)
        self.tickrate_layout.addWidget(self.tickrate_label)

        self.tickrate_graph = QGraphicsView(self)
        self.tickrate_graph.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
        self.tickrate_graph.setVerticalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
        self.tickrate_layout.addWidget(self.tickrate_graph)
        self.graphs_layout.addLayout(self.tickrate_layout, 0, 1, 1, 1)

        self.tslp_layout = QVBoxLayout()
        self.tslp_label = QLabel(self)
        self.tslp_layout.addWidget(self.tslp_label)

        self.tslp_graph = QGraphicsView(self)
        self.tslp_graph.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
        self.tslp_graph.setVerticalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
        self.tslp_layout.addWidget(self.tslp_graph)
        self.graphs_layout.addLayout(self.tslp_layout, 1, 1, 1, 1)

        self.queryrate_layout = QVBoxLayout()
        self.queryrate_label = QLabel(self)
        self.queryrate_layout.addWidget(self.queryrate_label)

        self.queryrate_graph = QGraphicsView(self)
        self.queryrate_layout.addWidget(self.queryrate_graph)
        self.graphs_layout.addLayout(self.queryrate_layout, 1, 2, 1, 1)

        self.trackers_layout = QVBoxLayout()
        self.trackers_label = QLabel(self)
        self.trackers_layout.addWidget(self.trackers_label)

        self.trackers_graph = QGraphicsView(self)
        self.trackers_layout.addWidget(self.trackers_graph)
        self.graphs_layout.addLayout(self.trackers_layout, 0, 2, 1, 1)

        self.main_inner_layout.addLayout(self.graphs_layout)

        self.main_outer_layout.addLayout(self.main_inner_layout)

        self.main_inner_layout.setStretch(1, 2)
        self.main_outer_layout.setStretch(1, 1)

        self.server_info_label.setText("Server info for (not connected):")
        self.online_players_label.setText("Online players (0):")
        self.tickrate_label.setText("Tickrate (tps):")
        self.tslp_label.setText("TSLP (ms):")
        self.trackers_label.setText("Trackers:")
        self.queryrate_label.setText("Queryrate (qps):")

        # Update the slower updating graphs
        self.main_window.main_tab_widget.currentChanged.connect(self._update_tickrate_graph)
        self.main_window.main_tab_widget.currentChanged.connect(self._update_queryrate_graph)
        self.main_window.resized_emitter.connect(self._update_tickrate_graph)
        self.main_window.resized_emitter.connect(self._update_queryrate_graph)
        
        self.main_window.connect_emitter.connect(self._reset_data)
        self.main_window.disconnect_emitter.connect(self._reset_data)
        self.main_window.reporter_selected_emitter.connect(lambda reporter: self._reset_data())
        self.main_window.online_player_added_emitter.connect(self._on_online_player_added)
        self.main_window.online_player_removed_emitter.connect(self._on_online_player_removed)

        self._background_thread.tickrate_update_emitter.connect(self._update_tickrate_graph)
        self._background_thread.tslp_update_emitter.connect(self._update_tslp_graph)
        # self._background_thread.trackers_update_emitter.connect(self._update_ping_graph)
        self._background_thread.query_update_emitter.connect(self._update_queryrate_graph)

        self._background_thread.start()

    def _reset_data(self):
        for item_widget, uuid in self._online_players_list:
            self.online_players_view.takeItem(self.online_players_view.row(item_widget))
        self._online_players_list.clear()

        self.tick_data.clear()
        self.trackers_data.clear()
        self.tslp_data.clear()
        self.query_data.clear()

        if self.tickrate_graph.scene() is not None:
            self.tickrate_graph.scene().clear()
            self.tickrate_graph.setScene(None)
        if self.trackers_graph.scene() is not None:
            self.trackers_graph.scene().clear()
            self.trackers_graph.setScene(None)
        if self.tslp_graph.scene() is not None:
            self.tslp_graph.scene().clear()
            self.tslp_graph.setScene(None)
        if self.queryrate_graph.scene() is not None:
            self.queryrate_graph.scene().clear()
            self.queryrate_graph.setScene(None)

    def _on_online_player_added(self, uuid: UUID, name: str) -> None:
        item_widget = QListWidgetItem(name)
        self.online_players_view.addItem(item_widget)
        self._online_players_list.append((item_widget, uuid))

    def _on_online_player_removed(self, uuid: UUID, name: str) -> None:
        for item_widget, uuid2 in self._online_players_list:
            if uuid == uuid2:
                self.online_players_view.takeItem(self.online_players_view.row(item_widget))
                self._online_players_list.remove((item_widget, uuid))
                break

    def _update_tickrate_graph(self) -> None:
        tickrate_size = self.tickrate_graph.size()
        if not self.tick_data or tickrate_size.height() <= 10 or tickrate_size.width() <= 10:
            return

        graph = np.zeros((tickrate_size.height() - 10, tickrate_size.width() - 10, 3), dtype=np.uint8)
        graph[:, :] = Config.BASE_COLOUR[:3]

        cv2.line(graph, (0, int(graph.shape[0] / 1.5)), (graph.shape[1], int(graph.shape[0] / 1.5)),
                 Config.DARK_COLOUR[:3], 1)
        cv2.line(graph, (0, int(graph.shape[0] / 3)), (graph.shape[1], int(graph.shape[0] / 3)),
                 Config.DARK_COLOUR[:3], 1)

        plot_graph(graph, self.tick_data, 30, Config.MID_COLOUR[:3], Config.GRAPH_LINE_THICKNESS)

        # Oops I guess I need to flip it lol
        # graph = graph[::-1, :]  # This makes it incompatible with cv2?
        graph = cv2.flip(graph, 0)

        cv2.putText(graph, "20.0tps", (0, int(graph.shape[0] / 3) - 2), cv2.FONT_HERSHEY_DUPLEX, Config.GRAPH_FONT_SCALE,
                    Config.TEXT_COLOUR[:3], 1, cv2.LINE_AA)
        cv2.putText(graph, "10.0tps", (0, int(graph.shape[0] / 1.5) - 2), cv2.FONT_HERSHEY_DUPLEX,
                    Config.GRAPH_FONT_SCALE, Config.TEXT_COLOUR[:3], 1, cv2.LINE_AA)

        scene = QGraphicsScene()
        scene.addPixmap(QPixmap.fromImage(array_to_qt_image(graph)))

        self.tickrate_graph.setScene(scene)

    def _update_tslp_graph(self) -> None:
        tslp_size = self.tslp_graph.size()
        if not self.tslp_data or tslp_size.height() <= 10 or tslp_size.width() <= 10:
            return

        graph = np.zeros((tslp_size.height() - 10, tslp_size.width() - 10, 3), dtype=np.uint8)
        graph[:, :] = Config.BASE_COLOUR[:3]

        cv2.line(graph, (0, graph.shape[0] // 2), (graph.shape[1], graph.shape[0] // 2), Config.DARK_COLOUR[:3], 1)

        max_tslp = max(100, int(math.ceil(max(self.tslp_data) / 50) * 50))
        # Nearest neighbor prevents the peaks from shifting, even if it isn't as smooth
        plot_graph(graph, self.tslp_data, max_tslp, Config.MID_COLOUR[:3], Config.GRAPH_LINE_THICKNESS, cv2.INTER_NEAREST)

        graph = cv2.flip(graph, 0)

        cv2.putText(graph, "%.1fms" % (max_tslp / 2), (0, graph.shape[0] // 2 - 2), cv2.FONT_HERSHEY_DUPLEX,
                    Config.GRAPH_FONT_SCALE, Config.TEXT_COLOUR[:3], 1, cv2.LINE_AA)

        scene = QGraphicsScene()
        scene.addPixmap(QPixmap.fromImage(array_to_qt_image(graph)))

        self.tslp_graph.setScene(scene)

    def _update_queryrate_graph(self) -> None:
        queryrate_size = self.queryrate_graph.size()
        if not self.query_data or queryrate_size.height() <= 10 or queryrate_size.width() <= 10:
            return

        graph = np.zeros((queryrate_size.height() - 10, queryrate_size.width() - 10, 3), dtype=np.uint8)
        graph[:, :] = Config.BASE_COLOUR[:3]

        cv2.line(graph, (0, graph.shape[0] // 4), (graph.shape[1], graph.shape[0] // 4), Config.DARK_COLOUR[:3], 1)
        cv2.line(graph, (0, graph.shape[0] // 2), (graph.shape[1], graph.shape[0] // 2), Config.DARK_COLOUR[:3], 1)
        cv2.line(graph, (0, int(graph.shape[0] / (4 / 3))), (graph.shape[1], int(graph.shape[0] / (4 / 3))),
                 Config.DARK_COLOUR[:3], 1)

        max_queryrate = max(20, int(math.ceil(max(self.query_data) / 20) * 20))
        plot_graph(graph, self.query_data, max_queryrate, Config.MID_COLOUR[:3], Config.GRAPH_LINE_THICKNESS)

        graph = cv2.flip(graph, 0)

        cv2.putText(graph, "%.1fqps" % (max_queryrate / (4 / 3)), (0, graph.shape[0] // 4 - 2), cv2.FONT_HERSHEY_DUPLEX,
                    Config.GRAPH_FONT_SCALE, Config.TEXT_COLOUR[:3], 1, cv2.LINE_AA)
        cv2.putText(graph, "%.1fqps" % (max_queryrate / 2), (0, graph.shape[0] // 2 - 2), cv2.FONT_HERSHEY_DUPLEX,
                    Config.GRAPH_FONT_SCALE, Config.TEXT_COLOUR[:3], 1, cv2.LINE_AA)
        cv2.putText(graph, "%.1fqps" % (max_queryrate / 4), (0, int(graph.shape[0] / (4 / 3)) - 2),
                    cv2.FONT_HERSHEY_DUPLEX, Config.GRAPH_FONT_SCALE, Config.TEXT_COLOUR[:3], 1, cv2.LINE_AA)

        scene = QGraphicsScene()
        scene.addPixmap(QPixmap.fromImage(array_to_qt_image(graph)))

        self.queryrate_graph.setScene(scene)

    class BackgroundThread(QThread):

        tickrate_update_emitter = pyqtSignal()
        tslp_update_emitter = pyqtSignal()
        query_update_emitter = pyqtSignal()
        trackers_update_emitter = pyqtSignal()

        def __init__(self, main_window, server_tab) -> None:
            super().__init__()

            self.main_window = main_window
            self.server_tab = server_tab

        def run(self) -> None:
            tickrate_update = time.time() - 1
            queryrate_update = time.time() - 0.1
            while True:
                start = time.time()
                if self.main_window.viewer is not None and self.main_window.viewer.current_reporter is not None:
                    tab_selected = self.main_window.main_tab_widget.currentWidget() == self.server_tab
                    current_reporter = self.main_window.viewer.current_reporter

                    self.server_tab.server_info_label.setText("Server info for (%s:%i):" % (current_reporter.host,
                                                                                            current_reporter.port))
                    self.server_tab.online_players_label.setText(
                        "Online players (%i):" % len(current_reporter.online_players))

                    self.server_tab.tslp_data.append(int(current_reporter.time_since_last_packet))

                    if tab_selected:
                        self.tslp_update_emitter.emit()

                    if time.time() - tickrate_update > 1:
                        tickrate_update = time.time()
                        self.server_tab.tick_data.append(current_reporter.tick_rate)
                        self.server_tab.trackers_data.append(len(current_reporter.get_trackers()))
                        if tab_selected:
                            self.tickrate_update_emitter.emit()
                            self.trackers_update_emitter.emit()

                    if time.time() - queryrate_update > 0.1:
                        queryrate_update = time.time()
                        self.server_tab.query_data.append(current_reporter.queries_per_second)
                        if tab_selected:
                            self.query_update_emitter.emit()

                    while len(self.server_tab.tick_data) > 240:
                        self.server_tab.tick_data.pop(0)
                    while len(self.server_tab.tslp_data) > 240:
                        self.server_tab.tslp_data.pop(0)
                    while len(self.server_tab.query_data) > 160:
                        self.server_tab.query_data.pop(0)
                    while len(self.server_tab.trackers_data) > 100:
                        self.server_tab.trackers_data.pop(0)

                    # online_players = current_reporter.get_online_players()
                    # self.server_tab.online_players_label.setText("Online players (%i):" % len(online_players))
                    # self.server_tab.online_players_view.setModel(QStringListModel(list(online_players.values())))

                else:
                    self.server_tab.server_info_label.setText("Server info for (not connected):")
                    self.server_tab.online_players_label.setText("Online players (0):")

                delta_time = time.time() - start
                if delta_time < 0.041666:
                    time.sleep(0.041666 - delta_time)
