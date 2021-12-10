#!/usr/bin/env python3

import cv2
import numpy as np

from typing import Pattern, List, Match

from PyQt5.QtGui import QImage


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
