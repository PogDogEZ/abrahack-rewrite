package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.query.IQuery;
import me.iska.jclient.network.packet.Type;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PriorityType extends Type<IQuery.Priority> {

    private final EnumType<IQuery.Priority> PRIORITY = new EnumType<>(IQuery.Priority.class);

    @Override
    public IQuery.Priority read(InputStream inputStream) throws IOException {
        return PRIORITY.read(inputStream);
    }

    @Override
    public void write(IQuery.Priority value, OutputStream outputStream) throws IOException {
        PRIORITY.write(value, outputStream);
    }
}
