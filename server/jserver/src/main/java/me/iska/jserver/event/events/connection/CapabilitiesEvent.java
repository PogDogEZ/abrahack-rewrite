package me.iska.jserver.event.events.connection;

import me.iska.jserver.event.EventCancellable;
import me.iska.jserver.network.Connection;
import me.iska.jserver.network.packet.packets.ClientCapabilitiesPacket;

import java.util.ArrayList;
import java.util.List;

public class CapabilitiesEvent extends EventCancellable {

    private final List<ClientCapabilitiesPacket.PacketRepresentation> clientPackets = new ArrayList<>();
    private final List<ClientCapabilitiesPacket.PacketRepresentation> serverPackets = new ArrayList<>();

    private final Connection connection;

    private boolean rejected;

    public CapabilitiesEvent(Connection connection, boolean rejected,
                             List<ClientCapabilitiesPacket.PacketRepresentation> clientPackets,
                             List<ClientCapabilitiesPacket.PacketRepresentation> serverPackets) {
        this.connection = connection;
        this.rejected = rejected;

        this.clientPackets.addAll(clientPackets);
        this.serverPackets.addAll(serverPackets);
    }

    public List<ClientCapabilitiesPacket.PacketRepresentation> getClientPackets() {
        return new ArrayList<>(clientPackets);
    }

    public void addClientPacket(ClientCapabilitiesPacket.PacketRepresentation packet) {
        clientPackets.add(packet);
    }

    public void setClientPackets(List<ClientCapabilitiesPacket.PacketRepresentation> packets) {
        clientPackets.clear();
        clientPackets.addAll(packets);
    }

    public void addClientPackets(List<ClientCapabilitiesPacket.PacketRepresentation> packets) {
        clientPackets.addAll(packets);
    }

    public void removeClientPacket(ClientCapabilitiesPacket.PacketRepresentation packet) {
        clientPackets.remove(packet);
    }

    public List<ClientCapabilitiesPacket.PacketRepresentation> getServerPackets() {
        return new ArrayList<>(serverPackets);
    }

    public void addServerPacket(ClientCapabilitiesPacket.PacketRepresentation packet) {
        serverPackets.add(packet);
    }

    public void setServerPackets(List<ClientCapabilitiesPacket.PacketRepresentation> packets) {
        serverPackets.clear();
        serverPackets.addAll(packets);
    }

    public void addServerPackets(List<ClientCapabilitiesPacket.PacketRepresentation> packets) {
        serverPackets.addAll(packets);
    }

    public void removeServerPacket(ClientCapabilitiesPacket.PacketRepresentation packet) {
        serverPackets.remove(packet);
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }
}
