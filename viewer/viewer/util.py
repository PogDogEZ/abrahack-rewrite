#!/usr/bin/env python3

import typing
from typing import List, Dict, Tuple
from uuid import UUID

import viewer.network.types as network_types
from pyclient.networking.types import Enum, Type
from pyclient.networking.types.basic import Short, String, Integer, Float, Boolean


class DataType(Enum):
    """
    Enum for the data types.
    """

    POSITION = 0
    ANGLE = 1
    CHUNK_POSITION = 2
    DIMENSION = 3
    PRIORITY = 4
    STRING = 5
    INTEGER = 6
    FLOAT = 7
    BOOLEAN = 8

    @classmethod
    def value_to_serializable(cls, value) -> typing.Type[Type]:
        """
        Returns the serializable type for the given value.

        :param value: The enum value.
        :return: The serializable type.
        """
        return (network_types.PositionSpec, network_types.AngleSpec, network_types.ChunkPositionSpec, Short, Priority,
                String, Integer, Float, Boolean)[value]

    @classmethod
    def value_to_type(cls, value) -> type:
        """
        Returns the type for the given value.

        :param value: The enum value.
        :return: The python type.
        """
        return (Position, Angle, ChunkPosition, int, Priority, str, int, float, bool)[value]

    @classmethod
    def type_to_value(cls, data_type: type):  # -> DataType:
        """
        Returns the enum value for the given python type.

        :param data_type: The python type.
        :return: The enum value.
        """
        return (Position, Angle, ChunkPosition, int, Priority, str, int, float, bool).index(data_type)


class Dimension(Enum):
    """
    Intermediary dimension enum, for client compatibility.
    """

    NETHER = 0
    OVERWORLD = 1
    END = 2


class Priority(Enum):
    """
    Task priorities.
    """

    LOW = 0
    MEDIUM = 1
    HIGH = 2
    USER = 3


class ChunkPosition:
    """
    A chunk position, duh.
    """

    @property
    def x(self) -> int:
        """
        :return: The X chunk ordinate.
        """

        return self._position[0]

    @property
    def z(self) -> int:
        """
        :return: The Z chunk ordinate.
        """

        return self._position[1]

    def __init__(self, x: int, z: int) -> None:
        self._position = (x, z)

    def __repr__(self) -> str:
        return "ChunkPosition(x=%i, z=%i)" % self._position


class Position:
    """
    A position, not a block position.
    """

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
    """
    An angle.
    """

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


class ChatMessage:
    """
    A chat message, the username is the username of the account that received the chat message.
    """

    @property
    def chat_message_id(self) -> int:
        return self._chat_message_id

    @property
    def username(self) -> str:
        return self._username

    @property
    def message(self) -> str:
        return self._message

    @property
    def timestamp(self) -> int:
        return self._timestamp

    def __init__(self, chat_message_id: int, username: str, message: str, timestamp: int) -> None:
        self._chat_message_id = chat_message_id
        self._username = username
        self._message = message
        self._timestamp = timestamp

    def __repr__(self) -> str:
        return "ChatMessage(username=%s, message=%r)" % (self._username, self._message)


class Player:
    """
    Intermediary player class, for client compatibility.
    """

    @property
    def username(self) -> str:
        """
        :return: The "username" (honestly I'm not even sure what this is lol, I think it's the UUID).
        """

        return self._username

    @property
    def uuid(self) -> UUID:
        return self._uuid

    @property
    def display_name(self) -> str:
        """
        :return: The in-game display name.
        """

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
        return "Player(username=%r, display_name=%r)" % (self._username, self._display_name)

    def set_profile_details(self, uuid: UUID, display_name: str) -> None:
        if self._uuid is None:
            self._uuid = uuid
        if not self._display_name:
            self._display_name = display_name


