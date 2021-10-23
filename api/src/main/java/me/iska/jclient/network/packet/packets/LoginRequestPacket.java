package me.iska.jclient.network.packet.packets;

import me.iska.jclient.impl.user.User;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="login_request", id=5, side=Packet.Side.CLIENT)
public class LoginRequestPacket extends Packet {

    private User user;
    private String password;

    public LoginRequestPacket(User user, String pasword) {
        this.user = user;
        this.password = pasword;
    }

    public LoginRequestPacket() {
        this(null, "");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        if (Registry.BOOLEAN.read(inputStream)) {
            user = Registry.USER.read(inputStream);
        } else {
            user = null;
        }
        password = Registry.STRING.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        if (user == null) {
            Registry.BOOLEAN.write(false, outputStream);
        } else {
            Registry.BOOLEAN.write(true, outputStream);
            Registry.USER.write(user, outputStream);
        }
        Registry.STRING.write(password, outputStream);
    }
}
