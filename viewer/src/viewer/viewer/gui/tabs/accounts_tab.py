#!/usr/bin/env python3

import time
from threading import Lock

import minecraft_launcher_lib
from PyQt5.QtCore import Qt, QThread, pyqtSignal
from PyQt5.QtWidgets import QWidget, QHBoxLayout, QVBoxLayout, QLabel, QPushButton, QFrame, QGridLayout, \
    QLineEdit, QLayout, QSpacerItem, QSizePolicy, QTreeWidget, QTreeWidgetItem, QMessageBox
from ..dialogs.progess_dialog import ProgressDialog
from ...util import Player


class AccountsTab(QWidget):
    """
    The accounts tab provides the details of the players current online, such as their position, etc, it also allows
    you to authenticate accounts.
    """

    def __init__(self, main_window) -> None:
        super().__init__()

        self.main_window = main_window

        self._progress = None
        self._mojang_login_thread = None
        self._microsoft_login_thread = None
        self._logout_thread = None

        self._accounts_list = []
        
        self.setObjectName("accounts_tab")

        self.main_layout = QHBoxLayout(self)

        self.accounts_layout = QVBoxLayout()

        # self.current_accounts_label = QLabel(self)
        # self.accounts_layout.addWidget(self.current_accounts_label)

        self.accounts_tree_widget = QTreeWidget(self)
        # self.accounts_tree_view.setModel(AccountsTab.PlayersModel(self.main_window))
        self.accounts_layout.addWidget(self.accounts_tree_widget)

        self.button_layout = QHBoxLayout()

        self.goto_button = QPushButton(self)
        self.goto_button.setEnabled(False)
        self.button_layout.addWidget(self.goto_button)

        self.remove_button = QPushButton(self)
        self.remove_button.setEnabled(False)
        self.button_layout.addWidget(self.remove_button)

        self.accounts_layout.addLayout(self.button_layout)
        self.main_layout.addLayout(self.accounts_layout)

        self.login_layout = QVBoxLayout()

        self.login_label = QLabel(self)
        self.login_label.setFrameShape(QFrame.NoFrame)
        self.login_label.setAlignment(Qt.AlignLeading | Qt.AlignLeft | Qt.AlignTop)

        self.login_layout.addWidget(self.login_label)

        self.details_layout = QGridLayout()
        self.details_layout.setSizeConstraint(QLayout.SetDefaultConstraint)

        self.username_edit = QLineEdit(self)
        self.details_layout.addWidget(self.username_edit, 2, 1, 1, 1)

        self.password_edit = QLineEdit(self)
        self.password_edit.setEchoMode(QLineEdit.Password)
        self.details_layout.addWidget(self.password_edit, 3, 1, 1, 1)

        self.passoword_label = QLabel(self)
        self.details_layout.addWidget(self.passoword_label, 3, 0, 1, 1)

        self.username_label = QLabel(self)
        self.details_layout.addWidget(self.username_label, 2, 0, 1, 1)

        self.login_layout.addLayout(self.details_layout)

        self.mojang_button = QPushButton(self)
        self.mojang_button.setEnabled(False)
        self.login_layout.addWidget(self.mojang_button)

        self.microsoft_button = QPushButton(self)
        self.microsoft_button.setEnabled(False)
        self.login_layout.addWidget(self.microsoft_button)

        self.login_layout.addItem(QSpacerItem(20, 40, QSizePolicy.Minimum, QSizePolicy.Expanding))

        self.main_layout.addLayout(self.login_layout)

        # self.current_accounts_label.setText(" Current accounts (0):")
        self.accounts_tree_widget.setHeaderLabel(" Current accounts (0):")
        self.goto_button.setText("Goto account")
        self.remove_button.setText("Remove account")
        self.login_label.setText("Login new account:")
        self.username_label.setText("Username:")
        self.passoword_label.setText("Password:")
        self.mojang_button.setText("Mojang login")
        self.microsoft_button.setText("Microsoft login")

        self.main_window.disconnect_emitter.connect(self._update_accounts_header)
        self.main_window.reporter_unselected_emitter.connect(self._update_accounts_header)
        self.main_window.player_added_emitter.connect(self._on_player_added)
        self.main_window.player_removed_emitter.connect(self._on_player_removed)
        self.main_window.player_logged_in_emitter.connect(self._on_player_updated)
        self.main_window.player_logged_out_emitter.connect(lambda player, reason: self._on_player_updated(player))
        self.main_window.player_updated_emitter.connect(self._on_player_updated)

        self.accounts_tree_widget.itemSelectionChanged.connect(self._on_item_selection_changed)
        self.goto_button.clicked.connect(self._on_goto)
        self.remove_button.clicked.connect(self._on_remove)
        self.username_edit.textEdited.connect(self._on_username_edit)
        self.username_edit.returnPressed.connect(lambda: self.password_edit.setFocus())
        self.password_edit.textEdited.connect(self._on_password_edit)
        self.password_edit.returnPressed.connect(lambda: self._on_mojang_login(False))
        self.mojang_button.clicked.connect(self._on_mojang_login)
        self.microsoft_button.clicked.connect(self._on_microsoft_login)

    # ----------------------------- Convenience methods ----------------------------- #

    def _get_item_widget(self, player: Player) -> QTreeWidgetItem:
        for item_widget, player2 in self._accounts_list:
            if player == player2:
                return item_widget

        # noinspection PyTypeChecker
        return None

    def _get_player(self, item_widget: QTreeWidgetItem) -> Player:
        for item_widget2, player in self._accounts_list:
            if item_widget == item_widget2:
                return player

        # noinspection PyTypeChecker
        return None

    def _update_accounts_header(self) -> None:
        if self.main_window.viewer is not None and self.main_window.viewer.current_reporter is not None:
            player_count = len(self.main_window.viewer.current_reporter.get_players())
        else:
            player_count = 0

        self.accounts_tree_widget.setHeaderLabel(" Current accounts (%i):" % player_count)

    # noinspection PyMethodMayBeStatic
    def _update_player_data(self, item_widget: QTreeWidgetItem, player: Player) -> QTreeWidgetItem:
        item_widget.setText(0, player.display_name)
        required_children = 7 if player.logged_in else 1

        if item_widget.childCount() != required_children:
            item_widget.takeChildren()
            for index in range(required_children):
                item_widget.addChild(QTreeWidgetItem([]))

        item_widget.child(0).setText(0, "Logged in: %s" % player.logged_in)

        if player.logged_in:
            item_widget.child(1).setText(0, "Position: %.1f, %.1f, %.1f" % (player.position.x, player.position.y,
                                                                           player.position.z))
            item_widget.child(1).setToolTip(0, "%.1f, %.1f, %.1f" % (player.position.x, player.position.y,
                                                                     player.position.z))
            item_widget.child(2).setText(0, "Angle: %.1f, %.1f" % (player.angle.yaw, player.angle.pitch))
            item_widget.child(2).setToolTip(0, "%.1f, %.1f" % (player.angle.yaw, player.angle.pitch))
            item_widget.child(3).setText(0, "Dimension: %i" % player.dimension)
            item_widget.child(4).setText(0, "Health: %.1f" % player.health)
            item_widget.child(5).setText(0, "Hunger: %i" % player.food)
            item_widget.child(6).setText(0, "Saturation: %.1f" % player.saturation)

        return item_widget

    # ----------------------------- General emitters ----------------------------- #

    def _on_player_added(self, player: Player) -> None:
        item_widget = self._update_player_data(QTreeWidgetItem([]), player)

        self._accounts_list.append((item_widget, player))
        self.accounts_tree_widget.addTopLevelItem(item_widget)

        self._update_accounts_header()

        # self.accounts_tree_widget.
        # self._accounts_list.append(repr(player))
        # self.accounts_tree_view.setModel(QStringListModel(self._accounts_list))

    def _on_player_removed(self, player: Player) -> None:
        item_widget = self._get_item_widget(player)
        if item_widget is not None:
            self.accounts_tree_widget.takeTopLevelItem(self.accounts_tree_widget.indexOfTopLevelItem(item_widget))
            self._accounts_list.remove((item_widget, player))

        self._update_accounts_header()

    def _on_player_updated(self, player: Player) -> None:
        item_widget = self._get_item_widget(player)
        if item_widget is not None:
            # item_widget.takeChildren()  # Clear the widget and re-add all the data
            self._update_player_data(item_widget, player)

    # ----------------------------- Widget emitters ----------------------------- #

    def _on_item_selection_changed(self) -> None:
        player = self._get_player(self.accounts_tree_widget.currentItem())
        if player is not None:  # We know we have a valid player selected, not just some player info
            self.goto_button.setEnabled(True)
            self.remove_button.setEnabled(True)

        else:  # No players selected, so disable the buttons
            self.goto_button.setEnabled(False)
            self.remove_button.setEnabled(False)

    def _on_goto(self, checked: bool) -> None:  # TODO: Goto the selected player
        ...

    def _on_remove(self, checked: bool) -> None:
        if self.main_window.viewer is None or self.main_window.viewer.current_reporter is None:
            QMessageBox.warning(self, "Error", "No reporter is currently selected.")  # TODO: Should this be silent?
            return

        player = self._get_player(self.accounts_tree_widget.currentItem())
        if player is not None:
            self._logout_thread = AccountsTab.LogoutThread(self.main_window, player)
            self._logout_thread.error_emitter.connect(lambda error: QMessageBox.warning(self, "Error", error))
            self._logout_thread.start()

    def _on_username_edit(self, text: str) -> None:
        if not text or not self.password_edit.text():
            self.mojang_button.setEnabled(False)
            self.microsoft_button.setEnabled(False)
        else:
            self.mojang_button.setEnabled(True)
            self.microsoft_button.setEnabled(True)

    def _on_password_edit(self, text: str) -> None:
        if not text or not self.username_edit.text():
            self.mojang_button.setEnabled(False)
            self.microsoft_button.setEnabled(False)
        else:
            self.mojang_button.setEnabled(True)
            self.microsoft_button.setEnabled(True)

    def _on_mojang_login(self, checked: bool) -> None:
        if not self.username_edit.text() or not self.password_edit.text() or self.main_window.viewer is None or \
                self.main_window.viewer.current_reporter is None:
            return

        self._progress = ProgressDialog(self, "Authenticating...", "Mojang Auth")
        self._mojang_login_thread = AccountsTab.MojangLoginThread(self.main_window, self.username_edit.text(),
                                                                  self.password_edit.text())

        self._mojang_login_thread.progress_emitter.connect(self._progress.progress_bar.setValue)
        self._mojang_login_thread.error_emitter.connect(self._on_login_error)
        self._mojang_login_thread.success_emitter.connect(self._on_login_success)

        self._mojang_login_thread.start()

    def _on_microsoft_login(self, checked: bool) -> None:
        if not self.username_edit.text() or not self.password_edit.text() or self.main_window.viewer is None or \
                self.main_window.viewer.current_reporter is None:
            return

        QMessageBox.warning(self, "Not Implemented", "Microsoft login is not implemented yet.")

    # ----------------------------- Virtual emitters ----------------------------- #

    def _on_login_error(self, error: str) -> None:
        self.password_edit.clear()
        self.mojang_button.setEnabled(False)
        self.microsoft_button.setEnabled(False)

        if self._progress is not None:
            self._progress.close()
            self._progress = None

        QMessageBox.critical(self, "Login Error", error)

    def _on_login_success(self) -> None:
        self.username_edit.clear()
        self.password_edit.clear()
        self.username_edit.setFocus()
        self.mojang_button.setEnabled(False)
        self.microsoft_button.setEnabled(False)

        if self._progress is not None:
            self._progress.close()
            self._progress = None

    class MojangLoginThread(QThread):
        """
        Login thread for authenticating Mojang accounts.
        """

        progress_emitter = pyqtSignal(int)
        error_emitter = pyqtSignal(str)
        success_emitter = pyqtSignal()

        def __init__(self, main_window, username: str, password: str):
            super().__init__()

            self.main_window = main_window
            self.username = username
            self.password = password

            self.message_lock = Lock()
            self.messages = []

        def run(self) -> None:
            time.sleep(0.3)
            self.progress_emitter.emit(20)

            try:
                auth_token = minecraft_launcher_lib.account.login_user(self.username, self.password)
                if "error" in auth_token:
                    raise Exception(auth_token["errorMessage"])

                self.progress_emitter.emit(70)

                self.main_window.viewer.login_account(self.username, legacy=False,
                                                      client_token=auth_token["clientToken"],
                                                      access_token=auth_token["accessToken"])
                self.progress_emitter.emit(100)
                self.success_emitter.emit()

            except Exception as error:
                self.error_emitter.emit(repr(error))

    class MicrosoftLoginThread(QThread):
        ...

    class LogoutThread(QThread):

        error_emitter = pyqtSignal(str)
        success_emitter = pyqtSignal()

        def __init__(self, main_window, player: Player) -> None:
            super().__init__()

            self.main_window = main_window
            self.player = player

        def run(self) -> None:
            try:
                self.main_window.viewer.logout_account(self.player.username)
                self.success_emitter.emit()
            except Exception as error:
                self.error_emitter.emit(str(error))
