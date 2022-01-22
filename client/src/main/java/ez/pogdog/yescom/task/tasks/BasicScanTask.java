package ez.pogdog.yescom.task.tasks;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.task.ILoadedChunkTask;
import ez.pogdog.yescom.task.TaskRegistry;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BasicScanTask implements ILoadedChunkTask {

    private final YesCom yesCom = YesCom.getInstance();

    private final List<IsLoadedQuery> currentQueries = new ArrayList<>();

    private final ChunkPosition startPos;
    private final ChunkPosition endPos;
    private final IQuery.Priority priority;
    private final int chunkSkip;
    private final Dimension dimension;

    private final long startTime;

    private final int maxIndex;

    private int taskID;

    private int currentIndex;

    /**
     * @param startPos The position to start
     * @param endPos The position to end at
     * @param chunkSkip The server render distance * 2
     * @param dimension The dimension to scan in
     */
    public BasicScanTask(ChunkPosition startPos, ChunkPosition endPos, Integer chunkSkip, Dimension dimension, IQuery.Priority priority) {
        this.startPos = new ChunkPosition(startPos.getX() / chunkSkip, startPos.getZ() / chunkSkip);
        this.endPos = new ChunkPosition(endPos.getX() / chunkSkip, endPos.getZ() / chunkSkip);
        this.chunkSkip = chunkSkip;
        this.dimension = dimension;
        this.priority = priority;

        startTime = System.currentTimeMillis();

        maxIndex = (getMaxX() - getMinX()) * (getMaxZ() - getMinZ());

        yesCom.logger.fine("Starting basic scan task.");
        yesCom.logger.fine(String.format("Start: %s.", startPos));
        yesCom.logger.fine(String.format("End: %s.", endPos));
        yesCom.logger.fine(String.format("Dimension: %s.", dimension));
        yesCom.logger.fine(String.format("Max index: %d.", maxIndex));

        currentIndex = 0;
    }

    public BasicScanTask() {
        this(new ChunkPosition(0, 0), new ChunkPosition(0, 0), 12, Dimension.OVERWORLD, IQuery.Priority.MEDIUM);
    }

    /* ------------------------ Implementations ------------------------ */

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
                currentQueries, currentIndex, getCurrentPosition(), yesCom.connectionHandler.getTimeSinceLastPacket()));
         */
    }

    @Override
    public void onFinished() {
        currentQueries.forEach(IsLoadedQuery::cancel);
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
    public TaskRegistry.RegisteredTask getRegisteredTask() {
        return TaskRegistry.getTask(getClass());
    }

    @Override
    public List<TaskRegistry.Parameter> getParameters() {
        List<TaskRegistry.Parameter> parameters = new ArrayList<>();
        List<TaskRegistry.ParamDescription> paramDescriptions = TaskRegistry.getTask(getClass()).getParamDescriptions();

        // Wouldn't have to use the Collections.singletonList if Java was good at type resolving
        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(0),
                Collections.singletonList(new ChunkPosition(startPos.getX() * chunkSkip, startPos.getZ() * chunkSkip))));
        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(1),
                Collections.singletonList(new ChunkPosition(endPos.getX() * chunkSkip, endPos.getZ() * chunkSkip))));
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
        return Math.min(100.0f, (float)currentIndex / (float)maxIndex * 100.0f);
    }

    @Override
    public String getFormattedResult(Object object) {
        return String.format("Found loaded (basic): %s (dim %s).", object, dimension);
    }

    @Override
    public ChunkPosition getCurrentPosition() {
        return new ChunkPosition(
                (currentIndex % (getMaxX() - getMinX()) + getMinX()) * chunkSkip,
                (Math.floorDiv(currentIndex, getMaxX() - getMinX()) + getMinZ()) * chunkSkip
        );
    }

    /* ------------------------ Private methods ------------------------ */

    private void onLoaded(ChunkPosition chunkPosition) {
        yesCom.logger.info(getFormattedResult(chunkPosition));
        if (yesCom.ycHandler != null) yesCom.ycHandler.onTaskResult(this, getFormattedResult(chunkPosition));
        onLoaded(chunkPosition, dimension);

        // yesCom.trackingHandler.onLoadedChunk(chunkPosition, dimension);
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

    public ChunkPosition getStartPos() {
        return startPos;
    }

    public ChunkPosition getEndPos() {
        return endPos;
    }
}
