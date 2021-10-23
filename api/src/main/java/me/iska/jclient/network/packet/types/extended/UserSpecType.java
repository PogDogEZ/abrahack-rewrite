package me.iska.jclient.network.packet.types.extended;

import me.iska.jclient.impl.user.Group;
import me.iska.jclient.impl.user.User;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UserSpecType extends Type<User> {

    @Override
    public User read(InputStream inputStream) throws IOException {
        String username = Registry.STRING.read(inputStream);
        int userID = Registry.INT.read(inputStream);
        short permission = Registry.SHORT.read(inputStream);
        Group group = Registry.BOOLEAN.read(inputStream) ? Registry.GROUP.read(inputStream) : null;

        return new User(username, userID, permission, group);
    }

    @Override
    public void write(User value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getUsername(), outputStream);
        Registry.INT.write(value.getUID(), outputStream);
        Registry.SHORT.write((short)value.getPermission(), outputStream);

        if (value.getGroup() != null) {
            Registry.BOOLEAN.write(true, outputStream);
            Registry.GROUP.write(value.getGroup(), outputStream);
        } else {
            Registry.BOOLEAN.write(false, outputStream);
        }
    }
}
