package me.iska.jclient.network.packet.types;

import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EnumType<R extends Enum<?>> extends Type<R> {

    private final Class<R> enumType;

    public EnumType(Class<R> enumType) {
        this.enumType = enumType;
    }

    @Override
    public R read(InputStream inputStream) throws IOException {
        byte[] ordinal = new byte[1];
        inputStream.read(ordinal);
        return enumType.getEnumConstants()[ordinal[0]];
    }

    @Override
    public void write(R value, OutputStream outputStream) throws IOException {
        byte[] ordinal = new byte[] { (byte)value.ordinal() };
        outputStream.write(ordinal);
    }
}
