#!/usr/bin/env python3
from typing import List

from network.networking.handlers import Handler
from network.networking.packets import Packet
from plugins.yescom.network.packets import DataRequestPacket
from plugins.yescom.network.packets.listening import SelectReporterPacket, SyncReporterPacket, PlayerActionPacket, \
    ReporterActionPacket, AccountActionPacket, AccountActionResponsePacket, TaskActionPacket, ChunkStatesPacket, \
    TrackerActionPacket, InfoUpdatePacket
from plugins.yescom.util import ActiveTask, Player, ChunkState, TrackedPlayer, Tracker


class Listener(Handler):

    # -------------------- Properties -------------------- #

    @property
    def handler_id(self) -> int:
        return self._handler_id

    @property
    def handler_name(self) -> str:
        return self._handler_name

    @property
    def reporter(self):  # -> Reporter:
        return self._reporter

    @property
    def account_action_id(self) -> int:
        return self._account_action_id

    # -------------------- Class Methods -------------------- #

    def __init__(self, system, connection, yescom, handler_id: int, handler_name: str) -> None:
        super().__init__(system, connection)

        self.yescom = yescom

        self._handler_id = handler_id
        self._handler_name = handler_name

        self._archivers = []

        self._reporter = None
        self._reporter_update_queue = []

        self._account_action_id = -1

    def on_packet(self, packet: Packet) -> None:
        if isinstance(packet, DataRequestPacket):
            if packet.request_type == DataRequestPacket.RequestType.DOWNLOAD:
                ...

            elif packet.request_type == DataRequestPacket.RequestType.CANCEL:
                for archiver in self._archivers:
                    archiver.cancel_request(self)

        elif isinstance(packet, SelectReporterPacket):
            sync_reporter = SyncReporterPacket()

            if packet.selected_reporter == -1:
                if self._reporter is not None:
                    self._reporter.unregister_listener(self)
                self._reporter = None
                sync_reporter.has_reporter = False
            else:
                reporter = self.yescom.get_reporter(handler_id=packet.selected_reporter)

                if reporter != self._reporter:
                    if self._reporter is not None:
                        self._reporter.unregister_listener(self)

                    self._reporter = reporter
                    self._reporter.register_listener(self)

                sync_reporter.has_reporter = True
                sync_reporter.reporter_id = reporter.handler_id
                sync_reporter.reporter_name = reporter.handler_name
                sync_reporter.set_registered_tasks(reporter.registered_tasks)
                sync_reporter.set_active_tasks(reporter.active_tasks)
                sync_reporter.set_players(reporter.players)
                sync_reporter.set_trackers(reporter.trackers)

            self.connection.send_packet(sync_reporter)

        elif isinstance(packet, TaskActionPacket):
            if self._reporter is None:
                self.connection.exit("Task action with no selected reporter!")
                return

            if packet.action == TaskActionPacket.Action.START:
                self._reporter.start_task(packet.task_name, packet.get_task_params())

            elif packet.action == TaskActionPacket.Action.STOP:
                self._reporter.stop_task(packet.task_id)

            elif packet.action == TaskActionPacket.Action.ADD:
                raise Exception("Got unexpected action ADD, on TaskActionPacket.")

            elif packet.action == TaskActionPacket.Action.REMOVE:
                raise Exception("Got unexpected action REMOVE, on TaskActionPacket.")

        elif isinstance(packet, AccountActionPacket):
            if self._reporter is None:
                self.connection.exit("Account action with no selected reporter!")
                return

            if self._account_action_id != -1:
                self.connection.exit("Concurrent account action!")
                return

            if packet.action == AccountActionPacket.Action.ADD:
                self._account_action_id = self._reporter.add_account(packet.username, packet.access_token,
                                                                     packet.client_token)

            elif packet.action == AccountActionPacket.Action.REMOVE:
                self._reporter.remove_account(packet.username)

    def on_update(self) -> None:
        ...

    # -------------------- Reporter Updates -------------------- #

    def add_active_task(self, active_task: ActiveTask) -> None:
        task_action = TaskActionPacket()
        task_action.action = TaskActionPacket.Action.ADD
        task_action.task_id = active_task.task_id
        task_action.task_name = active_task.registered_task.name
        task_action.set_task_params(active_task.parameters)

        self.connection.send_packet(task_action)

    def remove_active_task(self, active_task: ActiveTask) -> None:
        task_action = TaskActionPacket()
        task_action.action = TaskActionPacket.Action.REMOVE
        task_action.task_id = active_task.task_id

        self.connection.send_packet(task_action)

    def update_active_task(self, active_task: ActiveTask) -> None:
        task_action = TaskActionPacket()
        task_action.action = TaskActionPacket.Action.UPDATE
        task_action.task_id = active_task.task_id
        task_action.loaded_chunk_task = active_task.loaded_chunk_task
        task_action.progress = active_task.progress
        task_action.time_elapsed = active_task.time_elapsed
        task_action.current_position = active_task.current_position

        self.connection.send_packet(task_action)

    def on_active_task_result(self, active_task: ActiveTask, result: str) -> None:
        task_action = TaskActionPacket()
        task_action.action = TaskActionPacket.Action.RESULT
        task_action.task_id = active_task.task_id
        task_action.result = result

        self.connection.send_packet(task_action)

    def add_player(self, player: Player) -> None:
        player_action = PlayerActionPacket()
        player_action.action = PlayerActionPacket.Action.ADD
        player_action.player = player

        self.connection.send_packet(player_action)

    def remove_player(self, player: Player) -> None:
        player_action = PlayerActionPacket()
        player_action.action = PlayerActionPacket.Action.REMOVE
        player_action.player_name = player.username

        self.connection.send_packet(player_action)

    def update_player_pos(self, player: Player) -> None:
        player_action = PlayerActionPacket()
        player_action.action = PlayerActionPacket.Action.UPDATE_POSITION
        player_action.player_name = player.username
        player_action.new_position = player.position
        player_action.new_angle = player.angle

        self.connection.send_packet(player_action)

    def update_player_dim(self, player: Player) -> None:
        player_action = PlayerActionPacket()
        player_action.action = PlayerActionPacket.Action.UPDATE_DIMENSION
        player_action.player_name = player.username
        player_action.new_dimension = player.dimension

        self.connection.send_packet(player_action)

    def update_player_health(self, player: Player) -> None:
        player_action = PlayerActionPacket()
        player_action.action = PlayerActionPacket.Action.UPDATE_HEALTH
        player_action.new_health = player.health
        player_action.new_hunger = player.food
        player_action.new_saturation = player.saturation

        self.connection.send_packet(player_action)

    def update_chunk_states(self, chunk_states: List[ChunkState]) -> None:
        chunk_states_packet = ChunkStatesPacket()
        chunk_states_packet.set_chunk_states(chunk_states)

        self.connection.send_packet(chunk_states_packet)

    def add_tracker(self, tracker: Tracker) -> None:
        tracker_action = TrackerActionPacket()
        tracker_action.action = TrackerActionPacket.Action.ADD
        tracker_action.tracker = tracker

        self.connection.send_packet(tracker_action)

    def remove_tracker(self, tracker: Tracker) -> None:
        tracker_action = TrackerActionPacket()
        tracker_action.action = TrackerActionPacket.Action.REMOVE
        tracker_action.tracker_id = tracker.tracked_id

        self.connection.send_packet(tracker_action)

    def update_tracker(self, tracker: Tracker) -> None:
        tracker_action = TrackerActionPacket()
        tracker_action.action = TrackerActionPacket.Action.UPDATE
        tracker_action.tracker_id = tracker.tracked_id
        tracker_action.tracked_player = tracker.tracked_player

        self.connection.send_packet(tracker_action)

    def on_info_update(self, waiting_queries: int, ticking_queries: int, queries_per_second: float, is_connected: bool,
                       tick_rate: float, time_since_last_packet: int) -> None:
        info_update = InfoUpdatePacket()
        info_update.waiting_queries = waiting_queries
        info_update.ticking_queries = ticking_queries
        info_update.queries_per_second = queries_per_second
        info_update.is_connected = is_connected
        info_update.tick_rate = tick_rate
        info_update.time_since_last_packet = time_since_last_packet

        self.connection.send_packet(info_update)

    def sync_reporter(self, reporter) -> None:
        reporter_action = ReporterActionPacket()
        reporter_action.action = (ReporterActionPacket.Action.ADD if self.yescom.has_reporter(reporter) else
                                  ReporterActionPacket.Action.REMOVE)

        reporter_action.reporter_id = reporter.handler_id
        reporter_action.reporter_name = reporter.handler_name

        self.connection.send_packet(reporter_action)

    def do_full_sync(self) -> None:
        for reporter in self.yescom.get_reporters():
            self.sync_reporter(reporter)

    def account_response(self, successful: bool, message: str) -> None:
        account_action_response = AccountActionResponsePacket()
        account_action_response.successful = successful
        account_action_response.message = message

        self.connection.send_packet(account_action_response)

        self._account_action_id = -1

    # -------------------- Other Stuff -------------------- #

    def get_archivers(self) -> List:  # -> List[Archiver]:
        return self._archivers.copy()
