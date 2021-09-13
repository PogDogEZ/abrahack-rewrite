#!/usr/bin/env python3

import ctypes
import os
import re
import threading
import time

from typing import Any, List, Tuple, Type

import network.networking.packets

from network.impl.dummy import DummyConnection
from network.impl.event.bus import EventBus
from network.impl.event.syst import PostUserLoginEvent, PreUserLoginEvent, PreUserLogOutEvent, PostUserLogOutEvent, \
    SystemUpdateEvent
from network.impl.generic import GenericAsyncUpdater
from network.impl.logger import Logger
from network.impl.permissions import perm_check, PermissionChecker
from network.impl.plugin import BasePlugin
from network.impl.plugin.plugin_loader import PluginLoader
from network.impl.user import User, Group
from network.impl.user.group import UsernameOnlyGroup, PasswordOnlyGroup, TestGroup, SystemUsersGroup
from network.networking.connection import ServerConnection
from network.networking.packets import Packet
from network.networking.server import Server
from network.networking.types.enum import EncryptionType


class System:

    INSTANCE = None

    # -------------------- System properties -------------------- #

    @property
    def exit(self) -> bool:
        return self._exit

    @property
    def logger(self) -> Logger:
        return self._logger

    @property
    def event_bus(self) -> EventBus:
        return self._event_bus

    @property
    def server_name(self) -> str:
        return self._server_name

    @property
    def login_message(self) -> str:
        return self._login_message

    # -------------------- Class methods -------------------- #

    def __init__(self, logger: Logger, protocol_version: int = 4,
                 server_name: str = "testserver", login_message: str = "welcome",
                 encryption: bool = False, encryption_type: EncryptionType = EncryptionType.NONE,
                 compression: bool = True, compression_threshold: int = 256,
                 authentication: bool = True,
                 thread_timeout_ms: int = 500,
                 plugin_directory: str = "plugins") -> None:
        self._logger = logger

        if System.INSTANCE is not None:
            self.logger.error("System instance has already been created, exiting.")
            return

        System.INSTANCE = self

        self.logger.info("Initialising system...")

        self._permission_checker = PermissionChecker(self)

        self.protocol_version = protocol_version

        self._plugins = []  # Lists are thread-safe so no fancy stuff here

        self._connections = []
        self._servers = []

        self._groups = [SystemUsersGroup(self)]
        self._users = self._groups[0].get_users()

        self._logged_users = []  # For quicker access of checking on logged in users

        self.logger.debug("Registering system event handler...")
        self._event_bus = EventBus(self)
        self.logger.debug("Done.")

        self._exit = False

        self._async_updated_objects = {}

        self.log_in_user(self._users[0], "", DummyConnection(self))
        self.log_in_user(self._users[1], "", DummyConnection(self))

        self._server_name = server_name
        self._login_message = login_message
        self._encryption_enabled = encryption
        self._encryption_type = encryption_type
        self._compression_enabled = compression
        self._compression_threshold = compression_threshold
        self._authentication_enabled = authentication

        """
        if self._auth_type == AuthType.USERNAME:
            self.logger.debug("Initialising username only group as default server group...")
            self.default_group = UsernameOnlyGroup("username_only_default", 0, 1, self)
        elif self._auth_type == AuthType.PASSWORD:
            self.logger.debug("Initialising password only group as default server group...")
            self.default_group = PasswordOnlyGroup("password_only_default", 0, 1, self, server_password)
        """

        self.default_group = None  # TestGroup(self._server_name, ["local"], 0, 1, self)

        if self.default_group is not None:
            self.add_group(self.default_group)

        self.thread_timeout_ms = thread_timeout_ms

        self.plugin_loader = PluginLoader(self, plugin_directory)

        self.logger.info("System initialised.")

    def __repr__(self) -> str:
        return "System"

    # -------------------- Hidden methods -------------------- #

    def _do_safe_shutdown(self) -> None:
        start = time.time()

        self.logger.log("Initiating system shutdown...")

        self.logger.log("Closing all connections...")
        for connection in self._connections:
            try:
                updater = self.get_updater(connection)
            except (AttributeError, KeyError) as error:
                self.logger.debug("An error occurred while fetching async updater for '%r': %r" % (connection, error))
                continue

            self.kill_thread_with_optional_force(updater)

            connection.exit("Server shutdown.")
            threading.Thread(target=connection.on_update).start()  # We don't have to wait for the update

        self.logger.log("Closing all servers...")
        for server in self._servers:  # Servers have no exit handle, we need to kill the updater thread
            try:
                updater = self.get_updater(server)
            except (AttributeError, KeyError) as error:  # Doesn't matter, they aren't in a thread that we know about
                self.logger.debug("An error occurred while fetching async updater for '%r': %r" % (server, error))
                continue

            self.kill_thread_with_optional_force(updater)

        self.logger.log("Shutting down all plugins...")
        for plugin in self._plugins.copy():
            plugin.exit()

            if plugin.thread is not None and plugin.thread.is_alive():
                self.kill_thread_with_optional_force(plugin.thread)

        self.logger.log("System shutdown took %i millis." % ((time.time() - start) * 1000))

    def _do_force_shutdown(self) -> None:
        os._exit(1)

    # -------------------- Decorators -------------------- #

    def async_updated(self, obj: Any) -> None:
        self._async_updated_objects[obj] = GenericAsyncUpdater(obj, self, 0)
        self._async_updated_objects[obj].start()

    # -------------------- Attribute methods -------------------- #

    def get_encrypted(self) -> bool:
        return self._encryption_enabled

    def get_encryption_type(self) -> EncryptionType:
        return self._encryption_type

    def get_compression(self) -> bool:
        return self._compression_enabled

    def get_compression_threshold(self) -> int:
        return self._compression_threshold

    def get_authentication(self) -> bool:
        return self._authentication_enabled

    def is_async_updated(self, obj: Any) -> bool:
        return obj in self._async_updated_objects

    def get_updater(self, obj: Any) -> GenericAsyncUpdater:
        if not obj in self._async_updated_objects:
            raise AttributeError("Object '%r' is not async updated." % obj)

        return self._async_updated_objects[obj]

    def get_logged_users(self) -> List[User]:
        return self._logged_users.copy()

    def get_connections(self) -> List[ServerConnection]:
        return self._connections.copy()

    def get_servers(self) -> List[Server]:
        return self._servers.copy()

    # -------------------- Management methods -------------------- #

    def shutdown(self, force: bool = False) -> None:
        if force:
            self._do_safe_shutdown()
            self._do_force_shutdown()

        self._exit = True
        self.update()  # We need to update immediately on shutdown as the error may be fatal

    def kill_thread_with_optional_force(self, thread: threading.Thread) -> None:
        self.logger.debug("Attempting to kill thread '%r' with optional force..." % thread)

        if hasattr(thread, "exit"):
            self.logger.debug("Attempted soft exit, waiting on timeout of %i millis." % self.thread_timeout_ms)

            thread.exit()

            force_exit = False
            start = time.time()

            while thread.is_alive():
                if time.time() - start > (self.thread_timeout_ms / 1000):
                    force_exit = True
                    break

            if force_exit:
                self.kill_thread(thread)
                self.logger.debug("Killed thread '%r' with force." % thread)
            else:
                self.logger.debug("Killed thread '%r' with soft exit." % thread)
        else:
            self.kill_thread(thread)
            self.logger.debug("Killed thread '%r' with force." % thread)

    def kill_thread(self, thread: threading.Thread) -> bool:
        return self.thread_exception(thread, SystemExit)

    def thread_exception(self, thread: threading.Thread, exception: BaseException) -> bool:
        assert isinstance(thread, threading.Thread), "Param 'thread' must be of type threading.Thread"

        return bool(ctypes.pythonapi.PyThreadState_SetAsyncExc(ctypes.c_long(thread.ident), ctypes.py_object(exception)))

    # -------------------- Plugins -------------------- #

    def load_plugins(self) -> None:
        self.logger.info("Loading all plugins...")

        for plugin in self._plugins:
            plugin.load()

        self.logger.info("Done.")

    def get_plugin_from_name(self, plugin_name: str) -> None:
        assert isinstance(plugin_name, str), "Param 'plugin_name' must be of type str."

        for plugin in self._plugins:
            if plugin.NAME == plugin_name:
                return plugin

        raise LookupError("Unable to find plugin by name of %r." % plugin_name)

    def get_plugin_from_class(self, plugin_class: BasePlugin) -> None:
        for plugin in self._plugins:
            if isinstance(plugin, plugin_class):
                return plugin

        raise LookupError("Unable to find plugins from class '%r'." % plugin_class)

    # -------------------- User methods -------------------- #

    def log_in_user(self, user: User, password: str, connection: ServerConnection) -> None:
        assert isinstance(user, User), "Param 'user' must be of type User."

        # FIXME: Check if the connection already has an attached user, if so don't authenticate or log the old one out

        if user.is_logged_in():
            raise PermissionError("User is already logged in.")

        if not user in self._users:
            raise LookupError("User is not known to the system.")

        if self.event_bus.post(PreUserLoginEvent(user)):
            return
        user.group.authenticate(user, user.username, password)

        self.logger.info("User '%s@%s' logged in." % (user.username, user.group.name))
        self._logged_users.append(user)

        connection.attached_user = user
        self.event_bus.post(PostUserLoginEvent(user))

    def log_out_user(self, user: User, connection: ServerConnection) -> None:
        assert isinstance(user, User), "Param 'user' must be of type User."

        if not user.is_logged_in():
            return

        if not user in self._users:
            raise LookupError("User is not known to the system.")

        if self.event_bus.post(PreUserLogOutEvent(user)):
            return
        user.group.log_out_user(user)

        self.logger.info("User '%s@%s' has logged out." % (user.username, user.group.name))
        if user in self._logged_users:
            self._logged_users.remove(user)

        connection.attached_user = None
        self.event_bus.post(PostUserLogOutEvent(user))

    def parse_username(self, username: str) -> Tuple[str, str]:
        if not re.fullmatch("[A-Za-z0-9_]+(@[A-Za-z0-9_]+)?", username):
            raise TypeError("Username %r is not in valid username format." % username)

        uname, group = re.split("@[A-Za-z0-9_]+", username)[0], re.split("^[A-Za-z0-9_]+", username)[1].lstrip("@")
        if not group: group = self.default_group.name

        return uname, group

    def get_group_by_name(self, group_name: str) -> Group:
        for group in self._groups:
            if group.name == group_name:
                return group

        for group in self._groups:
            if group_name in group.proxy_names:
                return group

        raise LookupError("Group with name '%s' not found." % group_name)

    def get_group_by_id(self, group_id: int) -> Group:
        for group in self._groups:
            if group.gid == group_id:
                return group

        raise LookupError("Group with id %i not found." % group_id)

    def group_exists(self, group_name: str) -> bool:
        try:
            self.get_group_by_name(group_name)
            return True
        except LookupError:
            return False

    def group_id_exists(self, group_id: int) -> bool:
        try:
            self.get_group_by_id(group_id)
            return True
        except LookupError:
            return False

    # -------------------- Registration methods -------------------- #

    def register_plugin(self, plugin: BasePlugin) -> None:
        # assert isinstance(plugin, BasePlugin), "Param 'plugin' must be of type BasePlugin."

        if not plugin in self._plugins:
            self._plugins.append(plugin)
            self.logger.debug("Registered plugin: %r." % plugin)

    def unregister_plugin(self, plugin: BasePlugin) -> None:
        # assert isinstance(plugin, BasePlugin), "Param 'plugin' must be of type BasePlugin."

        if plugin in self._plugins:
            self._plugins.remove(plugin)
            self.logger.debug("Unregistered plugin: %r." % plugin)

    def remove_async_updater(self, updater: GenericAsyncUpdater) -> None:
        # assert isinstance(updater, GenericAsyncUpdater), "Param 'updater' must be of type GenericAsyncUpdater."

        if not updater._exit:  # Even if it isn't an updater that has been registered we don't want excess threads
            updater.exit()

        for obj in self._async_updated_objects:
            if self._async_updated_objects[obj] == updater:
                del(self._async_updated_objects[obj])
                return

    def register_new_connection(self, conn: ServerConnection) -> None:
        # assert isinstance(conn, Connection), "Param 'conn' must be of type Connection."

        if not conn in self._connections:
            self._connections.append(conn)
            self.logger.debug("Connection '%r' registered." % conn)

    def unregister_connection(self, conn: ServerConnection) -> None:
        # assert isinstance(conn, Connection), "Param 'conn' must be of type Connection."

        if conn in self._connections:
            if self.is_async_updated(conn):
                try:
                    self.get_updater(conn).exit()
                except AttributeError as error:
                    print(repr(error))
            self._connections.remove(conn)
            self.logger.debug("Connection '%r' unregistered." % conn)

    @perm_check(permission=5)
    def register_new_server(self, server: Server) -> None:
        # assert isinstance(server, Server), "Param 'server' must be of type Server."

        if not server in self._servers:
            self._servers.append(server)
            self.logger.debug("Server '%r' registered." % server)

    @perm_check(permission=5)
    def unregister_server(self, server: Server) -> None:
        # assert isinstance(server, Server), "Param 'server' must be of type Server."

        if server in self._servers:
            if self.is_async_updated(server):
                self.get_updater(server).exit()
            self._servers.remove(server)
            self.logger.debug("Server '%r' unregistered." % server)

    def add_user(self, user: User) -> None:
        # assert isinstance(user, User), "Param 'user' must be of type User."
        assert not user in self._users, "User already registered."

        self._users.append(user)
        self.logger.debug("User '%r' added." % user)

    def remove_user(self, user: User) -> None:
        # assert isinstance(user, User), "Param 'user' must be of type User."
        assert user in self._users, "User does not exist."

        self._users.remove(user)
        self.logger.debug("User '%r' removed." % user)

    def add_group(self, group: Group) -> None:
        # assert isinstance(group, Group), "Param 'group' must be of type Group."
        assert not group in self._groups, "Group already registered."

        if self.group_exists(group.name):
            raise AssertionError("A group with that name is already registered.")

        if self.group_id_exists(group.gid):
            raise AssertionError("A group with that id is already registered.")

        self._groups.append(group)
        self.logger.debug("Group '%r' added." % group)

    def remove_group(self, group: Group) -> None:
        # assert isinstance(group, Group), "Param 'group' must be of type Group."
        assert group in self._groups, "Group does not exist."

        self._groups.remove(group)
        self.logger.debug("Group '%r' removed." % group)

    def register_packets(self, packets: List[Type[Packet]]) -> None:
        network.networking.packets.packets.extend(packets)

    # -------------------- Update -------------------- #

    @perm_check(permission=5)
    def update(self) -> None:
        self._event_bus.post(SystemUpdateEvent())

        for server in self._servers:
            if self.is_async_updated(server):
                try:
                    self.get_updater(server).mark_dirty()  # The lazy solution for concurrency issues
                except (AttributeError, KeyError) as error:
                    self.logger.debug("An error occurred while updating '%r' asynchronously: %r" % (server, error))
                continue

            server.on_update()

        for conn in self._connections:
            if self.is_async_updated(conn):
                try:
                    self.get_updater(conn).mark_dirty()
                except (AttributeError, KeyError) as error:
                    self.logger.debug("An error occurred while updating '%r' asynchronously: %r" % (conn, error))
                continue

            conn.on_update()

        if self._exit:
            self._do_safe_shutdown()
