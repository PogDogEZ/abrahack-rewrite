package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.util.Position;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PositionType extends Type<Position> {

    @Override
    public Position read(InputStream inputStream) throws IOException {
        double x = Registry.DOUBLE.read(inputStream);
        double y = Registry.DOUBLE.read(inputStream);
        double z = Registry.DOUBLE.read(inputStream);
        return new Position(x, y, z);
    }

    @Override
    public void write(Position value, OutputStream outputStream) throws IOException {
        Registry.DOUBLE.write(value.getX(), outputStream);
        Registry.DOUBLE.write(value.getY(), outputStream);
        Registry.DOUBLE.write(value.getZ(), outputStream);
    }
}
