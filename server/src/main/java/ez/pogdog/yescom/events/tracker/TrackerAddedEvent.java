package ez.pogdog.yescom.events.tracker;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.Tracker;

public class TrackerAddedEvent extends ReporterEvent {

    private final Tracker tracker;

    public TrackerAddedEvent(YCReporter reporter, Tracker tracker) {
        super(reporter);
        this.tracker = tracker;
    }

    /**
     * @return The new tracker that was added.
     */
    public Tracker getTracker() {
        return tracker;
    }
}
