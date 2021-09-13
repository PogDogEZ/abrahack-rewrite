#!/usr/bin/env python3

from network.networking.handlers import Handler
from network.networking.packets import Packet
from plugins.yescom.network.handler.reporting import ListenerUpdate
from plugins.yescom.network.packets.listening import SelectReporterPacket, SyncReporterPacket, PlayerActionPacket


class Listener(Handler):

    def __init__(self, system, connection, yescom) -> None:
        super().__init__(system, connection)

        self.yescom = yescom

        self._reporter = None
        self._reporter_update_queue = []

    def on_packet(self, packet: Packet) -> None:
        if isinstance(packet, SelectReporterPacket):
            sync_reporter = SyncReporterPacket()

            if packet.selected_reporter == -1:
                self._reporter = None
                sync_reporter.has_reporter = False
            else:
                reporter = self.yescom.get_reporter(handler_id=packet.selected_reporter)

                if reporter != self._reporter:
                    self._reporter = reporter
                    self._reporter.register_listener(self)

                sync_reporter.has_reporter = True
                sync_reporter.reporter_id = reporter.handler_id
                sync_reporter.reporter_name = reporter.handler_name
                sync_reporter.set_tasks(reporter.registered_tasks)
                sync_reporter.set_players(reporter.players)

            self.connection.send_packet(sync_reporter)

    def on_update(self) -> None:
        ...

    def push_update(self, update: ListenerUpdate) -> None:
        if update.update_type == ListenerUpdate.Type.ACTIVE_TASK_ADD:
            ...

        elif update.update_type == ListenerUpdate.Type.ACTIVE_TASK_REMOVE:
            ...

        elif update.update_type == ListenerUpdate.Type.PLAYER_ADD:
            player_action = PlayerActionPacket()
            player_action.action = PlayerActionPacket.Action.ADD
            player_action.player = update.args[0]

            self.connection.send_packet(player_action)

        elif update.update_type == ListenerUpdate.Type.PLAYER_REMOVE:
            player_action = PlayerActionPacket()
            player_action.action = PlayerActionPacket.Action.REMOVE
            player_action.player_name = update.args[0].username

            self.connection.send_packet(player_action)

        elif update.update_type == ListenerUpdate.Type.PLAYER_POS:
            player_action = PlayerActionPacket()
            player_action.action = PlayerActionPacket.Action.UPDATE_POSITION
            player_action.player_name = update.args[0].username
            player_action.new_position = update.args[0].position
            player_action.new_angle = update.args[0].angle

            self.connection.send_packet(player_action)

        elif update.update_type == ListenerUpdate.Type.PLAYER_DIM:
            player_action = PlayerActionPacket()
            player_action.action = PlayerActionPacket.Action.UPDATE_DIMENSION
            player_action.player_name = update.args[0].username
            player_action.new_dimension = update.args[0].dimension

            self.connection.send_packet(player_action)

        elif update.update_type == ListenerUpdate.Type.PLAYER_HEALTH:
            player_action = PlayerActionPacket()
            player_action.action = PlayerActionPacket.Action.UPDATE_HEALTH
            player_action.player_name = update.args[0].username
            player_action.new_health = update.args[0].health
            player_action.new_hunger = update.args[0].food
            player_action.new_saturation = update.args[0].saturation

        elif update.update_type == ListenerUpdate.Type.LOADED_CHUNK:
            ...
