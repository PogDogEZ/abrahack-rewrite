package me.iska.jserver.network.packet.packets;

import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.user.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent by the server to inform the client if their login attempt was successful, as well as a reason as to why it was
 * unsuccessful, (or a login message if it was successful).
 */
@Packet.Info(name="login_response", id=6, side=Packet.Side.SERVER)
public class LoginResponsePacket extends Packet {

    private boolean successful;
    private String message;
    private User user;

    public LoginResponsePacket(boolean successful, String message, User user) {
        this.successful = successful;
        this.message = message;
        this.user = user;
    }

    public LoginResponsePacket() {
        this(false, "No additional info.", null);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        successful = Registry.BOOLEAN.read(inputStream);
        message = Registry.STRING.read(inputStream);

        if (successful && Registry.BOOLEAN.read(inputStream)) {
            user = Registry.USER.read(inputStream);
        } else {
            user = null;
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.BOOLEAN.write(successful, outputStream);
        Registry.STRING.write(message, outputStream);

        if (successful)  {
            if (user != null) {
                Registry.BOOLEAN.write(true, outputStream);
                Registry.USER.write(user, outputStream);
            } else {
                Registry.BOOLEAN.write(false, outputStream);
            }
        }
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
