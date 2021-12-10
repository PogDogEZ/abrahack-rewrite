#!/usr/bin/env python3

import logging
import time
from threading import Lock

from PyQt5.QtCore import QThread, pyqtSignal
from PyQt5.QtGui import QIntValidator
from PyQt5.QtWidgets import QWidget, QGridLayout, QVBoxLayout, QLabel, QHBoxLayout, QLineEdit, QPushButton, QSpacerItem, \
    QSizePolicy, QListWidget, QListWidgetItem, QMessageBox

from pyclient.networking.connection import Connection
from pyclient.networking.handlers.default import DefaultHandler
from pyclient.networking.handlers.handshake import HandShakeHandler
from viewer import Viewer, Reporter, DataExchangePacket
from viewer.config import Config
from viewer.gui.dialogs.progess_dialog import ProgressDialog


class ReporterTab(QWidget):

    def __init__(self, main_window) -> None:
        super().__init__()

        self.main_window = main_window

        self._background_thread = ReporterTab.BackgroundThread(self.main_window, self)

        self._progress = None
        self._log_handler = None
        self._connect_thread = None

        self._available_reporters = []
        
        self.setObjectName("reporter_tab")

        self.main_layout = QGridLayout(self)

        self.server_layout = QVBoxLayout()

        self.current_server_label = QLabel(self)
        self.server_layout.addWidget(self.current_server_label)

        self.ip_address_label = QLabel(self)
        self.server_layout.addWidget(self.ip_address_label)

        self.ip_entry_layout = QHBoxLayout()

        self.host_name_edit = QLineEdit(self)
        self.host_name_edit.setMaxLength(100)
        self.ip_entry_layout.addWidget(self.host_name_edit)

        self.colon_label = QLabel(self)
        self.ip_entry_layout.addWidget(self.colon_label)

        self.port_edit = QLineEdit(self)
        self.port_edit.setMaxLength(6)
        self.port_edit.setValidator(QIntValidator(0, 65535))
        self.ip_entry_layout.addWidget(self.port_edit)

        self.ip_entry_layout.setStretch(0, 7)
        self.ip_entry_layout.setStretch(2, 3)
        self.server_layout.addLayout(self.ip_entry_layout)

        self.buttons_layout = QHBoxLayout()

        self.connect_button = QPushButton(self)
        self.buttons_layout.addWidget(self.connect_button)

        self.disconnect_button = QPushButton(self)
        self.disconnect_button.setEnabled(False)
        self.buttons_layout.addWidget(self.disconnect_button)

        self.server_layout.addLayout(self.buttons_layout)
        self.server_layout.addItem(QSpacerItem(20, 40, QSizePolicy.Minimum, QSizePolicy.Expanding))

        self.main_layout.addLayout(self.server_layout, 0, 0, 1, 1)

        self.reporters_layout = QVBoxLayout()
        self.current_reporter_label = QLabel(self)

        self.reporters_layout.addWidget(self.current_reporter_label)

        self.reporters_list = QListWidget(self)
        self.reporters_layout.addWidget(self.reporters_list)

        self.buttons_layout = QHBoxLayout()

        self.select_button = QPushButton(self)
        self.select_button.setEnabled(False)
        self.buttons_layout.addWidget(self.select_button)

        self.deselect_button = QPushButton(self)
        self.deselect_button.setEnabled(False)
        self.buttons_layout.addWidget(self.deselect_button)

        self.refresh_button = QPushButton(self)
        self.refresh_button.setEnabled(False)
        self.buttons_layout.addWidget(self.refresh_button)

        self.reporters_layout.addLayout(self.buttons_layout)
        self.main_layout.addLayout(self.reporters_layout, 0, 1, 1, 1)

        self.current_server_label.setText("Current server: not connected")
        self.ip_address_label.setText("IP Address:")
        self.host_name_edit.setText("pogdog.cam")
        self.colon_label.setText(":")
        self.port_edit.setText("5001")
        self.connect_button.setText("Connect")
        self.disconnect_button.setText("Disconnect")
        self.current_reporter_label.setText("Current reporter: no reporter")
        self.select_button.setText("Select reporter")
        self.deselect_button.setText("Deselect reporter")
        self.refresh_button.setText("Refresh reporter")

        self.main_window.reporter_added_emitter.connect(self._on_reporter_added)
        self.main_window.reporter_removed_emitter.connect(self._on_reporter_removed)
        # self.reporters_list.itemClicked
        self.reporters_list.itemSelectionChanged.connect(lambda: self.select_button.setEnabled(len(self.reporters_list.selectedItems())))
        self.reporters_list.itemDoubleClicked.connect(lambda: self._on_select_reporter(False))
        self.connect_button.clicked.connect(self._on_connect)
        self.disconnect_button.clicked.connect(self._on_disconnect)
        self.select_button.clicked.connect(self._on_select_reporter)
        self.deselect_button.clicked.connect(self._on_deselect_reporter)
        self.refresh_button.clicked.connect(self._on_refresh_reporter)

        self._background_thread.start()

    def _on_reporter_added(self, reporter: Reporter) -> None:
        widget_item = QListWidgetItem(reporter.handler_name, self.reporters_list)
        widget_item.setToolTip("Reports for server %s:%i." % (reporter.host, reporter.port))
        self.reporters_list.addItem(widget_item)
        self._available_reporters.append((widget_item, reporter))

    def _on_reporter_removed(self, reporter: Reporter) -> None:
        for widget_item, reporter in self._available_reporters:
            if reporter == reporter:
                self.reporters_list.takeItem(self.reporters_list.row(widget_item))
                self._available_reporters.remove((widget_item, reporter))
                break

    def _on_connect(self, checked: bool) -> None:
        if self._progress is None and (self.main_window.connection is None or not self.main_window.connection.connected):
            if not self.port_edit.text().isnumeric():
                QMessageBox.warning(self, "Invalid Port", "The port must be a number.")
                return

            ip_address = self.host_name_edit.text(), int(self.port_edit.text())
            self.main_window.connection = Connection(*ip_address, 6)

            self._progress = ProgressDialog(self, "Connecting to %s:%i..." % ip_address, "Connecting")
            self._connect_thread = ReporterTab.ConnectThread(self.main_window)

            self._connect_thread.progress_emitter.connect(self._progress.progress_bar.setValue)
            self._connect_thread.error_emitter.connect(self._on_connect_error)
            self._connect_thread.success_emitter.connect(self._on_connect_success)

            self._connect_thread.start()

    def _on_connect_error(self, reason: str) -> None:
        if self._progress is not None:
            self._progress.close()
            self._progress = None

        QMessageBox.critical(self, "Couldn't Connect", reason)

    def _on_connect_success(self) -> None:
        if self._progress is not None:
            self._progress.close()
            self._progress = None

    def _on_disconnect(self, checked: bool) -> None:
        if self.main_window.connection is not None and self.main_window.connection.connected:
            # self.main_window.viewer.exit()
            self.main_window.connection.exit("Disconnected.")

            self.main_window.connection = None
            self.main_window.viewer = None

            # NormalDialog("Disconnected.", "Disconnected")

    def _on_select_reporter(self, checked: bool) -> None:
        if self.main_window.viewer is not None:
            for widget_item, reporter in self._available_reporters:
                if widget_item == self.reporters_list.currentItem():
                    self.main_window.viewer.current_reporter = reporter
                    return

    def _on_deselect_reporter(self, checked: bool) -> None:
        if self.main_window.viewer is not None and self.main_window.viewer.current_reporter is not None:
            self.main_window.viewer.current_reporter = None

    def _on_refresh_reporter(self, checked: bool) -> None:
        if self.main_window.viewer is not None and self.main_window.viewer.current_reporter is not None:
            previous_reporter = self.main_window.viewer.current_reporter
            self.main_window.viewer.current_reporter = None
            self.main_window.viewer.current_reporter = previous_reporter

    class BackgroundThread(QThread):

        connection_state_emitter = pyqtSignal(str)
        reporter_state_emitter = pyqtSignal(str)

        def __init__(self, main_window, reporter_tab) -> None:
            super().__init__()

            self.main_window = main_window
            self.reporter_tab = reporter_tab

        def run(self) -> None:
            while True:
                time.sleep(0.1)

                if self.main_window.connection is not None and self.main_window.connection.connected:
                    current_server = "%s:%i" % (self.main_window.connection.host, self.main_window.connection.port)
                    self.reporter_tab.connect_button.setEnabled(False)
                    self.reporter_tab.disconnect_button.setEnabled(True)
                else:
                    current_server = "(not connected)"
                    self.reporter_tab.connect_button.setEnabled(True)
                    self.reporter_tab.disconnect_button.setEnabled(False)

                self.reporter_tab.current_server_label.setText("Current server: %s" % current_server)

                if self.main_window.viewer is not None and self.main_window.viewer.current_reporter is not None:
                    current_reporter = self.main_window.viewer.current_reporter.handler_name
                    self.reporter_tab.deselect_button.setEnabled(True)  # FIXME: This seems to crash on windows?
                    self.reporter_tab.refresh_button.setEnabled(True)
                else:
                    current_reporter = "(no reporter)"
                    self.reporter_tab.deselect_button.setEnabled(False)
                    self.reporter_tab.refresh_button.setEnabled(False)
                self.reporter_tab.current_reporter_label.setText("Current reporter: %s" % current_reporter)

    class ConnectThread(QThread):

        progress_emitter = pyqtSignal(int)
        error_emitter = pyqtSignal(str)
        success_emitter = pyqtSignal()

        def __init__(self, main_window) -> None:
            super().__init__()

            self.main_window = main_window

            self.message_lock = Lock()
            self.messages = []

        def run(self) -> None:
            time.sleep(0.3)

            logging.debug("Attempting to connect to the server...")
            try:
                self.main_window.connection.connect()
                self.main_window.viewer = Viewer(self.main_window.connection)
            except Exception as error:
                logging.warning("Error while connecting:")
                logging.error(error, exc_info=True)
                self.error_emitter.emit(str(error))
                return

            self.progress_emitter.emit(10)  # Lol these are arbitrary but who cares

            logging.debug("Waiting for login...")
            while self.main_window.connection.connected and not isinstance(self.main_window.connection.handler,
                                                                           DefaultHandler):
                if isinstance(self.main_window.connection.handler, HandShakeHandler):
                    if self.main_window.connection.handler.state == HandShakeHandler.State.HAND_SHAKE:
                        self.progress_emitter.emit(30)
                    elif self.main_window.connection.handler.state == HandShakeHandler.State.ENCRYPTION:
                        self.progress_emitter.emit(50)
                    elif self.main_window.connection.handler.state == HandShakeHandler.State.LOGIN:
                        self.progress_emitter.emit(70)

                time.sleep(0.1)

            self.progress_emitter.emit(80)

            logging.debug("Waiting for YC init...")
            if self.main_window.connection.connected:
                self.main_window.viewer.init()

                while not self.main_window.viewer.initialized:
                    if not self.main_window.connection.connected:
                        self.error_emitter.emit(self.main_window.connection.exit_reason)
                        return

                    time.sleep(0.1)

                self.progress_emitter.emit(90)

            else:
                self.error_emitter.emit(self.main_window.connection.exit_reason)
                return

            logging.debug("Connected.")
            self.progress_emitter.emit(100)
            self.success_emitter.emit()
