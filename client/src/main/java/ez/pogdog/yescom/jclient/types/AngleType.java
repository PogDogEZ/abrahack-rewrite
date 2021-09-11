package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.util.Angle;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AngleType extends Type<Angle> {

    @Override
    public Angle read(InputStream inputStream) throws IOException {
        float yaw = Registry.FLOAT.read(inputStream);
        float pitch = Registry.FLOAT.read(inputStream);
        return new Angle(yaw, pitch);
    }

    @Override
    public void write(Angle value, OutputStream outputStream) throws IOException {
        Registry.FLOAT.write(value.getYaw(), outputStream);
        Registry.FLOAT.write(value.getPitch(), outputStream);
    }
}
