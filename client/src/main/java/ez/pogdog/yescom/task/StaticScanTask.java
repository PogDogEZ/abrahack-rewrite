package ez.pogdog.yescom.task;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.util.BlockPosition;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Written by NathanW 9/8/21
 */
public class StaticScanTask implements ITask {

    private final YesCom yesCom = YesCom.getInstance();

    private final Dimension dimension;

    private int currentQueries;
    private int currentIndex;

    private final ArrayList<BlockPosition> positions = new ArrayList<>();

    /**
     * @param dimension The dimension to check in
     * @param addonPositions Any points that you want to add on top of the default highway ones
     */
    public StaticScanTask(Dimension dimension, ArrayList<BlockPosition> addonPositions) {
        this.dimension = dimension;

        addDefaults();
        if(addonPositions != null) this.positions.addAll(addonPositions);

        yesCom.logger.debug("Starting selective check task");
        yesCom.logger.debug("Dimension: ", dimension);

        currentQueries = 0;
        currentIndex = 0;
    }

    /* ------------------------ Implementations ------------------------ */

    @Override
    public void onTick() {
        while (currentQueries < 200) {

            tickNextPosition();

            synchronized (this) {
                ++currentQueries;

                yesCom.queryHandler.addQuery(new IsLoadedQuery(getCurrentPosition(),
                        dimension, IQuery.Priority.MEDIUM, yesCom.configHandler.TYPE,
                        (query, result) -> {
                            synchronized (this) {
                                --currentQueries;

                                if (result == IsLoadedQuery.Result.LOADED)
                                    onLoaded(new ChunkPosition(query.getPosition().getX() / 16, query.getPosition().getZ() / 16));
                            }
                        }));
            }
        }

        yesCom.logger.infoDisappearing(String.format("Scanning: %d, %d / %d, %d, %s, %dms.",
                yesCom.queryHandler.getWaitingSize(), yesCom.queryHandler.getTickingSize(),
                currentQueries, currentIndex, getCurrentPosition(), yesCom.connectionHandler.getTimeSinceLastPacket()));
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public float getProgressPercent() {
        return 0;
    }

    @Override
    public int getEstTimeToFinish() {
        return 0;
    }

    @Override
    public String getName() {
        return "static_scan";
    }

    @Override
    public String getDescription() {
        return "A scan task that only checks coordinates from a given array.";
    }

    @Override
    public Map<String, String> getParamDescriptions() {
        Map<String, String> params = new HashMap<>();

        params.put("dimension", "The dimension to scan in.");
        params.put("addonPositions", "Static positions to keep scanning of.");

        return params;
    }

    /* ------------------------ Private methods ------------------------ */

    private BlockPosition getCurrentPosition() {
        return positions.get(currentIndex);
    }

    /**
     * TODO: Add better coordinates that are likely to catch players on the highways
     * Just some simple highway distances that may be good for picking up players
     */
    private void addDefaults() {
        positions.addAll(getAllHighwayPositions(1000));
        positions.addAll(getAllHighwayPositions(5000));
        positions.addAll(getAllHighwayPositions(10000));
        positions.addAll(getAllHighwayPositions(20000));
        positions.addAll(getAllHighwayPositions(50000));
        positions.addAll(getAllHighwayPositions(80000));
        positions.addAll(getAllHighwayPositions(100000));
        positions.addAll(getAllHighwayPositions(300000));
        positions.addAll(getAllHighwayPositions(500000));
        positions.addAll(getAllHighwayPositions(700000));
        positions.addAll(getAllHighwayPositions(1000000));
    }

    /**
     * Very fucking ugly, but I don't see a better way of doing it without it being even uglier
     * @param distance the distance to add all highway points
     * @return all points on highway
     */
    public ArrayList<BlockPosition> getAllHighwayPositions(int distance) {
        ArrayList<BlockPosition> positions = new ArrayList<>();
        positions.add(new BlockPosition(distance, 0, 0));
        positions.add(new BlockPosition(-distance, 0, 0));
        positions.add(new BlockPosition(0, 0, distance));
        positions.add(new BlockPosition(0, 0, -distance));
        positions.add(new BlockPosition(distance, 0, distance));
        positions.add(new BlockPosition(-distance, 0, distance));
        positions.add(new BlockPosition(distance, 0, -distance));
        positions.add(new BlockPosition(-distance, 0, -distance));

        return positions;
    }

    /**
     * Just increases the next position to check, so we can evenly check all positions
     */
    private void tickNextPosition() {
        if(positions.size() - 1 == currentIndex) {
            currentIndex = 0;
            return;
        }
        ++currentIndex;
    }

    private void onLoaded(ChunkPosition chunkPos) {
        yesCom.logger.info(String.format("Found someone on highway (static): %s (dim %s).", chunkPos.getPosition(), dimension));
        //TODO: Perfect place to pickup people for tracking, they are most likely heading down the highway to base/stash
    }
}
