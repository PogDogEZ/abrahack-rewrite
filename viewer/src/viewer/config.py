#!/usr/bin/env python3

from PyQt5.QtGui import QFont


class Config:

    # MS auth stuff from Nathan
    _CLIENT_ID = "83fba303-ac6f-4c76-831a-c908108f13c6"
    _CLIENT_SECRET = "PQF7Q~B4uVSljll6GogkZvNA~01wN_n~rUPd4"
    _REDIRECT_URI = "https://pogdog.azurewebsites.net/.auth/login/aad/callback"

    FONT = QFont("Cantarell", 11)  # Calibri for Windows
    LARGE_FONT = QFont("Cantarell", 12)
    SMALL_FONT = QFont("Cantarell", 8)

    TYPE_FONT = QFont("Source Code Pro", 11)  # Consolas for Windows
    LARGE_TYPE_FONT = QFont("Source Code Pro", 12)
    SMALL_TYPE_FONT = QFont("Source Code Pro", 8)

    LAG_THRESHOLD = 2000

    # RGBA
    TEXT_COLOUR = (0, 0, 0, 255)
    BRIGHT_TEXT_COLOUR = (255, 255, 255, 255)
    WINDOW_COLOUR = (239, 239, 239, 255)
    BUTTON_COLOUR = (239, 239, 239, 255)
    BASE_COLOUR = (255, 255, 255, 255)
    LIGHT_COLOUR = (255, 255, 255, 255)
    MID_COLOUR = (184, 184, 184, 255)
    DARK_COLOUR = (159, 159, 159, 255)

    GRAPH_LINE_THICKNESS = 2  # 1 for smaller screens
    GRAPH_FONT_SCALE = 0.5  # 0.3 for smaller screens

    CHAT_MESSAGE_CACHE = 150
    CHAT_JOIN_LEAVE_COLOUR = (179, 179, 179)
    WHISPER_COLOUR = (130, 77, 214)
    GREEN_COLOUR = (0, 150, 0)
    DEATH_COLOUR = (255, 0, 0)
    OTHER_COLOUR = (255, 159, 48)

    CHUNK_SIZE = (8, 8)
    GRID_SCALE = (2, 2)

    SCALE_SENSITIVITY = 15

    BACKGROUND_COLOUR = (184, 184, 184)
    LINE_COLOUR = (195, 195, 195)
    HIGHWAY_COLOUR = (150, 150, 150)
    PLAYER_COLOUR = (255, 0, 0)

    CURRENT_SCAN_COLOUR = (255, 255, 255)
    WAITING_COLOUR = (169, 49, 196)
    LOADED_COLOUR = (6, 184, 38)
    UNLOADED_COLOUR = (7, 7, 227)

