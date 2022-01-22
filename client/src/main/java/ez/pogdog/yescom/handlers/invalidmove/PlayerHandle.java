package ez.pogdog.yescom.handlers.invalidmove;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPlaceBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerCloseWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;
import com.github.steveice10.packetlib.packet.Packet;
import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.BlockPosition;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PlayerHandle {

    private final YesCom yesCom = YesCom.getInstance();

    private final Set<BlockPosition> confirmedStorages = new HashSet<>(); // HashSet to avoid duplicates
    private final Map<Integer, IsLoadedQuery> queryMap = new HashMap<>();
    private final Map<Integer, Integer> windowToTPIDMap = new HashMap<>();

    private final Player player;
    private final int listenerID;

    private BlockPosition bestStorage;

    private boolean expectingOpen;
    private long expectingSince;
    private int openAttempts;

    private boolean facingStorage;
    private boolean storageOpen;

    private boolean closedPacket;
    private int recievedWindowID;
    private int packetsSinceCloseWindow;

    private boolean forceNoARZIMode;

    private int queriesDone;

    public PlayerHandle(Player player) {
        this.player = player;
        listenerID = this.player.addPacketListener(this::onPacket);

        bestStorage = null;

        expectingOpen = false;
        expectingSince = System.currentTimeMillis() - yesCom.configHandler.REOPEN_TIME;
        openAttempts = 0;

        facingStorage = false;
        storageOpen = false;

        closedPacket = false;
        recievedWindowID = -1;
        packetsSinceCloseWindow = 0;

        forceNoARZIMode = false;

        queriesDone = 0;

    }

    @Override
    public String toString() {
        return String.format("PlayerHandler(player=%s)", player);
    }

    /* ------------------------ Events ------------------------ */

    public void onTick() {
        queriesDone = 0;

        if (player.isConnected() && player.getTimeLoggedIn() > yesCom.configHandler.MIN_TIME_CONNECTED &&
                player.getTimeSinceLastPacket() < yesCom.configHandler.MAX_PACKET_TIME) {
            if (!expectingOpen && (!facingStorage || (!storageOpen && queryMap.isEmpty()))) {
                yesCom.logger.finest(String.format("Attempting to open storage for %s.", this));
                resync();

                expectingOpen = true;
                expectingSince = System.currentTimeMillis();

            } else if (expectingOpen && System.currentTimeMillis() - expectingSince > yesCom.configHandler.REOPEN_TIME) {
                expectingOpen = false;

                if (openAttempts++ > yesCom.configHandler.MAX_OPEN_ATTEMPTS) {
                    yesCom.logger.warning(String.format("%s couldn't find a valid storage after %d attempts, disconnecting.",
                            this, openAttempts));
                    player.disconnect("Couldn't find valid storage!");
                } else {
                    yesCom.logger.warning(String.format("%s couldn't open storage, attempting again...", this));
                }
            }

            synchronized (this) {
                AtomicInteger rescheduled = new AtomicInteger();
                new HashMap<>(queryMap).forEach((teleportID, query) -> {
                    if (player.getCurrentTP() > teleportID || query.getTickingTime() > yesCom.configHandler.INVALID_MOVE_TIMEOUT) {
                        yesCom.logger.finest(String.format("%s has timed out (packet loss?), rescheduling.", query));
                        queryMap.remove(teleportID);
                        query.reschedule();
                        rescheduled.addAndGet(1);
                    }
                });
                if (rescheduled.get() > 0)
                    yesCom.logger.warning(String.format("%s packet loss (7), rescheduled: %d.", this, rescheduled.get()));
            }
        }
    }

    public void onExit() {
        queryMap.forEach((teleportID, query) -> query.reschedule());
        player.removePacketListener(listenerID);
    }

    private synchronized void onPacket(Packet packet) {
        // If we have read more than 5 packets expect that we weren't sent a position rotation packet (note that it
        // should only be 2 as we receive the sound packet before the close window, but better safe than sorry).
        if (closedPacket && ++packetsSinceCloseWindow > 5) {
            yesCom.logger.warning("%s packet loss (1)");
            resync();

            recievedWindowID = -1;
            packetsSinceCloseWindow = 0;
        }

        if (packet instanceof ServerChunkDataPacket) {
            ServerChunkDataPacket chunkData = (ServerChunkDataPacket)packet;

            List<Position> positions = new ArrayList<>();

            for (int index = 0; index < chunkData.getColumn().getChunks().length; ++index) {
                Chunk chunk = chunkData.getColumn().getChunks()[index];
                if (chunk == null || chunk.isEmpty()) continue;
                for (int x = 0; x < 16; ++x) {
                    for (int y = 0; y < 16; ++y) {
                        for (int z = 0; z < 16; ++z) {
                            BlockState blockState = chunk.getBlocks().get(x, y, z);
                            if (yesCom.invalidMoveHandler.VALID_STORAGES.contains(blockState.getId())) {
                                Position position = new Position(
                                        x + chunkData.getColumn().getX() * 16,
                                        y + index * 16,
                                        z + chunkData.getColumn().getZ() * 16);
                                yesCom.logger.fine("Found storage: " + position);
                                positions.add(position);
                            }
                        }
                    }
                }
            }

            confirmedStorages.addAll(positions.stream().map(BlockPosition::new).collect(Collectors.toList()));

        } else if (packet instanceof ServerBlockChangePacket) {
            ServerBlockChangePacket blockChange = (ServerBlockChangePacket)packet;

            BlockPosition blockPos = new BlockPosition(blockChange.getRecord().getPosition());

            if (yesCom.invalidMoveHandler.VALID_STORAGES.contains(blockChange.getRecord().getBlock().getId())) {
                // yesCom.logger.debug(blockChange + ", " + blockChange.getRecord().getBlock());
                confirmedStorages.add(blockPos);

            } else if (confirmedStorages.contains(blockPos)) { // TODO: This check for chunk data packets
                confirmedStorages.remove(blockPos); // The block is no longer a valid storage
                if (bestStorage == blockPos) storageOpen = false; // Idk which one would be sent first but whatever
            }

        } else if (packet instanceof ServerMultiBlockChangePacket) {
            ServerMultiBlockChangePacket multiBlockChange = (ServerMultiBlockChangePacket)packet;

            for (BlockChangeRecord record : multiBlockChange.getRecords()) {
                BlockPosition blockPos = new BlockPosition(record.getPosition());

                if (yesCom.invalidMoveHandler.VALID_STORAGES.contains(record.getBlock().getId())) {
                    yesCom.logger.finer(record.getPosition() + ", " + record.getBlock());
                    confirmedStorages.add(blockPos);

                } else if (confirmedStorages.contains(blockPos)) {
                    confirmedStorages.remove(blockPos);
                    if (bestStorage == blockPos) storageOpen = false;
                }
            }

        } else if (packet instanceof ServerPlayerPositionRotationPacket) {
            ServerPlayerPositionRotationPacket positionRotation = (ServerPlayerPositionRotationPacket)packet;
            int teleportID = positionRotation.getTeleportId();

            boolean arziMode = yesCom.configHandler.ARZI_MODE && !forceNoARZIMode;
            boolean widResync = arziMode && !yesCom.configHandler.ARZI_MODE_NO_WID_RESYNC;

            if (arziMode && widResync) {
                if (recievedWindowID == -1) {
                    yesCom.logger.warning(String.format("%s packet loss (2), got: %d.", this, teleportID));
                    // We can be sure that the storage isn't open, as we got not window ID, but that doesn't mean we
                    // should cancel ALL queries, as we can assume that they're still ok, so just reschedule the/
                    // corresponding query one
                    if (queryMap.containsKey(teleportID)) {
                        IsLoadedQuery query = queryMap.get(teleportID);
                        query.reschedule();
                        queryMap.remove(teleportID);
                    }
                    return;
                }

                int expectedTeleportID = windowToTPIDMap.getOrDefault(recievedWindowID, -1);
                int expectedWindowID = windowToTPIDMap.entrySet().stream()
                        .filter(entry -> entry.getValue() == positionRotation.getTeleportId())
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(-1);

                if (expectedTeleportID != -1 && expectedTeleportID != teleportID) { // Check we have the right TP ID
                    yesCom.logger.warning(String.format("%s packet loss (3), expected: %d, got: %d.", this,
                            expectedTeleportID, teleportID));

                    /* // This probably isn't valid
                    if (expectedWindowID != -1) {
                        teleportID = windowToTPIDMap.get(expectedWindowID);
                    } else {
                        resync(false);
                        return;
                    }
                     */

                    resync(false); // TODO: I mean do we really need to cancel ALL queries?

                // No valid teleport ID and no valid window ID, completely clueless, could have been teleported or
                // joined a new world
                } else if (expectedTeleportID == -1 && expectedWindowID == -1) {
                    yesCom.logger.warning(String.format("%s packet loss (4), got: %d.", this, teleportID));
                    resync(false);
                    return;
                }

                if (expectedWindowID != -1 && expectedWindowID != recievedWindowID) { // Check we have the right window ID
                    yesCom.logger.warning(String.format("%s packet loss (5), expected: %d, got: %d.", this,
                            expectedWindowID, recievedWindowID));

                    if (expectedTeleportID != teleportID) { // If we failed this too, then we're probably off by a little bit
                        resync(false);
                        return;
                    }
                }

            } else {
                if (!queryMap.containsKey(teleportID)) { // FUCK
                    yesCom.logger.warning(String.format("%s packet loss (6), got: %d.", this, teleportID));
                    resync();
                    return;
                }
            }

            IsLoadedQuery query = queryMap.get(teleportID);

            if (query != null) {
                query.setResult(closedPacket ? IsLoadedQuery.Result.LOADED : IsLoadedQuery.Result.UNLOADED);
                queryMap.remove(teleportID);
            }

            if (closedPacket && !arziMode) resync(); // Assume all queries were invalid after this stage

            closedPacket = false; // Reset to default
            recievedWindowID = -1;
            packetsSinceCloseWindow = 0;

            /*
            if (query != null) {
                switch (expectedTeleportID) {
                    case -2: { // Loaded no resync check
                        // This should NOT happen unless something has gone wrong
                        if (yesCom.configHandler.ARZI_MODE && !yesCom.configHandler.ARZI_MODE_NO_WID_RESYNC) {
                            yesCom.logger.warning(String.format("%s packet loss (2), got: %d.", this, positionRotation.getTeleportId()));

                            reSync();
                            query.reschedule();
                            break;
                        }

                        // Ok so yeah we may have guessed wrong but there is really nothing we can do about it if we have
                        query.setResult(IsLoadedQuery.Result.LOADED);

                        if (!yesCom.configHandler.ARZI_MODE) {
                            yesCom.logger.finest(String.format("%s found loaded, invalidating all previous queries and resyncing...", this));
                            reSync();
                        }
                        break;
                    }
                    case -1: { // Unloaded
                        query.setResult(IsLoadedQuery.Result.UNLOADED);
                        break;
                    }
                    default: { // Loaded + resync check
                        if (positionRotation.getTeleportId() != expectedTeleportID) {
                            yesCom.logger.warning(String.format("%s packet loss (4), expected: %d, got: %d.",
                                    this, expectedTeleportID, positionRotation.getTeleportId()));

                            reSync();
                            query.reschedule();
                        } else {
                            yesCom.logger.finer(String.format("%s found loaded with ID %d.", this, expectedTeleportID));
                            query.setResult(IsLoadedQuery.Result.LOADED);
                        }
                        break;
                    }
                }

            } else {
                // Happens on joining the world, as well as just unknown teleports
                yesCom.logger.warning(String.format("%s packet loss (5), expected: %d, got: %d.", this,
                        expectedTeleportID < 0 ? player.getEstimatedTP() : expectedTeleportID, positionRotation.getTeleportId()));
                reSync();
            }
            */

        } else if (packet instanceof ServerOpenWindowPacket) {
            ServerOpenWindowPacket openWindow = (ServerOpenWindowPacket)packet;
            openAttempts = 0;
            storageOpen = true;

            if (expectingOpen || queryMap.isEmpty()) { // No current queries?
                player.setEstimatedWindowID(openWindow.getWindowId());
                expectingOpen = false;
                return;
            }

            closedPacket = false;
            recievedWindowID = openWindow.getWindowId();

        } else if (packet instanceof ServerCloseWindowPacket) {
            ServerCloseWindowPacket closeWindow = (ServerCloseWindowPacket)packet;
            storageOpen = false;

            if (closedPacket) {
                // We should receive a close window and then a teleport packet always one after another
                yesCom.logger.warning(String.format("%s got second close window while only expecting one, WTF?", this));
                resync(); // Cancel this query even if we may get the correct result we'll have cleared our expected queries
            } else {
                closedPacket = true; // Could be a loaded chunk
                recievedWindowID = closeWindow.getWindowId();
                // System.out.println(closeWindow.getWindowId() + ", " + expectedTeleportID + ", " + windowToTPIDMap);
                packetsSinceCloseWindow = 0;
            }
        }
    }

    /* ------------------------ Private Methods ------------------------ */

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean findBestStorage() {
        Optional<BlockPosition> foundStorage;
        synchronized (this) {
            foundStorage = confirmedStorages.stream()
                    .min(Comparator.comparingDouble(player.getPosition()::getDistance));
        }

        if (foundStorage.isPresent() && player.getPosition().getDistance(foundStorage.get()) < 6.0f) {
            bestStorage = foundStorage.get();
            return true;

        } else {
            // yesCom.logger.warn(String.format("Couldn't find storage for %s!", this));
            return false;
        }
    }

    private void faceStorage() {
        if (bestStorage == null && !findBestStorage()) {
            facingStorage = false;
            return;
        }

        ez.pogdog.yescom.util.Position storageCenter = bestStorage.toPositionCenter();
        ez.pogdog.yescom.util.Position positionDiff = storageCenter.subtract(player.getPosition().add(0, 1.8, 0));

        double diffXZ = Math.sqrt(positionDiff.getX() * positionDiff.getX() + positionDiff.getZ() * positionDiff.getZ());

        Angle angle = new Angle((float)Math.toDegrees(Math.atan2(positionDiff.getZ(), positionDiff.getX())) - 90.0f,
                (float)(-Math.toDegrees(Math.atan2(positionDiff.getY(), diffXZ))));
        player.setAngle(angle);
        player.sendPacket(new ClientPlayerRotationPacket(true, angle.getYaw(), angle.getPitch()));

        yesCom.logger.finest(String.format("%s required angle: %s.", this, angle));

        facingStorage = true;
    }

    /**
     * Opens the best storage known.
     */
    private void openStorage() {
        if (bestStorage == null && !findBestStorage()) {
            yesCom.logger.warning(String.format("Couldn't find storage for %s!", this));
            storageOpen = false;
            return;
        }

        player.sendPacket(new ClientTeleportConfirmPacket(player.getEstimatedTP()));
        player.sendPacket(new ClientTeleportConfirmPacket(player.getCurrentTP()));

        player.sendPacket(new ClientPlayerPlaceBlockPacket(bestStorage.toSerializable(), BlockFace.UP, Hand.MAIN_HAND,
                0.0f, 0.0f, 0.0f));
        player.sendPacket(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND));
        player.setEstimatedWindowID(player.getEstimatedWindowID() % 100 + 1);
    }

    /* ------------------------ Random Stuff ------------------------ */

    public synchronized boolean addQuery(IsLoadedQuery query) {
        if (expectingOpen || !facingStorage || !storageOpen || query.isFinished() || query.getType() != IsLoadedQuery.Type.INVALID_MOVE ||
                queriesDone++ >= yesCom.configHandler.QUERIES_PER_TICK)
            return false;

        player.setEstimatedTP(player.getEstimatedTP() + 1);
        queryMap.put(player.getEstimatedTP(), query);

        BlockPosition position = query.getPosition();

        player.sendPacket(new ClientPlayerPositionPacket(false, position.getX(), position.getY() + 5000, position.getZ()));
        player.sendPacket(new ClientTeleportConfirmPacket(player.getEstimatedTP()));

        if (!expectingOpen && yesCom.configHandler.ARZI_MODE && !forceNoARZIMode) {
            if (!yesCom.configHandler.ARZI_MODE_NO_WID_RESYNC)
                windowToTPIDMap.put(player.getEstimatedWindowID(), player.getEstimatedTP());

            openStorage();
            storageOpen = true; // Assume this is true cos we're in ARZI mode
        }

        return true;
    }

    public int getCurrentQueries() {
        return queryMap.size();
    }

    public synchronized void resync(boolean openStorage) {
        yesCom.logger.finest(String.format("%s resynchronizing (rescheduling %d queries)...", this, queryMap.size()));

        player.setEstimatedTP(player.getCurrentTP());
        player.setEstimatedWindowID(player.getCurrentWindowID() % 100 + 1);

        player.sendPacket(new ClientTeleportConfirmPacket(player.getCurrentTP()));

        queryMap.forEach((teleportID, query) -> query.reschedule());
        queryMap.clear();

        windowToTPIDMap.clear();

        closedPacket = false;

        if (openStorage) {
            facingStorage = false;
            storageOpen = false;
            faceStorage();
            openStorage();
        }
    }

    public void resync() {
        resync(true);
    }

    /* ------------------------ Setters and Getters ------------------------ */

    public Player getPlayer() {
        return player;
    }

    public boolean isStorageOpen() {
        return storageOpen;
    }

    /**
     * Whether this handle is forcefully prevented from opening the storage on every query.
     * @return Whether the above is true.
     */
    public boolean isForceNoARZIMode() {
        return forceNoARZIMode;
    }

    /**
     * Enables / disables opening the storage on every query.
     * @param forceNoARZIMode The above.
     */
    public void setForceNoARZIMode(boolean forceNoARZIMode) {
        // Do this as we will be assuming that all our queries were valid under the last state
        if (this.forceNoARZIMode != forceNoARZIMode) {
            yesCom.logger.finest("ARZI mode state change, resyncing...");
            resync();
        }
        this.forceNoARZIMode = forceNoARZIMode;
    }
}
