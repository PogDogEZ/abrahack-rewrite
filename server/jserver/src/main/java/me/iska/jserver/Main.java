package me.iska.jserver;

import me.iska.jserver.network.Server;
import me.iska.jserver.util.Colour;

import java.io.Console;
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
        JServer jServer = new JServer("TestServer", 6);
        Runtime.getRuntime().addShutdownHook(new Thread(jServer::exit));

        Server server;
        try {
            server = new Server(new ServerSocket());
            jServer.connectionManager.addServer(server);
            server.bind("localhost", 5001);
        } catch (IOException error) {
            logger.warning("Couldn't add default server:");
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
