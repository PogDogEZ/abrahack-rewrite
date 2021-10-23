package me.iska.jclient.network.packet.types.basic;

import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StringType extends Type<String> {

    @Override
    public String read(InputStream inputStream) throws IOException {
        int length = new ShortType.Unsigned().read(inputStream);
        byte[] raw = new byte[length];
        inputStream.read(raw);
        return new String(raw, StandardCharsets.UTF_8);
    }

    @Override
    public void write(String value, OutputStream outputStream) throws IOException {
        byte[] raw = value.getBytes(StandardCharsets.UTF_8);
        new ShortType.Unsigned().write(raw.length, outputStream);
        outputStream.write(raw);
    }
}
