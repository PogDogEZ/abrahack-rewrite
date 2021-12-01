package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.handlers.ConfigHandler;
import ez.pogdog.yescom.util.DataType;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ConfigRuleType extends Type<ConfigHandler.ConfigRule> {

    private final EnumType<DataType> DATA_TYPE = new EnumType<>(DataType.class);

    @Override
    public ConfigHandler.ConfigRule read(InputStream inputStream) throws IOException {
        String name = Registry.STRING.read(inputStream);
        boolean enumValue = Registry.BOOLEAN.read(inputStream);

        if (!enumValue) {
            return new ConfigHandler.ConfigRule(name, DATA_TYPE.read(inputStream));
        } else {
            List<String> enumValues = new ArrayList<>();

            int valuesToRead = Registry.UNSIGNED_SHORT.read(inputStream);
            for (int index = 0; index < valuesToRead; ++index) enumValues.add(Registry.STRING.read(inputStream));

            return new ConfigHandler.ConfigRule(name, enumValues);
        }
    }

    @Override
    public void write(ConfigHandler.ConfigRule value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getName(), outputStream);
        Registry.BOOLEAN.write(value.isEnumValue(), outputStream);

        if (!value.isEnumValue()) {
            DATA_TYPE.write(value.getDataType(), outputStream);
        } else {
            Registry.UNSIGNED_SHORT.write(value.getEnumValues().size(), outputStream);
            for (String enumValue : value.getEnumValues()) Registry.STRING.write(enumValue, outputStream);
        }
    }
}
