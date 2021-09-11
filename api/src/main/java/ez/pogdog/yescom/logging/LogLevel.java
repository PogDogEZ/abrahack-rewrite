package ez.pogdog.yescom.logging;

public enum LogLevel {
    DEBUG("\u001b[34m"),
    INFO(""),
    WARNING("\u001b[91m"),
    ERROR("\u001b[31m"),
    FATAL("\u001b[41m\u001b[30m");

    private final String ansiColour;

    LogLevel(String ansiColour) {
        this.ansiColour = ansiColour;
    }

    public String getAnsiColour() {
        return ansiColour;
    }
}
