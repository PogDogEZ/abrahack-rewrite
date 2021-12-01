package ez.pogdog.yescom.events.task;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.task.ActiveTask;

public class TaskRemovedEvent extends ReporterEvent {

    private final ActiveTask task;

    public TaskRemovedEvent(YCReporter reporter, ActiveTask task) {
        super(reporter);

        this.task = task;
    }

    public ActiveTask getTask() {
        return task;
    }
}
