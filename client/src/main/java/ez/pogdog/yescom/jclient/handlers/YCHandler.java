package ez.pogdog.yescom.jclient.handlers;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.jclient.packets.AccountActionPacket;
import ez.pogdog.yescom.jclient.packets.AccountActionResponsePacket;
import ez.pogdog.yescom.jclient.packets.LoadedChunkPacket;
import ez.pogdog.yescom.jclient.packets.PlayerActionPacket;
import ez.pogdog.yescom.jclient.packets.TaskActionPacket;
import ez.pogdog.yescom.jclient.packets.TaskSyncPacket;
import ez.pogdog.yescom.jclient.packets.YCInitRequestPacket;
import ez.pogdog.yescom.jclient.packets.YCInitResponsePacket;
import ez.pogdog.yescom.task.ITask;
import ez.pogdog.yescom.task.TaskRegistry;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;
import me.iska.jclient.network.Connection;
import me.iska.jclient.network.handler.IHandler;
import me.iska.jclient.network.packet.Packet;

public class YCHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final Connection connection;
    private final String handlerName;

    private boolean initializing;
    private boolean initialized;

    public YCHandler(Connection connection, String handlerName) {
        this.connection = connection;
        this.handlerName = handlerName;

        initializing = false;
        initialized = false;
    }

    @Override
    public void handlePacket(Packet packet) {
        if (packet instanceof YCInitResponsePacket) {
            if (!initializing && initialized) {
                connection.exit("Got init response when already initialized.");
                yesCom.exit();
                return;
            }

            initializing = false;
            initialized = true;

            YCInitResponsePacket initResponse = (YCInitResponsePacket)packet;

            if (initResponse.isRejected()) {
                connection.exit(String.format("Rejected for: %s.", initResponse.getMessage()));
                yesCom.exit();
                return;
            }

            yesCom.logger.debug(String.format("We are handler ID %d.", initResponse.getHandlerID()));

            yesCom.logger.debug("Syncing tasks...");
            connection.sendPacket(new TaskSyncPacket());
            yesCom.logger.debug("Done.");

            yesCom.logger.info("Successfully initialized YC connection.");

        } else if (packet instanceof TaskActionPacket) {
            TaskActionPacket taskAction = (TaskActionPacket)packet;

            yesCom.logger.debug("Got task action.");

            switch (taskAction.getAction()) {
                case START: {
                    yesCom.addTask(taskAction.getTask());
                    break;
                }
                case STOP: {
                    yesCom.removeTask(taskAction.getTaskID());
                    break;
                }
            }

        } else if (packet instanceof AccountActionPacket) {
            AccountActionPacket accountAction = (AccountActionPacket)packet;

            yesCom.logger.debug("Got account action.");

            switch (accountAction.getAction()) {
                case ADD: {
                    boolean success;
                    String message;

                    try {
                        yesCom.accountHandler.login(accountAction.getUsername(), accountAction.getPassword());
                        success = true;
                        message = "Successfully logged in.";
                    } catch (RequestException error) {
                        yesCom.logger.warn(String.format("Error while logging in account: %s.", accountAction.getUsername()));
                        yesCom.logger.error(error.toString());

                        success = false;
                        message = error.getMessage();
                    }

                    connection.sendPacket(new AccountActionResponsePacket(accountAction.getActionID(), success, message));
                    break;
                }
                case REMOVE: {
                    yesCom.accountHandler.logout(accountAction.getUsername());
                    break;
                }
            }
        }
    }

    public void initConnection() {
        yesCom.logger.debug("Initializing YC connection...");

        connection.sendPacket(new YCInitRequestPacket(handlerName, yesCom.connectionHandler.getHost(),
                yesCom.connectionHandler.getPort(), false));
        initializing = true;
    }

    public boolean isInitialized() {
        return initialized || initializing;
    }

    /* ------------------------ Events ------------------------ */

    public void onTaskAdded(ITask task) {
        connection.sendPacket(new TaskActionPacket(TaskActionPacket.Action.ADD, task, TaskRegistry.getTask(task.getClass()), task.getID()));
    }

    public void onTaskRemoved(ITask task) {
        connection.sendPacket(new TaskActionPacket(TaskActionPacket.Action.REMOVE, null, null, task.getID()));
    }

    public void onPlayerAdded(Player player) {
        connection.sendPacket(new PlayerActionPacket(PlayerActionPacket.Action.ADD, player));
    }

    public void onPlayerRemoved(Player player) {
        connection.sendPacket(new PlayerActionPacket(PlayerActionPacket.Action.REMOVE, player));
    }

    public void onPositionChanged(Player player) {
        connection.sendPacket(new PlayerActionPacket(player.getAuthService().getUsername(), player.getPosition(), player.getAngle()));
    }

    public void onDimensionChanged(Player player) {
        connection.sendPacket(new PlayerActionPacket(player.getAuthService().getUsername(), player.getDimension()));
    }

    public void onHealthChanged(Player player) {
        connection.sendPacket(new PlayerActionPacket(player.getAuthService().getUsername(), player.getFoodStats()));
    }

    public void onLoadedChunk(ChunkPosition chunkPosition, Dimension dimension) {
        connection.sendPacket(new LoadedChunkPacket(chunkPosition, dimension));
    }
}