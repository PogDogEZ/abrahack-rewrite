#!/usr/bin/env python3

from typing import IO, List

from network.networking.packets import Packet, Side
from network.networking.types import Enum
from network.networking.types.basic import Boolean, String, UnsignedShort, Bytes, VarInt, Integer

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

        self.valid = False

        self.chunk_size = 65536
        self.expected_parts = 0

        self._invalid_data_ids = []

    def read(self, fileobj: IO) -> None:
        self.valid = Boolean.read(fileobj)

        if self.valid:
            self.chunk_size = Integer.read(fileobj)
            self.expected_parts = UnsignedShort.read(fileobj)
        else:
            self._invalid_data_ids.clear()

            invalid_data_ids_to_read = UnsignedShort.read(fileobj)
            for index in range(invalid_data_ids_to_read):
                self._invalid_data_ids.append(VarInt.read(fileobj))

    def write(self, fileobj: IO) -> None:
        Boolean.write(self.valid, fileobj)

        if self.valid:
            Integer.write(self.chunk_size, fileobj)
            UnsignedShort.write(self.expected_parts, fileobj)
        else:
            UnsignedShort.write(len(self._invalid_data_ids), fileobj)
            for invalid_data_id in self._invalid_data_ids:
                VarInt.write(invalid_data_id, fileobj)

    def get_invalid_data_ids(self) -> List[int]:
        return self._invalid_data_ids.copy()

    def add_invalid_data_id(self, invalid_data_id: int) -> None:
        self._invalid_data_ids.append(invalid_data_id)

    def set_invalid_data_ids(self, invalid_data_ids: List[int]) -> None:
        self._invalid_data_ids.clear()
        self._invalid_data_ids.extend(invalid_data_ids)

    def extend_invalid_data_ids(self, invalid_data_ids: List[int]) -> None:
        self._invalid_data_ids.extend(invalid_data_ids)

    def remove_invalid_data_ids(self, invalid_data_id: int) -> None:
        self._invalid_data_ids.remove(invalid_data_id)


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
