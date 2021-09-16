package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.handlers.TrackingHandler;
import ez.pogdog.yescom.tracking.information.TrackerPosition;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stores data that enables us to continue tracking a player once they log out and rejoin.
 */
public class TrackedPlayer implements ITracker {
    private final TrackerPosition logOutPos;
    private final Dimension dimension;
    private final ArrayList<UUID> possiblePlayers = new ArrayList<>();

    public TrackedPlayer(TrackerPosition logOutPos, List<UUID> possiblePlayers) {
        this.logOutPos = logOutPos;
        this.dimension = logOutPos.getDimension();

        if (possiblePlayers != null)
            this.possiblePlayers.addAll(possiblePlayers);
    }

    @Override
    public TrackingHandler.TrackerTickResult onTick() {
        return TrackingHandler.TrackerTickResult.Lost;
    }

    @Override
    public void onPossibleJoin(UUID uuid) {
    }

    public TrackedPlayer(TrackerPosition logOutPos) {
        this(logOutPos, null);
    }

    public List<UUID> getPossiblePlayers() {
        return possiblePlayers;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public TrackerPosition getLogOutPos() {
        return logOutPos;
    }

}
