package me.iska.jclient.network.packet.types.basic;

import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BooleanType extends Type<Boolean> {

    @Override
    public Boolean read(InputStream inputStream) throws IOException {
        byte[] data = new byte[1];
        inputStream.read(data);
        return data[0] == (byte)1;
    }

    @Override
    public void write(Boolean value, OutputStream outputStream) throws IOException {
        outputStream.write(new byte[] { value ? (byte)1 : (byte)0 });
    }
}
