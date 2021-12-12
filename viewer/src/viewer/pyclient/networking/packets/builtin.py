#!/usr/bin/env python3

from typing import IO, List, Tuple

from .packet import Packet, Side
from ..types.basic import UnsignedShort, Integer, String, Boolean, VarInt, Bytes
from ..types.enum import EncryptionType
from ..types.extended import UserType
from ...impl.user import User


class ServerInfoPacket(Packet):
    """
    Server info packet. This is sent by the server when the client first connects to it.
    """

    ID = 0
    NAME = "server_info"
    SIDE = Side.SERVER

    _COMPRESSION_BIT = 1
    _ENCRYPTION_BIT = 2
    _AUTHENTICATION_BIT = 3

    @property
    def compression(self) -> bool:
        return self._is_bit_set(self._COMPRESSION_BIT)

    @compression.setter
    def compression(self, compression: bool) -> None:
        self._set_bit(self._COMPRESSION_BIT, compression)

    @property
    def encryption(self) -> bool:
        return self._is_bit_set(self._ENCRYPTION_BIT)

    @encryption.setter
    def encryption(self, encryption: bool) -> None:
        self._set_bit(self._ENCRYPTION_BIT, encryption)

    @property
    def authentication(self) -> bool:
        return self._is_bit_set(self._AUTHENTICATION_BIT)

    @authentication.setter
    def authentication(self, authentication: bool) -> None:
        self._set_bit(self._AUTHENTICATION_BIT, authentication)

    def __init__(self, server_name: str = "", protocol_ver: int = 2,
                 encryption_type: EncryptionType = EncryptionType.NONE,
                 compression_threshold: int = 0) -> None:
        super().__init__()

        self.server_name = server_name
        self.protocol_ver = protocol_ver
        self.encryption_type = encryption_type
        self.compression_threshold = compression_threshold
        self._bitmask = 0

    def read(self, fileobj: IO) -> None:
        self.server_name = String.read(fileobj)
        self.protocol_ver = UnsignedShort.read(fileobj)
        self.encryption_type = EncryptionType.read(fileobj)
        self.compression_threshold = Integer.read(fileobj)
        self._bitmask = fileobj.read(1)[0]

    def write(self, fileobj: IO) -> None:
        String.write(self.server_name, fileobj)
        UnsignedShort.write(self.protocol_ver, fileobj)
        # noinspection PyTypeChecker
        EncryptionType.write(self.encryption_type, fileobj)
        Integer.write(self.compression_threshold, fileobj)
        fileobj.write(bytes([self._bitmask]))

    def _set_bit(self, bit: int, value: bool) -> None:
        if value:
            self._bitmask |= 2 ** (bit - 1)
        else:
            self._bitmask &= ~(2 ** (bit - 1))

    def _is_bit_set(self, bit: int) -> bool:
        return bool(self._bitmask & (2 ** (bit - 1)))


class EncryptionRequestPacket(Packet):
    """
    Sent by the client to begin the encryption key exchange.
    """

    ID = 1
    NAME = "encryption_request"
    SIDE = Side.CLIENT

    def __init__(self, a_peer_public_key: bytes = b"", key_size: int = 512, param_g: int = 0, param_p: int = 0) -> None:
        super().__init__()

        self.a_peer_public_key = a_peer_public_key
        self.key_size = key_size
        self.param_g = param_g
        self.param_p = param_p

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
    """
    Sent by the server to finalize the encryption key exchange.
    """

    ID = 2
    NAME = "encryption_response"
    SIDE = Side.SERVER

    def __init__(self, b_peer_public_key: bytes = b"", init_vector: bytes = b"") -> None:
        super().__init__()

        self.b_peer_public_key = b_peer_public_key
        self.init_vector = init_vector  # Optional

    def read(self, fileobj: IO) -> None:
        self.b_peer_public_key = Bytes.read(fileobj)
        self.init_vector = Bytes.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Bytes.write(self.b_peer_public_key, fileobj)
        Bytes.write(self.init_vector, fileobj)


class ClientCapabilitiesPacket(Packet):
    """
    Sent by the client to indicate to the server which packets it accepts.
    """

    ID = 3
    NAME = "client_capabilities"
    SIDE = Side.CLIENT

    def __init__(self, packets: List[Tuple[int, str, Side]] = None) -> None:
        super().__init__()

        self._packets = packets.copy() if packets is not None else []

    def read(self, fileobj: IO) -> None:
        self._packets.clear()

        packets_to_read = UnsignedShort.read(fileobj)
        for index in range(packets_to_read):
            packet_id = UnsignedShort.read(fileobj)
            packet_name = String.read(fileobj)
            packet_side = Side.read(fileobj)

            # noinspection PyTypeChecker
            self._packets.append((packet_id, packet_name, packet_side))

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(len(self._packets), fileobj)

        for packet in self._packets:
            UnsignedShort.write(packet[0], fileobj)
            String.write(packet[1], fileobj)
            # noinspection PyTypeChecker
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


class ClientCapabilitiesResponsePacket(Packet):
    """
    Sent by the server to indicate whether it can accept the client based on what packets it has said it supports.
    """

    ID = 4
    NAME = "client_capabilities_response"
    SIDE = Side.SERVER

    def __init__(self, rejected: bool = False) -> None:
        super().__init__()

        self.rejected = rejected

    def read(self, fileobj: IO) -> None:
        self.rejected = Boolean.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Boolean.write(self.rejected, fileobj)


