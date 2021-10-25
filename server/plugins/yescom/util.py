#!/usr/bin/env python3

import typing
from typing import List, Tuple, Dict
from uuid import UUID

import plugins.yescom.network.types as network_types
from network.networking.types import Enum, Type
from network.networking.types.basic import Short, String, Integer, Float, Boolean


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

    @classmethod
    def value_to_serializable(cls, value) -> typing.Type[Type]:
        return (network_types.PositionSpec, network_types.AngleSpec, network_types.ChunkPositionSpec, Short, Priority,
                String, Integer, Float, Boolean)[value]

    @classmethod
    def value_to_type(cls, value) -> type:
        return (Position, Angle, ChunkPosition, int, Priority, str, int, float, bool)[value]

    @classmethod
    def type_to_value(cls, data_type: type):  # -> DataType:
        return (Position, Angle, ChunkPosition, int, Priority, str, int, float, bool).index(data_type)


class Dimension(Enum):
    NETHER = 0
    OVERWORLD = 1
    END = 2


class Priority(Enum):
    LOW = 0
    MEDIUM = 1
    HIGH = 2
    USER = 3


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
        return "Player(username=%r, display_name=%r)" % (self._username, self._display_name)

    def set_profile_details(self, uuid: UUID, display_name: str) -> None:
        if self._uuid is None:
            self._uuid = uuid
        if not self._display_name:
            self._display_name = display_name


class RegisteredTask:

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

    def __eq__(self, other) -> bool:
        if not isinstance(other, RegisteredTask):
            return False
        else:
            return other._name == self._name and other._description == self._description

    def get_param_description(self, param_name: str):  # -> ParamDescription
        for param_description in self._param_descriptions:
            if param_description.name == param_name:
                return param_description

        raise LookupError("Couldn't find param description by name %r." % param_name)

    def create_new(self, task_id: int, **kwargs):  # -> ActiveTask:
        parameters = []
        for param_name in kwargs:
            try:
                param_description = self.get_param_description(param_name)
                parameters.append(ActiveTask.Parameter(param_description, kwargs[param_name]))
            except LookupError:
                ...

        return ActiveTask(self, task_id, parameters, 0, 0, [])

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
        def data_type(self) -> DataType:
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

    @property
    def registered_task(self) -> RegisteredTask:
        return self._registered_task

    @property
    def task_id(self) -> int:
        return self._task_id

    @property
    def parameters(self):  # -> List[Parameter]
        return self._parameters.copy()

    @property
    def loaded_chunk_task(self) -> bool:
        return self._loaded_chunk_task

    @property
    def progress(self) -> float:
        return self._progress

    @property
    def time_elapsed(self) -> int:
        return self._time_elapsed

    @property
    def current_position(self) -> ChunkPosition:
        return self._current_position

    @property
    def results(self) -> List[str]:
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
        return self._error_x

    @property
    def error_z(self) -> float:
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

    def __init__(self, previous_render_distances: Dict[int, int]) -> None:
        self._previous_render_distances = previous_render_distances.copy()

    def get_render_distances(self) -> Dict[int, int]:
        return self._previous_render_distances.copy()


class TrackedPlayer:

    @property
    def tracked_player_id(self) -> int:
        return self._tracked_player_id

    @property
    def tracking_data(self) -> TrackingData:
        return self._tracking_data

    @property
    def render_distance(self) -> RenderDistance:
        return self._render_distance

    @property
    def dimension(self) -> int:
        return self._dimension

    @property
    def logged_out(self) -> bool:
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

    @property
    def tracked_id(self) -> int:
        return self._tracker_id

    @property
    def tracked_player(self) -> TrackedPlayer:
        return self._tracked_player

    @tracked_player.setter
    def tracked_player(self, tracked_player : TrackedPlayer) -> None:
        self._tracked_player = tracked_player

    def __init__(self, tracker_id: int, tracked_player: TrackedPlayer) -> None:
        self._tracker_id = tracker_id
        self._tracked_player = tracked_player

    def __repr__(self) -> str:
        return "Tracker(ID=%i, player=%r)" % (self._tracker_id, self._tracked_player)
