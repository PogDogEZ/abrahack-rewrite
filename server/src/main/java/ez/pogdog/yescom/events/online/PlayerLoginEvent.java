package ez.pogdog.yescom.events.online;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;

import java.util.UUID;

/**
 * Called when any player logs into the server.
 */
public class PlayerLoginEvent extends ReporterEvent {

    private final UUID uuid;
    private final String displayName;

    public PlayerLoginEvent(YCReporter reporter, UUID uuid, String displayName) {
        super(reporter);

        this.uuid = uuid;
        this.displayName = displayName;
    }

    /**
     * @return The UUID of the player.
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * @return The display name of the player.
     */
    public String getDisplayName() {
        return displayName;
    }
}
