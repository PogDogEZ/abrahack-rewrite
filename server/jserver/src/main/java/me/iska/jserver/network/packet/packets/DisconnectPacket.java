package me.iska.jserver.network.packet.packets;

import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent by both parties to indicate that they are about to disconnect, given a reason, can also contain data informing
 * the client to a redirect IP and port.
 */
@Packet.Info(name="disconnect", id=14, side=Packet.Side.BOTH)
public class DisconnectPacket extends Packet {

    private String message;

    private boolean redirect;
    private String redirectIP;
    private int redirectPort;

    public DisconnectPacket(String message, boolean redirect, String redirectIP, int redirectPort) {
        this.message = message;
        this.redirect = redirect;
        this.redirectIP = redirectIP;
        this.redirectPort = redirectPort;
    }

    public DisconnectPacket(String message, String redirectIP, int redirectPort) {
        this(message, true, redirectIP, redirectPort);
    }

    public DisconnectPacket(String message) {
        this(message, false, "", 0);
    }

    public DisconnectPacket() {
        this("Generic disconnect.", false, "", 0);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        message = Registry.STRING.read(inputStream);
        redirect = Registry.BOOLEAN.read(inputStream);

        if (redirect) {
            redirectIP = Registry.STRING.read(inputStream);
            redirectPort = Registry.UNSIGNED_SHORT.read(inputStream);
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.STRING.write(message, outputStream);
        Registry.BOOLEAN.write(redirect, outputStream);

        if (redirect) {
            Registry.STRING.write(redirectIP, outputStream);
            Registry.UNSIGNED_SHORT.write(redirectPort, outputStream);
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getRedirect() {
        return redirect;
    }

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

    public String getRedirectIP() {
        return redirectIP;
    }

    public void setRedirectIP(String redirectIP) {
        this.redirectIP = redirectIP;
    }

    public int getRedirectPort() {
        return redirectPort;
    }

    public void setRedirectPort(int redirectPort) {
        this.redirectPort = redirectPort;
    }
}
