package me.iska.jclient.network.packet.types.extended;

import me.iska.jclient.user.Permission;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PermissionType extends Type<Permission> {

    @Override
    public Permission read(InputStream inputStream) throws IOException {
        String name = Registry.STRING.read(inputStream);
        int level = Registry.UNSIGNED_SHORT.read(inputStream);

        return new Permission(name, level);
    }

    @Override
    public void write(Permission value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getName(), outputStream);
        Registry.UNSIGNED_SHORT.write(value.getLevel(), outputStream);
    }
}
