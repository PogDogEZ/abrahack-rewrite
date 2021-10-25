#!/usr/bin/env python3

import plugins.yescom.network.packets.listening as listening
import plugins.yescom.network.packets.reporting as reporting
import plugins.yescom.network.packets.shared as shared

from .listening import *
from .reporting import *
from .shared import *

listening_packets = [
    YCInitRequestPacket,
    YCInitResponsePacket,
    YCExtendedResponsePacket,  # Technically it's not actually a packet here but I'm lazy
    UpdateDataIDsPacket,
    DataRequestPacket,
    DataResponsePacket,
    DataPartPacket,
    ReporterActionPacket,
    SelectReporterPacket,
    SyncReporterPacket,
    listening.TaskActionPacket,
    listening.PlayerActionPacket,
    listening.AccountActionPacket,
    listening.AccountActionResponsePacket,
    listening.ChunkStatesPacket,
    listening.TrackerActionPacket,
    listening.InfoUpdatePacket,
    listening.OnlinePlayersActionPacket,
]

reporting_packets = [
    YCInitRequestPacket,
    YCInitResponsePacket,
    YCExtendedResponsePacket,
    UpdateDataIDsPacket,
    DataRequestPacket,
    DataResponsePacket,
    DataPartPacket,
    ConfigActionPacket,
    TaskSyncPacket,
    reporting.TaskActionPacket,
    reporting.AccountActionPacket,
    reporting.AccountActionResponsePacket,
    reporting.PlayerActionPacket,
    reporting.ChunkStatesPacket,
    reporting.TrackerActionPacket,
    reporting.InfoUpdatePacket,
    reporting.OnlinePlayersActionPacket,
]

archiving_packets = [
    YCInitRequestPacket,
    YCInitResponsePacket,
    YCExtendedResponsePacket,
    UpdateDataIDsPacket,
    DataRequestPacket,
    DataResponsePacket,
    DataPartPacket,
]

all_packets = listening_packets.copy()
# Avoid duplicates
for packet in reporting_packets:
    if not packet in all_packets:
        all_packets.append(packet)
for packet in archiving_packets:
    if not packet in all_packets:
        all_packets.append(packet)
