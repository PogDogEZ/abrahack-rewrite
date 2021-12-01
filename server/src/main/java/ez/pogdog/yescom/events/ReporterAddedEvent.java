package ez.pogdog.yescom.events;

import ez.pogdog.yescom.network.handlers.YCReporter;

public class ReporterAddedEvent extends ReporterEvent {
    public ReporterAddedEvent(YCReporter reporter) {
        super(reporter);
    }
}
