package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Packet.Info(name="client_capabilities", id=3, side=Packet.Side.CLIENT)
public class ClientCapabilitiesPacket extends Packet {

    private final List<Class<? extends Packet>> acceptedPackets;

    public ClientCapabilitiesPacket(List<Class<? extends Packet>> acceptedPackets) {
        this.acceptedPackets = new ArrayList<>();
        this.acceptedPackets.addAll(acceptedPackets);
    }

    public ClientCapabilitiesPacket() {
        this(Registry.knownPackets);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        int entries = Registry.UNSIGNED_SHORT.read(inputStream);

        acceptedPackets.clear();

        for (int index = 0; index < entries; ++index) acceptedPackets.add(Registry.PACKET_SPEC.read(inputStream));
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(acceptedPackets.size(), outputStream);
        for (Class<? extends Packet> clazz : acceptedPackets) Registry.PACKET_SPEC.write(clazz, outputStream);
    }

    public void addPacket(Class<? extends Packet> clazz) {
        if (!acceptedPackets.contains(clazz)) acceptedPackets.add(clazz);
    }

    public List<Class<? extends Packet>> getAccepted() {
        return new ArrayList<>(acceptedPackets);
    }
}
