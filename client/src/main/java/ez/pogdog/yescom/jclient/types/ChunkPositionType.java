package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.util.ChunkPosition;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ChunkPositionType extends Type<ChunkPosition> {

    @Override
    public ChunkPosition read(InputStream inputStream) throws IOException {
        int x = Registry.INT.read(inputStream);
        int z = Registry.INT.read(inputStream);
        return new ChunkPosition(x, z);
    }

    @Override
    public void write(ChunkPosition value, OutputStream outputStream) throws IOException {
        Registry.INT.write(value.getX(), outputStream);
        Registry.INT.write(value.getZ(), outputStream);
    }
}
