package me.iska.jserver.event;

import me.iska.jserver.event.events.misc.EventOnException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple event bus, NOT skidded from SpleefNet.
 */
public class EventBus {

    private final boolean unregisterOnError = true;

    private final ConcurrentHashMap<Class<? extends Event>, ConcurrentHashMap<Object, Method>> subscriberMethodMap = new ConcurrentHashMap<>();
    private final List<Object> subscribers = new ArrayList<>();

    private boolean handlingError;

    public EventBus() {
        handlingError = false;
    }

    private void crash(Class<? extends Event> eventClazz, Object object, Method method, Throwable exception) {
    }

    private void postEvent(Class<? extends Event> clazz, Event event) {
        if (event == null || subscriberMethodMap.get(clazz) == null) return;
        // FIXME: Pre-sort the priorities to save performance
        subscriberMethodMap.get(clazz).entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getValue().getAnnotation(Listener.class).priority()))
                .forEach(entry -> {
                    if (!subscribers.contains(entry.getKey())) return;
                    try {
                        entry.getValue().setAccessible(true);
                        entry.getValue().invoke(entry.getKey(), clazz.cast(event));
                    } catch (IllegalAccessException | InvocationTargetException error) {
                        // Honestly I'd rather the client didn't crash when something goes wrong

                        if (error instanceof InvocationTargetException) {
                            Throwable targetException = ((InvocationTargetException)error).getTargetException();

                            targetException.printStackTrace();

                            // Avoid recursion when handling errors (i.e. the error handler is throwing the error)
                            if (!handlingError) {
                                handlingError = true;
                                EventOnException eventOnException = (EventOnException)post(new EventOnException(targetException));
                                handlingError = false;

                                if (!eventOnException.isCancelled())
                                    crash(event.getClass(), entry.getKey(), entry.getValue(), targetException);

                            } else {
                                crash(event.getClass(), entry.getKey(), entry.getValue(), targetException);
                            }
                        } else {
                            if (!handlingError) {
                                handlingError = true;
                                EventOnException eventOnException = (EventOnException)post(new EventOnException(error));
                                handlingError = false;

                                if (!eventOnException.isCancelled())
                                    crash(event.getClass(), entry.getKey(), entry.getValue(), error);

                            } else {
                                crash(event.getClass(), entry.getKey(), entry.getValue(), error);
                            }
                            // SpleefNet.getInstance().notifyAllOnError(entry.getKey(), error);
                        }

                        if (unregisterOnError) unregister(entry.getKey());
                    }
                });
    }

    /**
     * Registers an object to the event bus if it has not yet been registered.
     * @param object The object to register.
     */
    @SuppressWarnings("unchecked")
    public void register(Object object) {
        // I'm lazy so I'm just going to leave it at checking if subscribers contains the object, rather than the method
        // map itself
        if (object == null || subscribers.contains(object)) return;
        subscribers.add(object);
        Class<?> clazz = object.getClass();

        synchronized (subscriberMethodMap) {
            // Note getting all the declared methods gives us private and protected ones too
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getParameterTypes().length != 1) continue;
                Class<?> paramClazz = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(paramClazz) && method.isAnnotationPresent(Listener.class)) {
                    if (!subscriberMethodMap.containsKey(paramClazz))
                        subscriberMethodMap.put((Class<? extends Event>)paramClazz, new ConcurrentHashMap<>());
                    subscriberMethodMap.get(paramClazz).put(object, method);
                    // subscriberMethodMap.put((Class<? extends Event>)paramClazz, new Entry<>(object, method));
                }
            }
        }
    }

    /**
     * Unregisters an object from the event bus if it has already been registered.
     * @param object The object to unregister.
     */
    @SuppressWarnings("unchecked")
    public void unregister(Object object) {
        if (object == null || !subscribers.contains(object)) return;
        subscribers.remove(object);
        Class<?> clazz = object.getClass();

        synchronized (subscriberMethodMap) {
            for (Method method : clazz.getDeclaredMethods()) {
                /*
                // We can probably safely assume that the method signature will not have changed
                if (subscriberMethodMap.containsValue(new Entry<>(object, method)))
                    subscriberMethodMap.remove(method.getParameterTypes()[0], new Entry<>(object, method));
                 */

                if (method.getParameterTypes().length != 1) continue;
                Class<?> paramClazz = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(paramClazz) && method.isAnnotationPresent(Listener.class)) {
                    if (!subscriberMethodMap.containsKey(paramClazz))
                        subscriberMethodMap.put((Class<? extends Event>)paramClazz, new ConcurrentHashMap<>());
                    subscriberMethodMap.get(paramClazz).remove(object);
                }
            }
        }
    }

    /**
     * Posts an event to all subscribers on the event bus.
     * @param event The event to post.
     * @return The same event (stfu).
     */
    @SuppressWarnings("unchecked")
    public Event post(Event event) {
        Class<? extends Event> clazz = event.getClass();

        synchronized (subscriberMethodMap) {
            while (true) {
                Class<?> nextSuperClass = clazz.getSuperclass();

                if (subscriberMethodMap.containsKey(clazz)) postEvent(clazz, event);

                if (Event.class.isAssignableFrom(nextSuperClass)) {
                    clazz = (Class<? extends Event>)nextSuperClass;
                } else {
                    return event;
                }
            }
        }
    }

    /**
     * Same as post, except for EventCancellable.
     * @param event The event to post.
     * @return The same event.
     */
    public EventCancellable post(EventCancellable event) {
        return (EventCancellable)post((Event)event);
    }
}