class ConfigRule:
    """
    A config rule.
    """

    @property
    def name(self) -> str:
        """
        :return: The name of the rule.
        """

        return self._name

    @property
    def data_type(self) -> DataType:
        return self._data_type

    @property
    def enum_values(self) -> List[str]:
        """
        :return: The enum constant values, if this is an enum value.
        """

        return self._enum_values.copy() if self._enum_values is not None else []

    @property
    def enum_value(self) -> bool:
        """
        :return: A boolean indicating whether the value is an enum value or not.
        """

        return self._enum_value

    def __init__(self, name: str, data_type: DataType = DataType.STRING, enum_values: List[str] = None) -> None:
        self._name = name

        self._data_type = data_type if enum_values is None else DataType.STRING
        self._enum_values = enum_values.copy() if enum_values is not None else []

        self._enum_value = enum_values is not None

    def __repr__(self) -> str:
        return "ConfigRule(name=%s, data_type=%s)" % (self._name, DataType.name_from_value(self._data_type))

    def __eq__(self, other) -> bool:
        if not isinstance(other, ConfigRule):
            return False
        else:
            return self._name == other._name and self._data_type == other._data_type and \
                   self._enum_values == other._enum_values

    def __hash__(self) -> int:
        return hash((self._name, self._data_type, self._enum_value))


class RegisteredTask:
    """
    A representation of a registered task. This is a task that may not be currently active. It is used to indicate to
    the client which tasks can be run, and what parameters they take.
    """

    @property
    def name(self) -> str:
        return self._name

    @property
    def description(self) -> str:
        """
        :return: The general description for this task.
        """

        return self._description

    @property
    def param_descriptions(self):  # -> List[ParamDescription]
        """
        :return: The parameter descriptions for this task.
        """

        return self._param_descriptions.copy()

    def __init__(self, name: str, description: str, param_descriptions) -> None:
        self._name = name
        self._description = description
        self._param_descriptions = param_descriptions

    def __repr__(self) -> str:
        return "RegisteredTask(name=%s, description=%r)" % (self._name, self._description)

    def __eq__(self, other) -> bool:
        if not isinstance(other, RegisteredTask):
            return False
        else:
            return other._name == self._name and other._description == self._description

    def get_param_descriptions(self) -> List:  # -> List[ParamDescription]:
        return self._param_descriptions.copy()

    def get_param_description(self, param_name: str):  # -> ParamDescription
        """
        :param param_name: The name of the parameter to get the description for.
        :return: The "parameter description".
        """

        for param_description in self._param_descriptions:
            if param_description.name == param_name:
                return param_description

        raise LookupError("Couldn't find param description by name %r." % param_name)

    def create_new(self, task_id: int, **kwargs):  # -> ActiveTask:
        """
        Creates a new active task based on this registered task.

        :param task_id: The ID of the task.
        :param kwargs: The parameters to pass to the task.
        :return: The new active task.
        """

        parameters = []
        for param_name in kwargs:
            try:
                param_description = self.get_param_description(param_name)
                parameters.append(ActiveTask.Parameter(param_description, kwargs[param_name]))
            except LookupError:
                ...

        return ActiveTask(self, task_id, parameters, 0, 0, [])

    class ParamDescription:
        """
        Terrible way of representing parameters for tasks, shut up.
        """

        @property
        def name(self) -> str:
            return self._name

        @property
        def description(self) -> str:
            return self._description

        @property
        def input_type(self):  # -> InputType:
            """
            :return: Whether this is a singular or array parameter type.
            """

            return self._input_type

        @property
        def data_type(self) -> DataType:
            """
            :return: The data type of the parameter.
            """

            return self._data_type

        def __init__(self, name: str, description: str, input_type, data_type) -> None:
            self._name = name
            self._description = description
            self._input_type = input_type
            self._data_type = data_type

        def __repr__(self) -> str:
            return "ParamDescription(name=%r, itype=%s, dtype=%s)" % \
                   (self._name, RegisteredTask.ParamDescription.InputType.name_from_value(self._input_type),
                    DataType.name_from_value(self._data_type))

        class InputType(Enum):
            SINGULAR = 0
            ARRAY = 1


