#!/usr/bin/env python3

# from builtins import function  # ???
from typing import List, Any

from . import Event


class EventBus:

    def __init__(self, system) -> None:
        self.system = system
        self._listeners = []

    def __repr__(self) -> str:
        return "EventHandler(listeners=%i)" % len(self._listeners)

    # -------------------- Post -------------------- #

    def post(self, event: Event) -> Event:
        # assert isinstance(event, Event), "Param 'event' must be of type Event."

        return self._post(event)

    def _post(self, event: Event) -> Event:
        for listener in reversed(sorted(self._listeners, key=lambda event_listener: event_listener.priority)):
            if listener.handles_event(event):
                event = listener.call_func(event)

        return event

    # -------------------- Event listeners -------------------- #

    def register(self, listener: object) -> None:
        # assert isinstance(listener, EventListener), "Param 'listener' must be of type EventListener."

        if not listener in self._listeners:
            self.system.logger.debug("Registering event listener: %r." % listener)

            self._listeners.append(listener)

    def unregister(self, listener: object) -> None:
        # assert isinstance(listener, EventListener), "Param 'listener' must be of type EventListener."

        if listener in self._listeners:
            self.system.logger.debug("Unregistering event listener: %r." % listener)

            self._listeners.remove(listener)

    # -------------------- System functions -------------------- #

    def update(self) -> None:
        ...


class EventListener:

    @property
    def func(self):  # -> function:
        return self._func

    @property
    def events(self) -> List[Event]:
        return self._events.copy()

    @property
    def priority(self) -> int:
        return self._priority

    def __init__(self, func, events: List[Event], priority: int) -> None:
        # assert isinstance(events, list), "Param 'events' must be of type list."

        self._func = func
        self._events = events.copy()
        self._priority = priority

    def __call__(self, *args: Any, **kwargs: Any) -> Any:
        return self.call_func(*args, **kwargs)

    def __repr__(self) -> str:
        return "EventListener(func=%r, events=%r)" % (self._func, self._events)

    def call_func(self, *args: Any, **kwargs: Any) -> Any:
        return self._func(*args, **kwargs)

    def handles_event(self, event: Event) -> bool:
        return event.__class__ in self._events
