#!/usr/bin/env python3

from typing import List

from viewer.network.packets import YCInitRequestPacket, YCInitResponsePacket, ReporterActionPacket, SyncReporterPacket, \
    TaskActionPacket, PlayerActionPacket, AccountActionPacket, AccountActionResponsePacket, TrackerActionPacket, \
    InfoUpdatePacket, ChunkStatesPacket
from viewer.util import Reporter, RegisteredTask, ActiveTask
from pclient.networking.handlers import Handler
from pclient.networking.packets.packet import Packet


class YCHandler(Handler):

    @property
    def initialized(self) -> bool:
        return self._initialized

    @property
    def sync_done(self) -> bool:
        return self._sync_done

    def __init__(self, connection, viewer) -> None:
        super().__init__(connection)

        self.viewer = viewer

        self._initialized = False
        self._sync_done = True  # TODO: Something with this

        self._account_action_callback = None

    def on_packet(self, packet: Packet) -> None:
        if isinstance(packet, YCInitResponsePacket):
            if self._initialized:
                self.connection.exit("Received yc_init_response when already initialized.")
                return

            self._initialized = True

            if packet.rejected:
                self.connection.exit("Rejected on init due to: %s." % packet.message)
                return
            else:
                self.viewer.logger.info("Successfully initialized YC connection.")

        elif isinstance(packet, ReporterActionPacket):
            if packet.action == ReporterActionPacket.Action.ADD:
                try:
                    self.viewer.get_reporter(packet.reporter_name, packet.reporter_id)
                except LookupError:
                    reporter = Reporter(packet.reporter_id, packet.reporter_name)
                    self.viewer.register_reporter(reporter)

                    # self.viewer.logger.debug("Got new reporter: %r." % reporter)

            elif packet.action == ReporterActionPacket.Action.REMOVE:
                try:
                    self.viewer.unregister_reporter(self.viewer.get_reporter(handler_id=packet.reporter_id))
                except LookupError:
                    ...

        elif isinstance(packet, SyncReporterPacket):
            if packet.has_reporter:
                self.viewer._current_reporter = packet.reporter_id

                try:
                    reporter = self.viewer.get_reporter(handler_id=packet.reporter_id)
                except LookupError as error:
                    self.viewer.logger.warn("Couldn't find reporter in reporter sync:")
                    self.viewer.logger.error(repr(error))
                    self.viewer.current_reporter = -1
                    return

                self.viewer.logger.debug("Syncing reporter: %s." % reporter)

                reporter.reset()

                for registered_task in packet.get_registered_tasks():
                    reporter.add_registered_task(registered_task)

                for active_task in packet.get_active_tasks():
                    reporter.add_active_task(active_task)

                for player in packet.get_players():
                    reporter.add_player(player)

                for tracker in packet.get_trackers():
                    reporter.add_tracker(tracker)

            else:
                self.viewer._current_reporter = -1

        elif isinstance(packet, TaskActionPacket):
            try:
                reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)
            except LookupError:  # TODO: Something
                return

            if packet.action == TaskActionPacket.Action.START:
                ...

            elif packet.action == TaskActionPacket.Action.STOP:
                ...

            elif packet.action == TaskActionPacket.Action.ADD:
                active_task = ActiveTask(reporter.get_registered_task(packet.task_name), packet.task_id,
                                         packet.get_task_params(), 0, 0, [])
                self.viewer.logger.debug("Got task: %r." % active_task)
                reporter.add_active_task(active_task)

            else:
                try:
                    active_task = reporter.get_active_task(packet.task_id)
                except LookupError as error:
                    self.viewer.logger.warn("Error while fetching active task:")
                    self.viewer.logger.error(repr(error))
                    return

                if packet.action == TaskActionPacket.Action.REMOVE:
                    reporter.remove_active_task(active_task)

                elif packet.action == TaskActionPacket.Action.UPDATE:
                    active_task.update(packet.loaded_chunk_task, packet.progress, packet.time_elapsed,
                                       packet.current_position)

                elif packet.action == TaskActionPacket.Action.RESULT:
                    active_task.add_result(packet.result)

        elif isinstance(packet, PlayerActionPacket):
            try:
                reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)
            except LookupError:  # TODO: Something
                return

            if packet.action == PlayerActionPacket.Action.ADD:
                self.viewer.logger.debug("Got player: %r." % packet.player)

                reporter.add_player(packet.player)

            else:
                try:
                    player = reporter.get_player(packet.player_name)
                except LookupError:  # TODO: Something
                    return

                if packet.action == PlayerActionPacket.Action.REMOVE:
                    reporter.remove_player(player)

                elif packet.action == PlayerActionPacket.Action.UPDATE_POSITION:
                    player.position = packet.new_position
                    player.angle = packet.new_angle

                elif packet.action == PlayerActionPacket.Action.UPDATE_DIMENSION:
                    player.dimension = packet.new_dimension

                elif packet.action == PlayerActionPacket.Action.UPDATE_HEALTH:
                    player.health = packet.new_health
                    player.food = packet.new_hunger
                    player.saturation = packet.new_saturation

        elif isinstance(packet, AccountActionResponsePacket):
            if self._account_action_callback is not None:
                self._account_action_callback(packet.successful, packet.message)
                self._account_action_callback = None
            else:
                self.viewer.logger.warn("Got unknown account action response!")

        elif isinstance(packet, ChunkStatesPacket):
            try:
                reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)
            except LookupError as error:
                self.viewer.logger.warn("Error while fetching current reporter:")
                self.viewer.logger.error(repr(error))
                return

            reporter.update_chunk_states(packet.get_chunk_states())

        elif isinstance(packet, TrackerActionPacket):
            try:
                reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)
            except LookupError as error:
                self.viewer.logger.warn("Error while fetching current reporter:")
                self.viewer.logger.error(repr(error))
                return

            if packet.action == TrackerActionPacket.Action.ADD:
                self.viewer.logger.debug("Got tracker: %r." % packet.tracker)

                reporter.add_tracker(packet.tracker)

            else:
                try:
                    tracker = reporter.get_tracker(packet.tracker_id)
                except LookupError as error:
                    self.viewer.logger.warn("Error while fetching tracker:")
                    self.viewer.logger.error(repr(error))
                    return

                if packet.action == TrackerActionPacket.Action.REMOVE:
                    reporter.remove_tracker(tracker)

                elif packet.action == TrackerActionPacket.Action.UPDATE:
                    tracker.tracked_player = packet.tracked_player

        elif isinstance(packet, InfoUpdatePacket):
            try:
                reporter = self.viewer.get_reporter(handler_id=self.viewer.current_reporter)
            except LookupError as error:
                self.viewer.logger.warn("Error while fetching current reporter:")
                self.viewer.logger.error(repr(error))
                return

            reporter.update_info(packet.waiting_queries, packet.ticking_queries, packet.is_connected, packet.tick_rate,
                                 packet.time_since_last_packet)

    def init(self) -> None:
        if self._initialized:
            raise Exception("Already initialized.")

        yc_init_request = YCInitRequestPacket()
        yc_init_request.handler_name = self.viewer.name
        yc_init_request.listening = True
        yc_init_request.reporting = False
        yc_init_request.host_name = self.viewer.target_host
        yc_init_request.host_port = self.viewer.target_port

        self.viewer.logger.debug("Sending yc_init_request for target: %s:%i." % (self.viewer.target_host,
                                                                                 self.viewer.target_port))

        self.connection.send_packet(yc_init_request)

        self.viewer.logger.debug("Waiting for server response...")

    def start_task(self, registered_task: RegisteredTask, parameters: List[ActiveTask.Parameter]) -> None:
        task_action = TaskActionPacket()
        task_action.action = TaskActionPacket.Action.START
        task_action.task_name = registered_task.name
        task_action.set_task_params(parameters)

        self.connection.send_packet(task_action)

    def stop_task(self, task_id: int) -> None:
        task_action = TaskActionPacket()
        task_action.action = TaskActionPacket.Action.STOP
        task_action.task_id = task_id

        self.connection.send_packet(task_action)

    def add_account(self, username: str, password: str, callback) -> None:
        if self._account_action_callback is None:
            self._account_action_callback = callback

            self.viewer.logger.debug("Authenticating account: %r." % username)

            account_action = AccountActionPacket()
            account_action.action = AccountActionPacket.Action.ADD
            account_action.username = username
            account_action.password = password

            self.connection.send_packet(account_action)

        else:  # We are already performing an action so do nothing
            ...  # TODO: Maybe throw an error

    def remove_account(self, username: str) -> None:
        self.viewer.logger.debug("De-authenticating account: %r." % username)

        account_action = AccountActionPacket()
        account_action.action = AccountActionPacket.Action.REMOVE
        account_action.username = username

        self.connection.send_packet(account_action)
