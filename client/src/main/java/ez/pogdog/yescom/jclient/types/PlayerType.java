package ez.pogdog.yescom.jclient.types;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.Position;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class PlayerType extends Type<Player> {

    @Override
    public Player read(InputStream inputStream) throws IOException {
        String username = Registry.STRING.read(inputStream);

        UUID uuid = UUID.fromString(Registry.STRING.read(inputStream));
        String displayName = Registry.STRING.read(inputStream);

        Position position = YCRegistry.POSITION.read(inputStream);
        Angle angle = YCRegistry.ANGLE.read(inputStream);

        int dimension = Registry.UNSIGNED_SHORT.read(inputStream);

        float health = Registry.FLOAT.read(inputStream);
        int hunger = Registry.UNSIGNED_SHORT.read(inputStream);
        float saturation = Registry.FLOAT.read(inputStream);

        // This is merely for reading implementation, we won't actually be receiving this type
        return YesCom.getInstance().connectionHandler.getPlayer(uuid);
    }

    @Override
    public void write(Player value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getAuthService().getUsername(), outputStream);

        Registry.STRING.write(value.getAuthService().getSelectedProfile().getId().toString(), outputStream);
        Registry.STRING.write(value.getAuthService().getSelectedProfile().getName(), outputStream);

        YCRegistry.POSITION.write(value.getPosition(), outputStream);
        YCRegistry.ANGLE.write(value.getAngle(), outputStream);

        Registry.SHORT.write((short)value.getDimension().getMCDim(), outputStream);

        Registry.FLOAT.write(value.getFoodStats().getHealth(), outputStream);
        Registry.UNSIGNED_SHORT.write(value.getFoodStats().getHunger(), outputStream);
        Registry.FLOAT.write(value.getFoodStats().getSaturation(), outputStream);
    }
}
