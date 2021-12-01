package ez.pogdog.yescom.util;

import java.util.ArrayList;
import java.util.List;

public class ConfigRule {

    private final List<String> enumValues = new ArrayList<>();

    private final String name;
    private final DataType dataType;
    private final boolean enumValue;

    public ConfigRule(String name, DataType dataType) {
        this.name = name;
        this.dataType = dataType;

        enumValue = false;
    }

    public ConfigRule(String name, List<String> enumValues) {
        this.name = name;
        this.enumValues.addAll(enumValues);

        dataType = DataType.STRING; // We'll assume it's a string cos I'm lazy
        enumValue = true;
    }

    @Override
    public String toString() {
        return String.format("ConfigRule(name=%s, dataType=%s)", name, dataType);
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public String getName() {
        return name;
    }

    public DataType getDataType() {
        return dataType;
    }

    public boolean isEnumValue() {
        return enumValue;
    }
}
