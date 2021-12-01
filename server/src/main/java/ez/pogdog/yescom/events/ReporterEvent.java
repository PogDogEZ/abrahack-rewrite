package ez.pogdog.yescom.events;

import ez.pogdog.yescom.network.handlers.YCReporter;
import me.iska.jserver.event.Event;

/**
 * An abstract event involving a reporter, duh.
 */
public abstract class ReporterEvent extends Event {

    private final YCReporter reporter;

    public ReporterEvent(YCReporter reporter) {
        this.reporter = reporter;
    }

    public YCReporter getReporter() {
        return reporter;
    }
}
