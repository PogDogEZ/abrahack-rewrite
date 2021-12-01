package ez.pogdog.yescom.events.task;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.task.ActiveTask;

public class TaskResultEvent extends ReporterEvent {

    private final ActiveTask task;
    private final String result;

    public TaskResultEvent(YCReporter reporter, ActiveTask task, String result) {
        super(reporter);
        this.task = task;
        this.result = result;
    }

    public ActiveTask getTask() {
        return task;
    }

    public String getResult() {
        return result;
    }
}
