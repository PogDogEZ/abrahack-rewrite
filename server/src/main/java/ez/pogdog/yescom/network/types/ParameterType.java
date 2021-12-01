package ez.pogdog.yescom.network.types;

import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.util.task.parameter.ParamDescription;
import ez.pogdog.yescom.util.task.parameter.Parameter;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ParameterType extends Type<Parameter> {

    @Override
    @SuppressWarnings("unchecked")
    public Parameter read(InputStream inputStream) throws IOException {
        ParamDescription paramDescription = YCRegistry.PARAM_DESCRIPTION.read(inputStream);
        List<Object> values = new ArrayList<>();

        try {
            Type<Object> type = (Type<Object>)Registry.KNOWN_TYPES.get(paramDescription.getDataType().getClazz()).newInstance();

            switch (paramDescription.getInputType()) {
                case SINGULAR: {
                    values.add(type.read(inputStream));
                    break;
                }
                case ARRAY: {
                    int valuesToRead = Registry.INTEGER.read(inputStream);
                    for (int index = 0; index < valuesToRead; ++index) values.add(type.read(inputStream));
                    break;
                }
            }
        } catch (InstantiationException | IllegalAccessException error) {
            throw new IOException(error);
        }

        return new Parameter(paramDescription, values);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(Parameter value, OutputStream outputStream) throws IOException {
        ParamDescription paramDescription = value.getParamDescription();

        YCRegistry.PARAM_DESCRIPTION.write(paramDescription, outputStream);

        try {
            Type<Object> type = (Type<Object>)Registry.KNOWN_TYPES.get(paramDescription.getDataType().getClazz()).newInstance();

            switch (paramDescription.getInputType()) {
                case SINGULAR: {
                    type.write(value.getValue(), outputStream);
                    break;
                }
                case ARRAY: {
                    List<Object> paramValues = value.getValues();

                    Registry.INTEGER.write(paramValues.size(), outputStream);
                    for (Object paramValue : paramValues) type.write(paramValue, outputStream);
                    break;
                }
            }
        } catch (InstantiationException | IllegalAccessException error) {
            throw new IOException(error);
        }
    }
}
