#!/usr/bin/env python3

from typing import IO, List, Dict
from uuid import UUID

from ..network.types import ChunkPositionSpec, TrackedPlayerSpec, ChunkStateSpec, AngleSpec, PositionSpec, PlayerSpec, \
    ParameterSpec, ParamDescriptionSpec, TrackerSpec, ChatMessageSpec, RenderDistanceSpec, TrackingDataSpec, ConfigRuleSpec
from ..util import ChunkPosition, Position, Angle, Player, ActiveTask, RegisteredTask, Tracker, ConfigRule, DataType
from ...pyclient.networking.packets import Side, Packet
from ...pyclient.networking.types import Enum
from ...pyclient.networking.types.basic import UnsignedShort, String, Bytes, Boolean, Integer, VarInt, Float, Short, \
    Long

ID_OFFSET = 255


class YCInitRequestPacket(Packet):

    ID = ID_OFFSET
    NAME = "yc_init_request"
    SIDE = Side.CLIENT

    def __init__(self, client_type=0, handler_hash: bytes = b"", handler_public_key: bytes = b"",
                 handler_name: str = "", host_name: str = "localhost", host_port: int = 25565) -> None:
        super().__init__()

        self.client_type = client_type

        self.handler_hash = handler_hash
        self.handler_public_key = handler_public_key
        self.handler_name = handler_name

        self.host_name = host_name
        self.host_port = host_port

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

    def __init__(self, extended_init: bool = False, identity_proof_signature: bytes = b"", rejected: bool = False,
                 message: str = "", handler_id: int = 0) -> None:
        super().__init__()

        self.extended_init = extended_init
        self.identity_proof_signature = identity_proof_signature

        self.rejected = rejected
        self.message = message
        self.handler_id = handler_id

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

    def __init__(self, identity_proof_nonce: bytes = b"") -> None:
        super().__init__()

        self.identity_proof_nonce = identity_proof_nonce

    def read(self, fileobj: IO) -> None:
        self.identity_proof_nonce = Bytes.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Bytes.write(self.identity_proof_nonce, fileobj)


