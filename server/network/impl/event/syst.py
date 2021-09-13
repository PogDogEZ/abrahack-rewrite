#!/usr/bin/env python3

from . import Event
from ..user import User


class SystemUpdateEvent(Event):

    def __init__(self) -> None:
        super().__init__()

    def __repr__(self) -> str:
        return "SystemUpdateEvent()"


class PreUserLoginEvent(Event):

    is_cancellable = True

    def __init__(self, user: User) -> None:
        super().__init__()

        self._user = user

    def __repr__(self) -> str:
        return "PreUserLoginEvent(user=%r)" % self._user

    def get_user(self) -> User:
        return self._user


class PostUserLoginEvent(Event):

    def __init__(self, user: User) -> None:
        super().__init__()

        self._user = user

    def __repr__(self) -> str:
        return "PostUserLoginEvent(user=%r)" % self._user

    def get_user(self) -> User:
        return self._user


class PreUserLogOutEvent(Event):

    is_cancellable = True

    def __init__(self, user: User) -> None:
        super().__init__()

        self._user = user

    def __repr__(self) -> str:
        return "PreUserLogOutEvent(user=%r)" % self._user

    def get_user(self) -> User:
        return self._user


class PostUserLogOutEvent(Event):

    def __init__(self, user: User) -> None:
        super().__init__()

        self._user = user

    def __repr__(self) -> str:
        return "PostUserLogOutEvent(user=%r)" % self._user

    def get_user(self) -> User:
        return self._user
