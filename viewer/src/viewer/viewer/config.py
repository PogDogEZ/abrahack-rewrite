#!/usr/bin/env python3

import cv2
from PyQt5.QtGui import QFont


# noinspection PyUnresolvedReferences
class Config:  # TODO: Config saving and loading
    """
    The config for the viewer, this does not reflect the config of the client, instead it provides options only for the
    appearance and other behaviours of the viewer.
    """

    _COLOUR_MAPS = {
        "Autumn": cv2.COLORMAP_AUTUMN,
        "Bone": cv2.COLORMAP_BONE,
        "Jet": cv2.COLORMAP_JET,
        "Winter": cv2.COLORMAP_WINTER,
        "Rainbow": cv2.COLORMAP_RAINBOW,
        "Ocean": cv2.COLORMAP_OCEAN,
        "Summer": cv2.COLORMAP_SUMMER,
        "Spring": cv2.COLORMAP_SPRING,
        "Cool": cv2.COLORMAP_COOL,
        "HSV": cv2.COLORMAP_HSV,
        "Pink": cv2.COLORMAP_PINK,
        "Hot": cv2.COLORMAP_HOT,
        "Parula": cv2.COLORMAP_PARULA,
        "Cividis": cv2.COLORMAP_CIVIDIS,
        "Inferno": cv2.COLORMAP_INFERNO,
        "Magma": cv2.COLORMAP_MAGMA,
        "Viridis": cv2.COLORMAP_VIRIDIS,
        "Plasma": cv2.COLORMAP_PLASMA,
        "Twilight": cv2.COLORMAP_TWILIGHT,
        "Twilight shifted": cv2.COLORMAP_TWILIGHT_SHIFTED,
        "Turbo": cv2.COLORMAP_TURBO,
        "Deep green": cv2.COLORMAP_DEEPGREEN,
    }

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
    DISABLED_TEXT_COLOUR = (0, 0, 0, 128)
    BRIGHT_TEXT_COLOUR = (255, 255, 255, 255)

    HIGHLIGHT_COLOUR = (48, 141, 199, 255)

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

    SCALE_SENSITIVITY = 1000

    DECAY_RATE = 5
    INTERPOLATION_SIZE = (2, 2)
    INTERPOLATION_TYPE = cv2.INTER_LINEAR
    REGION_SIZE = (512, 512)
    COLOUR_MAP = _COLOUR_MAPS["Hot"]
    SELECTION_COLOUR = (255, 0, 0)
    LINE_COLOUR = (195, 195, 195)
    HIGHWAY_COLOUR = (150, 150, 150)
    PLAYER_COLOUR = (255, 0, 0)
