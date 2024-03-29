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

public class SpiralScanTask implements ITask {

    private final YesCom yesCom = YesCom.getInstance();

    private final List<Object> results = new ArrayList<>();

    private final ChunkPosition startPos;
    private final int chunkSkip;
    private final Dimension dimension;
    private final IQuery.Priority priority;

    private final long startTime;

    private int taskID;

    private int currentQueries;
    private int currentIndex;
    private int[] nextSpiral;

    private ChunkPosition currentPosition;

    private SpiralAlgorithm spiral;

    /**
     * @param startPos The center of the spiral
     * @param chunkSkip The server render distance * 2
     * @param dimension The dimension to scan in
     */
    public SpiralScanTask(ChunkPosition startPos, Integer chunkSkip, Dimension dimension, IQuery.Priority priority) {
        this.startPos = new ChunkPosition(startPos.getX() / chunkSkip, startPos.getZ() / chunkSkip);
        this.chunkSkip = chunkSkip;
        this.dimension = dimension;
        this.priority = priority;
        spiral = new SpiralAlgorithm();

        startTime = System.currentTimeMillis();

        nextSpiral = new int[] {0,0};

        yesCom.logger.fine("Starting spiral scan task.");
        yesCom.logger.fine(String.format("Start: %s.", startPos));
        yesCom.logger.fine("Dimension: " + dimension);

        currentQueries = 0;
        currentIndex = 0;

        currentPosition = new ChunkPosition(0, 0);
    }

    public SpiralScanTask() {
        this(new ChunkPosition(0, 0), 12, Dimension.OVERWORLD, IQuery.Priority.MEDIUM);
    }

    /* ------------------------ Implementations ------------------------ */

    @Override
    public void tick() {
        while (currentQueries < 200) {
            tickSpiral();
            ++currentIndex;

            synchronized (this) {
                currentPosition = getNextSpiral();
                ++currentQueries;

                yesCom.queryHandler.addQuery(new IsLoadedQuery(this, currentPosition, dimension, priority,
                        yesCom.configHandler.TYPE,
                        (query, result) -> {
                    synchronized (this) {
                        --currentQueries; // TODO: New system

                        ChunkState chunkState = yesCom.dataHandler.newChunkState(
                                result == IsLoadedQuery.Result.LOADED ? ChunkState.State.LOADED : ChunkState.State.UNLOADED,
                                query.getChunkPosition(), dimension, System.currentTimeMillis());
                        Emitters.CHUNK_STATE_EMITTER.emit(chunkState);

                        if (result == IsLoadedQuery.Result.LOADED)
                            onLoaded(query.getChunkPosition());
                    }
                }));
            }
        }

        /*
        yesCom.logger.infoDisappearing(String.format("Scanning: %d, %d / %d, %d, %s, %dms.",
                yesCom.queryHandler.getWaitingSize(), yesCom.queryHandler.getTickingSize(),
                currentQueries, currentIndex, getNextSpiral(), yesCom.connectionHandler.getTimeSinceLastPacket()));
         */
    }

    @Override
    public void finish() {
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

        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(0), Collections.singletonList(startPos)));
        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(1), Collections.singletonList(chunkSkip)));
        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(2), Collections.singletonList(dimension)));
        parameters.add(new TaskRegistry.Parameter(paramDescriptions.get(3), Collections.singletonList(priority)));

        return parameters;
    }

    @Override
    public boolean isFinished() {
        return false;
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
        return false;
    }

    @Override
    public float getProgress() {
        return 0.0f;
    }

    @Override
    public boolean hasCurrentPosition() {
        return true;
    }

    @Override
    public ChunkPosition getCurrentPosition() {
        return currentPosition;
    }

    /* ------------------------ Private methods ------------------------ */

    private void onLoaded(ChunkPosition chunkPosition) {
        yesCom.logger.info(String.format("Found loaded (spiral): %s (dim %s).", chunkPosition, dimension));
        results.add(chunkPosition);

        // yesCom.dataHandler.saveUncompressed(getFormattedResult(chunkPosition.getPosition()), "SpiralScanLog");
    }

    private ChunkPosition getNextSpiral() {
        return new ChunkPosition(startPos.getX() + (nextSpiral[0] * chunkSkip),
                startPos.getZ() + (nextSpiral[1] * chunkSkip));
    }

    private void tickSpiral() {
        nextSpiral = spiral.next();
    }

    public ChunkPosition getStartPos() {
        return startPos;
    }

    /**
     * Spiral algorithm - NOTE: USED INTERNET FOR HELP, JUST FOR PROOF OF CONCEPT
     * TODO: Put this somewhere else/clean up? Algorithm interface? More efficient?
     */
    private class SpiralAlgorithm {
        int radius, side, step;

        public SpiralAlgorithm() {
            this.radius = 0;
            this.side = 0;
            this.step = 0;
        }

        public int[] next() {
            int[] i = {0, 0};
            if (radius == 0) {
                radius++;
                step = -radius;
                return i;
            }
            switch (side) {
                case 0:
                    i = new int[]{radius, -step};
                    break;
                case 1:
                    i = new int[]{-step, -radius};
                    break;
                case 2:
                    i = new int[]{-radius, step};
                    break;
                case 3:
                    i = new int[]{step, radius};
                    break;
            }
            step++;
            if (step >= radius) {
                side++;
                if (side % 4 == 0) {
                    radius++;
                    side = 0;
                }
                step = -radius;
            }
            return i;
        }
    }
}
