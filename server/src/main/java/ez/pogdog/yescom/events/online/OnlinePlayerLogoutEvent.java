package ez.pogdog.yescom.events.online;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;

import java.util.UUID;

/**
 * Called when any player logs out.
 */
public class OnlinePlayerLogoutEvent extends ReporterEvent {

    private final UUID uuid;

    public OnlinePlayerLogoutEvent(YCReporter reporter, UUID uuid) {
        super(reporter);

        this.uuid = uuid;
    }

    /**
     * @return The UUID of the player.
     */
    public UUID getUUID() {
        return uuid;
    }
}