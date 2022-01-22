package ez.pogdog.yescom.handlers.connection;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.IHandler;
import ez.pogdog.yescom.util.Dimension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Class responsible for handling connections to the Minecraft server. Can provide info about the Minecraft server and
 * accounts currently connected.
 */
public class ConnectionHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    public final Map<UUID, Long> recentJoins = new HashMap<>();
    public final Map<UUID, Long> recentLeaves = new HashMap<>();

    private final List<Player> players = new ArrayList<>();
    private final Map<UUID, Long> healthLogout = new HashMap<>();

    private final Map<UUID, String> UUIDtoNameCache = new HashMap<>();

    private final String host;
    private final int port;

    private long lastLoginTime;

    public ConnectionHandler(String host, int port) {
        this.host = host;
        this.port = port;

        lastLoginTime = System.currentTimeMillis() + yesCom.configHandler.LOGIN_TIME;

        yesCom.logger.info(String.format("Server is %s:%d.", host, port));
    }

    @Override
    public synchronized void onTick() {
        // Tick health logout
        new HashMap<>(healthLogout).forEach((uuid, logoutTime) -> {
            // Assume that someone has logged in and healed them
            if (System.currentTimeMillis() - logoutTime > 500 && isPlayerOnline(uuid)) healthLogout.remove(uuid);
        });

        // Tick players
        players.forEach(player -> {
            if (player.getFoodStats().getHealth() <= yesCom.configHandler.LOG_OUT_HEALTH) {
                yesCom.logger.warning(String.format("%s logged out as health was below %.1f.",
                        player.getAuthService().getSelectedProfile().getName(), yesCom.configHandler.LOG_OUT_HEALTH));
                healthLogout.put(player.getAuthService().getSelectedProfile().getId(), System.currentTimeMillis());
                player.disconnect(String.format("Health disconnect at health: %.1f.", player.getFoodStats().getHealth()));
            }

            if (!player.isConnected() && System.currentTimeMillis() - lastLoginTime > yesCom.configHandler.LOGIN_TIME)
                login(player);
        });

        // Check recent leaves and joins
        new HashMap<>(recentJoins).forEach((uuid, time) -> {
            if (System.currentTimeMillis() - time > yesCom.configHandler.LOGIN_CACHE_TIME) recentJoins.remove(uuid);
        });
        new HashMap<>(recentLeaves).forEach((uuid, time) -> {
            if (System.currentTimeMillis() - time > yesCom.configHandler.LOGOUT_CACHE_TIME)
                recentLeaves.remove(uuid);
        });
    }

    @Override
    public void onExit() {
    }

    /* ------------------------ Private methods ------------------------ */

    private boolean healthLogin(Player player) {
        AuthenticationService authService = player.getAuthService();
        return !healthLogout.containsKey(authService.getSelectedProfile().getId()) ||
                System.currentTimeMillis() - healthLogout.get(authService.getSelectedProfile().getId()) > yesCom.configHandler.HEALTH_RELOG_TIME;
    }

    private synchronized void onJoinLeave(Player.PlayerAction action, UUID uuid) {
        switch (action) {
            case ADD: {
                if (yesCom.ycHandler != null) yesCom.ycHandler.onPlayerJoin(uuid, UUIDtoNameCache.get(uuid));
                // yesCom.trackingHandler.onPlayerJoin(uuid);
                if (recentLeaves.containsKey(uuid) && System.currentTimeMillis() - recentLeaves.get(uuid) < 2000) {
                    recentLeaves.remove(uuid);
                    return;
                }
                recentJoins.put(uuid, System.currentTimeMillis());
                break;
            }
            case REMOVE: {
                if (yesCom.ycHandler != null) yesCom.ycHandler.onPlayerLeave(uuid);
                if (recentJoins.containsKey(uuid) && System.currentTimeMillis() - recentJoins.get(uuid) < 2000) {
                    recentJoins.remove(uuid);
                    return;
                }
                recentLeaves.put(uuid, System.currentTimeMillis());
                break;
            }
        }
    }

    /* ------------------------ Server stuff ------------------------ */

    /**
     * Returns whether a player, given by their in game name, is currently online in the server, can include players not
     * being used by the program.
     * @param name The in game name of the player.
     * @return Whether they are online.
     */
    public synchronized boolean isPlayerOnline(String name) {
        int count = 0;
        for (Player player : players) {
            if (player.isPlayerOnline(name)) ++count;
        }
        return count >= players.size() / 2;
    }

    /**
     * Returns whether a player, given by their UUID, is currently online in the server, can include players not being
     * used by the program.
     * @param uuid The UUID of the player.
     * @return Whether they are online.
     */
    public synchronized boolean isPlayerOnline(UUID uuid) {
        int count = 0;
        for (Player player : players) {
            if (player.isPlayerOnline(uuid)) ++count;
        }
        return count > players.size() / 2;
    }

    /**
     * Returns the mean tickrate across all currently online players.
     * @return The mean tickrate.
     */
    public synchronized float getMeanTickRate() {
        return (float)players.stream().mapToDouble(Player::getMeanTickRate).sum() / Math.max(1.0f, players.size());
    }

    public synchronized float getMeanServerPing() {
        return (float)players.stream().mapToDouble(Player::getServerPing).sum() / Math.max(1.0f, players.size());
    }

    /* ------------------------ Account and player stuff ------------------------ */

    /**
     * Adds a known player.
     * @param player The player to add.
     */
    public synchronized void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);

            if (yesCom.ycHandler != null) {
                AuthenticationService authService = player.getAuthService();
                yesCom.ycHandler.onPlayerAdded(authService.getUsername(), authService.getSelectedProfile().getId(),
                        authService.getSelectedProfile().getName());
            }
        }
    }

    /**
     * Removes a player.
     * @param player The player to remove.
     */
    public synchronized void removePlayer(Player player) {
        players.remove(player);
        if (yesCom.ycHandler != null) yesCom.ycHandler.onPlayerRemoved(player.getAuthService().getUsername());
    }

    /**
     * Logs in a player, and adds them if they are not known.
     * @param player The player to login.
     */
    public void login(Player player) {
        addPlayer(player);
        AuthenticationService authService = player.getAuthService();

        if (System.currentTimeMillis() - lastLoginTime > yesCom.configHandler.LOGIN_TIME && !player.isConnected() &&
                player.getCanLogin() && healthLogin(player) && !isPlayerOnline(authService.getSelectedProfile().getId())) {
            yesCom.logger.fine(String.format("Logging in %s...", authService.getSelectedProfile().getName()));
            lastLoginTime = System.currentTimeMillis();
            // healthLogout.remove(authService.getSelectedProfile().getId());

            MinecraftProtocol protocol = new MinecraftProtocol(authService.getSelectedProfile(), authService.getAccessToken());
            Session session = new TcpClientSession(host, port, protocol, new Client(host, port, protocol,
                    new TcpSessionFactory(null)), null);

            player.setSession(session);
            player.addJoinLeaveListener(this::onJoinLeave);

            // coordExploit.ceHandler.onPlayerAdded(player);

            if (yesCom.ycHandler != null) yesCom.ycHandler.onPlayerLogin(player); // Fixed the issue with invalid player names
            session.addListener(new SessionReactionAdapter(player));
            session.connect();

            yesCom.logger.fine(String.format("%s logged in: %s.", authService.getSelectedProfile().getName(), session.isConnected()));

            if (!session.isConnected()) {
                // TODO: Perhaps try to refresh the token? Idk why else it wouldn't have connected <- except the server being down
                yesCom.logger.warning(String.format("Couldn't log %s in, immediate disconnect.", authService.getSelectedProfile().getName()));
                try {
                    player.getAuthService().login(); // TODO: This may not work, if it fails to connect after x amount of times, deauth
                } catch (RequestException error) {
                    yesCom.logger.warning(String.format("Couldn't re-login auth server for %s.", authService.getSelectedProfile().getName()));
                    yesCom.logger.throwing(ConnectionHandler.class.getSimpleName(), "login", error);
                }
            } else {
                healthLogout.remove(authService.getSelectedProfile().getId());
            }
        }
    }

    public void logout(Player player) {
        if (player == null || !players.contains(player)) return;
        player.disconnect("Logged out.");
    }

    /**
     * Sends a packet for the specific session.
     * @param player The player to send the packet on.
     * @param packet The packet to send.
     * @return Whether the packet was able to be sent or not.
     */
    public boolean sendPacket(Player player, Packet packet) {
        if (!player.isConnected() || player.getTimeLoggedIn() < yesCom.configHandler.MIN_TIME_CONNECTED) return false;
        player.sendPacket(packet);
        return true;
    }

    public synchronized Player sendPacket(Predicate<Player> predicate, Packet packet) {
        Optional<Player> bestMatch = players.stream()
                .filter(predicate)
                .findFirst();

        if (bestMatch.isPresent() && sendPacket(bestMatch.get(), packet)) {
            return bestMatch.get();
        } else {
            return null;
        }
    }

    /**
     * Sends a packet for a specific username. Doesn't do anything if the username was not found.
     * @param username The username of the user.
     * @param packet The packet to send.
     * @return The player that sent the packet.
     */
    public Player sendPacket(String username, Packet packet) {
        return sendPacket(player -> player.getAuthService().getSelectedProfile().getName().equalsIgnoreCase(username), packet);
    }

    /**
     * Sends a packet on any available connection.
     * @param packet The packet to send.
     * @return The player that sent the packet.
     */
    public Player sendPacket(Packet packet) {
        return sendPacket(player -> player.isConnected() && player.getTimeLoggedIn() > yesCom.configHandler.MIN_TIME_CONNECTED,
                packet);
    }

    /**
     * Gets a player via their UUID.
     * @param uuid The UUID.
     * @return The player, null if no matching player was found.
     */
    public synchronized Player getPlayer(UUID uuid) {
        return players.stream()
                .filter(player -> player.getAuthService().getSelectedProfile().getId().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets a player via their in game display name.
     * @param name Their username or in game display name (not case-sensitive).
     * @return The player, null if no matching player was found.
     */
    public synchronized Player getPlayer(String name) {
        return players.stream()
                .filter(player -> player.getAuthService().getUsername().equalsIgnoreCase(name) ||
                        player.getAuthService().getSelectedProfile().getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public synchronized Map<UUID, String> getOnlinePlayers() {
        return players.stream()
                .filter(Player::isConnected)
                .sorted(Comparator.comparing(Player::getTimeLoggedIn))
                .map(Player::getOnlinePlayers)
                .findFirst()
                .orElse(new HashMap<>());
    }

    /**
     * Returns the time since the last packet for ALL players connected to the server, this will be the minimum time.
     * @return The time connected.
     */
    public synchronized int getTimeSinceLastPacket() {
        Player player = players.stream().min(Comparator.comparingInt(Player::getTimeSinceLastPacket)).orElse(null);
        return player == null ? 0 : player.getTimeSinceLastPacket();
    }

    public synchronized boolean hasAccountsIn(Dimension dimension) {
        return players.stream().anyMatch(player -> player.getDimension() == dimension);
    }

    public synchronized boolean isConnected() {
        return players.stream().anyMatch(Player::isConnected);
    }

    public void setNameForUUID(UUID uuid, String name) {
        UUIDtoNameCache.put(uuid, name);
    }

    public String getNameForUUID(UUID uuid) {
        return UUIDtoNameCache.getOrDefault(uuid, "");
    }

    /* ------------------------ Setters and Getters ------------------------ */

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    private class SessionReactionAdapter extends SessionAdapter {

        private final Player player;

        public SessionReactionAdapter(Player player) {
            this.player = player;
        }

        @Override
        public void packetReceived(PacketReceivedEvent event) {
            player.onPacket(event.getPacket());
        }

        @Override
        public void connected(ConnectedEvent event) {
            yesCom.invalidMoveHandler.addHandle(player);
        }

        @Override
        public void disconnected(DisconnectedEvent event) {
            if (event.getCause() != null) {
                yesCom.logger.warning(event.getReason());
                yesCom.logger.throwing(SessionReactionAdapter.class.getSimpleName(), "disconnected", event.getCause());
            }

            if (yesCom.ycHandler != null) yesCom.ycHandler.onPlayerLogout(player, event.getReason());
            yesCom.logger.info(String.format("%s was disconnected for: %s",
                    player.getAuthService().getSelectedProfile().getName(), event.getReason()));
            yesCom.invalidMoveHandler.removeHandle(player);
            lastLoginTime = System.currentTimeMillis(); // Do this because if we get chain kicked we don't want to try and login immediately
        }

        public Player getPlayer() {
            return player;
        }
    }
}
