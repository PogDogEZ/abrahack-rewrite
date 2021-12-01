package me.iska.jserver.network.packet.types.basic;

import me.iska.jserver.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ShortType extends Type<Short> {

    @Override
    public Short read(InputStream inputStream) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        inputStream.read(byteBuffer.array());
        return byteBuffer.getShort();
    }

    @Override
    public void write(Short value, OutputStream outputStream) throws IOException {
        outputStream.write(ByteBuffer.allocate(2).putShort(value).array());
    }

    public static class Unsigned extends Type<Integer> {

        @Override
        public Integer read(InputStream inputStream) throws IOException {
            ByteBuffer byteBuffer = ByteBuffer.allocate(2);
            inputStream.read(byteBuffer.array());
            return Short.toUnsignedInt(byteBuffer.getShort());
        }

        @Override
        public void write(Integer value, OutputStream outputStream) throws IOException {
            outputStream.write(ByteBuffer.allocate(2).putShort((short)(int)value).array());
        }
    }
}
