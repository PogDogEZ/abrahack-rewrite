#!/usr/bin/env python3

from typing import IO, List

from network.networking.packets import Packet, Side
from network.networking.types import Enum
from network.networking.types.basic import Integer, String, UnsignedShort, Boolean, Float, Short
from plugins.yescom.network.types import PlayerSpec, ParamDescriptionSpec, Priority, Dimension, ChunkPositionSpec, \
    AngleSpec, PositionSpec
from plugins.yescom.util import Player, Task, Position, Angle

ID_OFFSET = 255


class ReporterActionPacket(Packet):

    ID = ID_OFFSET + 2
    NAME = "reporter_action"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.action = ReporterActionPacket.Action.ADD
        self.reporter_id = 0
        self.reporter_name = ""
        self.reporter_host = ""
        self.reporter_port = 0

    def read(self, fileobj: IO) -> None:
        self.action = ReporterActionPacket.Action.read(fileobj)
        self.reporter_id = UnsignedShort.read(fileobj)

        if self.action == ReporterActionPacket.Action.ADD:
            self.reporter_name = String.read(fileobj)
            self.reporter_host = String.read(fileobj)
            self.reporter_port = Integer.read(fileobj)

    def write(self, fileobj: IO) -> None:
        ReporterActionPacket.Action.write(self.action, fileobj)
        UnsignedShort.write(self.reporter_id, fileobj)

        if self.action == ReporterActionPacket.Action.ADD:
            String.write(self.reporter_name, fileobj)
            String.write(self.reporter_host, fileobj)
            Integer.write(self.reporter_port, fileobj)

    class Action(Enum):
        ADD = 0
        REMOVE = 1


class SelectReporterPacket(Packet):

    ID = ID_OFFSET + 3
    NAME = "select_reporter"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.selected_reporter = 0

    def read(self, fileobj: IO) -> None:
        self.selected_reporter = UnsignedShort.read(fileobj)

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(self.selected_reporter, fileobj)


class SyncReporterPacket(Packet):

    ID = ID_OFFSET + 4
    NAME = "sync_reporter"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.has_reporter = False
        self.reporter_id = 0
        self.reporter_name = ""

        self._reporter_tasks = []
        self._reporter_players = []

    def read(self, fileobj: IO) -> None:
        self.has_reporter = Boolean.read(fileobj)

        if self.has_reporter:
            self.reporter_id = UnsignedShort.read(fileobj)
            self.reporter_name = String.read(fileobj)

            self._reporter_tasks.clear()
            self._reporter_players.clear()

            tasks_to_read = UnsignedShort.read(fileobj)
            for index in range(tasks_to_read):
                task_name = String.read(fileobj)
                task_description = String.read(fileobj)

                param_descriptions = []

                param_descriptions_to_read = UnsignedShort.read(fileobj)
                for p_index in range(param_descriptions_to_read):
                    param_descriptions.append(ParamDescriptionSpec.read(fileobj))

                self._reporter_tasks.append(Task(task_name, task_description, param_descriptions))

            players_to_read = UnsignedShort.read(fileobj)
            for index in range(players_to_read):
                self._reporter_players.append(PlayerSpec.read(fileobj))

    def write(self, fileobj: IO) -> None:
        Boolean.write(self.has_reporter, fileobj)

        if self.has_reporter:
            UnsignedShort.write(self.reporter_id, fileobj)
            String.write(self.reporter_name, fileobj)

            UnsignedShort.write(len(self._reporter_tasks), fileobj)
            for task in self._reporter_tasks:
                String.write(task.name, fileobj)
                String.write(task.description, fileobj)

                for param_description in task.param_descriptions:
                    ParamDescriptionSpec.write(param_description, fileobj)

            UnsignedShort.write(len(self._reporter_players), fileobj)
            for player in self._reporter_players:
                PlayerSpec.write(player, fileobj)

    def get_tasks(self) -> List[Task]:
        return self._reporter_tasks.copy()

    def set_tasks(self, tasks: List[Task]) -> None:
        self._reporter_tasks.clear()
        self._reporter_tasks.extend(tasks)

    def extend_task(self, tasks: List[Task]) -> None:
        self._reporter_tasks.extend(tasks)

    def add_task(self, task: Task) -> None:
        self._reporter_tasks.append(task)

    def get_players(self) -> List[Player]:
        return self._reporter_players.copy()

    def set_players(self, players: List[Player]) -> None:
        self._reporter_players.clear()
        self._reporter_players.extend(players)

    def extend_player(self, players: List[Player]) -> None:
        self._reporter_players.extend(players)

    def add_player(self, player: Player) -> None:
        self._reporter_players.append(player)


class TaskActionPacket(Packet):

    ID = ID_OFFSET + 5
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


class PlayerActionPacket(Packet):

    ID = ID_OFFSET + 6
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