class ActiveTask:
    """
    An active task, this is a task that is currently running.
    """

    @property
    def registered_task(self) -> RegisteredTask:
        """
        :return: The registered task representation of this.
        """

        return self._registered_task

    @property
    def task_id(self) -> int:
        """
        :return: The assigned ID of this task.
        """

        return self._task_id

    @property
    def parameters(self):  # -> List[Parameter]
        """
        :return: The parameters used to create this task.
        """

        return self._parameters.copy()

    @property
    def loaded_chunk_task(self) -> bool:
        """
        :return: Whether this is a loaded chunk task.
        """

        return self._loaded_chunk_task

    @property
    def progress(self) -> float:
        """
        :return: The completed progress of this task.
        """

        return self._progress

    @property
    def time_elapsed(self) -> int:
        return self._time_elapsed

    @property
    def current_position(self) -> ChunkPosition:
        """
        :return: The current position of this task, this is only applicable to loaded chunk tasks.
        """

        return self._current_position

    @property
    def results(self) -> List[str]:
        """
        :return: The results (string representations of them).
        """

        return self._results.copy()

    def __init__(self, registered_task: RegisteredTask, task_id: int, parameters: List, progress: float,
                 time_elapsed: int, results: List[str]) -> None:
        self._registered_task = registered_task
        self._task_id = task_id
        self._parameters = parameters.copy()
        self._loaded_chunk_task = False

        self._progress = progress
        self._time_elapsed = time_elapsed
        self._current_position = ChunkPosition(0, 0)

        self._results = results.copy()

    def __eq__(self, other) -> bool:
        if not isinstance(other, ActiveTask):
            return False
        else:
            return other._registered_task == self._registered_task and other._task_id == self._task_id

    def update(self, loaded_chunk_task: bool, progress: float, time_elapsed: int,
               current_position: ChunkPosition) -> None:
        """
        Updates the progress of this task.

        :param loaded_chunk_task: Whether this is a loaded chunk task.
        :param progress: The completed progress of this task.
        :param time_elapsed: The time elapsed since this task started.
        :param current_position: The current position of this task, this is only applicable to loaded chunk tasks.
        """

        self._loaded_chunk_task = loaded_chunk_task
        self._progress = progress
        self._time_elapsed = time_elapsed
        self._current_position = current_position

    def add_result(self, result: str) -> None:
        if not result in self._results:
            self._results.append(result)

    def remove_result(self, result: str) -> None:
        if result in self._results:
            self._results.remove(result)

    class Parameter:
        """
        A parameter used to create this task.
        """

        @property
        def param_description(self) -> RegisteredTask.ParamDescription:
            return self._param_description

        @property
        def value(self) -> object:
            return self._values[0]

        @property
        def values(self) -> List[object]:
            return self._values.copy()

        def __init__(self, param_description: RegisteredTask.ParamDescription, *values: Tuple[object]) -> None:
            self._param_description = param_description
            self._values = list(values)

        def __repr__(self) -> str:
            return "Parameter(description=%r, values=%r)" % (self._param_description, self._values)


class ChunkState:
    """
    Stores information about the "state" of a chunk (if it's loaded or not).
    """

    @property
    def chunk_state_id(self) -> int:
        return self._chunk_state_id

    @property
    def state(self):  # -> ChunkState.State:
        return self._state

    @property
    def chunk_position(self) -> ChunkPosition:
        return self._chunk_position

    @property
    def dimension(self) -> int:
        return self._dimension

    @property
    def found_at(self) -> int:
        """
        :return: The time this chunk state was found.
        """

        return self._found_at

    def __init__(self, chunk_state_id: int, state, chunk_position: ChunkPosition, dimension: int, found_at: int) -> None:
        self._chunk_state_id = chunk_state_id
        self._state = state
        self._chunk_position = chunk_position
        self._dimension = dimension
        self._found_at = found_at

    def __repr__(self) -> str:
        return "ChunkState(ID=%i, state=%s, position=%r)" % (self._chunk_state_id,
                                                             ChunkState.State.name_from_value(self._state),
                                                             self._chunk_position)

    def __eq__(self, other) -> bool:
        if not isinstance(other, ChunkState):
            return False
        else:
            return other._chunk_state_id == self._chunk_state_id

    class State(Enum):
        LOADED = 0
        UNLOADED = 1


