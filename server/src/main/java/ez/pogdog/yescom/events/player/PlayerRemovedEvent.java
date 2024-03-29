package ez.pogdog.yescom.events.player;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.Player;

public class PlayerRemovedEvent extends ReporterEvent {

    private final Player player;

    public PlayerRemovedEvent(YCReporter reporter, Player player) {
        super(reporter);

        this.player = player;
    }

    /**
     * @return The player that was removed.
     */
    public Player getPlayer() {
        return player;
    }
}

