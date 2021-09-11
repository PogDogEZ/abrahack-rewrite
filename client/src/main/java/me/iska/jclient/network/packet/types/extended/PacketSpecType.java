package me.iska.jclient.network.packet.types.extended;

import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PacketSpecType extends Type<Class<? extends Packet>> {

    @Override
    public Class<? extends Packet> read(InputStream inputStream) throws IOException {
        int packetID = Registry.UNSIGNED_SHORT.read(inputStream);
        String packetName = Registry.STRING.read(inputStream);
        Packet.Side packetSide = new EnumType<>(Packet.Side.class).read(inputStream);

        return Registry.knownPackets.stream()
                .filter(clazz -> Packet.getID(clazz) == packetID && Packet.getName(clazz).equals(packetName) &&
                        Packet.getSide(clazz) == packetSide)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void write(Class<? extends Packet> value, OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(Packet.getID(value), outputStream);
        Registry.STRING.write(Packet.getName(value), outputStream);
        new EnumType<>(Packet.Side.class).write(Packet.getSide(value), outputStream);
    }
}
