package ez.pogdog.yescom.network.handlers;

import ez.pogdog.yescom.events.InfoUpdateEvent;
import ez.pogdog.yescom.events.ReporterAddedEvent;
import ez.pogdog.yescom.events.ReporterRemovedEvent;
import ez.pogdog.yescom.events.config.SyncConfigRuleEvent;
import ez.pogdog.yescom.events.data.DataBroadcastEvent;
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
import ez.pogdog.yescom.network.packets.listening.*;
import ez.pogdog.yescom.network.packets.shared.*;
import me.iska.jserver.event.Listener;
import me.iska.jserver.network.Connection;
import me.iska.jserver.network.packet.Packet;

import java.util.*;

public class YCListener extends YCHandler {

    private final Map<UUID, String> playerLogins = new HashMap<>();
    private final List<UUID> playerLogouts = new ArrayList<>();

    private final String handlerName;

    private YCReporter currentReporter;
    private boolean accountAction;
    private boolean configAction;
    private ActionRequestPacket.Action otherAction;

    public YCListener(Connection connection, int handlerID, String handlerName) {
        super(connection, handlerID);

        jServer.eventBus.register(this);

        this.handlerName = handlerName;

        currentReporter = null;
        accountAction = false;
        configAction = false;
        otherAction = null;

        syncReporters();
    }

    @Override
    public void handlePacket(Packet packet) {
        if (packet instanceof DataExchangePacket) {
            DataExchangePacket dataExchange = (DataExchangePacket)packet;

            switch (dataExchange.getRequestType()) {
                case DOWNLOAD: {
                    break;
                }
                case UPLOAD: {
                    break;
                }
            }

        } else if (packet instanceof ReporterActionPacket) {
            ReporterActionPacket reporterAction = (ReporterActionPacket)packet;

            if (reporterAction.getAction() == ReporterActionPacket.Action.SELECT) {
                if (reporterAction.getReporterID() == -1 && currentReporter != null) {
                    logger.fine(String.format("Unselecting current reporter for %s.", this));

                    currentReporter = null;
                    accountAction = false; // Don't want to think we still have actions pending
                    configAction = false;
                    otherAction = null;

                    resyncCurrentReporter();

                } else {
                    YCHandler selectedHandler = yesCom.handlersManager.getHandler(reporterAction.getReporterID());

                    if (!(selectedHandler instanceof YCReporter)) {
                        resyncCurrentReporter();
                        connection.sendPacket(new ReporterActionPacket(ReporterActionPacket.Action.REMOVE,
                                reporterAction.getReporterID(), ""));
                        logger.fine(String.format("%s selected invalid reporter.", this));

                    } else {
                        currentReporter = (YCReporter)selectedHandler;
                        accountAction = false;
                        configAction = false;
                        otherAction = null;

                        logger.fine(String.format("%s selected reporter %s.", this, currentReporter));
                        resyncCurrentReporter();
                    }
                }
            }

        } else if (packet instanceof ConfigActionPacket) {
            ConfigActionPacket configAction = (ConfigActionPacket)packet;

            if (accountAction || this.configAction || otherAction != null) {
                connection.sendPacket(new ActionResponsePacket(-1, false, "Action already in progress."));
                return;

            } else if (currentReporter == null) {
                logger.fine(String.format("%s attempted account action with no reporter.", this));
                connection.sendPacket(new ActionResponsePacket(-1, false, "No current reporter."));
                resyncCurrentReporter();
                return;
            }

            switch (configAction.getAction()) {
                case SET_RULE: {
                    this.configAction = true;
                    currentReporter.setConfigRule(getID(), configAction.getRule(), configAction.getValue());
                    break;
                }
                case GET_RULE: {
                    this.configAction = true;
                    currentReporter.getConfigRule(getID(), configAction.getRuleName());
                    break;
                }
                case SYNC_RULE: {
                    connection.sendPacket(new ActionResponsePacket(-1, false, "Invalid action."));
                    break;
                }
            }

        } else if (packet instanceof TaskActionPacket) {
            TaskActionPacket taskAction = (TaskActionPacket)packet;

            switch (taskAction.getAction()) {
                case START: {
                    if (currentReporter == null) {
                        logger.fine(String.format("%s attempted to start task with no reporter.", this));
                        resyncCurrentReporter();

                    } else {
                        currentReporter.startTask(taskAction.getTaskName(), taskAction.getTaskParameters());
                    }
                    break;
                }
                case STOP: {
                    if (currentReporter == null) {
                        logger.fine(String.format("%s attempted to stop task with no reporter.", this));
                        resyncCurrentReporter();

                    } else {
                        currentReporter.stopTask(taskAction.getTaskID());
                    }
                    break;
                }
            }

        } else if (packet instanceof AccountActionPacket) {
            AccountActionPacket accountAction = (AccountActionPacket)packet;

            if (this.accountAction || configAction || otherAction != null) {
                connection.sendPacket(new ActionResponsePacket(-1, false, "Action already in progress."));
                return;

            } else if (currentReporter == null) {
                logger.fine(String.format("%s attempted account action with no reporter.", this));
                connection.sendPacket(new ActionResponsePacket(-1, false, "No current reporter."));
                resyncCurrentReporter();
                return;
            }

            this.accountAction = true;

            switch (accountAction.getAction()) {
                case LOGIN: {
                    if (accountAction.isLegacy()) {
                        currentReporter.loginAccount(getID(), accountAction.getUsername(), accountAction.getPassword());
                    } else {
                        currentReporter.loginAccount(getID(), accountAction.getUsername(), accountAction.getClientToken(),
                                accountAction.getAccessToken());
                    }
                    break;
                }
                case LOGOUT: {
                    currentReporter.logoutAccount(getID(), accountAction.getUsername());
                    break;
                }
            }

        } else if (packet instanceof TrackerActionPacket) {
            TrackerActionPacket trackerAction = (TrackerActionPacket)packet;

            if (trackerAction.getAction() == TrackerActionPacket.Action.UNTRACK) {
                if (currentReporter == null) {
                    logger.fine(String.format("%s attempted to untrack tracker with no reporter.", this));
                    resyncCurrentReporter();
                    return;
                }

                currentReporter.untrackTracker(trackerAction.getTrackerID());
            }

        } else if (packet instanceof ActionRequestPacket) {
            ActionRequestPacket actionRequest = (ActionRequestPacket)packet;

            if (accountAction || configAction || otherAction != null) {
                connection.sendPacket(new ActionResponsePacket(-1, false, "Action already in progress."));
                return;

            } else if (currentReporter == null) {
                logger.fine(String.format("%s attempted action with no reporter.", this));
                connection.sendPacket(new ActionResponsePacket(-1, false, "No current reporter."));
                resyncCurrentReporter();
                return;
            }

            otherAction = actionRequest.getAction();
            currentReporter.requestAction(getID(), actionRequest.getAction(), actionRequest.getData());
        }
    }

