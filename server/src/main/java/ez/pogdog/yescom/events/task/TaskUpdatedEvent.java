package ez.pogdog.yescom.events.task;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.task.ActiveTask;

public class TaskUpdatedEvent extends ReporterEvent {

    private final ActiveTask task;
    private final boolean loadedChunkTask;
    private final float progress;
    private final int timeElapsed;
    private final ChunkPosition currentPosition;

    public TaskUpdatedEvent(YCReporter reporter, ActiveTask task, boolean loadedChunkTask, float progress, int timeElapsed,
                            ChunkPosition currentPosition) {
        super(reporter);

        this.task = task;
        this.loadedChunkTask = loadedChunkTask;
        this.progress = progress;
        this.timeElapsed = timeElapsed;
        this.currentPosition = currentPosition;
    }

    public ActiveTask getTask() {
        return task;
    }

    public boolean isLoadedChunkTask() {
        return loadedChunkTask;
    }

    public float getProgress() {
        return progress;
    }

    public int getTimeElapsed() {
        return timeElapsed;
    }

    public ChunkPosition getCurrentPosition() {
        return currentPosition;
    }
}
