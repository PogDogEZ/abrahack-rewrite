#!/usr/bin/env python3
from typing import IO, List

from pclient.networking.packets import Side, Packet
from pclient.networking.types import Enum
from pclient.networking.types.basic import UnsignedShort, String, Bytes, Boolean, Integer, VarInt, Float, Short, Long
from viewer.network.types import ChunkPositionSpec, TrackedPlayerSpec, ChunkStateSpec, AngleSpec, PositionSpec, \
    PlayerSpec, ParameterSpec, ParamDescriptionSpec, TrackerSpec
from viewer.util import ChunkState, ChunkPosition, Position, Angle, TrackedPlayer, Player, ActiveTask, RegisteredTask, \
    Tracker

ID_OFFSET = 255


class YCInitRequestPacket(Packet):

    ID = ID_OFFSET
    NAME = "yc_init_request"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.client_type = YCInitRequestPacket.ClientType.LISTENING

        self.handler_hash = b""
        self.handler_public_key = b""
        self.handler_name = ""

        self.host_name = ""
        self.host_port = 25565

    def read(self, fileobj: IO) -> None:
        self.client_type = YCInitRequestPacket.ClientType.read(fileobj)

        if self.client_type in (YCInitRequestPacket.ClientType.REPORTING, YCInitRequestPacket.ClientType.ARCHIVING):
            self.handler_hash = Bytes.read(fileobj)
            self.handler_public_key = Bytes.read(fileobj)

        self.handler_name = String.read(fileobj)

        self.host_name = String.read(fileobj)
        self.host_port = UnsignedShort.read(fileobj)

    def write(self, fileobj: IO) -> None:
        YCInitRequestPacket.ClientType.write(self.client_type, fileobj)

        if self.client_type in (YCInitRequestPacket.ClientType.REPORTING, YCInitRequestPacket.ClientType.ARCHIVING):
            Bytes.write(self.handler_hash, fileobj)
            Bytes.write(self.handler_public_key, fileobj)

        String.write(self.handler_name, fileobj)

        String.write(self.host_name, fileobj)
        UnsignedShort.write(self.host_port, fileobj)

    class ClientType(Enum):
        LISTENING = 0
        REPORTING = 1
        ARCHIVING = 2


class YCInitResponsePacket(Packet):

    ID = ID_OFFSET + 1
    NAME = "yc_init_response"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.extended_init = False
        self.identity_proof_signature = b""

        self.rejected = False
        self.message = ""
        self.handler_id = 0

    def read(self, fileobj: IO) -> None:
        self.extended_init = Boolean.read(fileobj)

        if self.extended_init:
            self.identity_proof_signature = Bytes.read(fileobj)

        else:
            self.rejected = Boolean.read(fileobj)
            self.message = String.read(fileobj)

            if not self.rejected:
                self.handler_id = UnsignedShort.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Boolean.write(self.extended_init, fileobj)

        if self.extended_init:
            Bytes.write(self.identity_proof_signature, fileobj)

        else:
            Boolean.write(self.rejected, fileobj)
            String.write(self.message, fileobj)

            if not self.rejected:
                UnsignedShort.write(self.handler_id, fileobj)


class YCExtendedResponsePacket(Packet):

    ID = ID_OFFSET + 2
    NAME = "yc_extended_response"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.identity_proof_nonce = b""

    def read(self, fileobj: IO) -> None:
        self.identity_proof_nonce = Bytes.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Bytes.write(self.identity_proof_nonce, fileobj)


class UpdateDataIDsPacket(Packet):
    ID = ID_OFFSET + 3
    NAME = "update_data_ids"
    SIDE = Side.BOTH

    def __init__(self) -> None:
        super().__init__()

        self.data_type = UpdateDataIDsPacket.DataType.CHUNK_STATE
        self.data_id_max = 0
        self.data_id_min = 0

    def read(self, fileobj: IO) -> None:
        self.data_type = UpdateDataIDsPacket.DataType.read(fileobj)
        self.data_id_max = VarInt.read(fileobj)
        self.data_id_min = VarInt.read(fileobj)

    def write(self, fileobj: IO) -> None:
        UpdateDataIDsPacket.DataType.write(self.data_type, fileobj)
        VarInt.write(self.data_id_max, fileobj)
        VarInt.write(self.data_id_min, fileobj)

    class DataType(Enum):
        CHUNK_STATE = 0
        RENDER_DISTANCE = 1
        TRACKED_PLAYER = 2
        ONLINE_PLAYER = 3


