package tech.futureclient.message;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import discord4j.rest.util.Color;
import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.network.handlers.YCHandler;
import ez.pogdog.yescom.network.handlers.YCReporter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import tech.futureclient.ChatBridge;
import tech.futureclient.message.impl.FromMinecraft;

import java.net.URL;
import java.util.UUID;

public class MessageHandler
{
    private static final YesCom yesCom = YesCom.getInstance();

    public static void sendToDiscord(FromMinecraft message) {
        try {
            MessageType type = message.getType();
            ChatBridge.channel.createEmbed(spec -> {
                switch (type) {
                    case NORMAL:
                    {
                        spec.setColor(Color.WHITE);
                        if (message.getSenderName() != null) {
                            UUID uuid = UUID.fromString(getUuid(message.getSenderName()));
                            spec.setAuthor(message.getSenderName(), "https://namemc.com/profile/" + uuid,
                                    "https://mc-heads.net/avatar/" + uuid + "/500.png");
                        }
                        break;
                    }
                    case DEATH:
                    {
                        spec.setColor(Color.RED);
                        break;
                    }

                    case LOG:
                    {
                        spec.setColor(Color.GRAY_CHATEAU);
                        break;
                    }

                    case WHISPER:
                    {
                        if (message.getSenderName() != null) {
                            UUID uuid = UUID.fromString(getUuid(message.getSenderName()));
                            spec.setAuthor(message.getSenderName(), "https://namemc.com/profile/" + uuid,
                                    "https://mc-heads.net/avatar/" + uuid + "/500.png");
                        }
                        spec.setColor(Color.DEEP_LILAC);
                        break;
                    }

                    case SERVER:
                    {
                        spec.setColor(Color.YELLOW);
                        break;
                    }

                    case GREEN_TEXT:
                    {
                        if (message.getSenderName() != null) {
                            UUID uuid = UUID.fromString(getUuid(message.getSenderName()));
                            spec.setAuthor(message.getSenderName(), "https://namemc.com/profile/" + uuid,
                                    "https://mc-heads.net/avatar/" + uuid + "/500.png");
                        }
                        spec.setColor(Color.SEA_GREEN);
                        break;
                    }
                }
                spec.setDescription(message.getMessage());
            }).block();
        }catch (Exception ignored) {}
    }

    public static String getUuid(String name) {
        JsonParser parser = new JsonParser();
        String url = "https://api.mojang.com/users/profiles/minecraft/" + name;
        try {
            String UUIDJson = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
            if(UUIDJson.isEmpty()) return "invalid name";
            JsonObject UUIDObject = (JsonObject) parser.parse(UUIDJson);
            return reformatUuid(UUIDObject.get("id").toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "error";
    }

    private static String reformatUuid(String uuid) {
        String longUuid = "";

        longUuid += uuid.substring(1, 9) + "-";
        longUuid += uuid.substring(9, 13) + "-";
        longUuid += uuid.substring(13, 17) + "-";
        longUuid += uuid.substring(17, 21) + "-";
        longUuid += uuid.substring(21, 33);

        return longUuid;
    }
}
