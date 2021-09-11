package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.util.ChunkPosition;

import java.util.UUID;

/**
 * All the tracking logic, runs independently as it's own process, assinged to an ID, passes off to Player Handler with all it's info when it loses track,
 * stores all movement data as raw hits
 */

//TODO: Actually write this
public class Tracker implements ITracker {

    private final YesCom yesCom = YesCom.getInstance();

    private final ChunkPosition initialChunk;
    private final TrackedPlayer player;
    private final UUID id;

    /**
     * @param chunk Initial position assigned by the handler
     * @param player Only matters if the tracker is a reassignment of an archived player, can be null otherwise
     */
    //TODO: Add things to params, this is just a rough outline of things you may want with the creation of a tracker
    public Tracker(ChunkPosition chunk, TrackedPlayer player) {
        this.initialChunk = chunk;
        this.player = player;

        id = UUID.randomUUID();
    }

    @Override
    public void onTick() {

    }

    @Override
    public boolean isLost() {
        return false;
    }

    @Override
    public UUID getID() {
        return id;
    }

    @Override
    public TrackedPlayer getTrackedPlayer() {
        return player;
    }
}
