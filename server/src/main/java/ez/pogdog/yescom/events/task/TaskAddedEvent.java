package ez.pogdog.yescom.events.task;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.task.ActiveTask;

public class TaskAddedEvent extends ReporterEvent {

    private final ActiveTask task;

    public TaskAddedEvent(YCReporter reporter, ActiveTask task) {
        super(reporter);

        this.task = task;
    }

    public ActiveTask getTask() {
        return task;
    }
}
