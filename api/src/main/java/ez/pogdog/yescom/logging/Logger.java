package ez.pogdog.yescom.logging;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Logger {

    public final List<Message> messages;

    private final String name;
    private LogLevel logLevel;

    private boolean showColour;
    private boolean showTime;
    private boolean showName;
    private boolean showClass;

    public Logger(String name, LogLevel logLevel, boolean showColour, boolean showTime, boolean showName, boolean showClass) {
        this.name = name;
        this.logLevel = logLevel;

        this.showColour = showColour;
        this.showTime = showTime;
        this.showName = showName;
        this.showClass = showClass;

        messages = new ArrayList<>();
    }

    public Logger(String name, LogLevel logLevel) {
        this(name, logLevel, true, true, true, true);
    }

    public Logger(String name) {
        this(name, LogLevel.DEBUG, true, true, true, true);
    }

    @Override
    public String toString() {
        return String.format("Logger(name=%s)", name);
    }

    public void log(LogLevel level, boolean newLine, Object... message) {
        StringBuilder stringBuilder = new StringBuilder();

        // Find the name of the first known class that isn't this one
        String foundClassName = Thread.currentThread().getName();
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (!element.getClassName().equals(this.getClass().getName()) &&
                    element.getClassName().matches("((me\\.node3112)|(me\\.iska)|(ez\\.pogdog)).+")) {
                String[] classPath = element.getClassName().split("\\.");
                foundClassName = classPath[classPath.length - 1];
                break;
            }
        }

        if (showTime) stringBuilder.append(String.format("[%s] ", LocalTime.now()));
        if (showName) stringBuilder.append(String.format("[%s] ", name));
        if (showClass) {
            stringBuilder.append(String.format("[%s/%s] ", foundClassName, level.toString()));
        } else {
            stringBuilder.append(String.format("[%s] ", level.toString()));
        }

        // stringBuilder.append(String.format("[%s] [%s] [%s/%s] ", LocalTime.now(), name, foundClassName, level.toString()));

        for (Object messagePart : message) stringBuilder.append(messagePart).append(" ");
        stringBuilder.append(newLine ? "\n" : "\r");

        // messages.add(new Message(level, foundClassName, stringBuilder.toString()));

        if (level.ordinal() >= logLevel.ordinal()) {
            if (showColour) System.out.print("\u001b[2K" + level.getAnsiColour());
            System.out.print(stringBuilder.toString());
            if (showColour) System.out.print("\u001b[0m");
        }
    }

    public void debug(Object... message) {
        log(LogLevel.DEBUG, true, message);
    }

    public void debugDisappearing(Object... message) {
        log(LogLevel.DEBUG, true, message);
    }

    public void info(Object... message) {
        log(LogLevel.INFO, true, message);
    }

    public void infoDisappearing(Object... message) {
        log(LogLevel.INFO, false, message);
    }

    public void warn(Object... message) {
        log(LogLevel.WARNING, true, message);
    }

    public void warnDisappearing(Object... message) {
        log(LogLevel.WARNING, false, message);
    }

    public void error(Object... message) {
        log(LogLevel.ERROR, true, message);
    }

    public void errorDisappearing(Object... message) {
        log(LogLevel.ERROR, false, message);
    }

    public void fatal(Object... message) {
        log(LogLevel.FATAL, true, message);
    }

    public void fatalDisappearing(Object... message) {
        log(LogLevel.FATAL, false, message);
    }

    public String getName() {
        return name;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public boolean showsColour() {
        return showColour;
    }

    public void setShowColour(boolean showColour) {
        this.showColour = showColour;
    }

    public boolean showsTime() {
        return showTime;
    }

    public void setShowTime(boolean showTime) {
        this.showTime = showTime;
    }

    public boolean showsName() {
        return showName;
    }

    public void setShowName(boolean showName) {
        this.showName = showName;
    }

    public void setShowClass(boolean showClass) {
        this.showClass = showClass;
    }

    public boolean showsClass() {
        return showClass;
    }
}
