package me.iska.jclient.network.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class Type<T> {
    public abstract T read(InputStream inputStream) throws IOException;
    public abstract void write(T value, OutputStream outputStream) throws IOException;
}
