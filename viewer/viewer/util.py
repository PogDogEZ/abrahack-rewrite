#!/usr/bin/env python3

import time
from typing import List
from uuid import UUID


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
        self.yaw = 0
        self.pitch = 0

        self.dimension = 0

        self.health = 20
        self.food = 20
        self.saturation = 5

        self._last_connection_time = int(time.time() * 1000)

    def __repr__(self) -> str:
        return "Account(username=%r)" % self._username

    def set_profile_details(self, uuid: UUID, display_name: str) -> None:
        if self._uuid is None:
            self._uuid = uuid
        if not self._display_name:
            self._display_name = display_name


class Reporter:

    @property
    def assigned_id(self) -> int:
        return self._id

    @property
    def name(self) -> str:
        return self._name

    def __init__(self, assigned_id: int, name: str) -> None:
        self._id = assigned_id
        self._name = name

        self._players = []

    def __repr__(self) -> str:
        return "Reporter(name=%s, id=%i)" % (self._name, self._id)

    def add_player(self, player: Player) -> None:
        if not player in self._players:
            self._players.append(player)

    def remove_player(self, player: Player) -> None:
        if player in self._players:
            self._players.remove(player)

    def get_players(self) -> List[Player]:
        return self._players.copy()
