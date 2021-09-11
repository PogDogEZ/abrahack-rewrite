package me.iska.jclient.network.packet.types.basic;

import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class FloatType extends Type<Float> {

    @Override
    public Float read(InputStream inputStream) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        inputStream.read(byteBuffer.array());
        return byteBuffer.getFloat();
    }

    @Override
    public void write(Float value, OutputStream outputStream) throws IOException {
        outputStream.write(ByteBuffer.allocate(4).putFloat(value).array());
    }
}
