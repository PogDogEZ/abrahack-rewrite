#!/usr/bin/env python3

import logging
import time
from threading import RLock
from typing import List
from uuid import UUID

from PyQt5.QtCore import Qt, QMetaObject, pyqtSignal, QThread
from PyQt5.QtGui import QResizeEvent
from PyQt5.QtWidgets import QMainWindow, QWidget, QSizePolicy, QGridLayout, QGroupBox, QTabWidget, QMessageBox

from pyclient.networking.connection import Connection
from viewer import Viewer, Reporter, DataExchangePacket
from viewer.config import Config
from viewer.events import ReporterEvent, DataEvent, PlayerEvent, DataBoundsEvent, ConnectEvent, DisconnectEvent, \
    OnlinePlayerEvent, Event, TaskEvent, TrackerEvent
from viewer.gui.dialogs.progess_dialog import ProgressDialog
from viewer.gui.tabs.accounts_tab import AccountsTab
from viewer.gui.tabs.chat_tab import ChatTab
from viewer.gui.tabs.config_tab import ConfigTab
from viewer.gui.tabs.grid_view_tab import GridViewTab
from viewer.gui.tabs.reporter_tab import ReporterTab
from viewer.gui.tabs.server_tab import ServerTab
from viewer.gui.tabs.tasks_tab import TasksTab
from viewer.util import Player, ActiveTask, Tracker


