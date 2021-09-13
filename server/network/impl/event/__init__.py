#!/usr/bin/env python3

class Event:

    is_cancellable = False
    _cancelled = False

    def __init__(self) -> None:
        ...

    def __repr__(self) -> str:
        return "Event()"

    def __bool__(self) -> bool:
        return self.get_cancelled()

    def set_cancelled(self, cancelled: bool) -> None:
        if cancelled and not self.is_cancellable:
            raise AttributeError("Event %r is not cancellable." % self)

        self._cancelled = cancelled

    def get_cancelled(self) -> bool:
        return self._cancelled
