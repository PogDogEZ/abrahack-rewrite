package ez.pogdog.yescom.network.types;

import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.util.Player;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Type;

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

        Player player = new Player(username, uuid, displayName);
        player.setLoggedIn(Registry.BOOLEAN.read(inputStream));
        player.setCanLogin(Registry.BOOLEAN.read(inputStream));

        if (player.isLoggedIn()) {
            player.setPosition(YCRegistry.POSITION.read(inputStream));
            player.setAngle(YCRegistry.ANGLE.read(inputStream));
            player.setDimension(Registry.UNSIGNED_SHORT.read(inputStream));

            player.setHealth(Registry.FLOAT.read(inputStream));
            player.setFood(Registry.UNSIGNED_SHORT.read(inputStream));
            player.setSaturation(Registry.FLOAT.read(inputStream));
        }

        return player;
    }

    @Override
    public void write(Player value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getUsername(), outputStream);
        Registry.BYTES.write(ByteBuffer.allocate(16)
                .putLong(value.getUUID().getMostSignificantBits())
                .putLong(value.getUUID().getLeastSignificantBits())
                .array(), outputStream);
        Registry.STRING.write(value.getDisplayName(), outputStream);
        Registry.BOOLEAN.write(value.isLoggedIn(), outputStream);
        Registry.BOOLEAN.write(value.getCanLogin(), outputStream);

        if (value.isLoggedIn()) {
            YCRegistry.POSITION.write(value.getPosition(), outputStream);
            YCRegistry.ANGLE.write(value.getAngle(), outputStream);

            Registry.SHORT.write((short)value.getDimension(), outputStream);

            Registry.FLOAT.write(value.getHealth(), outputStream);
            Registry.UNSIGNED_SHORT.write(value.getFood(), outputStream);
            Registry.FLOAT.write(value.getSaturation(), outputStream);
        }
    }
}
