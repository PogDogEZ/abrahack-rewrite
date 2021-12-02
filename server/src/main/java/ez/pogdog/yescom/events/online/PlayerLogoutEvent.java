package ez.pogdog.yescom.events.online;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;

import java.util.UUID;

/**
 * Called when any player logs out.
 */
public class PlayerLogoutEvent extends ReporterEvent {

    private final UUID uuid;

    public PlayerLogoutEvent(YCReporter reporter, UUID uuid) {
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