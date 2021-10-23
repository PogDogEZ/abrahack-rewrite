package ez.pogdog.yescom.data.serializable;

import ez.pogdog.yescom.data.ISerializable;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.math.BigInteger;

public class ChunkState implements ISerializable {

    private final BigInteger chunkStateID;
    private final State state;
    private final ChunkPosition chunkPosition;
    private final Dimension dimension;
    private final long foundAt;

    public ChunkState(BigInteger chunkStateID, State state, ChunkPosition chunkPosition, Dimension dimension, long foundAt) {
        this.chunkStateID = chunkStateID;
        this.state = state;
        this.chunkPosition = chunkPosition;
        this.dimension = dimension;
        this.foundAt = foundAt;
    }

    @Override
    public String toString() {
        return String.format("ChunkState(state=%s, position=%s)", state, chunkPosition);
    }

    @Override
    public BigInteger getID() {
        return chunkStateID;
    }

    public State getState() {
        return state;
    }

    public ChunkPosition getChunkPosition() {
        return chunkPosition;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public long getFoundAt() {
        return foundAt;
    }

    public enum State {
        LOADED, UNLOADED;
    }
}
