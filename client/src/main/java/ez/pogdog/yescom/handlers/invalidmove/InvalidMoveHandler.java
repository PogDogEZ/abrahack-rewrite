package ez.pogdog.yescom.handlers.invalidmove;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.handlers.ConfigHandler;
import ez.pogdog.yescom.handlers.IHandler;
import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.query.IsLoadedQuery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class InvalidMoveHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    /**
     * Valid storages / containers that can be used.
     * 23 - dispenser.
     * 54 - chest.
     * 130 - ender chest.
     * 154 - hopper.
     * 158 - dropper.
     */
    public final List<Integer> VALID_STORAGES = Arrays.asList(23, 54, 130, 154, 158);

    private final Deque<PlayerHandle> handles = new ConcurrentLinkedDeque<>();

    @Override
    public void onTick() {
        if (yesCom.configHandler.TYPE != IsLoadedQuery.Type.INVALID_MOVE) return;

        handles.forEach(PlayerHandle::onTick);
    }

    @Override
    public void onExit() {
        handles.forEach(PlayerHandle::onExit);
    }

    public void addHandle(Player player) {
        if (handles.stream().noneMatch(handle -> handle.getPlayer().equals(player))) handles.add(new PlayerHandle(player));
    }

    public PlayerHandle getHandle(Player player) {
        return handles.stream().filter(handle -> handle.getPlayer().equals(player)).findFirst().orElse(null);
    }

    public void removeHandle(Player player) {
        PlayerHandle handle = getHandle(player);
        if (handle != null) handles.remove(handle);
    }

    public Player startQuery(IsLoadedQuery query) {
        Optional<PlayerHandle> bestHandle = handles.stream()
                .filter(handle -> handle.getPlayer().isConnected() &&
                        handle.getPlayer().getTimeLoggedIn() > yesCom.configHandler.MIN_TIME_CONNECTED &&
                        handle.getPlayer().getDimension() == query.getDimension() &&
                        handle.isStorageOpen())
                .min(Comparator.comparingInt(PlayerHandle::getCurrentQueries));

        if (bestHandle.isPresent()) {
            if (!bestHandle.get().addQuery(query)) return null;
            return bestHandle.get().getPlayer();
        } else {
            return null;
        }
    }

    public int getAvailableAccounts(int dimension) {
        return (int)handles.stream()
                .filter(handle -> handle.getPlayer().isConnected() &&
                        handle.getPlayer().getTimeLoggedIn() > yesCom.configHandler.MIN_TIME_CONNECTED &&
                        handle.getPlayer().getDimension() == dimension &&
                        handle.isStorageOpen())
                .count();
    }
}
