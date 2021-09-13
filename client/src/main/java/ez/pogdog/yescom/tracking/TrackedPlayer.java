package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.UUID;

/**
 * Stores data that enables us to continue tracking a player once they log out and rejoin.
 */
public class TrackedPlayer {
    private final ChunkPosition logOutPos;
    private final Dimension dimension;
    private final UUID[] possiblePlayers;

    public TrackedPlayer(ChunkPosition logOutPos, Dimension dimension, UUID[] possiblePlayers) {
        this.logOutPos = logOutPos;
        this.dimension = dimension;
        this.possiblePlayers = possiblePlayers;
    }

    public UUID[] getPossiblePlayers() {
        return possiblePlayers;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public ChunkPosition getLogOutPos() {
        return logOutPos;
    }

    public UUID getMostLikelyPlayer() {
        return possiblePlayers[0];
    }
}
