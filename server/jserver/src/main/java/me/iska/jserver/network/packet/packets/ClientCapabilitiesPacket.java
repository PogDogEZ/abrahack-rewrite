package me.iska.jserver.network.packet.packets;

import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Client capabilities, sent by the client to indicate which packets it can accept.
 */
@Packet.Info(name="client_capabilities", id=3, side= Packet.Side.CLIENT)
public class ClientCapabilitiesPacket extends Packet {

    private final EnumType<Side> SIDE = new EnumType<>(Side.class);

    private final List<PacketRepresentation> packets = new ArrayList<>();

    public ClientCapabilitiesPacket(List<Class<? extends Packet>> packets) {
        this.packets.addAll(packets.stream().map(PacketRepresentation::new).collect(Collectors.toList()));
    }

    public ClientCapabilitiesPacket() {
        this(Registry.KNOWN_PACKETS);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        int entries = Registry.UNSIGNED_SHORT.read(inputStream);

        packets.clear();

        for (int index = 0; index < entries; ++index) {
            int ID = Registry.UNSIGNED_SHORT.read(inputStream);
            String name = Registry.STRING.read(inputStream);
            Side side = new EnumType<>(Side.class).read(inputStream);

            packets.add(new PacketRepresentation(name, ID, side));
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(packets.size(), outputStream);

        for (PacketRepresentation representation : packets) {
            Registry.UNSIGNED_SHORT.write(representation.getID(), outputStream);
            Registry.STRING.write(representation.getName(), outputStream);
            SIDE.write(representation.getSide(), outputStream);
        }
    }

    public void addPacket(PacketRepresentation representation) {
        if (!packets.contains(representation)) packets.add(representation);
    }

    public List<PacketRepresentation> getPackets() {
        return new ArrayList<>(packets);
    }

    /**
     * Intermediate representation of a packet, includes the name, ID and side it originates from.
     */
    public static class PacketRepresentation {

        private final String name;
        private final int ID;
        private final Side side;

        public PacketRepresentation(String name, int ID, Side side) {
            this.name = name;
            this.ID = ID;
            this.side = side;
        }

        public PacketRepresentation(Class<? extends Packet> clazz) {
            this(Packet.getName(clazz), Packet.getID(clazz), Packet.getSide(clazz));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            PacketRepresentation that = (PacketRepresentation)other;
            return ID == that.ID && name.equals(that.name) && side == that.side;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, ID, side);
        }

        @Override
        public String toString() {
            return String.format("PacketRepresentation(name=%s, ID=%d, side=%s)", name, ID, side);
        }

        public String getName() {
            return name;
        }

        public int getID() {
            return ID;
        }

        public Side getSide() {
            return side;
        }
    }
}
