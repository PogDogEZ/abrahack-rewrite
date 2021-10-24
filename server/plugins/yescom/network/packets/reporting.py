#!/usr/bin/env python3

from typing import IO, List

from network.networking.packets import Packet, Side, String
from network.networking.types import Enum
from network.networking.types.basic import Boolean, UnsignedInteger, Float, UnsignedShort, Short, Integer, Long
from plugins.yescom.network.types import PlayerSpec, PositionSpec, AngleSpec, ParamDescriptionSpec, ChunkPositionSpec, \
    ParameterSpec, TrackedPlayerSpec, ChunkStateSpec, TrackerSpec
from plugins.yescom.util import Position, Angle, RegisteredTask, ChunkPosition, ChunkState, ActiveTask

ID_OFFSET = 255


class ConfigActionPacket(Packet):  # TODO: This packet

    ID = ID_OFFSET + 7
    NAME = "config_action"
    SIDE = Side.BOTH

    def __init__(self) -> None:
        super().__init__()

    def read(self, fileobj: IO) -> None:
        ...

    def write(self, fileobj: IO) -> None:
        ...

    class Action(Enum):
        ...


class TaskSyncPacket(Packet):

    ID = ID_OFFSET + 8
    NAME = "task_sync"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self._registered_tasks = []

    def read(self, fileobj: IO) -> None:
        self._registered_tasks.clear()

        tasks_to_read = UnsignedShort.read(fileobj)
        for index in range(tasks_to_read):
            name = String.read(fileobj)
            description = String.read(fileobj)

            param_descriptions = []

            param_descriptions_to_read = UnsignedShort.read(fileobj)
            for p_index in range(param_descriptions_to_read):
                param_descriptions.append(ParamDescriptionSpec.read(fileobj))

            self._registered_tasks.append(RegisteredTask(name, description, param_descriptions))

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(len(self._registered_tasks), fileobj)
        for task in self._registered_tasks:
            String.write(task.name, fileobj)
            String.write(task.description, fileobj)

            UnsignedShort.write(len(task.param_descriptions), fileobj)
            for param_description in task.param_descriptions:
                ParamDescriptionSpec.write(param_description, fileobj)

    def get_registered_tasks(self) -> List[RegisteredTask]:
        return self._registered_tasks.copy()

    def add_registered_task(self, registered_task: RegisteredTask) -> None:
        self._registered_tasks.append(registered_task)

    def set_registered_tasks(self, registered_tasks: List[RegisteredTask]) -> None:
        self._registered_tasks.clear()
        self._registered_tasks.extend(registered_tasks)

    def extend_registered_tasks(self, registered_tasks: List[RegisteredTask]) -> None:
        self._registered_tasks.extend(registered_tasks)

    def remove_registered_task(self, registered_task: RegisteredTask) -> None:
        self._registered_tasks.remove(registered_task)


