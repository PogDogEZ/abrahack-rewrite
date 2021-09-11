#!/usr/bin/env python3

from typing import Tuple, Any, IO

from viewer.network.types import Dimension
from viewer.util import Position, ChunkPosition
from pclient.networking.types import Enum, Type


class Query:  # TODO: Queries (for serialization)
    ...


class IsLoadedQuery(Query):

    @property
    def position(self) -> ChunkPosition:
        return ChunkPosition(0, 0)

    @property
    def dimension(self) -> Dimension:
        return Dimension.OVERWORLD

    @property
    def type(self):  # -> Type
        return IsLoadedQuery.Type.INVALID_MOVE

    @property
    def result(self):  # -> Result
        return IsLoadedQuery.Result.LOADED

    class Type(Enum):
        DIGGING = 0
        INVALID_MOVE = 1

    class Result:
        LOADED = 0
        UNLOADED = 1
