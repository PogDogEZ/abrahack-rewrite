#!/usr/bin/env python3

from typing import IO
from uuid import UUID

from pclient.networking.types import Type
from pclient.networking.types.basic import Double, Integer, String, Float, Short, UnsignedShort, Long, Bytes
from viewer.util import Position, ChunkPosition, Player, Angle, RegisteredTask, ActiveTask, TrackedPlayer, ChunkState, \
    DataType


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

        uuid = UUID(bytes=Bytes.read(fileobj))
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

        Bytes.write(player.uuid.bytes, fileobj)
        String.write(player.display_name, fileobj)

        PositionSpec.write(player.position, fileobj)
        AngleSpec.write(player.angle, fileobj)

        Short.write(player.dimension, fileobj)

        Float.write(player.health, fileobj)
        UnsignedShort.write(player.food, fileobj)
        Float.write(player.saturation, fileobj)


class ParamDescriptionSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> RegisteredTask.ParamDescription:
        name = String.read(fileobj)
        description = String.read(fileobj)
        input_type = RegisteredTask.ParamDescription.InputType.read(fileobj)
        data_type = DataType.read(fileobj)

        return RegisteredTask.ParamDescription(name, description, input_type, data_type)

    @classmethod
    def write(cls, param_description: RegisteredTask.ParamDescription, fileobj: IO) -> None:
        String.write(param_description.name, fileobj)
        String.write(param_description.description, fileobj)
        RegisteredTask.ParamDescription.InputType.write(param_description.input_type, fileobj)
        # noinspection PyTypeChecker
        DataType.write(param_description.data_type, fileobj)


class ParameterSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> ActiveTask.Parameter:
        param_description = ParamDescriptionSpec.read(fileobj)
        serializable = DataType.value_to_serializable(param_description.data_type)

        values = []

        if param_description.input_type == RegisteredTask.ParamDescription.InputType.SINGULAR:
            values.append(serializable.read(fileobj))

        elif param_description.input_type == RegisteredTask.ParamDescription.InputType.ARRAY:
            values_to_read = Integer.read(fileobj)
            for index in range(values_to_read):
                values.append(serializable.read(fileobj))

        return ActiveTask.Parameter(param_description, *values)

    # noinspection PyTypeChecker
    @classmethod
    def write(cls, parameter: ActiveTask.Parameter, fileobj: IO) -> None:
        ParamDescriptionSpec.write(parameter.param_description, fileobj)
        serializable = DataType.value_to_serializable(parameter.param_description.data_type)

        if parameter.param_description.input_type == RegisteredTask.ParamDescription.InputType.SINGULAR:
            serializable.write(parameter.value, fileobj)

        elif parameter.param_description.input_type == RegisteredTask.ParamDescription.InputType.ARRAY:
            Integer.write(len(parameter.values), fileobj)
            for value in parameter.values:
                serializable.write(value, fileobj)


class ChunkStateSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> ChunkState:
        state = ChunkState.State.read(fileobj)
        position = ChunkPositionSpec.read(fileobj)
        dimension = Short.read(fileobj)
        found_at = Long.read(fileobj)

        return ChunkState(state, position, dimension, found_at)

    @classmethod
    def write(cls, chunk_state: ChunkState, fileobj: IO) -> None:
        ChunkState.State.write(chunk_state.state, fileobj)
        ChunkPositionSpec.write(chunk_state.chunk_position, fileobj)
        Short.write(chunk_state.dimension, fileobj)
        Long.write(chunk_state.found_at, fileobj)


class TrackedPlayerSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> TrackedPlayer:
        tracked_player_id = Integer.read(fileobj)
        position = ChunkPositionSpec.read(fileobj)
        dimension = Short.read(fileobj)
        speed_x = Float.read(fileobj)
        speed_z = Float.read(fileobj)

        return TrackedPlayer(tracked_player_id, position, dimension, speed_x, speed_z)

    @classmethod
    def write(cls, tracked_player: TrackedPlayer, fileobj: IO) -> None:
        Integer.write(tracked_player.tracked_player_id, fileobj)
        ChunkPositionSpec.write(tracked_player.position, fileobj)
        Short.write(tracked_player.dimension, fileobj)
        Float.write(tracked_player.speed_x, fileobj)
        Float.write(tracked_player.speed_z, fileobj)
