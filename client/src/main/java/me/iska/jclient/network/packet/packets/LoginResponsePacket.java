package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="login_response", id=6, side=Packet.Side.SERVER)
public class LoginResponsePacket extends Packet {

    private boolean successful;
    private String message;

    public LoginResponsePacket(boolean successful, String message) {
        this.successful = successful;
        this.message = message;
    }

    public LoginResponsePacket() {
        this(false, "No additional info.");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        successful = Registry.BOOLEAN.read(inputStream);
        message = Registry.STRING.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.BOOLEAN.write(successful, outputStream);
        Registry.STRING.write(message, outputStream);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }
}
