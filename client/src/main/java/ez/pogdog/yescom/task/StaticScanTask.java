package ez.pogdog.yescom.task;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.util.BlockPosition;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticScanTask implements ILoadedChunkTask {

    private final YesCom yesCom = YesCom.getInstance();

    private final Dimension dimension;
    private final IQuery.Priority priority;

    private final long startTime;

    private int taskID;

    private int currentQueries;
    private int currentIndex;

    private final ArrayList<BlockPosition> positions = new ArrayList<>();

    /**
     * @param dimension The dimension to check in
     * @param addonPositions Any points that you want to add on top of the default highway ones
     */
    public StaticScanTask(Dimension dimension, ArrayList<BlockPosition> addonPositions, IQuery.Priority priority) {
        this.dimension = dimension;
        this.priority = priority;

        startTime = System.currentTimeMillis();

        addDefaults();
        if(addonPositions != null) this.positions.addAll(addonPositions);

        yesCom.logger.debug("Starting selective check task");
        yesCom.logger.debug("Dimension: ", dimension);

        currentQueries = 0;
        currentIndex = 0;
    }

    public StaticScanTask() {
        this(Dimension.OVERWORLD, new ArrayList<>(), IQuery.Priority.MEDIUM);
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
    public void onFinished() {
    }

    @Override
    public int getID() {
        return taskID;
    }

    @Override
    public void setID(int ID) {
        taskID = ID;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public int getTimeElapsed() {
        return (int)(System.currentTimeMillis() - startTime);
    }

    @Override
    public float getProgress() {
        return 0.0f;
    }

    @Override
    public String getFormattedResult(Object result) {
        return String.format("Found someone on highway (static): %s (dim %s).", result, dimension);
    }

    /* ------------------------ Private methods ------------------------ */

    private void onLoaded(ChunkPosition chunkPosition) {
        yesCom.logger.info(getFormattedResult(chunkPosition));
        onLoaded(chunkPosition, dimension);
        //TODO: Perfect place to pickup people for tracking, they are most likely heading down the highway to base/stash
    }

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
}