class TaskActionPacket(Packet):

    ID = ID_OFFSET + 9
    NAME = "task_action"
    SIDE = Side.BOTH

    def __init__(self) -> None:
        super().__init__()

        self.action = TaskActionPacket.Action.ADD
        self.task_name = ""
        self._task_params = []
        self.task_id = 0
        self.loaded_chunk_task = False

        self.progress = 0
        self.time_elapsed = 0
        self.current_position = ChunkPosition(0, 0)

        self.result = ""

    def read(self, fileobj: IO) -> None:
        self._task_params.clear()

        self.action = TaskActionPacket.Action.read(fileobj)

        if self.action == TaskActionPacket.Action.START:
            self.task_name = String.read(fileobj)

            params_to_read = UnsignedShort.read(fileobj)
            for index in range(params_to_read):
                self._task_params.append(ParameterSpec.read(fileobj))

        elif self.action == TaskActionPacket.Action.STOP:
            self.task_id = UnsignedShort.read(fileobj)

        elif self.action == TaskActionPacket.Action.ADD:
            self.task_id = UnsignedShort.read(fileobj)
            self.task_name = String.read(fileobj)

            params_to_read = UnsignedShort.read(fileobj)
            for index in range(params_to_read):
                self._task_params.append(ParameterSpec.read(fileobj))

        elif self.action == TaskActionPacket.Action.REMOVE:
            self.task_id = UnsignedShort.read(fileobj)

        elif self.action == TaskActionPacket.Action.UPDATE:
            self.loaded_chunk_task = Boolean.read(fileobj)

            self.progress = Float.read(fileobj)
            self.time_elapsed = Integer.read(fileobj)

            if self.loaded_chunk_task:
                self.current_position = ChunkPositionSpec.read(fileobj)

        elif self.action == TaskActionPacket.Action.RESULT:
            self.result = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        TaskActionPacket.Action.write(self.action, fileobj)

        if self.action == TaskActionPacket.Action.START:
            String.write(self.task_name, fileobj)

            UnsignedShort.write(len(self._task_params), fileobj)
            for parameter in self._task_params:
                ParameterSpec.write(parameter, fileobj)

        elif self.action == TaskActionPacket.Action.STOP:
            UnsignedShort.write(self.task_id, fileobj)

        elif self.action == TaskActionPacket.Action.ADD:
            UnsignedShort.write(self.task_id, fileobj)
            String.write(self.task_name, fileobj)

            UnsignedShort.write(len(self._task_params), fileobj)
            for parameter in self._task_params:
                ParameterSpec.write(parameter, fileobj)

        elif self.action == TaskActionPacket.Action.REMOVE:
            UnsignedShort.write(self.task_id, fileobj)

        elif self.action == TaskActionPacket.Action.UPDATE:
            Boolean.write(self.loaded_chunk_task, fileobj)

            Float.write(self.progress, fileobj)
            Integer.write(self.time_elapsed, fileobj)

            if self.loaded_chunk_task:
                ChunkPositionSpec.write(self.current_position, fileobj)

        elif self.action == TaskActionPacket.Action.RESULT:
            String.write(self.result, fileobj)

    def get_task_params(self) -> List[ActiveTask.Parameter]:
        return self._task_params

    def add_task_param(self, task_param: ActiveTask.Parameter) -> None:
        self._task_params.append(task_param)

    def set_task_params(self, task_params: List[ActiveTask.Parameter]) -> None:
        self._task_params.clear()
        self._task_params.extend(task_params)

    def extend_task_params(self, task_params: List[ActiveTask.Parameter]) -> None:
        self._task_params.extend(task_params)

    def remove_task_param(self, task_param: ActiveTask.Parameter) -> None:
        self._task_params.remove(task_param)

    class Action(Enum):
        START = 0
        STOP = 1
        ADD = 2
        REMOVE = 3
        UPDATE = 4
        RESULT = 5


class AccountActionPacket(Packet):

    ID = ID_OFFSET + 10
    NAME = "account_action"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.action = AccountActionPacket.Action.ADD
        self.action_id = 0
        self.username = ""
        self.access_token = ""
        self.client_token = ""

    def read(self, fileobj: IO) -> None:
        self.action = AccountActionPacket.Action.read(fileobj)
        self.action_id = UnsignedInteger.read(fileobj)
        self.username = String.read(fileobj)

        if self.action == AccountActionPacket.Action.ADD:
            self.access_token = String.read(fileobj)
            self.client_token = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        AccountActionPacket.Action.write(self.action, fileobj)
        UnsignedInteger.write(self.action_id, fileobj)
        String.write(self.username, fileobj)

        if self.action == AccountActionPacket.Action.ADD:
            String.write(self.access_token, fileobj)
            String.write(self.client_token, fileobj)

    class Action(Enum):
        ADD = 0
        REMOVE = 1


class AccountActionResponsePacket(Packet):

    ID = ID_OFFSET + 11
    NAME = "account_action_response"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.action_id = 0
        self.successful = True
        self.message = ""

    def read(self, fileobj: IO) -> None:
        self.action_id = UnsignedInteger.read(fileobj)
        self.successful = Boolean.read(fileobj)
        self.message = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        UnsignedInteger.write(self.action_id, fileobj)
        Boolean.write(self.successful, fileobj)
        String.write(self.message, fileobj)


