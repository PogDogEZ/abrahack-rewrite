#!/usr/bin/env python3

from typing import List, Tuple

from . import Event
from ...networking.packets.packet import Packet, Side


class GenericNetworkConnectionEvent(Event):

    def __init__(self, connection) -> None:  # Connection = circular import
        super().__init__()

        self._connection = connection

    def __repr__(self) -> str:
        return "GenericNetworkConnectionEvent(connection=%r)" % self._connection

    def get_connection(self):
        return self._connection


class PreConnectionUpdateEvent(GenericNetworkConnectionEvent):

    def __init__(self, connection) -> None:
        super().__init__(connection)

    def __repr__(self) -> str:
        return "PreConnectionUpdateEvent(connection=%r)" % self._connection


class PostConnectionUpdateEvent(GenericNetworkConnectionEvent):

    def __init__(self, connection) -> None:
        super().__init__(connection)

    def __repr__(self) -> str:
        return "PostConnectionUpdateEvent(connection=%r)" % self._connection


class PrePacketInEvent(GenericNetworkConnectionEvent):

    is_cancellable = True

    def __init__(self, packet, connection) -> None:
        super().__init__(connection)

        self._packet = packet

    def __repr__(self) -> str:
        return "PrePacketInEvent(packet=%r, connection=%r)" % (self._packet, self._connection)

    def get_packet(self) -> Packet:
        return self._packet


class PostPacketInEvent(GenericNetworkConnectionEvent):

    def __init__(self, packet: Packet, connection) -> None:
        super().__init__(connection)

        self._packet = packet

    def __repr__(self) -> str:
        return "PostPacketInEvent(packet=%r, connection=%r)" % (self._packet, self._connection)

    def get_packet(self) -> Packet:
        return self._packet


class PrePacketOutEvent(GenericNetworkConnectionEvent):

    is_cancellable = True

    def __init__(self, packet: Packet, force: bool, no_compression: bool, connection) -> None:
        super().__init__(connection)

        self._packet = packet
        self._force = force
        self._no_compression = no_compression

    def __repr__(self) -> str:
        return "PrePacketOutEvent(packet=%r, force=%s, no_compression=%s, connection=%r)" % (self._packet, self._force,
                                                                                             self._no_compression,
                                                                                             self._connection)

    def get_packet(self) -> Packet:
        return self._packet

    def get_force(self) -> bool:
        return self._force

    def set_force(self, force: bool) -> None:
        # assert isinstance(force, bool), "Param 'force' must be of type bool."

        self._force = force

    def get_no_compression(self) -> bool:
        return self._no_compression

    def set_no_compression(self, no_compression: bool) -> None:
        # assert isinstance(no_compression, bool), "Param 'no_compression' must be of type bool."

        self._no_compression = no_compression


class PostPacketOutEvent(GenericNetworkConnectionEvent):

    def __init__(self, packet: Packet, connection) -> None:
        super().__init__(connection)

        self._packet = packet

    def __repr__(self) -> str:
        return "PostPacketOutEvent(packet=%r, connection=%r)" % (self._packet, self._connection)

    def get_packet(self) -> Packet:
        return self._packet


class StartEncryptionEvent(GenericNetworkConnectionEvent):

    def __init__(self, connection) -> None:
        super().__init__(connection)

    def __repr__(self) -> str:
        return "StartEncryptionEvent(connection=%r)" % self._connection


class EncryptionSuccessEvent(GenericNetworkConnectionEvent):

    def __init__(self, connection) -> None:
        super().__init__(connection)

    def __repr__(self) -> str:
        return "EncryptionSuccessEvent(connection=%r)" % self._connection


class ClientCapabilitiesEvent(GenericNetworkConnectionEvent):

    def __init__(self, unmapped_packets: List[Tuple[int, str, Side]], packets: List[Tuple[int, str, Side]],
                 rejected: bool, connection) -> None:
        super().__init__(connection)

        self._unmapped_packets = unmapped_packets
        self._packets = packets
        self._rejected = rejected

    def __repr__(self) -> str:
        return "ClientCapabilitiesEvent(rejected=%s, connection=%r)" % (self._rejected, self._connection)

    def get_unmapped_packets(self) -> List[Tuple[int, str, Side]]:
        return self._unmapped_packets.copy()

    def add_unmapped_packet(self, unmapped_packet: Tuple[int, str, Side]) -> None:
        self._unmapped_packets.append(unmapped_packet)

    def set_unmapped_packets(self, unmapped_packets: List[Tuple[int, str, Side]]) -> None:
        self._unmapped_packets.clear()
        self._unmapped_packets.extend(unmapped_packets)

    def extend_unmapped_packets(self, unmapped_packets: List[Tuple[int, str, Side]]) -> None:
        self._unmapped_packets.extend(unmapped_packets)

    def remove_unmapped_packet(self, unmapped_packet: Tuple[int, str, Side]) -> None:
        self._unmapped_packets.remove(unmapped_packet)

    def get_packets(self) -> List[Tuple[int, str, Side]]:
        return self._packets.copy()

    def get_rejected(self) -> bool:
        return self._rejected

    def set_rejected(self, rejected: bool) -> None:
        self._rejected = rejected


class BeginAuthenticationEvent(GenericNetworkConnectionEvent):

    def __init__(self, connection) -> None:
        super().__init__(connection)

    def __repr__(self) -> str:
        return "BeginAuthenticationEvent(connection=%r)" % self._connection


class AuthenticationFailedEvent(GenericNetworkConnectionEvent):

    is_cancellable = True

    def __init__(self, reason: str, connection) -> None:
        super().__init__(connection)

        self._reason = reason

    def __repr__(self) -> str:
        return "AuthenticationFailedEvent(reason=%r, connection=%r)" % (self._reason, self._connection)

    def get_reason(self) -> bool:
        return self._reason

    def set_reason(self, reason: str) -> None:
        # assert isinstance(reason, str), "Param 'reason' must be of type str."

        self._reason = reason


class AuthenticationSuccessEvent(GenericNetworkConnectionEvent):

    def __init__(self, connection):
        super().__init__(connection)

    def __repr__(self):
        return "AuthenticationSuccessEvent(connection=%r)" % self._connection


class PreDisconnectEvent(GenericNetworkConnectionEvent):

    def __init__(self, connection) -> None:
        super().__init__(connection)

    def __repr__(self) -> str:
        return "PreDisconnectEvent(connection=%r)" % self._connection


class PostDisconnectEvent(GenericNetworkConnectionEvent):

    def __init__(self, connection) -> None:
        super().__init__(connection)

    def __repr__(self) -> str:
        return "PostDisconnectEvent(connection=%r)" % self._connection
