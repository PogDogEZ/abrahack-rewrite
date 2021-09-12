package ez.pogdog.yescom;

import ez.pogdog.yescom.handlers.AccountHandler;
import ez.pogdog.yescom.handlers.ConfigHandler;
import ez.pogdog.yescom.handlers.QueryHandler;
import ez.pogdog.yescom.handlers.SaveHandler;
import ez.pogdog.yescom.handlers.connection.ConnectionHandler;
import ez.pogdog.yescom.handlers.invalidmove.InvalidMoveHandler;
import ez.pogdog.yescom.handlers.tracking.TrackedPlayerHandler;
import ez.pogdog.yescom.handlers.tracking.TrackingHandler;
import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.jclient.handlers.YCHandler;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.task.BasicScanTask;
import ez.pogdog.yescom.task.ITask;
import ez.pogdog.yescom.logging.LogLevel;
import ez.pogdog.yescom.logging.Logger;
import ez.pogdog.yescom.task.SpiralScanTask;
import ez.pogdog.yescom.task.StaticScanTask;
import ez.pogdog.yescom.tracking.ITracker;
import ez.pogdog.yescom.tracking.TrackedPlayer;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;
import me.iska.jclient.impl.user.Group;
import me.iska.jclient.impl.user.User;
import me.iska.jclient.network.Connection;
import me.iska.jclient.network.handler.DefaultHandler;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PogDog Software Suite Presents:
 *
 * Yescom© - Sequel to Nocom; a project carried out by NerdsInc
 * Nocom information found here: https://github.com/nerdsinspace/nocom-explanation/blob/main/README.md
 *
 * More info on this program: https://github.com/node3112/coordexploit/blob/main/README.md
 *
 * Project Credits:
 * [soon]
 */
public class YesCom {

    public static YesCom instance;

    public static YesCom getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        Logger tempLogger = new Logger("yescom", LogLevel.DEBUG);
        tempLogger.setShowName(false);

        Options options = new Options();

        Option logLevelOpt = new Option("l", "logLevel", true, "The log level.");
        logLevelOpt.setType(LogLevel.class);
        options.addOption(logLevelOpt);

        Option accountsFileOpt = new Option("af", "accountsFile", true, "The path to the accounts file.");
        accountsFileOpt.setArgName("path");
        options.addOption(accountsFileOpt);

        Option rulesFileOpt = new Option("rf", "configFile", true, "The path to the config file.");
        rulesFileOpt.setArgName("path");
        options.addOption(rulesFileOpt);

        Option hostOpt = new Option("h", "host", true, "The host IP to connect to.");
        hostOpt.setRequired(true);
        options.addOption(hostOpt);

        Option portOpt = new Option("p", "port", true, "The port to use.");
        portOpt.setType(Integer.class);
        options.addOption(portOpt);

        CommandLineParser parser = new DefaultParser();

        tempLogger.info("Welcome to YesCom \ud83d\ude08!!!!");

