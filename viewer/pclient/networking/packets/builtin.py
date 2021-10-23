#!/usr/bin/env python3

from typing import IO, List, Tuple

from pclient.networking.packets.packet import Packet, Side, MetaPacket
from pclient.networking.types.basic import UnsignedShort, Integer, String, Boolean, VarInt, Bytes
from pclient.networking.types.enum import EncryptionType
from pclient.networking.types.extended import PacketSpec, UserType


class ServerInfoPacket(Packet):

    ID = 0
    NAME = "server_info"
    SIDE = Side.SERVER

    @property
    def bitmask(self) -> int:
        return self._bitmask

    def __init__(self) -> None:
        super().__init__()

        self.server_name = ""
        self.protocol_ver = 2
        self.cipher = EncryptionType.NONE
        self.compression_threshold = 0
        self._bitmask = 0

    def read(self, fileobj: IO) -> None:
        self.server_name = String.read(fileobj)
        self.protocol_ver = UnsignedShort.read(fileobj)
        self.cipher = EncryptionType.read(fileobj)
        self.compression_threshold = Integer.read(fileobj)
        self._bitmask = fileobj.read(1)[0]

    def write(self, fileobj: IO) -> None:
        String.write(self.server_name, fileobj)
        UnsignedShort.write(self.protocol_ver, fileobj)
        EncryptionType.write(self.cipher, fileobj)
        Integer.write(self.compression_threshold, fileobj)
        fileobj.write(bytes([self._bitmask]))

    def set_bit(self, bit: int) -> None:
        self._bitmask |= 2**(bit - 1)

    def is_bit_set(self, bit: int) -> bool:
        return bool(self._bitmask & (2 ** (bit - 1)))

    def set_compression_bit(self) -> None:
        self.set_bit(1)

    def compression(self) -> bool:
        return self.is_bit_set(1)

    def set_encryption_bit(self) -> None:
        self.set_bit(2)

    def encryption(self) -> bool:
        return self.is_bit_set(2)

    def set_authentication_bit(self) -> None:
        self.set_bit(3)

    def authentication(self) -> bool:
        return self.is_bit_set(3)


class StartEncryptionPacket(Packet):

    ID = 1
    NAME = "encryption_request"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.a_peer_public_key = b""
        self.key_size = 512
        self.param_g = 0
        self.param_p = 0

    def read(self, fileobj: IO) -> None:
        self.a_peer_public_key = Bytes.read(fileobj)
        self.key_size = UnsignedShort.read(fileobj)
        self.param_g = VarInt.read(fileobj)
        self.param_p = VarInt.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Bytes.write(self.a_peer_public_key, fileobj)
        UnsignedShort.write(self.key_size, fileobj)
        VarInt.write(self.param_g, fileobj)
        VarInt.write(self.param_p, fileobj)


class EncryptionResponsePacket(Packet):

    ID = 2
    NAME = "encryption_response"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.b_peer_public_key = b""
        self.iv = b""  # Optional

    def read(self, fileobj: IO) -> None:
        self.b_peer_public_key = Bytes.read(fileobj)
        self.iv = Bytes.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Bytes.write(self.b_peer_public_key, fileobj)
        Bytes.write(self.iv, fileobj)


class ClientCapabilitiesPacket(Packet):

    ID = 3
    NAME = "client_capabilities"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self._packets = []

    def read(self, fileobj: IO) -> None:
        self._packets.clear()

        entries = UnsignedShort.read(fileobj)
        for index in range(entries):
            packet_id = UnsignedShort.read(fileobj)
            packet_name = String.read(fileobj)
            packet_side = Side.read(fileobj)

            self._packets.append((packet_id, packet_name, packet_side))

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(len(self._packets), fileobj)

        for packet in self._packets:
            UnsignedShort.write(packet[0], fileobj)
            String.write(packet[1], fileobj)
            Side.write(packet[2], fileobj)

    def get_packets(self) -> List[Tuple[int, str, Side]]:
        return self._packets.copy()

    def set_packets(self, packets: List[Tuple[int, str, Side]]) -> None:
        self._packets.clear()
        self._packets.extend(packets)

    def extend_packets(self, packets: List[Tuple[int, str, Side]]) -> None:
        self._packets.extend(packets)

    def add_packet(self, packet: Tuple[int, str, Side]) -> None:
        if not packet in self._packets:
            self._packets.append(packet)


class ClientCapabilitiesResponsePacket(Packet):  # TODO: More verbose on conflicts

    ID = 4
    NAME = "client_capabilities_response"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.rejected = False

    def read(self, fileobj: IO) -> None:
        self.rejected = Boolean.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Boolean.write(self.rejected, fileobj)


