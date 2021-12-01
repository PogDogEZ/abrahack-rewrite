package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.ConfigHandler;
import ez.pogdog.yescom.jclient.YCRegistry;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Sent by the client to synchronise all the config rules.
 */
@Packet.Info(name="config_sync", id=YCRegistry.ID_OFFSET + 12, side=Packet.Side.CLIENT)
public class ConfigSyncPacket extends Packet {

    private final Map<ConfigHandler.ConfigRule, Object> rules = new HashMap<>();

    public ConfigSyncPacket(Map<ConfigHandler.ConfigRule, ?> rules) {
        this.rules.putAll(rules);
    }

    public ConfigSyncPacket() {
        this(new HashMap<>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(InputStream inputStream) throws IOException {
        rules.clear();

        int rulesToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < rulesToRead; ++index) {
            ConfigHandler.ConfigRule configRule = YCRegistry.CONFIG_RULE.read(inputStream);
            Type<Object> type;
            try {
                type = (Type<Object>)Registry.KNOWN_TYPES.get(configRule.getDataType().getClazz()).newInstance();
            } catch (InstantiationException | IllegalAccessException error) {
                YesCom.getInstance().logger.warning("Couldn't deserialize config rule:");
                YesCom.getInstance().logger.throwing(ConfigSyncPacket.class.getName(), "read", error);
                continue;
            }
            Object value = type.read(inputStream);

            rules.put(configRule, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(rules.size(), outputStream);
        for (Map.Entry<ConfigHandler.ConfigRule, Object> rule : rules.entrySet()) {
            Type<Object> type;
            try {
                type = (Type<Object>)Registry.KNOWN_TYPES.get(rule.getKey().getDataType().getClazz()).newInstance();
            } catch (InstantiationException | IllegalAccessException error) {
                YesCom.getInstance().logger.warning("Couldn't serialize config rule:");
                YesCom.getInstance().logger.throwing(ConfigSyncPacket.class.getName(), "write", error);
                continue;
            }

            YCRegistry.CONFIG_RULE.write(rule.getKey(), outputStream);
            type.write(rule.getValue(), outputStream);
        }
    }

    /**
     * @return The rules and their values.
     */
    public Map<ConfigHandler.ConfigRule, Object> getRules() {
        return new HashMap<>(rules);
    }

    public void putRule(ConfigHandler.ConfigRule rule, Object value) {
        rules.put(rule, value);
    }

    public void setRules(Map<ConfigHandler.ConfigRule, Object> rules) {
        this.rules.clear();
        this.rules.putAll(rules);
    }

    public void addRules(Map<ConfigHandler.ConfigRule, Object> rules) {
        this.rules.putAll(rules);
    }

    public void removeRule(ConfigHandler.ConfigRule rule) {
        rules.remove(rule);
    }
}
