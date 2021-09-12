package ez.pogdog.yescom.task;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicScanTask implements ILoadedChunkTask {

    private final YesCom yesCom = YesCom.getInstance();

    private final ChunkPosition startPos;
    private final ChunkPosition endPos;
    private final IQuery.Priority priority;
    private final int chunkSkip;
    private final Dimension dimension;

    private final long startTime;

    private final int maxIndex;

    private int taskID;

    private int currentQueries;
    private int currentIndex;

    /**
     * @param startPos The position to start
     * @param endPos The position to end at
     * @param chunkSkip The server render distance * 2
     * @param dimension The dimension to scan in
     */
    public BasicScanTask(ChunkPosition startPos, ChunkPosition endPos, int chunkSkip, Dimension dimension, IQuery.Priority priority) {
        this.startPos = new ChunkPosition(startPos.getX() / chunkSkip, startPos.getZ() / chunkSkip);
        this.endPos = new ChunkPosition(endPos.getX() / chunkSkip, endPos.getZ() / chunkSkip);
        this.chunkSkip = chunkSkip;
        this.dimension = dimension;
        this.priority = priority;

        startTime = System.currentTimeMillis();

        maxIndex = (getMaxX() - getMinX()) * (getMaxZ() - getMinZ());

        yesCom.logger.debug("Starting basic scan task.");
        yesCom.logger.debug(String.format("Start: %s.", startPos));
        yesCom.logger.debug(String.format("End: %s.", endPos));
        yesCom.logger.debug("Dimension: ", dimension);
        yesCom.logger.debug(String.format("Max index: %d.", maxIndex));

        currentQueries = 0;
        currentIndex = 0;
    }

    public BasicScanTask() {
        this(new ChunkPosition(0, 0), new ChunkPosition(0, 0), 12, Dimension.OVERWORLD, IQuery.Priority.MEDIUM);
    }

    /* ------------------------ Implementations ------------------------ */

    @Override
    public void onTick() {
        while (currentQueries < 200 && currentIndex < maxIndex) {
            ChunkPosition position = getCurrentPosition();
            ++currentIndex;

            synchronized (this) {
                ++currentQueries;

                yesCom.queryHandler.addQuery(new IsLoadedQuery(position.getPosition(8, 0, 8),
                        dimension, priority, yesCom.configHandler.TYPE,
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
        return currentIndex >= maxIndex && currentQueries <= 0;
    }

    @Override
    public int getTimeElapsed() {
        return (int)(System.currentTimeMillis() - startTime);
    }

    @Override
    public float getProgress() {
        return Math.min(100.0f, (float)currentIndex / (float)maxIndex * 100.0f);
    }

    @Override
    public String getFormattedResult(Object object) {
        return String.format("Found loaded (basic): %s (dim %s).", object, dimension);
    }

    /* ------------------------ Private methods ------------------------ */

    private void onLoaded(ChunkPosition chunkPosition) {
        yesCom.logger.info(getFormattedResult(chunkPosition));
        onLoaded(chunkPosition, dimension);
    }

    private int getMinX() {
        return Math.min(startPos.getX(), endPos.getX());
    }

    private int getMaxX() {
        return Math.max(startPos.getX(), endPos.getX());
    }

    private int getMinZ() {
        return Math.min(startPos.getZ(), endPos.getZ());
    }

    private int getMaxZ() {
        return Math.max(startPos.getZ(), endPos.getZ());
    }

    private ChunkPosition getCurrentPosition() {
        return new ChunkPosition(
                (currentIndex % (getMaxX() - getMinX()) + getMinX()) * chunkSkip,
                (Math.floorDiv(currentIndex, getMaxZ() - getMinZ()) + getMinZ()) * chunkSkip
        );
    }

    public ChunkPosition getStartPos() {
        return startPos;
    }

    public ChunkPosition getEndPos() {
        return endPos;
    }
}
