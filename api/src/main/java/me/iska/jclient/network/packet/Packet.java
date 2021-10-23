package me.iska.jclient.network.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

@Packet.Info(name="base_packet", id=-1, side=Packet.Side.NONE)
public abstract class Packet {

    public static String getName(Class<? extends Packet> clazz) {
        Packet.Info info = clazz.getAnnotation(Packet.Info.class);
        return info.name();
    }

    public static int getID(Class<? extends Packet> clazz) {
        Packet.Info info = clazz.getAnnotation(Packet.Info.class);
        return info.id();
    }

    public static Side getSide(Class<? extends Packet> clazz) {
        Packet.Info info = clazz.getAnnotation(Packet.Info.class);
        return info.side();
    }

    public String toString() {
        return String.format("Packet(name=%s, id=%d)", getName(getClass()), getID(getClass()));
    }

    public abstract void read(InputStream inputStream) throws IOException;
    public abstract void write(OutputStream outputStream) throws IOException;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Info {
        String name();
        int id();
        Side side();
    }

    public enum Side {
        NONE,
        SERVER,
        CLIENT,
        BOTH;
    }
}
