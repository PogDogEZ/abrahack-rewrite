package ez.pogdog.yescom.network.types;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.util.Angle;
import ez.pogdog.yescom.util.Position;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PlayerType extends Type<Player> {

    @Override
    public Player read(InputStream inputStream) throws IOException {
        String username = Registry.STRING.read(inputStream);

        UUID uuid = UUID.nameUUIDFromBytes(Registry.BYTES.read(inputStream));
        String displayName = Registry.STRING.read(inputStream);
        boolean loggedIn = Registry.BOOLEAN.read(inputStream);

        if (loggedIn) {
            Position position = YCRegistry.POSITION.read(inputStream);
            Angle angle = YCRegistry.ANGLE.read(inputStream);

            int dimension = Registry.UNSIGNED_SHORT.read(inputStream);

            float health = Registry.FLOAT.read(inputStream);
            int hunger = Registry.UNSIGNED_SHORT.read(inputStream);
            float saturation = Registry.FLOAT.read(inputStream);
        }

        // This is merely for reading implementation, we won't actually be receiving this type
        return YesCom.getInstance().connectionHandler.getPlayer(uuid);
    }

    @Override
    public void write(Player value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getAuthService().getUsername(), outputStream);

        UUID uuid = value.getAuthService().getSelectedProfile().getId();
        Registry.BYTES.write(ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array(), outputStream);
        Registry.STRING.write(value.getAuthService().getSelectedProfile().getName(), outputStream);
        Registry.BOOLEAN.write(true, outputStream); // By definition of a player, they should be logged in

        YCRegistry.POSITION.write(value.getPosition(), outputStream);
        YCRegistry.ANGLE.write(value.getAngle(), outputStream);

        Registry.SHORT.write((short)value.getDimension().getMCDim(), outputStream);

        Registry.FLOAT.write(value.getFoodStats().getHealth(), outputStream);
        Registry.UNSIGNED_SHORT.write(value.getFoodStats().getHunger(), outputStream);
        Registry.FLOAT.write(value.getFoodStats().getSaturation(), outputStream);
    }
}
