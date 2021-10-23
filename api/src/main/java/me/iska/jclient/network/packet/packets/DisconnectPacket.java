package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="disconnect", id=14, side=Packet.Side.BOTH)
public class DisconnectPacket extends Packet {

    private String message;

    public DisconnectPacket(String message) {
        this.message = message;
    }

    public DisconnectPacket() {
        this("Generic Disconnect");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        message = Registry.STRING.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.STRING.write(message, outputStream);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
