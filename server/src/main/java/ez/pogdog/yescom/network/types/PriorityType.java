package ez.pogdog.yescom.network.types;

import ez.pogdog.yescom.util.Priority;
import me.iska.jserver.network.packet.Type;
import me.iska.jserver.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PriorityType extends Type<Priority> {

    private final EnumType<Priority> PRIORITY = new EnumType<>(Priority.class);

    @Override
    public Priority read(InputStream inputStream) throws IOException {
        return PRIORITY.read(inputStream);
    }

    @Override
    public void write(Priority value, OutputStream outputStream) throws IOException {
        PRIORITY.write(value, outputStream);
    }
}
