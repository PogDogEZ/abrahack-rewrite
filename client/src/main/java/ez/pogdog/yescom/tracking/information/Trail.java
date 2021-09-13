package ez.pogdog.yescom.tracking.information;

import ez.pogdog.yescom.util.BlockPosition;
import ez.pogdog.yescom.util.ChunkPosition;

import java.util.ArrayList;

/**
 * Class used for storing route information about a tracker.
 * Works by recording position and time of points to be reconstructed into a coherent trail.
 */
public class Trail {

    private final ArrayList<BreadCrumb> crumbsList = new ArrayList<>();

    private final BlockPosition startingPoint;

    /**
     * Average distance between points, basically how accurate the trail is.
     */
    private float resolution;

    public Trail(BlockPosition startingPoint) {
        this.startingPoint = startingPoint;
        resolution = 0;

        crumbsList.add(new BreadCrumb(startingPoint));
    }

    public Trail(ChunkPosition startingPoint) {
        this(startingPoint.getPosition());
    }

    /* ------------------------ Public Methods ------------------------ */

    public void addBreadCrumb(BlockPosition position) {
        crumbsList.add(new BreadCrumb(position));

        // I assume this is faster than iterating the hashmap
        resolution += (
                position.getDistanceTo(crumbsList.get(crumbsList.size() - 2).getPosition()) - resolution) / crumbsList.size();
    }

    public BlockPosition getStartingPoint() {
        return startingPoint;
    }

    public BlockPosition getCurrentPoint() {
        return crumbsList.get(crumbsList.size()-1).getPosition();
    }

    public class BreadCrumb {
        private final BlockPosition position;
        private final int time;

        public BreadCrumb(BlockPosition position) {
            this.position = position;
            time = (int) (System.currentTimeMillis() / 1000);
        }

        public BlockPosition getPosition() {
            return position;
        }

        public int getTime() {
            return time;
        }
    }
}
