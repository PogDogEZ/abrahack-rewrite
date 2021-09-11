package me.iska.jclient.network.packet.types.basic;

import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class LongType extends Type<Long> {

    @Override
    public Long read(InputStream inputStream) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        inputStream.read(byteBuffer.array());
        return byteBuffer.getLong();
    }

    @Override
    public void write(Long value, OutputStream outputStream) throws IOException {
        outputStream.write(ByteBuffer.allocate(8).putLong(value).array());
    }
}
