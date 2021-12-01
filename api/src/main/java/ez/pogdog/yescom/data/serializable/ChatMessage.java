package ez.pogdog.yescom.data.serializable;

import ez.pogdog.yescom.data.ISerializable;

import java.math.BigInteger;

/**
 * For handling chat data, the username is the username of the account that received the chat message.
 */
public class ChatMessage implements ISerializable {

    private final BigInteger chatMessageID;
    private final String username;
    private final String message;
    private final long timestamp;

    public ChatMessage(BigInteger chatMessageID, String username, String message, long timestamp) {
        this.chatMessageID = chatMessageID;
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
    }

    @Override
    public BigInteger getID() {
        return chatMessageID;
    }

    /**
     * @return The username of the account that received the message.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return The message that was sent.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return The time at which the message was received.
     */
    public long getTimestamp() {
        return timestamp;
    }
}
