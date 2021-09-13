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
]

reporting_packets = [
    YCInitRequestPacket,
    YCInitResponsePacket,
    TaskSyncPacket,
    TaskActionPacket,
    reporting.AccountActionPacket,
    reporting.AccountActionResponsePacket,
    PlayerActionPacket,
    LoadedChunkPacket,
]

all_packets = listening_packets.copy()
# Avoid duplicates
for packet in reporting_packets:
    if not packet in all_packets:
        all_packets.append(packet)
