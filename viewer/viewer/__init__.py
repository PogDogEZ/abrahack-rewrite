#!/usr/bin/env python3

import logging
import time
from typing import List, Union
from uuid import UUID

from pyclient.networking.connection import Connection
from pyclient.networking.handlers import Handler
from pyclient.networking.packets import Packet
from viewer.network.packets import ReporterActionPacket, YCInitRequestPacket, YCInitResponsePacket, ReporterSyncPacket, \
    DataExchangePacket, ConfigActionPacket, TaskActionPacket, PlayerActionPacket, TrackerActionPacket, InfoUpdatePacket, \
    ActionResponsePacket, AccountActionPacket
from viewer.reporter import Reporter
from viewer.util import ActiveTask, RegisteredTask


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

        return self.get_reporter(self._current_reporter)

    @current_reporter.setter
    def current_reporter(self, handler_or_id: Union[Reporter, int]) -> None:
        """
        :param handler_or_id: The reporter to select, or the ID of the reporter to select.
        """

        if self._current_reporter != -1:
            self.get_reporter(self._current_reporter).reset()
        self._current_reporter = -1

        reporter_action = ReporterActionPacket()
        reporter_action.action = ReporterActionPacket.Action.SELECT

        if isinstance(handler_or_id, Reporter):
            reporter_action.reporter_id = handler_or_id.handler_id
        else:
            reporter_action.reporter_id = handler_or_id

        self.connection.send_packet(reporter_action)

        self._account_action = False  # Reset actions
        self._config_action = False
        self._other_action = None

    def __init__(self, connection: Connection, name: str = "test") -> None:
        super().__init__(connection)

        self._id = -1
        self._name = name

        self._current_reporter = -1
        self._reporters = []

        self._config_listeners = []  # TODO: Listeners
        self._task_listeners = []
        self._player_listeners = []
        self._tracker_listeners = []

        self._uuid_to_username_cache = {}  # TODO: Dump this cache

        self._initializing = False
        self._initialized = False

        self._account_action = False
        self._config_action = False
        self._other_action = None

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

        elif isinstance(packet, DataExchangePacket):
            if packet.request_type == DataExchangePacket.RequestType.UPLOAD:
                print(packet.get_data())

        elif isinstance(packet, ReporterActionPacket):
            if packet.action == ReporterActionPacket.Action.ADD:
                self.register_reporter(Reporter(packet.reporter_id, packet.reporter_name))
            else:
                self.unregister_reporter(self.get_reporter(packet.reporter_id))

        elif isinstance(packet, ReporterSyncPacket):
            if not packet.has_reporter:
                logging.debug("Unselecting current reporter.")
                self._current_reporter = -1

            else:
                self._current_reporter = packet.reporter_id
                current_reporter = self.current_reporter

                current_reporter.reset()
                current_reporter.set_config_rules(packet.get_rules())
                current_reporter.set_registered_tasks(packet.get_registered_tasks())
                current_reporter.set_active_tasks(packet.get_active_tasks())
                current_reporter.set_players(packet.get_players())
                current_reporter.set_trackers(packet.get_trackers())

                logging.debug("Current reporter: %r." % current_reporter)

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
                    current_reporter.add_active_task(ActiveTask(current_reporter.get_registered_task(packet.task_name),
                                                                packet.task_id, packet.get_task_params(), 0, 0, []))

                elif packet.action == TaskActionPacket.Action.REMOVE:
                    current_reporter.remove_active_task(current_reporter.get_active_task(packet.task_id))

                elif packet.action == TaskActionPacket.Action.UPDATE:
                    current_reporter.get_active_task(packet.task_id).update(packet.loaded_chunk_task, packet.progress,
                                                                            packet.time_elapsed, packet.current_position)

                elif packet.action == TaskActionPacket.Action.RESULT:
                    current_reporter.get_active_task(packet.task_id).add_result(packet.result)

        elif isinstance(packet, PlayerActionPacket):
            current_reporter = self.current_reporter

            if current_reporter is not None:
                if packet.action == PlayerActionPacket.Action.ADD:
                    logging.debug("New player: %r." % packet.player)
                    current_reporter.add_player(packet.player)

                else:
                    player = current_reporter.get_player(packet.player_name)

                    if packet.action == PlayerActionPacket.Action.REMOVE:
                        logging.debug("%r was disconnected for %r", player, packet.disconnect_reason)
                        current_reporter.remove_player(player)

                    elif packet.action == PlayerActionPacket.Action.UPDATE_POSITION:
                        player.position = packet.new_position
                        player.angle = packet.new_angle

                    elif packet.action == PlayerActionPacket.Action.UPDATE_DIMENSION:
                        player.dimension = packet.new_dimension

                    elif packet.action == PlayerActionPacket.Action.UPDATE_HEALTH:
                        player.health = packet.new_health
                        player.food = packet.new_hunger
                        player.saturation = packet.new_saturation

        elif isinstance(packet, TrackerActionPacket):
            current_reporter = self.current_reporter

            if current_reporter is not None:
                if packet.action == TrackerActionPacket.Action.ADD:
                    current_reporter.add_tracker(packet.tracker)

                else:
                    tracker = current_reporter.get_tracker(packet.tracker_id)

                    if packet.action == TrackerActionPacket.Action.REMOVE:
                        current_reporter.remove_tracker(tracker)

                    elif packet.action == TrackerActionPacket.Action.UPDATE:
                        tracker.set_tracked_player_ids(packet.get_tracked_player_ids())

        elif isinstance(packet, InfoUpdatePacket):
            current_reporter = self.current_reporter

            if current_reporter is not None:
                current_reporter.update_info(packet.waiting_queries, packet.ticking_queries, packet.queries_per_second,
                                             packet.connected, packet.tick_rate, packet.server_ping,
                                             packet.time_since_last_packet)

        elif isinstance(packet, ActionResponsePacket):
            if not self._account_action and not self._config_action and self._other_action is None:
                logging.warning("Action response with no known action.")
                return

            self._action_success = packet.successful
            self._action_message = packet.message

            self._account_action = False
            self._config_action = False
            self._other_action = None

    # ----------------------------- Management stuff ----------------------------- #

    def exit(self) -> None:
        """
        Exits the viewer.
        """

        if self.connection is not None and self.connection.connected:
            self.connection.exit("Exited.")

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

    # ----------------------------- Actions ----------------------------- #

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

        if self._account_action or self._config_action or self._other_action is not None:
            raise Exception("Already waiting for an action to complete.")

        self._account_action = True
        try:
            self.connection.send_packet(AccountActionPacket(action=AccountActionPacket.Action.LOGIN, username=username,
                                                            legacy=legacy, password=password, client_token=client_token,
                                                            access_token=access_token))
        except Exception as error:  # I know some mfs will enter this incorrectly cos I did lol + better safe than sorry
            self._account_action = False
            raise error

        while self._account_action:
            time.sleep(0.1)

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

        if self._account_action or self._config_action or self._other_action is not None:
            raise Exception("Already waiting for an action to complete.")

        self._account_action = True
        try:
            self.connection.send_packet(AccountActionPacket(action=AccountActionPacket.Action.LOGOUT, username=username))
        except Exception as error:
            self._account_action = False
            raise error

        while self._account_action:
            time.sleep(0.1)

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

            self._reporters.remove(reporter)

            if reporter.handler_id == self._current_reporter:
                self._current_reporter = -1

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

    # ----------------------------- Tasks ----------------------------- #

    def start_task(self, registered_task: RegisteredTask, parameters: List[ActiveTask.Parameter]) -> None:
        if self._current_reporter != -1:
            logging.debug("Starting new task %r." % registered_task)
            self._handler.start_task(registered_task, parameters)

    def stop_task(self, task_id: int) -> None:
        if self._current_reporter != -1:
            logging.debug("Stopping task with id %i." % task_id)
            self._handler.stop_task(task_id)

    def add_legacy_account(self, username: str, password: str, callback) -> None:  # FIXME: Maybe don't do this here
        if self._current_reporter != -1:
            self._handler.add_legacy_account(username, password, callback)

    def remove_account(self, username: str) -> None:
        if self._current_reporter != -1:
            self._handler.remove_account(username)

    # ----------------------------- UUID to name cache ----------------------------- #

    def set_name_for_uuid(self, uuid: UUID, name: str) -> None:
        """
        Sets the name for a UUID.

        :param uuid: The UUID.
        :param name: The display name.
        """
        self._uuid_to_username_cache[uuid] = name

    def get_name_for_uuid(self, uuid: UUID) -> str:
        """
        Converts a UUID to a display name.

        :param uuid: The UUID.
        :return: The display name.
        """

        return self._uuid_to_username_cache.get(uuid, "")
