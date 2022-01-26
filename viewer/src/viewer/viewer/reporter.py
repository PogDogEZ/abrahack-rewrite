#!/usr/bin/env python3

from .util import *


class Reporter:
    """
    A clientside representation of a reporter. A reporter is the client responsible for providing the viewer with data.
    """

    @property
    def handler_id(self) -> int:
        """
        :return: The serverside ID of the reporter.
        """

        return self._id

    @property
    def handler_name(self) -> str:
        """
        :return: The serverside name of the reporter.
        """

        return self._name

    @property
    def host(self) -> str:
        return self._host

    @property
    def port(self) -> int:
        return self._port

    @property
    def registered_tasks(self) -> List[RegisteredTask]:
        """
        :return: The registered tasks that the reporter supports.
        """

        return self._registered_tasks.copy()

    @property
    def active_tasks(self) -> List[ActiveTask]:
        """
        :return: The active tasks that the reporter is currently running.
        """

        return self._active_tasks.copy()

    @property
    def players(self) -> List[Player]:
        return self._players.copy()

    @property
    def trackers(self) -> List[Tracker]:
        return self._trackers.copy()

    @property
    def online_players(self) -> Dict[UUID, str]:
        return self._online_players.copy()

    @property
    def waiting_queries(self) -> int:
        return self._waiting_queries

    @property
    def ticking_queries(self) -> int:
        return self._ticking_queries

    @property
    def query_rate(self) -> float:
        return self._query_rate

    @property
    def dropped_queries(self) -> float:
        return self._dropped_queries

    @property
    def is_connected(self) -> bool:
        """
        :return: Whether or not the reporter is connected to the Minecraft server.
        """

        return self._is_connected

    @property
    def tick_rate(self) -> float:
        """
        :return: The estimated tickrate that the Minecraft server is running at.
        """

        return self._tick_rate

    @property
    def server_ping(self) -> float:
        """
        :return: The ping from the Minecraft server to the client, as measured by the server.
        """

        return self._server_ping

    @property
    def time_since_last_packet(self) -> int:
        return self._time_since_last_packet

    def __init__(self, handler_id: int, handler_name: str, host: str, port: int) -> None:
        self._id = handler_id
        self._name = handler_name
        self._host = host
        self._port = port

        self._config_rules = {}
        self._registered_tasks = []
        self._active_tasks = []
        self._players = []
        self._trackers = []

        self._log_messages = []
        self._chat_messages = []

        self._dim_data = {
            -1: {},
            0: {},
            1: {},
        }

        self._online_players = {}

        self._waiting_queries = 0
        self._ticking_queries = 0
        self._query_rate = 0
        self._dropped_queries = 0

        self._is_connected = False
        self._tick_rate = 20
        self._server_ping = 0
        self._time_since_last_packet = 0

    def __repr__(self) -> str:
        return "Reporter(name=%s, id=%i)" % (self._name, self._id)

    # ------------------------------ Misc ------------------------------ #

    def reset(self) -> None:
        """
        Resets the reporter to its default state.
        """

        self._config_rules.clear()
        self._active_tasks.clear()
        self._players.clear()
        self._trackers.clear()
        self._online_players.clear()

        self._dim_data[-1].clear()
        self._dim_data[0].clear()
        self._dim_data[1].clear()

        self._waiting_queries = 0
        self._ticking_queries = 0
        self._queries_per_second = 0

        self._is_connected = False
        self._tick_rate = 20
        self._time_since_last_packet = 0

    def update_info(self, waiting_queries: int, ticking_queries: int, query_rate: float, dropped_queries: float,
                    is_connected: bool, tick_rate: float = 0, server_ping: float = 0,
                    time_since_last_packet: int = 0) -> None:
        """
        Updates the reporter info.

        :param waiting_queries: The number of queries that are waiting to be processed.
        :param ticking_queries: The number of queries that are currently being processed.
        :param query_rate: The number of queries that are being processed per second.
        :param dropped_queries: The nubmer of queries that are being dropped per second.
        :param is_connected: Whether the reporter is connected to the server.
        :param tick_rate: The tick rate of the server.
        :param server_ping: The server measured ping.
        :param time_since_last_packet: The time since the last packet was received from the server.
        """

        self._waiting_queries = waiting_queries
        self._ticking_queries = ticking_queries
        self._query_rate = query_rate
        self._dropped_queries = dropped_queries
        self._is_connected = is_connected
        self._tick_rate = tick_rate
        self._server_ping = server_ping
        self._time_since_last_packet = time_since_last_packet

    # ------------------------------ Config rules ------------------------------ #

    def put_config_rule(self, config_rule: ConfigRule, value: object) -> None:
        """
        Puts a config rule, and its value, into the reporter.

        :param config_rule: The config rule to put.
        :param value: The value of the config rule.
        """

        self._config_rules[config_rule] = value

    def set_config_rules(self, config_rules: Dict[ConfigRule, object]) -> None:
        self._config_rules.clear()
        self._config_rules.update(config_rules)

    def remove_config_rule(self, config_rule: ConfigRule) -> None:
        del self._config_rules[config_rule]

    def get_config_rules(self) -> Dict[ConfigRule, object]:
        return self._config_rules.copy()

    # ------------------------------ Registered tasks ------------------------------ #

    def add_registered_task(self, registered_task: RegisteredTask) -> None:
        """
        Adds a registered task to the reporter.

        :param registered_task: The registered task to add.
        """

        if not registered_task in self._registered_tasks:
            self._registered_tasks.append(registered_task)

    def set_registered_tasks(self, registered_tasks: List[RegisteredTask]) -> None:
        """
        Sets the registered tasks of the reporter.

        :param registered_tasks: The registered tasks to set.
        """

        self._registered_tasks.clear()
        self._registered_tasks.extend(registered_tasks)

    def remove_registered_task(self, registered_task: RegisteredTask) -> None:
        """
        Removes a registered task from the reporter.

        :param registered_task: The registered task to remove.
        """

        if registered_task in self._registered_tasks:
            self._registered_tasks.remove(registered_task)

    def get_registered_task(self, name: str) -> RegisteredTask:
        """
        Gets a registered task by its name, throws a LookupError if the task is not found.

        :param name: The name of the registered task.
        :return: The registered task.
        """

        for registered_task in self._registered_tasks:
            if registered_task.name == name:
                return registered_task

        raise LookupError("Couldn't find registered task by name %r." % name)

    def get_registered_tasks(self) -> List[RegisteredTask]:
        return self._registered_tasks.copy()

    # ------------------------------ Active tasks ------------------------------ #

    def add_active_task(self, active_task: ActiveTask) -> None:
        if not active_task in self._active_tasks:
            self._active_tasks.append(active_task)

    def set_active_tasks(self, active_tasks: List[ActiveTask]) -> None:
        """
        Sets the active tasks of the reporter.

        :param active_tasks: The active tasks to set.
        """

        self._active_tasks.clear()
        self._active_tasks.extend(active_tasks)

    def remove_active_task(self, active_task: ActiveTask) -> None:
        if active_task in self._active_tasks:
            self._active_tasks.remove(active_task)

    def get_active_task(self, task_id: int) -> ActiveTask:
        for active_task in self._active_tasks:
            if active_task.task_id == task_id:
                return active_task

        raise LookupError("Couldn't find active task by id %i." % task_id)

    def get_active_tasks(self) -> List[ActiveTask]:
        return self._active_tasks.copy()

    # ------------------------------ Players ------------------------------ #

    def add_player(self, player: Player) -> None:
        """
        Adds a player to the reporter.

        :param player: The player to add.
        """

        if not player in self._players:
            self._players.append(player)

    def set_players(self, players: List[Player]) -> None:
        """
        Sets the players of the reporter.

        :param players: The players to set.
        """

        self._players.clear()
        self._players.extend(players)

    def remove_player(self, player: Player) -> None:
        """
        Removes a player from the reporter.

        :param player: The player to remove.
        """

        if player in self._players:
            self._players.remove(player)

    def get_player(self, name: str) -> Player:
        """
        Gets a player by their USERNAME (not their display name).

        :param name: The player's username.
        """

        for player in self._players:
            if player.username == name or player.display_name == name:
                return player

        raise LookupError("Couldn't find player by name %r." % name)

    def get_players(self) -> List[Player]:
        return self._players.copy()

    # ------------------------------ Chunk data ------------------------------ #

    def update_chunk_states(self, chunk_states: List[ChunkState]) -> None:
        """
        Updates the chunk states of the reporter.

        :param chunk_states: The chunk states to update.
        """

        for chunk_state in chunk_states:
            self._dim_data[chunk_state.dimension][chunk_state.chunk_position] = chunk_state

    def has_state(self, dimension: int, position: ChunkPosition) -> bool:
        """
        Returns whether a chunk state exists for the given dimension and position.

        :param dimension: The dimension of the chunk.
        :param position: The position of the chunk.
        :return: Whether or not the chunk state exists.
        """

        return position in self._dim_data[dimension][position]

    def get_state(self, dimension: int, position: ChunkPosition) -> ChunkState:
        """
        Gets a chunk state from a dimension and position.

        :param dimension: The dimension of the chunk.
        :param position: The position of the chunk.
        :return: The chunk state.
        """

        return self._dim_data[dimension][position]

    def get_states(self, dimension: int) -> List[ChunkState]:
        """
        Gets the chunk states in the requested dimension;

        :param dimension: The dimension to get the chunk states from.
        :return: A list of chunk states.
        """

        return list(self._dim_data[dimension].values())

    # ------------------------------ Tracked players ------------------------------ #

    def add_tracker(self, tracker: Tracker) -> None:
        """
        Adds a tracker to this reporter.

        :param tracker: The tracker to add.
        """

        if not tracker in self._trackers:
            self._trackers.append(tracker)

    def set_trackers(self, trackers: List[Tracker]) -> None:
        """
        Sets the trackers of this reporter.

        :param trackers: The trackers to set.
        """

        self._trackers.clear()
        self._trackers.extend(trackers)

    def remove_tracker(self, tracker: Tracker) -> None:
        """
        Removes a tracker from this reporter.

        :param tracker: The tracker to remove.
        """

        if tracker in self._trackers:
            self._trackers.remove(tracker)

    def get_tracker(self, tracker_id: int) -> Tracker:
        """
        Gets a tracker by its ID, a LookupError is thrown if the tracker doesn't exist.

        :param tracker_id: The ID of the tracker to get.
        :return: The tracker with the given ID.
        """

        for tracker in self._trackers:
            if tracker.tracker_id == tracker_id:
                return tracker

        raise LookupError("Couldn't find tracker by ID %i." % tracker_id)

    def get_trackers(self) -> List[Tracker]:
        """
        Gets all trackers.

        :return: A list of all trackers.
        """

        return self._trackers.copy()

    # ------------------------------ Online players ------------------------------ #

    def put_online_player(self, uuid: UUID, display_name: str) -> None:  # TODO: Overhaul online players
        self._online_players[uuid] = display_name

    def set_online_players(self, online_players: Dict[UUID, str]) -> None:
        self._online_players.clear()
        self._online_players.update(online_players)

    def put_online_players(self, online_players: Dict[UUID, str]) -> None:
        self._online_players.update(online_players)

    def remove_online_player(self, uuid: UUID) -> None:
        if uuid in self._online_players:
            del self._online_players[uuid]

    def is_player_online(self, uuid: UUID) -> bool:
        return uuid in self._online_players

    def get_online_player(self, uuid: UUID) -> str:
        if not uuid in self._online_players:
            raise LookupError("Couldn't find online player by uuid %r." % uuid)

        return self._online_players[uuid]

    def get_online_players(self) -> Dict[UUID, str]:
        return self._online_players.copy()
