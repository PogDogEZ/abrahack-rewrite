package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class QueryHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final List<IQuery> waiting = new ArrayList<>();
    private final Deque<IQuery> ticking = new ConcurrentLinkedDeque<>();
    private final Deque<IQuery> finished = new ConcurrentLinkedDeque<>();

    private final List<Float> queryRateSamples = new ArrayList<>();

    private final Map<Integer, Float> queryTicks = new HashMap<>();

    private int currentQueries;
    private long lastQueryUpdate;

    public QueryHandler() {
        currentQueries = 0;
        lastQueryUpdate = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        queryTicks.forEach((channel, ticks) -> queryTicks.put(channel, ticks - 1.0f));

        if (System.currentTimeMillis() - lastQueryUpdate > 250) {
            queryRateSamples.add(currentQueries / 0.25f);
            while (queryRateSamples.size() > 10) queryRateSamples.remove(0);
            lastQueryUpdate = System.currentTimeMillis();
            currentQueries = 0;
        }

        int index = 0;
        while (index < waiting.size()) {
            IQuery query;
            synchronized (this) {
                query = waiting.get(index);
            }

            switch (query.handle()) {
                case AWAIT: {
                    ++index;
                    break;
                }
                case START: {
                    int channel = query.getChannel();
                    float currentWeight = queryTicks.getOrDefault(channel, 0.0f);

                    if (currentWeight > 1.0f) {
                        ++index;
                        continue;
                    }

                    ++currentQueries;

                    float queryWeight = query.getWeight();

                    // Safeguard against this
                    if (Float.isFinite(queryWeight)) queryTicks.put(channel, currentWeight + queryWeight);

                    synchronized (this) {
                        waiting.remove(query);
                    }
                    ticking.add(query);
                    break;
                }
                case REMOVE: {
                    synchronized (this) {
                        waiting.remove(query);
                    }
                    break;
                }
            }
        }

        List<IQuery> toRemove = new ArrayList<>();

        for (IQuery query: ticking) {
            switch (query.tick()) {
                case AWAIT: {
                    break;
                }
                case REMOVE: {
                    finished.add(query);
                    toRemove.add(query);
                    break;
                }
            }
        }

        toRemove.forEach(ticking::remove);

        while (finished.size() > yesCom.configHandler.MAX_FINISHED_CACHE) finished.removeFirst();

        /*
        yesCom.logger.infoDisappearing(String.format("wait/tick: %d/%d, tslp: %dms.",
                yesCom.queryHandler.getWaitingSize(), yesCom.queryHandler.getTickingSize(),
                yesCom.connectionHandler.getTimeSinceLastPacket()));
         */
    }

    @Override
    public void onExit() {
    }

    public synchronized void addQuery(IQuery query) {
        if (!query.isFinished()) {
            int priorityOrdinal = query.getPriority().ordinal();

            for (int index = 0; index < waiting.size(); ++index) {
                if (waiting.get(index).getPriority().ordinal() < priorityOrdinal) {
                    waiting.add(index, query);
                    return;
                }
            }

            waiting.add(query);
        }
    }

    public int getWaitingSize() {
        return waiting.size();
    }

    public int getTickingSize() {
        return ticking.size();
    }

    public float getQueriesPerSecond() {
        return queryRateSamples.stream().reduce(Float::sum).orElse(0.0f) / Math.max(1.0f, queryRateSamples.size());
    }

    public List<IQuery> getFinished() {
        return new ArrayList<>(finished);
    }
}