    @Override
    public synchronized void update() {
        if (!playerLogins.isEmpty()) {
            connection.sendPacket(new OnlinePlayersActionPacket(playerLogins));
            playerLogins.clear();
        }

        if (!playerLogouts.isEmpty()) {
            connection.sendPacket(new OnlinePlayersActionPacket(playerLogouts));
            playerLogouts.clear();
        }
    }

    @Override
    public void exit(String reason) {
        jServer.eventBus.unregister(this);
    }

    /* ----------------------------- Listeners ----------------------------- */

    @Listener
    public void onReporterAdded(ReporterAddedEvent event) {
        connection.sendPacket(new ReporterActionPacket(ReporterActionPacket.Action.ADD, event.getReporter().getID(),
                event.getReporter().getName()));
    }

    @Listener
    public void onReporterRemoved(ReporterRemovedEvent event) {
        connection.sendPacket(new ReporterActionPacket(ReporterActionPacket.Action.REMOVE, event.getReporter().getID(),
                event.getReporter().getName()));

        if (event.getReporter() == currentReporter) {
            currentReporter = null;
            resyncCurrentReporter();
        }
    }

    @Listener
    public void onDataBroadcast(DataBroadcastEvent event) {
        if (event.getReporter() == currentReporter)
            connection.sendPacket(new DataExchangePacket(event.getDataType(), event.getData(), new ArrayList<>()));
    }

    @Listener
    public void onSyncConfigRule(SyncConfigRuleEvent event) {
        if (event.getReporter() == currentReporter)
            connection.sendPacket(new ConfigActionPacket(ConfigActionPacket.Action.SYNC_RULE, event.getConfigRule(),
                    event.getValue()));
    }

    @Listener
    public void onTaskAdded(TaskAddedEvent event) {
        if (event.getReporter() == currentReporter) connection.sendPacket(new TaskActionPacket(TaskActionPacket.Action.ADD, event.getTask()));
    }

    @Listener
    public void onTaskRemoved(TaskRemovedEvent event) {
        if (event.getReporter() == currentReporter) connection.sendPacket(new TaskActionPacket(TaskActionPacket.Action.REMOVE, event.getTask()));
    }

    @Listener
    public void onTaskUpdate(TaskUpdatedEvent event) {
        if (event.getReporter() == currentReporter)
            connection.sendPacket(new TaskActionPacket(event.getTask().getID(), event.isLoadedChunkTask(),
                    event.getProgress(), event.getTimeElapsed(), event.getCurrentPosition()));
    }