class DataRequestPacket(Packet):

    ID = ID_OFFSET + 4
    NAME = "data_request"
    SIDE = Side.BOTH

    def __init__(self) -> None:
        super().__init__()

        self.request_type = DataRequestPacket.RequestType.DOWNLOAD
        self.data_type = DataRequestPacket.DataType.CHUNK_STATE

        self._data_ids = []

        self.chunk_size = 65536
        self.expected_parts = 0

    def read(self, fileobj: IO) -> None:
        self.request_type = DataRequestPacket.RequestType.read(fileobj)

        if self.request_type == DataRequestPacket.RequestType.DOWNLOAD:
            self.data_type = DataRequestPacket.DataType.read(fileobj)
            self._data_ids.clear()

            ids_to_read = UnsignedShort.read(fileobj)
            for index in range(ids_to_read):
                self._data_ids.append(VarInt.read(fileobj))

        elif self.request_type == DataRequestPacket.RequestType.UPLOAD:
            self.chunk_size = Integer.read(fileobj)
            self.expected_parts = UnsignedShort.read(fileobj)

    def write(self, fileobj: IO) -> None:
        DataRequestPacket.RequestType.write(self.request_type, fileobj)

        if self.request_type == DataRequestPacket.RequestType.DOWNLOAD:
            DataRequestPacket.DataType.write(self.data_type, fileobj)
            UnsignedShort.write(len(self._data_ids), fileobj)
            for data_id in self._data_ids:
                VarInt.write(data_id, fileobj)

        elif self.request_type == DataRequestPacket.RequestType.UPLOAD:
            Integer.write(self.chunk_size, fileobj)
            UnsignedShort.write(self.expected_parts, fileobj)

    def get_data_ids(self) -> List[int]:
        return self._data_ids.copy()

    def add_data_id(self, data_id: int) -> None:
        self._data_ids.append(data_id)

    def set_data_ids(self, data_ids: List[int]) -> None:
        self._data_ids.clear()
        self._data_ids.extend(data_ids)

    def extend_data_ids(self, data_ids: List[int]) -> None:
        self._data_ids.extend(data_ids)

    def remove_data_ids(self, data_id: int) -> None:
        self._data_ids.remove(data_id)

    class RequestType(Enum):
        DOWNLOAD = 0
        UPLOAD = 1
        CANCEL = 2

    class DataType(Enum):
        CHUNK_STATE = 0
        RENDER_DISTANCE = 1
        TRACKED_PLAYER = 2
        ONLINE_PLAYER = 3


class DataResponsePacket(Packet):

    ID = ID_OFFSET + 5
    NAME = "data_response"
    SIDE = Side.BOTH

    def __init__(self) -> None:
        super().__init__()

        self.chunk_size = 65536
        self.expected_parts = 0

    def read(self, fileobj: IO) -> None:
        self.chunk_size = Integer.read(fileobj)
        self.expected_parts = UnsignedShort.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Integer.write(self.chunk_size, fileobj)
        UnsignedShort.write(self.expected_parts, fileobj)


class DataPartPacket(Packet):

    ID = ID_OFFSET + 6
    NAME = "data_part"
    SIDE = Side.BOTH

    def __init__(self) -> None:
        super().__init__()

        self.data_part = 0
        self.data = b""

    def read(self, fileobj: IO) -> None:
        self.data_part = UnsignedShort.read(fileobj)
        self.data = Bytes.read(fileobj)

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(self.data_part, fileobj)
        Bytes.write(self.data, fileobj)


class ReporterActionPacket(Packet):

    ID = ID_OFFSET + 7
    NAME = "reporter_action"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.action = ReporterActionPacket.Action.ADD
        self.reporter_id = 0
        self.reporter_name = ""

    def read(self, fileobj: IO) -> None:
        self.action = ReporterActionPacket.Action.read(fileobj)
        self.reporter_id = UnsignedShort.read(fileobj)

        if self.action == ReporterActionPacket.Action.ADD:
            self.reporter_name = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        ReporterActionPacket.Action.write(self.action, fileobj)
        UnsignedShort.write(self.reporter_id, fileobj)

        if self.action == ReporterActionPacket.Action.ADD:
            String.write(self.reporter_name, fileobj)

    class Action(Enum):
        ADD = 0
        REMOVE = 1


