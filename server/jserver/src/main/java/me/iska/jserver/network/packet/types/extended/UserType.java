package me.iska.jserver.network.packet.types.extended;

import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Type;
import me.iska.jserver.user.Group;
import me.iska.jserver.user.IPermission;
import me.iska.jserver.user.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UserType extends Type<User> {

    @Override
    public User read(InputStream inputStream) throws IOException {
        Registry.STRING.read(inputStream);
        int id = Registry.INTEGER.read(inputStream);

        int permissionsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < permissionsToRead; ++index) Registry.PERMISSION.read(inputStream);

        Group group = Registry.BOOLEAN.read(inputStream) ? Registry.GROUP.read(inputStream) : null;

        if (group != null) {
            try {
                return group.getUser(id);
            } catch (IllegalArgumentException error) {
                throw new IOException(error);
            }
        } else {
            throw new IOException("Null group when reading user, despite expected lookup.");
        }
    }

    @Override
    public void write(User value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getUsername(), outputStream);
        Registry.INTEGER.write(value.getID(), outputStream);

        Registry.UNSIGNED_SHORT.write(value.getPermissions().size(), outputStream);
        for (IPermission permission : value.getPermissions()) Registry.PERMISSION.write(permission, outputStream);

        if (value.getGroup() != null) {
            Registry.BOOLEAN.write(true, outputStream);
            Registry.GROUP.write(value.getGroup(), outputStream);
        } else {
            Registry.BOOLEAN.write(false, outputStream);
        }
    }
}
