package ez.pogdog.yescom.network.types;

import ez.pogdog.yescom.util.DataType;
import ez.pogdog.yescom.util.task.parameter.ParamDescription;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Type;
import me.iska.jserver.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ParamDescriptionType extends Type<ParamDescription> {

    private final EnumType<ParamDescription.InputType> INPUT_TYPE = new EnumType<>(ParamDescription.InputType.class);
    private final EnumType<DataType> DATA_TYPE = new EnumType<>(DataType.class);

    @Override
    public ParamDescription read(InputStream inputStream) throws IOException {
        String paramName = Registry.STRING.read(inputStream);
        String paramDescription = Registry.STRING.read(inputStream);
        ParamDescription.InputType paramInputType = INPUT_TYPE.read(inputStream);
        DataType paramDataType = DATA_TYPE.read(inputStream);

        return new ParamDescription(paramName, paramDescription, paramInputType, paramDataType);
    }

    @Override
    public void write(ParamDescription value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getName(), outputStream);
        Registry.STRING.write(value.getDescription(), outputStream);
        INPUT_TYPE.write(value.getInputType(), outputStream);
        DATA_TYPE.write(value.getDataType(), outputStream);
    }
}
