package ez.pogdog.yescom.tracking.trackers.adaptive;

import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.util.ChunkPosition;

public interface IPhase {

    /**
     * Returns the update time for the tracker.
     * @return The update time in ms.
     */
    int getUpdateTime();

    /**
     * Returns the maximum number of offsets that this phase needs to check.
     */
    int getMaxOffsets();

    /**
     * Returns an offset given an index.
     * @param index The index.
     * @param distance The distance to check from the center.
     * @return The offset.
     */
    ChunkPosition getOffset(int index, float distance);
    IsLoadedQuery.Result getResult(int index);
}
