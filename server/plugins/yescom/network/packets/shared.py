#!/usr/bin/env python3

from typing import IO

from network.networking.packets import Packet, Side
from network.networking.types.basic import Boolean, String, UnsignedShort

ID_OFFSET = 255


class YCInitRequestPacket(Packet):

    ID = ID_OFFSET
    NAME = "yc_init_request"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.handler_name = ""
        self.host_name = ""
        self.host_port = 25565
        self.listening = False

    def read(self, fileobj: IO) -> None:
        self.handler_name = String.read(fileobj)
        self.host_name = String.read(fileobj)
        self.host_port = UnsignedShort.read(fileobj)
        self.listening = Boolean.read(fileobj)

    def write(self, fileobj: IO) -> None:
        String.write(self.handler_name, fileobj)
        String.write(self.host_name, fileobj)
        UnsignedShort.write(self.host_port, fileobj)
        Boolean.write(self.listening, fileobj)


class YCInitResponsePacket(Packet):

    ID = ID_OFFSET + 1
    NAME = "yc_init_response"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.rejected = False
        self.handler_id = 0
        self.message = ""
        self.host_name = ""
        self.host_port = 0

    def read(self, fileobj: IO) -> None:
        self.rejected = Boolean.read(fileobj)
        self.handler_id = UnsignedShort.read(fileobj)
        self.message = String.read(fileobj)
        if not self.rejected:
            self.host_name = String.read(fileobj)
            self.host_port = UnsignedShort.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Boolean.write(self.rejected, fileobj)
        UnsignedShort.write(self.handler_id, fileobj)
        String.write(self.message, fileobj)
        if not self.rejected:
            String.write(self.host_name, fileobj)
            UnsignedShort.write(self.host_port, fileobj)
