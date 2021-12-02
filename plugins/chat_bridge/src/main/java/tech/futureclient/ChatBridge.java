package tech.futureclient;


import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.Player;
import tech.futureclient.message.impl.FromDiscord;
import discord4j.rest.RestClientBuilder;
import tech.futureclient.plugin.Hook;

import java.util.LinkedList;

public class ChatBridge {

    public ChatBridge()
    {
        registerBot();
        messageThread.start();
    }

    public static GatewayDiscordClient client;
    public static MessageChannel channel;

    private static MessageThread messageThread = new MessageThread();

    public static LinkedList<FromDiscord> toSend = new LinkedList<>();

    public static void registerBot() {
        client = DiscordClientBuilder.create(TEMP_CONSTANTS.Token)
                .build()
                .login()
                .block();

        channel = (MessageChannel) client
                .getChannelById(Snowflake.of(TEMP_CONSTANTS.Channel))
                .block();

        client.on(MessageCreateEvent.class).subscribe(event -> {
            final Message message = event.getMessage();

            if (message.getAuthor().map(user -> !user.isBot()).orElse(false)) {
                if (!message.getChannelId().equals(Snowflake.of(TEMP_CONSTANTS.Channel))) return;
                FromDiscord msg = new FromDiscord(message.getContent(), message.getAuthor().get().getUsername());
                if(msg.isCommand() && !msg.isAllowedCommand()) return;
                toSend.add(msg);
            }
        });
    }


    public static class MessageThread extends Thread {
        @Override
        public void run() {
            long time = System.currentTimeMillis();

            while (true) {
                System.out.checkError();
                if (!toSend.isEmpty()) {
                    if (toSend.size() > 5) toSend.clear();
                    if (System.currentTimeMillis() - time > TEMP_CONSTANTS.Cooldown * 1000L) {
                        time = System.currentTimeMillis();
                        if (toSend.getFirst().getMessage() == null) return;
                        Hook.activeReporter().sendChatMessage(-1, Hook.activePlayer.getDisplayName(), toSend.removeFirst().getMessage());
                    }
                }
            }
        }
    }
}

