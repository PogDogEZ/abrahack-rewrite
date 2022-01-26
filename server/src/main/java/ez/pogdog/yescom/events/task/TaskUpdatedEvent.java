package ez.pogdog.yescom.events.task;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.task.ActiveTask;

public class TaskUpdatedEvent extends ReporterEvent {

    private final ActiveTask task;
    private final int timeElapsed;
    private final boolean hasProgress;
    private final float progress;
    private final boolean hasCurrentPosition;
    private final ChunkPosition currentPosition;

    public TaskUpdatedEvent(YCReporter reporter, ActiveTask task, int timeElapsed, boolean hasProgress, float progress,
                            boolean hasCurrentPosition, ChunkPosition currentPosition) {
        super(reporter);

        this.task = task;
        this.timeElapsed = timeElapsed;
        this.hasProgress = hasProgress;
        this.progress = progress;
        this.hasCurrentPosition = hasCurrentPosition;
        this.currentPosition = currentPosition;
    }

    public ActiveTask getTask() {
        return task;
    }

    public int getTimeElapsed() {
        return timeElapsed;
    }

    public boolean getHasProgress() {
        return hasProgress;
    }

    public float getProgress() {
        return progress;
    }

    public boolean getHasCurrentPosition() {
        return hasCurrentPosition;
    }

    public ChunkPosition getCurrentPosition() {
        return currentPosition;
    }
}
