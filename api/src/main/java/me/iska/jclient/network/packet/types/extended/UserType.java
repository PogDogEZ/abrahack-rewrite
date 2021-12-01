package me.iska.jclient.network.packet.types.extended;

import me.iska.jclient.user.Group;
import me.iska.jclient.user.Permission;
import me.iska.jclient.user.User;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class UserType extends Type<User> {

    @Override
    public User read(InputStream inputStream) throws IOException {
        String name = Registry.STRING.read(inputStream);
        int id = Registry.INTEGER.read(inputStream);

        List<Permission> permissions = new ArrayList<>();
        int permissionsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < permissionsToRead; ++index) permissions.add(Registry.PERMISSION.read(inputStream));

        Group group = Registry.BOOLEAN.read(inputStream) ? Registry.GROUP.read(inputStream) : null;

        return new User(name, id, group, permissions);
    }

    @Override
    public void write(User value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getUsername(), outputStream);
        Registry.INTEGER.write(value.getID(), outputStream);

        Registry.UNSIGNED_SHORT.write(value.getPermissions().size(), outputStream);
        for (Permission permission : value.getPermissions()) Registry.PERMISSION.write(permission, outputStream);

        if (value.getGroup() != null) {
            Registry.BOOLEAN.write(true, outputStream);
            Registry.GROUP.write(value.getGroup(), outputStream);
        } else {
            Registry.BOOLEAN.write(false, outputStream);
        }
    }
}
