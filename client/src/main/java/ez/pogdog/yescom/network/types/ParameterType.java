package ez.pogdog.yescom.network.types;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.task.TaskRegistry;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ParameterType extends Type<TaskRegistry.Parameter> {

    @Override
    @SuppressWarnings("unchecked")
    public TaskRegistry.Parameter read(InputStream inputStream) throws IOException {
        TaskRegistry.ParamDescription paramDescription = YCRegistry.PARAM_DESCRIPTION.read(inputStream);
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
            YesCom.getInstance().logger.warning("Couldn't deserialize parameter:");
            YesCom.getInstance().logger.throwing(ParameterType.class.getName(), "read", error);
        }

        return new TaskRegistry.Parameter(paramDescription, values);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(TaskRegistry.Parameter value, OutputStream outputStream) throws IOException {
        TaskRegistry.ParamDescription paramDescription = value.getParamDescription();

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
            YesCom.getInstance().logger.warning("Couldn't serialize parameter:");
            YesCom.getInstance().logger.throwing(ParameterType.class.getName(), "write", error);
        }
    }
}
