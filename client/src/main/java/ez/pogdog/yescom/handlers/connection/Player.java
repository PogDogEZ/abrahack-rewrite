package ez.pogdog.yescom.handlers.connection;

import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerCloseWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowPropertyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.Dimension;
import ez.pogdog.yescom.util.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A player connected to the server.
 */
public class Player {

    private final YesCom yesCom = YesCom.getInstance();

    private final Map<UUID, String> onlinePlayers = new ConcurrentHashMap<>();
    private final Map<Long, Integer> timeToTPID = new HashMap<>();
    private final List<Float> tickValues = new ArrayList<>();

    private final Map<Integer, Consumer<Packet>> packetListeners = new ConcurrentHashMap<>();
    private final Map<Integer, BiConsumer<PlayerAction, UUID>> joinLeaveListeners = new ConcurrentHashMap<>();

    private final AuthenticationService authService;
    private Session session;

    private final FoodStats foodStats;

    private boolean canLogin;

    private int packetListenerID;
    private int joinLeaveListenerID;

    private long lastTimeUpdate;
    private long lastWorldTicks;

    private Position position;
    private Angle angle;
    private boolean onGround;

    private Dimension dimension;

    private int currentTP;
    private int estimatedTP;

    private int currentWindowID;
    private int estimatedWindowID;

    private int serverPing; // TODO: Estimated ping as well

    private long lastLoginTime;
    private long lastPacketTime;

    public Player(AuthenticationService authService) {
        this.authService = authService;

        session = null;
        canLogin = true;

        packetListenerID = 0;
        joinLeaveListenerID = 0;

        lastTimeUpdate = -1L;
        lastWorldTicks = 0;

        position = new Position(0.0, 0.0, 0.0);
        angle = new Angle(0.0f, 0.0f);
        onGround = true;

        dimension = Dimension.OVERWORLD;

        foodStats = new FoodStats(20.0f, 20, 5.0f);

        currentTP = 0;
        estimatedTP = 0;

        currentWindowID = 0;
        estimatedWindowID = 0;

        lastLoginTime = System.currentTimeMillis();
        lastPacketTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.format("Player(name=%s)", authService.getSelectedProfile().getName());
    }

    /* ------------------------ Private methods ------------------------ */

    private void notifyPacketListeners(Packet packet) {
        packetListeners.forEach((listenerID, listener) -> listener.accept(packet));
    }

    private void notifyJoinLeaveListeners(PlayerAction action, UUID uuid) {
        joinLeaveListeners.forEach((listenerID, listener) -> listener.accept(action, uuid));
    }

    private void whyDoIHaveToDoThis() throws ClassNotFoundException {
    }

    /* ------------------------ Events ------------------------ */