class MainWindow(QMainWindow):
    """
    Main window for the UI. Disclaimer, the UI was designed in Qt Designer so some code may look weird, it is almost
    directly copied across from the generated code, plus, as of writing this, I have no idea how to use Qt.
    """

    INSTANCE = None

    resized_emitter = pyqtSignal()

    connect_emitter = pyqtSignal()
    disconnect_emitter = pyqtSignal()

    chat_data_emitter = pyqtSignal(list)
    chat_data_bounds_emitter = pyqtSignal(int, int)
    chunk_data_emitter = pyqtSignal(list)
    chunk_data_bounds_emitter = pyqtSignal(int, int)
    tracked_player_data_emitter = pyqtSignal(list)
    tracked_player_data_bounds_emitter = pyqtSignal(int, int)

    reporter_added_emitter = pyqtSignal(Reporter)
    reporter_removed_emitter = pyqtSignal(Reporter)
    reporter_selected_emitter = pyqtSignal(Reporter)
    reporter_unselected_emitter = pyqtSignal()

    player_added_emitter = pyqtSignal(Player)
    player_removed_emitter = pyqtSignal(Player, str)
    player_updated_emitter = pyqtSignal(Player)

    task_added_emitter = pyqtSignal(ActiveTask)
    task_removed_emitter = pyqtSignal(ActiveTask)
    task_updated_emitter = pyqtSignal(ActiveTask)
    task_result_emitter = pyqtSignal(ActiveTask, str)

    tracker_added_emitter = pyqtSignal(Tracker)
    tracker_removed_emitter = pyqtSignal(Tracker)
    tracker_updated_emitter = pyqtSignal(Tracker)

    online_player_added_emitter = pyqtSignal(UUID, str)
    online_player_removed_emitter = pyqtSignal(UUID, str)

    @property
    def connection(self) -> Connection:
        return self._connection

    @connection.setter
    def connection(self, connection: Connection) -> None:
        self._connection = connection

    @property
    def viewer(self) -> Viewer:
        return self._viewer

    @viewer.setter
    def viewer(self, viewer: Viewer) -> None:
        self._viewer = viewer

        if self._viewer is not None:
            self.viewer.add_listener(self._event_listener)

    def __init__(self) -> None:
        super().__init__()

        MainWindow.INSTANCE = self

        self._connection = None
        self._viewer = None

        self._progress = None

        self.data_sync_lock = RLock()  # Used to control access to _data_sync_queue
        self.data_download_lock = RLock()  # Used to stop concurrent downloads (results in an error)
        self._data_sync_queue = {}

        self._background_thread = MainWindow.BackgroundThread(self)
        self._data_sync_thread = None

        self.setObjectName("main_window")
        self.resize(1, 1)

        # Gui stuff below

        self.central_widget = QWidget(self)
        self.central_widget.setEnabled(True)

        size_policy = QSizePolicy(QSizePolicy.Expanding, QSizePolicy.Expanding)
        size_policy.setHorizontalStretch(0)
        size_policy.setVerticalStretch(0)
        size_policy.setHeightForWidth(self.central_widget.sizePolicy().hasHeightForWidth())

        self.central_widget.setSizePolicy(size_policy)
        self.central_widget.setLayoutDirection(Qt.LeftToRight)

        self.main_outer_layout = QGridLayout(self.central_widget)
        self.main_outer_layout.setSpacing(0)
        self.main_outer_layout.setContentsMargins(0, 0, 0, 0)

        self.main_group_box = QGroupBox(self.central_widget)
        self.main_group_box.setAlignment(Qt.AlignJustify | Qt.AlignVCenter)
        self.main_group_box.setFlat(True)
        self.main_group_box.setCheckable(False)

        self.main_inner_layout = QGridLayout(self.main_group_box)
        self.main_inner_layout.setSpacing(0)
        self.main_inner_layout.setContentsMargins(0, 0, 0, 0)

        self.main_tab_widget = QTabWidget(self.main_group_box)
        self.main_tab_widget.setObjectName("main_tab_widget")
        self.main_tab_widget.setTabPosition(QTabWidget.North)
        self.main_tab_widget.setTabShape(QTabWidget.Rounded)
        self.main_tab_widget.setElideMode(Qt.ElideNone)
        self.main_tab_widget.setUsesScrollButtons(True)

        self.reporter_tab = ReporterTab(self)
        self.grid_view_tab = GridViewTab(self)
        self.accounts_tab = AccountsTab(self)
        self.tasks_tab = TasksTab(self)
        self.server_tab = ServerTab(self)
        self.chat_tab = ChatTab(self)
        self.config_tab = ConfigTab()

        self.main_tab_widget.addTab(self.reporter_tab, "")
        self.main_tab_widget.addTab(self.grid_view_tab, "")
        self.main_tab_widget.addTab(self.accounts_tab, "")
        self.main_tab_widget.addTab(self.tasks_tab, "")
        self.main_tab_widget.addTab(self.server_tab, "")
        self.main_tab_widget.addTab(self.chat_tab, "")
        self.main_tab_widget.addTab(self.config_tab, "")

        self.setWindowTitle("Viewer")
        self.main_group_box.setTitle(" YesCom Viewer (no reporter)    ")  # FIXME: Why do I have to add spaces to make it not cut off?

        self.main_tab_widget.setTabText(self.main_tab_widget.indexOf(self.reporter_tab), "Reporter")
        self.main_tab_widget.setTabText(self.main_tab_widget.indexOf(self.grid_view_tab), "Grid View")
        self.main_tab_widget.setTabText(self.main_tab_widget.indexOf(self.accounts_tab), "Accounts")
        self.main_tab_widget.setTabText(self.main_tab_widget.indexOf(self.tasks_tab), "Tasks")
        self.main_tab_widget.setTabText(self.main_tab_widget.indexOf(self.server_tab), "Server")
        self.main_tab_widget.setTabText(self.main_tab_widget.indexOf(self.chat_tab), "Chat")
        self.main_tab_widget.setTabText(self.main_tab_widget.indexOf(self.config_tab), "Config")

        self.main_outer_layout.addWidget(self.main_group_box)
        self.main_inner_layout.addWidget(self.main_tab_widget)

        self.setCentralWidget(self.central_widget)
        QMetaObject.connectSlotsByName(self)

        self.reporter_selected_emitter.connect(self._on_reporter_selected)
        self._background_thread.reporter_state_emitter.connect(lambda state: self.main_group_box.setTitle(" YesCom Viewer %s    " % state))

        self._background_thread.start()

    def resizeEvent(self, resize_event: QResizeEvent) -> None:
        self.resized_emitter.emit()

        super().resizeEvent(resize_event)

    # ----------------------------- General emitters ----------------------------- #

    def _on_reporter_selected(self, reporter: Reporter) -> None:
        if self._progress is None:
            self._progress = ProgressDialog(self, "Syncing data...", "Data Sync")
            self._data_sync_thread = MainWindow.DataSyncThread(self)

            self._data_sync_thread.progress_emitter.connect(self._progress.progress_bar.setValue)
            self._data_sync_thread.error_emitter.connect(self._on_data_sync_error)
            self._data_sync_thread.success_emitter.connect(self._on_data_sync_success)

            self._data_sync_thread.start()

    def _on_data_sync_error(self, error: str) -> None:
        if self._progress is not None:
            self._progress.close()
            self._progress = None

        QMessageBox.critical(self, "Data Sync Error", error)

    def _on_data_sync_success(self) -> None:
        if self._progress is not None:
            self._progress.close()
            self._progress = None

    # ----------------------------- Viewer event listener ----------------------------- #

    def _event_listener(self, event: Event) -> None:
        if isinstance(event, ConnectEvent):
            self.connect_emitter.emit()

        elif isinstance(event, DisconnectEvent):
            self.disconnect_emitter.emit()

        elif isinstance(event, DataEvent):
            if event.data_type == DataExchangePacket.DataType.CHAT:
                self.chat_data_emitter.emit(event.data)
            elif event.data_type == DataExchangePacket.DataType.CHUNK_STATE:
                self.chunk_data_emitter.emit(event.data)
            elif event.data_type == DataExchangePacket.DataType.TRACKED_PLAYER:
                self.tracked_player_data_emitter.emit(event.data)

        elif isinstance(event, DataBoundsEvent):
            if event.data_type == DataExchangePacket.DataType.CHAT:
                self.chat_data_bounds_emitter.emit(event.min_data_id, event.max_data_id)
            elif event.data_type == DataExchangePacket.DataType.CHUNK_STATE:
                self.chunk_data_bounds_emitter.emit(event.min_data_id, event.max_data_id)
            elif event.data_type == DataExchangePacket.DataType.TRACKED_PLAYER:
                self.tracked_player_data_bounds_emitter.emit(event.min_data_id, event.max_data_id)

        elif isinstance(event, ReporterEvent):
            if event.event_type == ReporterEvent.EventType.ADDED:
                self.reporter_added_emitter.emit(event.reporter)
            elif event.event_type == ReporterEvent.EventType.REMOVED:
                self.reporter_removed_emitter.emit(event.reporter)
            else:
                if event.reporter is not None:
                    self.reporter_selected_emitter.emit(event.reporter)
                else:
                    self.reporter_unselected_emitter.emit()

        elif isinstance(event, PlayerEvent):
            if event.event_type == PlayerEvent.EventType.ADDED:
                self.player_added_emitter.emit(event.player)
            elif event.event_type == PlayerEvent.EventType.REMOVED:
                self.player_removed_emitter.emit(event.player, event.reason)
            else:
                self.player_updated_emitter.emit(event.player)

        elif isinstance(event, TaskEvent):
            if event.event_type == TaskEvent.EventType.ADDED:
                self.task_added_emitter.emit(event.active_task)
            elif event.event_type == TaskEvent.EventType.REMOVED:
                self.task_removed_emitter.emit(event.active_task)
            elif event.event_type == TaskEvent.EventType.UPDATED:
                self.task_updated_emitter.emit(event.active_task)
            else:
                self.task_result_emitter.emit(event.active_task, event.result)

        elif isinstance(event, TrackerEvent):
            if event.event_type == TrackerEvent.EventType.ADDED:
                self.tracker_added_emitter.emit(event.tracker)
            elif event.event_type == TrackerEvent.EventType.REMOVED:
                self.tracker_removed_emitter.emit(event.tracker)
            else:
                self.tracker_updated_emitter.emit(event.tracker)

        elif isinstance(event, OnlinePlayerEvent):
            if event.event_type == OnlinePlayerEvent.EventType.ADDED:
                self.online_player_added_emitter.emit(event.uuid, event.name)
            else:
                self.online_player_removed_emitter.emit(event.uuid, event.name)

    # ----------------------------- Public methods ----------------------------- #

    def request_data_sync(self, data_type: DataExchangePacket.DataType, data_ids: List[int]) -> None:
        """
        Requests that the client synchronize the provided data.

        :param data_type: The type of data to synchronize.
        :param data_ids: The list of data IDs to synchronize.
        """

        with self.data_sync_lock:
            if not data_type in self._data_sync_queue:
                self._data_sync_queue[data_type] = []
            self._data_sync_queue[data_type].extend(data_ids)

    # ----------------------------- Threads ----------------------------- #

    class BackgroundThread(QThread):
        """
        Background thread, responsible for updating UI elements as well as requesting that certain data be synced.
        """

        reporter_state_emitter = pyqtSignal(str)

        def __init__(self, main_window) -> None:
            super().__init__()

            self.main_window = main_window

        # noinspection PyProtectedMember
        def run(self) -> None:
            while True:
                start = time.time()

                if self.main_window.viewer is not None and self.main_window.viewer.current_reporter is not None:
                    current_reporter = self.main_window.viewer.current_reporter
                    state_str = "tick: %.1ftps, ping: %.1fms, query: %.1fqps" % (current_reporter.tick_rate,
                                                                                 current_reporter.server_ping,
                                                                                 current_reporter.queries_per_second)

                    if current_reporter.time_since_last_packet > Config.LAG_THRESHOLD:
                        state_str += ", lag: %is" % (current_reporter.time_since_last_packet // 1000)

                    self.reporter_state_emitter.emit("(" + state_str + ")")

                    with self.main_window.data_sync_lock:
                        data_sync_queue = self.main_window._data_sync_queue.copy()
                        self.main_window._data_sync_queue.clear()

                    with self.main_window.data_download_lock:
                        for data_type in data_sync_queue:
                            try:
                                self.main_window.viewer.request_data(data_type, data_sync_queue[data_type])
                            except Exception as error:
                                logging.warning("An error occurred while syncing data:")
                                logging.error(error, exc_info=True)
                else:
                    self.reporter_state_emitter.emit("(no reporter)")

                delta_time = time.time() - start
                if delta_time < 0.05:  # Run it on a 20tps loop
                    time.sleep(0.05 - delta_time)

    class DataSyncThread(QThread):

        progress_emitter = pyqtSignal(int)
        error_emitter = pyqtSignal(str)
        success_emitter = pyqtSignal()

        def __init__(self, main_window) -> None:
            super().__init__()

            self.main_window = main_window

        def run(self) -> None:
            with self.main_window.data_download_lock:
                try:
                    logging.info("Retrieving data bounds...")
                    self.progress_emitter.emit(10)

                    logging.debug("Retrieving bounds for tick data...")
                    tick_start_time, tick_end_time, _ = self.main_window.viewer.request_numeric_data_bounds(DataExchangePacket.DataType.TICK_DATA)
                    self.progress_emitter.emit(20)
                    logging.debug("Retrieving bounds for ping data...")
                    ping_start_time, ping_end_time, _ = self.main_window.viewer.request_numeric_data_bounds(DataExchangePacket.DataType.PING_DATA)
                    self.progress_emitter.emit(30)
                    logging.debug("Retrieving bounds for TSLP data...")
                    tslp_start_time, tslp_end_time, _ = self.main_window.viewer.request_numeric_data_bounds(DataExchangePacket.DataType.TSLP_DATA)
                    self.progress_emitter.emit(40)

                    logging.debug("Retrieving bounds for chat data...")
                    min_chat_id, max_chat_id = self.main_window.viewer.request_data_bounds(DataExchangePacket.DataType.CHAT)
                    chat_to_request = min(65535, max_chat_id, min(Config.CHAT_MESSAGE_CACHE, max_chat_id - min_chat_id) *
                                          max(1, len(self.main_window.viewer.current_reporter.get_players())))
                    self.progress_emitter.emit(50)

                    logging.debug("Retrieving bounds for tracked players...")
                    min_tracked_player_id, max_tracked_player_id = self.main_window.viewer.request_data_bounds(DataExchangePacket.DataType.TRACKED_PLAYER)
                    tracked_players_to_request = max_tracked_player_id - min_tracked_player_id
                    self.progress_emitter.emit(60)

                    logging.info("Retrieving data objects...")
                    self.progress_emitter.emit(70)

                    logging.debug("Retrieving %i chat messages..." % chat_to_request)
                    self.main_window.viewer.request_data(DataExchangePacket.DataType.CHAT,
                                                         [max_chat_id - index for index in range(chat_to_request)])
                    self.progress_emitter.emit(80)

                    logging.debug("Retrieving %i tracked players..." % tracked_players_to_request)
                    self.main_window.viewer.request_data(DataExchangePacket.DataType.TRACKED_PLAYER,
                                                         [index for index in range(tracked_players_to_request)])
                    self.progress_emitter.emit(90)

                except Exception as error:
                    self.error_emitter.emit(repr(error))
                    return

                self.progress_emitter.emit(100)
                self.success_emitter.emit()
