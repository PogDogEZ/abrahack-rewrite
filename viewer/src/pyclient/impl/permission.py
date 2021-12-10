#!/usr/bin/env python3


class Permission:
    """
    A permission for a user, contains the name and level of the permission.
    """

    @property
    def name(self) -> str:
        return self._name

    @property
    def level(self) -> int:
        return self._level

    def __init__(self, name: str, level: int) -> None:
        self._name = name
        self._level = level

    def __repr__(self) -> str:
        return "Permission(name=%s, level=%i)" % (self._name, self._level)
