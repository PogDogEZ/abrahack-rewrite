package ez.pogdog.yescom.events;

import ez.pogdog.yescom.network.handlers.YCReporter;

public class ReporterRemovedEvent extends ReporterEvent {
    public ReporterRemovedEvent(YCReporter reporter) {
        super(reporter);
    }
}
