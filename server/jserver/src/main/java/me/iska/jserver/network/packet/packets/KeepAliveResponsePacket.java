package me.iska.jserver.network.packet.packets;

import me.iska.jserver.network.packet.Packet;

/**
 * Sent by the client in response to keep alive packets.
 */
@Packet.Info(name="keep_alive_response", id=13, side= Packet.Side.CLIENT)
public class KeepAliveResponsePacket extends KeepAlivePacket {

    public KeepAliveResponsePacket(long keepAliveID) {
        super(keepAliveID);
    }

    public KeepAliveResponsePacket() {
        super();
    }
}
