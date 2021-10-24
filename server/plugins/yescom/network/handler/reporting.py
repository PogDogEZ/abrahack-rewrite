#!/usr/bin/env python3

from typing import List

from network.networking.packets import Packet
from plugins.yescom.network.handler.archiving import Archiver
from plugins.yescom.network.handler.listening import Listener
from plugins.yescom.network.packets.reporting import TaskSyncPacket, TaskActionPacket, PlayerActionPacket, \
    AccountActionPacket, AccountActionResponsePacket, TrackerActionPacket, \
    InfoUpdatePacket, ChunkStatesPacket
from plugins.yescom.util import Player, RegisteredTask, ActiveTask, TrackedPlayer, Tracker


class Reporter(Archiver):

    @property
    def handler_id(self) -> int:
        return self._handler_id

    @property
    def handler_name(self) -> str:
        return self._handler_name

    @property
    def registered_tasks(self) -> List[RegisteredTask]:
        return self._registered_tasks.copy()

    @property
    def active_tasks(self) -> List[ActiveTask]:
        return self._active_tasks.copy()

    @property
    def players(self) -> List[Player]:
        return self._players.copy()

    @property
    def trackers(self) -> List[Tracker]:
        return self._trackers.copy()

    # -------------------- Class Methods -------------------- #

    def __init__(self, system, connection, yescom, handler_id: int, handler_name: str) -> None:
        super().__init__(system, connection, yescom, handler_id, handler_name, False, True)

        self._listeners = []
        self._archivers = []

        self._config = {}

        self._registered_tasks = []
        self._active_tasks = []
        self._players = []
        self._trackers = []

        self._account_action_id = 0

    # -------------------- Events -------------------- #

    def on_packet(self, packet: Packet) -> None:
        super().on_packet(packet)

        if isinstance(packet, TaskSyncPacket):
            self.yescom.logger.debug("Syncing tasks...")

            self._registered_tasks.clear()
            self._registered_tasks.extend(packet.get_registered_tasks())

            self.yescom.logger.debug("Done, %i tasks synced." % len(self._registered_tasks))

            for listener in self._listeners:
                listener.do_full_sync()

        elif isinstance(packet, TaskActionPacket):
            if packet.action == TaskActionPacket.Action.START:
                raise Exception("Got unexpected action START, on TaskActionPacket.")

            elif packet.action == TaskActionPacket.Action.STOP:
                raise Exception("Got unexpected action STOP, on TaskActionPacket.")

            elif packet.action == TaskActionPacket.Action.ADD:
                try:
                    registered_task = self.get_registered_task(packet.task_name)
                except LookupError as error:
                    self.yescom.logger.warn("Error while fetching registered task:")
                    self.yescom.logger.error(repr(error))
                    return

                active_task = ActiveTask(registered_task, packet.task_id, packet.get_task_params(), 0, 0, [])

                self.yescom.logger.debug("New task: %r." % active_task)

                if not active_task in self._active_tasks:
                    self._active_tasks.append(active_task)

                for listener in self._listeners:
                    listener.add_active_task(active_task)

            else:
                try:
                    active_task = self.get_active_task(packet.task_id)
                except LookupError as error:
                    self.yescom.logger.warn("Error while fetching active task:")
                    self.yescom.logger.error(repr(error))
                    return

                if packet.action == TaskActionPacket.Action.REMOVE:
                    self._active_tasks.remove(active_task)

                    for listener in self._listeners:
                        listener.remove_active_task(active_task)

                elif packet.action == TaskActionPacket.Action.UPDATE:
                    active_task.update(packet.loaded_chunk_task, packet.progress, packet.time_elapsed,
                                       packet.current_position)

                    for listener in self._listeners:
                        listener.update_active_task(active_task)

                elif packet.action == TaskActionPacket.Action.RESULT:
                    active_task.add_result(packet.result)

                    for listener in self._listeners:
                        listener.on_active_task_result(active_task, packet.result)

        elif isinstance(packet, AccountActionResponsePacket):
            for listener in self._listeners:
                if listener.account_action_id == packet.action_id:
                    listener.account_response(packet.successful, packet.message)
                    break

        elif isinstance(packet, PlayerActionPacket):
            if packet.action == PlayerActionPacket.Action.ADD:
                if not packet.player in self._players:
                    self.yescom.logger.debug("New player: %r." % packet.player)

                    self._players.append(packet.player)

                    for listener in self._listeners:
                        listener.add_player(packet.player)

            else:
                try:
                    player = self.get_player_by_name(packet.player_name)
                except Exception as error:
                    self.yescom.logger.warn("Error while fetching player:")
                    self.yescom.logger.error(repr(error))
                    return

                if packet.action == PlayerActionPacket.Action.REMOVE:
                    self._players.remove(player)

                    for listener in self._listeners:
                        listener.remove_player(player)

                elif packet.action == PlayerActionPacket.Action.UPDATE_POSITION:
                    player.position = packet.new_position
                    player.angle = packet.new_angle

                    for listener in self._listeners:
                        listener.update_player_pos(player)

                elif packet.action == PlayerActionPacket.Action.UPDATE_DIMENSION:
                    player.dimension = packet.new_dimension

                    for listener in self._listeners:
                        listener.update_player_dim(player)

                elif packet.action == PlayerActionPacket.Action.UPDATE_HEALTH:
                    player.health = packet.new_health
                    player.food = packet.new_hunger
                    player.saturation = packet.new_saturation

                    for listener in self._listeners:
                        listener.update_player_health(player)

        elif isinstance(packet, ChunkStatesPacket):
            for listener in self._listeners:
                listener.update_chunk_states(packet.get_chunk_states())

        elif isinstance(packet, TrackerActionPacket):
            if packet.action == TrackerActionPacket.Action.ADD:
                if not packet.tracker in self._trackers:
                    self.yescom.logger.debug("New tracker: %r." % packet.tracker)

                    self._trackers.append(packet.tracker)

                    for listener in self._listeners:
                        listener.add_tracker(packet.tracker)

            else:
                try:
                    tracker = self.get_tracker(packet.tracker_id)
                except LookupError as error:
                    self.yescom.logger.warn("Error while fetching tracker:")
                    self.yescom.logger.error(repr(error))
                    return

                if packet.action == TrackerActionPacket.Action.REMOVE:
                    self._trackers.remove(tracker)

                    for listener in self._listeners:
                        listener.remove_tracker(tracker)

                elif packet.action == TrackerActionPacket.Action.UPDATE:
                    tracker.tracked_player = packet.tracked_player

                    for listener in self._listeners:
                        listener.update_tracker(tracker)

        elif isinstance(packet, InfoUpdatePacket):  # Probably don't need to cache this since we get it every 250ms
            for listener in self._listeners:
                listener.on_info_update(packet.waiting_queries, packet.ticking_queries, packet.is_connected,
                                        packet.tick_rate, packet.time_since_last_packet)

    def on_update(self) -> None:
        ...

    def register_listener(self, listener: Listener) -> None:
        if not listener in self._listeners:
            self._listeners.append(listener)

    def unregister_listener(self, listener: Listener) -> None:
        if listener in self._listeners:
            self._listeners.remove(listener)

    def start_task(self, task_name: str, task_params: List[ActiveTask.Parameter]) -> None:
        task_action = TaskActionPacket()
        task_action.action = TaskActionPacket.Action.START
        task_action.task_name = task_name
        task_action.set_task_params(task_params)

        self.connection.send_packet(task_action)

    def stop_task(self, task_id: int) -> None:
        task_action = TaskActionPacket()
        task_action.action = TaskActionPacket.Action.STOP
        task_action.task_id = task_id

        self.connection.send_packet(task_action)

    def add_account(self, username: str, access_token: str, client_token: str) -> int:
        self._account_action_id += 1

        account_action = AccountActionPacket()
        account_action.action = AccountActionPacket.Action.ADD
        account_action.action_id = self._account_action_id
        account_action.username = username
        account_action.access_token = access_token
        account_action.client_token = client_token

        self.connection.send_packet(account_action)

        return self._account_action_id

    def remove_account(self, username: str) -> None:
        account_action = AccountActionPacket()
        account_action.action = AccountActionPacket.Action.REMOVE
        account_action.username = username

        self.connection.send_packet(account_action)

    # -------------------- Tasks -------------------- #

    def get_registered_task(self, name: str) -> RegisteredTask:
        for registered_task in self._registered_tasks:
            if registered_task.name == name:
                return registered_task

        raise LookupError("Couldn't find registered task by name %r." % name)

    def get_active_task(self, task_id: int) -> ActiveTask:
        for active_task in self._active_tasks:
            if active_task.task_id == task_id:
                return active_task

        raise LookupError("Couldn't find active task by ID %i." % task_id)

    # -------------------- Players -------------------- #

    def get_player_by_name(self, name: str) -> Player:
        """
        Returns a player that this handler knows about.
        :param name: The in game name or username of the player.
        :return: The player.
        """

        for player in self._players:
            if name in (player.username, player.display_name):
                return player

        raise LookupError("Couldn't find player by name %r." % name)

    # -------------------- Trackers -------------------- #

    def get_tracker(self, tracker_id: int) -> Tracker:
        for tracker in self._trackers:
            if tracker.tracked_id == tracker_id:
                return tracker

        raise LookupError("Couldn't find tracker by ID %i.", tracker_id)

    # -------------------- Other Stuff -------------------- #

    def get_listeners(self) -> List[Listener]:
        return self._listeners.copy()

    def get_archivers(self) -> List[Archiver]:
        return self._archivers.copy()
