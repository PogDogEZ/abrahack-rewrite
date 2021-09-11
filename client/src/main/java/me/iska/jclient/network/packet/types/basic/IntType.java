package me.iska.jclient.network.packet.types.basic;

import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class IntType extends Type<Integer> {

    @Override
    public Integer read(InputStream inputStream) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        inputStream.read(byteBuffer.array());
        return byteBuffer.getInt();
    }

    @Override
    public void write(Integer value, OutputStream outputStream) throws IOException {
        outputStream.write(ByteBuffer.allocate(4).putInt(value).array());
    }

    public static class Unsigned extends Type<Long> {

        @Override
        public Long read(InputStream inputStream) throws IOException {
            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            inputStream.read(byteBuffer.array());
            return Integer.toUnsignedLong(byteBuffer.getInt());
        }

        @Override
        public void write(Long value, OutputStream outputStream) throws IOException {
            outputStream.write(ByteBuffer.allocate(4).putInt((int)(long)value).array());
        }
    }
}