class DataExchangePacket(Packet):
    """
    Sent by either the server or client to upload or download data.
    """

    ID = ID_OFFSET + 3
    NAME = "data_exchange"
    SIDE = Side.BOTH

    def __init__(self, request_type=0, data_type=0, request_id: int = -1, start_time: int = 0, end_time: int = 0,
                 update_interval: int = 0, max_data_id: int = 0, min_data_id: int = 0) -> None:
        super().__init__()

        self.request_type = request_type
        self.data_type = data_type
        self.request_id = request_id

        self._data = []
        self._invalid_data_ids = []
        self._data_ids = []

        self.start_time = start_time
        self.end_time = end_time
        self.update_interval = update_interval

        self.max_data_id = max_data_id
        self.min_data_id = min_data_id

    def read(self, fileobj: IO) -> None:
        self.request_type = DataExchangePacket.RequestType.read(fileobj)
        self.data_type = DataExchangePacket.DataType.read(fileobj)
        self.request_id = Integer.read(fileobj)

        if self.request_type == DataExchangePacket.RequestType.DOWNLOAD:
            if self.data_type in (DataExchangePacket.DataType.TICK_DATA, DataExchangePacket.DataType.PING_DATA,
                                  DataExchangePacket.DataType.TSLP_DATA):
                self.start_time = Long.read(fileobj)

            else:
                self._data_ids.clear()
                ids_to_read = UnsignedShort.read(fileobj)

                for index in range(ids_to_read):
                    self._data_ids.append(VarInt.read(fileobj))

        elif self.request_type == DataExchangePacket.RequestType.UPLOAD:
            self._invalid_data_ids.clear()

            ids_to_read = UnsignedShort.read(fileobj)
            for index in range(ids_to_read):
                self._invalid_data_ids.append(VarInt.read(fileobj))

            self._data.clear()
            data_to_read = UnsignedShort.read(fileobj)

            if self.data_type in (DataExchangePacket.DataType.TICK_DATA, DataExchangePacket.DataType.PING_DATA,
                                  DataExchangePacket.DataType.TSLP_DATA):
                for index in range(data_to_read):
                    self._data.append(Float.read(fileobj))

                self.start_time = Long.read(fileobj)
                self.end_time = Long.read(fileobj)
                self.update_interval = UnsignedShort.read(fileobj)

            elif self.data_type == DataExchangePacket.DataType.ONLINE_PLAYER:  # TODO: Online player data
                ...

            elif self.data_type == DataExchangePacket.DataType.LOGS:
                for index in range(data_to_read):
                    self._data.append(String.read(fileobj))

            elif self.data_type == DataExchangePacket.DataType.CHAT:
                for index in range(data_to_read):
                    self._data.append(ChatMessageSpec.read(fileobj))

            elif self.data_type == DataExchangePacket.DataType.CHUNK_STATE:
                for index in range(data_to_read):
                    self._data.append(ChunkStateSpec.read(fileobj))

            elif self.data_type == DataExchangePacket.DataType.RENDER_DISTANCE:
                for index in range(data_to_read):
                    self._data.append(RenderDistanceSpec.read(fileobj))

            elif self.data_type == DataExchangePacket.DataType.TRACKED_PLAYER:
                for index in range(data_to_read):
                    self._data.append(TrackedPlayerSpec.read(fileobj))

            elif self.data_type == DataExchangePacket.DataType.TRACKING_DATA:
                for index in range(data_to_read):
                    self._data.append(TrackingDataSpec.read(fileobj))

        elif self.request_type == DataExchangePacket.RequestType.SET_BOUNDS:
            if self.data_type in (DataExchangePacket.DataType.TICK_DATA, DataExchangePacket.DataType.PING_DATA,
                                  DataExchangePacket.DataType.TSLP_DATA):
                self.start_time = Long.read(fileobj)
                self.end_time = Long.read(fileobj)
                self.update_interval = Long.read(fileobj)

            else:
                self.max_data_id = VarInt.read(fileobj)
                self.min_data_id = VarInt.read(fileobj)

    def write(self, fileobj: IO) -> None:
        DataExchangePacket.RequestType.write(self.request_type, fileobj)
        DataExchangePacket.DataType.write(self.data_type, fileobj)
        Integer.write(self.request_id, fileobj)

        if self.request_type == DataExchangePacket.RequestType.DOWNLOAD:
            if self.data_type in (DataExchangePacket.DataType.TICK_DATA, DataExchangePacket.DataType.PING_DATA,
                                  DataExchangePacket.DataType.TSLP_DATA):
                Long.write(self.start_time, fileobj)
                Long.write(self.end_time, fileobj)

            else:
                UnsignedShort.write(len(self._data_ids), fileobj)
                for data_id in self._data_ids:
                    VarInt.write(data_id, fileobj)

        elif self.request_type == DataExchangePacket.RequestType.UPLOAD:
            UnsignedShort.write(len(self._invalid_data_ids), fileobj)
            for data_id in self._invalid_data_ids:
                VarInt.write(data_id, fileobj)

            UnsignedShort.write(len(self._data), fileobj)
            if self.data_type in (DataExchangePacket.DataType.TICK_DATA, DataExchangePacket.DataType.PING_DATA,
                                  DataExchangePacket.DataType.TSLP_DATA):
                for data in self._data:
                    Float.write(data, fileobj)

                Long.write(self.start_time, fileobj)
                Long.write(self.end_time, fileobj)
                UnsignedShort.write(self.update_interval, fileobj)

            elif self.data_type == DataExchangePacket.DataType.ONLINE_PLAYER:  # TODO: Online player data
                ...

            elif self.data_type == DataExchangePacket.DataType.LOGS:
                for data in self._data:
                    String.write(data, fileobj)

            elif self.data_type == DataExchangePacket.DataType.CHAT:
                for data in self._data:
                    ChatMessageSpec.write(data, fileobj)

            elif self.data_type == DataExchangePacket.DataType.CHUNK_STATE:
                for data in self._data:
                    ChunkStateSpec.write(data, fileobj)

            elif self.data_type == DataExchangePacket.DataType.RENDER_DISTANCE:
                for data in self._data:
                    RenderDistanceSpec.write(data, fileobj)

            elif self.data_type == DataExchangePacket.DataType.TRACKED_PLAYER:
                for data in self._data:
                    TrackedPlayerSpec.write(data, fileobj)

            elif self.data_type == DataExchangePacket.DataType.TRACKING_DATA:
                for data in self._data:
                    TrackingDataSpec.write(data, fileobj)

        elif self.request_type == DataExchangePacket.RequestType.SET_BOUNDS:
            for data_id in self._data_ids:
                VarInt.write(data_id, fileobj)

    def get_data(self) -> List[object]:
        return self._data.copy()

    def add_data(self, data: object) -> None:
        self._data.append(data)

    def set_data(self, data: List[object]) -> None:
        self._data.clear()
        self._data.extend(data)

    def extend_data(self, data: List[object]) -> None:
        self._data.extend(data)

    def remove_data(self, data: object) -> None:
        self._data.remove(data)

    def get_invalid_data_ids(self) -> List[int]:
        return self._invalid_data_ids.copy()

    def add_invalid_data_id(self, invalid_data_id: int) -> None:
        self._invalid_data_ids.append(invalid_data_id)

    def set_invalid_data_ids(self, invalid_data_ids: List[int]) -> None:
        self._invalid_data_ids.clear()
        self._invalid_data_ids.extend(invalid_data_ids)

    def extend_invalid_data_ids(self, invalid_data_ids: List[int]) -> None:
        self._invalid_data_ids.extend(invalid_data_ids)

    def remove_invalid_data_id(self, invalid_data_id: int) -> None:
        self._invalid_data_ids.remove(invalid_data_id)

    def get_data_ids(self) -> List[int]:
        return self._data_ids.copy()

    def add_data_id(self, data_id: int) -> None:
        self._data_ids.append(data_id)

    def set_data_ids(self, data_ids: List[int]) -> None:
        self._data_ids.clear()
        self._data_ids.extend(data_ids)

    def extend_data_ids(self, data_ids: List[int]) -> None:
        self._data_ids.extend(data_ids)

    def remove_data_id(self, data_id: int) -> None:
        self._data_ids.remove(data_id)

    class RequestType(Enum):
        DOWNLOAD = 0
        UPLOAD = 1

        GET_BOUNDS = 2
        SET_BOUNDS = 3

    class DataType(Enum):
        TICK_DATA = 0
        PING_DATA = 1
        TSLP_DATA = 2

        ONLINE_PLAYER = 3

        LOGS = 4
        CHAT = 5

        CHUNK_STATE = 6
        RENDER_DISTANCE = 7
        TRACKED_PLAYER = 8
        TRACKING_DATA = 9


