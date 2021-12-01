package me.iska.jserver.event;

/**
 * Cancellable event, can be used arbitrarily.
 */
public abstract class EventCancellable extends Event {

    private boolean cancelled;

    public EventCancellable() {
        cancelled = false;
    }

    public void cancel() {
        setCancelled(true);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean isCancelled) {
        this.cancelled = isCancelled;
    }
}
