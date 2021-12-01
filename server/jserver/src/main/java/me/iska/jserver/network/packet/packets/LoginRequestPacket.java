package me.iska.jserver.network.packet.packets;

import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent by the client when attempting to login to the server.
 */
@Packet.Info(name="login_request", id=5, side= Packet.Side.CLIENT)
public class LoginRequestPacket extends Packet {

    private String username;
    private String groupName;
    private String password;

    public LoginRequestPacket(String username, String groupName, String password) {
        this.username = username;
        this.groupName = groupName;
        this.password = password;
    }

    public LoginRequestPacket() {
        this("", "", "");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        username = Registry.STRING.read(inputStream);
        password = Registry.STRING.read(inputStream);
        groupName = Registry.STRING.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.STRING.write(username, outputStream);
        Registry.STRING.write(password, outputStream);
        Registry.STRING.write(groupName, outputStream);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
