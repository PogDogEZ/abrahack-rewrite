#!/usr/bin/env python3

from typing import IO

from . import Type
from .basic import String, Integer, Boolean, UnsignedShort
from ...impl.group import Group
from ...impl.permission import Permission
from ...impl.user import User


class PermissionType(Type):

    @classmethod
    def read(cls, fileobj: IO) -> Permission:
        name = String.read(fileobj)
        level = UnsignedShort.read(fileobj)

        return Permission(name, level)

    @classmethod
    def write(cls, permission: Permission, fileobj: IO) -> None:
        String.write(permission.name, fileobj)
        UnsignedShort.write(permission.level, fileobj)


class GroupType(Type):

    @classmethod
    def read(cls, fileobj: IO) -> Group:
        group_name = String.read(fileobj)
        group_id = Integer.read(fileobj)

        permissions = []
        permissions_to_read = UnsignedShort.read(fileobj)
        for index in range(permissions_to_read):
            permissions.append(PermissionType.read(fileobj))

        return Group(group_name, group_id, permissions)

    @classmethod
    def write(cls, group: Group, fileobj: IO) -> None:
        String.write(group.name, fileobj)
        Integer.write(group.group_id, fileobj)

        UnsignedShort.write(len(group.permissions), fileobj)
        for permission in group.permissions:
            PermissionType.write(permission, fileobj)


class UserType(Type):

    @classmethod
    def read(cls, fileobj: IO) -> User:
        username = String.read(fileobj)
        user_id = Integer.read(fileobj)

        permissions = []
        permissions_to_read = UnsignedShort.read(fileobj)
        for index in range(permissions_to_read):
            permissions.append(PermissionType.read(fileobj))

        if Boolean.read(fileobj):
            group = GroupType.read(fileobj)
        else:
            group = None

        return User(username, user_id, group, permissions)

    @classmethod
    def write(cls, user: User, fileobj: IO) -> None:
        String.write(user.username, fileobj)
        Integer.write(user.user_id, fileobj)

        UnsignedShort.write(len(user.permissions), fileobj)
        for permission in user.permissions:
            PermissionType.write(permission, fileobj)

        if user.group is not None:
            Boolean.write(True, fileobj)
            GroupType.write(user.group, fileobj)
        else:
            Boolean.write(False, fileobj)
