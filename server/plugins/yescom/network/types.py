#!/usr/bin/env python3

from typing import IO
from uuid import UUID

from network.networking.types import Type
from network.networking.types.basic import String, Float, Double, Integer, Short, UnsignedShort, Long, Bytes, VarInt, \
    Boolean
from plugins.yescom.util import Position, ChunkPosition, Player, Angle, RegisteredTask, ActiveTask, TrackedPlayer, \
    ChunkState, DataType, RenderDistance, TrackingData, Tracker


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


class RenderDistanceSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> RenderDistance:
        render_distance_id = VarInt.read(fileobj)
        center_position = ChunkPositionSpec.read(fileobj)
        render_distance = UnsignedShort.read(fileobj)
        error_x = Float.read(fileobj)
        error_z = Float.read(fileobj)

        return RenderDistance(render_distance_id, center_position, render_distance, error_x, error_z)

    @classmethod
    def write(cls, render_distance: RenderDistance, fileobj: IO) -> None:
        VarInt.write(render_distance.render_distance_id, fileobj)
        ChunkPositionSpec.write(render_distance.center_position, fileobj)
        UnsignedShort.write(render_distance.render_distance, fileobj)
        Float.write(render_distance.error_x, fileobj)
        Float.write(render_distance.error_z, fileobj)


class TrackedPlayerSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> TrackedPlayer:
        tracked_player_id = VarInt.read(fileobj)

        possible_players = {}
        possible_players_to_read = UnsignedShort.read(fileobj)
        for index in range(possible_players_to_read):
            uuid = UUID(bytes=Bytes.read(fileobj))
            count = UnsignedShort.read(fileobj)
            possible_players[uuid] = count

        render_distances = {}
        render_distances_to_read = UnsignedShort.read(fileobj)
        for index in range(render_distances_to_read):
            timestamp = Long.read(fileobj)
            render_distance_id = VarInt.read(fileobj)
            render_distances[timestamp] = render_distance_id

        render_distance = RenderDistanceSpec.read(fileobj)
        dimension = Short.read(fileobj)
        logged_out = Boolean.read(fileobj)
        found_at = Long.read(fileobj)

        tracked_player = TrackedPlayer(tracked_player_id, TrackingData(render_distances), render_distance, dimension,
                                       logged_out, found_at)
        tracked_player.set_possible_players(possible_players)

        return tracked_player

    @classmethod
    def write(cls, tracked_player: TrackedPlayer, fileobj: IO) -> None:
        VarInt.write(tracked_player.tracked_player_id, fileobj)

        possible_players = tracked_player.get_possible_players()
        UnsignedShort.write(len(possible_players), fileobj)
        for uuid in possible_players:
            Bytes.write(uuid.bytes, fileobj)
            UnsignedShort.write(possible_players[uuid], fileobj)

        render_distances = tracked_player.tracking_data.get_render_distances()
        UnsignedShort.write(len(render_distances), fileobj)
        for timestamp in render_distances:
            Long.write(timestamp, fileobj)
            VarInt.write(render_distances[timestamp], fileobj)

        RenderDistanceSpec.write(tracked_player.render_distance, fileobj)
        Short.write(tracked_player.dimension, fileobj)
        Boolean.write(tracked_player.logged_out, fileobj)
        Long.write(tracked_player.found_at, fileobj)


class TrackerSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> Tracker:
        tracker_id = Long.read(fileobj)
        tracked_player = TrackedPlayerSpec.read(fileobj)

        return Tracker(tracker_id, tracked_player)

    @classmethod
    def write(cls, tracker: Tracker, fileobj: IO) -> None:
        Long.write(tracker.tracked_id, fileobj)
        TrackedPlayerSpec.write(tracker.tracked_player, fileobj)
