package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.tracking.resolvers.DimChangeResolver;
import ez.pogdog.yescom.tracking.resolvers.QuickResolver;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrackedPlayer {

    private final YesCom yesCom = YesCom.getInstance();

    private final Map<UUID, Integer> possiblePlayers = new HashMap<>();
    private final List<ChunkPosition> previousPositions = new ArrayList<>();
    private final List<Float> previousSpeeds = new ArrayList<>();

    private final int trackedPlayerID;
    private final long foundAt;

    private ChunkPosition possiblePosition;
    private RenderDistance renderDistance;
    private Dimension dimension;

    private float speedX;
    private float speedZ;

    private boolean currentlyChecking;
    private int checkingPositions;
    private IResolver currentResolver;
    private ITracker currentTracker;

    public TrackedPlayer(int trackedPlayerID, ChunkPosition possiblePosition, RenderDistance renderDistance, Dimension dimension) {
        this.trackedPlayerID = trackedPlayerID;
        this.possiblePosition = possiblePosition;
        this.renderDistance = renderDistance;
        this.dimension = dimension;

        foundAt = System.currentTimeMillis();

        currentResolver = null;
        currentTracker = null;
    }

    @Override
    public String toString() {
        return String.format("TrackedPlayer(ID=%d, state=%s, dimension=%s, position=%s)", trackedPlayerID, currentState,
                dimension, possiblePosition);
    }

    private void startPositionCheck(LoadedCheck loadedCheck) {
        yesCom.logger.debug(String.format("Loaded check state: %s.", loadedCheck));

        float speedX = this.speedX * (System.currentTimeMillis() - lastResolve) / 1000.0f;
        float speedZ = this.speedZ * (System.currentTimeMillis() - lastResolve) / 1000.0f;

        List<ChunkPosition> chunkPositions = new ArrayList<>();
        switch (loadedCheck) {
            default:
            case AHEAD: {
                // chunkPositions.add(possiblePosition);
                chunkPositions.add(possiblePosition.add((int)speedX, (int)speedZ));
                break;
            }
            case STATIONARY: { // Did we guess their speed wrong?
                chunkPositions.add(possiblePosition);
                break;
            }
            case AROUND: { // We're panicking a little more right now since we may have lost them, so send more checks faster
                for (float multiplier = 0.5f; multiplier < 5.0f; multiplier += 0.5f) { // FUCK geza and WALLHACKS
                    float extrapolateX = yesCom.configHandler.RENDER_DISTANCE * multiplier;
                    float extrapolateZ = yesCom.configHandler.RENDER_DISTANCE * multiplier;

                    chunkPositions.add(possiblePosition.add((int)extrapolateX, (int)extrapolateZ));
                    chunkPositions.add(possiblePosition.subtract((int)extrapolateX, (int)extrapolateZ));
                    chunkPositions.add(possiblePosition.add((int)extrapolateZ, (int)extrapolateX));
                    chunkPositions.add(possiblePosition.subtract((int)extrapolateZ, (int)extrapolateX));
                }
                break;
            }
            case FINAL: { // Ok final resort is to do a wider search and then we'll call it quits
                onLost();
                break;
            }
        }
        currentlyChecking = true;
        checkingPositions = chunkPositions.size();

        chunkPositions.forEach(chunkPosition -> {
            if (!renderDistance.contains(chunkPosition) && yesCom.trackingHandler.isChunkKnown(chunkPosition, dimension)) {
                --checkingPositions;
                onResult(chunkPosition, IsLoadedQuery.Result.UNLOADED, loadedCheck);
                return;
            }

            yesCom.queryHandler.addQuery(new IsLoadedQuery(chunkPosition.getPosition(8, 0, 8),
                    dimension, IQuery.Priority.HIGH, yesCom.configHandler.TYPE, (query, result) -> {
                ++checksTaken;
                if (!currentlyChecking) return;
                --checkingPositions;

                onResult(query.getChunkPosition(), result, loadedCheck);
            }));
        });
    }

    private void onResult(ChunkPosition chunkPosition, IsLoadedQuery.Result result, LoadedCheck loadedCheck) {
        switch (result) {
            case LOADED: {
                currentlyChecking = false; // We've found a valid one so stop all the others from doing anything
                checkingPositions = 0;
                doResolve(chunkPosition);
                break;
            }
            case UNLOADED: {
                switch (loadedCheck) {
                    case AHEAD: {
                        if (checkingPositions <= 0) startPositionCheck(LoadedCheck.STATIONARY);
                        break;
                    }
                    case STATIONARY: {
                        if (checkingPositions <= 0) startPositionCheck(LoadedCheck.AROUND);
                        break;
                    }
                    case AROUND: {
                        if (checkingPositions <= 0) startPositionCheck(LoadedCheck.FINAL);
                        break;
                    }
                    case FINAL: {
                        if (checkingPositions <= 0) onLost();
                        break;
                    }
                }
                break;
            }
        }
    }

    private void doResolve(ChunkPosition initialLoaded) {
        switch (currentState) {
            case FORCE_RESOLVE: {
                if (currentResolver == null || currentResolver.isComplete()) {
                    // Resolve as accurately as possible
                    currentResolver = new QuickResolver(initialLoaded, dimension, yesCom.configHandler.TRACKING_QUICK_MIN_PHASE,
                            resolver -> {});
                    ++resolveSamples;
                }
                break;
            }
            case HIGH_MOVEMENT: {
                float speed = (float)Math.sqrt(Math.pow(speedX, 2) + Math.pow(speedZ, 2));

                if (currentResolver == null || currentResolver.isComplete()) {
                    float maxPhase = yesCom.configHandler.TRACKING_QUICK_MAX_PHASE - yesCom.configHandler.TRACKING_QUICK_MIN_PHASE;
                    maxPhase *= speed / 3.125f;
                    maxPhase += yesCom.configHandler.TRACKING_QUICK_MIN_PHASE;

                    currentResolver = new QuickResolver(initialLoaded, dimension, (int)maxPhase, resolver -> {});
                }
                break;
            }
            case MINIMAL_MOVEMENT: {
                if (currentResolver == null || currentResolver.isComplete())
                    currentResolver = new QuickResolver(initialLoaded, dimension, yesCom.configHandler.TRACKING_QUICK_MIN_PHASE,
                            resolver -> {});
                break;
            }
            case NO_MOVEMENT: { // No render distance resolution with no movement
                break;
            }
        }
    }

    private void onLost() {
        yesCom.logger.warn(String.format("Lost tracker %s.", this));

        switch (dimension) {
            case NETHER: {
                if (yesCom.connectionHandler.hasAccountsIn(Dimension.OVERWORLD)) {
                    currentResolver = new DimChangeResolver(
                            new ChunkPosition(possiblePosition.getX() * 8, possiblePosition.getZ() * 8),
                            Dimension.OVERWORLD);
                } else {
                    doLogout();
                }
                break;
            }
            case OVERWORLD: {
                if (yesCom.connectionHandler.hasAccountsIn(Dimension.NETHER)) {
                    currentResolver = new DimChangeResolver(
                            new ChunkPosition(possiblePosition.getX() / 8, possiblePosition.getZ() / 8),
                            Dimension.NETHER);
                } else {
                    doLogout();
                }
                break;
            }
            case END: {
                doLogout();
                break;
            }
        }
    }

    private void doLogout() {
        yesCom.logger.info(String.format("%s logged out, possible players: %s.", this,
                yesCom.connectionHandler.recentLeaves.keySet()));
        yesCom.connectionHandler.recentLeaves.forEach((uuid, time) -> possiblePlayers.put(uuid, possiblePlayers.getOrDefault(uuid, 0) + 1));

        LoggedPlayer loggedPlayer = new LoggedPlayer(trackedPlayerID, possiblePosition, dimension);
        loggedPlayer.setPossiblePlayers(possiblePlayers);

        yesCom.trackingHandler.removeTrackedPlayer(this);
        if (!possiblePlayers.isEmpty()) yesCom.trackingHandler.addLoggedPlayer(loggedPlayer);
    }

    public void onTick() {
        switch (currentState) {
            case FORCE_RESOLVE: {
                if (resolveSamples > yesCom.configHandler.TRACKING_RESOLVE_SAMPLES) {
                    resolveSamples = 0;
                    currentState = State.HIGH_MOVEMENT;
                } else {
                    requeryTime = 50; // Force resolving so requery every 1 tick
                }
                break;
            }
            case HIGH_MOVEMENT: {
                // Requery every time they are estimated to nearly have travelled a half of their render distance
                requeryTime = Math.min(yesCom.configHandler.TRACKING_MAX_REQUERY_TIME,
                        (int)(yesCom.configHandler.RENDER_DISTANCE / 2.0f / Math.sqrt(Math.pow(speedX, 2) + Math.pow(speedZ, 2))) * 1000);
                break;
            }
            case MINIMAL_MOVEMENT: {
                requeryTime = 5000;
                break;
            }
            case NO_MOVEMENT: {
                requeryTime = 50000;
                break;
            }
        }

        if (System.currentTimeMillis() - lastCheck > requeryTime) {
            lastCheck = System.currentTimeMillis();

            if ((currentResolver == null || currentResolver.isComplete()) && !currentlyChecking && checkingPositions <= 0) {
                checksTaken = 0;
                startPositionCheck(LoadedCheck.AHEAD);
            }
        }

        if (currentResolver != null && currentResolver.isComplete()) {
            previousPositions.add(possiblePosition);

            List<ChunkPosition> speedSamples = previousPositions.subList(Math.max(0, previousPositions.size() - yesCom.configHandler.TRACKING_SPEED_SAMPLES),
                    previousPositions.size());

            long deltaTime = System.currentTimeMillis() - lastResolve;

            float lastSpeedX = speedX;
            float lastSpeedZ = speedZ;

            speedX = 0.0f;
            speedZ = 0.0f;

            ChunkPosition lastPosition = speedSamples.get(0);
            for (int index = 1; index < speedSamples.size(); ++index) {
                speedX += speedSamples.get(index).getX() - lastPosition.getX();
                speedZ += speedSamples.get(index).getZ() - lastPosition.getZ();

                lastPosition = speedSamples.get(index);
            }

            // Actually calculate the speed correctly this time lol
            speedX /= Math.max(1.0f, speedSamples.size() - 1) * (deltaTime / 1000.0f);
            speedZ /= Math.max(1.0f, speedSamples.size() - 1) * (deltaTime / 1000.0f);

            if (Math.abs(speedX - lastSpeedX) > 0.05f || Math.abs(speedZ - lastSpeedZ) > 0.05f && yesCom.handler != null)
                yesCom.handler.onTrackedSpeedChanged(this);

            previousSpeeds.add((float)Math.sqrt(Math.pow(speedX, 2) + Math.pow(speedZ, 2)));

            if (currentResolver.getRenderDistance() == null) { // We weren't able to resolve render distance
                yesCom.logger.info(String.format("Lost render distance data for %s.", this));
                doLogout();

            } else {
                if (!renderDistance.equals(currentResolver.getRenderDistance()) || dimension != currentResolver.getDimension()) {
                    if (dimension != currentResolver.getDimension()) {
                        speedSamples.clear();
                        speedX = 0.0f;
                        speedZ = 0.0f;
                    }

                    possiblePosition = currentResolver.getRenderDistance().getCenterPosition();
                    renderDistance = currentResolver.getRenderDistance();
                    dimension = currentResolver.getDimension();

                    if (yesCom.handler != null) yesCom.handler.onTrackedPositionChanged(this);
                }

                checksTaken += currentResolver.getChecksTaken();
                yesCom.logger.debug(String.format("Render %s distance resolved.", renderDistance));
            }

            currentResolver = null;

            yesCom.logger.info(String.format("Update tracker %s, speed: %.2f chunks/s (delta: %dms).", this,
                    Math.sqrt(Math.pow(speedX, 2) + Math.pow(speedZ, 2)), deltaTime));
            yesCom.logger.info(String.format("Took %d checks to update (%.2f checks/s).", checksTaken,
                    checksTaken / (deltaTime / 1000.0f)));

            checksTaken = 0;
            lastResolve = System.currentTimeMillis(); // Different from last check as this is for speed and such
        }
    }

    public int getPossiblePlayer(UUID uuid) {
        return possiblePlayers.getOrDefault(uuid, 0);
    }

    public Map<UUID, Integer> getPossiblePlayers() {
        return new HashMap<>(possiblePlayers);
    }

    public void putPossiblePlayer(UUID uuid, int likeliness) {
        possiblePlayers.put(uuid, likeliness);
    }

    public void setPossiblePlayers(Map<UUID, Integer> possiblePlayers) {
        this.possiblePlayers.clear();
        this.possiblePlayers.putAll(possiblePlayers);
    }

    public void putPossiblePlayers(Map<UUID, Integer> possiblePlayers) {
        this.possiblePlayers.putAll(possiblePlayers);
    }

    public void removePossiblePlayer(UUID uuid) {
        possiblePlayers.remove(uuid);
    }

    public int getTrackedPlayerID() {
        return trackedPlayerID;
    }

    public int getTrackingSince() {
        return (int)(System.currentTimeMillis() - foundAt);
    }

    public ChunkPosition getPossiblePosition() {
        return possiblePosition;
    }

    public void setPossiblePosition(ChunkPosition possiblePosition) {
        this.possiblePosition = possiblePosition;
    }

    public RenderDistance getRenderDistance() {
        return renderDistance;
    }

    public void setRenderDistance(RenderDistance renderDistance) {
        this.renderDistance = renderDistance;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    public float getSpeedX() {
        return speedX;
    }

    public float getSpeedZ() {
        return speedZ;
    }

    public enum State {
        FORCE_RESOLVE, HIGH_MOVEMENT, MEDIUM_MOVEMENT, MINIMAL_MOVEMENT, NO_MOVEMENT;
    }

    private enum LoadedCheck {
        AHEAD, AROUND, STATIONARY, FINAL;
    }
}
