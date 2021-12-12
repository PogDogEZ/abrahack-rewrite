#!/usr/bin/env python3
from typing import List


class Group:
    """
    A clientside representation of a group.
    """

    @property
    def name(self) -> str:
        return self._name

    @property
    def group_id(self) -> int:
        return self._group_id

    @property
    def permissions(self) -> List:  # -> List[Permission]:
        return self._permissions.copy()

    def __init__(self, name: str, group_id: int, permissions: List) -> None:
        self._name = name
        self._group_id = group_id
        self._permissions = permissions.copy()

    def __repr__(self) -> str:
        return "Group(name=%r, gid=%i)" % (self._name, self._group_id)
