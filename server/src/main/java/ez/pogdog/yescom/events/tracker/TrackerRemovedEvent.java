package ez.pogdog.yescom.events.tracker;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.Tracker;

public class TrackerRemovedEvent extends ReporterEvent {

    private final Tracker tracker;

    public TrackerRemovedEvent(YCReporter reporter, Tracker tracker) {
        super(reporter);
        this.tracker = tracker;
    }

    /**
     * @return The tracker that was removed.
     */
    public Tracker getTracker() {
        return tracker;
    }
}
