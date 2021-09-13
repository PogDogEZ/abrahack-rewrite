#!/usr/bin/env python3

from typing import List

from network.networking.handlers import Handler
from network.networking.packets import Packet
from plugins.yescom.network.handler.listening import Listener
from plugins.yescom.network.packets import TaskSyncPacket, TaskActionPacket, PlayerActionPacket, LoadedChunkPacket
from plugins.yescom.util import Player, Task


class ListenerUpdate:

    def __init__(self, update_type, *args) -> None:
        self.update_type = update_type
        self.args = args

    class Type:
        ACTIVE_TASK_ADD = 0
        ACTIVE_TASK_REMOVE = 1
        PLAYER_ADD = 2
        PLAYER_REMOVE = 3
        PLAYER_POS = 4
        PLAYER_DIM = 5
        PLAYER_HEALTH = 6
        LOADED_CHUNK = 7


class Reporter(Handler):

    @property
    def handler_id(self) -> int:
        return self._handler_id

    @property
    def handler_name(self) -> str:
        return self._handler_name

    @property
    def registered_tasks(self) -> List[Task]:
        return self._registered_tasks.copy()

    @property
    def active_tasks(self) -> None:
        ...

    @property
    def players(self) -> List[Player]:
        return self._players.copy()

    # -------------------- Class Methods -------------------- #

    def __init__(self, system, connection, yescom, handler_id: int, handler_name: str) -> None:
        super().__init__(system, connection)

        self.yescom = yescom

        self._handler_id = handler_id
        self._handler_name = handler_name

        self._listeners = []

        self._registered_tasks = []
        self._active_tasks = []
        self._players = []

        self._loaded_chunks = {
            -1: [],
            0: [],
            1: [],
        }

    # -------------------- Events -------------------- #

    def on_packet(self, packet: Packet) -> None:
        if isinstance(packet, TaskSyncPacket):
            self.yescom.logger.debug("Syncing tasks...")

            self._registered_tasks.clear()
            self._registered_tasks.extend(packet.get_tasks())

            self.yescom.logger.debug("Done, %i tasks synced." % len(self._registered_tasks))

        elif isinstance(packet, TaskActionPacket):
            if packet.action == TaskActionPacket.Action.START:
                raise Exception("Got unexpected action START, on TaskActionPacket.")

            elif packet.action == TaskActionPacket.Action.STOP:
                raise Exception("Got unexpected action STOP, on TaskActionPacket.")

            elif packet.action == TaskActionPacket.Action.ADD:
                ...  # self._push_update(ListenerUpdate(ListenerUpdate.Type.ACTIVE_TASK, None))

            elif packet.action == TaskActionPacket.Action.REMOVE:
                ...  # self._push_update(ListenerUpdate(ListenerUpdate.Type.ACTIVE_TASK, None))

        elif isinstance(packet, PlayerActionPacket):
            if packet.action == PlayerActionPacket.Action.ADD:
                if not packet.player in self._players:
                    self.yescom.logger.debug("New player: %r." % packet.player)

                    self._players.append(packet.player)
                    self._push_update(ListenerUpdate(ListenerUpdate.Type.PLAYER_ADD, packet.player))

            else:
                try:
                    player = self.get_player_by_name(packet.player_name)
                except Exception as error:
                    self.yescom.logger.warn("Error while fetching player:")
                    self.yescom.logger.error(repr(error))
                    return

                if packet.action == PlayerActionPacket.Action.REMOVE:
                    self._players.remove(player)

                    self._push_update(ListenerUpdate(ListenerUpdate.Type.PLAYER_REMOVE, player))

                elif packet.action == PlayerActionPacket.Action.UPDATE_POSITION:
                    player.position = packet.new_position
                    player.angle = packet.new_angle

                    self._push_update(ListenerUpdate(ListenerUpdate.Type.PLAYER_POS, player))

                elif packet.action == PlayerActionPacket.Action.UPDATE_DIMENSION:
                    player.dimension = packet.new_dimension

                    self._push_update(ListenerUpdate(ListenerUpdate.Type.PLAYER_DIM, player))

                elif packet.action == PlayerActionPacket.Action.UPDATE_HEALTH:
                    player.health = packet.new_health
                    player.food = packet.new_hunger
                    player.saturation = packet.new_saturation

                    self._push_update(ListenerUpdate(ListenerUpdate.Type.PLAYER_HEALTH, player))

        elif isinstance(packet, LoadedChunkPacket):
            self._loaded_chunks[packet.dimension].append(packet.chunk_position)

    def on_update(self) -> None:
        ...

    def _push_update(self, update: ListenerUpdate) -> None:
        for listener in self._listeners:
            listener.push_update(update)

    def register_listener(self, listener: Listener) -> None:
        if not listener in self._listeners:
            self._listeners.append(listener)

    def unregister_listener(self, listener: Listener) -> None:
        if listener in self._listeners:
            self._listeners.remove(listener)

    # -------------------- Tasks -------------------- #

    def get_registered_task(self, name: str) -> Task:
        ...

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
