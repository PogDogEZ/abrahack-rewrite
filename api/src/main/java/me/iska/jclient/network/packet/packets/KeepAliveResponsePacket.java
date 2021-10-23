package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;

@Packet.Info(name="keep_alive_response", id=13, side=Packet.Side.CLIENT)
public class KeepAliveResponsePacket extends KeepAlivePacket {

    public KeepAliveResponsePacket(long keepAliveID) {
        super(keepAliveID);
    }

    public KeepAliveResponsePacket() {
        super();
    }
}
