package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.data.serializable.ChatMessage;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

public class ChatMessageType extends Type<ChatMessage> {

    @Override
    public ChatMessage read(InputStream inputStream) throws IOException {
        BigInteger chatMessageID = Registry.VAR_INTEGER.read(inputStream);
        String username = Registry.STRING.read(inputStream);
        String message = Registry.STRING.read(inputStream);
        long timestamp = Registry.LONG.read(inputStream);

        return new ChatMessage(chatMessageID, username, message, timestamp);
    }

    @Override
    public void write(ChatMessage value, OutputStream outputStream) throws IOException {
        Registry.VAR_INTEGER.write(value.getID(), outputStream);
        Registry.STRING.write(value.getUsername(), outputStream);
        Registry.STRING.write(value.getMessage(), outputStream);
        Registry.LONG.write(value.getTimestamp(), outputStream);
    }
}