class SelectReporterPacket(Packet):

    ID = ID_OFFSET + 8
    NAME = "select_reporter"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.selected_reporter = 0

    def read(self, fileobj: IO) -> None:
        self.selected_reporter = Integer.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Integer.write(self.selected_reporter, fileobj)


class SyncReporterPacket(Packet):

    ID = ID_OFFSET + 9
    NAME = "sync_reporter"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.has_reporter = False
        self.reporter_id = 0
        self.reporter_name = ""

        self._registered_tasks = []
        self._active_tasks = []
        self._players = []
        self._trackers = []

    def read(self, fileobj: IO) -> None:
        self.has_reporter = Boolean.read(fileobj)

        if self.has_reporter:
            self.reporter_id = UnsignedShort.read(fileobj)
            self.reporter_name = String.read(fileobj)

            self._registered_tasks.clear()
            self._players.clear()

            registered_tasks_to_read = UnsignedShort.read(fileobj)
            for index in range(registered_tasks_to_read):
                task_name = String.read(fileobj)
                task_description = String.read(fileobj)

                param_descriptions = []

                param_descriptions_to_read = UnsignedShort.read(fileobj)
                for p_index in range(param_descriptions_to_read):
                    param_descriptions.append(ParamDescriptionSpec.read(fileobj))

                self._registered_tasks.append(RegisteredTask(task_name, task_description, param_descriptions))

            active_tasks_to_read = UnsignedShort.read(fileobj)
            for index in range(active_tasks_to_read):
                task_id = UnsignedShort.read(fileobj)
                task_name = String.read(fileobj)

                parameters = []

                parameters_to_read = UnsignedShort.read(fileobj)
                for p_index in range(parameters_to_read):
                    parameters.append(ParameterSpec.read(fileobj))

                progress = Float.read(fileobj)
                time_elapsed = Integer.read(fileobj)

                results = []

                results_to_read = UnsignedShort.read(fileobj)
                for r_index in range(results_to_read):
                    results.append(String.read(fileobj))

                for registered_task in self._registered_tasks:
                    if registered_task.name == task_name:
                        self._active_tasks.append(ActiveTask(registered_task, task_id, parameters, progress,
                                                             time_elapsed, results))

            players_to_read = UnsignedShort.read(fileobj)
            for index in range(players_to_read):
                self._players.append(PlayerSpec.read(fileobj))

            trackers_to_read = Integer.read(fileobj)
            for index in range(trackers_to_read):
                self._trackers.append(TrackerSpec.read(fileobj))

    def write(self, fileobj: IO) -> None:
        Boolean.write(self.has_reporter, fileobj)

        if self.has_reporter:
            UnsignedShort.write(self.reporter_id, fileobj)
            String.write(self.reporter_name, fileobj)

            UnsignedShort.write(len(self._registered_tasks), fileobj)
            for registered_task in self._registered_tasks:
                String.write(registered_task.name, fileobj)
                String.write(registered_task.description, fileobj)

                UnsignedShort.write(len(registered_task.param_descriptions), fileobj)
                for param_description in registered_task.param_descriptions:
                    ParamDescriptionSpec.write(param_description, fileobj)

            UnsignedShort.write(len(self._active_tasks), fileobj)
            for active_task in self._active_tasks:
                UnsignedShort.write(active_task.task_id, fileobj)
                String.write(active_task.registered_task.name, fileobj)

                UnsignedShort.write(len(active_task.parameters), fileobj)
                for parameter in active_task.parameters:
                    ParameterSpec.write(parameter, fileobj)

                Float.write(active_task.progress, fileobj)
                Integer.write(active_task.time_elapsed, fileobj)

                UnsignedShort.write(len(active_task.results), fileobj)
                for result in active_task.results:
                    String.write(result, fileobj)

            UnsignedShort.write(len(self._players), fileobj)
            for player in self._players:
                PlayerSpec.write(player, fileobj)

            Integer.write(len(self._trackers), fileobj)
            for tracker in self._trackers:
                TrackerSpec.write(tracker, fileobj)

    def get_registered_tasks(self) -> List[RegisteredTask]:
        return self._registered_tasks.copy()

    def add_registered_task(self, registered_task: RegisteredTask) -> None:
        self._registered_tasks.append(registered_task)

    def set_registered_tasks(self, registered_tasks: List[RegisteredTask]) -> None:
        self._registered_tasks.clear()
        self._registered_tasks.extend(registered_tasks)

    def extend_registered_task(self, registered_tasks: List[RegisteredTask]) -> None:
        self._registered_tasks.extend(registered_tasks)

    def remove_registered_task(self, registered_task: RegisteredTask) -> None:
        self._registered_tasks.remove(registered_task)

    def get_active_tasks(self) -> List[ActiveTask]:
        return self._active_tasks.copy()

    def add_active_task(self, active_task: ActiveTask) -> None:
        self._active_tasks.append(active_task)

    def set_active_tasks(self, active_tasks: List[ActiveTask]) -> None:
        self._active_tasks.clear()
        self._active_tasks.extend(active_tasks)

    def extend_active_task(self, active_tasks: List[ActiveTask]) -> None:
        self._active_tasks.extend(active_tasks)

    def remove_active_task(self, active_task: ActiveTask) -> None:
        self._active_tasks.remove(active_task)

    def get_players(self) -> List[Player]:
        return self._players.copy()

    def add_player(self, player: Player) -> None:
        self._players.append(player)

    def set_players(self, players: List[Player]) -> None:
        self._players.clear()
        self._players.extend(players)

    def extend_players(self, players: List[Player]) -> None:
        self._players.extend(players)

    def remove_player(self, player: Player) -> None:
        self._players.remove(player)

    def get_trackers(self) -> List[Tracker]:
        return self._trackers.copy()

    def add_tracker(self, tracker: Tracker) -> None:
        self._trackers.append(tracker)

    def set_trackers(self, trackers: List[Tracker]) -> None:
        self._trackers.clear()
        self._trackers.extend(trackers)

    def extend_trackers(self, trackers: List[Tracker]) -> None:
        self._trackers.extend(trackers)

    def remove_tracker(self, tracker: Tracker) -> None:
        self._trackers.remove(tracker)


