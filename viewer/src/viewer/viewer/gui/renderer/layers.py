#!/usr/bin/env python3

import math
from typing import Dict, Tuple

import cv2
import numpy as np

from ...config import Config
from ...util import Dimension, ChunkState


class Layer:
    """
    A render layer.
    """

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

    def draw(self, image: np.ndarray, mouse_position: Tuple[int, int], left_offset: Tuple[float, float],
             scale: Tuple[float, float]) -> np.ndarray:
        ...


class GridLayer(Layer):
    """
    Draws a grid when you zoom in far enough.
    """

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "grid", {})

    def draw(self, image: np.ndarray, mouse_position: Tuple[int, int], left_offset: Tuple[float, float],
             scale: Tuple[float, float]) -> np.ndarray:
        if scale[0] > Config.GRID_SCALE[0]:
            # FIXME: Clean up this
            for x in range(int(image.shape[1] / (Config.CHUNK_SIZE[0] * scale[0])) + 1):
                image = cv2.line(image, (int((x + (left_offset[0] % 1)) * Config.CHUNK_SIZE[0] * scale[0]), 0),
                                 (int((x + (left_offset[0] % 1)) * Config.CHUNK_SIZE[0] * scale[0]), image.shape[0]),
                                 Config.LINE_COLOUR, 2)

        if scale[1] > Config.GRID_SCALE[1]:
            for y in range(int(image.shape[0] / (Config.CHUNK_SIZE[1] * scale[1])) + 1):
                image = cv2.line(image, (0, int((y + (left_offset[1] % 1)) * Config.CHUNK_SIZE[1] * scale[1])),
                                 (image.shape[1], int((y + (left_offset[1] % 1)) * Config.CHUNK_SIZE[1] * scale[1])),
                                 Config.LINE_COLOUR, 2)
        return image


class StatesLayer(Layer):
    """
    Draws the chunk states.
    """

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "states", {})

    def draw(self, image: np.ndarray, mouse_position: Tuple[int, int], left_offset: Tuple[float, float],
             scale: Tuple[float, float]) -> np.ndarray:
        if self.renderer.viewer is None:
            return image
        current_reporter = self.renderer.viewer.current_reporter
        if current_reporter is None:
            return image

        # TODO: Removed for backwards compatibility, add again later
        """
        match self.renderer.current_dimension:  # Yoooo this new syntax is so cool
            case Dimension.NETHER:
                states = self.renderer.nether_states
            case Dimension.OVERWORLD:
                states = self.renderer.overworld_states
            case Dimension.END:
                states = self.renderer.end_states
        """

        if self.renderer.current_dimension == Dimension.NETHER:
            states = self.renderer.nether_states
        elif self.renderer.current_dimension == Dimension.OVERWORLD:
            states = self.renderer.overworld_states
        else:
            states = self.renderer.end_states

        chunk_size = (image.shape[1] / (Config.CHUNK_SIZE[0] * scale[0]), image.shape[0] / (Config.CHUNK_SIZE[1] * scale[1]))
        for x in range(chunk_size[0] // Config.REGION_SIZE[0]):
            for y in range(chunk_size[1] // Config.REGION_SIZE[1]):
                coords = (int(left_offset[0]) + x, int(left_offset[1]) + y)
                # noinspection PyUnboundLocalVariable
                if coords in states:
                    region = states[coords]
                    if chunk_size[0] > Config.REGION_SIZE[0]:
                        ...
                    if chunk_size[1] > Config.REGION_SIZE[1]:
                        ...

        return image


class HighwaysLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "highways", {})

    def draw(self, image: np.ndarray, mouse_position: Tuple[int, int], left_offset: Tuple[float, float],
             scale: Tuple[float, float]) -> np.ndarray:
        image = cv2.line(image, (math.floor(left_offset[0] * Config.CHUNK_SIZE[0] * scale[0]), 0),
                         (math.floor(left_offset[0] * Config.CHUNK_SIZE[0] * scale[0]), image.shape[0]),
                         Config.HIGHWAY_COLOUR, self.renderer.scaled_line_width(*scale, -.75), cv2.LINE_AA)
        image = cv2.line(image, (0, math.floor(left_offset[1] * Config.CHUNK_SIZE[1] * scale[1])),
                         (image.shape[1], math.floor(left_offset[1] * Config.CHUNK_SIZE[1] * scale[1])),
                         Config.HIGHWAY_COLOUR, self.renderer.scaled_line_width(*scale, -.75), cv2.LINE_AA)

        return image


class PlayersLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "players", {})

    def draw(self, image: np.ndarray, mouse_position: Tuple[int, int], left_offset: Tuple[float, float],
             scale: Tuple[float, float]) -> np.ndarray:
        if self.renderer.viewer is None:
            return image
        current_reporter = self.renderer.viewer.current_reporter
        if current_reporter is None:
            return image

        for player in current_reporter.get_players():
            if player.dimension == Dimension.value_to_mc(self.renderer.current_dimension):
                adjusted_coords = (int(((player.position.x / 16 + left_offset[0]) * Config.CHUNK_SIZE[0]) * scale[0]),
                                   int(((player.position.z / 16 + left_offset[1]) * Config.CHUNK_SIZE[1]) * scale[1]))
                image = cv2.circle(image, adjusted_coords, round(scale[0] * 2), Config.PLAYER_COLOUR, -1, cv2.LINE_AA)
                image = cv2.putText(image, player.display_name, adjusted_coords, cv2.FONT_HERSHEY_PLAIN,
                                    scale[0] / 3, (255, 255, 255), round(scale[0] / 2), cv2.LINE_AA)
        return image


class TrackedPlayersLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "tracked_players", {})

    def draw(self, image: np.ndarray, mouse_position: Tuple[int, int], left_offset: Tuple[float, float],
             scale: Tuple[float, float]) -> np.ndarray:
        if self.renderer.viewer is None:
            return image
        current_reporter = self.renderer.viewer.current_reporter
        if current_reporter is None:
            return image

        for tracker in current_reporter.get_trackers():
            tracked_player = tracker.tracked_player
            position = tracked_player.render_distance.center_position
            render_distance = tracked_player.render_distance.render_distance

            if tracked_player.dimension == self.renderer.main_frame.current_dimension - 1:
                self.renderer.draw_rect(image, position.x - math.floor(render_distance / 2),
                                        position.z - math.floor(render_distance / 2),
                                        position.x + math.ceil(render_distance / 2),
                                        position.z + math.ceil(render_distance / 2), (0, 0, 255),
                                        self.renderer.scaled_line_width(*self.renderer.main_frame.scale, -.5))

                top_left_coords = (int((position.x - math.floor(render_distance / 2) + left_offset[0]) *
                                       Config.CHUNK_SIZE[0] * scale[0]),
                                   int((position.z - math.floor(render_distance / 2) + left_offset[1] - 0.1) *
                                       Config.CHUNK_SIZE[1] * scale[1]))
                center_coords = (int((position.x + 0.5 + left_offset[0]) * Config.CHUNK_SIZE[0] * scale[0]),
                                 int((position.z + 0.5 + left_offset[1]) * Config.CHUNK_SIZE[1] * scale[1]))

                # speed = (tracked_player.speed_x ** 2 + tracked_player.speed_z ** 2) ** .5

                username = "unknown"
                best_uuid = tracked_player.get_best_possible_player()
                if best_uuid is not None:
                    username = self.renderer.viewer.get_name_for_uuid(best_uuid)

                image = cv2.circle(image, center_coords, round(scale[0] * 2), Config.PLAYER_COLOUR, -1, cv2.LINE_AA)
                image = cv2.putText(image, "ID: %i, name: %r" % (tracked_player.tracked_player_id, username), top_left_coords,
                                    cv2.FONT_HERSHEY_PLAIN, scale[0] / 2, (255, 255, 255), round(scale[0] / 2),
                                    cv2.LINE_AA)

        return image


class SelectedLayer(Layer):

    def __init__(self, renderer) -> None:
        super().__init__(renderer, "selected", {})

    def draw(self, image: np.ndarray, mouse_position: Tuple[int, int], left_offset: Tuple[float, float],
             scale: Tuple[float, float]) -> np.ndarray:
        # for selected in self.renderer.main_frame.selected_chunks:
        #     self.renderer.draw_rect(image, selected.x, selected.z, selected.x + 1, selected.z + 1, (0, 255, 0),
        #                             self.renderer.scaled_line_width(*self.renderer.main_frame.scale, 2))

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

    def draw(self, image: np.ndarray, mouse_position: Tuple[int, int], left_offset: Tuple[float, float],
             scale: Tuple[float, float]) -> np.ndarray:
        mouse_pos = [
            int(math.floor(mouse_position[0] / Config.CHUNK_SIZE[0] / scale[0] - left_offset[0])),
            int(math.floor(mouse_position[1] / Config.CHUNK_SIZE[1] / scale[1] - left_offset[1]))
        ]

        # return self.renderer.draw_rect(image, mouse_pos[0], mouse_pos[1], mouse_pos[0] + 1, mouse_pos[1] + 1,
        #                                ((0, 0, 255) if not self.renderer.main_frame.mouse_grabbed else (0, 255, 0)),
        #                                min(255, self.renderer.scaled_line_width(*scale, 1)))
        return image
