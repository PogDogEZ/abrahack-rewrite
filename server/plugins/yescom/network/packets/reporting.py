#!/usr/bin/env python3

from typing import IO, List

from network.networking.packets import Packet, Side, String
from network.networking.types import Enum, Type
from network.networking.types.basic import Boolean, UnsignedInteger, Float, UnsignedShort, Short, Integer
from plugins.yescom.network.types import PlayerSpec, PositionSpec, AngleSpec, ParamDescriptionSpec, ChunkPositionSpec, \
    Dimension, Priority
from plugins.yescom.util import Position, Angle, Task, ChunkPosition

ID_OFFSET = 255


class TaskSyncPacket(Packet):

    ID = ID_OFFSET + 3
    NAME = "task_sync"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self._tasks = []

    def read(self, fileobj: IO) -> None:
        self._tasks.clear()

        tasks_to_read = UnsignedShort.read(fileobj)
        for index in range(tasks_to_read):
            name = String.read(fileobj)
            description = String.read(fileobj)

            param_descriptions = []

            param_descriptions_to_read = UnsignedShort.read(fileobj)
            for p_index in range(param_descriptions_to_read):
                param_descriptions.append(ParamDescriptionSpec.read(fileobj))

            self._tasks.append(Task(name, description, param_descriptions))

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(len(self._tasks), fileobj)
        for task in self._tasks:
            String.write(task.name, fileobj)
            String.write(task.description, fileobj)

            UnsignedShort.write(len(task.param_descriptions), fileobj)
            for param_description in task.param_descriptions:
                ParamDescriptionSpec.write(param_description, fileobj)

    def get_tasks(self) -> List[Task]:
        return self._tasks.copy()

    def set_tasks(self, tasks: List[Task]) -> None:
        self._tasks.clear()
        self._tasks.extend(tasks)

    def extend_tasks(self, tasks: List[Task]) -> None:
        self._tasks.extend(tasks)

    def add_task(self, task: Task) -> None:
        self._tasks.append(task)


class TaskActionPacket(Packet):

    ID = ID_OFFSET + 4
    NAME = "task_action"
    SIDE = Side.BOTH

    def __init__(self) -> None:
        super().__init__()

        self.action = TaskActionPacket.Action.ADD
        self.task_name = ""
        self.task_params = {}
        self.task_id = 0

    def read(self, fileobj: IO) -> None:
        self.task_params.clear()

        self.action = TaskActionPacket.Action.read(fileobj)

        if self.action == TaskActionPacket.Action.START:
            self.task_name = String.read(fileobj)

            params_to_read = UnsignedShort.read(fileobj)
            for index in range(params_to_read):
                param_description = ParamDescriptionSpec.read(fileobj)
                value = self._get_serializable(param_description.data_type).read(fileobj)

                self.task_params[param_description] = value

        elif self.action == TaskActionPacket.Action.STOP:
            self.task_id = UnsignedShort.read(fileobj)

        elif self.action == TaskActionPacket.Action.ADD:
            self.task_name = String.read(fileobj)
            self.task_id = UnsignedShort.read(fileobj)

        elif self.action == TaskActionPacket.Action.REMOVE:
            self.task_id = UnsignedShort.read(fileobj)

    def write(self, fileobj: IO) -> None:
        TaskActionPacket.Action.write(self.action, fileobj)

        if self.action == TaskActionPacket.Action.START:
            String.write(self.task_name, fileobj)
            UnsignedShort.write(len(self.task_params), fileobj)

            for parameter in self.task_params:
                ParamDescriptionSpec.write(parameter, fileobj)
                self._get_serializable(parameter.data_type).write(self.task_params[parameter], fileobj)

        elif self.action == TaskActionPacket.Action.STOP:
            UnsignedShort.write(self.task_id, fileobj)

        elif self.action == TaskActionPacket.Action.ADD:
            String.write(self.task_name, fileobj)
            UnsignedShort.write(self.task_id, fileobj)

        elif self.action == TaskActionPacket.Action.REMOVE:
            UnsignedShort.write(self.task_id, fileobj)

    @staticmethod
    def _get_serializable(data_type: Task.ParamDescription.DataType):  # -> Type:  # FIXME: Ugly ass code
        if data_type == Task.ParamDescription.DataType.POSITION:
            return PositionSpec

        elif data_type == Task.ParamDescription.DataType.ANGLE:
            return AngleSpec

        elif data_type == Task.ParamDescription.DataType.CHUNK_POSITION:
            return ChunkPositionSpec

        elif data_type == Task.ParamDescription.DataType.DIMENSION:
            return Dimension

        elif data_type == Task.ParamDescription.DataType.PRIORITY:
            return Priority

        elif data_type == Task.ParamDescription.DataType.STRING:
            return String

        elif data_type == Task.ParamDescription.DataType.INTEGER:
            return Integer

        elif data_type == Task.ParamDescription.DataType.FLOAT:
            return Float

        elif data_type == Task.ParamDescription.DataType.BOOLEAN:
            return Boolean

    class Action(Enum):
        START = 0
        STOP = 1
        ADD = 2
        REMOVE = 3


class AccountActionPacket(Packet):

    ID = ID_OFFSET + 5
    NAME = "account_action"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.action = AccountActionPacket.Action.ADD
        self.action_id = 0
        self.username = ""
        self.password = ""

    def read(self, fileobj: IO) -> None:
        self.action = AccountActionPacket.Action.read(fileobj)
        self.action_id = UnsignedInteger.read(fileobj)
        self.username = String.read(fileobj)

        if self.action == AccountActionPacket.Action.ADD:
            self.password = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        AccountActionPacket.Action.write(self.action, fileobj)
        UnsignedInteger.write(self.action_id, fileobj)
        String.write(self.username, fileobj)

        if self.action == AccountActionPacket.Action.ADD:
            String.write(self.password, fileobj)

    class Action(Enum):
        ADD = 0
        REMOVE = 1


class AccountActionResponsePacket(Packet):

    ID = ID_OFFSET + 6
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

    ID = ID_OFFSET + 7
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


class LoadedChunkPacket(Packet):

    ID = ID_OFFSET + 8
    NAME = "loaded_chunk"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.chunk_position = ChunkPosition(0, 0)
        self.dimension = 0

    def read(self, fileobj: IO) -> None:
        self.chunk_position = ChunkPositionSpec.read(fileobj)
        self.dimension = Short.read(fileobj)

    def write(self, fileobj: IO) -> None:
        ChunkPositionSpec.write(self.chunk_position, fileobj)
        Short.write(self.dimension, fileobj)
