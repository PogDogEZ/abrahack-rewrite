#!/usr/bin/env python3


class User:

    @property
    def username(self) -> str:
        return self._username

    @property
    def user_id(self) -> int:
        return self._user_id

    @property
    def uid(self) -> int:
        return self._user_id

    @property
    def permission(self) -> int:
        return self._permission

    @property
    def group(self):  # -> Group
        return self._group

    def __init__(self, username: str, user_id: int, permission: int, group) -> None:
        self._username = username
        self._user_id = user_id
        self._permission = permission
        self._group = group

    def __repr__(self) -> str:
        return "User(username=%r, uid=%i)" % (self._username, self._user_id)


class Group:

    @property
    def name(self) -> str:
        return self._name

    @property
    def group_id(self) -> int:
        return self._group_id

    @property
    def gid(self) -> int:
        return self._group_id

    @property
    def default_permission(self) -> int:
        return self._default_permission

    def __init__(self, name: str, group_id: int, default_permission: int) -> None:
        self._name = name
        self._group_id = group_id
        self._default_permission = default_permission

    def __repr__(self) -> str:
        return "Group(name=%r, gid=%i)" % (self._name, self._group_id)
