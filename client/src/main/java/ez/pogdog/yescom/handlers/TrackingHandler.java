package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.tracking.ITracker;
import ez.pogdog.yescom.tracking.Tracker;
import ez.pogdog.yescom.tracking.TrackedPlayer;
import ez.pogdog.yescom.tracking.algorithm.TrackingAlgorithm;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.*;

/*
 * Handles assigning trackers, manages all running trackers, only handles CURRENT tracking
 */
public class TrackingHandler implements IHandler {

    private int TICK_COUNT = 0;

    private final ArrayList<Tracker> activeTrackers = new ArrayList<>();
    /**
     * We add lost trackers to this ArrayList to prevent deleting useful data due to player logging out for short period of time.
     * If the tracker remains lost for over 24 hours it is turned into a TrackedPlayer.
     */
    private final ArrayList<Tracker> lostTrackers = new ArrayList<>();

    private final ArrayList<TrackedPlayer> trackedPlayers = new ArrayList<>();

    private final HashMap<ITracker, List<UUID>> joinListeners = new HashMap<>();

    private final HashMap<UUID, Integer> joinsList = new HashMap<>();
    private final HashMap<UUID, Integer> leavesList = new HashMap<>();

    public final int TICK_DAY  = 1_728_000;
    public final int TICK_HOUR = 72_000;
    public final int TICK_MIN  = 1_200;
    public final int TICK_SEC  = 20;

    /* ------------------------ Implementations ------------------------ */

    @Override
    public void onTick() {
        activeTrackers.forEach(tracker -> {
            TrackerTickResult tickResult = tracker.onTick();
            switch (tickResult) {
                case Good:
                    break;
                case Lost:
                    /*
                    activeTrackers.remove(tracker);
                    if (!tracker.getPossiblePlayers().isEmpty()) { // Not much we can do if we don't know who the player could be
                        lostTrackers.add(tracker);
                        joinListeners.put(tracker, tracker.getPossiblePlayers());
                    }
                     */
                    break;
            }
        });

        lostTrackers.forEach(tracker -> {
            switch (tracker.onTick()) {
                case Good:
                    lostTrackers.remove(tracker);
                    activeTrackers.add(tracker);
                    break;
                case Lost:
                    if (tracker.getLostTicks() >= TICK_DAY) {
                        //TODO Add trail and activity data to database.
                        lostTrackers.remove(tracker);
                        trackedPlayers.add(new TrackedPlayer(tracker.getPosition()));
                    }
            }
        });

        trackedPlayers.forEach(tracker -> {
            if (tracker.onTick() == TrackerTickResult.Good) {
                trackedPlayers.remove(tracker);
                continueTracker(tracker, tracker.getDimension());
            }
        });

        for (Map.Entry<UUID, Integer> entry : joinsList.entrySet()) {
            if (TICK_COUNT - entry.getValue() >= YesCom.getInstance().configHandler.JOIN_RECORD_TIMER)
                joinsList.remove(entry.getKey());
        }

        for (Map.Entry<UUID, Integer> entry : leavesList.entrySet()) {
            if (TICK_COUNT - entry.getValue() >= YesCom.getInstance().configHandler.LEAVE_RECORD_TIMER)
                leavesList.remove(entry.getKey());
        }

        ++TICK_COUNT;
    }

    @Override
    public void onExit() {
        activeTrackers.forEach(tracker -> {
            trackedPlayers.add(new TrackedPlayer(tracker.getPosition()));
        });

        lostTrackers.forEach(tracker -> {
            trackedPlayers.add(new TrackedPlayer(tracker.getPosition()));
        });

        //TODO serialize data and save it to "data/trackers"
    }

    /* ------------------------ Tracking Methods ---------------------- */

    public void addTracker(ChunkPosition chunk, Dimension dimension) {
        activeTrackers.add(new Tracker(chunk, dimension));
        YesCom.getInstance().logger.info(String.format("Added tracker at %s in %s", chunk.toString(), dimension.toString()));
    }

    public void addTracker(Tracker tracker) {
        activeTrackers.add(tracker);
    }

    public void continueTracker(TrackedPlayer trackedPlayer, Dimension dimension) {
        activeTrackers.add(new Tracker(trackedPlayer, dimension));
    }

    /* ------------------------ Public Methods ------------------------ */

    //TODO call these methods when a player joins/leaves the game
    public void onPlayerJoin(UUID uuid) {
        joinsList.put(uuid, TICK_COUNT);

        for (Map.Entry<ITracker, List<UUID>> entry : joinListeners.entrySet()) {
            if (entry.getValue().contains(uuid))
                entry.getKey().onPossibleJoin(uuid);
        }
    }

    public void onPlayerLeave(UUID uuid) {
        leavesList.put(uuid, TICK_COUNT);
    }

    public List<UUID> getJoinRecords(int timeLimit) {
        List<UUID> joins = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : joinsList.entrySet()) {
            if (timeLimit >= TICK_COUNT - entry.getValue())
                joins.add(entry.getKey());
        }

        return joins;
    }

    public List<UUID> getLeaveRecords(int timeLimit) {
        List<UUID> leaves = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : leavesList.entrySet()) {
            if (timeLimit >= TICK_COUNT - entry.getValue())
                leaves.add(entry.getKey());
        }

        return leaves;
    }

    public enum TrackerTickResult {
        Good, Lost;
    }
}
