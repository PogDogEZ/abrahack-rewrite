package ez.pogdog.yescom;

import ez.pogdog.yescom.handlers.*;
import ez.pogdog.yescom.handlers.connection.ConnectionHandler;
import ez.pogdog.yescom.handlers.invalidmove.InvalidMoveHandler;
import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.jclient.handlers.YCDataHandler;
import ez.pogdog.yescom.jclient.handlers.YCHandler;
import ez.pogdog.yescom.task.ITask;
import me.iska.jclient.network.Connection;
import me.iska.jclient.network.handler.handlers.DefaultHandler;
import me.iska.jclient.util.Colour;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

/**
 * PogDog Software Suite Presents:
 *
 * Yescom© - Sequel to Nocom; a project carried out by NerdsInc
 * Nocom information found here: https://github.com/nerdsinspace/nocom-explanation/blob/main/README.md
 *
 * More info on this program: https://github.com/PogDogEZ/abrahack-rewrite/blob/main/README.md
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

    private static final Map<Level, String> LEVEL_COLOURS = new HashMap<>();

    public static YesCom getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        // TODO: This colour stuff could prolly go in clinit
        LEVEL_COLOURS.put(Level.SEVERE, Colour.Foreground.RED);
        LEVEL_COLOURS.put(Level.WARNING, Colour.Foreground.YELLOW);
        LEVEL_COLOURS.put(Level.INFO, Colour.RESET);
        LEVEL_COLOURS.put(Level.CONFIG, Colour.RESET);
        LEVEL_COLOURS.put(Level.FINE, Colour.Foreground.BLUE);
        LEVEL_COLOURS.put(Level.FINER, Colour.Foreground.BLUE);
        LEVEL_COLOURS.put(Level.FINEST, Colour.Foreground.BLUE);

        // Set up the loggers
        Logger tempLogger = Logger.getLogger("yescom");

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new Formatter() {
            @Override
            @SuppressWarnings("StringConcatenationInLoop")
            public String format(LogRecord record) {
                String message = String.format("%s[%tT] [%s] %s %s%n", LEVEL_COLOURS.get(record.getLevel()),
                        record.getMillis(), record.getLevel(), record.getMessage(), Colour.RESET);

                if (record.getThrown() != null) {
                    message += String.format("%s%s %n", Colour.Foreground.RED, record.getThrown());
                    for (StackTraceElement element : record.getThrown().getStackTrace())
                        message += String.format("\tat %s.%s(%s:%d) %n", element.getClassName(), element.getMethodName(),
                                element.getFileName(), element.getLineNumber());
                    message += Colour.RESET;
                }

                return message;
            }
        });

        tempLogger.setLevel(Level.ALL);
        tempLogger.setUseParentHandlers(false);
        tempLogger.addHandler(consoleHandler);

        Logger.getLogger("jclient").setLevel(Level.ALL);
        Logger.getLogger("jclient").setUseParentHandlers(false);
        Logger.getLogger("jclient").addHandler(consoleHandler);

        Options options = new Options();

        Option logLevelOpt = new Option("l", "logLevel", true, "The log level.");
        logLevelOpt.setType(Level.class);
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

        Option noYCConnectionOpt = new Option("noyc", "noYCConnection", false,
                "Stops the connection to a remote server, for data reporting,");
        noYCConnectionOpt.setType(Boolean.class);
        options.addOption(noYCConnectionOpt);

        Option ycHandlerName = new Option("n", "handlerName", true, "The name of the YC handler.");
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
            boolean noYCConnection = cmd.hasOption("noYCConnection");
            String handlerName = cmd.getOptionValue("handlerName");

            try {
                Level level = Level.parse(logLevel.toUpperCase(Locale.ROOT));

                tempLogger.setLevel(level);
                consoleHandler.setLevel(level);
                Logger.getLogger("jclient").setLevel(level);

            } catch (IllegalArgumentException | NullPointerException ignored) {
            }

            instance = new YesCom(accountsFile == null ? "accounts.txt" : accountsFile,
                    configFile == null ? "config.yml" : configFile,
                    host, port == null ? 25565 : Integer.decode(port),
                    noYCConnection, handlerName == null ? "test" : handlerName);
            Runtime.getRuntime().addShutdownHook(new Thread(instance::exit));
            instance.run();

        } catch (Exception error) {
            tempLogger.severe("Couldn't parse command line args.");
            tempLogger.throwing(YesCom.class.getSimpleName(), "main", error);

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
    public YCHandler ycHandler;
    public YCDataHandler ycDataHandler;

    private int reconnectAttempts;

    private boolean alive;
    private int taskID;

    private long lastTaskUpdate;

    public YesCom(String accountFilePath, String configFilePath, String host, int port, boolean noYCConnection,
                  String handlerName) {
        instance = this;

        logger = Logger.getLogger("yescom");
        logger.info("Initializing...");

        logger.fine("Registering packets...");
        YCRegistry.registerPackets();
        logger.fine("Done.");

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
        ycHandler = null;

        reconnectAttempts = 0;

        alive = true;
        taskID = 0;

        lastTaskUpdate = System.currentTimeMillis();

        if (!noYCConnection) restartConnection();
    }

    /* ----------------------------- Private methods ----------------------------- */

    private synchronized void onTick() {
        if (!noYCConnection && (!connection.isConnected() && (!connection.isExited() || ++reconnectAttempts < 5))) {
            try {
                connection.connect(new Socket());
            } catch (IOException ignored) {
                // error.printStackTrace();
            }

            // Edit: WHAT?? Of course tick if we're not connected we don't want to lose the trackers due to internet issues
            // return; // Don't tick until we've connected

        } else if (connection != null && connection.isExited()) {
            logger.severe(String.format("Connection closed due to: %s", connection.getExitReason()));
            exit();
            return;

        } else {
            reconnectAttempts = 0;
        }

        if (connection != null && ycHandler != null && connection.getPrimaryHandler() instanceof DefaultHandler &&
                !ycHandler.isInitialized()) {
            ycHandler.initConnection();
            return;
        }

        // Only down here so that the handler gets notified about the added task
        if (currentTasks.isEmpty() && (ycHandler == null || ycHandler.isSynced()));

        configHandler.onTick();

        accountHandler.onTick();
        connectionHandler.onTick();

        boolean doTaskUpdate = System.currentTimeMillis() - lastTaskUpdate > 1000;
        if (doTaskUpdate) lastTaskUpdate = System.currentTimeMillis();

        new ArrayList<>(currentTasks).forEach(task -> {
            if (task.isFinished()) {
                logger.fine(String.format("Finished task: %s.", task));
                removeTask(task);
            } else {
                task.onTick();
                if (doTaskUpdate && ycHandler != null) ycHandler.onTaskUpdate(task);
            }
        });

        if (ycHandler != null) {
            if (connectionHandler.isConnected()) {
                ycHandler.onInfoUpdate(queryHandler.getWaitingSize(), queryHandler.getTickingSize(), queryHandler.getQueriesPerSecond(),
                        connectionHandler.getMeanTickRate(), connectionHandler.getMeanServerPing(), connectionHandler.getTimeSinceLastPacket());
            } else {
                ycHandler.onInfoUpdate(queryHandler.getWaitingSize(), queryHandler.getTickingSize(), queryHandler.getQueriesPerSecond());
            }
        }

        invalidMoveHandler.onTick();
        queryHandler.onTick();

        trackingHandler.onTick();

        dataHandler.onTick();

        if (ycHandler != null) ycHandler.onTick();
    }

    /* ----------------------------- Management stuff ----------------------------- */

    @SuppressWarnings("BusyWait")
    public void run() {
        while (alive) {
            long startTime = System.currentTimeMillis();
            onTick();
            long finishTime = System.currentTimeMillis();

            if (finishTime - startTime < 50) { // 50ms is the Minecraft tick time
                try {
                    Thread.sleep(50 - (finishTime - startTime));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * Called on exit.
     */
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

    /**
     * Restarts the connection.
     */
    public void restartConnection() {
        connection = new Connection(configHandler.HOST_NAME, configHandler.HOST_PORT);
        connection.setAccountSupplier(() -> new Connection.Account(configHandler.USERNAME, configHandler.PASSWORD,
                configHandler.GROUP_NAME));
        ycHandler = new YCHandler(connection, handlerName);
        ycDataHandler = new YCDataHandler(connection);
        connection.addSecondaryHandler(ycHandler);
        connection.addSecondaryHandler(ycDataHandler);
    }

    /* ----------------------------- Tasks ----------------------------- */

    /**
     * @return The currently active tasks.
     */
    public synchronized List<ITask> getTasks() {
        return new ArrayList<>(currentTasks);
    }

    public synchronized ITask getTask(int taskID) {
        return currentTasks.stream().filter(task -> task.getID() == taskID).findFirst().orElse(null);
    }

    /**
     * Adds a task.
     * @param task The task to add.
     */
    public synchronized void addTask(ITask task) {
        if (!currentTasks.contains(task)) {
            task.setID(taskID);
            if (++taskID >= Short.MAX_VALUE * 2) taskID = 0;

            currentTasks.add(task);
            if (ycHandler != null) ycHandler.onTaskAdded(task);
        }
    }

    /**
     * Removes a task.
     * @param task The task to remove.
     */
    public synchronized void removeTask(ITask task) {
        if (currentTasks.remove(task)) {
            task.onFinished();
            if (ycHandler != null) ycHandler.onTaskRemoved(task);
        }
    }

    /**
     * Removes a task given the ID.
     * @param taskID The task ID.
     */
    public void removeTask(int taskID) {
        currentTasks.stream().filter(task -> task.getID() == taskID).findFirst().ifPresent(task -> {
            currentTasks.remove(task);
            task.onFinished();
            if (ycHandler != null) ycHandler.onTaskRemoved(task);
        });
    }
}
