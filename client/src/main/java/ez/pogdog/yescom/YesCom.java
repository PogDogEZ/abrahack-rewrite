package ez.pogdog.yescom;

import ez.pogdog.yescom.handlers.AccountHandler;
import ez.pogdog.yescom.handlers.ConfigHandler;
import ez.pogdog.yescom.handlers.QueryHandler;
import ez.pogdog.yescom.handlers.DataHandler;
import ez.pogdog.yescom.handlers.connection.ConnectionHandler;
import ez.pogdog.yescom.handlers.invalidmove.InvalidMoveHandler;
import ez.pogdog.yescom.handlers.TrackingHandler;
import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.jclient.handlers.YCHandler;
import ez.pogdog.yescom.task.ITask;
import ez.pogdog.yescom.logging.LogLevel;
import ez.pogdog.yescom.logging.Logger;
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
 * Node - Wrote most of the client/server/viewer, as well as had the idea for the exploit originally.
 * NathanW - Worked on the client (barely), found the good way to listen for loaded chunks.
 * Arzi - Worked on the client
 *
 * Honorable mentions
 * Ianmc05
 * Hobrin
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
        accountsFileOpt.setType(String.class);
        options.addOption(accountsFileOpt);

        Option rulesFileOpt = new Option("rf", "configFile", true, "The path to the config file.");
        rulesFileOpt.setArgName("path");
        rulesFileOpt.setType(String.class);
        options.addOption(rulesFileOpt);

        Option hostOpt = new Option("h", "host", true, "The host IP to connect to.");
        hostOpt.setRequired(true);
        hostOpt.setType(String.class);
        options.addOption(hostOpt);

        Option portOpt = new Option("p", "port", true, "The port to use.");
        portOpt.setType(Integer.class);
        options.addOption(portOpt);

        Option noYCConnectionOpt = new Option("noyc", "no-yc-connection", false,
                "Stops the connection to a remote server, for data reporting,");
        noYCConnectionOpt.setType(Boolean.class);
        options.addOption(noYCConnectionOpt);

        Option ycHandlerName = new Option("n", "handler-name", true, "The name of the YC handler.");
        ycHandlerName.setArgName("name");
        ycHandlerName.setType(String.class);
        options.addOption(ycHandlerName);

        CommandLineParser parser = new DefaultParser();

        tempLogger.info("Welcome to YesCom \ud83d\ude08!!!!");

        try {
            CommandLine cmd = parser.parse(options, args);
            String logLevel = cmd.getOptionValue("logLevel");
            String accountsFile = cmd.getOptionValue("accountsFile");
            String configFile = cmd.getOptionValue("configFile");
            String host = cmd.getOptionValue("host");
            String port = cmd.getOptionValue("port");
            boolean noYCConnection = cmd.hasOption("no-yc-connection");
            String handlerName = cmd.getOptionValue("handler-name");

            try {
                tempLogger.setLogLevel(LogLevel.valueOf(logLevel.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException | NullPointerException ignored) {
            }

            instance = new YesCom(tempLogger.getLogLevel(),
                    accountsFile == null ? "accounts.txt" : accountsFile,
                    configFile == null ? "config.yml" : configFile,
                    host, port == null ? 25565 : Integer.decode(port),
                    noYCConnection, handlerName == null ? "test" : handlerName);
            Runtime.getRuntime().addShutdownHook(new Thread(instance::exit));
            instance.run();

        } catch (ParseException error) {
            tempLogger.fatal("Couldn't parse command line args.");
            tempLogger.error(error.toString());

            System.exit(1);
        }
    }

    public final Logger logger;

    public final AccountHandler accountHandler;
    public final ConfigHandler configHandler;
    public final ConnectionHandler connectionHandler;
    public final InvalidMoveHandler invalidMoveHandler;
    public final QueryHandler queryHandler;
    public final DataHandler dataHandler;
    public final TrackingHandler trackingHandler;
    private final List<ITask> currentTasks = new ArrayList<>();

    private final String handlerName;
    private final boolean noYCConnection;

    public Connection connection;
    public YCHandler handler;

    private int reconnectAttempts;

    private boolean alive;
    private int taskID;

    private long lastTaskUpdate;

    public YesCom(LogLevel logLevel, String accountFilePath, String configFilePath, String host, int port,
                  boolean noYCConnection, String handlerName) {
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
        dataHandler = new DataHandler();
        trackingHandler = new TrackingHandler();

        this.handlerName = handlerName;
        this.noYCConnection = noYCConnection;

        connection = null;
        handler = null;

        reconnectAttempts = 0;

        alive = true;
        taskID = 0;

        lastTaskUpdate = System.currentTimeMillis();
    }

    private void onTick() {
        if (!noYCConnection && (connection == null || (!connection.isConnected() && (!connection.isExited() || ++reconnectAttempts < 5)))) {
            connection = new Connection(configHandler.HOST_NAME, configHandler.HOST_PORT, logger);
            connection.setAuthSuppliers(
                    () -> new User(configHandler.USERNAME, 1, 4,
                            new Group(configHandler.GROUP_NAME, 1, 4)),
                    () -> configHandler.PASSWORD);
            handler = new YCHandler(connection, handlerName);
            connection.addHandler(handler);

            try {
                connection.connect();
            } catch (IOException ignored) {
                // error.printStackTrace();
            }
            return; // Don't tick until we've connected
        } else if (connection != null && connection.isExited()) {
            logger.fatal(String.format("Connection closed due to: %s", connection.getExitReason()));
            exit();
            return;
        } else {
            reconnectAttempts = 0;
        }

        if (connection != null && handler != null && connection.getHandler() instanceof DefaultHandler &&
                !handler.isInitialized()) {
            handler.initConnection();
            return;
        }

        // Only down here so that the handler gets notified about the added task
        if (currentTasks.isEmpty() && (handler == null || handler.isSynced()));

        configHandler.onTick();

        accountHandler.onTick();
        connectionHandler.onTick();

        boolean doTaskUpdate = System.currentTimeMillis() - lastTaskUpdate > 1000;
        if (doTaskUpdate) lastTaskUpdate = System.currentTimeMillis();

        new ArrayList<>(currentTasks).forEach(task -> {
            if (task.isFinished()) {
                logger.debug(String.format("Finished task: %s.", task));
                removeTask(task);
            } else {
                task.onTick();
                if (doTaskUpdate && handler != null) handler.onTaskUpdate(task);
            }
        });

        if (handler != null) {
            if (connectionHandler.isConnected()) {
                handler.onInfoUpdate(queryHandler.getWaitingSize(), queryHandler.getTickingSize(), queryHandler.getQueriesPerSecond(),
                        connectionHandler.getMeanTickRate(), connectionHandler.getTimeSinceLastPacket());
            } else {
                handler.onInfoUpdate(queryHandler.getWaitingSize(), queryHandler.getTickingSize(), queryHandler.getQueriesPerSecond());
            }
        }

        invalidMoveHandler.onTick();
        queryHandler.onTick();

        trackingHandler.onTick();

        dataHandler.onTick();

        if (handler != null) handler.onTick();
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

            dataHandler.onExit();
            configHandler.onExit();
            logger.info("Done!");
        }
    }

    public List<ITask> getTasks() {
        return new ArrayList<>(currentTasks);
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
            if (handler != null) handler.onTaskRemoved(task);
        }
    }

    public void removeTask(int taskID) {
        currentTasks.stream().filter(task -> task.getID() == taskID).findFirst().ifPresent(task -> {
            currentTasks.remove(task);
            task.onFinished();
            if (handler != null) handler.onTaskRemoved(task);
        });
    }
}