class PlayerActionPacket(Packet):

    ID = ID_OFFSET + 12
    NAME = "player_action"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.action = PlayerActionPacket.Action.ADD

        self.player = None

        self.player_name = ""

        self.new_position = Position(0, 0, 0)
        self.new_angle = Angle(0, 0)

        self.new_dimension = 0

        self.new_health = 20
        self.new_hunger = 20
        self.new_saturation = 5

    def read(self, fileobj: IO) -> None:
        self.action = PlayerActionPacket.Action.read(fileobj)

        if self.action == PlayerActionPacket.Action.ADD:
            self.player = PlayerSpec.read(fileobj)
        else:
            self.player_name = String.read(fileobj)

            if self.action == PlayerActionPacket.Action.UPDATE_POSITION:
                self.new_position = PositionSpec.read(fileobj)
                self.new_angle = AngleSpec.read(fileobj)

            elif self.action == PlayerActionPacket.Action.UPDATE_DIMENSION:
                self.new_dimension = Short.read(fileobj)

            elif self.action == PlayerActionPacket.Action.UPDATE_HEALTH:
                self.new_health = Float.read(fileobj)
                self.new_hunger = UnsignedShort.read(fileobj)
                self.new_saturation = Float.read(fileobj)

    def write(self, fileobj: IO) -> None:
        PlayerActionPacket.Action.write(self.action, fileobj)

        if self.action == PlayerActionPacket.Action.ADD:
            PlayerSpec.write(self.player, fileobj)
        else:
            String.write(self.player_name, fileobj)

            if self.action == PlayerActionPacket.Action.UPDATE_POSITION:
                PositionSpec.write(self.new_position, fileobj)
                AngleSpec.write(self.new_angle, fileobj)

            elif self.action == PlayerActionPacket.Action.UPDATE_DIMENSION:
                Short.write(self.new_dimension, fileobj)

            elif self.action == PlayerActionPacket.Action.UPDATE_HEALTH:
                Float.write(self.new_health, fileobj)
                UnsignedShort.write(self.new_hunger, fileobj)
                Float.write(self.new_saturation, fileobj)

    class Action(Enum):
        ADD = 0
        REMOVE = 1
        UPDATE_POSITION = 2
        UPDATE_DIMENSION = 3
        UPDATE_HEALTH = 4


class ChunkStatesPacket(Packet):

    ID = ID_OFFSET + 13
    NAME = "chunk_states"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self._chunk_states = []

    def read(self, fileobj: IO) -> None:
        self._chunk_states.clear()

        chunk_states_to_read = UnsignedShort.read(fileobj)
        for index in range(chunk_states_to_read):
            self._chunk_states.append(ChunkStateSpec.read(fileobj))

    def write(self, fileobj: IO) -> None:
        ...

    def get_chunk_states(self) -> List[ChunkState]:
        return self._chunk_states.copy()

    def add_chunk_state(self, chunk_state: ChunkState) -> None:
        self._chunk_states.append(chunk_state)

    def set_chunk_states(self, chunk_states: List[ChunkState]) -> None:
        self._chunk_states.clear()
        self._chunk_states.extend(chunk_states)

    def extend_chunk_states(self, chunk_states: List[ChunkState]) -> None:
        self._chunk_states.extend(chunk_states)

    def remove_chunk_state(self, chunk_state: ChunkState) -> None:
        self._chunk_states.remove(chunk_state)


class TrackerActionPacket(Packet):

    ID = ID_OFFSET + 14
    NAME = "tracker_action"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.action = TrackerActionPacket.Action.ADD
        self.tracker = None
        self.tracker_id = 0
        self.tracked_player = None

    def read(self, fileobj: IO) -> None:
        self.action = TrackerActionPacket.Action.read(fileobj)

        if self.action == TrackerActionPacket.Action.ADD:
            self.tracker = TrackerSpec.read(fileobj)
        else:
            self.tracker_id = Long.read(fileobj)

            if self.action == TrackerActionPacket.Action.UPDATE:
                self.tracked_player = TrackedPlayerSpec.read(fileobj)

    def write(self, fileobj: IO) -> None:
        TrackerActionPacket.Action.write(self.action, fileobj)

        if self.action == TrackerActionPacket.Action.ADD:
            TrackerSpec.write(self.tracker, fileobj)
        else:
            Long.write(self.tracker_id, fileobj)

            if self.action == TrackerActionPacket.Action.UPDATE:
                TrackedPlayerSpec.write(self.tracked_player, fileobj)

    class Action(Enum):
        ADD = 0
        REMOVE = 1
        UPDATE = 2


class InfoUpdatePacket(Packet):

    ID = ID_OFFSET + 15
    NAME = "info_update"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.waiting_queries = 0
        self.ticking_queries = 0
        self.is_connected = False
        self.tick_rate = 20
        self.time_since_last_packet = 0

    def read(self, fileobj: IO) -> None:
        self.waiting_queries = UnsignedShort.read(fileobj)
        self.ticking_queries = UnsignedShort.read(fileobj)
        self.is_connected = Boolean.read(fileobj)

        if self.is_connected:
            self.tick_rate = Float.read(fileobj)
            self.time_since_last_packet = UnsignedShort.read(fileobj)

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(self.waiting_queries, fileobj)
        UnsignedShort.write(self.ticking_queries, fileobj)
        Boolean.write(self.is_connected, fileobj)

        if self.is_connected:
            Float.write(self.tick_rate, fileobj)
            UnsignedShort.write(self.time_since_last_packet, fileobj)
