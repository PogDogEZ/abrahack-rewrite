package ez.pogdog.yescom.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Emits objects on events.
 * @param <T> The object to emit.
 */
public class Emitter<T> {

    private final List<Consumer<T>> listeners = new ArrayList<>();

    private final Class<? extends T> clazz;

    public Emitter(Class<? extends T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Connects a listener to this emitter.
     * @param listener The listener.
     */
    public void connect(Consumer<T> listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    /**
     * Disconnects a listener from this emitter.
     * @param listener The listener.
     */
    public void disconnect(Consumer<T> listener) {
        listeners.remove(listener);
    }

    /**
     * Emits an object to the listeners.
     * @param object The object to emit.
     */
    public void emit(T object) {
        listeners.forEach(listener -> listener.accept(object));
    }

    /**
     * @return The class of the object that this emitter emits.
     */
    public Class<? extends T> getClazz() {
        return clazz;
    }
}
