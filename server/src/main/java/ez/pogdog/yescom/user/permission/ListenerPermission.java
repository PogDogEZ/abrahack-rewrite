package ez.pogdog.yescom.user.permission;

import me.iska.jserver.user.IPermission;

public class ListenerPermission implements IPermission {

    @Override
    public boolean equals(Object other) {
        return other instanceof ListenerPermission;
    }

    @Override
    public String getName() {
        return "yescom.listener";
    }

    @Override
    public int getLevel() {
        return 0;
    }
}