class TaskActionPacket(Packet):

    ID = ID_OFFSET + 10
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


class PlayerActionPacket(Packet):

    ID = ID_OFFSET + 11
    NAME = "player_action"
    SIDE = Side.SERVER

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


class AccountActionPacket(Packet):

    ID = ID_OFFSET + 12
    NAME = "account_action"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.action = AccountActionPacket.Action.ADD
        self.username = ""
        self.access_token = ""
        self.client_token = ""

    def read(self, fileobj: IO) -> None:
        self.action = AccountActionPacket.Action.read(fileobj)

        self.username = String.read(fileobj)

        if self.action == AccountActionPacket.Action.ADD:
            self.access_token = String.read(fileobj)
            self.client_token = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        AccountActionPacket.Action.write(self.action, fileobj)

        String.write(self.username, fileobj)

        if self.action == AccountActionPacket.Action.ADD:
            String.write(self.access_token, fileobj)
            String.write(self.client_token, fileobj)

    class Action(Enum):
        ADD = 0
        REMOVE = 1


class AccountActionResponsePacket(Packet):

    ID = ID_OFFSET + 13
    NAME = "account_action_response"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.successful = True
        self.message = ""

    def read(self, fileobj: IO) -> None:
        self.successful = Boolean.read(fileobj)
        self.message = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Boolean.write(self.successful, fileobj)
        String.write(self.message, fileobj)


class ChunkStatesPacket(Packet):

    ID = ID_OFFSET + 14
    NAME = "chunk_states"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self._chunk_states = []

    def read(self, fileobj: IO) -> None:
        self._chunk_states.clear()

        chunk_states_to_read = UnsignedShort.read(fileobj)
        for index in range(chunk_states_to_read):
            self._chunk_states.append(ChunkStateSpec.read(fileobj))

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(len(self._chunk_states), fileobj)
        for chunk_state in self._chunk_states:
            ChunkStateSpec.write(chunk_state, fileobj)

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

    ID = ID_OFFSET + 15
    NAME = "tracker_action"
    SIDE = Side.SERVER

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

    ID = ID_OFFSET + 16
    NAME = "info_update"
    SIDE = Side.SERVER

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


packets = (
    YCInitRequestPacket,
    YCInitResponsePacket,
    YCExtendedResponsePacket,
    UpdateDataIDsPacket,
    DataRequestPacket,
    DataResponsePacket,
    DataPartPacket,
    ReporterActionPacket,
    SelectReporterPacket,
    SyncReporterPacket,
    TaskActionPacket,
    PlayerActionPacket,
    AccountActionPacket,
    AccountActionResponsePacket,
    ChunkStatesPacket,
    TrackerActionPacket,
    InfoUpdatePacket,
)
