#!/usr/bin/env python3

from PyQt5.QtWidgets import QWidget


class ConfigTab(QWidget):

    def __init__(self) -> None:
        super().__init__()

        self.setObjectName("config_tab")