class RenderDistance:
    """
    Stores information about the render distance of a player.
    """

    @property
    def render_distance_id(self) -> int:
        return self._render_distance_id

    @property
    def center_position(self) -> ChunkPosition:
        return self._center_position

    @property
    def render_distance(self) -> int:
        return self._render_distance

    @property
    def error_x(self) -> float:
        """
        :return: The error in the x direction.
        """

        return self._error_x

    @property
    def error_z(self) -> float:
        """
        :return: The error in the z direction.
        """

        return self._error_z

    def __init__(self, render_distance_id: int, center_position: ChunkPosition, render_distance: int, error_x: float,
                 error_z: float) -> None:
        self._render_distance_id = render_distance_id
        self._center_position = center_position
        self._render_distance = render_distance
        self._error_x = error_x
        self._error_z = error_z

    def __repr__(self) -> str:
        return "RenderDistance(ID=%i, center=%r)" % (self._render_distance_id, self._center_position)

    def __eq__(self, other) -> bool:
        if not isinstance(other, RenderDistance):
            return False
        else:
            return other._render_distance_id == self._render_distance_id


class TrackingData:
    """
    Stores previous render distances of a player, as well as the time they were found at.
    """

    def __init__(self, previous_render_distances: Dict[int, int] = {}) -> None:
        self._previous_render_distances = previous_render_distances.copy()

    def get_render_distances(self) -> Dict[int, int]:
        return self._previous_render_distances.copy()


class TrackedPlayer:
    """
    The representation of a tracked player.
    """

    @property
    def tracked_player_id(self) -> int:
        return self._tracked_player_id

    @property
    def tracking_data(self) -> TrackingData:
        """
        :return: The previous tracking data of this player.
        """

        return self._tracking_data

    @property
    def render_distance(self) -> RenderDistance:
        """
        :return: The current render distance of this player.
        """

        return self._render_distance

    @property
    def dimension(self) -> int:
        return self._dimension

    @property
    def logged_out(self) -> bool:
        """
        :return: Whether or not this player is logged out.
        """

        return self._logged_out

    @property
    def found_at(self) -> int:
        return self._found_at

    def __init__(self, tracked_player_id: int, tracking_data: TrackingData, render_distance: RenderDistance,
                 dimension: int, logged_out: bool, found_at: int) -> None:
        self._tracked_player_id = tracked_player_id
        self._tracking_data = tracking_data
        self._render_distance = render_distance
        self._dimension = dimension
        self._logged_out = logged_out
        self._found_at = found_at

        self._possible_players = {}

    def __repr__(self) -> str:
        return "TrackedPlayer(ID=%i, render_distance=%s, dimension=%i)" % (self._tracked_player_id,
                                                                           self._render_distance, self._dimension)

    def __eq__(self, other) -> bool:
        if not isinstance(other, TrackedPlayer):
            return False
        else:
            return other._tracked_player_id == self._tracked_player_id

    def get_possible_players(self) -> Dict[UUID, int]:
        return self._possible_players.copy()

    def put_possible_player(self, uuid: UUID, count: int) -> None:
        self._possible_players[uuid] = count

    def set_possible_players(self, possible_players: Dict[UUID, int]) -> None:
        self._possible_players.clear()
        self._possible_players.update(possible_players)

    def put_possible_players(self, possible_players: Dict[UUID, int]) -> None:
        self._possible_players.update(possible_players)

    def remove_possible_player(self, uuid: UUID) -> None:
        del self._possible_players[uuid]


class Tracker:
    """
    A tracker, idk what to say.
    """

    @property
    def tracker_id(self) -> int:
        return self._tracker_id

    def __init__(self, tracker_id: int, tracked_player_ids: List[int]) -> None:
        self._tracker_id = tracker_id
        self._tracked_player_ids = tracked_player_ids.copy()

    def __repr__(self) -> str:
        return "Tracker(ID=%i)" % self._tracker_id

    def add_tracked_player_id(self, tracked_player_id: int) -> None:
        """
        Adds a tracked player ID to this tracker.

        :param tracked_player_id: The ID of the tracked player.
        """

        self._tracked_player_ids.append(tracked_player_id)

    def set_tracked_player_ids(self, tracked_player_ids: List[int]) -> None:
        self._tracked_player_ids.clear()
        self._tracked_player_ids.extend(tracked_player_ids)

    def remove_tracked_player_id(self, tracked_player_id: int) -> None:
        """
        Removes a tracked player ID from this tracker.

        :param tracked_player_id: The ID of the tracked player.
        """

        self._tracked_player_ids.remove(tracked_player_id)

    def get_tracked_player_ids(self) -> List[int]:
        return self._tracked_player_ids.copy()
