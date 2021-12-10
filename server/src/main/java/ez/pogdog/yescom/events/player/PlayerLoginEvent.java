package ez.pogdog.yescom.events.player;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.Player;

/**
 * Called when a player associated with YesCom logs in.
 */
public class PlayerLoginEvent extends ReporterEvent {

    private final Player player;

    public PlayerLoginEvent(YCReporter reporter, Player player) {
        super(reporter);

        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
