package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.tracking.IResolver;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.tracking.ITracker;
import ez.pogdog.yescom.tracking.resolvers.MultiResolver;
import ez.pogdog.yescom.tracking.resolvers.QuickResolver;
import ez.pogdog.yescom.tracking.trackers.BasicTracker;
import ez.pogdog.yescom.tracking.trackers.PanicTracker;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.*;

/*
 * Handles assigning trackers, manages all running trackers, only handles CURRENT tracking
 */
public class TrackingHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final List<TrackedPlayer> onlinePlayers = new ArrayList<>();
    private final List<TrackedPlayer> loggedPlayers = new ArrayList<>();
    private final Map<Long, ITracker> trackers = new HashMap<>();
    private final List<IResolver> resolvers = new ArrayList<>();

    private long trackerID;

    public TrackingHandler() {
        trackerID = 0L;
    }

    @Override
    public void onTick() {
        new ArrayList<>(onlinePlayers).forEach(trackedPlayer -> {
            /*
            if (trackedPlayer.getCurrentTracker() == null) trackBasic(trackedPlayer); // Assign basic tracker at first
            trackedPlayer.getCurrentTracker().onTick();
             */

            // FIXME: Handle this in the trackers?
            Optional<TrackedPlayer> overLapping = onlinePlayers.stream()
                    .filter(trackedPlayer1 -> trackedPlayer != trackedPlayer1 &&
                            trackedPlayer1.getRenderDistance().contains(trackedPlayer.getRenderDistance().getCenterPosition()))
                    .findAny();

            if (overLapping.isPresent()) {
                yesCom.logger.debug(String.format("%s and %s are overlapping, removing one.", trackedPlayer, overLapping.get()));

                if (trackedPlayer.getTrackingSince() > overLapping.get().getTrackingSince()) { // Keep the older one
                    removeTrackedPlayer(overLapping.get());
                    new HashMap<>(trackers).forEach((trackerID, tracker) -> {
                        if (tracker.getTrackedPlayer().equals(overLapping.get())) removeTracker(tracker);
                    });
                } else {
                    removeTrackedPlayer(trackedPlayer);
                    new HashMap<>(trackers).forEach((trackerID, tracker) -> {
                        if (tracker.getTrackedPlayer().equals(trackedPlayer)) removeTracker(tracker);
                    });
                }
            }
        });
        new ArrayList<>(resolvers).forEach(resolver -> {
            if (resolver.isComplete()) {
                yesCom.logger.info(String.format("Found render distance %s in %d checks.", resolver.getRenderDistance(),
                        resolver.getChecksTaken()));
                resolvers.remove(resolver);
            }
        });
        new ArrayList<>(trackers.values()).forEach(tracker ->{
            tracker.onTick();
            if(yesCom.handler != null) {
                yesCom.handler.onTrackerUpdate(tracker);
            }
        });
    }

    @Override
    public void onExit() {
    }

    private void onRenderResolve(QuickResolver quickResolver) {
        RenderDistance renderDistance = quickResolver.getRenderDistance();
        Dimension dimension = quickResolver.getDimension();

        if (onlinePlayers.stream().noneMatch(trackedPlayer -> trackedPlayer.getDimension() == dimension &&
                trackedPlayer.getRenderDistance().contains(renderDistance.getCenterPosition()))) {

            TrackedPlayer player = yesCom.dataHandler.newTrackedPlayer(new TrackedPlayer.TrackingData(yesCom.dataHandler), renderDistance, dimension, false,
                    System.currentTimeMillis());
            addTrackedPlayer(player);
            trackBasic(player);

        } else {
            onlinePlayers.forEach(trackedPlayer -> {
                if (trackedPlayer.getDimension() == dimension && trackedPlayer.getRenderDistance().contains(renderDistance.getCenterPosition())) {
                    trackedPlayer.setRenderDistance(renderDistance);
                    trackBasic(trackedPlayer);
                }
            });
        }
    }

    public void onPlayerJoin(UUID uuid) {
        loggedPlayers.forEach(loggedPlayer -> {
            if (loggedPlayer.getPossiblePlayer(uuid) > 0) {
                yesCom.queryHandler.addQuery(new IsLoadedQuery(loggedPlayer.getRenderDistance().getCenterPosition(),
                        loggedPlayer.getDimension(), IQuery.Priority.MEDIUM, yesCom.configHandler.TYPE, (query, result) -> {
                    if (result == IsLoadedQuery.Result.LOADED) {
                        loggedPlayer.putPossiblePlayer(uuid, loggedPlayer.getPossiblePlayer(uuid) + 3);
                        loggedPlayer.setLoggedOut(false);

                        yesCom.logger.info(String.format("Reassigning %s due to potential login.", loggedPlayer));

                        loggedPlayer.setLoggedOut(false);
                        removeLoggedPlayer(loggedPlayer);
                        addTrackedPlayer(loggedPlayer);

                        quickResolve(loggedPlayer.getRenderDistance().getCenterPosition(), loggedPlayer.getDimension(), 4);
                    }
                }));
            }
        });
    }

    public void onLoadedChunk(ChunkPosition loadedChunk, Dimension dimension) {
        boolean inPlayerRender = onlinePlayers.stream()
                .anyMatch(trackedPlayer -> trackedPlayer.getDimension() == dimension && trackedPlayer.getRenderDistance().contains(loadedChunk));
        boolean isResolving = resolvers.stream()
                .anyMatch(resolver -> resolver.getDimension() == dimension && resolver.getRenderDistance() != null &&
                        resolver.getRenderDistance().contains(loadedChunk));
        if (!inPlayerRender && !isResolving) quickResolve(loadedChunk, dimension, 4);
    }

    /**
     * Good for resolving render distances quickly.
     * @param initialLoaded The initial loaded position.
     * @param dimension The dimension.
     * @param maxPhase The maximum phase, less means it resolves faster but has more error.
     */
    public void quickResolve(ChunkPosition initialLoaded, Dimension dimension, int maxPhase) {
        yesCom.logger.debug(String.format("Resolving quick for initial %s, dim %s, max phase: %d.", initialLoaded,
                dimension, maxPhase));
        QuickResolver resolver = new QuickResolver(initialLoaded, dimension, maxPhase, this::onRenderResolve);
        resolvers.add(resolver);
    }

    public MultiResolver resolveMulti(ChunkPosition initialLoaded, Dimension dimension) {
        yesCom.logger.debug(String.format("Resolving multi for initial %s, dim %s.", initialLoaded, dimension));
        return null;
    }

    public synchronized void trackBasic(TrackedPlayer trackedPlayer) {
        yesCom.logger.debug(String.format("Starting basic tracker for %s.", trackedPlayer));
        new HashMap<>(trackers).forEach((trackerID, tracker) -> {
            if (tracker.getTrackedPlayer().equals(trackedPlayer)) removeTracker(tracker);
        });
        addTracker(new BasicTracker(trackerID++, trackedPlayer));
    }

    public synchronized void trackPanic(TrackedPlayer trackedPlayer) {
        yesCom.logger.debug(String.format("Starting panic tracker for %s.", trackedPlayer));
        new HashMap<>(trackers).forEach((trackerID, tracker) -> {
            if (tracker.getTrackedPlayer().equals(trackedPlayer)) removeTracker(tracker);
        });
        addTracker(new PanicTracker(trackerID++, trackedPlayer));
        /*
        if (trackedPlayer.getCurrentTracker() != null) trackedPlayer.getCurrentTracker().onLost();
         */
    }

    /**
     * Returns a tracker based on its tracker ID.
     * @param trackerID The tracker ID.
     * @return The tracker, null if no tracker by that ID was found.
     */
    public ITracker getTracker(long trackerID) {
        return trackers.get(trackerID);
    }

    /**
     * Returns the tracker currently tracking a player in question.
     * @param trackedPlayer The tracked player.
     * @return The tracker, null if the player is not being tracked currently.
     */
    public ITracker getTracker(TrackedPlayer trackedPlayer) {
        return trackers.entrySet().stream()
                .filter(entry -> entry.getValue().getTrackedPlayer().equals(trackedPlayer))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    public void addTracker(ITracker tracker) {
        if (!trackers.containsKey(tracker.getTrackerID()) && !trackers.containsValue(tracker)) {
            trackers.put(tracker.getTrackerID(), tracker);

            if (yesCom.handler != null) yesCom.handler.onTrackerAdded(tracker);
        }
    }

    public void removeTracker(ITracker tracker) {
        if (trackers.containsKey(tracker.getTrackerID())) {
            tracker.onLost();
            trackers.remove(tracker.getTrackerID());

            if (yesCom.handler != null) yesCom.handler.onTrackerRemoved(tracker);
        }
    }

    public void handleLogout(TrackedPlayer trackedPlayer) {
        if (!trackedPlayer.isLoggedOut()) {
            trackedPlayer.setLoggedOut(true);
            removeTrackedPlayer(trackedPlayer);
            addLoggedPlayer(trackedPlayer);

            yesCom.connectionHandler.recentLeaves.forEach((uuid, time) -> trackedPlayer.putPossiblePlayer(uuid,
                    trackedPlayer.getPossiblePlayer(uuid) + 1));
            yesCom.logger.info(String.format("%s has logged out, most probable: %s.", trackedPlayer,
                    yesCom.connectionHandler.getNameForUUID(trackedPlayer.getBestPossiblePlayer())));
        }
    }

    public boolean isChunkKnown(ChunkPosition chunkPosition, Dimension dimension) {
        return onlinePlayers.stream()
                .anyMatch(trackedPlayer ->trackedPlayer.getDimension() == dimension && trackedPlayer.getRenderDistance().contains(chunkPosition));
    }

    public void addTrackedPlayer(TrackedPlayer trackedPlayer) {
        if (!onlinePlayers.contains(trackedPlayer)) onlinePlayers.add(trackedPlayer);

    }

    public void removeTrackedPlayer(TrackedPlayer trackedPlayer) {
        onlinePlayers.remove(trackedPlayer);
    }

    public void addLoggedPlayer(TrackedPlayer loggedPlayer) {
        if (!loggedPlayers.contains(loggedPlayer)) loggedPlayers.add(loggedPlayer);
    }

    public void removeLoggedPlayer(TrackedPlayer loggedPlayer) {
        loggedPlayers.remove(loggedPlayer);
    }
}