    public void onPacket(Packet packet) {
        if (!(packet instanceof ServerChatPacket)) lastPacketTime = System.currentTimeMillis(); // Async chat on Paper

        if (packet instanceof ServerJoinGamePacket) {
            dimension = Dimension.fromMC(((ServerJoinGamePacket)packet).getDimension());

            if (yesCom.ycHandler != null) yesCom.ycHandler.onDimensionChanged(this);

        } else if (packet instanceof ServerRespawnPacket) {
            dimension = Dimension.fromMC(((ServerRespawnPacket)packet).getDimension());

            if (yesCom.ycHandler != null) yesCom.ycHandler.onDimensionChanged(this);

        } else if (packet instanceof ServerPlayerPositionRotationPacket) {
            ServerPlayerPositionRotationPacket positionRotation = (ServerPlayerPositionRotationPacket)packet;

            position = new Position(positionRotation.getX(), positionRotation.getY(), positionRotation.getZ());
            angle = new Angle(positionRotation.getYaw(), positionRotation.getPitch());

            currentTP = positionRotation.getTeleportId();

            if (yesCom.ycHandler != null) yesCom.ycHandler.onPositionChanged(this);

        } else if (packet instanceof ServerOpenWindowPacket) {
            currentWindowID = ((ServerOpenWindowPacket)packet).getWindowId();

        } else if (packet instanceof ServerWindowItemsPacket) {
            currentWindowID = ((ServerWindowItemsPacket)packet).getWindowId();

        } else if (packet instanceof ServerWindowPropertyPacket) {
            currentWindowID = ((ServerWindowPropertyPacket)packet).getWindowId();

        } else if (packet instanceof ServerCloseWindowPacket) {
            currentWindowID = ((ServerCloseWindowPacket)packet).getWindowId();

        } else if (packet instanceof ServerPlayerHealthPacket) {
            foodStats.update((ServerPlayerHealthPacket)packet);

            if (yesCom.ycHandler != null) yesCom.ycHandler.onHealthChanged(this);

        } else if (packet instanceof ServerPlayerListEntryPacket) {
            ServerPlayerListEntryPacket serverListEntry = (ServerPlayerListEntryPacket)packet;

            switch (serverListEntry.getAction()) {
                case ADD_PLAYER: {
                    Arrays.stream(serverListEntry.getEntries()).forEach(entry -> {
                        UUID uuid = entry.getProfile().getId();
                        String name = entry.getProfile().getName();

                        yesCom.connectionHandler.setNameForUUID(uuid, name);

                        onlinePlayers.put(uuid, name);
                        notifyJoinLeaveListeners(PlayerAction.ADD, uuid);
                    });
                    break;
                }
                case REMOVE_PLAYER: {
                    Arrays.stream(serverListEntry.getEntries()).forEach(entry -> {
                        UUID uuid = entry.getProfile().getId();

                        onlinePlayers.remove(uuid);
                        notifyJoinLeaveListeners(PlayerAction.REMOVE, uuid);
                    });
                    break;
                }
                case UPDATE_LATENCY: {
                    Arrays.stream(serverListEntry.getEntries()).forEach(entry -> {
                        if (entry.getProfile().getId().equals(authService.getSelectedProfile().getId()))
                            serverPing = entry.getPing();
                    });
                    break;
                }
            }

        } else if (packet instanceof ServerUpdateTimePacket) {
            ServerUpdateTimePacket updateTime = (ServerUpdateTimePacket)packet;

            if (lastTimeUpdate == -1L) {
                lastTimeUpdate = System.currentTimeMillis();
                lastWorldTicks = updateTime.getWorldAge();
            } else {
                synchronized (this) {
                    tickValues.add((updateTime.getWorldAge() - lastWorldTicks) / ((System.currentTimeMillis() - lastTimeUpdate) / 1000.0f));
                    while (tickValues.size() > 20) tickValues.remove(0);
                }
                lastTimeUpdate = System.currentTimeMillis();
                lastWorldTicks = updateTime.getWorldAge();
            }

        } else if (packet instanceof ServerChatPacket) {
            yesCom.logger.finest(String.format("Chat from %s: %s", this, ((ServerChatPacket)packet).getMessage().getFullText()));
            yesCom.dataHandler.newChatMessage(authService.getUsername(), ((ServerChatPacket)packet).getMessage().getFullText(),
                    System.currentTimeMillis());
        }

        notifyPacketListeners(packet);
    }

    public void onTick() {
    }

    public void onExit() {
        if (isConnected()) disconnect("Exit.");
    }

    /**
     * Registers a listener to listen for when packets are received from this player.
     * @param callBack The callback.
     * @return The ID of the callback for later removal.
     */
    public int addPacketListener(Consumer<Packet> callBack) {
        packetListeners.put(++packetListenerID, callBack);
        return packetListenerID;
    }

    /**
     * Removes a listener from the packet listeners for this player.
     * @param ID The ID of the packet listener.
     */
    public void removePacketListener(int ID) {
        packetListeners.remove(ID);
    }

    /**
     * Registers a listener for when a player joins or leaves the server.
     * @param callBack The callback.
     * @return The ID of the callback for later removal.
     */
    public int addJoinLeaveListener(BiConsumer<PlayerAction, UUID> callBack) {
        joinLeaveListeners.put(++joinLeaveListenerID, callBack);
        return joinLeaveListenerID;
    }

    /**
     * Removes a listener from join leave listeners.
     * @param ID The ID of the join leave listener.
     */
    public void removeJoinLeaveListener(int ID) {
        joinLeaveListeners.remove(ID);
    }

    /* ------------------------ MCProtocolLib stuff and more ------------------------ */

    public AuthenticationService getAuthService() {
        return authService;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        if (this.session != null && this.session.isConnected()) disconnect("Reconnect");
        this.session = session;
        lastLoginTime = System.currentTimeMillis();
    }

    /**
     * Disconnects the session from the server.
     * @param reason The given reason that will be logged.
     */
    public void disconnect(String reason) {
        if (session == null) return;

        try {
            session.disconnect(reason);
            whyDoIHaveToDoThis();
        } catch (ClassNotFoundException | NoClassDefFoundError error) { // This is a workaround for MCProtocolLib's weirdness
            yesCom.logger.warning(String.format("Failed to disconnect %s: %s", this, error.getMessage()));
            yesCom.logger.throwing(this.getClass().getSimpleName(), "disconnect", error);
        }

        session = null;
    }

