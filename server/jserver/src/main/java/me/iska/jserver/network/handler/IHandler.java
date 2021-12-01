package me.iska.jserver.network.handler;

import me.iska.jserver.network.packet.Packet;

/**
 * Handles incoming packets.
 */
public interface IHandler {

    /**
     * Handles async incoming packets.
     * @param packet The packet that was received.
     */
    void handlePacket(Packet packet);

    /**
     * Called on sync connection update.
     */
    void update();

    /**
     * Called on connection exit.
     */
    void exit(String reason);
}
