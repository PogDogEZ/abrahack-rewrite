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
import ez.pogdog.yescom.tracking.trackers.adaptive.AdaptiveTracker;
import ez.pogdog.yescom.tracking.trackers.PanicTracker;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.*;

/*
 * Handles assigning trackers, manages all running trackers, only handles CURRENT tracking
 */
public class TrackingHandler implements IHandler { // FIXME: Holy shit PLEASE overhaul this fucking class and the tracking system in general

    private final YesCom yesCom = YesCom.getInstance();

    public final Random random = new Random();

    private final List<TrackedPlayer> onlinePlayers = new ArrayList<>();
    private final List<TrackedPlayer> loggedPlayers = new ArrayList<>();
    private final Map<Long, ITracker> trackers = new HashMap<>();
    private final List<IResolver> resolvers = new ArrayList<>();

    private final Map<TrackedPlayer, Long> recentLogouts = new HashMap<>();
    private final List<TrackedPlayer> restartAffected = new ArrayList<>();

    private final List<UUID> handlingLogins = new ArrayList<>();

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
                yesCom.logger.fine(String.format("%s and %s are overlapping, removing one.", trackedPlayer, overLapping.get()));

                if (trackedPlayer.getTrackingSince() < overLapping.get().getTrackingSince()) { // Keep the older one
                    removeTrackedPlayer(overLapping.get());
                    new HashMap<>(trackers).forEach((trackerID, tracker) -> {
                        if (tracker.getTrackedPlayers().equals(overLapping.get())) removeTracker(tracker);
                    });
                } else {
                    removeTrackedPlayer(trackedPlayer);
                    new HashMap<>(trackers).forEach((trackerID, tracker) -> {
                        if (tracker.getTrackedPlayers().equals(trackedPlayer)) removeTracker(tracker);
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
            // Don't update the tracker if it has just been removed
            if (trackers.containsValue(tracker) && yesCom.ycHandler != null) yesCom.ycHandler.onTrackerUpdate(tracker);
        });

        boolean serverDown = !yesCom.connectionHandler.isConnected();

        new HashMap<>(recentLogouts).forEach((trackedPlayer, logoutTime) -> {
            if (!serverDown && System.currentTimeMillis() - logoutTime > yesCom.configHandler.MAX_RESTART_CHECK_TIME) {
                recentLogouts.remove(trackedPlayer);
            } else if (serverDown) {
                restartAffected.add(trackedPlayer);
            }
        });

        if (serverDown) {
            recentLogouts.clear();
        } else if (!restartAffected.isEmpty()) {
            restartAffected.forEach(trackedPlayer -> {
                yesCom.logger.info(String.format("Reassigning %s due to restart.", trackedPlayer));

                trackedPlayer.setLoggedOut(false);
                removeLoggedPlayer(trackedPlayer);
                addTrackedPlayer(trackedPlayer);

                quickResolve(trackedPlayer.getRenderDistance().getCenterPosition(), trackedPlayer.getDimension(), 4);
            });
            restartAffected.clear();
        }
    }

    @Override
    public void onExit() {
    }

    /* ----------------------------- Events ----------------------------- */