    public void disconnect() {
        disconnect("Unknown reason.");
    }

    /**
     * Sends a packet to the server, if the session is connected.
     * @param packet The packet to be sent.
     */
    public void sendPacket(Packet packet) {
        if (session.isConnected()) session.send(packet);
    }

    /**
     * Sends a confirm teleport packet with the current estimated TP.
     */
    public void sendEstimatedTP() {
        sendPacket(new ClientTeleportConfirmPacket(estimatedTP));
    }

    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    /**
     * Returns whether a player, given by their UUID, is currently online in the server.
     * @param uuid The UUID of the player.
     * @return Whether they are online.
     */
    public boolean isPlayerOnline(UUID uuid) {
        return onlinePlayers.containsKey(uuid);
    }

    /**
     * Returns whether a player, given by their in game name, is currently online in the server.
     * @param name The in game name of the player.
     * @return Whether they are online.
     */
    public boolean isPlayerOnline(String name) {
        return onlinePlayers.entrySet().stream().anyMatch(entry -> entry.getValue().equalsIgnoreCase(name));
    }

    public Map<UUID, String> getOnlinePlayers() {
        return new HashMap<>(onlinePlayers);
    }

    /**
     * Returns the mean server tickrate that this player has recorded.
     * @return The mean server tickrate.
     */
    public synchronized float getMeanTickRate() {
        return Math.max(0.0f, Math.min(30.0f, (float)tickValues.stream().mapToDouble(Float::doubleValue).sum() / Math.max(1.0f, tickValues.size())));
    }

    public int getTimeLoggedIn() {
        return (int)(System.currentTimeMillis() - lastLoginTime);
    }

    public int getTimeSinceLastPacket() {
        return (int)(System.currentTimeMillis() - lastPacketTime);
    }

    public void sendChatMessage(String chatMessage) {
        yesCom.logger.fine(String.format("%s sending chat message: %s", this, chatMessage));
        sendPacket(new ClientChatPacket(chatMessage));
    }

    /* ------------------------ Position and angle stuff ------------------------ */

    public double getX() {
        return position.getX();
    }

    public double getY() {
        return position.getY();
    }

    public double getZ() {
        return position.getZ();
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Angle getAngle() {
        return angle;
    }

    public void setAngle(Angle angle) {
        this.angle = angle;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    /* ------------------------ TP and window ID ------------------------ */

    public int getCurrentTP() {
        return currentTP;
    }

    public void setCurrentTP(int currentTP) {
        this.currentTP = currentTP;
    }

    public int getEstimatedTP() {
        return estimatedTP;
    }

    public void setEstimatedTP(int estimatedTP) {
        this.estimatedTP = estimatedTP;
    }

    public int getCurrentWindowID() {
        return currentWindowID;
    }

    public int getEstimatedWindowID() {
        return estimatedWindowID;
    }

    public void setEstimatedWindowID(int estimatedWindowID) {
        this.estimatedWindowID = estimatedWindowID;
    }

    /* ------------------------ Other setters and getters ------------------------ */

    /**
     * @return Whether or not this player can automatically login.
     */
    public boolean getCanLogin() {
        return canLogin;
    }

    public void setCanLogin(boolean canLogin) {
        if (canLogin != this.canLogin) yesCom.ycHandler.onPlayerToggleCanLogin(this);
        this.canLogin = canLogin;
    }

    public int getServerPing() {
        return serverPing;
    }

    public FoodStats getFoodStats() {
        return foodStats;
    }

    public enum PlayerAction {
        ADD, REMOVE;
    }

    public static class FoodStats {

        private float health;
        private int hunger;
        private float saturation;

        public FoodStats(float health, int hunger, float saturation) {
            this.health = health;
            this.hunger = hunger;
            this.saturation = saturation;
        }

        public void update(ServerPlayerHealthPacket packet) {
            health = packet.getHealth();
            hunger = packet.getFood();
            saturation = packet.getSaturation();
        }

        public float getHealth() {
            return health;
        }

        public void setHealth(float health) {
            this.health = health;
        }

        public int getHunger() {
            return hunger;
        }

        public void setHunger(int hunger) {
            this.hunger = hunger;
        }

        public float getSaturation() {
            return saturation;
        }

        public void setSaturation(float saturation) {
            this.saturation = saturation;
        }
    }
}
