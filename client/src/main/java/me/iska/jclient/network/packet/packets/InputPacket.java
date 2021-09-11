package me.iska.jclient.network.packet.packets;

import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="input", id=10, side= Packet.Side.SERVER)
public class InputPacket extends Packet {

    private String prompt;

    public InputPacket(String prompt) {
        this.prompt = prompt;
    }

    public InputPacket() {
        this("");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        prompt = Registry.STRING.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Registry.STRING.write(prompt, outputStream);
    }
}