class ConfigActionPacket(Packet):
    """
    Sent by either the server or client to indicate an action being performed on a config rule.
    """

    ID = ID_OFFSET + 4
    NAME = "config_action"
    SIDE = Side.BOTH

    def __init__(self, action=0, action_id: int = -1, rule: ConfigRule = None, value: object = None,
                 rule_name: str = "") -> None:
        super().__init__()

        self.action = action
        self.action_id = action_id

        self.rule = rule
        self.value = value

        self.rule_name = rule_name

    def read(self, fileobj: IO) -> None:
        self.action = ConfigActionPacket.Action.read(fileobj)
        self.action_id = Long.read(fileobj)

        if self.action in (ConfigActionPacket.Action.SET_RULE, ConfigActionPacket.Action.SYNC_RULE):
            self.rule = ConfigRuleSpec.read(fileobj)
            self.value = DataType.value_to_serializable(self.rule.data_type).read(fileobj)

        elif self.action == ConfigActionPacket.Action.GET_RULE:
            self.rule_name = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        ConfigActionPacket.Action.write(self.action, fileobj)
        Long.write(self.action_id, fileobj)

        if self.action in (ConfigActionPacket.Action.SET_RULE, ConfigActionPacket.Action.SYNC_RULE):
            ConfigRuleSpec.write(self.rule, fileobj)
            DataType.value_to_serializable(self.rule.data_type).write(self.value, fileobj)

        elif self.action == ConfigActionPacket.Action.GET_RULE:
            String.write(self.rule_name, fileobj)

    class Action(Enum):
        SET_RULE = 0
        GET_RULE = 1

        SYNC_RULE = 2


