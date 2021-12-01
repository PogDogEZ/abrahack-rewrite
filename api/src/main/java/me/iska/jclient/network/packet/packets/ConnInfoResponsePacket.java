package me.iska.jclient.network.packet.packets;

import me.iska.jclient.user.User;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent by the server in response to a connection info request.
 */
@Packet.Info(name="connection_info_response", id=8, side= Packet.Side.SERVER)
public class ConnInfoResponsePacket extends Packet {

    private int ping;
    private User user;
    private String host;
    private int port;

    public ConnInfoResponsePacket(int ping, User user, String host, int port) {
        this.ping = ping;
        this.user = user;
        this.host = host;
        this.port = port;
    }

    public ConnInfoResponsePacket() {
        this(0, null, "localhost", 5001);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        ping = Registry.UNSIGNED_SHORT.read(inputStream);
        user = Registry.USER.read(inputStream);
        host = Registry.STRING.read(inputStream);
        port = Registry.INTEGER.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(ping, outputStream);
        Registry.USER.write(user, outputStream);
        Registry.STRING.write(host, outputStream);
        Registry.INTEGER.write(port, outputStream);
    }

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
