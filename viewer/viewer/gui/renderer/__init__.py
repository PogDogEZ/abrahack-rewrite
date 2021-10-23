#!/usr/bin/env python3
from typing import List

import cv2
import numpy as np
from numba import jit
from numba.experimental import jitclass

import viewer.gui.renderer.layers as layers

from viewer.config import Config


class Renderer:

    @property
    def layers(self) -> List[layers.Layer]:
        return self._layers.copy()

    def __init__(self, mcviewer, main_frame) -> None:
        self.viewer = mcviewer
        self.main_frame = main_frame

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

    def draw_rect(self, image: np.ndarray, chunk_x: float, chunk_y: float, chunk_x1: float, chunk_y1: float,
                   colour: tuple, line_width: int) -> np.ndarray:
        scale = self.main_frame.scale
        left_offset = self.main_frame.left_offset

        scaled_chunk_size = (Config.CHUNK_SIZE[0] * scale[0], Config.CHUNK_SIZE[1] * scale[1])
        diff = (scaled_chunk_size[0] * (max(chunk_x, chunk_x1) - min(chunk_x, chunk_x1)),
                scaled_chunk_size[1] * (max(chunk_y, chunk_y1) - min(chunk_y, chunk_y1)))
        screen_bounds = (
            -(scaled_chunk_size[0] * diff[0]),
            -(scaled_chunk_size[1] * diff[1]),
            self.main_frame.size[0] + (scaled_chunk_size[0] * diff[0]),
            self.main_frame.size[1] + (scaled_chunk_size[1] * diff[1]),
        )

        pixel_coord1 = (int((chunk_x + left_offset[0]) * scaled_chunk_size[0]),
                        int((chunk_y + left_offset[1]) * scaled_chunk_size[1]))
        pixel_coord2 = (int((chunk_x1 + left_offset[0]) * scaled_chunk_size[0]),
                        int((chunk_y1 + left_offset[1]) * scaled_chunk_size[1]))

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

    def render(self) -> np.ndarray:
        image = np.zeros((self.main_frame.size[1], self.main_frame.size[0], 3), np.uint8)
        image[:] = Config.BACKGROUND_COLOUR

        for layer in self._layers:
            image = layer.draw(image)

        return image

