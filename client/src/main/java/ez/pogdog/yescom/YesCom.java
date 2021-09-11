package ez.pogdog.yescom;

import ez.pogdog.yescom.handlers.AccountHandler;
import ez.pogdog.yescom.handlers.ConfigHandler;
import ez.pogdog.yescom.handlers.QueryHandler;
import ez.pogdog.yescom.handlers.SaveHandler;
import ez.pogdog.yescom.handlers.connection.ConnectionHandler;
import ez.pogdog.yescom.handlers.invalidmove.InvalidMoveHandler;
import ez.pogdog.yescom.handlers.tracking.TrackedPlayerHandler;
import ez.pogdog.yescom.handlers.tracking.TrackingHandler;
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
import me.iska.jclient.network.Connection;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class YesCom {

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

    public final List<ITask> currentTasks = new ArrayList<>();

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

    private boolean alive;

    public YesCom(LogLevel logLevel, String accountFilePath, String configFilePath, String host, int port) {
        instance = this;

        logger = new Logger("yescom", logLevel);
        logger.setShowName(false);

        logger.info("Initializing...");

        connection = null;
        handler = null;

        configHandler = new ConfigHandler(configFilePath);
        accountHandler = new AccountHandler(accountFilePath);
        connectionHandler = new ConnectionHandler(host, port);
        invalidMoveHandler = new InvalidMoveHandler();
        queryHandler = new QueryHandler();
        saveHandler = new SaveHandler();
        trackedPlayerHandler = new TrackedPlayerHandler();
        trackingHandler = new TrackingHandler();

        alive = true;

        //currentTasks.add(new StaticScanTask(Dimension.NETHER, null));
        currentTasks.add(new SpiralScanTask(new ChunkPosition(0, 0), 12, Dimension.OVERWORLD, IQuery.Priority.LOW));

        logger.info("Done.");
    }

    private void onTick() {
        configHandler.onTick();

        accountHandler.onTick();
        connectionHandler.onTick();

        new ArrayList<>(currentTasks).forEach(task -> {
            if (task.isFinished()) {
                logger.debug(String.format("Finished task: %s.", task));
                currentTasks.remove(task);
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
        logger.info("Exiting...");
        alive = false;

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
