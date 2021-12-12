#!/usr/bin/env python3

import io
import logging
import time
from typing import List, Union, Tuple
from uuid import UUID

from .events import ReporterEvent, DataEvent, PlayerEvent, DataBoundsEvent, ConnectEvent, DisconnectEvent, \
    OnlinePlayerEvent, TaskEvent, TrackerEvent
from .network.packets import ReporterActionPacket, YCInitRequestPacket, YCInitResponsePacket, ReporterSyncPacket, \
    DataExchangePacket, ConfigActionPacket, TaskActionPacket, PlayerActionPacket, TrackerActionPacket, InfoUpdatePacket, \
    ActionResponsePacket, AccountActionPacket, OnlinePlayersActionPacket, ActionRequestPacket
from .reporter import Reporter
from .util import ActiveTask, RegisteredTask, Player
from ..pyclient.networking.connection import Connection
from ..pyclient.networking.handlers import Handler
from ..pyclient.networking.packets import Packet
from ..pyclient.networking.types.basic import String


class Viewer(Handler):
    """
    The Viewer class is responsible for managing the connection to the server.
    """

    @property
    def handler_name(self) -> str:
        """
        :return: The name of the handler, as reported to the server.
        """

        return self._name

    @property
    def handler_id(self) -> int:
        """
        :return: The server's assigned handler ID.
        """

        return self._id

    @property
    def current_reporter(self) -> Reporter:
        """
        :return: The handler ID of the current reporter.
        """

        if self._current_reporter == -1:
            # noinspection PyTypeChecker
            return None

        try:
            return self.get_reporter(self._current_reporter)
        except LookupError:
            return None

    @current_reporter.setter
    def current_reporter(self, handler_or_id: Union[Reporter, int]) -> None:
        """
        :param handler_or_id: The reporter to select, or the ID of the reporter to select.
        """

        reporter_action = ReporterActionPacket()
        reporter_action.action = ReporterActionPacket.Action.SELECT

        if handler_or_id is None:
            handler_or_id = -1

        if isinstance(handler_or_id, Reporter):
            reporter_action.reporter_id = handler_or_id.handler_id
        else:
            reporter_action.reporter_id = handler_or_id

        self.connection.send_packet(reporter_action)

        self._downloading_data = False

        self._account_action = False  # Reset actions
        self._config_action = False
        self._task_action = False
        self._tracker_action = False
        self._other_action = None

    @property
    def initialized(self) -> bool:
        return self._initialized and not self._initializing

    def __init__(self, connection: Connection, name: str = "test") -> None:
        super().__init__(connection)

        self._id = -1
        self._name = name

        self._current_reporter = -1
        self._queued_unregister = False
        self._reporters = []

        self._event_listeners = []

        self._uuid_to_username_cache = {}  # TODO: Dump this cache
        self._username_to_uuid_cache = {}

        self._initializing = False
        self._initialized = False

        self._downloading_data = False

        self._account_action = False  # FIXME: Is this really even appropriate anymore given how many actions there are?
        self._config_action = False
        self._task_action = False
        self._tracker_action = False
        self._other_action = None

        self._data = []
        self._invalid_data_ids = []
        self._data_start_time = 0
        self._data_end_time = 0
        self._data_update_interval = 0
        self._data_max_id = 0
        self._data_min_id = 0

        self._action_success = False
        self._action_message = ""

    def __repr__(self) -> str:
        return "Viewer(connection=%r)" % self.connection

    def on_packet(self, packet: Packet) -> None:
        if isinstance(packet, YCInitResponsePacket):
            if not self._initializing:
                raise Exception("Received unexpected YCInitResponsePacket.")
            if packet.extended_init:
                raise Exception("Extended init not supported.")
            if packet.rejected:
                raise Exception("Initialization rejected: %s" % packet.message)

            logging.info("Successfully initialized YC connection.")
            logging.debug("We are handler ID %i." % packet.handler_id)

            self._id = packet.handler_id

            self._initializing = False
            self._initialized = True

            self._post_event(ConnectEvent())

        elif isinstance(packet, DataExchangePacket):
            if packet.request_type == DataExchangePacket.RequestType.UPLOAD:
                if packet.request_id == -1:  # Broadcast data
                    self._post_event(DataEvent(packet.data_type, packet.get_data(), packet.get_invalid_data_ids(),
                                               packet.start_time, packet.end_time, packet.update_interval))

                else:
                    self._data.clear()
                    self._data.extend(packet.get_data())
                    self._invalid_data_ids.clear()
                    self._invalid_data_ids.extend(packet.get_invalid_data_ids())

                    self._data_start_time = packet.start_time
                    self._data_end_time = packet.end_time
                    self._data_update_interval = packet.update_interval

                    self._post_event(DataEvent(packet.data_type, packet.get_data(), packet.get_invalid_data_ids(),
                                               packet.start_time, packet.end_time, packet.update_interval))

                    self._downloading_data = False

            elif packet.request_type == DataExchangePacket.RequestType.SET_BOUNDS:
                self._data_start_time = packet.start_time
                self._data_end_time = packet.end_time
                self._data_update_interval = packet.update_interval

                self._data_max_id = packet.max_data_id
                self._data_min_id = packet.min_data_id

                self._post_event(DataBoundsEvent(packet.data_type, packet.start_time, packet.end_time,
                                                 packet.update_interval, packet.max_data_id, packet.min_data_id))

                self._downloading_data = False

        elif isinstance(packet, ReporterActionPacket):
            if packet.action == ReporterActionPacket.Action.ADD:
                current_reporter = Reporter(packet.reporter_id, packet.reporter_name, packet.reporter_host, packet.reporter_port)

                self.register_reporter(current_reporter)
                self._post_event(ReporterEvent(ReporterEvent.EventType.ADDED, current_reporter))
            else:
                try:
                    current_reporter = self.get_reporter(packet.reporter_id)

                    self.unregister_reporter(current_reporter)
                    self._post_event(ReporterEvent(ReporterEvent.EventType.REMOVED, current_reporter))
                except LookupError:
                    ...

        elif isinstance(packet, ReporterSyncPacket):
            # Always reset out current reporter on received a new one
            current_reporter = self.current_reporter
            if current_reporter is not None:
                for active_task in current_reporter.get_active_tasks():
                    self._post_event(TaskEvent(TaskEvent.EventType.REMOVED, active_task))
                for player in current_reporter.get_players():
                    self._post_event(PlayerEvent(PlayerEvent.EventType.REMOVED, player))
                for tracker in current_reporter.get_trackers():
                    self._post_event(TrackerEvent(TrackerEvent.EventType.REMOVED, tracker))
                for uuid, name in current_reporter.get_online_players().items():
                    self._post_event(OnlinePlayerEvent(OnlinePlayerEvent.EventType.REMOVED, uuid, name))
                current_reporter.reset()

            if not packet.has_reporter:
                logging.debug("Unselecting current reporter.")

                self._current_reporter = -1
                # Unregister it once we know we have lost our reporter
                if current_reporter is not None and self._queued_unregister:
                    self._queued_unregister = False
                    self.unregister_reporter(current_reporter)

                self._downloading_data = False

                self._account_action = False
                self._config_action = False
                self._task_action = False
                self._tracker_action = False
                self._other_action = None

                # noinspection PyTypeChecker
                self._post_event(ReporterEvent(ReporterEvent.EventType.SELECTED, None))

            else:
                self._current_reporter = packet.reporter_id

                self._downloading_data = False

                self._account_action = False
                self._config_action = False
                self._task_action = False
                self._tracker_action = False
                self._other_action = None

                current_reporter = self.current_reporter

                if current_reporter is not None:
                    current_reporter.reset()
                    current_reporter.set_config_rules(packet.get_rules())
                    current_reporter.set_registered_tasks(packet.get_registered_tasks())
                    current_reporter.set_active_tasks(packet.get_active_tasks())
                    current_reporter.set_players(packet.get_players())
                    current_reporter.set_trackers(packet.get_trackers())

                    for active_task in packet.get_active_tasks():
                        self._post_event(TaskEvent(TaskEvent.EventType.ADDED, active_task))
                    for player in packet.get_players():
                        self._post_event(PlayerEvent(PlayerEvent.EventType.ADDED, player))
                    for tracker in packet.get_trackers():
                        self._post_event(TrackerEvent(TrackerEvent.EventType.UPDATED, tracker))

                    logging.debug("Current reporter: %r." % current_reporter)

                    self._post_event(ReporterEvent(ReporterEvent.EventType.SELECTED, current_reporter))

        elif isinstance(packet, ConfigActionPacket):
            current_reporter = self.current_reporter

            if current_reporter is not None:
                if packet.action == ConfigActionPacket.Action.SYNC_RULE:
                    logging.debug("Syncing config rule %r: %r." % (packet.rule, packet.value))
                    current_reporter.put_config_rule(packet.rule, packet.value)

        elif isinstance(packet, TaskActionPacket):
            current_reporter = self.current_reporter

            if current_reporter is not None:
                if packet.action == TaskActionPacket.Action.ADD:
                    active_task = ActiveTask(current_reporter.get_registered_task(packet.task_name), packet.task_id,
                                             packet.get_task_params(), 0, 0, [])
                    current_reporter.add_active_task(active_task)

                    self._post_event(TaskEvent(TaskEvent.EventType.ADDED, active_task))

                elif packet.action == TaskActionPacket.Action.REMOVE:
                    active_task = current_reporter.get_active_task(packet.task_id)
                    current_reporter.remove_active_task(active_task)

                    self._post_event(TaskEvent(TaskEvent.EventType.REMOVED, active_task))

                elif packet.action == TaskActionPacket.Action.UPDATE:
                    active_task = current_reporter.get_active_task(packet.task_id)
                    active_task.update(packet.loaded_chunk_task, packet.progress, packet.time_elapsed,
                                       packet.current_position)

                    self._post_event(TaskEvent(TaskEvent.EventType.UPDATED, active_task))

                elif packet.action == TaskActionPacket.Action.RESULT:
                    active_task = current_reporter.get_active_task(packet.task_id)
                    active_task.add_result(packet.result)

                    self._post_event(TaskEvent(TaskEvent.EventType.RESULT, active_task, packet.result))

        elif isinstance(packet, PlayerActionPacket):
            current_reporter = self.current_reporter

            if current_reporter is not None:
                if packet.action == PlayerActionPacket.Action.ADD:
                    player = Player(packet.player_name, packet.uuid, packet.display_name)
                    logging.debug("New player: %r." % player)

                    current_reporter.add_player(player)
                    self._post_event(PlayerEvent(PlayerEvent.EventType.ADDED, player))

                else:
                    player = current_reporter.get_player(packet.player_name)
                    if player is None:
                        return

                    if packet.action == PlayerActionPacket.Action.REMOVE:
                        current_reporter.remove_player(player)

                        self._post_event(PlayerEvent(PlayerEvent.EventType.REMOVED, player))

                    elif packet.action == PlayerActionPacket.Action.LOGIN:
                        player.logged_in = True

                        self._post_event(PlayerEvent(PlayerEvent.EventType.LOGIN, player))

                    elif packet.action == PlayerActionPacket.Action.LOGOUT:
                        logging.debug("%r was disconnected for %r", player, packet.disconnect_reason)
                        player.logged_in = False

                        self._post_event(PlayerEvent(PlayerEvent.EventType.LOGOUT, player, packet.disconnect_reason))

                    elif packet.action == PlayerActionPacket.Action.UPDATE_POSITION:
                        player.position = packet.new_position
                        player.angle = packet.new_angle

                        self._post_event(PlayerEvent(PlayerEvent.EventType.UPDATED, player))

                    elif packet.action == PlayerActionPacket.Action.UPDATE_DIMENSION:
                        player.dimension = packet.new_dimension

                        self._post_event(PlayerEvent(PlayerEvent.EventType.UPDATED, player))

                    elif packet.action == PlayerActionPacket.Action.UPDATE_HEALTH:
                        player.health = packet.new_health
                        player.food = packet.new_hunger
                        player.saturation = packet.new_saturation

                        self._post_event(PlayerEvent(PlayerEvent.EventType.UPDATED, player))

        elif isinstance(packet, TrackerActionPacket):
            current_reporter = self.current_reporter

            if current_reporter is not None:
                if packet.action == TrackerActionPacket.Action.ADD:
                    current_reporter.add_tracker(packet.tracker)

                    self._post_event(TrackerEvent(TrackerEvent.EventType.ADDED, packet.tracker))

                else:
                    tracker = current_reporter.get_tracker(packet.tracker_id)

                    if packet.action == TrackerActionPacket.Action.REMOVE:
                        current_reporter.remove_tracker(tracker)

                        self._post_event(TrackerEvent(TrackerEvent.EventType.REMOVED, tracker))

                    elif packet.action == TrackerActionPacket.Action.UPDATE:
                        tracker.set_tracked_player_ids(packet.get_tracked_player_ids())

                        self._post_event(TrackerEvent(TrackerEvent.EventType.UPDATED, tracker))

        elif isinstance(packet, InfoUpdatePacket):
            current_reporter = self.current_reporter

            if current_reporter is not None:
                current_reporter.update_info(packet.waiting_queries, packet.ticking_queries, packet.queries_per_second,
                                             packet.connected, packet.tick_rate, packet.server_ping,
                                             packet.time_since_last_packet)

        elif isinstance(packet, OnlinePlayersActionPacket):
            current_reporter = self.current_reporter

            if current_reporter is not None:
                if packet.action == OnlinePlayersActionPacket.Action.ADD:
                    for uuid, username in packet.get_online_players().items():
                        self._uuid_to_username_cache[uuid] = username
                        self._username_to_uuid_cache[username] = uuid
                        self._post_event(OnlinePlayerEvent(OnlinePlayerEvent.EventType.ADDED, uuid, username))
                    current_reporter.put_online_players(packet.get_online_players())

                else:
                    for uuid in packet.get_online_players():
                        self._post_event(OnlinePlayerEvent(OnlinePlayerEvent.EventType.REMOVED, uuid,
                                                           self._uuid_to_username_cache[uuid]))
                        current_reporter.remove_online_player(uuid)

        elif isinstance(packet, ActionResponsePacket):
            if not self._account_action and not self._config_action and not self._task_action and \
                    not self._tracker_action and self._other_action is None:
                logging.warning("Action response with no known action.")
                return

            self._action_success = packet.successful
            self._action_message = packet.message

            self._account_action = False
            self._config_action = False
            self._task_action = False
            self._tracker_action = False
            self._other_action = None

    def on_exit(self, reason: str) -> None:
        self.exit()

    # ----------------------------- Management stuff ----------------------------- #

    def _post_event(self, event) -> None:
        for event_listener in self._event_listeners:
            event_listener(event)

    def exit(self) -> None:
        """
        Exits the viewer.
        """

        for reporter in self._reporters:
            self._post_event(ReporterEvent(ReporterEvent.EventType.REMOVED, reporter))

        current_reporter = self.current_reporter
        if current_reporter is not None:
            for active_task in current_reporter.get_active_tasks():
                self._post_event(TaskEvent(TaskEvent.EventType.REMOVED, active_task))
            for player in current_reporter.get_players():
                self._post_event(PlayerEvent(PlayerEvent.EventType.REMOVED, player))
            for tracker in current_reporter.get_trackers():
                self._post_event(TrackerEvent(TrackerEvent.EventType.REMOVED, tracker))
            for uuid, name in current_reporter.get_online_players().items():
                self._post_event(OnlinePlayerEvent(OnlinePlayerEvent.EventType.REMOVED, uuid, name))

        self._current_reporter = -1

        if self.connection is not None and self.connection.connected:
            self.connection.exit("Exited.")

        self._post_event(DisconnectEvent())

    def init(self) -> None:
        """
        Initializes the YC connection.
        """

        if self._initializing or self._initialized:
            raise Exception("Already initialized.")

        self.connection.register_handler(self)

        self._initializing = True

        logging.info("Initializing YC connection.")
        self.connection.send_packet(YCInitRequestPacket(client_type=YCInitRequestPacket.ClientType.LISTENING,
                                                        handler_name=self._name))

    def add_listener(self, listener) -> None:
        """
        Adds an event listener to this viewer.

        :param listener: The listener (function) to add.
        """

        self._event_listeners.append(listener)

    def remove_listener(self, listener) -> None:
        """
        Removes an event listener from this viewer.

        :param listener: The listener (function) to remove.
        """

        self._event_listeners.remove(listener)

    # ----------------------------- Actions ----------------------------- #

    def request_numeric_data(self, data_type: DataExchangePacket.DataType, start_time: int,
                             end_time: int) -> Tuple[int, int, int, List[float]]:
        """
        Requests numeric data from the current reporter, this can be tickrate, ping or TSLP data.

        :param data_type: The type of data (should be numeric).
        :param start_time: The start time to request the data from, in unix milliseconds.
        :param end_time: The end time to request the data from, in unix milliseconds.
        :return: The start time, end time, update interval and data itself.
        """

        if not self._initialized or self._initializing:
            raise Exception("Not initialized.")

        if self._downloading_data:
            raise Exception("Already downloading data.")

        if self.current_reporter is None:
            raise Exception("No reporter selected.")

        if not data_type in (DataExchangePacket.DataType.TICK_DATA, DataExchangePacket.DataType.PING_DATA,
                             DataExchangePacket.DataType.TSLP_DATA):
            raise Exception("Invalid data type requested.")

        self._downloading_data = True
        try:
            self.connection.send_packet(DataExchangePacket(request_type=DataExchangePacket.RequestType.DOWNLOAD,
                                                           data_type=data_type, start_time=start_time, end_time=end_time))
        except Exception as error:
            self._downloading_data = False
            raise error

        while self._downloading_data:
            time.sleep(0.01)

        return self._data_start_time, self._data_end_time, self._data_update_interval, self._data.copy()

    def request_data(self, data_type: DataExchangePacket.DataType, data_ids: List[int]) -> Tuple[List[object], List[int]]:
        """
        Requests non-numeric data from the current reporter.

        :param data_type: The type of data to request.
        :param data_ids: The data IDs to request.
        :return: The data, as well as the invalidated IDs.
        """

        if not self._initialized or self._initializing:
            raise Exception("Not initialized.")

        if self._downloading_data:
            raise Exception("Already downloading data.")

        if self.current_reporter is None:
            raise Exception("No reporter selected.")

        if data_type in (DataExchangePacket.DataType.TICK_DATA, DataExchangePacket.DataType.PING_DATA,
                         DataExchangePacket.DataType.TSLP_DATA):
            raise Exception("Invalid data type requested.")

        self._downloading_data = True
        try:
            data_exchange = DataExchangePacket(request_type=DataExchangePacket.RequestType.DOWNLOAD,
                                               data_type=data_type)
            data_exchange.set_data_ids(data_ids)

            self.connection.send_packet(data_exchange)
        except Exception as error:
            self._downloading_data = False
            raise error

        while self._downloading_data:
            time.sleep(0.01)

        return self._data.copy(), self._invalid_data_ids.copy()

    def request_numeric_data_bounds(self, data_type: DataExchangePacket.DataType) -> Tuple[int, int, int]:
        """
        Requests the numeric data bounds from the current reporter.

        :param data_type: The type of data to request.
        :return: The start, end and update interval.
        """

        if not self._initialized or self._initializing:
            raise Exception("Not initialized.")

        if self._downloading_data:
            raise Exception("Already downloading data.")

        if self.current_reporter is None:
            raise Exception("No reporter selected.")

        if not data_type in (DataExchangePacket.DataType.TICK_DATA, DataExchangePacket.DataType.PING_DATA,
                             DataExchangePacket.DataType.TSLP_DATA):
            raise Exception("Invalid data type requested.")

        self._downloading_data = True
        try:
            self.connection.send_packet(DataExchangePacket(request_type=DataExchangePacket.RequestType.GET_BOUNDS,
                                                           data_type=data_type))
        except Exception as error:
            self._downloading_data = False
            raise error

        while self._downloading_data:
            time.sleep(0.01)

        return self._data_start_time, self._data_end_time, self._data_update_interval

    def request_data_bounds(self, data_type: DataExchangePacket.DataType) -> Tuple[int, int]:
        """
        Requests the data bounds of the current reporter.

        :param data_type: The type of data to request.
        :return: The min and max bounds of the data.
        """

        if not self._initialized or self._initializing:
            raise Exception("Not initialized.")

        if self._downloading_data:
            raise Exception("Already downloading data.")

        if self.current_reporter is None:
            raise Exception("No reporter selected.")

        if data_type in (DataExchangePacket.DataType.TICK_DATA, DataExchangePacket.DataType.PING_DATA,
                         DataExchangePacket.DataType.TSLP_DATA):
            raise Exception("Invalid data type requested.")

        self._downloading_data = True
        try:
            self.connection.send_packet(DataExchangePacket(request_type=DataExchangePacket.RequestType.GET_BOUNDS,
                                                           data_type=data_type))
        except Exception as error:
            self._downloading_data = False
            raise error

        while self._downloading_data:
            time.sleep(0.01)

        return self._data_min_id, self._data_max_id

    def sync_config_rule(self, name: str) -> str:
        """
        Requests that a config rule with the provided name is synced, throws an exception if something is invalid.

        :param name: The name of the config rule.
        :return: The success message.
        """

        if self._initializing or not self._initialized:
            raise Exception("Not initialized.")

        if self._account_action or self._config_action or self._task_action or self._tracker_action or \
                self._other_action is not None:
            raise Exception("Already performing an action.")

        if self.current_reporter is None:
            raise Exception("No current reporter.")

        self._config_action = True
        try:
            self.connection.send_packet(ConfigActionPacket(action=ConfigActionPacket.Action.GET_RULE, rule_name=name))
        except Exception as error:
            self._config_action = False
            raise error

        while self._config_action:
            time.sleep(0.01)

        if self._action_success:
            return self._action_message
        else:
            raise Exception(self._action_message)

    def set_config_rule(self, name: str, value: object) -> str:
        """
        Sets a config rule to a certain value, given the name of it, throws an exception if something is invalid.

        :param name: The name of the config rule.
        :param value: The
        :return: The success message.
        """

        if self._initializing or not self._initialized:
            raise Exception("Not initialized.")

        if self._account_action or self._config_action or self._task_action or self._tracker_action or \
                self._other_action is not None:
            raise Exception("Already performing an action.")

        current_reporter = self.current_reporter
        if current_reporter is None:
            raise Exception("No current reporter.")

        for config_rule in current_reporter.get_config_rules():
            if config_rule.name.lower() == name.lower():
                break
        else:
            raise Exception("Config rule by name %r not found." % name)

        self._config_action = True
        try:
            self.connection.send_packet(ConfigActionPacket(action=ConfigActionPacket.Action.SET_RULE, rule=config_rule,
                                                           value=value))
        except Exception as error:
            self._config_action = False
            raise error

        while self._config_action:
            time.sleep(0.01)

        if self._action_success:
            return self._action_message
        else:
            raise Exception(self._action_message)

    def login_account(self, username: str, legacy: bool = False, password: str = None, client_token: str = None,
                      access_token: str = None) -> str:
        """
        Attempts to login an account, this is a blocking method and throws an exception if the login fails, also throws
        an exception if the viewer is already waiting for an action to complete.

        :param username: The username (email) of the account,
        :param legacy: Whether to use the legacy auth system, (use a password or client + access tokens).
        :param password: The password of the account if using the legacy auth.
        :param client_token: The client token if using the new auth.
        :param access_token: Thee access token is using the new auth.
        :return: The success message.
        """

        if not self._initialized or self._initializing:
            raise Exception("Not initialized.")

        if self._account_action or self._config_action or self._task_action or self._tracker_action or \
                self._other_action is not None:
            raise Exception("Already waiting for an action to complete.")

        if self.current_reporter is None:
            raise Exception("No current reporter.")

        self._account_action = True
        try:
            self.connection.send_packet(AccountActionPacket(action=AccountActionPacket.Action.LOGIN, username=username,
                                                            legacy=legacy, password=password, client_token=client_token,
                                                            access_token=access_token))
        except Exception as error:  # I know some mfs will enter this incorrectly cos I did lol + better safe than sorry
            self._account_action = False
            raise error

        while self._account_action:
            time.sleep(0.01)

        if self._action_success:
            return self._action_message
        else:
            raise Exception(self._action_message)

    def logout_account(self, username: str) -> str:
        """
        Attempts to logout an account, this is a blocking method and throws an exception if the logout fails, also throws
        an exception if the viewer is already waiting for an action to complete.

        :param username: The username (not email or display name) of the account.
        :return: The success message.
        """

        if not self._initialized or self._initializing:
            raise Exception("Not initialized.")

        if self._account_action or self._config_action or self._task_action or self._tracker_action or \
                self._other_action is not None:
            raise Exception("Already waiting for an action to complete.")

        if self.current_reporter is None:
            raise Exception("No current reporter.")

        self._account_action = True
        try:
            self.connection.send_packet(AccountActionPacket(action=AccountActionPacket.Action.LOGOUT, username=username))
        except Exception as error:
            self._account_action = False
            raise error

        while self._account_action:
            time.sleep(0.01)

        if self._action_success:
            return self._action_message
        else:
            raise Exception(self._action_message)

    def start_task_raw(self, task_name: str, parameters: List[ActiveTask.Parameter]) -> str:
        """
        Starts a task given the name of the task, and the raw parameters.

        :param task_name: The name of the task to start.
        :param parameters: The raw parameters for the task.
        :return: The success message.
        """

        if not self._initialized or self._initializing:
            raise Exception("Not initialized.")

        if self._account_action or self._config_action or self._task_action or self._tracker_action or \
                self._other_action is not None:
            raise Exception("Already waiting for an action to complete.")

        current_reporter = self.current_reporter
        if current_reporter is None:
            raise Exception("No current reporter.")

        registered_task = current_reporter.get_registered_task(task_name)

        self._task_action = True
        try:
            task_action = TaskActionPacket(action=TaskActionPacket.Action.START, task_name=task_name)
            task_action.set_task_params(parameters)
            self.connection.send_packet(task_action)
        except Exception as error:
            self._task_action = False
            raise error

        while self._task_action:
            time.sleep(0.01)

        if self._action_success:
            return self._action_message
        else:
            raise Exception(self._action_message)

    def start_task(self, task_name: str, **parameters) -> str:
        """
        Starts a task given the name of the task, and some parameters, these are turned into compatible parameters
        automatically.

        :param task_name: The name of the task to start.
        :param parameters: The parameters required for the task.
        :return: The success message.
        """

        if not self._initialized or self._initializing:
            raise Exception("Not initialized.")

        if self._account_action or self._config_action or self._task_action or self._tracker_action or \
                self._other_action is not None:
            raise Exception("Already waiting for an action to complete.")

        current_reporter = self.current_reporter
        if current_reporter is None:
            raise Exception("No current reporter.")

        registered_task = current_reporter.get_registered_task(task_name)

        serializable_params = []
        for param_name in parameters:
            param_description = registered_task.get_param_description(param_name)
            serializable_params.append(ActiveTask.Parameter(param_description, parameters[param_name]))

        return self.start_task_raw(task_name, serializable_params)

    def stop_task(self, task_id: int) -> str:
        """
        Stops a task given its task ID.

        :param task_id: The task ID to stop.
        :return: The success message.
        """

        if not self._initialized or self._initializing:
            raise Exception("Not initialized.")

        if self._account_action or self._config_action or self._task_action or self._tracker_action or \
                self._other_action is not None:
            raise Exception("Already waiting for an action to complete.")

        if self.current_reporter is None:
            raise Exception("No current reporter.")

        self._task_action = True
        try:
            self.connection.send_packet(TaskActionPacket(action=TaskActionPacket.Action.STOP, task_id=task_id))
        except Exception as error:
            self._task_action = False
            raise error

        while self._task_action:
            time.sleep(0.01)

        if self._action_success:
            return self._action_message
        else:
            raise Exception(self._action_message)

    def untrack_tracker(self, tracker_id: int) -> str:
        """
        Untracks a tracker given its tracker ID.

        :param tracker_id: The tracker ID to untrack.
        :return: The success message.
        """

        if not self._initialized or self._initializing:
            raise Exception("Not initialized.")

        if self._account_action or self._config_action or self._task_action or self._tracker_action or \
                self._other_action is not None:
            raise Exception("Already waiting for an action to complete.")

        if self.current_reporter is None:
            raise Exception("No current reporter.")

        self._tracker_action = True
        try:
            self.connection.send_packet(TrackerActionPacket(action=TrackerActionPacket.Action.UNTRACK,
                                                            tracker_id=tracker_id))
        except Exception as error:
            self._tracker_action = False
            raise error

        while self._tracker_action:
            time.sleep(0.01)

        if self._action_success:
            return self._action_message
        else:
            raise Exception(self._action_message)

    def send_chat_message(self, username: str, message: str) -> str:
        """
        Sends a chat message to the server.

        :param username: The USERNAME of the account.
        :param message: The message to send.
        :return: The success message.
        """

        if not self._initialized or self._initializing:
            raise Exception("Not initialized.")

        if self._account_action or self._config_action or self._task_action or self._tracker_action or \
                self._other_action is not None:
            raise Exception("Already waiting for an action to complete.")

        if self.current_reporter is None:
            raise Exception("No current reporter.")

        self._other_action = ActionRequestPacket.Action.SEND_CHAT_MESSAGE
        fileobj = io.BytesIO()
        try:
            String.write(username, fileobj)
            String.write(message, fileobj)

            self.connection.send_packet(ActionRequestPacket(action=ActionRequestPacket.Action.SEND_CHAT_MESSAGE,
                                                            data=fileobj.getvalue()))
        except Exception as error:
            self._other_action = None
            raise error

        while self._other_action is not None:
            time.sleep(0.01)

        if self._action_success:
            return self._action_message
        else:
            raise Exception(self._action_message)

    # ----------------------------- Reporters ----------------------------- #

    def register_reporter(self, reporter: Reporter) -> None:
        """
        Registers a reporter as known.

        :param reporter: The reporter to register.
        """

        if not reporter in self._reporters:
            logging.debug("Registered reporter: %r." % reporter)
            self._reporters.append(reporter)

            if self._current_reporter == -1:
                self.current_reporter = reporter.handler_id

    def unregister_reporter(self, reporter: Reporter) -> None:
        """
        Unregisters a reporter.

        :param reporter: The reporter to unregister.
        """

        if reporter in self._reporters:
            logging.debug("Unregistered reporter: %r." % reporter)

            if reporter.handler_id != self._current_reporter:
                self._reporters.remove(reporter)  # Don't do this here as we need to wait for the reporter sync
            else:
                self._queued_unregister = True

    def get_reporter(self, handler_id: int = None, handler_name: str = None) -> Reporter:
        """
        Gets a reporter by its handler name or ID.

        :param handler_name: The name of the reporter.
        :param handler_id: The ID of the reporter.
        """

        for reporter in self._reporters:
            if (handler_name is None or reporter.handler_name == handler_name) and (handler_id is None or
                                                                                    reporter.handler_id == handler_id):
                return reporter

        raise LookupError("Couldn't find reporter by %r, %r." % (handler_name, handler_id))

    def get_reporters(self) -> List[Reporter]:
        """
        :return: A list of all known reporters.
        """

        return self._reporters.copy()

    # ----------------------------- UUID to name cache ----------------------------- #

    def get_name_for_uuid(self, uuid: UUID) -> str:
        """
        Converts a UUID to a display name.

        :param uuid: The UUID.
        :return: The display name.
        """

        return self._uuid_to_username_cache.get(uuid, "")

    def get_uuid_for_name(self, username: str) -> UUID:
        """
        Converts a display name to a UUID.

        :param username: The display name.
        :return: The UUID, None if not found.
        """

        return self._username_to_uuid_cache.get(username, None)
