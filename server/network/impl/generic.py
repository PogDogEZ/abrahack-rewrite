#!/usr/bin/env python3

import threading
import time
from typing import Any


class GenericSystemObject:

    def __init__(self, system) -> None:
        self.system = system

    def __repr__(self) -> str:
        return "GenericSystemObject()"


class GenericAsyncSystemObject(threading.Thread, GenericSystemObject):

    @property
    def generic_class(self) -> Any:
        return self._generic_class

    def __init__(self, generic_class: Any, system) -> None:
        super().__init__()

        self.system = system
        self._generic_class = generic_class

        self._exit = False

    def __repr__(self) -> str:
        return "GenericAsyncSystemObject()"

    def getName(self) -> str:
        return self.__repr__()

    def exit(self) -> None:
        self._exit = True

    def run(self) -> None:
        ...


class GenericAsyncUpdater(GenericAsyncSystemObject):

    @property
    def sleep_time(self) -> float:
        return self._sleep_time

    @sleep_time.setter
    def sleep_time(self, value: float) -> None:
        self._sleep_time = value

    def __init__(self, generic_class: Any, system, sleep_time: float = 0.005) -> None:
        super().__init__(generic_class, system)

        self._sleep_time = sleep_time
        self._queued_update = False

    def __repr__(self) -> str:
        return "GenericAsyncUpdater(generic_class=%r)" % self.generic_class

    def exit(self) -> None:
        self._exit = True
        self.system.remove_async_updater(self)

    def mark_dirty(self) -> None:
        self._queued_update = True

    def run(self) -> None:
        while not self.system.exit and not self._exit:
            start = time.time()
            if self._queued_update:
                # Fixed issue with waiting for next update twice, assuming the generic class' update takes too much time
                self._queued_update = False
                self._generic_class.on_update()

            time_taken = time.time() - start
            if time_taken < self._sleep_time:
                time.sleep(self._sleep_time - time_taken)