class TaskActionPacket(Packet):
    """
    Sent by the client to request a task is started or stopped, and sent by the server to notify the client about a task
    being added, removed or updated.
    """

    ID = ID_OFFSET + 5
    NAME = "task_action"
    SIDE = Side.BOTH

    def __init__(self, action=0, action_id: int = -1, task_name: str = "", task_id: int = 0,
                 time_elapsed: int = 0, has_progress: bool = False, progress: float = 0,
                 has_current_position: bool = False, current_position: ChunkPosition = ChunkPosition(0, 0),
                 result: str = "") -> None:
        super().__init__()

        self.action = action
        self.action_id = action_id
        self.task_name = task_name
        self._task_params = []
        self.task_id = task_id

        self.time_elapsed = time_elapsed

        self.has_progress = has_progress
        self.progress = progress

        self.has_current_position = has_current_position
        self.current_position = current_position

        self.result = result

    def read(self, fileobj: IO) -> None:
        self._task_params.clear()

        self.action = TaskActionPacket.Action.read(fileobj)
        self.action_id = Long.read(fileobj)

        if self.action == TaskActionPacket.Action.START:
            self.task_name = String.read(fileobj)

            params_to_read = UnsignedShort.read(fileobj)
            for index in range(params_to_read):
                self._task_params.append(ParameterSpec.read(fileobj))

        else:
            self.task_id = UnsignedShort.read(fileobj)

            if self.action == TaskActionPacket.Action.ADD:
                self.task_name = String.read(fileobj)

                params_to_read = UnsignedShort.read(fileobj)
                for index in range(params_to_read):
                    self._task_params.append(ParameterSpec.read(fileobj))

            elif self.action == TaskActionPacket.Action.UPDATE:
                self.time_elapsed = Integer.read(fileobj)

                self.has_progress = Boolean.read(fileobj)
                if self.has_progress:
                    self.progress = Float.read(fileobj)

                self.has_current_position = Boolean.read(fileobj)
                if self.has_current_position:
                    self.current_position = ChunkPositionSpec.read(fileobj)

            elif self.action == TaskActionPacket.Action.RESULT:
                self.result = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        TaskActionPacket.Action.write(self.action, fileobj)
        Long.write(self.action_id, fileobj)

        if self.action == TaskActionPacket.Action.START:
            String.write(self.task_name, fileobj)

            UnsignedShort.write(len(self._task_params), fileobj)
            for parameter in self._task_params:
                ParameterSpec.write(parameter, fileobj)

        else:
            UnsignedShort.write(self.task_id, fileobj)

            if self.action == TaskActionPacket.Action.ADD:
                String.write(self.task_name, fileobj)

                UnsignedShort.write(len(self._task_params), fileobj)
                for parameter in self._task_params:
                    ParameterSpec.write(parameter, fileobj)

            elif self.action == TaskActionPacket.Action.UPDATE:
                Integer.write(self.time_elapsed, fileobj)

                Boolean.write(self.has_progress, fileobj)  # TODO: Maybe shrink these booleans into 1 byte?
                if self.has_progress:
                    Float.write(self.progress, fileobj)

                Boolean.write(self.has_current_position, fileobj)
                if self.has_current_position:
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
    """
    Sent by the client to request an account is added or removed.
    """

    ID = ID_OFFSET + 6
    NAME = "account_action"
    SIDE = Side.BOTH

    def __init__(self, action=0, action_id: int = -1, username: str = "", legacy: bool = True, password: str = "",
                 access_token: str = "", client_token: str = "") -> None:
        super().__init__()

        self.action = action
        self.action_id = action_id
        self.username = username

        self.legacy = legacy

        self.password = password

        self.access_token = access_token
        self.client_token = client_token

    def read(self, fileobj: IO) -> None:
        self.action = AccountActionPacket.Action.read(fileobj)
        self.action_id = Long.read(fileobj)

        self.username = String.read(fileobj)

        if self.action == AccountActionPacket.Action.LOGIN:
            self.legacy = Boolean.read(fileobj)

            if self.legacy:
                self.password = String.read(fileobj)
            else:
                self.access_token = String.read(fileobj)
                self.client_token = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        AccountActionPacket.Action.write(self.action, fileobj)
        Long.write(self.action_id, fileobj)

        String.write(self.username, fileobj)

        if self.action == AccountActionPacket.Action.LOGIN:
            Boolean.write(self.legacy, fileobj)

            if self.legacy:
                String.write(self.password, fileobj)
            else:
                String.write(self.access_token, fileobj)
                String.write(self.client_token, fileobj)

    class Action(Enum):
        LOGIN = 0
        LOGOUT = 1


