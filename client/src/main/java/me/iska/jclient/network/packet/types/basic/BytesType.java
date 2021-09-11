package me.iska.jclient.network.packet.types.basic;

import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BytesType extends Type<byte[]> {

    @Override
    public byte[] read(InputStream inputStream) throws IOException {
        int length = (int)(long)new IntType().read(inputStream);
        byte[] bytes = new byte[length];
        inputStream.read(bytes);
        return bytes;
    }

    @Override
    public void write(byte[] value, OutputStream outputStream) throws IOException {
        new IntType().write(value.length, outputStream);
        outputStream.write(value);
    }
}
