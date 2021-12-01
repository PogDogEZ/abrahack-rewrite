package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent by the client in response to input requests.
 */
@Packet.Info(name="input_response", id=11, side= Packet.Side.CLIENT)
public class InputResponsePacket extends Packet {

    private String response;

    public InputResponsePacket(String response) {
        this.response = response;
    }

    public InputResponsePacket() {
        this("");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        response = Registry.STRING.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.STRING.write(response, outputStream);
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
