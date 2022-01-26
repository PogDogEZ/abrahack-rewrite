package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.IHandler;
import ez.pogdog.yescom.query.queries.IsLoadedQuery;
import ez.pogdog.yescom.util.Dimension;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Handles assigning trackers, manages all running trackers.
 */
public class TrackingHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final Map<Dimension, Map<Long, ITracker>> trackers = new HashMap<>();
    private final Map<Dimension, Float> queryRates = new HashMap<>();

    private long trackerID;

    public TrackingHandler() {
        for (Dimension dimension : Dimension.values()) trackers.put(dimension, new ConcurrentHashMap<>());

        trackerID = 0L;
    }

    @Override
    public void tick() {
        // Set up the query rates for this tick
        queryRates.clear();
        for (Dimension dimension : Dimension.values()) queryRates.put(dimension, getMaximumQueryRate(dimension));

        // Update trackers
        trackers.forEach((dimension, dimensionTrackers) -> dimensionTrackers.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getValue().getPriority().ordinal())) // Sort by priority
                .forEach(entry -> entry.getValue().onTick()));
    }

    @Override
    public void exit() {
    }

    /* ------------------------ Requests ------------------------ */

    public boolean requestQuery(ITracker tracker, IsLoadedQuery query) {
        return false;
    }

    public boolean requestResolver(ITracker tracker, IResolver resolver) {
        return false;
    }

    /* ------------------------ Setters and getters ------------------------ */

    /**
     * Estimates the maximum query throughput of the given dimension.
     * @param dimension The dimension.
     * @return The estimated maximum query throughput.
     */
    public float getMaximumQueryRate(Dimension dimension) {
        if (yesCom.configHandler.TYPE == IsLoadedQuery.Type.DIGGING) {
            return 0.0f; // TODO: Digging
        } else {
            return (float)yesCom.configHandler.LOADED_QUERIES_PER_TICK * 20.0f * yesCom.invalidMoveHandler.getAvailableAccounts(dimension);
        }
    }
}
