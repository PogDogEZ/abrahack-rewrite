package ez.pogdog.yescom;

import ez.pogdog.yescom.managers.HandlersManager;
import ez.pogdog.yescom.managers.TrustedManager;
import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.network.handlers.YCHandler;
import me.iska.jserver.JServer;
import me.iska.jserver.event.Listener;
import me.iska.jserver.event.events.connection.CapabilitiesEvent;
import me.iska.jserver.exception.PluginException;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.packets.ClientCapabilitiesPacket;
import me.iska.jserver.plugin.IPlugin;

import java.util.*;
import java.util.logging.Logger;

/**
 * YesCom server plugin.
 */
@IPlugin.Info(name="YesCom", version="0.9", dependencies={})
public class YesCom implements IPlugin {

    private static YesCom instance;

    public static YesCom getInstance() {
        return instance;
    }

    private final JServer jServer = JServer.getInstance();
    private final Logger logger = JServer.getLogger();

    private final Map<UUID, String> UUIDToNameCache = new HashMap<>();
    private final Map<String, UUID> nameToUUIDCache = new HashMap<>();

    public final HandlersManager handlersManager = new HandlersManager();
    public final TrustedManager trustedManager = new TrustedManager();

    public YesCom() {
        instance = this;
    }

    @Override
    public void load() throws PluginException {
        YCRegistry.registerPackets();
        jServer.eventBus.register(this);
    }

    @Override
    public void unload() throws PluginException {
        jServer.eventBus.unregister(this);
    }

    @Listener
    public void onCapabilities(CapabilitiesEvent event) {
        logger.finer(String.format("Checking capabilities for %s.", event.getConnection()));

        boolean canListen = true;
        boolean canReport = true;

        for (Class<? extends Packet> packetClazz : YCRegistry.SHARED_PACKETS) {
            if (!event.getClientPackets().contains(new ClientCapabilitiesPacket.PacketRepresentation(packetClazz))) {
                canListen = false;
                canReport = false;
            }
        }

        for (Class<? extends Packet> packetClazz : YCRegistry.LISTENER_PACKETS) {
            if (!event.getClientPackets().contains(new ClientCapabilitiesPacket.PacketRepresentation(packetClazz)))
                canListen = false;
        }

        for (Class<? extends Packet> packetClazz : YCRegistry.REPORTER_PACKETS) {
            if (!event.getClientPackets().contains(new ClientCapabilitiesPacket.PacketRepresentation(packetClazz)))
                canReport = false;
        }

        if (!canListen && !canReport) {
            logger.finer(String.format("%s cannot listen or report.", event.getConnection()));
        } else {
            logger.finer(String.format("%s can listen or report.", event.getConnection()));
            event.getConnection().addSecondaryHandler(new YCHandler(event.getConnection(), -1));
            event.setCancelled(true);
            event.setRejected(false);
        }
    }

    public void putUUIDToName(UUID uuid, String name) {
        UUIDToNameCache.put(uuid, name);
        nameToUUIDCache.put(name, uuid);
    }

    /**
     * Get the name of the player with the specified UUID.
     * @param uuid The UUID of the player.
     * @return The name of the player, null if not found.
     */
    public String getName(UUID uuid) {
        return UUIDToNameCache.get(uuid);
    }

    /**
     * Get the UUID of the player with the specified name.
     * @param name The name of the player.
     * @return The UUID of the player, null if not found.
     */
    public UUID getUUID(String name) {
        return nameToUUIDCache.get(name);
    }
}