class PlayerActionPacket(Packet):
    """
    Sent by the server to add, remove or update the state of a player.
    """

    ID = ID_OFFSET + 7
    NAME = "player_action"
    SIDE = Side.BOTH

    def __init__(self, action=0, player_name: str = "", uuid: UUID = None, display_name: str = "",
                 disconnect_reason: str = "", can_login: bool = True, new_position: Position = Position(0, 0, 0),
                 new_angle: Angle = Angle(0, 0), new_dimension: int = 0, new_health: float = 20, new_hunger: int = 20,
                 new_saturation: float = 20) -> None:
        super().__init__()

        self.action = action

        self.player_name = player_name
        self.uuid = uuid
        self.display_name = display_name

        self.disconnect_reason = disconnect_reason

        self.can_login = can_login

        self.new_position = new_position
        self.new_angle = new_angle

        self.new_dimension = new_dimension

        self.new_health = new_health
        self.new_hunger = new_hunger
        self.new_saturation = new_saturation

    def read(self, fileobj: IO) -> None:
        self.action = PlayerActionPacket.Action.read(fileobj)
        self.player_name = String.read(fileobj)

        if self.action == PlayerActionPacket.Action.ADD:
            self.uuid = UUID(bytes=Bytes.read(fileobj))
            self.display_name = String.read(fileobj)

        elif self.action == PlayerActionPacket.Action.REMOVE:
            ...

        elif self.action == PlayerActionPacket.Action.LOGIN:
            ...

        elif self.action == PlayerActionPacket.Action.LOGOUT:
            self.disconnect_reason = String.read(fileobj)

        elif self.action == PlayerActionPacket.Action.TOGGLE_LOGIN:
            self.can_login = Boolean.read(fileobj)

        elif self.action == PlayerActionPacket.Action.UPDATE_POSITION:
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
        String.write(self.player_name, fileobj)

        if self.action == PlayerActionPacket.Action.ADD:
            Bytes.write(self.uuid.bytes, fileobj)
            String.write(self.display_name, fileobj)

        elif self.action == PlayerActionPacket.Action.REMOVE:
            ...

        elif self.action == PlayerActionPacket.Action.LOGIN:
            ...

        elif self.action == PlayerActionPacket.Action.LOGOUT:
            String.write(self.disconnect_reason, fileobj)

        elif self.action == PlayerActionPacket.Action.TOGGLE_LOGIN:
            Boolean.write(self.can_login, fileobj)

        elif self.action == PlayerActionPacket.Action.UPDATE_POSITION:
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
        LOGIN = 2
        LOGOUT = 3
        TOGGLE_LOGIN = 4
        UPDATE_POSITION = 5
        UPDATE_DIMENSION = 6
        UPDATE_HEALTH = 7


class TrackerActionPacket(Packet):
    """
    Sent by the server to inform if a tracker has been added, removed or updated.
    """

    ID = ID_OFFSET + 8
    NAME = "tracker_action"
    SIDE = Side.BOTH

    def __init__(self, action=0, action_id: int = -1, tracker: Tracker = None, tracker_id: int = 0) -> None:
        super().__init__()

        self.action = action
        self.action_id = action_id
        self.tracker = tracker
        self.tracker_id = tracker_id
        self._tracked_player_ids = []

    def read(self, fileobj: IO) -> None:
        self.action = TrackerActionPacket.Action.read(fileobj)
        self.action_id = Long.read(fileobj)

        if self.action == TrackerActionPacket.Action.ADD:
            self.tracker = TrackerSpec.read(fileobj)
        else:
            self.tracker_id = Long.read(fileobj)

            if self.action == TrackerActionPacket.Action.UPDATE:
                self._tracked_player_ids.clear()

                tracked_player_ids_to_read = UnsignedShort.read(fileobj)
                for index in range(tracked_player_ids_to_read):
                    self._tracked_player_ids.append(VarInt.read(fileobj))

    def write(self, fileobj: IO) -> None:
        TrackerActionPacket.Action.write(self.action, fileobj)
        Long.write(self.action_id, fileobj)

        if self.action == TrackerActionPacket.Action.ADD:
            TrackerSpec.write(self.tracker, fileobj)
        else:
            Long.write(self.tracker_id, fileobj)

            if self.action == TrackerActionPacket.Action.UPDATE:
                UnsignedShort.write(len(self._tracked_player_ids), fileobj)
                for tracked_player_id in self._tracked_player_ids:
                    VarInt.write(tracked_player_id, fileobj)

    def get_tracked_player_ids(self) -> List[int]:
        return self._tracked_player_ids.copy()

    def add_tracked_player_id(self, tracked_player_id: int) -> None:
        self._tracked_player_ids.append(tracked_player_id)

    def set_tracked_player_ids(self, tracked_player_ids: List[int]) -> None:
        self._tracked_player_ids.clear()
        self._tracked_player_ids.extend(tracked_player_ids)

    def extend_tracked_player_ids(self, tracked_player_ids: List[int]) -> None:
        self._tracked_player_ids.extend(tracked_player_ids)

    def remove_tracked_player_id(self, tracked_player_id: int) -> None:
        self._tracked_player_ids.append(tracked_player_id)

    class Action(Enum):
        ADD = 0
        REMOVE = 1
        UPDATE = 2
        UNTRACK = 3


