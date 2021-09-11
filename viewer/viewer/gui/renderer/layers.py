#!/usr/bin/env python3
import math

import cv2
import numpy as np

from viewer.config import Config


class Layer:

    @property
    def name(self) -> str:
        return self._name

    def __init__(self, renderer, name: str) -> None:
        self.renderer = renderer

        self._name = name

    def __repr__(self) -> str:
        return "Layer(name=%s)" % self._name

    def draw(self, image: np.ndarray) -> np.ndarray:
        ...


class GridLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "grid")

    def draw(self, image: np.ndarray) -> np.ndarray:
        scale = self.renderer.main_frame.scale
        left_offset = self.renderer.main_frame.left_offset
        size = self.renderer.main_frame.size

        if scale[0] > Config.GRID_SCALE[0]:
            # FIXME: Clean up this
            for x in range(int(size[0] / (Config.CHUNK_SIZE[0] * scale[0])) + 1):
                image = cv2.line(image,
                                 (int((x + (left_offset[0] % 1)) * Config.CHUNK_SIZE[0] * scale[0]), 0),
                                 (int((x + (left_offset[0] % 1)) * Config.CHUNK_SIZE[0] * scale[0]), size[1]),
                                 Config.LINE_COLOUR, 2)

        if scale[1] > Config.GRID_SCALE[1]:
            for y in range(int(size[1] / (Config.CHUNK_SIZE[1] * scale[1])) + 1):
                image = cv2.line(image,
                                 (0, int((y + (left_offset[1] % 1)) * Config.CHUNK_SIZE[1] * scale[1])),
                                 (size[0], int((y + (left_offset[1] % 1)) * Config.CHUNK_SIZE[1] * scale[1])),
                                 Config.LINE_COLOUR, 2)
        return image


class StatesLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "states")

    def draw(self, image: np.ndarray) -> np.ndarray:
        curr_dimension_data = self.renderer.main_frame._data[self.renderer.main_frame.current_dimension].copy()

        for chunk in curr_dimension_data:
            if chunk in self.renderer.main_frame.selected_chunks:
                continue

            chunk_state = curr_dimension_data[chunk]

            colour = Config.WAITING_COLOUR
            if chunk_state[0] == StatesLayer.QueryState.LOADED:
                colour = Config.LOADED_COLOUR
            elif chunk_state[0] == StatesLayer.QueryState.UNLOADED:
                colour = Config.UNLOADED_COLOUR

            self.renderer.draw_rect(image, chunk[0], chunk[1], chunk[0] + 1, chunk[1] + 1, colour,
                                    self.renderer.scaled_line_width(*self.renderer.main_frame.scale, -.5))

        return image

    class QueryState:  # TODO: Find a better way around this
        WAITING = 0
        LOADED = 1
        UNLOADED = 2


class HighWaysLayer(Layer):
    ...


class PlayersLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "players")

    def draw(self, image: np.ndarray) -> np.ndarray:
        """
        for player in self.renderer.viewer.players:
            if player.dimension == self.current_dimension:
                adjusted_coords = (int((player.position.x + self._left_offset[0] * Config.CHUNK_SIZE[0]) *
                                       self._current_scale[0]),
                                   int((player.position.z + self._left_offset[1] * Config.CHUNK_SIZE[1]) *
                                       self._current_scale[1]))
                image = cv2.circle(image, adjusted_coords, round(self._current_scale[0] * 2), self.PLAYER_COLOUR, -1, cv2.LINE_AA)
                image = cv2.putText(image, repr(player), adjusted_coords, cv2.FONT_HERSHEY_PLAIN,
                                    self._current_scale[0] / 3, (255, 255, 255), round(self._current_scale[0] / 2),
                                    cv2.LINE_AA)
        """
        return image


class SelectedLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "selected")

    def draw(self, image: np.ndarray) -> np.ndarray:
        for selected in self.renderer.main_frame.selected_chunks:
            self.renderer.draw_rect(image, selected[0], selected[1], selected[0] + 1, selected[1] + 1, (0, 255, 0),
                                    self.renderer.scaled_line_width(*self.renderer.main_frame.scale, 2))
        return image


class CursorLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "cursor")

    def draw(self, image: np.ndarray) -> np.ndarray:
        scale = self.renderer.main_frame.scale
        left_offset = self.renderer.main_frame.left_offset
        mouse_position = self.renderer.main_frame.mouse_position

        mouse_pos = [
            int(math.floor(mouse_position[0] / Config.CHUNK_SIZE[0] / scale[0] - left_offset[0])),
            int(math.floor(mouse_position[1] / Config.CHUNK_SIZE[1] / scale[1] - left_offset[1]))
        ]

        return self.renderer.draw_rect(image, mouse_pos[0], mouse_pos[1], mouse_pos[0] + 1, mouse_pos[1] + 1,
                                       ((0, 0, 255) if not self.renderer.main_frame.mouse_grabbed else (0, 255, 0)),
                                       min(255, self.renderer.scaled_line_width(*scale, 1)))
