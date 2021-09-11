package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="loaded_chunk", id=YCRegistry.ID_OFFSET + 5, side=Packet.Side.CLIENT)
public class LoadedChunkPacket extends Packet {

    private ChunkPosition chunkPosition;
    private Dimension dimension;

    public LoadedChunkPacket(ChunkPosition chunkPosition, Dimension dimension) {
        this.chunkPosition = chunkPosition;
        this.dimension = dimension;
    }

    public LoadedChunkPacket() {
        this(new ChunkPosition(0, 0), Dimension.OVERWORLD);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        chunkPosition = YCRegistry.CHUNK_POSITION.read(inputStream);
        dimension = Dimension.fromMC(Registry.SHORT.read(inputStream));
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        YCRegistry.CHUNK_POSITION.write(chunkPosition, outputStream);
        Registry.SHORT.write((short)dimension.getMCDim(), outputStream);
    }

    public ChunkPosition getChunkPosition() {
        return chunkPosition;
    }

    public void setChunkPosition(ChunkPosition chunkPosition) {
        this.chunkPosition = chunkPosition;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }
}
