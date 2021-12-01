package ez.pogdog.yescom.events.player;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.Player;

public class PlayerAddedEvent extends ReporterEvent {

    private final Player player;

    public PlayerAddedEvent(YCReporter reporter, Player player) {
        super(reporter);

        this.player = player;
    }

    /**
     * @return The player that was added.
     */
    public Player getPlayer() {
        return player;
    }
}
