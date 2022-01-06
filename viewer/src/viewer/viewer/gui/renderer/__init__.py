#!/usr/bin/env python3

from typing import List, Tuple

import cv2
import numpy as np

from . import layers
from ...config import Config
from ...util import Dimension


class Renderer:

    @property
    def viewer(self):  # -> Viewer:
        return self.main_window.viewer

    @property
    def layers(self) -> List[layers.Layer]:
        return self._layers.copy()

    def __init__(self, main_window) -> None:
        self.main_window = main_window

        self.left_offset = (0, 0)
        self.scale = (1, 1)
        self.size = (500, 500)

        self.current_dimension = Dimension.NETHER

        self.nether_states = {}
        self.overworld_states = {}
        self.end_states = {}

        self._layers = [
            layers.StatesLayer(self),
            layers.GridLayer(self),
            layers.HighwaysLayer(self),
            layers.SelectedLayer(self),
            layers.TrackedPlayersLayer(self),
            layers.PlayersLayer(self),
            layers.CursorLayer(self),
        ]

    @staticmethod
    def scaled_line_width(scale_x: float, scale_y: float, factor: float = 1) -> int:
        line_width = (scale_x + scale_y) / (2 + factor)
        return max(1, round(line_width))

    def draw_rect(self, image: np.ndarray, chunk_pos1: Tuple[float, float], chunk_pos2: Tuple[float, float],
                  colour: tuple, line_width: int) -> np.ndarray:
        scale = self.scale
        left_offset = self.left_offset

        scaled_chunk_size = (Config.CHUNK_SIZE[0] * scale[0], Config.CHUNK_SIZE[1] * scale[1])
        diff = (scaled_chunk_size[0] * (max(chunk_pos1[0], chunk_pos2[0]) - min(chunk_pos1[0], chunk_pos2[0])),
                scaled_chunk_size[1] * (max(chunk_pos1[1], chunk_pos2[1]) - min(chunk_pos1[1], chunk_pos2[1])))
        screen_bounds = (
            -(scaled_chunk_size[0] * diff[0]),
            -(scaled_chunk_size[1] * diff[1]),
            image.shape[1] + (scaled_chunk_size[0] * diff[0]),
            image.shape[0] + (scaled_chunk_size[1] * diff[1]),
        )

        pixel_coord1 = (int((chunk_pos1[0] + left_offset[0]) * scaled_chunk_size[0]),
                        int((chunk_pos1[1] + left_offset[1]) * scaled_chunk_size[1]))
        pixel_coord2 = (int((chunk_pos2[0] + left_offset[0]) * scaled_chunk_size[0]),
                        int((chunk_pos2[1] + left_offset[1]) * scaled_chunk_size[1]))

        # image[pixel_coord1[1]: pixel_coord2[1], pixel_coord1[0]: pixel_coord2[0]] = colour

        if screen_bounds[0] <= pixel_coord1[0] <= screen_bounds[2] and \
                screen_bounds[1] <= pixel_coord1[1] <= screen_bounds[3] and \
                screen_bounds[0] <= pixel_coord2[0] <= screen_bounds[2] and \
                screen_bounds[1] <= pixel_coord2[1] <= screen_bounds[3]:
            if line_width == -1:
                image[max(0, pixel_coord1[1]): max(0, pixel_coord2[1]), max(0, pixel_coord1[0]): max(0, pixel_coord2[0])] = colour
            else:
                return cv2.rectangle(image, pixel_coord1, pixel_coord2, colour, min(255, line_width))

        return image

    def render(self, size: Tuple[int, int], mouse_position: Tuple[int, int]) -> np.ndarray:
        image = np.zeros((size[1], size[0], 3), np.uint8)
        image[:] = Config.BASE_COLOUR[:3]

        for layer in self._layers:
            image = layer.draw(image, mouse_position, self.left_offset, self.scale)

        return image