class InfoUpdatePacket(Packet):
    """
    Sent every 250ms to update information about the state of the current reporter, as well as information about the
    state of the Minecraft server that the reporter is connected to (if it is connected, which is also specified by the
    packet).
    """

    ID = ID_OFFSET + 9
    NAME = "info_update"
    SIDE = Side.BOTH

    def __init__(self, waiting_queries: int = 0, ticking_queries: int = 0, query_rate: float = 0,
                 dropped_queries: float = 0, connected: bool = False, tick_rate: float = 0, server_ping: float = 0,
                 time_since_last_packet: int = 0) -> None:
        super().__init__()

        self.waiting_queries = waiting_queries
        self.ticking_queries = ticking_queries
        self.query_rate = query_rate
        self.dropped_queries = dropped_queries
        self.connected = connected
        self.tick_rate = tick_rate
        self.server_ping = server_ping
        self.time_since_last_packet = time_since_last_packet

    def read(self, fileobj: IO) -> None:
        self.waiting_queries = UnsignedShort.read(fileobj)
        self.ticking_queries = UnsignedShort.read(fileobj)
        self.query_rate = Float.read(fileobj)
        self.dropped_queries = Float.read(fileobj)
        self.connected = Boolean.read(fileobj)

        if self.connected:
            self.tick_rate = Float.read(fileobj)
            self.server_ping = Float.read(fileobj)
            self.time_since_last_packet = UnsignedShort.read(fileobj)

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(self.waiting_queries, fileobj)
        UnsignedShort.write(self.ticking_queries, fileobj)
        Float.write(self.query_rate, fileobj)
        Float.write(self.dropped_queries, fileobj)
        Boolean.write(self.connected, fileobj)

        if self.connected:
            Float.write(self.tick_rate, fileobj)
            Float.write(self.server_ping, fileobj)
            UnsignedShort.write(self.time_since_last_packet, fileobj)


class OnlinePlayersActionPacket(Packet):

    ID = ID_OFFSET + 10
    NAME = "online_players_action"
    SIDE = Side.BOTH

    def __init__(self, action=0) -> None:
        super().__init__()

        self.action = action
        self._online_players = {}

    def read(self, fileobj: IO) -> None:
        self._online_players.clear()

        self.action = OnlinePlayersActionPacket.Action.read(fileobj)

        online_players_to_read = UnsignedShort.read(fileobj)
        for index in range(online_players_to_read):
            uuid = UUID(bytes=Bytes.read(fileobj))
            if self.action == OnlinePlayersActionPacket.Action.ADD:
                name = String.read(fileobj)
            else:
                name = ""
            self._online_players[uuid] = name

    def write(self, fileobj: IO) -> None:
        OnlinePlayersActionPacket.Action.write(self.action, fileobj)

        UnsignedShort.write(len(self._online_players), fileobj)
        for uuid in self._online_players:
            Bytes.write(uuid.bytes, fileobj)
            if self.action == OnlinePlayersActionPacket.Action.ADD:
                String.write(self._online_players[uuid], fileobj)

    def get_online_players(self) -> Dict[UUID, str]:
        return self._online_players.copy()

    def get_online_player(self, uuid: UUID) -> str:
        return self._online_players[uuid]

    def put_online_player(self, uuid: UUID, display_name: str) -> None:
        self._online_players[uuid] = display_name

    def set_online_players(self, online_players: Dict[UUID, str]) -> None:
        self._online_players.clear()
        self._online_players.update(online_players)

    def put_online_players(self, online_players: Dict[UUID, str]) -> None:
        self._online_players.update(online_players)

    def remove_online_player(self, uuid: UUID) -> None:
        del self._online_players[uuid]

    class Action(Enum):
        ADD = 0
        REMOVE = 1


