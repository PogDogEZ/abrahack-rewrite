package ez.pogdog.yescom.network.types;

import ez.pogdog.yescom.task.TaskRegistry;
import ez.pogdog.yescom.util.DataType;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ParamDescriptionType extends Type<TaskRegistry.ParamDescription> {

    private final EnumType<TaskRegistry.ParamDescription.InputType> INPUT_TYPE = new EnumType<>(TaskRegistry.ParamDescription.InputType.class);
    private final EnumType<DataType> DATA_TYPE = new EnumType<>(DataType.class);

    @Override
    public TaskRegistry.ParamDescription read(InputStream inputStream) throws IOException {
        String paramName = Registry.STRING.read(inputStream);
        String paramDescription = Registry.STRING.read(inputStream);
        TaskRegistry.ParamDescription.InputType paramInputType = INPUT_TYPE.read(inputStream);
        DataType paramDataType = DATA_TYPE.read(inputStream);

        return new TaskRegistry.ParamDescription(paramName, paramDescription, paramInputType, paramDataType);
    }

    @Override
    public void write(TaskRegistry.ParamDescription value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getName(), outputStream);
        Registry.STRING.write(value.getDescription(), outputStream);
        INPUT_TYPE.write(value.getInputType(), outputStream);
        DATA_TYPE.write(value.getDataType(), outputStream);
    }
}
