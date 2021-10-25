#!/usr/bin/env python3
from typing import List

from . import Group, User


class SystemUsersGroup(Group):

    def __init__(self, system) -> None:
        super().__init__("systgroup", ["syst", "system"], -1, 5, system)

        self.add_user(User("main", "", True, 5, -3, self, self.system))
        self.add_user(User("system", "", True, 5, -2, self, self.system))
        self.add_user(User("pyshell", "", True, 5, -1, self, self.system))

    def __repr__(self) -> str:
        return "SystemUsersGroup(name=%r, gid=%i)" % (self._group_name, self._group_id)

    def authenticate(self, user: User, username: str, password: str) -> bool:  # TODO: Overriden function doesn't return bool
        if not self.system._permission_checker.check_system_association():
            raise PermissionError("Unable to log in to system associated account.")
        return True

    def log_out_user(self, user: User) -> None:
        ...


class UsernameOnlyGroup(Group):

    def __init__(self, group_name: str, proxy_names: List[str], group_id: int, default_permission: int, system) -> None:
        super().__init__(group_name, proxy_names, group_id, default_permission, system)

    def __repr__(self) -> str:
        return "UsernameOnlyGroup(name=%r, gid=%i)" % (self._group_name, self._group_id)

    def authenticate(self, user: User, username: str, password: str) -> bool:
        ...

    def log_out_user(self, user: User) -> None:
        super().log_out_user(user)

        if user in self._users:
            self._users.remove(user)

        self.system.remove_user(user)


class PasswordOnlyGroup(Group):

    def __init__(self, group_name: str, proxy_names: List[str], group_id: int, default_permission: int, system,
                 password: str) -> None:
        super().__init__(group_name, proxy_names, group_id, default_permission, system)

        self._password = password

    def __repr__(self) -> str:
        return "PasswordOnlyGroup(name=%r, gid=%i)" % (self._group_name, self._group_id)

    def authenticate(self, user: User, username: str, password: str) -> bool:
        if password != self._password:
            raise PermissionError("Incorrect password.")

        return super().authenticate(user, username, "")

    def log_out_user(self, user: User) -> None:
        super().log_out_user(user)

        if user in self._users:
            self._users.remove(user)

        self.system.remove_user(user)


class TestGroup(Group):

    def __init__(self, group_name: str, proxy_names: List[str], group_id: int, default_permission: int, system) -> None:
        super().__init__(group_name, proxy_names, group_id, default_permission, system)

        self.add_user(User("iska", "t", False, 4, 0, self, self.system))  # FIXME: This is dumb (duh) make this work properly
        self.add_user(User("node", "t", False, 4, 1, self, self.system))
        self.add_user(User("nathan", "ilovenode", False, 4, 2, self, self.system))
        self.add_user(User("arzi", "ilovenode", False, 4, 3, self, self.system))
        self.add_user(User("yescom", "fucknerdsinc", False, 4, 4, self, self.system))

        for user in self._users:
            self.system.add_user(user)

    def __repr__(self) -> str:
        return "TestGroup(name=%r, gid=%i)" % (self._group_name, self._group_id)