class ActionRequestPacket(Packet):
    """
    This is a general action request packet, it is used to perform actions that require less data, such as sending a
    chat message.
    """

    ID = ID_OFFSET + 11
    NAME = "action_request"
    SIDE = Side.BOTH

    def __init__(self, action=0, action_id: int = -1, data: bytes = b"") -> None:
        super().__init__()

        self.action = action
        self.action_id = action_id
        self.data = data

    def read(self, fileobj: IO) -> None:
        self.action = ActionRequestPacket.Action.read(fileobj)
        self.action_id = Long.read(fileobj)
        self.data = Bytes.read(fileobj)

    def write(self, fileobj: IO) -> None:
        ActionRequestPacket.Action.write(self.action, fileobj)
        Long.write(self.action_id, fileobj)
        Bytes.write(self.data, fileobj)

    class Action(Enum):
        TOGGLE_LOGIN = 0
        SEND_CHAT_MESSAGE = 1
        UNTRACK_PLAYER = 2


class ActionResponsePacket(Packet):
    """
    Sent by the server in response to an action, to indicate whether or not it was successful, as well as providing
    an additional message, could be an error or other.
    """

    ID = ID_OFFSET + 12
    NAME = "action_response"
    SIDE = Side.BOTH

    def __init__(self, action_id: int = -1, successful: bool = True, message: str = "") -> None:
        super().__init__()

        self.action_id = action_id
        self.successful = successful
        self.message = message

    def read(self, fileobj: IO) -> None:
        self.action_id = Long.read(fileobj)
        self.successful = Boolean.read(fileobj)
        self.message = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Long.write(self.action_id, fileobj)
        Boolean.write(self.successful, fileobj)
        String.write(self.message, fileobj)


class ReporterActionPacket(Packet):
    """
    Sent by the server to inform the client if a reporter is added or removed, as well as by the client to select the
    current reporter, or deselect the reporter.
    """

    ID = ID_OFFSET + 13
    NAME = "reporter_action"
    SIDE = Side.BOTH

    def __init__(self, action=0, reporter_id: int = 0, reporter_name: str = "", reporter_host: str = "",
                 reporter_port: int = 0) -> None:
        super().__init__()

        self.action = action
        self.reporter_id = reporter_id
        self.reporter_name = reporter_name
        self.reporter_host = reporter_host
        self.reporter_port = reporter_port

    def read(self, fileobj: IO) -> None:
        self.action = ReporterActionPacket.Action.read(fileobj)
        self.reporter_id = Short.read(fileobj)

        if self.action == ReporterActionPacket.Action.ADD:
            self.reporter_name = String.read(fileobj)
            self.reporter_host = String.read(fileobj)
            self.reporter_port = UnsignedShort.read(fileobj)

    def write(self, fileobj: IO) -> None:
        ReporterActionPacket.Action.write(self.action, fileobj)
        Short.write(self.reporter_id, fileobj)

        if self.action == ReporterActionPacket.Action.ADD:
            String.write(self.reporter_name, fileobj)
            String.write(self.reporter_host, fileobj)
            UnsignedShort.write(self.reporter_port, fileobj)

    class Action(Enum):
        ADD = 0
        REMOVE = 1
        SELECT = 2


