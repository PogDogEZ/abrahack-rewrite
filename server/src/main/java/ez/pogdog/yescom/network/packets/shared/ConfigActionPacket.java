package ez.pogdog.yescom.network.packets.shared;

import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.util.ConfigRule;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Type;
import me.iska.jserver.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent by either side to indicate an action being performed on a config rule, can by synchronising the rule or updating it.
 */
@Packet.Info(name="config_action", id=YCRegistry.ID_OFFSET + 4, side=Packet.Side.BOTH)
public class ConfigActionPacket extends Packet {

    private final EnumType<Action> ACTION = new EnumType<>(Action.class);

    private Action action;
    private long actionID;

    private ConfigRule rule;
    private Object value;

    private String ruleName;

    public ConfigActionPacket(Action action, long actionID, ConfigRule rule, Object value, String ruleName) {
        this.action = action;
        this.actionID = actionID;
        this.rule = rule;
        this.value = value;
        this.ruleName = ruleName;
    }

    public ConfigActionPacket(Action action, long actionID, ConfigRule rule, Object value) {
        this(action, actionID, rule, value, rule.getName());
    }

    public ConfigActionPacket(Action action, ConfigRule rule, Object value) {
        this(action, -1, rule, value, rule.getName());
    }

    public ConfigActionPacket(long actionID, String ruleName) {
        this(Action.GET_RULE, actionID, null, null, ruleName);
    }

    public ConfigActionPacket() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(InputStream inputStream) throws IOException {
        action = ACTION.read(inputStream);
        actionID = Registry.LONG.read(inputStream);

        switch (action) {
            case SET_RULE:
            case SYNC_RULE: {
                rule = YCRegistry.CONFIG_RULE.read(inputStream);

                Type<Object> type;
                try {
                    type = (Type<Object>)Registry.KNOWN_TYPES.get(rule.getDataType().getClazz()).newInstance();
                } catch (InstantiationException | IllegalAccessException error) {
                    throw new IOException(error);
                }

                value = type.read(inputStream);
                break;
            }
            case GET_RULE: {
                ruleName = Registry.STRING.read(inputStream);
                break;
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(OutputStream outputStream) throws IOException {
        ACTION.write(action, outputStream);
        Registry.LONG.write(actionID, outputStream);

        switch (action) {
            case SET_RULE:
            case SYNC_RULE: {
                Type<Object> type;
                try {
                    type = (Type<Object>)Registry.KNOWN_TYPES.get(rule.getDataType().getClazz()).newInstance();
                } catch (InstantiationException | IllegalAccessException error) {
                    throw new IOException(error);
                }

                YCRegistry.CONFIG_RULE.write(rule, outputStream);
                type.write(value, outputStream);
                break;
            }
            case GET_RULE: {
                Registry.STRING.write(ruleName, outputStream);
                break;
            }
        }
    }

    /**
     * @return The action being performed.
     */
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * @return The unique action ID.
     */
    public long getActionID() {
        return actionID;
    }

    public void setActionID(long actionID) {
        this.actionID = actionID;
    }

    /**
     * @return The rule.
     */
    public ConfigRule getRule() {
        return rule;
    }

    public void setRule(ConfigRule rule) {
        this.rule = rule;
    }

    /**
     * @return The value of the rule.
     */
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * @return The name of the rule.
     */
    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public enum Action {
        SET_RULE, GET_RULE,
        SYNC_RULE;
    }
}
