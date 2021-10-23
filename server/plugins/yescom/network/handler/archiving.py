#!/usr/bin/env python3
from typing import Union, List

from network.networking.handlers import Handler
from network.networking.packets import Packet
from network.networking.types import Enum
from plugins.yescom.network.packets import UpdateDataIDsPacket


class Archiver(Handler):

    @property
    def can_upload(self) -> bool:
        return self._can_upload

    @property
    def can_download(self) -> bool:
        return self._can_download

    def __init__(self, system, connection, yescom, handler_id: int, handler_name: str, can_upload: bool = True,
                 can_download: bool = True) -> None:
        super().__init__(system, connection)

        self.yescom = yescom

        self._handler_id = handler_id
        self._handler_name = handler_name

        self._can_upload = can_upload
        self._can_download = can_download

        self._current_requests = {}

        self._data_ids = {
            Archiver.DataType.CHUNK_STATE: [0, 0],
            Archiver.DataType.RENDER_DISTANCE: [0, 0],
            Archiver.DataType.TRACKED_PLAYER: [0, 0],
            Archiver.DataType.ONLINE_PLAYER: [0, 0],
        }

    def on_packet(self, packet: Packet) -> None:
        if isinstance(packet, UpdateDataIDsPacket):
            self._data_ids[packet.data_type] = [packet.data_id_min, packet.data_id_max]

    # -------------------- Data IDs -------------------- #

    def has_data_id(self, data_type, data_id: int) -> bool:
        return self._data_ids[data_type][0] < data_id <= self._data_ids[data_type][1]

    # -------------------- Requests -------------------- #

    def start_request(self, requester, data_ids: List[int]) -> None:
        self._current_requests[requester] = data_ids

    def cancel_request(self, requester) -> None:
        if requester in self._current_requests:
            del self._current_requests[requester]

    class DataType(Enum):
        CHUNK_STATE = 0
        RENDER_DISTANCE = 1
        TRACKED_PLAYER = 2
        ONLINE_PLAYER = 3
