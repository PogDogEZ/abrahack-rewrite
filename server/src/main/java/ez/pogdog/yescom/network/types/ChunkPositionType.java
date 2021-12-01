package ez.pogdog.yescom.network.types;

import ez.pogdog.yescom.util.ChunkPosition;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ChunkPositionType extends Type<ChunkPosition> {

    @Override
    public ChunkPosition read(InputStream inputStream) throws IOException {
        int x = Registry.INTEGER.read(inputStream);
        int z = Registry.INTEGER.read(inputStream);
        return new ChunkPosition(x, z);
    }

    @Override
    public void write(ChunkPosition value, OutputStream outputStream) throws IOException {
        Registry.INTEGER.write(value.getX(), outputStream);
        Registry.INTEGER.write(value.getZ(), outputStream);
    }
}