class LoginRequestPacket(Packet):
    """
    Sent by the client when attempting to login.
    """

    ID = 5
    NAME = "login_request"
    SIDE = Side.CLIENT

    def __init__(self, username: str = "", password: str = "", group_name: str = "") -> None:
        super().__init__()

        self.username = username
        self.password = password
        self.group_name = group_name

    def read(self, fileobj: IO) -> None:
        self.username = String.read(fileobj)
        self.password = String.read(fileobj)
        self.group_name = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        String.write(self.username, fileobj)
        String.write(self.password, fileobj)
        String.write(self.group_name, fileobj)


class LoginResponsePacket(Packet):
    """
    Sent by the server to inform the client as to whether the login request was successful.
    """

    ID = 6
    NAME = "login_response"
    SIDE = Side.SERVER

    def __init__(self, success: bool = False, message: str = "No additional info.", user: User = None) -> None:
        super().__init__()

        self.success = success
        self.message = message
        self.user = user

    def read(self, fileobj: IO) -> None:
        self.success = Boolean.read(fileobj)
        self.message = String.read(fileobj)

        if self.success and Boolean.read(fileobj):
            self.user = UserType.read(fileobj)

    def write(self, fileobj: IO) -> None:
        Boolean.write(self.success, fileobj)
        String.write(self.message, fileobj)

        if self.success:
            if self.user is not None:
                Boolean.write(True, fileobj)
                UserType.write(self.user, fileobj)
            else:
                Boolean.write(False, fileobj)


class ConnectionInfoRequestPacket(Packet):
    """
    Sent by the client to request connection info.
    """

    ID = 7
    NAME = "connection_info_request"
    SIDE = Side.CLIENT

    def read(self, fileobj: IO) -> None:
        ...

    def write(self, fileobj: IO) -> None:
        ...


class ConnectionInfoResponsePacket(Packet):
    """
    Sent by the server in response to a connection info request. Contains ping, user and ip data.
    """

    ID = 8
    NAME = "connection_info_response"
    SIDE = Side.SERVER

    def __init__(self, ping: int = 0, user: User = None, host: str = "localhost", port: int = 5001) -> None:
        super().__init__()

        self.ping = ping
        self.user = user
        self.host = host
        self.port = port

    def read(self, fileobj: IO) -> None:
        self.ping = UnsignedShort.read(fileobj)
        self.user = UserType.read(fileobj)
        self.host = String.read(fileobj)
        self.port = Integer.read(fileobj)

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(self.ping, fileobj)
        UserType.write(self.user, fileobj)
        String.write(self.host, fileobj)
        Integer.write(self.port, fileobj)


class PrintPacket(Packet):
    """
    Sent by the server to display info, given a channel, on the client. Channels 0-4 are log levels fatal to debug.
    """

    ID = 9
    NAME = "print"
    SIDE = Side.SERVER

    def __init__(self, channel: int = 0, message: str = "") -> None:
        super().__init__()

        self.channel = channel
        self.message = message

    def read(self, fileobj: IO) -> None:
        self.channel = UnsignedShort.read(fileobj)
        self.message = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        UnsignedShort.write(self.channel, fileobj)
        String.write(self.message, fileobj)


class InputPacket(Packet):
    """
    Sent by the server to request input from the client, given a prompt.
    """

    ID = 10
    NAME = "input"
    SIDE = Side.SERVER

    def __init__(self, prompt: str = ""):
        super().__init__()

        self.prompt = prompt

    def read(self, fileobj: IO) -> None:
        self.prompt = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        String.write(self.prompt, fileobj)


class InputResponsePacket(Packet):
    """
    Sent by the client in response to an input request.
    """

    ID = 11
    NAME = "input_response"
    SIDE = Side.CLIENT

    def __init__(self, response: str = "") -> None:
        super().__init__()

        self.response = response

    def read(self, fileobj: IO) -> None:
        self.response = String.read(fileobj)

    def write(self, fileobj: IO) -> None:
        String.write(self.response, fileobj)


class KeepAlivePacket(Packet):
    """
    Sent by the server periodically to make sure the client is still listening.
    """

    ID = 12
    NAME = "keep_alive"
    SIDE = Side.SERVER

    def __init__(self, keep_alive_id: int = 0) -> None:
        super().__init__()

        self.keep_alive_id = keep_alive_id

    def read(self, fileobj: IO) -> None:
        self.keep_alive_id = VarInt.read(fileobj)

    def write(self, fileobj: IO) -> None:
        VarInt.write(self.keep_alive_id, fileobj)


class KeepAliveResponsePacket(KeepAlivePacket):
    """
    Sent by the client in response to the keep alive packet.
    """

    ID = 13
    NAME = "keep_alive_response"
    SIDE = Side.CLIENT


class DisconnectPacket(Packet):
    """
    Sent by either side to indicate that they want to terminate the connection, a reason can be provided.
    """

    ID = 14
    NAME = "disconnect"
    SIDE = Side.BOTH

    def __init__(self, message: str = "Generic disconnect.", redirect: bool = False, redirect_ip: str = "",
                 redirect_port: int = 0) -> None:
        super().__init__()

        self.message = message
        self.redirect = redirect
        self.redirect_ip = redirect_ip
        self.redirect_port = redirect_port

    def read(self, fileobj: IO) -> None:
        self.message = String.read(fileobj)
        self.redirect = Boolean.read(fileobj)

        if self.redirect:
            self.redirect_ip = String.read(fileobj)
            self.redirect_port = UnsignedShort.read(fileobj)

    def write(self, fileobj: IO) -> None:
        String.write(self.message, fileobj)
        Boolean.write(self.redirect, fileobj)

        if self.redirect:
            String.write(self.redirect_ip, fileobj)
            UnsignedShort.write(self.redirect_port, fileobj)
