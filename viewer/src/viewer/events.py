#!/usr/bin/env python3

from enum import Enum  # Actual enum this time
from typing import List
from uuid import UUID

from viewer.network.packets import DataExchangePacket
from viewer.reporter import Reporter
from viewer.util import Player, ActiveTask, Tracker


class Event:
    ...


class ConnectEvent(Event):
    ...


class DisconnectEvent(Event):
    ...


class DataEvent(Event):

    @property
    def data_type(self) -> DataExchangePacket.DataType:
        """
        :return: The data type being sent.
        """

        return self._data_type

    @property
    def data(self) -> List[object]:
        """
        :return: The data that was sent.
        """

        return self._data.copy()

    @property
    def invalid_data_ids(self) -> List[int]:
        """
        :return: The IDs of the data that was invalid.
        """

        return self._invalid_data_ids.copy()

    @property
    def start_time(self) -> int:
        """
        :return: The start time of numeric data.
        """

        return self._start_time

    @property
    def end_time(self) -> int:
        """
        :return: The end time of numeric data.
        """

        return self._end_time

    @property
    def update_interval(self) -> int:
        return self._update_interval

    def __init__(self, data_type: DataExchangePacket.DataType, data: List[object], invalid_data_ids: List[int],
                 start_time: int, end_time: int, update_interval: int) -> None:
        self._data_type = data_type
        self._data = data
        self._invalid_data_ids = invalid_data_ids
        self._start_time = start_time
        self._end_time = end_time
        self._update_interval = update_interval

    def __repr__(self) -> str:
        return "DataEvent(data_type=%s)" % DataExchangePacket.DataType.name_from_value(self._data_type)


class DataBoundsEvent(Event):

    @property
    def data_type(self) -> DataExchangePacket.DataType:
        """
        :return: The type of data the bounds were reported for.
        """

        return self._data_type

    @property
    def start_time(self) -> int:
        """
        :return: The start time of numeric data.
        """

        return self._start_time

    @property
    def end_time(self) -> int:
        """
        :return: The end time of the numeric data.
        """

        return self._end_time

    @property
    def update_interval(self) -> int:
        """
        :return: The update interval for the numeric data.
        """

        return self._update_interval

    @property
    def min_data_id(self) -> int:
        """
        :return: The minimum data ID.
        """

        return self._min_data_id

    @property
    def max_data_id(self) -> int:
        """
        :return: The maximum data ID.
        """

        return self._max_data_id

    def __init__(self, data_type: DataExchangePacket.DataType, start_time: int, end_time: int, update_interval: int,
                 min_data_id: int, max_data_id: int) -> None:
        self._data_type = data_type
        self._start_time = start_time
        self._end_time = end_time
        self._update_interval = update_interval
        self._min_data_id = min_data_id
        self._max_data_id = max_data_id

    def __repr__(self) -> str:
        return "DataBoundsEvent(data_type=%s)" % DataExchangePacket.DataType.name_from_value(self._data_type)


class ReporterEvent(Event):

    @property
    def event_type(self):  # -> EventType
        """
        :return: The type of reporter event.
        """

        return self._event_type

    @property
    def reporter(self) -> Reporter:
        """
        :return: The reporter that was reported.
        """

        return self._reporter

    def __init__(self, event_type, reporter: Reporter) -> None:
        self._event_type = event_type
        self._reporter = reporter

    def __repr__(self) -> str:
        return "ReporterEvent(type=%s, reporter=%r)" % (self._event_type.name, self._reporter)

    class EventType(Enum):
        ADDED = 0
        REMOVED = 1
        SELECTED = 2


class PlayerEvent(Event):

    @property
    def event_type(self):  # -> EventType
        return self._event_type

    @property
    def player(self) -> Player:
        return self._player

    @property
    def reason(self) -> str:
        return self._reason

    def __init__(self, event_type, player: Player, reason: str = "") -> None:
        self._event_type = event_type
        self._player = player
        self._reason = reason

    def __repr__(self) -> str:
        return "PlayerEvent(type=%s, player=%r)" % (self._event_type.name, self._player)

    class EventType(Enum):
        ADDED = 0
        REMOVED = 1
        LOGIN = 2
        LOGOUT = 3
        UPDATED = 4


class TaskEvent(Event):

    @property
    def event_type(self):  # -> EventType
        return self._event_type

    @property
    def active_task(self) -> ActiveTask:
        return self._active_task

    @property
    def result(self) -> str:
        return self._result

    def __init__(self, event_type, active_task: ActiveTask, result: str = "") -> None:
        self._event_type = event_type
        self._active_task = active_task
        self._result = result

    def __repr__(self) -> str:
        return "TaskEvent(type=%s, active_task=%r)" % (self._event_type.name, self._active_task)

    class EventType(Enum):
        ADDED = 0
        REMOVED = 1
        UPDATED = 2
        RESULT = 3


class TrackerEvent(Event):

    @property
    def event_type(self):  # -> EventType:
        return self._event_type

    @property
    def tracker(self) -> Tracker:
        return self._tracker

    def __init__(self, event_type, tracker: Tracker) -> None:
        self._event_type = event_type
        self._tracker = tracker

    def __repr__(self) -> str:
        return "TrackerEvent(event_type=%s, tracker=%r)" % (self._event_type, self._tracker)

    class EventType(Enum):
        ADDED = 0
        REMOVED = 1
        UPDATED = 2


class OnlinePlayerEvent(Event):

    @property
    def event_type(self):  # -> EventType:
        return self._event_type

    @property
    def uuid(self) -> UUID:
        return self._uuid

    @property
    def name(self) -> str:
        return self._name

    def __init__(self, event_type, uuid: UUID, name: str) -> None:
        self._event_type = event_type
        self._uuid = uuid
        self._name = name

    def __repr__(self) -> str:
        return "OnlinePlayerEvent(type=%s, uuid=%r, name=%r)" % (self._event_type.name, self._uuid, self._name)

    class EventType(Enum):
        ADDED = 0
        REMOVED = 1
