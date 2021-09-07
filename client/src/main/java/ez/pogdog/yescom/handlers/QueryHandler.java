package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class QueryHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final Deque<IQuery> waiting = new ConcurrentLinkedDeque<>();
    private final Deque<IQuery> ticking = new ConcurrentLinkedDeque<>();
    private final Deque<IQuery> finished = new ConcurrentLinkedDeque<>();

    private final List<IQuery> cached = new ArrayList<>();

    private float queryTicks;

    public QueryHandler() {
        queryTicks = 0.0f;
    }

    @Override
    public void onTick() {
        queryTicks = Math.max(0.0f, queryTicks - 1.0f);

        List<IQuery> toRemove = new ArrayList<>();

        for (IQuery query : waiting) {
            switch (query.handle()) {
                case AWAIT: {
                    break;
                }
                case START: {
                    queryTicks += query.getWeight();
                    ticking.add(query);
                }
                case REMOVE: {
                    toRemove.add(query);
                    break;
                }
            }
            if (queryTicks > 1.0f) break;
        }

        toRemove.forEach(waiting::remove);
        toRemove.clear();

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
    }

    @Override
    public void onExit() {
    }

    public void addQuery(IQuery query) {
        if (!query.isFinished()) waiting.add(query);
    }

    public int getWaitingSize() {
        return waiting.size();
    }

    public int getTickingSize() {
        return ticking.size();
    }

    public List<IQuery> getFinished() {
        return new ArrayList<>(finished);
    }

    public void cacheCurrent() {
        cached.addAll(waiting);
        waiting.clear();
    }

    public void unCacheCurrent() {
        waiting.addAll(cached);
        cached.clear();
    }
}
