package me.iska.jclient.network.handler;

import me.iska.jclient.network.packet.Packet;

public interface IHandler {
    void handlePacket(Packet packet);
}
