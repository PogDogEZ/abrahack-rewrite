package tech.futureclient.plugin;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.ChatMessage;
import ez.pogdog.yescom.events.data.DataBroadcastEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.network.packets.shared.DataExchangePacket;

import ez.pogdog.yescom.util.Player;
import me.iska.jserver.JServer;
import me.iska.jserver.event.Listener;
import me.iska.jserver.exception.PluginException;
import me.iska.jserver.plugin.IPlugin;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.JSONValue;
import tech.futureclient.ChatBridge;
import tech.futureclient.message.MessageHandler;
import tech.futureclient.message.MessageType;
import tech.futureclient.message.impl.FromMinecraft;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Objects;
import java.util.UUID;

/**
 *  Chat Bridge - © 2021 Future Development. All rights reserved.
 *
 *  This is just a shitty paste the bridge I made like 5 months ago.
 *
 * @author https://github.com/NathanW-05
 * @since December 1st, 2021
 */

@IPlugin.Info
(
        name="chat_bridge",
        version="1.0",
        dependencies={ "yescom" }
)
public class Hook implements IPlugin
{

    public static Player activePlayer;

    public static YCReporter activeReporter()
    {
        for(YCReporter reporter : YesCom.getInstance().handlersManager.getReporters())
        {
            if (reporter.getMinecraftHost().equalsIgnoreCase("constantiam.net"))
            {
                return reporter;
            }
        }
        return null;
    }

    @Override
    public void load() throws PluginException
    {
        JServer.getInstance().eventBus.register(this);

        System.out.println("Fuck You");
        ChatBridge bot = new ChatBridge();
    }

    @Override
    public void unload() throws PluginException
    {
        JServer.getInstance().eventBus.unregister(this);
    }

    @Listener
    public void onDataBroadcast(DataBroadcastEvent event)
    {
       if(YesCom.getInstance() == null) return;

        if (event.getDataType() == DataExchangePacket.DataType.CHAT)
        {

            for (Object data : event.getData()) {
                ChatMessage chatMessage = (ChatMessage)data;

                FromMinecraft mssg = new FromMinecraft(chatMessage.getMessage());

                if(activeReporter() == null) return;

                if (mssg.getType() == MessageType.WHISPER) {
                    for (Player pl : activeReporter().getPlayers()) {
                        if (pl.getDisplayName().equals(chatMessage.getUsername())) {
                            activePlayer = pl;
                            MessageHandler.sendToDiscord(mssg);
                        }
                    }
                } else {
                    if (activePlayer == null) {
                        if (activeReporter().getPlayers().get(0) != null) {
                            activePlayer = activeReporter().getPlayers().get(0);
                        }
                    }
                    if (chatMessage.getUsername().equals(activePlayer.getDisplayName())) {
                        MessageHandler.sendToDiscord(mssg);
                    }
                }
            }
        }
    }
}
