#!/usr/bin/env python3

from typing import IO
from uuid import UUID

from viewer.util import Position, ChunkPosition, Player, Reporter
from pclient.networking.types import Enum, Type
from pclient.networking.types.basic import Double, Integer, String, Boolean, Float, Short, UnsignedShort


class Dimension(Enum):
    NETHER = 0
    OVERWORLD = 1
    END = 2


class PositionSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> Position:
        x = Double.read(fileobj)
        y = Double.read(fileobj)
        z = Double.read(fileobj)

        return Position(x, y, z)

    @classmethod
    def write(cls, position: Position, fileobj: IO) -> None:
        Double.write(position.x, fileobj)
        Double.write(position.y, fileobj)
        Double.write(position.z, fileobj)


class ChunkPosSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> ChunkPosition:
        x = Integer.read(fileobj)
        z = Integer.read(fileobj)

        return ChunkPosition(x, z)

    @classmethod
    def write(cls, chunk_pos: ChunkPosition, fileobj: IO) -> None:
        Integer.write(chunk_pos.x, fileobj)
        Integer.write(chunk_pos.z, fileobj)


class PlayerSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> Player:
        username = String.read(fileobj)

        player = Player(username)

        uuid = UUID(String.read(fileobj))
        display_name = String.read(fileobj)

        player.set_profile_details(uuid, display_name)

        player.position = PositionSpec.read(fileobj)
        player.yaw = Float.read(fileobj)
        player.pitch = Float.read(fileobj)

        player.dimension = Short.read(fileobj)

        player.health = Float.read(fileobj)
        player.food = UnsignedShort.read(fileobj)
        player.saturation = Float.read(fileobj)

        return player

    @classmethod
    def write(cls, player: Player, fileobj: IO) -> None:
        String.write(player.username, fileobj)

        String.write(str(player.uuid), fileobj)
        String.write(player.display_name, fileobj)

        PositionSpec.write(player.position, fileobj)
        Float.write(player.yaw, fileobj)
        Float.write(player.pitch, fileobj)

        Short.write(player.dimension, fileobj)

        Float.write(player.health, fileobj)
        UnsignedShort.write(player.food, fileobj)
        Float.write(player.saturation, fileobj)
