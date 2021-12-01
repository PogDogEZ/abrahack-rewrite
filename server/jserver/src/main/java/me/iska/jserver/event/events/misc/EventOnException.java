package me.iska.jserver.event.events.misc;

import me.iska.jserver.event.EventCancellable;

public class EventOnException extends EventCancellable {

    private final Throwable throwable;

    public EventOnException(Throwable throwable) {
        this.throwable = throwable;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
