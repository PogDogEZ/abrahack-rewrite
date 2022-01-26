package ez.pogdog.yescom.task.tasks;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.ChunkState;
import ez.pogdog.yescom.event.Emitters;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.queries.IsLoadedQuery;
import ez.pogdog.yescom.task.ITask;
import ez.pogdog.yescom.task.TaskRegistry;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StaticScanTask implements ITask {

    /**
     * TODO: Add better coordinates that are likely to catch players on the highways
     * Just some simple highway distances that may be good for picking up players
     */
    public static List<ChunkPosition> getDefaults() {
        List<ChunkPosition> positions = new ArrayList<>();

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

        return positions;
    }

    /**
     * Very fucking ugly, but I don't see a better way of doing it without it being even uglier
     * @param distance the distance to add all highway points
     * @return all points on highway
     */
    public static List<ChunkPosition> getAllHighwayPositions(int distance) {
        List<ChunkPosition> positions = new ArrayList<>();

        positions.add(new ChunkPosition(distance, 0));
        positions.add(new ChunkPosition(-distance, 0));
        positions.add(new ChunkPosition(0, distance));
        positions.add(new ChunkPosition(0, -distance));
        positions.add(new ChunkPosition(distance, distance));
        positions.add(new ChunkPosition(-distance, distance));
        positions.add(new ChunkPosition(distance, -distance));
        positions.add(new ChunkPosition(-distance, -distance));

        return positions;
    }

    private final YesCom yesCom = YesCom.getInstance();

    private final List<ChunkPosition> positions = new ArrayList<>();
    private final List<IsLoadedQuery> currentQueries = new ArrayList<>();
    private final List<Object> results = new ArrayList<>();

    private final Dimension dimension;
    private final IQuery.Priority priority;

    private final long startTime;

    private int taskID;

    private int currentIndex;

    /**
     * @param positions Any points that you want to check.
     * @param dimension The dimension to check in.
     */
    public StaticScanTask(List<ChunkPosition> positions, Dimension dimension, IQuery.Priority priority) {
        this.positions.addAll(positions);
        this.dimension = dimension;
        this.priority = priority;

        startTime = System.currentTimeMillis();

        // addDefaults();

        yesCom.logger.fine(String.format("Starting static scan task with %d positions to check.", this.positions.size()));
        yesCom.logger.fine(String.format("Dimension: %s.", dimension));

        currentIndex = 0;
    }

    public StaticScanTask() {
        this(getDefaults(), Dimension.OVERWORLD, IQuery.Priority.MEDIUM);
    }

    /* ------------------------ Implementations ------------------------ */

    @Override
    public void tick() {
        while (!positions.isEmpty() && currentIndex < positions.size() && currentQueries.size() < 20) {
            ChunkPosition currentPosition = getCurrentPosition();
            ++currentIndex;

            synchronized (this) {
                IsLoadedQuery isLoadedQuery = new IsLoadedQuery(this, currentPosition, dimension, IQuery.Priority.MEDIUM,
                        yesCom.configHandler.TYPE, (query, result) -> {
                    synchronized (this) {
                        currentQueries.remove(query);

                        ChunkState chunkState = yesCom.dataHandler.newChunkState(
                                result == IsLoadedQuery.Result.LOADED ? ChunkState.State.LOADED : ChunkState.State.UNLOADED,
                                query.getChunkPosition(), dimension, System.currentTimeMillis());
                        Emitters.CHUNK_STATE_EMITTER.emit(chunkState);
                        
                        if (result == IsLoadedQuery.Result.LOADED) onLoaded(query.getChunkPosition());
                    }
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
    public void finish() {
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
    public TaskRegistry.RegisteredTask getRegisteredTask() {
        return TaskRegistry.getTask(getClass());
    }

    @Override
    public List<TaskRegistry.Parameter> getParameters() {
        List<TaskRegistry.Parameter> parameters = new ArrayList<>();
        List<TaskRegistry.ParamDescription> paramDescriptions = TaskRegistry.getTask(getClass()).getParamDescriptions();

        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(0), positions));
        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(1), Collections.singletonList(dimension)));
        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(2), Collections.singletonList(priority)));

        return parameters;
    }

    @Override
    public boolean isFinished() {
        return (positions.isEmpty() || currentIndex >= positions.size()) && currentQueries.isEmpty();
    }

    @Override
    public List<Object> getResults() {
        return results;
    }

    @Override
    public int getTimeElapsed() {
        return (int)(System.currentTimeMillis() - startTime);
    }

    @Override
    public boolean hasProgress() {
        return true;
    }

    @Override
    public float getProgress() {
        return currentIndex / (float)positions.size() * 100.0f;
    }

    @Override
    public boolean hasCurrentPosition() {
        return true;
    }

    @Override
    public ChunkPosition getCurrentPosition() {
        return positions.get(Math.min(positions.size() - 1, currentIndex));
    }

    /* ------------------------ Private methods ------------------------ */

    private void onLoaded(ChunkPosition chunkPosition) {
        yesCom.logger.info(String.format("Found loaded (static): %s (dim %s).", chunkPosition, dimension));
        results.add(chunkPosition);

        // yesCom.trackingHandler.onLoadedChunk(chunkPosition, dimension);
    }
}
