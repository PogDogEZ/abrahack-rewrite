package ez.pogdog.yescom.logging;

import java.util.Objects;

public class Message {

    private final LogLevel level;
    private final String className;
    private final String text;

    public Message(LogLevel level, String className, String text) {
        this.level = level;
        this.className = className;
        this.text = text;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Message message = (Message)other;
        return text.equals(message.text) && level == message.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, level);
    }

    @Override
    public String toString() {
        return text;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getClassName() {
        return className;
    }

    public String getText() {
        return text;
    }
}
