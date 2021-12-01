package ez.pogdog.yescom.events.config;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.util.ConfigRule;

public class SyncConfigRuleEvent extends ReporterEvent {

    private final ConfigRule configRule;
    private final Object value;

    public SyncConfigRuleEvent(YCReporter reporter, ConfigRule configRule, Object value) {
        super(reporter);
        this.configRule = configRule;
        this.value = value;
    }

    public ConfigRule getConfigRule() {
        return configRule;
    }

    public Object getValue() {
        return value;
    }
}
