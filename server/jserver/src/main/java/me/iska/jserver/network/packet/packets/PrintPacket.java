package me.iska.jserver.network.packet.packets;

import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent by the server to display info, given a channel, on the client. Channels 0-4 are log levels fatal to debug.
 */
@Packet.Info(name="print", id=9, side=Packet.Side.SERVER)
public class PrintPacket extends Packet {

    private int channel;
    private String message;

    public PrintPacket(int channel, String message) {
        this.channel = channel;
        this.message = message;
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        channel = Registry.UNSIGNED_SHORT.read(inputStream);
        message = Registry.STRING.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(channel, outputStream);
        Registry.STRING.write(message, outputStream);
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
