package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="connection_info_response", id=8, side=Packet.Side.SERVER)
public class ConnectionInfoResponsePacket extends Packet {

    private int ping;
    private String user;
    private String host;
    private int port;

    public ConnectionInfoResponsePacket(int ping, String user, String host, int port) {
        this.ping = ping;
        this.user = user;
        this.host = host;
        this.port = port;
    }

    public ConnectionInfoResponsePacket() {
        this(0, "", "localhost", 5001);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        ping = Registry.UNSIGNED_SHORT.read(inputStream);
        user = Registry.STRING.read(inputStream);
        host = Registry.STRING.read(inputStream);
        port = Registry.INT.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(ping, outputStream);
        Registry.STRING.write(user, outputStream);
        Registry.STRING.write(host, outputStream);
        Registry.INT.write(port, outputStream);
    }

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
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
