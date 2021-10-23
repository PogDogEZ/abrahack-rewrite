package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="config_action", id=YCRegistry.ID_OFFSET + 7, side=Packet.Side.BOTH)
public class ConfigActionPacket extends Packet {

    public ConfigActionPacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {

    }

    @Override
    public void write(OutputStream outputStream) throws IOException {

    }
}
