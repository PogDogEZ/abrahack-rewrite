#!/usr/bin/env python3

"""
Defines base Packet class and SideEnum.
"""

from typing import IO

from pclient.networking.types import Enum


class Side(Enum):
    NONE = 0
    SERVER = 1
    CLIENT = 2
    BOTH = 3


class MetaPacket(type):

    def __new__(cls, name: str, bases, body):
        assert "ID" in body, "Derived packets from 'Packet' must have field 'ID'."
        assert "NAME" in body, "Derived packets from 'Packet' must have field 'NAME'."
        assert "SIDE" in body, "Derived packets from 'Packet' must have field 'SIDE'."

        assert "read" in body, "Derived packets from 'Packet' must define method 'read'."
        assert "write" in body, "Derived packets from 'Packet' must define method 'write'."

        assert isinstance(body["ID"], int), "Derived packets from 'Packet' must have field 'ID' of type int."
        assert isinstance(body["NAME"], str), "Derived packets from 'Packet' must have field 'NAME' of type str."
        assert isinstance(body["SIDE"], int), \
            "Derived packets from 'Packet' must have field 'SIDE' of type SideEnum."

        return super().__new__(cls, name, bases, body)

    def __repr__(self) -> str:
        return "MetaPacket()"


class Packet(metaclass=MetaPacket):

    ID = -1
    NAME = "base"
    SIDE = Side.NONE  # The side of the packet (server(0), client(1) or both(2))

    def __init__(self) -> None:
        ...

    def __repr__(self) -> str:
        return "Packet(name=%r, id=%i)" % (self.NAME, self.ID)

    def read(self, fileobj: IO) -> None:
        ...

    def write(self, fileobj: IO) -> None:
        ...
