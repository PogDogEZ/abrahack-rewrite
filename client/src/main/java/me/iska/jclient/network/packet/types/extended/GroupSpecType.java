package me.iska.jclient.network.packet.types.extended;

import me.iska.jclient.impl.user.Group;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GroupSpecType extends Type<Group> {

    @Override
    public Group read(InputStream inputStream) throws IOException {
        String groupName = Registry.STRING.read(inputStream);
        int groupID = Registry.INT.read(inputStream);
        short defaultPermission = Registry.SHORT.read(inputStream);

        return new Group(groupName, groupID, defaultPermission);
    }

    @Override
    public void write(Group value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getName(), outputStream);
        Registry.INT.write(value.getGID(), outputStream);
        Registry.SHORT.write((short)value.getDefaultPermission(), outputStream);
    }
}
