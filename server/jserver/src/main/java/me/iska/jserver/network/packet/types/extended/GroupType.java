package me.iska.jserver.network.packet.types.extended;

import me.iska.jserver.JServer;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Type;
import me.iska.jserver.user.Group;
import me.iska.jserver.user.IPermission;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GroupType extends Type<Group> {

    @Override
    public Group read(InputStream inputStream) throws IOException {
        Registry.STRING.read(inputStream);
        int id = Registry.INTEGER.read(inputStream);

        int permissionsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
        for (int index = 0; index < permissionsToRead; ++index) Registry.PERMISSION.read(inputStream);

        try {
            return JServer.getInstance().userManager.getGroup(id);
        } catch (IllegalArgumentException error) {
            throw new IOException(error);
        }
    }

    @Override
    public void write(Group value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getName(), outputStream);
        Registry.INTEGER.write(value.getID(), outputStream);

        Registry.UNSIGNED_SHORT.write(value.getPermissions().size(), outputStream);
        for (IPermission permission : value.getPermissions()) Registry.PERMISSION.write(permission, outputStream);
    }
}
