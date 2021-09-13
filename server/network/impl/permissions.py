#!/usr/bin/env python3

import inspect
import threading
from threading import _MainThread  # Nice one PyCharm
from typing import Any, List

from network.impl.generic import GenericAsyncSystemObject
from network.impl.user import User


def perm_check(permission: int = 4):  # -> function:

    def check_permissions(*args: Any, _func, _permission: int = permission, **kwargs: Any) -> Any:
        user = PermissionChecker.INSTANCE.get_user()

        permission = 0
        if user is not None:
            permission = user.permission

        if permission < _permission:
            raise PermissionError("Insufficient permissions for function %r, (%i to %i)" % (_func, permission,
                                                                                            _permission))

        return _func(*args, **kwargs)

    # Not a very pretty solution
    return lambda func: lambda *args, **kwargs: check_permissions(*args, _func=func, **kwargs)


class PermissionChecker:

    INSTANCE = None

    def __init__(self, system) -> None:
        PermissionChecker.INSTANCE = self

        self.system = system

    def __repr__(self) -> str:
        return "PermissionChecker()"

    def _get_class_from_stack(self, stack: List[Any]) -> None:
        ...

    def is_system_associated(self, obj: Any) -> bool:
        if obj == self.system: return True
        elif isinstance(obj, _MainThread): return True
        else: return False

    def check_system_association(self) -> bool:  # TODO: PermissionChecker.check_system_association()
        current_thread = threading.current_thread()

        if self.is_system_associated(current_thread):
            return True

        for frame_info in inspect.stack():
            ...

    # This function will attempt to figure out the user based on current threads and analysing frames
    def get_user(self) -> User:  # TODO: PermissionChecker.get_user()
        current_thread = threading.current_thread()

        if isinstance(current_thread, _MainThread):
            return self.system.get_group_by_id(-1).get_user_by_name("main")
        elif isinstance(current_thread, GenericAsyncSystemObject):
            if hasattr(current_thread.generic_class, "attached_user"):
                if current_thread.generic_class.attached_user is not None:
                    return current_thread.generic_class.attached_user

        for frame_info in inspect.stack():
            print(frame_info)
