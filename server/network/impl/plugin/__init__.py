#!/usr/bin/env python3

import inspect
import threading
import types
from typing import Iterable, Type

from network.impl.event import Event
from network.impl.event.bus import EventListener


def subscribe_event(events: Iterable[Type[Event]] = (), priority: int = 0):
    assert isinstance(events, list) or isinstance(events, tuple), "Param 'events' must be of type list or tuple."

    if isinstance(events, list):
        events = events.copy()

    return lambda func: (EventListener(func, list(events), priority))


class MetaPlugin(type):

    def __new__(cls, name: str, bases, body):  # -> BasePlugin:
        assert "NAME" in body, "Derived plugins from 'BasePlugin' must have field 'NAME'."
        assert "VERSION" in body, "Derived plugins from 'BasePlugin' must have field 'VERSION'."

        assert isinstance(body["NAME"], str), "Derived plugins from 'BasePlugin' must have field 'NAME' as str."
        assert isinstance(body["VERSION"], str) or isinstance(body["VERSION"], int) or isinstance(body["VERSION"], float), \
            "Derived plugins from 'BasePlugin' must have field 'VERSION' as str, int or float."

        return super().__new__(cls, name, bases, body)

    def __repr__(self) -> str:
        return "MetaPlugin()"


class BasePlugin(metaclass=MetaPlugin):

    NAME = "Base"
    VERSION = 0

    @property
    def thread(self) -> threading.Thread:
        return self._thread

    @thread.setter
    def thread(self, new_thread: threading.Thread) -> None:
        if new_thread is not None:
            assert isinstance(new_thread, threading.Thread), "Attribute 'thread' must be None or of type threading.Thread."

        self.system.kill_thread_with_optional_force(self._thread)  # We don't want excess threads
        self._thread = new_thread

    def __init__(self, system, thread: threading.Thread) -> None:
        if thread is not None:
            assert isinstance(thread, threading.Thread), "Param 'thread' must be None or of type threading.Thread."

        self.system = system

        self._thread = thread
        self._exit = False

        self._event_handles = []

        for member in inspect.getmembers(self):
            if isinstance(member[1], EventListener):
                event_listener = member[1]

                # Convert the stored class function to a bound method of this instance
                event_listener._func = types.MethodType(event_listener._func, self)

                for event in event_listener.events:
                    for handle in self._event_handles:
                        if handle.handles_event(event):
                            raise ValueError("Duplicate event handles for event %r in the plugin %r were found." %
                                             (event, self))
                self._event_handles.append(event_listener)

        self.system.register_plugin(self)

    def __repr__(self) -> str:
        return "BasePlugin(name=%s, version=%s)" % (self.NAME, self.VERSION)

    def load(self) -> None:  # Called after all plugins have been initialised (as well as system)
        for event_handle in self._event_handles:
            self.system.event_bus.register(event_handle)

        if self._thread is not None:
            self._thread.start()

    def unload(self) -> None:
        for event_handle in self._event_handles:
            self.system.event_bus.unregister(event_handle)

        if self._thread is not None and self._thread.is_alive():
            self.system.kill_thread_with_optional_force(self._thread)

        self.system.unregister_plugin(self)

    def exit(self) -> None:
        self._exit = True
        self.unload()

    """
    def subscribe_event(self, events=()):
        assert isinstance(events, list) or isinstance(events, tuple), "Param 'events' must be of type list or tuple."

        def decorator(func):
            if func is None:
                return

            for event in events:
                if event in self._event_handles:
                    raise ValueError("An event handle for event %r is already present in the plugin %r." % (event, self))

            self._event_handles.append(EventListener(func, (events if isinstance(events, list) else list(events))))

            return func

        return decorator
    """
