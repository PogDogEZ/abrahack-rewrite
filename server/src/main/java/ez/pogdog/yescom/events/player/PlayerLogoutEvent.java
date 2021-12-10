package ez.pogdog.yescom.events.player;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.Player;

/**
 * Called when a player associated with YesCom logs out.
 */
public class PlayerLogoutEvent extends ReporterEvent {

    private final Player player;
    private final String reason;

    public PlayerLogoutEvent(YCReporter reporter, Player player, String reason) {
        super(reporter);

        this.player = player;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public String getReason() {
        return reason;
    }
}
