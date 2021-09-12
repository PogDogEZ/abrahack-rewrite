package ez.pogdog.yescom.tracking.information;

import ez.pogdog.yescom.util.BlockPosition;
import ez.pogdog.yescom.util.ChunkPosition;

public class TrackerPosition {

    private final BlockPosition startingPosition;

    private BlockPosition position;

    public TrackerPosition(BlockPosition startingPosition) {
        this.startingPosition = startingPosition;
        position = startingPosition;
    }

    public TrackerPosition(ChunkPosition startingPosition) {
        this(startingPosition.getPosition());
    }

    /* ------------------------ Public Methods ------------------------ */

    public BlockPosition getStartingPosition() {
        return startingPosition;
    }

    public BlockPosition getPosition() {
        return position;
    }

    public int getX() {
        return position.getX();
    }

    public int getZ() {
        return position.getZ();
    }
}
