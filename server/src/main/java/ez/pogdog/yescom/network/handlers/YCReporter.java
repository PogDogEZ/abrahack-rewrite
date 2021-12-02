package ez.pogdog.yescom.network.handlers;

import ez.pogdog.yescom.events.data.DataBroadcastEvent;
import ez.pogdog.yescom.events.InfoUpdateEvent;
import ez.pogdog.yescom.events.config.SyncConfigRuleEvent;
import ez.pogdog.yescom.events.online.PlayerLoginEvent;
import ez.pogdog.yescom.events.online.PlayerLogoutEvent;
import ez.pogdog.yescom.events.player.PlayerAddedEvent;
import ez.pogdog.yescom.events.player.PlayerRemovedEvent;
import ez.pogdog.yescom.events.player.PlayerUpdatedEvent;
import ez.pogdog.yescom.events.task.TaskAddedEvent;
import ez.pogdog.yescom.events.task.TaskRemovedEvent;
import ez.pogdog.yescom.events.task.TaskResultEvent;
import ez.pogdog.yescom.events.task.TaskUpdatedEvent;
import ez.pogdog.yescom.events.tracker.TrackerAddedEvent;
import ez.pogdog.yescom.events.tracker.TrackerRemovedEvent;
import ez.pogdog.yescom.events.tracker.TrackerUpdatedEvent;
import ez.pogdog.yescom.network.packets.reporting.*;
import ez.pogdog.yescom.network.packets.shared.*;
import ez.pogdog.yescom.util.ConfigRule;
import ez.pogdog.yescom.util.Player;
import ez.pogdog.yescom.util.Tracker;
import ez.pogdog.yescom.util.task.ActiveTask;
import ez.pogdog.yescom.util.task.RegisteredTask;
import ez.pogdog.yescom.util.task.parameter.Parameter;
import me.iska.jserver.network.Connection;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class YCReporter extends YCHandler {

    private final Map<Integer, Request> requests = new HashMap<>();
    private final Map<Long, Action> actions = new HashMap<>();

    private final Map<ConfigRule, Object> config = new HashMap<>();
    private final Map<Integer, ActiveTask> activeTasks = new HashMap<>();
    private final Map<UUID, String> onlinePlayers = new HashMap<>();
    private final List<RegisteredTask> registeredTasks = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
    private final List<Tracker> trackers = new ArrayList<>();

    private final String handlerName;

    private final String minecraftHost;
    private final int minecraftPort;

    private int requestID;
    private long actionID;

    public YCReporter(Connection connection, int handlerID, String handlerName, String minecraftHost, int minecraftPort) {
        super(connection, handlerID);

        this.handlerName = handlerName;
        this.minecraftHost = minecraftHost;
        this.minecraftPort = minecraftPort;

        requestID = 0;
        actionID = 0;
    }

    @Override
    public String toString() {
        return String.format("YCReporter(ID=%d, name=%s)", getID(), handlerName);
    }

    @Override
    public synchronized void handlePacket(Packet packet) {
        if (packet instanceof DataExchangePacket) {
            DataExchangePacket dataExchange = (DataExchangePacket)packet;

            switch (dataExchange.getRequestType()) {
                case DOWNLOAD: {
                    break;
                }
                case UPLOAD: {
                    if (dataExchange.getRequestID() == -1) {
                        jServer.eventBus.post(new DataBroadcastEvent(this, dataExchange.getDataType(), dataExchange.getData()));

                    } else {
                        if (!requests.containsKey(dataExchange.getRequestID())) {
                            logger.warning(String.format("Received data exchange packet for unknown request ID %d", dataExchange.getRequestID()));
                            connection.exit("Invalid data request ID.");
                            return;
                        }

                        Request request = requests.get(dataExchange.getRequestID());
                        requests.remove(dataExchange.getRequestID());

                        YCHandler originator = yesCom.handlersManager.getHandler(request.getOriginatorID());

                        if (originator == null) { // They could've disconnected
                            logger.finer(String.format("Received data exchange packet for unknown originator ID %d.", request.getOriginatorID()));
                            return;
                        }

                        originator.provideData(dataExchange.getDataType(), dataExchange.getData(), dataExchange.getInvalidDataIDs());
                    }
                    break;
                }
            }

        } else if (packet instanceof ConfigSyncPacket) {
            ConfigSyncPacket configSync = (ConfigSyncPacket)packet;

            logger.finer(String.format("Syncing config for %s.", this));
            config.clear();
            config.putAll(configSync.getRules());
            logger.finer(String.format("%d rules synced for %s.", config.size(), this));

            // Do this in case any listeners are added before we are fully synced
            config.forEach((rules, value) -> jServer.eventBus.post(new SyncConfigRuleEvent(this, rules, value)));

        } else if (packet instanceof ConfigActionPacket) {
            ConfigActionPacket configAction = (ConfigActionPacket)packet;

            switch (configAction.getAction()) {
                case SET_RULE:
                case GET_RULE: {
                    connection.exit(String.format("Invalid config action: %s", configAction.getAction()));
                    break;
                }
                case SYNC_RULE: {
                    logger.finer(String.format("Syncing rule %s (%s) for %s.", configAction.getRule(), configAction.getValue(), this));
                    config.put(configAction.getRule(), configAction.getValue());

                    if (configAction.getActionID() == -1) {
                        jServer.eventBus.post(new SyncConfigRuleEvent(this, configAction.getRule(), configAction.getValue()));

                    } else {
                        if (!actions.containsKey(configAction.getActionID())) {
                            logger.warning(String.format("Received config action packet for unknown action ID %d.", configAction.getActionID()));
                            connection.exit("Invalid config action ID.");
                            return;
                        }

                        Action action = actions.get(configAction.getActionID());
                        // Don't remove the action as we should get an action response after this packet
                        // actions.remove(configAction.getActionID());

                        if (action.getOriginatorID() == -1) return;
                        YCHandler originator = yesCom.handlersManager.getHandler(action.getOriginatorID());

                        if (!(originator instanceof YCListener) || ((YCListener)originator).getCurrentReporter() != this) {
                            logger.finer(String.format("Received config action packet for invalid originator ID %d.", action.getOriginatorID()));
                            return;
                        }

                        // We don't want to broadcast it everywhere if it's directed at one listener
                        ((YCListener)originator).onSyncConfigRule(new SyncConfigRuleEvent(this, configAction.getRule(),
                                configAction.getValue()));
                    }
                    break;
                }
            }

        } else if (packet instanceof TaskSyncPacket) {
            TaskSyncPacket taskSync = (TaskSyncPacket)packet;

            logger.finer(String.format("Syncing tasks for %s.", this));
            registeredTasks.clear();
            registeredTasks.addAll(taskSync.getTasks());
            logger.finer(String.format("%d tasks synced for %s.", registeredTasks.size(), this));

            // Fix for a bug when selecting reporters before fully syncing, causes registered tasks not to be accounted
            // for on the listener
            yesCom.handlersManager.getListeners().forEach(listener -> {
                if (listener.getCurrentReporter() == this) listener.resyncCurrentReporter();
            });

        } else if (packet instanceof TaskActionPacket) {
            TaskActionPacket taskAction = (TaskActionPacket)packet;

            switch (taskAction.getAction()) {
                case START:
                case STOP: {
                    connection.exit(String.format("Invalid task action: %s", taskAction.getAction()));
                    break;
                }
                case ADD: {
                    RegisteredTask registeredTask = getRegisteredTask(taskAction.getTaskName());
                    if (registeredTask == null) {
                        connection.exit(String.format("Invalid task name: %s", taskAction.getTaskName()));
                        return;
                    }

                    ActiveTask activeTask = new ActiveTask(registeredTask, taskAction.getTaskID(), taskAction.getTaskParameters(),
                            0.0f, 0, new ArrayList<>());
                    logger.finer(String.format("Adding active task: %s", activeTask));
                    activeTasks.put(taskAction.getTaskID(), activeTask);

                    jServer.eventBus.post(new TaskAddedEvent(this, activeTask));
                    break;
                }
                case REMOVE: {
                    if (!activeTasks.containsKey(taskAction.getTaskID())) {
                        connection.exit(String.format("Invalid task ID: %d", taskAction.getTaskID()));

                    } else {
                        ActiveTask task = activeTasks.get(taskAction.getTaskID());
                        logger.finer(String.format("Removing active task: %s", task));
                        activeTasks.remove(taskAction.getTaskID());

                        jServer.eventBus.post(new TaskRemovedEvent(this, task));
                    }
                    break;
                }
                case UPDATE: {
                    ActiveTask activeTask = activeTasks.get(taskAction.getTaskID());
                    if (activeTask == null) {
                        connection.exit(String.format("Invalid task ID: %d", taskAction.getTaskID()));
                        return;
                    }

                    jServer.eventBus.post(new TaskUpdatedEvent(this, activeTask, taskAction.isLoadedChunkTask(),
                            taskAction.getProgress(), taskAction.getTimeElapsed(), taskAction.getCurrentPosition()));

                    activeTask.setLoadedChunkTask(taskAction.isLoadedChunkTask());
                    activeTask.setProgress(taskAction.getProgress());
                    activeTask.setTimeElapsed(taskAction.getTimeElapsed());
                    if (taskAction.isLoadedChunkTask()) activeTask.setCurrentPosition(taskAction.getCurrentPosition());
                    break;
                }
                case RESULT: {
                    ActiveTask activeTask = activeTasks.get(taskAction.getTaskID());
                    if (activeTask == null) {
                        connection.exit(String.format("Invalid task ID: %d", taskAction.getTaskID()));
                        return;
                    }

                    logger.finer(String.format("New result for active task %s: %s", activeTask, taskAction.getResult()));
                    activeTask.addResult(taskAction.getResult());

                    jServer.eventBus.post(new TaskResultEvent(this, activeTask, taskAction.getResult()));
                    break;
                }
            }

        } else if (packet instanceof PlayerActionPacket) {
            PlayerActionPacket playerAction = (PlayerActionPacket)packet;

            switch (playerAction.getAction()) {
                case ADD: {
                    logger.finer(String.format("Adding player: %s", playerAction.getPlayer()));
                    players.add(playerAction.getPlayer());

                    jServer.eventBus.post(new PlayerAddedEvent(this, playerAction.getPlayer()));
                    break;
                }
                case REMOVE: {
                    Player player = getPlayer(playerAction.getPlayerName());
                    if (player == null) {
                        connection.exit("Invalid player name: " + playerAction.getPlayerName());
                        return;
                    }

                    logger.finer(String.format("Removing player: %s", player));
                    players.remove(player);

                    jServer.eventBus.post(new PlayerRemovedEvent(this, player, playerAction.getDisconnectReason()));
                    break;
                }
                case UPDATE_POSITION: {
                    Player player = getPlayer(playerAction.getPlayerName());
                    if (player == null) {
                        connection.exit("Invalid player name: " + playerAction.getPlayerName());
                        return;
                    }

                    // logger.finer(String.format("Updating player position: %s", player));

                    jServer.eventBus.post(new PlayerUpdatedEvent(this, player, playerAction.getNewPosition(), playerAction.getNewAngle()));

                    player.setPosition(playerAction.getNewPosition());
                    player.setAngle(playerAction.getNewAngle());
                    break;
                }
                case UPDATE_DIMENSION: {
                    Player player = getPlayer(playerAction.getPlayerName());
                    if (player == null) {
                        connection.exit("Invalid player name: " + playerAction.getPlayerName());
                        return;
                    }

                    logger.finer(String.format("Updating player dimension: %s", player));

                    jServer.eventBus.post(new PlayerUpdatedEvent(this, player, playerAction.getNewDimension()));

                    player.setDimension(playerAction.getNewDimension());
                    break;
                }
                case UPDATE_HEALTH: {
                    Player player = getPlayer(playerAction.getPlayerName());
                    if (player == null) {
                        connection.exit("Invalid player name: " + playerAction.getPlayerName());
                        return;
                    }

                    logger.finer(String.format("Updating player health: %s", player));

                    jServer.eventBus.post(new PlayerUpdatedEvent(this, player, playerAction.getNewHealth(),
                            playerAction.getNewHunger(), playerAction.getNewSaturation()));

                    player.setHealth(playerAction.getNewHealth());
                    player.setFood(playerAction.getNewHunger());
                    player.setSaturation(playerAction.getNewSaturation());
                    break;
                }
            }

        } else if (packet instanceof TrackerActionPacket) {
            TrackerActionPacket trackerAction = (TrackerActionPacket)packet;

            switch (trackerAction.getAction()) {
                case UNTRACK: {
                    connection.exit("Invalid tracker action: UNTRACK");
                    return;
                }
                case ADD: {
                    logger.finer(String.format("Adding tracker: %s", trackerAction.getTracker()));
                    trackers.add(trackerAction.getTracker());

                    jServer.eventBus.post(new TrackerAddedEvent(this, trackerAction.getTracker()));
                    break;
                }
                case REMOVE: {
                    Tracker tracker = getTracker(trackerAction.getTrackerID());
                    if (tracker == null) {
                        connection.exit(String.format("Invalid tracker ID: %d", trackerAction.getTrackerID()));
                        return;
                    }

                    logger.finer(String.format("Removing tracker: %s", tracker));
                    trackers.remove(tracker);

                    jServer.eventBus.post(new TrackerRemovedEvent(this, tracker));
                    break;
                }
                case UPDATE: {
                    Tracker tracker = getTracker(trackerAction.getTrackerID());
                    if (tracker == null) {
                        connection.exit(String.format("Invalid tracker ID: %d", trackerAction.getTrackerID()));
                        return;
                    }

                    // logger.finer(String.format("Updating tracker: %s", tracker));
                    tracker.setTrackedPlayerIDs(trackerAction.getTrackedPlayerIDs());

                    jServer.eventBus.post(new TrackerUpdatedEvent(this, tracker));
                    break;
                }
            }

        } else if (packet instanceof InfoUpdatePacket) {
            InfoUpdatePacket infoUpdate = (InfoUpdatePacket)packet;

            jServer.eventBus.post(new InfoUpdateEvent(this, infoUpdate.getWaitingQueries(), infoUpdate.getTickingQueries(),
                    infoUpdate.getQueriesPerSecond(), infoUpdate.getIsConnected(), infoUpdate.getTickRate(), infoUpdate.getServerPing(),
                    infoUpdate.getTimeSinceLastPacket()));

        } else if (packet instanceof OnlinePlayersActionPacket) {
            OnlinePlayersActionPacket onlinePlayersAction = (OnlinePlayersActionPacket)packet;

            switch (onlinePlayersAction.getAction()) {
                case ADD: {
                    onlinePlayersAction.getOnlinePlayers().forEach((uuid, displayName) -> {
                        jServer.eventBus.post(new PlayerLoginEvent(this, uuid, displayName));

                        yesCom.putUUIDToName(uuid, displayName);
                        onlinePlayers.put(uuid, displayName);
                    });
                    break;
                }
                case REMOVE: {
                    onlinePlayersAction.getOnlinePlayers().forEach((uuid, displayName) -> {
                        jServer.eventBus.post(new PlayerLogoutEvent(this, uuid));

                        onlinePlayers.remove(uuid);
                    });
                    break;
                }
            }

        } else if (packet instanceof ActionResponsePacket) {
            ActionResponsePacket actionResponse = (ActionResponsePacket)packet;

            logger.finer(String.format("%s got action response.", this));
            if (!actions.containsKey(actionResponse.getActionID()) && actionResponse.getActionID() != -1) {
                logger.warning(String.format("Received action response packet for unknown action ID %d.", actionResponse.getActionID()));
                connection.exit("Invalid action ID.");
                return;
            }

            Action action = actions.get(actionResponse.getActionID());
            actions.remove(actionResponse.getActionID());

            if (action.getOriginatorID() == -1) return;
            YCHandler originator = yesCom.handlersManager.getHandler(action.getOriginatorID());

            if (!(originator instanceof YCListener) || ((YCListener)originator).getCurrentReporter() != this) {
                logger.finer(String.format("Received config action packet for invalid originator ID %d.", action.getOriginatorID()));
                return;
            }

            ((YCListener)originator).onActionResponse(actionResponse.isSuccessful(), actionResponse.getMessage());
        }
    }

    /* ----------------------------- Requests and actions ----------------------------- */

    @Override
    public void provideData(DataExchangePacket.DataType dataType, List<Object> data, List<BigInteger> invalidDataIDs) {
    }

    @Override
    public synchronized void requestData(int originatorID, DataExchangePacket.DataType dataType, List<BigInteger> dataIDs) {
        requests.put(requestID, new Request(requestID, originatorID));
        connection.sendPacket(new DataExchangePacket(dataType, requestID, dataIDs));

        ++requestID;
    }

    public synchronized void requestAction(int originatorID, ActionRequestPacket.Action action, byte[] data) {
        actions.put(actionID, new Action(actionID, originatorID));
        connection.sendPacket(new ActionRequestPacket(action, actionID, data));

        if (++actionID == Long.MAX_VALUE) actionID = 0; // FIXME: If we're just gonna reset it why have it as a long?
    }

    /**
     * Sends a chat message given the username and message.
     * @param originatorID The handler ID of the originator of the request.
     * @param username The username to send the chat message on.
     * @param message The message to send.
     */
    public void sendChatMessage(int originatorID, String username, String message) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            Registry.STRING.write(username, outputStream);
            Registry.STRING.write(message, outputStream);
        } catch (IOException ignored) {
        }

        requestAction(originatorID, ActionRequestPacket.Action.SEND_CHAT_MESSAGE, outputStream.toByteArray());
    }

    /* ----------------------------- Config ----------------------------- */

    public Map<ConfigRule, Object> getConfigRules() {
        return new HashMap<>(config);
    }

    /**
     * Requests that the given config rule is set to a value.
     * @param originatorID The handler ID of the originator of the request.
     * @param configRule The config rule.
     * @param value The value to set the config rule to.
     */
    public synchronized void setConfigRule(int originatorID, ConfigRule configRule, Object value) {
        actions.put(actionID, new Action(actionID, originatorID));
        connection.sendPacket(new ConfigActionPacket(ConfigActionPacket.Action.SET_RULE, actionID, configRule, value));

        if (++actionID == Long.MAX_VALUE) actionID = 0;
    }

    /**
     * Requests that the given config rule is synced.
     * @param originatorID The handler ID of the originator of the request.
     * @param ruleName The name of the rule to sync.
     */
    public synchronized void getConfigRule(int originatorID, String ruleName) {
        actions.put(actionID, new Action(actionID, originatorID));
        connection.sendPacket(new ConfigActionPacket(actionID, ruleName));

        if (++actionID == Long.MAX_VALUE) actionID = 0;
    }

    /* ----------------------------- Tasks ----------------------------- */

    public List<RegisteredTask> getRegisteredTasks() {
        return new ArrayList<>(registeredTasks);
    }

    /**
     * Gets a registered task by its name.
     * @param name The name of the task.
     * @return The registered task, null if not found.
     */
    public RegisteredTask getRegisteredTask(String name) {
        return registeredTasks.stream().filter(task -> task.getName().equals(name)).findFirst().orElse(null);
    }

    public List<ActiveTask> getActiveTasks() {
        return new ArrayList<>(activeTasks.values());
    }

    /**
     * Gets an active task by its ID.
     * @param taskID The ID of the task.
     * @return The active task, null if not found.
     */
    public ActiveTask getActiveTask(int taskID) {
        return activeTasks.get(taskID);
    }

    /**
     * Requests to start a task, given the name of the registered task and parameters.
     * @param taskName The name of the registered task.
     * @param parameters The parameters for the task.
     */
    public synchronized void startTask(int originatorID, String taskName, List<Parameter> parameters) {
        actions.put(actionID, new Action(actionID, originatorID));
        connection.sendPacket(new TaskActionPacket(actionID, taskName, parameters));

        if (++actionID == Long.MAX_VALUE) actionID = 0;
    }

    /**
     * Requests that a task, given by ID, is stopped.
     * @param taskID The ID of the task to stop.
     */
    public synchronized void stopTask(int originatorID, int taskID) {
        actions.put(actionID, new Action(actionID, originatorID));
        connection.sendPacket(new TaskActionPacket(TaskActionPacket.Action.STOP, actionID, taskID));

        if (++actionID == Long.MAX_VALUE) actionID = 0;
    }

    /* ----------------------------- Players ----------------------------- */

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    /**
     * Gets a player by their username.
     * @param username The username of the player.
     * @return The player, null if not found.
     */
    public Player getPlayer(String username) {
        return players.stream().filter(player -> player.getUsername().equalsIgnoreCase(username)).findFirst().orElse(null);
    }

    /**
     * Attempts to login an account with the legacy auth system.
     * @param originatorID The handler ID of the originator of the request.
     * @param username The username of the account.
     * @param password The password of the account.
     */
    public synchronized void loginAccount(int originatorID, String username, String password) {
        actions.put(actionID, new Action(actionID, originatorID));
        connection.sendPacket(new AccountActionPacket(actionID, username, password));

        if (++actionID == Long.MAX_VALUE) actionID = 0;
    }

    /**
     * Attempts to login an account with the new auth system.
     * @param originatorID The handler ID of the originator of the request.
     * @param username The username of the account.
     * @param clientToken The client token.
     * @param accessToken The access token.
     */
    public synchronized void loginAccount(int originatorID, String username, String clientToken, String accessToken) {
        actions.put(actionID, new Action(actionID, originatorID));
        connection.sendPacket(new AccountActionPacket(actionID, username, clientToken, accessToken));

        if (++actionID == Long.MAX_VALUE) actionID = 0;
    }

    /**
     * Attempts to logout an account.
     * @param originatorID The handler ID of the originator of the request.
     * @param username The username of the account.
     */
    public synchronized void logoutAccount(int originatorID, String username) {
        actions.put(actionID, new Action(actionID, originatorID));
        connection.sendPacket(new AccountActionPacket(actionID, username));

        if (++actionID == Long.MAX_VALUE) actionID = 0;
    }

    /* ----------------------------- Trackers ----------------------------- */

    public List<Tracker> getTrackers() {
        return new ArrayList<>(trackers);
    }

    /**
     * Gets a tracker by their ID.
     * @param trackerID The ID of the tracker.
     * @return The tracker, null if not found.
     */
    public Tracker getTracker(long trackerID) {
        return trackers.stream().filter(tracker -> tracker.getTrackerID() == trackerID).findFirst().orElse(null);
    }

    /**
     * Untracks (immediately stops) a tracker.
     * @param trackerID The ID of the tracker to untrack.
     */
    public void untrackTracker(long trackerID) {
        connection.sendPacket(new TrackerActionPacket(TrackerActionPacket.Action.UNTRACK, trackerID));
    }

    /* ----------------------------- Online players ----------------------------- */

    public Map<UUID, String> getOnlinePlayers() {
        return new HashMap<>(onlinePlayers);
    }

    /* ----------------------------- Setters and getters ----------------------------- */

    public String getName() {
        return handlerName;
    }

    public String getMinecraftHost() {
        return minecraftHost;
    }

    public int getMinecraftPort() {
        return minecraftPort;
    }

    /**
     * Contains information about a request being made.
     */
    public static class Request {

        private final int requestID;
        private final int originatorID;

        public Request(int requestID, int originatorID) {
            this.requestID = requestID;
            this.originatorID = originatorID;
        }

        public int getRequestID() {
            return requestID;
        }

        public int getOriginatorID() {
            return originatorID;
        }
    }

    /**
     * Contains information about an action being performed.
     */
    public static class Action {

        private final long actionID;
        private final int originatorID;

        public Action(long actionID, int originatorID) {
            this.actionID = actionID;
            this.originatorID = originatorID;
        }

        public long getActionID() {
            return actionID;
        }

        public int getOriginatorID() {
            return originatorID;
        }
    }
}
