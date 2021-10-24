#!/usr/bin/env python3

import os
from tkinter import *


class Config:

    _CLIENT_ID = "83fba303-ac6f-4c76-831a-c908108f13c6"
    _CLIENT_SECRET = "PQF7Q~B4uVSljll6GogkZvNA~01wN_n~rUPd4"
    _REDIRECT_URI = "https://pogdog.azurewebsites.net/.auth/login/aad/callback"

    if os.name in ("posix",):
        FONT = ("Calibri", 11, "bold")
        TYPE_FONT = ("Source Code Pro Semibold", 10)
        SMALL_TYPE_FONT = ("Source Code Pro Semibold", 9)
        LARGE_FONT = ("Calibri", 12, "bold")
        SMALL_FONT = ("Calibri", 9, "bold")

        VIEWER_SIZE = (1024, 768)
        BORDER_WIDTH = 2

    else:
        FONT = ("Calibri", 10, "bold")
        TYPE_FONT = ("Cascadia Code SemiBold", 10)
        SMALL_TYPE_FONT = ("Cascadia Code SemiBold", 9)
        LARGE_FONT = ("Calibri", 11, "bold")
        SMALL_FONT = ("Calibri", 9, "bold")

        VIEWER_SIZE = (576, 432)
        BORDER_WIDTH = 1
        
    # VIEWER_SIZE = (1024, 768)
    # VIEWER_SIZE = (768, 576)

    CHUNK_SIZE = (8, 8)
    GRID_SCALE = (2, 2)

    RELIEF = SOLID

    SCALE_SENSITIVITY = 15  # TODO: Settings for these

    TEXT_COLOUR = (0, 0, 0)
    HIGHLIGHT_COLOUR = (236, 236, 236)
    WINDOW_COLOUR = (235, 235, 235)
    WIDGET_COLOUR = (200, 200, 200)

    BACKGROUND_COLOUR = (184, 184, 184)
    LINE_COLOUR = (195, 195, 195)
    HIGHWAY_COLOUR = (150, 150, 150)
    PLAYER_COLOUR = (255, 0, 0)

    CURRENT_SCAN_COLOUR = (255, 255, 255)
    WAITING_COLOUR = (169, 49, 196)
    LOADED_COLOUR = (6, 184, 38)
    UNLOADED_COLOUR = (7, 7, 227)

