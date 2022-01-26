package ez.pogdog.yescom.network.packets.reporting;

import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.util.ConfigRule;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Sent by the client to synchronise all the config rules.
 */
@Packet.Info(name="config_sync", id=YCRegistry.ID_OFFSET + 13, side=Packet.Side.CLIENT)
public class ConfigSyncPacket extends Packet {

    private final Map<ConfigRule, Object> rules = new HashMap<>();

    public ConfigSyncPacket(Map<ConfigRule, ?> rules) {
        this.rules.putAll(rules);
    }

    public ConfigSyncPacket() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(InputStream inputStream) throws IOException {
        rules.clear();

        int rulesToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < rulesToRead; ++index) {
            ConfigRule configRule = YCRegistry.CONFIG_RULE.read(inputStream);
            Type<Object> type;
            try {
                type = (Type<Object>)Registry.KNOWN_TYPES.get(configRule.getDataType().getClazz()).newInstance();
            } catch (InstantiationException | IllegalAccessException error) {
                throw new IOException(error);
            }
            Object value = type.read(inputStream);

            rules.put(configRule, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(OutputStream outputStream) throws IOException {
        Registry.UNSIGNED_SHORT.write(rules.size(), outputStream);
        for (Map.Entry<ConfigRule, Object> rule : rules.entrySet()) {
            Type<Object> type;
            try {
                type = (Type<Object>)Registry.KNOWN_TYPES.get(rule.getKey().getDataType().getClazz()).newInstance();
            } catch (InstantiationException | IllegalAccessException error) {
                throw new IOException(error);
            }

            YCRegistry.CONFIG_RULE.write(rule.getKey(), outputStream);
            type.write(rule.getValue(), outputStream);
        }
    }

    /**
     * @return The rules and their values.
     */
    public Map<ConfigRule, Object> getRules() {
        return new HashMap<>(rules);
    }

    public void putRule(ConfigRule rule, Object value) {
        rules.put(rule, value);
    }

    public void setRules(Map<ConfigRule, Object> rules) {
        this.rules.clear();
        this.rules.putAll(rules);
    }

    public void addRules(Map<ConfigRule, Object> rules) {
        this.rules.putAll(rules);
    }

    public void removeRule(ConfigRule rule) {
        rules.remove(rule);
    }
}
