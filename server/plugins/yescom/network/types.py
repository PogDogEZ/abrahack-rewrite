#!/usr/bin/env python3

from typing import Any, IO
from uuid import UUID

from network.networking.types import Type, Enum
from network.networking.types.basic import String, Float, Double, Integer, Short, UnsignedShort
from plugins.yescom.util import Position, ChunkPosition, Player, Angle, Task


class Dimension(Enum):
    NETHER = 0
    OVERWORLD = 1
    END = 2


class Priority(Enum):
    LOW = 0
    MEDIUM = 1
    HIGH = 2
    USER = 3


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


class AngleSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> Angle:
        yaw = Float.read(fileobj)
        pitch = Float.read(fileobj)

        return Angle(yaw, pitch)

    @classmethod
    def write(cls, angle: Angle, fileobj: IO) -> None:
        Float.write(angle.yaw, fileobj)
        Float.write(angle.pitch, fileobj)


class ChunkPositionSpec(Type):

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
        player.angle = AngleSpec.read(fileobj)

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
        AngleSpec.write(player.angle, fileobj)

        Short.write(player.dimension, fileobj)

        Float.write(player.health, fileobj)
        UnsignedShort.write(player.food, fileobj)
        Float.write(player.saturation, fileobj)


class ParamDescriptionSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> Task.ParamDescription:
        name = String.read(fileobj)
        description = String.read(fileobj)
        input_type = Task.ParamDescription.InputType.read(fileobj)
        data_type = Task.ParamDescription.DataType.read(fileobj)

        return Task.ParamDescription(name, description, input_type, data_type)

    @classmethod
    def write(cls, param_description: Task.ParamDescription, fileobj: IO) -> None:
        String.write(param_description.name, fileobj)
        String.write(param_description.description, fileobj)
        Task.ParamDescription.InputType.write(param_description.input_type, fileobj)
        Task.ParamDescription.DataType.write(param_description.data_type, fileobj)
