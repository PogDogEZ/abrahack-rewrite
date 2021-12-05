package me.iska.jserver;

import me.iska.jserver.network.Server;
import me.iska.jserver.util.Colour;
import org.apache.commons.cli.*;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

public class Main {

    public static final Logger logger = Logger.getLogger("jserver");

    private static final Map<Level, String> LEVEL_COLOURS = new HashMap<>();

    static { // Setting up the logger
        LEVEL_COLOURS.put(Level.SEVERE, Colour.Foreground.RED);
        LEVEL_COLOURS.put(Level.WARNING, Colour.Foreground.YELLOW);
        LEVEL_COLOURS.put(Level.INFO, Colour.RESET);
        LEVEL_COLOURS.put(Level.CONFIG, Colour.RESET);
        LEVEL_COLOURS.put(Level.FINE, Colour.Foreground.BLUE);
        LEVEL_COLOURS.put(Level.FINER, Colour.Foreground.BLUE);
        LEVEL_COLOURS.put(Level.FINEST, Colour.Foreground.BLUE);

        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);
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

        /*
        try {
            FileHandler fileHandler = new FileHandler("log.txt");
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);
        } catch (IOException ignored) {
        }
         */
    }

    @SuppressWarnings({ "BusyWait", "InfiniteLoopStatement" })
    public static void main(String[] args) {
        logger.info("Starting JServer...");

        Options options = new Options();

        Option serverNameOption = new Option("n", "name", true, "The name of the server.");
        serverNameOption.setType(String.class);
        serverNameOption.setRequired(true);
        options.addOption(serverNameOption);

        Option protocolVersionOption = new Option("v", "protocolVersion", true, "The protocol version to use.");
        protocolVersionOption.setType(Integer.class);
        options.addOption(protocolVersionOption);

        Option workingDirectoryOption = new Option("wd", "workingDirectory", true, "The working directory.");
        workingDirectoryOption.setType(String.class);
        options.addOption(workingDirectoryOption);

        Option hostOption = new Option("h", "host", true, "The host to bind to.");
        hostOption.setType(String.class);
        options.addOption(hostOption);

        Option portOption = new Option("p", "port", true, "The port to bind to.");
        portOption.setType(Integer.class);
        options.addOption(portOption);

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            String serverName = cmd.getOptionValue("name");
            String protocolVersion = cmd.getOptionValue("protocolVersion");
            String workingDirectory = cmd.getOptionValue("workingDirectory");
            String host = cmd.getOptionValue("host");
            String port = cmd.getOptionValue("port");

            JServer jServer = new JServer(serverName,
                    protocolVersion == null ? 6 : Integer.parseInt(protocolVersion),
                    workingDirectory == null ? new File(".") : new File(workingDirectory),
                    host == null ? "localhost" : host,
                    port == null ? 5001 : Integer.parseInt(port));
            Runtime.getRuntime().addShutdownHook(new Thread(jServer::exit));

            Server server  = new Server(new ServerSocket());
            jServer.connectionManager.addServer(server);
            server.bind(host == null ? "localhost" : host, port == null ? 5001 : Integer.parseInt(port));

        } catch (Exception error) {
            logger.severe("Couldn't parse command line args.");
            logger.throwing(Main.class.getSimpleName(), "main", error);

            System.exit(1);
        }

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
