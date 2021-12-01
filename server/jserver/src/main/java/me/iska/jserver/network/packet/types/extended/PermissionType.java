package me.iska.jserver.network.packet.types.extended;

import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Type;
import me.iska.jserver.user.IPermission;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ServiceLoader;

public class PermissionType extends Type<IPermission> {

    @Override
    public IPermission read(InputStream inputStream) throws IOException {
        String name = Registry.STRING.read(inputStream);
        int level = Registry.UNSIGNED_SHORT.read(inputStream);

        for (IPermission permission : ServiceLoader.load(IPermission.class)) {
            try {
                if (permission.getName().equals(name))
                    return permission.getClass().getConstructor(int.class).newInstance(level);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException |
                    InvocationTargetException error) {
                throw new IOException(error);
            }
        }

        throw new IOException(String.format("No permission with name %s known.", name));
    }

    @Override
    public void write(IPermission value, OutputStream outputStream) throws IOException {
        Registry.STRING.write(value.getName(), outputStream);
        Registry.UNSIGNED_SHORT.write(value.getLevel(), outputStream);
    }
}
