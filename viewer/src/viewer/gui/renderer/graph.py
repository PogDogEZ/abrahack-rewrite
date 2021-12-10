#!/usr/bin/env python3

from typing import List, Tuple

import cv2
import numpy as np


def plot_graph(graph: np.ndarray, data: List[float], max_item: float, colour: Tuple[int, int, int],
               thickness: int, interpolation: int = cv2.INTER_LINEAR) -> None:
    data = np.array(data, dtype=np.float32)  # Scale the data
    data /= max_item
    data *= graph.shape[0]
    data = cv2.resize(data, (1, graph.shape[1]), interpolation=interpolation)  # Resize the data
    # data = np.convolve(data.reshape(size[0]), np.ones(size[0]))  # Smooth the data

    coords = np.dstack(
        [np.arange(graph.shape[1]), data.reshape(graph.shape[1])])  # Combine with the position in the array
    cv2.polylines(graph, [coords.astype(np.int32)], False, colour, thickness, cv2.LINE_AA)
