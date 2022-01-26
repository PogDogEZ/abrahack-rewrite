package ez.pogdog.yescom.query;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.IHandler;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.task.ITask;
import ez.pogdog.yescom.tracking.IResolver;
import ez.pogdog.yescom.tracking.ITracker;
import ez.pogdog.yescom.util.Dimension;
import ez.pogdog.yescom.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Responsible for handling queries, including starting and limiting the number of concurrent queries.
 */
public class QueryHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final List<Pair<IQuery, Float>> waiting = new ArrayList<>();
    private final Deque<IQuery> ticking = new ConcurrentLinkedDeque<>();

    private final List<Float> queryRateSamples = new ArrayList<>();
    private final List<Float> droppedQuerySamples = new ArrayList<>();
    private final Map<Class<? extends IRequester>, Integer> waitingCount = new HashMap<>();
    private final Map<Class<? extends IRequester>, Integer> tickingCount = new HashMap<>();
    private final Map<Class<? extends IQuery>, Map<Dimension, Float>> queryTicks = new HashMap<>();
    private final Map<Class<? extends IQuery>, Map<Dimension, Float>> maxThroughPut = new HashMap<>();

    private int processedQueries;
    private int rescheduledQueries;
    private long lastQueryUpdate;
    private long lastDroppedUpdate;

    public QueryHandler() {
        processedQueries = 0;
        rescheduledQueries = 0;
        lastQueryUpdate = System.currentTimeMillis();
        lastDroppedUpdate = System.currentTimeMillis();
    }

    @Override
    public synchronized void tick() {
        waitingCount.clear(); // Clear these, the first time the getter is called, this will be computed
        tickingCount.clear();

        maxThroughPut.clear();
        for (Map.Entry<Class<? extends IQuery>, IQuery.IQueryInfo> entry : QueryRegistry.QUERIES.entrySet()) {
            for (Dimension dimension : Dimension.values())
                maxThroughPut.computeIfAbsent(entry.getKey(), value -> new HashMap<>()).put(dimension,
                        entry.getValue().getMaxThroughPut(dimension));
        }

        int timeElapsed = (int)(System.currentTimeMillis() - lastQueryUpdate);
        if (timeElapsed > 250) {
            queryRateSamples.add(Math.max(0, processedQueries) / (timeElapsed / 1000.0f));
            while (queryRateSamples.size() > 10) queryRateSamples.remove(0);

            lastQueryUpdate = System.currentTimeMillis();
            processedQueries = 0;
        }

        timeElapsed = (int)(System.currentTimeMillis() - lastDroppedUpdate);
        if (timeElapsed > 500) {
            droppedQuerySamples.add(Math.max(0, rescheduledQueries) / (timeElapsed / 1000.0f));
            while (droppedQuerySamples.size() > 10) droppedQuerySamples.remove(0);

            lastDroppedUpdate = System.currentTimeMillis();
            rescheduledQueries = 0;
        }

        List<Pair<IQuery, Float>> toRemove = new ArrayList<>();
        for (Pair<IQuery, Float> pair : waiting) {
            IQuery query = pair.getFirst();

            Map<Dimension, Float> ticks = queryTicks.computeIfAbsent(query.getClass(), value -> new ConcurrentHashMap<>());
            float collectiveWeight = ticks.computeIfAbsent(query.getDimension(), value -> 0.0f);
            float individualWeight = QueryRegistry.QUERIES.get(query.getClass()).getWeight(query, query.getDimension());

            if (collectiveWeight >= 1.0f) continue; // Done all we can this tick
            switch (query.handle()) {
                case AWAIT: {
                    continue;
                }
                case START: {
                    ++processedQueries;
                    ticking.add(query);
                }
                case REMOVE: {
                    toRemove.add(pair);
                    break;
                }
            }

            ticks.put(query.getDimension(), collectiveWeight + individualWeight);
        }
        waiting.removeAll(toRemove);

        ticking.forEach(query -> {
            switch (query.tick()) {
                case AWAIT: {
                    break;
                }
                case REMOVE: {
                    ticking.remove(query);
                    break;
                }
            }
        });

        // Decrement all the ticks by 1, as this tick has passed
        queryTicks.forEach((queryClass, ticks) -> ticks.forEach((channel, weight) -> ticks.put(channel, Math.max(0.0f, weight - 1.0f))));
    }

    @Override
    public void exit() {
    }

    /**
     * Adds a query to the queue.
     * @param query The query to add.
     * @param reschedule Whether to reschedule the query, this will insert it at the beginning of the queue.
     */
    public synchronized void addQuery(IQuery query, boolean reschedule) {
        if (!query.isFinished()) {
            if (reschedule) {
                --processedQueries; // Don't count this query as processed
                ++rescheduledQueries;
                waiting.add(0, new Pair<>(query, 0.0f)); // Put it to the front of the queries
                return;
            }

            /*
            float requesterWeight = 1.0f;
            if (query.getRequester() != null) {
                Class<? extends IRequester> requesterClass = query.getRequester().getClass();

                requesterWeight = Math.max(1.0f, getWaitingSize(requesterClass)) / Math.max(1.0f, (float)waiting.size());
                if (ITask.class.isAssignableFrom(requesterClass)) {
                    requesterWeight *= yesCom.configHandler.TASK_QUERY_WEIGHT;
                } else if (IResolver.class.isAssignableFrom(requesterClass)) {
                    requesterWeight *= yesCom.configHandler.RESOLVER_QUERY_WEIGHT;
                } else if (ITracker.class.isAssignableFrom(requesterClass)) {
                    requesterWeight *= yesCom.configHandler.TRACKER_QUERY_WEIGHT;
                }
            }
             */

            float priority = query.getPriority().ordinal();
            for (int index = 0; index < waiting.size(); ++index) { // Insert the query into the list based on its priority
                if (waiting.get(index).getSecond() < priority) {
                    waiting.add(index, new Pair<>(query, priority));
                    return;
                }
            }
            waiting.add(new Pair<>(query, priority));
        }
    }

    /**
     * Adds a query to the queue.
     * @param query The query to add.
     */
    public void addQuery(IQuery query) {
        addQuery(query, false);
    }

    /**
     * Counts the number of queries waiting to be processed.
     * @param requesterClass The class of the requester, for those queries.
     * @return The number of queries waiting to be processed.
     */
    public int getWaitingSize(Class<? extends IRequester> requesterClass) {
        return waitingCount.computeIfAbsent(requesterClass,
                value -> (int)waiting.stream()
                        .filter(query -> query.getFirst().getRequester().getClass() == requesterClass)
                        .count());
    }

    /**
     * @return The total number of queries currently being processed.
     */
    public int getWaitingSize() {
        return waiting.size();
    }

    /**
     * Counts the number of queries currently being processed.
     * @param requesterClass The class of the requester, for those queries.
     * @return The number of queries currently being processed.
     */
    public int getTickingSize(Class<? extends IRequester> requesterClass) {
        return tickingCount.computeIfAbsent(requesterClass,
                value -> (int)ticking.stream()
                        .filter(query -> query.getRequester().getClass() == requesterClass)
                        .count());
    }

    /**
     * @return The total number of queries currently being processed.
     */
    public int getTickingSize() {
        return ticking.size();
    }

    public float getMaxThroughPut(Class<? extends IQuery> queryClass, Dimension dimension) {
        return maxThroughPut.get(queryClass).get(dimension);
    }

    public void putMaxThroughPut(Class<? extends IQuery> queryClass, Dimension dimension, float maxThroughPut) {
        this.maxThroughPut.get(queryClass).put(dimension, maxThroughPut);
    }

    /**
     * @return The number of queries being processed per second.
     */
    public float getQueryRate() {
        return queryRateSamples.stream().reduce(Float::sum).orElse(0.0f) / Math.max(1.0f, queryRateSamples.size());
    }

    /**
     * @return The number of queries being dropped per second.
     */
    public float getDroppedQueries() {
        return droppedQuerySamples.stream().reduce(Float::sum).orElse(0.0f) / Math.max(1.0f, droppedQuerySamples.size());
    }
}
