package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Packet.Info(name="client_capabilities", id=3, side=Packet.Side.CLIENT)
public class ClientCapabilitiesPacket extends Packet {

    private final List<Class<? extends Packet>> packets = new ArrayList<>();

    public ClientCapabilitiesPacket(List<Class<? extends Packet>> packets) {
        this.packets.addAll(packets);
    }

    public ClientCapabilitiesPacket() {
        this(Registry.knownPackets);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        int entries = Registry.UNSIGNED_SHORT.read(inputStream);

        packets.clear();

        for (int index = 0; index < entries; ++index) {
            int ID = Registry.UNSIGNED_SHORT.read(inputStream);
            String name = Registry.STRING.read(inputStream);
            Side side = new EnumType<>(Side.class).read(inputStream);
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(packets.size(), outputStream);

        for (Class<? extends Packet> clazz : packets) {
            Registry.UNSIGNED_SHORT.write(Packet.getID(clazz), outputStream);
            Registry.STRING.write(Packet.getName(clazz), outputStream);
            new EnumType<>(Side.class).write(Packet.getSide(clazz), outputStream);
        }
    }

    public void addPacket(Class<? extends Packet> clazz) {
        if (!packets.contains(clazz)) packets.add(clazz);
    }

    public List<Class<? extends Packet>> getPackets() {
        return new ArrayList<>(packets);
    }
}
