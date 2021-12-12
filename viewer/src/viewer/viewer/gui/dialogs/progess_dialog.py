#!/usr/bin/env python3

import logging
import time
from logging import LogRecord, Handler
from threading import RLock

from PyQt5.QtCore import Qt, QMetaObject, QThread, pyqtSignal
from PyQt5.QtGui import QCloseEvent, QTextCursor
from PyQt5.QtWidgets import QWidget, QDialog, QVBoxLayout, QLabel, QProgressBar, QTextEdit

from ...config import Config


class ProgressDialog(QDialog):  # TODO: Make this a message box

    # INSTANCE = None

    def __init__(self, parent: QWidget, progress_text: str, window_title: str = "Progress",
                 show_log_messages: bool = True) -> None:
        """
        :param parent: The parent widget.
        :param progress_text: The text to display above the progress bar.
        :param window_title: The title of the window.
        :param show_log_messages: Whether to show log messages in the extra text area.
        """

        # if ProgressDialog.INSTANCE is not None:
        #     ProgressDialog.INSTANCE.setWindowState(ProgressDialog.INSTANCE.windowState() & ~Qt.WindowMinimized |
        #                                            Qt.WindowActive)
        #     ProgressDialog.INSTANCE.activateWindow()
        #     return

        super().__init__(parent)
        # ProgressDialog.INSTANCE = self

        self._log_handle_thread = ProgressDialog.LogHandleThread()
        self._fake_log_handler = ProgressDialog.FakeLogHandler(self._log_handle_thread)

        self.setObjectName("progress_dialog")
        # self.setStyleSheet("background-color: white")
        self.resize(1, 1)  # self.resize(720, 300)

        self.main_layout = QVBoxLayout(self)
        self.main_layout.setSpacing(6)
        self.main_layout.setContentsMargins(6, 6, 6, 6)

        self.text_label = QLabel(self)
        self.text_label.setAlignment(Qt.AlignHCenter | Qt.AlignTop)
        self.main_layout.addWidget(self.text_label)

        # self.current_progress_label = QLabel(self)
        # self.current_progress_label.setAlignment(Qt.AlignHCenter | Qt.AlignTop)
        # self.main_layout.addWidget(self.current_progress_label)

        self.progress_bar = QProgressBar(self)
        self.progress_bar.setValue(0)
        self.main_layout.addWidget(self.progress_bar)

        self.extra_text_edit = QTextEdit(self)
        self.extra_text_edit.setFont(Config.SMALL_TYPE_FONT)
        bounding_rect = self.extra_text_edit.fontMetrics().boundingRect("M" * 60)  # Hacky but whatever
        self.extra_text_edit.setFixedWidth(bounding_rect.width())
        self.extra_text_edit.setFixedHeight(bounding_rect.height() * 8)
        self.extra_text_edit.setUndoRedoEnabled(False)
        # self.extra_text_edit.setLineWrapMode(QTextEdit.NoWrap)
        self.extra_text_edit.setReadOnly(True)
        self.main_layout.addWidget(self.extra_text_edit)

        self.setWindowTitle(window_title)
        self.text_label.setText(progress_text)

        QMetaObject.connectSlotsByName(self)

        if show_log_messages:
            logging.getLogger("").addHandler(self._fake_log_handler)  # Get all the sent log messages
            self._log_handle_thread.message_emitter.connect(self._on_message)
            self._log_handle_thread.start()

        self.show()

    def closeEvent(self, close_event: QCloseEvent) -> None:
        ProgressDialog.INSTANCE = None
        self._log_handle_thread._close = True
        logging.getLogger("").removeHandler(self._fake_log_handler)

        super().closeEvent(close_event)

    def _on_message(self, message: str) -> None:
        self.extra_text_edit.append(message)
        self.extra_text_edit.moveCursor(QTextCursor.End)

    class FakeLogHandler(Handler):

        def __init__(self, thread) -> None:
            super().__init__()

            self.thread = thread

        def handle(self, record: LogRecord) -> bool:
            with self.thread.message_lock:  # Hand this off to the thread
                self.thread.messages.append("[%s] %s" % (record.levelname, record.getMessage()))  # This format looks nicer on screen
            return False

    class LogHandleThread(QThread):
        """
        Log handle thread, so we can add messages to the extra text edit without crashing PyQT.
        """

        message_emitter = pyqtSignal(str)

        def __init__(self) -> None:
            super().__init__()

            self.message_lock = RLock()
            self.messages = []

            self._close = False

        def run(self) -> None:
            while not self._close:
                with self.message_lock:
                    if len(self.messages) > 0:
                        self.message_emitter.emit(self.messages.pop(0))

                time.sleep(0.05)  # Don't slow down other threads
