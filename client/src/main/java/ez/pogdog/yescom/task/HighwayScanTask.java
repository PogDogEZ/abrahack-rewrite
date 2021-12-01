package ez.pogdog.yescom.task;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HighwayScanTask implements ILoadedChunkTask {

    private final YesCom yesCom = YesCom.getInstance();

    private final int[] HIGHWAY_X_ORDINATES = new int[] { 1, 1, 1, 0, 0, -1, -1, -1 };
    private final int[] HIGHWAY_Z_ORDINATES = new int[] { -1, 0, 1, -1, 1, -1, 0, 1 };

    private final List<IsLoadedQuery> currentQueries = new ArrayList<>();

    private final int maxDistance;
    private final int minDistance;
    private final int chunkSkip;
    private final Dimension dimension;
    private final IQuery.Priority priority;

    private final long startTime;

    private final int maxIndex;

    private int taskID;

    private int currentIndex;

    /**
     * Scans the highways.
     * @param maxDistance The maximum distance to scan to.
     * @param minDistance The minimum distance to scan from.
     * @param chunkSkip The number of chunks to linearly skip.
     * @param dimension The dimension to scan in.
     * @param priority The query priority.
     */
    public HighwayScanTask(Integer maxDistance, Integer minDistance, Integer chunkSkip, Dimension dimension, IQuery.Priority priority) {
        this.maxDistance = Math.max(minDistance, maxDistance) / chunkSkip;
        this.minDistance = Math.min(minDistance, maxDistance) / chunkSkip;
        this.chunkSkip = chunkSkip;
        this.dimension = dimension;
        this.priority = priority;

        startTime = System.currentTimeMillis();

        maxIndex = (this.maxDistance - this.minDistance) * 8;

        yesCom.logger.fine("Starting highway scan task.");
        yesCom.logger.fine(String.format("Min dist: %d.", minDistance));
        yesCom.logger.fine(String.format("Max dist: %d.", maxDistance));
        yesCom.logger.fine(String.format("Dimension: %s.", dimension));
        yesCom.logger.fine(String.format("Max index: %d.", maxIndex));

        currentIndex = 0;
    }

    @Override
    public void onTick() {
        while (currentQueries.size() < 20 && currentIndex < maxIndex) {
            ChunkPosition position = getCurrentPosition();
            ++currentIndex;

            synchronized (this) {
                IsLoadedQuery isLoadedQuery = new IsLoadedQuery(position, dimension, priority, yesCom.configHandler.TYPE,
                        (query, result) -> {
                    currentQueries.remove(query);
                    if (result == IsLoadedQuery.Result.LOADED) onLoaded(query.getChunkPosition());
                });

                currentQueries.add(isLoadedQuery);
                yesCom.queryHandler.addQuery(isLoadedQuery);
            }
        }

        /*
        yesCom.logger.infoDisappearing(String.format("Scanning: %d, %d / %d, %d, %s, %dms.",
                yesCom.queryHandler.getWaitingSize(), yesCom.queryHandler.getTickingSize(),
                currentQueries.size(), currentIndex, getCurrentPosition(), yesCom.connectionHandler.getTimeSinceLastPacket()));
         */
    }

    @Override
    public void onFinished() {
        synchronized (this) {
            currentQueries.forEach(IsLoadedQuery::cancel);
            currentQueries.clear();
        }
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
    public List<TaskRegistry.Parameter> getParameters() {
        List<TaskRegistry.Parameter> parameters = new ArrayList<>();
        List<TaskRegistry.ParamDescription> paramDescriptions = TaskRegistry.getTask(getClass()).getParamDescriptions();

        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(0), Collections.singletonList(maxDistance)));
        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(1), Collections.singletonList(minDistance)));
        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(2), Collections.singletonList(chunkSkip)));
        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(3), Collections.singletonList(dimension)));
        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(4), Collections.singletonList(priority)));

        return parameters;
    }

    @Override
    public boolean isFinished() {
        return currentIndex >= maxIndex && currentQueries.isEmpty();
    }

    @Override
    public int getTimeElapsed() {
        return (int)(System.currentTimeMillis() - startTime);
    }

    @Override
    public float getProgress() {
        return currentIndex / (float)maxIndex;
    }

    @Override
    public String getFormattedResult(Object result) {
        return String.format("Found loaded (highway): %s (dim %s).", result, dimension);
    }

    @Override
    public ChunkPosition getCurrentPosition() {
        return new ChunkPosition(
                HIGHWAY_X_ORDINATES[currentIndex % 8] * (currentIndex / 8 + minDistance) * chunkSkip,
                HIGHWAY_Z_ORDINATES[currentIndex % 8] * (currentIndex / 8 + minDistance) * chunkSkip
        );
    }

    private void onLoaded(ChunkPosition chunkPosition) {
        yesCom.logger.info(getFormattedResult(chunkPosition));
        if (yesCom.ycHandler != null) yesCom.ycHandler.onTaskResult(this, getFormattedResult(chunkPosition));
        onLoaded(chunkPosition, dimension);

        yesCom.trackingHandler.onLoadedChunk(chunkPosition, dimension);
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public int getMinDistance() {
        return minDistance;
    }
}
