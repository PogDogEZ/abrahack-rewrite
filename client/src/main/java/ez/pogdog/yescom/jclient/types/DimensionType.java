package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.util.Dimension;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DimensionType extends Type<Dimension> {

    @Override
    public Dimension read(InputStream inputStream) throws IOException {
        return Dimension.fromMC(Registry.SHORT.read(inputStream));
    }

    @Override
    public void write(Dimension value, OutputStream outputStream) throws IOException {
        Registry.SHORT.write((short)value.getMCDim(), outputStream);
    }
}
