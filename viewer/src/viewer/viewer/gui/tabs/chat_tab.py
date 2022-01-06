#!/usr/bin/env python3

import operator
import re
import time
from typing import List

from PyQt5.QtCore import Qt, QThread, QStringListModel, pyqtSignal
from PyQt5.QtWidgets import QWidget, QVBoxLayout, QLabel, QHBoxLayout, QComboBox, QTextBrowser, QLineEdit, QPushButton, \
    QCompleter, QMessageBox

from ...config import Config
from ...util import Player, ChatMessage


class ChatTab(QWidget):
    """
    Displays the current chat on the server in realtime.
    """

    # FIXME: This is pretty Constantiam-oriented, make it more general
    username = re.compile("(?P<player>[A-Za-z0-9_]{3,16})")
    join_leave_message = re.compile(r"^%s (joined|left) the game$" % username.pattern)
    whisper_to_message = re.compile(r"^To %s: .*" % username.pattern)
    whisper_from_message = re.compile("^%s whispers: .*" % username.pattern)
    regular_text = re.compile("^<%s> .*" % username.pattern)
    green_text = re.compile("^<%s> >.*" % username.pattern)
    death_message = re.compile("^%s .+" % username.pattern)  # FIXME: Kinda a shitty pattern for death messages

    def __init__(self, main_window) -> None:
        super().__init__()

        self.main_window = main_window

        self._available_accounts = []
        self._chat_data = {}
        self._min_chat_id = 0
        self._max_chat_id = 0

        self._ignore_send = False
        self._completion_await_thread = None
        
        self.setObjectName("chat_tab")

        self.main_layout = QVBoxLayout(self)

        self.player_select_layout = QHBoxLayout()

        self.server_chat_label = QLabel(self)
        self.server_chat_label.setTextFormat(Qt.PlainText)
        self.server_chat_label.setScaledContents(False)
        self.player_select_layout.addWidget(self.server_chat_label)

        self.players_combo_box = QComboBox(self)
        self.player_select_layout.addWidget(self.players_combo_box)

        self.main_layout.addLayout(self.player_select_layout)

        self.chat_text_browser = QTextBrowser(self)
        self.main_layout.addWidget(self.chat_text_browser)

        self.chat_entry_layout = QHBoxLayout()

        self.player_name_completer = QCompleter()

        self.chat_edit = QLineEdit(self)
        self.chat_edit.setCompleter(self.player_name_completer)
        self.chat_entry_layout.addWidget(self.chat_edit)

        self.send_button = QPushButton(self)
        self.send_button.setEnabled(False)
        self.send_button.setAutoDefault(False)
        self.send_button.setFlat(False)

        self.chat_entry_layout.addWidget(self.send_button)

        self.main_layout.addLayout(self.chat_entry_layout)

        self.server_chat_label.setText(" Server chat, current account:")
        self.send_button.setText("Send message")

        self.main_window.connect_emitter.connect(self._reset_chat_data)
        self.main_window.disconnect_emitter.connect(self._reset_chat_data)
        self.main_window.reporter_selected_emitter.connect(lambda reporter: self._reset_chat_data())
        self.main_window.chat_data_emitter.connect(self._on_chat_data)
        self.main_window.chat_data_bounds_emitter.connect(self._on_chat_data_bounds)
        self.main_window.player_added_emitter.connect(self._on_player_added)
        self.main_window.player_removed_emitter.connect(self._on_player_removed)
        self.main_window.player_logged_out_emitter.connect(self._on_player_logged_out)

        self.player_name_completer.activated.connect(self._on_player_name_activated)
        self.chat_edit.textEdited.connect(self._on_text_edited)
        self.chat_edit.returnPressed.connect(self._on_return_pressed)
        self.players_combo_box.currentTextChanged.connect(lambda text: self._update_chat_browser())
        self.send_button.clicked.connect(self._on_send_message)

    def _reset_chat_data(self) -> None:
        self._chat_data.clear()

    def _update_chat_browser(self) -> None:
        previous_position = self.chat_text_browser.verticalScrollBar().sliderPosition()
        is_maximum = previous_position == self.chat_text_browser.verticalScrollBar().maximum()

        self.chat_text_browser.clear()
        if not self.players_combo_box.currentText():
            return

        current_username = ""
        for display_name, player in self._available_accounts:
            if display_name == self.players_combo_box.currentText():
                current_username = player.username
                break

        for username in self._chat_data:
            while len(self._chat_data[username][2]) > Config.CHAT_MESSAGE_CACHE:
                if self._chat_data[username][0] in self._chat_data[username][2]:
                    del self._chat_data[username][2][self._chat_data[username][0]]
                self._chat_data[username][0] += 1

            if username == current_username:
                for chat_message_id, chat_message in sorted(self._chat_data[username][2].items(),
                                                            key=operator.itemgetter(0)):
                    colour = Config.TEXT_COLOUR[:3]

                    message = chat_message.message

                    if self.join_leave_message.fullmatch(message):
                        colour = Config.CHAT_JOIN_LEAVE_COLOUR
                    elif self.whisper_to_message.fullmatch(message) or self.whisper_from_message.fullmatch(message):
                        colour = Config.WHISPER_COLOUR
                    elif self.green_text.fullmatch(message):
                        colour = Config.GREEN_COLOUR
                    elif self.death_message.fullmatch(message):
                        colour = Config.DEATH_COLOUR
                    elif not self.regular_text.fullmatch(message):
                        colour = Config.OTHER_COLOUR

                    # noinspection PyStringFormat
                    self.chat_text_browser.append("<span style=\"color: #%02x%02x%02x\">%s</span>" %
                                                  (*colour, message.replace("<", "&lt;").replace(">", "&gt;")))

        if is_maximum:
            self.chat_text_browser.verticalScrollBar().setValue(self.chat_text_browser.verticalScrollBar().maximum())
        else:
            self.chat_text_browser.verticalScrollBar().setValue(previous_position)

    # ----------------------------- General emitters ----------------------------- #

    def _on_chat_data(self, chat_data: List[ChatMessage]) -> None:
        for chat_message in chat_data:
            if not chat_message.username in self._chat_data:
                self._chat_data[chat_message.username] = [chat_message.chat_message_id,
                                                          chat_message.chat_message_id, {}]
            if chat_message.chat_message_id < self._chat_data[chat_message.username][0]:
                self._chat_data[chat_message.username][0] = chat_message.chat_message_id
            if chat_message.chat_message_id > self._chat_data[chat_message.username][1]:
                self._chat_data[chat_message.username][1] = chat_message.chat_message_id
            self._chat_data[chat_message.username][2][chat_message.chat_message_id] = chat_message

        self._update_chat_browser()

    def _on_chat_data_bounds(self, min_chat_id: int, max_chat_id: int) -> None:
        ...

        # self._min_chat_id = min_chat_id
        # self._max_chat_id = max_chat_id

    def _on_player_added(self, player: Player) -> None:
        self._available_accounts.append((player.display_name, player))
        self.players_combo_box.addItem(player.display_name)

        self._update_chat_browser()

    def _on_player_removed(self, player: Player) -> None:
        for display_name, player2 in self._available_accounts:
            if player == player2:
                self.players_combo_box.removeItem(self.players_combo_box.findText(display_name))
                self._available_accounts.remove((display_name, player))

        self._update_chat_browser()

    def _on_player_logged_out(self, player: Player, reason: str) -> None:
        ...

    # ----------------------------- Widget emitters ----------------------------- #

    def _on_player_name_activated(self, text: str) -> None:
        self._ignore_send = True

    def _on_text_edited(self) -> None:
        chat_edit_text = self.chat_edit.text()

        if chat_edit_text:
            self.send_button.setEnabled(True)
            if self.main_window.viewer is not None:
                current_reporter = self.main_window.viewer.current_reporter
                last_word = chat_edit_text.split(" ")[-1].lower()

                complete_names = []

                if current_reporter is not None and last_word:
                    for uuid, display_name in current_reporter.get_online_players().items():
                        display_name = display_name.lower()

                        if display_name.startswith(last_word):  # FIXME: Fix capitalization
                            complete_names.append(chat_edit_text + display_name.replace(last_word, "", 1))

                self.player_name_completer.setModel(QStringListModel(complete_names))
                self.chat_edit.setCompleter(self.player_name_completer)

        else:
            self.send_button.setEnabled(False)

    def _on_return_pressed(self) -> None:
        self._ignore_send = False
        # Need to do this as we'll get the completer suggestion after this
        self._completion_await_thread = ChatTab.CompletionAwaitThread(self)
        self._completion_await_thread.send_message_emitter.connect(self._on_send_message)
        self._completion_await_thread.start()

    def _on_send_message(self, checked) -> None:
        message = self.chat_edit.text()
        self.chat_edit.clear()

        if self.main_window.viewer is None:
            QMessageBox.warning(self, "Can't Send", "No viewer connected.")
        elif self.players_combo_box.currentText() == "":
            QMessageBox.warning(self, "Can't Send", "No player selected.")
        else:
            # Lol this is bad practice to send with the display name, but the client accepts display names anyway so
            # whatever, I'm lazy
            try:
                self.main_window.viewer.send_chat_message(self.players_combo_box.currentText(), message)
            except Exception as error:
                QMessageBox.warning(self, "Can't Send", str(error))

    # ----------------------------- Threads ----------------------------- #

    class CompletionAwaitThread(QThread):

        send_message_emitter = pyqtSignal(bool)

        def __init__(self, chat_tab) -> None:
            super().__init__()

            self.chat_tab = chat_tab

        def run(self) -> None:
            time.sleep(0.1)
            if not self.chat_tab._ignore_send:
                self.send_message_emitter.emit(False)
            self.chat_tab._ignore_send = False
