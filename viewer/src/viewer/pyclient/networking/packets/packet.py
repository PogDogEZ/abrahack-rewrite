#!/usr/bin/env python3

"""
Defines base Packet class and SideEnum.
"""

from typing import IO

from ..types import Enum


class Side(Enum):
    """
    The side that the packet should be expected from.
    """

    SERVER = 0
    CLIENT = 1
    BOTH = 2


class Packet:

    # Packets constants that should always be overridden
    ID = -1
    NAME = "base"
    SIDE = Side.BOTH  # The side of the packet (server(0), client(1) or both(2))

    def __repr__(self) -> str:
        return "Packet(name=%r, id=%i)" % (self.NAME, self.ID)

    def read(self, fileobj: IO) -> None:
        """
        Reads a packet from a provided buffer, data is stored in this class.

        :param fileobj: The buffer to read from.
        """

        ...

    def write(self, fileobj: IO) -> None:
        """
        Writes this packet to a provided buffer, data stored in this class is written.

        :param fileobj: The buffer to write into.
        """

        ...
