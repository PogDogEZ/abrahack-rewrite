#!/usr/bin/env python3

import os

import cv2
import numpy as np

from typing import Pattern, List, Match, Tuple, Dict, Union

from PIL import ImageFont, ImageDraw, Image
from PyQt5.QtCore import QStandardPaths
from PyQt5.QtGui import QImage, QFontDatabase


def array_to_qt_image(cv_img: np.ndarray) -> QImage:
    """
    Converts a numpy array to a QImage.
    https://gist.github.com/docPhil99/ca4da12c9d6f29b9cea137b617c7b8b1

    :param cv_img: The numpy array
    """

    rgb_image = cv2.cvtColor(cv_img, cv2.COLOR_BGR2RGB)
    height, width, channels = rgb_image.shape
    bytes_per_line = channels * width
    return QImage(rgb_image.data, width, height, bytes_per_line, QImage.Format_RGB888)


def match_all(pattern: Pattern, string: str) -> List[Match]:
    """
    Returns all the matches of a given pattern in a string.

    :param pattern: The RE pattern.
    :param string: The string to match from.
    :return: All the matches.
    """

    matches = []
    match = pattern.match(string)
    while match is not None and match.string:
        matches.append(match)
        string = string[match.end():]
        match = pattern.match(string)
    return matches


def draw_font(src: np.ndarray,
              font: ImageFont,
              text: str,
              position: Tuple[int, int],
              colour: Union[Tuple[int, int, int], Tuple[int, int, int, int]],
              bold: bool = False) -> np.ndarray:
    """
    Draws text onto a given image.
    https://stackoverflow.com/questions/37191008/load-truetype-font-to-opencv

    :param src: The source image.
    :param font: The font to use.
    :param text: The text to draw.
    :param position: The position to draw the text on at.
    :param colour: The colour of the text to draw.
    :param bold: Whether or not to draw the text bold.
    :return: The image with the text drawn on it.
    """

    font_size = font.getsize(text)
    if font_size[0] < src.shape[1] and font_size[1] < src.shape[0]:
        text_image = Image.fromarray(src[position[1]: position[1] + font_size[1],
                                         position[0]: position[0] + font_size[0], :])
        draw = ImageDraw.Draw(text_image)
        draw.text((0, 0), text, fill=colour, font=font, stroke_width=(1 if bold else 0), stroke_fill=colour)
        text_image = np.array(text_image)
        src[position[1]: position[1] + text_image.shape[0],
            position[0]: position[0] + text_image.shape[1]] = text_image

    return src


def get_font_paths() -> Tuple[List[str], Dict[str, str], List[str]]:
    """
    Returns the paths of the fonts installed on the system.
    https://stackoverflow.com/questions/19098440/how-to-get-the-font-file-path-with-qfont-in-qt

    :return: Unloadable fonts, a dictionary of the fonts and their names, and the paths of the fonts.
    """

    font_paths = QStandardPaths.standardLocations(QStandardPaths.FontsLocation)

    accounted = []
    unloadable = []
    family_to_path = {}

    def find_fonts(path: str) -> None:
        for file in os.listdir(path):
            file = os.path.join(path, file)

            if os.path.isdir(file):
                find_fonts(file)

            elif os.path.splitext(file)[1].lower() in (".ttf", ".otf"):
                idx = db.addApplicationFont(file)  # Add font path

                if idx < 0:
                    unloadable.append(file)  # Font wasn't loaded if idx is -1
                else:
                    names = db.applicationFontFamilies(idx)  # Load back font family name

                    for name in names:
                        if name in family_to_path:
                            accounted.append((name, file))
                        else:
                            family_to_path[name] = file

                    # This isn't a 1:1 mapping, for example
                    # 'C:/Windows/Fonts/HTOWERT.TTF' (regular) and
                    # 'C:/Windows/Fonts/HTOWERTI.TTF' (italic) are different
                    # but applicationFontFamilies will return 'High Tower Text' for both

    db = QFontDatabase()
    for font_path in font_paths:  # Go through all font paths
        if os.path.exists(font_path):
            find_fonts(font_path)  # Go through all files at each path

    # noinspection PyTypeChecker
    return unloadable, family_to_path, accounted