        try {
            CommandLine cmd = parser.parse(options, args);
            String logLevel = cmd.getOptionValue("logLevel");
            String accountsFile = cmd.getOptionValue("accountsFile");
            String configFile = cmd.getOptionValue("configFile");
            String host = cmd.getOptionValue("host");
            String port = cmd.getOptionValue("port");

            try {
                tempLogger.setLogLevel(LogLevel.valueOf(logLevel.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException | NullPointerException ignored) {
            }

            instance = new YesCom(tempLogger.getLogLevel(),
                    accountsFile == null ? "accounts.txt" : accountsFile,
                    configFile == null ? "config.yml" : configFile,
                    host, port == null ? 25565 : Integer.decode(port));
            Runtime.getRuntime().addShutdownHook(new Thread(instance::exit));
            instance.run();

        } catch (ParseException error) {
            tempLogger.fatal("Couldn't parse command line args.");
            tempLogger.error(error.toString());

            System.exit(1);
        }
    }

    public final Logger logger;

    public final Connection connection;
    public final YCHandler handler;

    public final AccountHandler accountHandler;
    public final ConfigHandler configHandler;
    public final ConnectionHandler connectionHandler;
    public final InvalidMoveHandler invalidMoveHandler;
    public final QueryHandler queryHandler;
    public final SaveHandler saveHandler;
    public final TrackingHandler trackingHandler;
    public final TrackedPlayerHandler trackedPlayerHandler;

    private final List<ITask> currentTasks = new ArrayList<>();

    private boolean alive;
    private int taskID;

    public YesCom(LogLevel logLevel, String accountFilePath, String configFilePath, String host, int port) {
        instance = this;

        logger = new Logger("yescom", logLevel);
        logger.setShowName(false);

        logger.info("Initializing...");

        logger.debug("Registering packets...");
        YCRegistry.registerPackets();
        logger.debug("Done.");

        configHandler = new ConfigHandler(configFilePath);
        accountHandler = new AccountHandler(accountFilePath);
        connectionHandler = new ConnectionHandler(host, port);
        invalidMoveHandler = new InvalidMoveHandler();
        queryHandler = new QueryHandler();
        saveHandler = new SaveHandler();
        trackedPlayerHandler = new TrackedPlayerHandler();
        trackingHandler = new TrackingHandler();

        /*
        connection = new Connection(configHandler.HOST_NAME, configHandler.HOST_PORT, logger);
        connection.setAuthSuppliers(
                () -> new User(configHandler.USERNAME, 1, 4,
                        new Group(configHandler.GROUP_NAME, 1, 4)),
                () -> configHandler.PASSWORD);
        handler = new YCHandler(connection, "test");
        connection.addHandler(handler);
         */

        connection = null;
        handler = null;

        alive = true;
        taskID = 0;

        //currentTasks.add(new StaticScanTask(Dimension.NETHER, null));
        addTask(new SpiralScanTask(new ChunkPosition(0, 0), 12, Dimension.OVERWORLD, IQuery.Priority.LOW));

        logger.info("Done.");
    }

    private void onTick() {
        if (connection != null && !connection.isConnected() && !connection.isExited()) {
            try {
                connection.connect();
            } catch (IOException error) {
                error.printStackTrace();
            }
            return; // Don't tick until we've connected
        } else if (connection != null && connection.isExited()) {
            logger.fatal(String.format("Connection closed due to: %s", connection.getExitReason()));
            exit();
            return;
        }

        if (connection != null && handler != null && connection.getHandler() instanceof DefaultHandler &&
                !handler.isInitialized())
            handler.initConnection();

        configHandler.onTick();

        accountHandler.onTick();
        connectionHandler.onTick();

        new ArrayList<>(currentTasks).forEach(task -> {
            if (task.isFinished()) {
                logger.debug(String.format("Finished task: %s.", task));
                removeTask(task);
            } else {
                task.onTick();
            }
        });

        new ArrayList<>(trackingHandler.activeTrackers).forEach(tracker -> {
            if (tracker.isLost()) {
                logger.debug(String.format("Lost tracker with ID: %s.", tracker.getID()));
                trackingHandler.activeTrackers.remove(tracker);
            } else {
                tracker.onTick();
            }
        });

        invalidMoveHandler.onTick();
        queryHandler.onTick();

        trackingHandler.onTick();
        trackedPlayerHandler.onTick();

        saveHandler.onTick();
    }

    @SuppressWarnings("BusyWait")
    public void run() {
        while (alive) {
            long startTime = System.currentTimeMillis();
            onTick();
            long finishTime = System.currentTimeMillis();

            if (finishTime - startTime < 50) {
                try {
                    Thread.sleep(50 - (finishTime - startTime));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public void exit() {
        if (alive) {
            logger.info("Exiting...");
            alive = false;

            if (connection != null && (connection.isConnected() || !connection.isExited())) connection.exit("Shutdown.");

            accountHandler.onExit();
            connectionHandler.onExit();

            invalidMoveHandler.onExit();
            queryHandler.onExit();

            trackingHandler.onExit();
            trackedPlayerHandler.onExit();

            saveHandler.onExit();
            configHandler.onExit();
            logger.info("Done!");
        }
    }

    public void addTask(ITask task) {
        if (!currentTasks.contains(task)) {
            currentTasks.add(task);
            task.setID(taskID++);
            if (handler != null) handler.onTaskAdded(task);
        }
    }

    public void removeTask(ITask task) {
        if (currentTasks.remove(task)) {
            task.onFinished();
            if (handler != null) handler.onTaskAdded(task);
        }
    }

    public void removeTask(int taskID) {
        ITask task = currentTasks.remove(taskID);
        if (task != null) {
            task.onFinished();
            if (handler != null) handler.onTaskRemoved(task);
        }
    }
}
