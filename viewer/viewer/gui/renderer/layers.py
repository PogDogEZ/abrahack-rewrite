#!/usr/bin/env python3

import math
from typing import Dict, Tuple, List

import cv2
import numpy as np
from numba import jit

from viewer.config import Config
from viewer.util import Dimension, ChunkState


class Layer:

    @property
    def name(self) -> str:
        return self._name

    @property
    def options(self) -> Dict[str, object]:
        return self._options

    def __init__(self, renderer, name: str, options: Dict[str, object]) -> None:
        self.renderer = renderer

        self._name = name
        self._options = options

    def __repr__(self) -> str:
        return "Layer(name=%s)" % self._name

    def draw(self, image: np.ndarray) -> np.ndarray:
        ...


class GridLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "grid", {})

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
        super().__init__(renderer, "states", {})

    def draw(self, image: np.ndarray) -> np.ndarray:
        if self.renderer.viewer.current_reporter == -1:
            return image

        reporter = self.renderer.viewer.get_reporter(handler_id=self.renderer.viewer.current_reporter)

        # FIXME: Faster rendering

        for active_task in reporter.get_active_tasks():
            if active_task.loaded_chunk_task:
                self.renderer.draw_rect(image, active_task.current_position.x, active_task.current_position.z,
                                        active_task.current_position.x + 1, active_task.current_position.z + 1,
                                        Config.CURRENT_SCAN_COLOUR, -1)

            if active_task.registered_task.name in ("static_scan",):
                try:
                    scanning_chunks = active_task.get_parameter("positions").values
                    dimension = active_task.get_parameter("dimension").value
                except LookupError:
                    continue

                if dimension == Dimension.value_to_mc(self.renderer.main_frame.current_dimension):
                    for chunk_position in scanning_chunks:
                        self.renderer.draw_rect(image, chunk_position.x, chunk_position.z, chunk_position.x + 1,
                                                chunk_position.z + 1, Config.WAITING_COLOUR, -1)

        for chunk_state in reporter.get_states(Dimension.value_to_mc(self.renderer.main_frame.current_dimension)):
            if chunk_state.chunk_position in self.renderer.main_frame.selected_chunks:
                continue

            chunk_position = chunk_state.chunk_position
            self.renderer.draw_rect(image, chunk_position.x, chunk_position.z, chunk_position.x + 1,
                                    chunk_position.z + 1,
                                    (Config.LOADED_COLOUR if chunk_state.state == ChunkState.State.LOADED else
                                     Config.UNLOADED_COLOUR), -1)

        return image


class HighwaysLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "highways", {})

    def draw(self, image: np.ndarray) -> np.ndarray:
        scale = self.renderer.main_frame.scale
        left_offset = self.renderer.main_frame.left_offset
        size = self.renderer.main_frame.size

        image = cv2.line(image, (math.floor(left_offset[0] * Config.CHUNK_SIZE[0] * scale[0]), 0),
                         (math.floor(left_offset[0] * Config.CHUNK_SIZE[0] * scale[0]), size[1]),
                         Config.HIGHWAY_COLOUR, self.renderer.scaled_line_width(*scale, -.75), cv2.LINE_AA)
        image = cv2.line(image, (0, math.floor(left_offset[1] * Config.CHUNK_SIZE[1] * scale[1])),
                         (size[0], math.floor(left_offset[1] * Config.CHUNK_SIZE[1] * scale[1])),
                         Config.HIGHWAY_COLOUR, self.renderer.scaled_line_width(*scale, -.75), cv2.LINE_AA)

        return image


class PlayersLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "players", {})

    def draw(self, image: np.ndarray) -> np.ndarray:
        players = []

        if self.renderer.viewer.current_reporter != -1:
            reporter = self.renderer.viewer.get_reporter(handler_id=self.renderer.viewer.current_reporter)
            players = reporter.players

        scale = self.renderer.main_frame.scale
        left_offset = self.renderer.main_frame.left_offset

        for player in players:
            if player.dimension == self.renderer.main_frame.current_dimension - 1:  # Very hacky but shut up
                adjusted_coords = (int(((player.position.x / 16 + left_offset[0]) * Config.CHUNK_SIZE[0]) * scale[0]),
                                   int(((player.position.z / 16 + left_offset[1]) * Config.CHUNK_SIZE[1]) * scale[1]))
                image = cv2.circle(image, adjusted_coords, round(scale[0] * 2), Config.PLAYER_COLOUR, -1, cv2.LINE_AA)
                image = cv2.putText(image, player.display_name, adjusted_coords, cv2.FONT_HERSHEY_PLAIN,
                                    scale[0] / 3, (255, 255, 255), round(scale[0] / 2), cv2.LINE_AA)
        return image


class TrackedPlayersLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "tracked_players", {})

    def draw(self, image: np.ndarray) -> np.ndarray:
        tracked_players = []

        if self.renderer.viewer.current_reporter != -1:
            reporter = self.renderer.viewer.get_reporter(handler_id=self.renderer.viewer.current_reporter)
            tracked_players = reporter.tracked_players

        scale = self.renderer.main_frame.scale
        left_offset = self.renderer.main_frame.left_offset

        for tracked_player in tracked_players:
            if tracked_player.dimension == self.renderer.main_frame.current_dimension - 1:
                self.renderer.draw_rect(image, tracked_player.position.x - math.floor(Config.RENDER_DISTANCE / 2),
                                        tracked_player.position.z - math.floor(Config.RENDER_DISTANCE / 2),
                                        tracked_player.position.x + math.ceil(Config.RENDER_DISTANCE / 2),
                                        tracked_player.position.z + math.ceil(Config.RENDER_DISTANCE / 2), (0, 0, 255),
                                        self.renderer.scaled_line_width(*self.renderer.main_frame.scale, -.5))

                top_left_coords = (int((tracked_player.position.x - math.floor(Config.RENDER_DISTANCE / 2) + left_offset[0]) *
                                       Config.CHUNK_SIZE[0] * scale[0]),
                                   int((tracked_player.position.z - math.floor(Config.RENDER_DISTANCE / 2) + left_offset[1] - 0.1) *
                                       Config.CHUNK_SIZE[1] * scale[1]))
                center_coords = (int((tracked_player.position.x + 0.5 + left_offset[0]) * Config.CHUNK_SIZE[0] * scale[0]),
                                 int((tracked_player.position.z + 0.5 + left_offset[1]) * Config.CHUNK_SIZE[1] * scale[1]))

                speed = (tracked_player.speed_x ** 2 + tracked_player.speed_z ** 2) ** .5

                image = cv2.circle(image, center_coords, round(scale[0] * 2), Config.PLAYER_COLOUR, -1, cv2.LINE_AA)
                image = cv2.putText(image, "ID: %i, speed: %.1f chunks/s (%.1f blocks/s)" %
                                    (tracked_player.tracked_player_id, speed, speed * 16),
                                    top_left_coords, cv2.FONT_HERSHEY_PLAIN,
                                    scale[0] / 3, (255, 255, 255), round(scale[0] / 2), cv2.LINE_AA)

        return image


class SelectedLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "selected", {})

    def draw(self, image: np.ndarray) -> np.ndarray:
        for selected in self.renderer.main_frame.selected_chunks:
            self.renderer.draw_rect(image, selected.x, selected.z, selected.x + 1, selected.z + 1, (0, 255, 0),
                                    self.renderer.scaled_line_width(*self.renderer.main_frame.scale, 2))

        return image

    """
    @staticmethod
    @jit(nopython=False)
    def _draw(image: np.ndarray, selected_chunks: List[Tuple[int, int]], line_width: int, chunk_size: Tuple[int, int],
              scale: Tuple[float, float], left_offset: Tuple[float, float]) -> np.ndarray:
        colour = np.array([0, 255, 0], dtype=np.uint8)
        
        for selected_chunk in selected_chunks:
            pixel_coord1 = (int((selected_chunk[0] + left_offset[0]) * chunk_size[0] * scale[0]),
                            int((selected_chunk[1] + left_offset[1]) * chunk_size[1] * scale[1]))
            pixel_coord2 = (int((selected_chunk[0] + 1 + left_offset[0]) * chunk_size[0] * scale[0]),
                            int((selected_chunk[1] + 1 + left_offset[1]) * chunk_size[1] * scale[1]))

            image[pixel_coord1[1]: pixel_coord2[1], pixel_coord1[0]: pixel_coord2[0]] = colour

        return image
    """


class CursorLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "cursor", {})

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
