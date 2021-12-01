package me.iska.jclient.network.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Basic packet type, all packets must extend this class.
 */
@Packet.Info(name="base_packet", id=-1, side=Packet.Side.SERVER)
public abstract class Packet {

    /**
     * Gets the name of a packet given a class.
     * @param clazz The class to get the name of.
     * @return The name of the packet.
     */
    public static String getName(Class<? extends Packet> clazz) {
        Info info = clazz.getAnnotation(Info.class);
        return info.name();
    }

    /**
     * Gets the ID of a packet given a class.
     * @param clazz The class to get the ID of.
     * @return The ID of the packet.
     */
    public static int getID(Class<? extends Packet> clazz) {
        Info info = clazz.getAnnotation(Info.class);
        return info.id();
    }

    /**
     * Gets the side of a packet given a class.
     * @param clazz The class to get the side of.
     * @return The side of the packet.
     */
    public static Side getSide(Class<? extends Packet> clazz) {
        Info info = clazz.getAnnotation(Info.class);
        return info.side();
    }

    public String toString() {
        return String.format("Packet(name=%s, id=%d)", getName(getClass()), getID(getClass()));
    }

    /**
     * Reads a packet from an input stream.
     * @param inputStream The input stream to read from.
     * @throws IOException If an I/O error occurs.
     */
    public abstract void read(InputStream inputStream) throws IOException;

    /**
     * Writes a packet to an output stream.
     * @param outputStream The output stream to write to.
     * @throws IOException If an I/O error occurs.
     */
    public abstract void write(OutputStream outputStream) throws IOException;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Info {
        String name();
        int id();
        Side side();
    }

    /**
     * Which side the packet will be sent from.
     */
    public enum Side {
        SERVER,
        CLIENT,
        BOTH;
    }
}
