#!/usr/bin/env python3

"""
Packets package for network. Packets inherit from the base Packet class. Packets can be added (if compatible), and
should work with clients if they have the ability to read such packets too. Built in packets should be included in
every client.
"""

from .builtin import *

packets = [
    ServerInfoPacket,
    StartEncryptionPacket,
    EncryptionResponsePacket,
    ClientCapabilitiesPacket,
    ClientCapabilitiesResponsePacket,
    LoginPacket,
    LoginResponsePacket,
    ConnectionInfoRequestPacket,
    ConnectionInfoResponsePacket,
    PrintPacket,
    InputPacket,
    InputResponsePacket,
    KeepAlivePacket,
    KeepAliveResponsePacket,
    DisconnectPacket,
]