    @Listener
    public void onTaskResult(TaskResultEvent event) {
        if (event.getReporter() == currentReporter)
            connection.sendPacket(new TaskActionPacket(event.getTask(), event.getResult()));
    }

    @Listener
    public void onPlayerAdded(PlayerAddedEvent event) {
        if (event.getReporter() == currentReporter)
            connection.sendPacket(new PlayerActionPacket(event.getPlayer()));
    }

    @Listener
    public void onPlayerRemoved(PlayerRemovedEvent event) {
        if (event.getReporter() == currentReporter)
            connection.sendPacket(new PlayerActionPacket(event.getPlayer(), event.getReason()));
    }

    @Listener
    public void onPlayerUpdate(PlayerUpdatedEvent event) {
        if (event.getReporter() == currentReporter) {
            switch (event.getUpdateType()) {
                case POSITION: {
                    connection.sendPacket(new PlayerActionPacket(event.getPlayer().getUsername(), event.getNewPosition(),
                            event.getNewAngle()));
                    break;
                }
                case DIMENSION: {
                    connection.sendPacket(new PlayerActionPacket(event.getPlayer().getUsername(), event.getNewDimension()));
                    break;
                }
                case HEALTH: {
                    connection.sendPacket(new PlayerActionPacket(event.getPlayer().getUsername(), event.getNewHealth(),
                            event.getNewHunger(), event.getNewSaturation()));
                    break;
                }
            }
        }
    }

    @Listener
    public void onTrackerAdded(TrackerAddedEvent event) {
        if (event.getReporter() == currentReporter)
            connection.sendPacket(new TrackerActionPacket(TrackerActionPacket.Action.ADD, event.getTracker()));
    }

    @Listener
    public void onTrackerRemoved(TrackerRemovedEvent event) {
        if (event.getReporter() == currentReporter)
            connection.sendPacket(new TrackerActionPacket(TrackerActionPacket.Action.REMOVE, event.getTracker()));
    }

    @Listener
    public void onTrackerUpdate(TrackerUpdatedEvent event) {
        if (event.getReporter() == currentReporter)
            connection.sendPacket(new TrackerActionPacket(TrackerActionPacket.Action.UPDATE, event.getTracker()));
    }

    @Listener
    public void onInfoUpdate(InfoUpdateEvent event) {
        if (event.getReporter() == currentReporter)
            connection.sendPacket(new InfoUpdatePacket(event.getWaitingQueries(), event.getTickingQueries(),
                    event.getQueriesPerSecond(), event.isConnected(), event.getTickRate(), event.getServerPing(),
                    event.getTimeSinceLastPacket()));
    }

    @Listener
    public synchronized void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getReporter() == currentReporter) {
            playerLogins.put(event.getUUID(), event.getDisplayName());
            playerLogouts.remove(event.getUUID());
        }
    }

    @Listener
    public synchronized void onPlayerLogout(PlayerLogoutEvent event) {
        if (event.getReporter() == currentReporter) {
            playerLogouts.add(event.getUUID());
            playerLogins.remove(event.getUUID());
        }
    }

    /* ----------------------------- Sync stuff ----------------------------- */

    /**
     * Informs the listener of all the current reporters.
     */
    public void syncReporters() {
        logger.fine(String.format("%s syncing...", this));
        for (YCReporter reporter : yesCom.handlersManager.getReporters())
            connection.sendPacket(new ReporterActionPacket(ReporterActionPacket.Action.ADD, reporter.getID(), reporter.getName()));
        logger.fine("Done.");
    }

    /**
     * Resynchronizes the current reporter.
     */
    public void resyncCurrentReporter() {
        if (currentReporter != null) {
            connection.sendPacket(new ReporterSyncPacket(currentReporter.getID(), currentReporter.getName(),
                    currentReporter.getConfigRules(), currentReporter.getRegisteredTasks(), currentReporter.getActiveTasks(),
                    currentReporter.getPlayers(), currentReporter.getTrackers()));
            connection.sendPacket(new OnlinePlayersActionPacket(currentReporter.getOnlinePlayers()));
        } else {
            connection.sendPacket(new ReporterSyncPacket());
        }
    }

    /* ----------------------------- Other ----------------------------- */

    public void onActionResponse(boolean successful, String message) {
        if (accountAction || configAction || otherAction != null) {
            accountAction = false;
            configAction = false;
            otherAction = null;

            connection.sendPacket(new ActionResponsePacket(-1, successful, message));
        }
    }

    /* ----------------------------- Setters and getters ----------------------------- */

    public String getName() {
        return handlerName;
    }

    public YCReporter getCurrentReporter() {
        return currentReporter;
    }
}
