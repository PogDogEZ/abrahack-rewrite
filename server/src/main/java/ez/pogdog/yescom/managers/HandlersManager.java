package ez.pogdog.yescom.managers;

import ez.pogdog.yescom.events.ReporterAddedEvent;
import ez.pogdog.yescom.events.ReporterRemovedEvent;
import ez.pogdog.yescom.network.handlers.YCHandler;
import ez.pogdog.yescom.network.handlers.YCListener;
import ez.pogdog.yescom.network.handlers.YCReporter;
import me.iska.jserver.JServer;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HandlersManager {

    private final JServer jServer = JServer.getInstance();
    private final Logger logger = JServer.getLogger();

    private final List<YCHandler> handlers = new ArrayList<>();

    private int currentHandlerID;

    public HandlersManager() {
        currentHandlerID = 0;
    }

    /**
     * @return A list of all listeners.
     */
    public List<YCListener> getListeners() {
        return handlers.stream()
                .filter(handler -> handler instanceof YCListener)
                .map(handler -> (YCListener)handler)
                .collect(Collectors.toList());
    }

    /**
     * @return A list of all reporters.
     */
    public List<YCReporter> getReporters() {
        return handlers.stream()
                .filter(handler -> handler instanceof YCReporter)
                .map(handler -> (YCReporter)handler)
                .collect(Collectors.toList());
    }

    /**
     * Gets a handler given an ID.
     * @param handlerID The handler ID.
     * @return The handler, null if not found.
     */
    public YCHandler getHandler(int handlerID) {
        return handlers.stream().filter(handler -> handler.getID() == handlerID).findFirst().orElse(null);
    }

    /**
     * Add a new handler to the list of handlers.
     * @param handler The handler to add.
     */
    public void addHandler(YCHandler handler) {
        if (!handlers.contains(handler)) {
            logger.fine(String.format("Adding new handler: %s.", handler));

            if (handler instanceof YCReporter) jServer.eventBus.post(new ReporterAddedEvent((YCReporter)handler));

            handlers.add(handler);
            ++currentHandlerID;
        }
    }

    public void removeHandler(YCHandler handler) {
        if (handlers.contains(handler)) {
            logger.fine(String.format("Removing handler: %s.", handler));

            if (handler instanceof YCReporter) jServer.eventBus.post(new ReporterRemovedEvent((YCReporter)handler));

            handlers.remove(handler);
        }
    }

    /**
     * @return The current handler ID.
     */
    public int getCurrentHandlerID() {
        return currentHandlerID;
    }
}
