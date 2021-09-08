package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class QueryHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();
    private final ConfigHandler configHandler = yesCom.configHandler;

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

        for (IQuery query : sortQueue(waiting)) {
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

        for (IQuery query: sortQueue(ticking)) {
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

    private Deque<IQuery> sortQueue(Deque<IQuery> queries) {
        Deque<IQuery> userPriorityQueue = new ConcurrentLinkedDeque<>();
        Deque<IQuery> highPriorityQueue = new ConcurrentLinkedDeque<>();
        Deque<IQuery> mediumPriorityQueue = new ConcurrentLinkedDeque<>();
        Deque<IQuery> lowPriorityQueue = new ConcurrentLinkedDeque<>();

        for (IQuery query : queries) {
            switch (query.getPriority()) {
                case USER:
                    userPriorityQueue.add(query);
                    break;
                case HIGH:
                    highPriorityQueue.add(query);
                    break;
                case MEDIUM:
                    mediumPriorityQueue.add(query);
                    break;
                case LOW:
                    lowPriorityQueue.add(query);
                    break;
            }
        }

        Deque<IQuery> prioritizedQueue = new ConcurrentLinkedDeque<>();

        while (!userPriorityQueue.isEmpty() || !highPriorityQueue.isEmpty() ||
                !mediumPriorityQueue.isEmpty() || !lowPriorityQueue.isEmpty()) {
            float userPriority   = (float) (userPriorityQueue.size() * configHandler.USER_MULTIPLIER);
            float highPriority   = (float) (highPriorityQueue.size() * configHandler.HIGH_MULTIPLIER);
            float mediumPriority = (float) (mediumPriorityQueue.size() * configHandler.MEDIUM_MULTIPLIER);
            float lowPriority    = (float) (lowPriorityQueue.size() * configHandler.LOW_MULTIPLIER);

            // Gets the highest value out of priorities
            float max = Math.max(userPriority, Math.max(highPriority, Math.max(mediumPriority, lowPriority)));

            if (max == highPriority) {
                prioritizedQueue.addLast(highPriorityQueue.poll());
            } else if (max == mediumPriority) {
                prioritizedQueue.addLast(mediumPriorityQueue.poll());
            } else if (max == lowPriority) {
                prioritizedQueue.addLast(lowPriorityQueue.poll());
            } else {
                prioritizedQueue.addLast(userPriorityQueue.poll());
            }
        }

        return prioritizedQueue;
    }
}
