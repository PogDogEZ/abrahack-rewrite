package ez.pogdog.yescom.data;

import ez.pogdog.yescom.data.serializable.ChunkState;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;

import java.math.BigInteger;

public interface IDataProvider {
    ChunkState getChunkState(BigInteger chunkStateID);
    RenderDistance getRenderDistance(BigInteger renderDistanceID);
    TrackedPlayer getTrackedPlayer(BigInteger trackedPlayerID);
}