class ReporterSyncPacket(Packet):
    """
    Sent by the server to synchronize a selected reporter with the client.
    """

    ID = ID_OFFSET + 14
    NAME = "reporter_sync"
    SIDE = Side.SERVER

    def __init__(self, has_reporter: bool = False, reporter_id: int = 0, reporter_name: str = "") -> None:
        super().__init__()

        self.has_reporter = has_reporter
        self.reporter_id = reporter_id
        self.reporter_name = reporter_name

        self._config_rules = {}
        self._registered_tasks = []
        self._active_tasks = []
        self._players = []
        self._trackers = []

    def read(self, fileobj: IO) -> None:
        self.has_reporter = Boolean.read(fileobj)

        if self.has_reporter:
            self.reporter_id = Short.read(fileobj)
            self.reporter_name = String.read(fileobj)

            self._config_rules.clear()
            self._registered_tasks.clear()
            self._active_tasks.clear()
            self._players.clear()
            self._trackers.clear()

            config_rules_to_read = UnsignedShort.read(fileobj)
            for index in range(config_rules_to_read):
                config_rule = ConfigRuleSpec.read(fileobj)
                value = DataType.value_to_serializable(config_rule.data_type).read(fileobj)

                self._config_rules[config_rule] = value

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

                time_elapsed = Integer.read(fileobj)

                has_progress = Boolean.read(fileobj)
                progress = 0
                if has_progress:
                    progress = Float.read(fileobj)

                has_current_position = Boolean.read(fileobj)
                current_position = ChunkPosition(0, 0)
                if has_current_position:
                    current_position = ChunkPositionSpec.read(fileobj)

                results = []

                results_to_read = UnsignedShort.read(fileobj)
                for r_index in range(results_to_read):
                    results.append(String.read(fileobj))

                for registered_task in self._registered_tasks:
                    if registered_task.name == task_name:
                        self._active_tasks.append(ActiveTask(registered_task, task_id, parameters, time_elapsed,
                                                             has_progress, progress, has_current_position,
                                                             current_position, results))
                        break

            players_to_read = UnsignedShort.read(fileobj)
            for index in range(players_to_read):
                self._players.append(PlayerSpec.read(fileobj))

            trackers_to_read = Integer.read(fileobj)
            for index in range(trackers_to_read):
                self._trackers.append(TrackerSpec.read(fileobj))

    def write(self, fileobj: IO) -> None:
        Boolean.write(self.has_reporter, fileobj)

        if self.has_reporter:
            Short.write(self.reporter_id, fileobj)
            String.write(self.reporter_name, fileobj)

            UnsignedShort.write(len(self._config_rules), fileobj)
            for config_rule, value in self._config_rules.values():
                ConfigRuleSpec.write(config_rule, fileobj)
                DataType.value_to_serializable(config_rule.data_type).write(value, fileobj)

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

                Integer.write(active_task.time_elapsed, fileobj)

                Boolean.write(active_task.has_progress, fileobj)
                if active_task.has_progress:
                    Float.write(active_task.progress, fileobj)

                Boolean.write(active_task.has_current_position, fileobj)
                if active_task.has_current_position:
                    ChunkPositionSpec.write(active_task.current_position, fileobj)

                UnsignedShort.write(len(active_task.results), fileobj)
                for result in active_task.results:
                    String.write(result, fileobj)

            UnsignedShort.write(len(self._players), fileobj)
            for player in self._players:
                PlayerSpec.write(player, fileobj)

            Integer.write(len(self._trackers), fileobj)
            for tracker in self._trackers:
                TrackerSpec.write(tracker, fileobj)

    def get_rules(self) -> Dict[ConfigRule, object]:
        return self._config_rules.copy()

    def put_rule(self, rule: ConfigRule, value: object) -> None:
        self._config_rules[rule] = value

    def set_rules(self, rules: Dict[ConfigRule, object]) -> None:
        self._config_rules.clear()
        self._config_rules.update(rules)

    def put_rules(self, rules: Dict[ConfigRule, object]) -> None:
        self._config_rules.update(rules)

    def remove_rule(self, rule: ConfigRule) -> None:
        del self._config_rules[rule]

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


packets = (
    YCInitRequestPacket,
    YCInitResponsePacket,
    YCExtendedResponsePacket,
    DataExchangePacket,
    ConfigActionPacket,
    TaskActionPacket,
    PlayerActionPacket,
    AccountActionPacket,
    TrackerActionPacket,
    InfoUpdatePacket,
    OnlinePlayersActionPacket,
    ActionRequestPacket,
    ActionResponsePacket,

    ReporterActionPacket,
    ReporterSyncPacket,
)
