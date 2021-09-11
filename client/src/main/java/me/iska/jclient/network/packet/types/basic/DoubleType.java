package me.iska.jclient.network.packet.types.basic;

import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class DoubleType extends Type<Double> {

    @Override
    public Double read(InputStream inputStream) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        inputStream.read(byteBuffer.array());
        return byteBuffer.getDouble();
    }

    @Override
    public void write(Double value, OutputStream outputStream) throws IOException {
        outputStream.write(ByteBuffer.allocate(8).putDouble(value).array());
    }
}
