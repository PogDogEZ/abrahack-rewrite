#!/usr/bin/env python3

"""
The user classes for the network project.
"""

from typing import List

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives.hashes import Hash, SHA256

from network.impl.generic import GenericSystemObject


class User(GenericSystemObject):

    _username = "none"
    __password = "none"
    _permission = 0
    _user_id = -1

    @property
    def username(self) -> str:
        return self._username

    @property
    def permission(self) -> int:
        return self._permission

    @property
    def user_id(self) -> int:
        return self._user_id

    @property
    def log_id(self) -> int:
        return self._log_id

    def __init__(self, username: str, password: str, is_password_hashed: bool,
                 permission: int, user_id: int, group, system) -> None:
        super().__init__(system)

        if group.username_exists(username) or group.user_id_exists(user_id):
            raise ValueError("The attempted creation of a user with a duplicate uid and/or uname was performed.")

        self._username = username
        self.__password = password
        self._is_password_hashed = is_password_hashed
        self._permission = permission
        self._user_id = user_id

        self.group = group
        self.group.add_user(self)

        self._logged_in = False
        self._log_id = -1

    def __repr__(self) -> str:
        return "User(username='%s', uid=%i)" % (self._username, self._user_id)

    def __str__(self) -> str:
        return "%s@%s(%i:%i)" % (self._username, self.group.name, self._user_id, self.group.gid)

    def is_logged_in(self) -> bool:
        return self._logged_in

    def login(self, password: str) -> None:
        if self.is_logged_in():
            raise PermissionError("User is already logged in.")

        if self._is_password_hashed:
            digest = Hash(SHA256(), default_backend())
            digest.update(password.encode())
            password = hex(int().from_bytes(digest.finalize(), "big"))[2:].upper()

        if password != (self.__password.upper() if self._is_password_hashed else self.__password):
            raise PermissionError("Invalid password for user.")

        self._logged_in = True

    def log_out(self) -> None:
        if not self.is_logged_in():
            raise PermissionError("User is not logged in.")

        self._logged_in = False


class Group(GenericSystemObject):

    _group_name = "none"
    _proxy_names = []
    _group_id = -1
    _default_permission = 0

    @property
    def name(self) -> str:
        return self._group_name

    @property
    def proxy_names(self) -> List[str]:
        return self._proxy_names.copy()

    @property
    def gid(self) -> int:
        return self._group_id

    @property
    def default_permission(self) -> int:
        return self._default_permission

    def __init__(self, group_name: str, proxy_names: List[str], group_id: int, default_permission: int, system) -> None:
        super().__init__(system)

        self._group_name = group_name
        self._proxy_names = proxy_names.copy()
        self._group_id = group_id
        self._default_permission = default_permission

        self._users = []

    def __repr__(self) -> str:
        return "Group(name=%r, gid=%i)" % (self._group_name, self._group_id)

    def get_users(self) -> List[User]:
        return self._users.copy()

    def add_user(self, user: User) -> None:
        # assert isinstance(user, User), "Param 'user' must be of type User."

        if not user in self._users:
            self._users.append(user)

    def get_user_by_name(self, username: str) -> User:
        for user in self._users:
            if user.username == username:
                return user

        raise LookupError("User by name '%s' not found." % username)

    def get_user_by_id(self, uid: int) -> User:
        for user in self._users:
            if user.user_id == uid:
                return user

        raise LookupError("User by uid %i not found." % uid)

    def username_exists(self, username: str) -> bool:
        try:  # I know it's bad to do this but I'm lazy
            self.get_user_by_name(username)
            return True
        except LookupError:
            return False

    def user_id_exists(self, uid: int) -> bool:
        try:
            self.get_user_by_id(uid)
            return True
        except LookupError:
            return False

    def authenticate(self, user: User, username: str, password: str):
        assert user in self._users, "User is not part of this group."

        if username == user.username:
            user.login(password)

    def log_out_user(self, user: User) -> None:
        user.log_out()

    def get_next_uid(self) -> int:
        current_uid = 0

        for user in self._users:
            if user.user_id > current_uid:
                current_uid = user.user_id

        return current_uid + 1
