#!/usr/bin/env python3

from typing import List


class User:
    """
    A clientside representation of a user.
    """

    @property
    def username(self) -> str:
        return self._username

    @property
    def user_id(self) -> int:
        """
        :return: The group assigned ID of the user.
        """

        return self._user_id

    @property
    def uid(self) -> int:
        """
        :return: The group assigned ID of the user.
        """

        return self._user_id

    @property
    def group(self):  # -> Group
        """
        :return: The group that the user belongs to.
        """

        return self._group

    @property
    def permissions(self) -> List:  # -> List[Permission]
        return self._permissions.copy()

    def __init__(self, username: str, user_id: int, group, permissions: List) -> None:
        self._username = username
        self._user_id = user_id
        self._group = group
        self._permissions = permissions.copy()

    def __repr__(self) -> str:
        return "User(username=%s, uid=%i)" % (self._username, self._user_id)
