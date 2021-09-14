package ez.pogdog.yescom.tracking.information;

import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;


/**
 * Stores positional information used in the tracking process.
 */
public class TrackerPosition {

    private final ChunkPosition startingPosition;

    private int posX;
    private int posZ;
    private Dimension dimension;

    public TrackerPosition(ChunkPosition startingPosition, Dimension dimension) {
        this.startingPosition = startingPosition;
        posX = startingPosition.getX();
        posZ = startingPosition.getZ();
        this.dimension = dimension;
    }

    /* ------------------------ Public Methods ------------------------ */

    public void moved(int newX, int newZ, Dimension newDimension) {
        posX = newX;
        posZ = newZ;
        dimension = newDimension;
    }

    public void moved(ChunkPosition chunkPosition, Dimension dimension) {
        moved(chunkPosition.getX(), chunkPosition.getZ(), dimension);
    }

    public ChunkPosition getStartingPosition() {
        return startingPosition;
    }

    public ChunkPosition getPosition() {
        return new ChunkPosition(posX, posZ);
    }

    public Dimension getDimension() {
        return dimension;
    }

    public int getX() {
        return posX;
    }

    public int getZ() {
        return posZ;
    }
}
