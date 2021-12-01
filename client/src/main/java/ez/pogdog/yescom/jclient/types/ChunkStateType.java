package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.data.serializable.ChunkState;
import ez.pogdog.yescom.util.Dimension;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

public class ChunkStateType extends Type<ChunkState> {

    private final EnumType<ChunkState.State> STATE = new EnumType<>(ChunkState.State.class);

    @Override
    public ChunkState read(InputStream inputStream) throws IOException {
        BigInteger chunkStateID = Registry.VAR_INTEGER.read(inputStream);
        ChunkState.State state = STATE.read(inputStream);
        ChunkPosition chunkPosition = YCRegistry.CHUNK_POSITION.read(inputStream);
        Dimension dimension = Dimension.fromMC(Registry.SHORT.read(inputStream));
        long foundAt = Registry.LONG.read(inputStream);

        return new ChunkState(chunkStateID, state, chunkPosition, dimension, foundAt);
    }

    @Override
    public void write(ChunkState value, OutputStream outputStream) throws IOException {
        Registry.VAR_INTEGER.write(value.getID(), outputStream);
        STATE.write(value.getState(), outputStream);
        YCRegistry.CHUNK_POSITION.write(value.getChunkPosition(), outputStream);
        Registry.SHORT.write((short)value.getDimension().getMCDim(), outputStream);
        Registry.LONG.write(value.getFoundAt(), outputStream);
    }
}
