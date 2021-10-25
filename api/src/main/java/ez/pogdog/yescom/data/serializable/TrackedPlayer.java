package ez.pogdog.yescom.data.serializable;

import ez.pogdog.yescom.data.IDataProvider;
import ez.pogdog.yescom.data.ISerializable;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class TrackedPlayer implements ISerializable {

    private final Map<UUID, Integer> possiblePlayers = new HashMap<>();

    private final BigInteger trackedPlayerID;
    private final long foundAt;

    private TrackingData trackingData;

    private RenderDistance renderDistance;
    private Dimension dimension;

    private boolean loggedOut;

    public TrackedPlayer(BigInteger trackedPlayerID, TrackingData trackingData, RenderDistance renderDistance,
                         Dimension dimension, boolean loggedOut, long foundAt) {
        this.trackedPlayerID = trackedPlayerID;
        this.trackingData = trackingData;
        this.renderDistance = renderDistance;
        this.dimension = dimension;

        this.loggedOut = loggedOut;

        this.foundAt = foundAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        TrackedPlayer that = (TrackedPlayer)other;
        return foundAt == that.foundAt && loggedOut == that.loggedOut && possiblePlayers.equals(that.possiblePlayers) && trackedPlayerID.equals(that.trackedPlayerID) && trackingData.equals(that.trackingData) && Objects.equals(renderDistance, that.renderDistance) && dimension == that.dimension;
    }

    @Override
    public int hashCode() {
        return Objects.hash(possiblePlayers, trackedPlayerID, foundAt, trackingData, renderDistance, dimension, loggedOut);
    }

    @Override
    public String toString() {
        return String.format("TrackedPlayer(ID=%d, dimension=%s, position=%s)", trackedPlayerID, dimension,
                renderDistance.getCenterPosition());
    }

    @Override
    public BigInteger getID() {
        return trackedPlayerID;
    }

    /**
     * Returns the player most likely to be the one this tracked player corresponds to.
     * @return The UUID of the player.
     */
    public UUID getBestPossiblePlayer() {
        return possiblePlayers.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Gets the number of times a player has been flagged as this tracked player.
     * @param uuid The UUID of the player in question.
     * @return The number of times the player has been flagged.
     */
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

    public long getFoundAt() {
        return foundAt;
    }

    public int getTrackingSince() {
        return (int)(System.currentTimeMillis() - foundAt);
    }

    public TrackingData getTrackingData() {
        return trackingData;
    }

    public void setTrackingData(TrackingData trackingData) {
        this.trackingData = trackingData;
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

    public boolean isLoggedOut() {
        return loggedOut;
    }

    public void setLoggedOut(boolean loggedOut) {
        this.loggedOut = loggedOut;
    }

    public static class TrackingData {

        private final Map<Long, BigInteger> previousRenderDistances = new HashMap<>();

        private final IDataProvider dataProvider;

        public TrackingData(IDataProvider dataProvider, Map<Long, BigInteger> previousRenderDistances) {
            this.dataProvider = dataProvider;
            this.previousRenderDistances.putAll(previousRenderDistances);
        }

        public TrackingData(IDataProvider dataProvider) {
            this(dataProvider, new HashMap<>());
        }

        /**
         * Lagrange polynomial interpolation.
         * https://en.wikipedia.org/wiki/Lagrange_polynomial
         * https://en.wikipedia.org/wiki/Polynomial_interpolation
         * @param knownPoints The previous points.
         * @param nextTime The position at the timestamp you want to find out.
         * @return The position at the timestamp.
         */
        private ChunkPosition lagrangeInterpolate(Map<Long, ChunkPosition> knownPoints, float nextTime) {
            double xSum = 0.0;
            double zSum = 0.0;

            Set<Map.Entry<Long, ChunkPosition>> entrySet = knownPoints.entrySet();

            for (Map.Entry<Long, ChunkPosition> knownEntry : entrySet) {
                double xProduct = 0.0;
                double zProduct = 0.0;

                for (Map.Entry<Long, ChunkPosition> unknownEntry : entrySet) {
                    if (!knownEntry.equals(unknownEntry)) {
                        xProduct *= (nextTime - knownEntry.getValue().getX()) / (knownEntry.getValue().getX() - unknownEntry.getValue().getX());
                        zProduct *= (nextTime - knownEntry.getValue().getZ()) / (knownEntry.getValue().getZ() - unknownEntry.getValue().getZ());
                    }
                }

                xSum += xProduct * knownEntry.getKey();
                zSum += zProduct * knownEntry.getKey();
            }

            return new ChunkPosition((int)xSum, (int)zSum);
        }

        private Map<Long, ChunkPosition> getData(long from, long until) {
            /*
            List<ChunkPosition> positions = new ArrayList<>();

            for (long offset = 0; offset < until - from; ++offset) {
                ChunkPosition realPosition = realData.get(from + offset);

                if (realPosition == null && !realData.isEmpty()) {
                    positions.add(lagrangeInterpolate(realData, from + offset));
                } else if (realPosition != null) {
                    positions.add(realPosition);
                }
            }
             */

            return previousRenderDistances.entrySet().stream()
                    .filter(entry -> from > entry.getKey() && until <= entry.getKey()) // TODO: Account for render distance error
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> dataProvider.getRenderDistance(entry.getValue()).getCenterPosition()));
        }

        /**
         * Returns estimated velocity based on previous data recorded.
         * @param from When to measure data from.
         * @param until When to measure data until.
         * @return The estimated velocity in chunks/s.
         */
        public double getVelocity(long from, long until) {
            Map<Long, ChunkPosition> previousPositions = getData(from / 1000L, until / 1000L);

            double averageX = 0.0;
            double averageZ = 0.0;

            ChunkPosition lastPosition = null;
            long lastTime = 0L;

            for (Map.Entry<Long, ChunkPosition> entry : previousPositions.entrySet()) {
                if (lastPosition == null) {
                    lastPosition = entry.getValue();
                    lastTime = entry.getKey();
                } else {
                    double deltaTime = Math.max(1.0, entry.getKey() - lastTime);
                    averageX += (entry.getValue().getX() - lastPosition.getX()) / deltaTime;
                    averageZ += (entry.getValue().getZ() - lastPosition.getZ()) / deltaTime;
                }
            }

            averageX /= Math.max(1.0, previousPositions.size() - 1.0);
            averageZ /= Math.max(1.0, previousPositions.size() - 1.0);

            return Math.sqrt(averageX * averageX + averageZ * averageZ);
        }

        /**
         * Returns estimated acceleration based on previous data recorded.
         * @param from When to measure data from.
         * @param until When to measure data until.
         * @return The estimated acceleration in chunks(s^(-2)).
         */
        public double getAcceleration(long from, long until) {
            /*
            List<ChunkPosition> previousPositions = getData(from / 1000L, until / 1000L);

            List<ChunkPosition> velocities = new ArrayList<>();
            ChunkPosition lastPosition = null;

            for (ChunkPosition chunkPosition : previousPositions) {
                if (lastPosition == null) {
                    lastPosition = chunkPosition;
                } else {
                    velocities.add(chunkPosition.subtract(lastPosition));
                }
            }

            ChunkPosition averageAcceleration = new ChunkPosition(0, 0);
            ChunkPosition lastVelocity = null;

            for (ChunkPosition velocity : velocities) {
                if (lastVelocity == null) {
                    lastVelocity = velocity;
                } else {
                    averageAcceleration = averageAcceleration.add(velocity.subtract(lastVelocity));
                }
            }

            double averageX = averageAcceleration.getX() / Math.max(1.0, velocities.size() - 1.0);
            double averageZ = averageAcceleration.getZ() / Math.max(1.0, velocities.size() - 1.0);

            return Math.sqrt(averageX * averageX + averageZ * averageZ);
             */
            return 0.0;
        }

        /**
         * Returns the estimated heading (angle of travel) based on previous data recorded.
         * @param from When to measure data from.
         * @param until When to measure data until.
         * @return The estimated heading in degrees (0 to 360).
         */
        public float getHeading(long from, long until) {
            // List<ChunkPosition> previousPositions = getData(from / 1000L, until / 1000L);

            float averageHeading = 0.0f;
            return 0.0f;
        }

        public Map<Long, BigInteger> getRenderDistances() {
            return new HashMap<>(previousRenderDistances);
        }

        public void addRenderDistance(RenderDistance renderDistance) {
            previousRenderDistances.put(System.currentTimeMillis() / 1000L, renderDistance.getID());
        }
    }
}
