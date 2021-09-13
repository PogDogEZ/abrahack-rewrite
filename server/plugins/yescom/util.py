#!/usr/bin/env python3

import time

from uuid import UUID

from network.networking.types import Enum


class ChunkPosition:

    @property
    def x(self) -> int:
        return self._position[0]

    @property
    def z(self) -> int:
        return self._position[1]

    def __init__(self, x: int, z: int) -> None:
        self._position = (x, z)

    def __repr__(self) -> str:
        return "ChunkPosition(x=%i, z=%i)" % self._position


class Position:

    @property
    def x(self) -> float:
        return self._position[0]

    @property
    def y(self) -> float:
        return self._position[1]

    @property
    def z(self) -> float:
        return self._position[2]

    def __init__(self, x: float, y: float, z: float) -> None:
        self._position = (x, y, z)

    def __repr__(self) -> str:
        return "Position(x=%.1f, y=%.1f, z=%.1f)" % self._position


class Angle:

    @property
    def yaw(self) -> float:
        return self._yaw

    @property
    def pitch(self) -> float:
        return self._pitch

    def __init__(self, yaw: float, pitch: float) -> None:
        self._yaw = yaw
        self._pitch = pitch

    def __repr__(self) -> str:
        return "Angle(yaw=%.1f, pitch=%.1f)" % (self._yaw, self._pitch)


class Player:

    @property
    def username(self) -> str:
        return self._username

    @username.setter
    def username(self, username: str) -> None:
        self._username = username

    @property
    def uuid(self) -> UUID:
        return self._uuid

    @property
    def display_name(self) -> str:
        return self._display_name

    def __init__(self, username: str) -> None:
        self._username = username

        self._uuid = None
        self._display_name = ""

        self.position = Position(0, 0, 0)
        self.angle = Angle(0, 0)

        self.dimension = 0

        self.health = 20
        self.food = 20
        self.saturation = 5

        # self._last_connection_time = int(time.time() * 1000)

    def __repr__(self) -> str:
        return "Account(username=%r)" % self._username

    def set_profile_details(self, uuid: UUID, display_name: str) -> None:
        if self._uuid is None:
            self._uuid = uuid
        if not self._display_name:
            self._display_name = display_name


class Task:

    @property
    def name(self) -> str:
        return self._name

    @property
    def description(self) -> str:
        return self._description

    @property
    def param_descriptions(self):  # -> List[ParamDescription]
        return self._param_descriptions.copy()

    def __init__(self, name: str, description: str, param_descriptions) -> None:
        self._name = name
        self._description = description
        self._param_descriptions = param_descriptions

    class ParamDescription:

        @property
        def name(self) -> str:
            return self._name

        @property
        def description(self) -> str:
            return self._description

        @property
        def input_type(self):  # -> InputType:
            return self._input_type

        @property
        def data_type(self):  # -> DataType:
            return self._data_type

        def __init__(self, name: str, description: str, input_type, data_type) -> None:
            self._name = name
            self._description = description
            self._input_type = input_type
            self._data_type = data_type

        class InputType(Enum):
            SINGULAR = 0
            ARRAY = 1

        class DataType(Enum):
            POSITION = 0
            ANGLE = 1
            CHUNK_POSITION = 2
            DIMENSION = 3
            PRIORITY = 4
            STRING = 5
            INTEGER = 6
            FLOAT = 7
            BOOLEAN = 8


class ActiveTask:
    ...
