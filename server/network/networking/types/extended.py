#!/usr/bin/env python3
import typing
from typing import IO

from . import Type
from .basic import String, Short, Integer, UnsignedShort, Boolean
from .. import packets
from ..packets.packet import Packet, Side
from ...impl.user import User, Group


class PacketSpec(Type):

    @classmethod
    def read(cls, fileobj: IO) -> typing.Type[Packet]:
        packet_id = UnsignedShort.read(fileobj)
        packet_name = String.read(fileobj)
        packet_side = Side.read(fileobj)

        for packet in packets.packets:
            if packet.ID == packet_id and packet.NAME == packet_name and packet.SIDE == packet_side:
                return packet

        return packet_id, packet_name, packet_side  # Kinda hacky but whatever

    @classmethod
    def write(cls, packet_type: typing.Type[Packet], fileobj: IO) -> None:
        UnsignedShort.write(packet_type.ID, fileobj)
        String.write(packet_type.NAME, fileobj)
        Side.write(packet_type.SIDE, fileobj)


class UserType(Type):

    @classmethod
    def read(cls, fileobj: IO) -> User:
        username = String.read(fileobj)
        user_id = Integer.read(fileobj)
        permission = Short.read(fileobj)

        if Boolean.read(fileobj):
            group = GroupType.read(fileobj)
        else:
            group = None

        # This acts as more of a placeholder so we don't need to give it the system, or anything else
        return User(username, "", False, permission, user_id, group, None)

    @classmethod
    def write(cls, user: User, fileobj: IO) -> None:
        String.write(user.username, fileobj)
        Integer.write(user.user_id, fileobj)
        Short.write(user.permission, fileobj)

        if user.group is not None:
            Boolean.write(True, fileobj)
            GroupType.write(user.group, fileobj)
        else:
            Boolean.write(False, fileobj)


class GroupType(Type):

    @classmethod
    def read(cls, fileobj: IO) -> Group:
        group_name = String.read(fileobj)
        group_id = Integer.read(fileobj)
        default_permission = Short.read(fileobj)

        return Group(group_name, [], group_id, default_permission, None)

    @classmethod
    def write(cls, group: Group, fileobj: IO) -> None:
        String.write(group.name, fileobj)
        Integer.write(group.gid, fileobj)
        Short.write(group.default_permission, fileobj)
