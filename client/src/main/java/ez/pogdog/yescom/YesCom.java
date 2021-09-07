package ez.pogdog.yescom;

import ez.pogdog.yescom.handlers.AccountHandler;
import ez.pogdog.yescom.handlers.ConfigHandler;
import ez.pogdog.yescom.handlers.QueryHandler;
import ez.pogdog.yescom.handlers.SaveHandler;
import ez.pogdog.yescom.handlers.connection.ConnectionHandler;
import ez.pogdog.yescom.handlers.invalidmove.InvalidMoveHandler;
import ez.pogdog.yescom.handlers.task.BasicScanTask;
import ez.pogdog.yescom.handlers.task.ITask;
import ez.pogdog.yescom.logging.LogLevel;
import ez.pogdog.yescom.logging.Logger;
import ez.pogdog.yescom.util.ChunkPosition;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    public final List<ITask> currentTasks = new ArrayList<>();

    public final Logger logger;

    public final AccountHandler accountHandler;
    public final ConfigHandler configHandler;
    public final ConnectionHandler connectionHandler;
    public final InvalidMoveHandler invalidMoveHandler;
    public final QueryHandler queryHandler;
    public final SaveHandler saveHandler;

    private boolean alive;

    public YesCom(LogLevel logLevel, String accountFilePath, String configFilePath, String host, int port) {
        instance = this;

        logger = new Logger("yescom", logLevel);
        logger.setShowName(false);

        logger.info("Initializing...");

        configHandler = new ConfigHandler(configFilePath);
        accountHandler = new AccountHandler(accountFilePath);
        connectionHandler = new ConnectionHandler(host, port);
        invalidMoveHandler = new InvalidMoveHandler();
        queryHandler = new QueryHandler();
        saveHandler = new SaveHandler();

        alive = true;

        currentTasks.add(new BasicScanTask(new ChunkPosition(-150, -150), new ChunkPosition(150, 150), 12, -1));

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

        invalidMoveHandler.onTick();
        queryHandler.onTick();

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

        saveHandler.onExit();
        configHandler.onExit();
        logger.info("Done!");
    }
}