    /**
     * Called when a render distance has been resolved.
     * @param quickResolver The resolver that resolved the render distance.
     */
    @Deprecated
    public void onRenderResolve(QuickResolver quickResolver) {
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

    /**
     * Called when a player joins the game.
     * @param uuid The UUID of the player that joined.
     */
    public void onPlayerJoin(UUID uuid) {
        if (loggedPlayers.isEmpty()) return; // No one to check for

        synchronized (this) {
            if (handlingLogins.contains(uuid)) return;
            handlingLogins.add(uuid);
        }

        loggedPlayers.forEach(loggedPlayer -> {
            if (loggedPlayer.getPossiblePlayer(uuid) > 0) {
                yesCom.queryHandler.addQuery(new IsLoadedQuery(loggedPlayer.getRenderDistance().getCenterPosition(),
                        loggedPlayer.getDimension(), IQuery.Priority.MEDIUM, yesCom.configHandler.TYPE, (query, result) -> {
                    if (result == IsLoadedQuery.Result.LOADED) {
                        loggedPlayer.putPossiblePlayer(uuid, loggedPlayer.getPossiblePlayer(uuid) + 3);
                        loggedPlayer.setLoggedOut(false);

                        recentLogouts.remove(loggedPlayer);

                        yesCom.logger.info(String.format("Reassigning %s due to potential login for player %s.", loggedPlayer,
                                yesCom.connectionHandler.getNameForUUID(uuid)));

                        removeLoggedPlayer(loggedPlayer);
                        addTrackedPlayer(loggedPlayer);

                        quickResolve(loggedPlayer.getRenderDistance().getCenterPosition(), loggedPlayer.getDimension(), 4);
                    } else {
                        loggedPlayer.putPossiblePlayer(uuid, loggedPlayer.getPossiblePlayer(uuid) - 1);
                    }

                    synchronized (this) {
                        handlingLogins.remove(uuid);
                    }
                }));
            }
        });
    }

    /**
     * Called when a loaded chunk is found.
     * @param loadedChunk The position of the loaded chunk.
     * @param dimension The dimension of the loaded chunk.
     */
    public void onLoadedChunk(ChunkPosition loadedChunk, Dimension dimension) {
        boolean inPlayerRender = onlinePlayers.stream()
                .anyMatch(trackedPlayer -> trackedPlayer.getDimension() == dimension && trackedPlayer.getRenderDistance().contains(loadedChunk));
        boolean isResolving = resolvers.stream()
                .anyMatch(resolver -> resolver.getDimension() == dimension && resolver.getRenderDistance() != null &&
                        resolver.getRenderDistance().contains(loadedChunk));

        if (!inPlayerRender && !isResolving) {
            // quickResolve(loadedChunk, dimension, 4);
            TrackedPlayer player = yesCom.dataHandler.newTrackedPlayer(new TrackedPlayer.TrackingData(yesCom.dataHandler),
                    yesCom.dataHandler.newRenderDistance(loadedChunk, yesCom.configHandler.RENDER_DISTANCE, 0.0f, 0.0f),
                    dimension, false, System.currentTimeMillis());
            addTrackedPlayer(player);
            trackBasic(player);
        }
    }

    /* ----------------------------- Resolvers ----------------------------- */

    /**
     * Good for resolving render distances quickly.
     * @param initialLoaded The initial loaded position.
     * @param dimension The dimension.
     * @param maxPhase The maximum phase, less means it resolves faster but has more error.
     */
    public void quickResolve(ChunkPosition initialLoaded, Dimension dimension, int maxPhase) {
        yesCom.logger.fine(String.format("Resolving quick for initial %s, dim %s, max phase: %d.", initialLoaded,
                dimension, maxPhase));
        QuickResolver resolver = new QuickResolver(initialLoaded, dimension, maxPhase, this::onRenderResolve);
        resolvers.add(resolver);
    }

    public MultiResolver resolveMulti(ChunkPosition initialLoaded, Dimension dimension) {
        yesCom.logger.fine(String.format("Resolving multi for initial %s, dim %s.", initialLoaded, dimension));
        return null;
    }

    /* ----------------------------- Trackers ----------------------------- */

    public synchronized void trackBasic(TrackedPlayer trackedPlayer) {
        yesCom.logger.fine(String.format("Starting basic tracker for %s.", trackedPlayer));
        new HashMap<>(trackers).forEach((trackerID, tracker) -> {
            if (tracker.getTrackedPlayers().contains(trackedPlayer)) removeTracker(tracker);
        });
        addTracker(new AdaptiveTracker(trackerID++, trackedPlayer));
    }

    public synchronized void trackPanic(TrackedPlayer trackedPlayer) {
        yesCom.logger.fine(String.format("Starting panic tracker for %s.", trackedPlayer));
        new HashMap<>(trackers).forEach((trackerID, tracker) -> {
            if (tracker.getTrackedPlayers().contains(trackedPlayer)) removeTracker(tracker); // FIXME: Fix this obviously
        });
        addTracker(new PanicTracker(trackerID++, trackedPlayer));
        /*
        if (trackedPlayer.getCurrentTracker() != null) trackedPlayer.getCurrentTracker().onLost();
         */
    }

    /* ----------------------------- Trackers and tracked players ----------------------------- */

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
                .filter(entry -> entry.getValue().getTrackedPlayers().contains(trackedPlayer))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    public List<ITracker> getTrackers() {
        return new ArrayList<>(trackers.values());
    }

    public void addTracker(ITracker tracker) {
        if (!trackers.containsKey(tracker.getTrackerID()) && !trackers.containsValue(tracker)) {
            if (yesCom.ycHandler != null) yesCom.ycHandler.onTrackerAdded(tracker);

            trackers.put(tracker.getTrackerID(), tracker);
        }
    }

    public void removeTracker(ITracker tracker) {
        if (trackers.containsKey(tracker.getTrackerID())) {
            tracker.onLost();
            trackers.remove(tracker.getTrackerID());

            if (yesCom.ycHandler != null) yesCom.ycHandler.onTrackerRemoved(tracker);
        }
    }

    /**
     * Completely stops a tracker, without letting it know.
     * @param tracker The tracker.
     */
    public void untrack(ITracker tracker) {
        if (trackers.containsKey(tracker.getTrackerID())) {
            // TODO: What should we do with the tracked players, just remove them if they aren't present in any other trackers?
            trackers.remove(tracker.getTrackerID());

            if (yesCom.ycHandler != null) yesCom.ycHandler.onTrackerRemoved(tracker);
        }
    }

    public void handleLogout(TrackedPlayer trackedPlayer) {
        if (!trackedPlayer.isLoggedOut()) {
            trackedPlayer.setLoggedOut(true);
            removeTrackedPlayer(trackedPlayer);

            if (!yesCom.connectionHandler.recentLeaves.isEmpty()) {
                addLoggedPlayer(trackedPlayer);
                yesCom.connectionHandler.recentLeaves.forEach((uuid, time) -> trackedPlayer.putPossiblePlayer(uuid,
                        trackedPlayer.getPossiblePlayer(uuid) + 1));
                yesCom.logger.info(String.format("%s has logged out, most probable: %s.", trackedPlayer,
                        yesCom.connectionHandler.getNameForUUID(trackedPlayer.getBestPossiblePlayer())));
            } else {
                yesCom.logger.warning(String.format("Lost tracked player %s, no probable logouts.", trackedPlayer));
            }

            recentLogouts.put(trackedPlayer, System.currentTimeMillis());
        }
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

    /* ----------------------------- Other ----------------------------- */

    /**
     * Returns whether any of the tracked players' render distances contain a chunk position.
     * @param chunkPosition The chunk position to check for.
     * @param dimension The dimension to check in.
     * @return Whether the chunk is known.
     */
    public boolean isChunkKnown(ChunkPosition chunkPosition, Dimension dimension) {
        return onlinePlayers.stream()
                .anyMatch(trackedPlayer ->trackedPlayer.getDimension() == dimension && trackedPlayer.getRenderDistance().contains(chunkPosition));
    }
}