class LoginPacket(Packet):

    ID = 5
    NAME = "login_request"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.user = None
        self.password = ""

    def read(self, fileobj: IO) -> None:
        if Boolean.read(fileobj):
            self.user = UserType.read(fileobj)
        else:
            self.user = None

        self.password = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        if self.user is not None:
            Boolean.write(True, fileobj)
            UserType.write(self.user, fileobj)
        else:
            Boolean.write(False, fileobj)

        String.write(self.password, fileobj)


class LoginResponsePacket(Packet):

    ID = 6
    NAME = "login_response"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.successful_authentication = False
        self.message = "No additional info."

    def read(self, fileobj: IO) -> None:
        self.successful_authentication = Boolean.read(fileobj)
        self.message = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Boolean.write(self.successful_authentication, fileobj)
        String.write(self.message, fileobj)


class ConnectionInfoRequestPacket(Packet):

    ID = 7
    NAME = "connection_info_request"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

    def read(self, fileobj: IO) -> None:
        ...

    def write(self, fileobj: IO) -> None:
        ...


class ConnectionInfoResponsePacket(Packet):

    ID = 8
    NAME = "connection_info_response"
    SIDE = Side.SERVER

    @property
    def ping(self) -> int:
        return self._ping

    @ping.setter
    def ping(self, value: int) -> None:
        # assert isinstance(value, int), "Attribute 'ping' must be of type int."

        self._ping = value

    @property
    def user(self) -> str:
        return self._user

    @user.setter
    def user(self, value: str) -> None:
        # assert isinstance(value, str), "Attribute 'user' must be of type str."

        self._user = value

    @property
    def host(self) -> str:
        return self._host

    @host.setter
    def host(self, value: str) -> None:
        # assert isinstance(value, str), "Attribute 'host' must be of type str."

        self._host = value

    @property
    def port(self) -> int:
        return self._port

    @port.setter
    def port(self, value: int) -> None:
        # assert isinstance(value, int), "Attribute 'port' must be of type int."

        self._port = value

    def __init__(self) -> None:
        super().__init__()

        self._ping = 0
        self._user = ""
        self._host = ""
        self._port = 0

    def read(self, fileobj: IO) -> None:
        self._ping = UnsignedShort.read(fileobj)
        self._user = String.read(fileobj)
        self._host = String.read(fileobj)
        self._port = Integer.read(fileobj)

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(self._ping, fileobj)
        String.write(self._user, fileobj)
        String.write(self._host, fileobj)
        Integer.write(self._port, fileobj)


class PrintPacket(Packet):

    ID = 9
    NAME = "print"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.channel = 0
        self.message = ""

    def read(self, fileobj: IO) -> None:
        self.channel = UnsignedShort.read(fileobj)
        self.message = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(self.channel, fileobj)
        String.write(self.message, fileobj)


class InputPacket(Packet):

    ID = 10
    NAME = "input"
    SIDE = Side.SERVER

    def __init__(self):
        super().__init__()

        self.prompt = ""

    def read(self, fileobj: IO) -> None:
        self.prompt = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        String.write(self.prompt, fileobj)


class InputResponsePacket(Packet):

    ID = 11
    NAME = "input_response"
    SIDE = Side.CLIENT

    def __init__(self) -> None:
        super().__init__()

        self.response = ""

    def read(self, fileobj: IO) -> None:
        self.response = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        String.write(self.response, fileobj)


class KeepAlivePacket(Packet):

    ID = 12
    NAME = "keep_alive"
    SIDE = Side.SERVER

    def __init__(self) -> None:
        super().__init__()

        self.keep_alive_id = 0

    def read(self, fileobj: IO) -> None:
        self.keep_alive_id = VarInt.read(fileobj)

    def write(self, fileobj: IO) -> None:
        VarInt.write(self.keep_alive_id, fileobj)


class KeepAliveResponsePacket(KeepAlivePacket):

    ID = 13
    NAME = "keep_alive_response"
    SIDE = Side.CLIENT

    def read(self, fileobj: IO) -> None:
        super().read(fileobj)

    def write(self, fileobj: IO) -> None:
        super().write(fileobj)


class DisconnectPacket(Packet):

    ID = 14
    NAME = "disconnect"
    SIDE = Side.BOTH

    def __init__(self) -> None:
        super().__init__()

        self.message = "Generic Disconnect"

    def read(self, fileobj: IO) -> None:
        self.message = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        String.write(self.message, fileobj)
