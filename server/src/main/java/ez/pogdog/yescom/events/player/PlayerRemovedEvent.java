package ez.pogdog.yescom.events.player;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.Player;

public class PlayerRemovedEvent extends ReporterEvent {

    private final Player player;
    private final String reason;

    public PlayerRemovedEvent(YCReporter reporter, Player player, String reason) {
        super(reporter);

        this.player = player;
        this.reason = reason;
    }

    /**
     * @return The player that was removed.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The reason the player was disconnected.
     */
    public String getReason() {
        return reason;
    }
}

