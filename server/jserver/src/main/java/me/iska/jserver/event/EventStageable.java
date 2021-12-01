package me.iska.jserver.event;

/**
 * An event that happens in multiple stages.
 */
public abstract class EventStageable extends EventCancellable {

    private final EventStage eventStage;

    public EventStageable(EventStage eventStage) {
        this.eventStage = eventStage;
    }

    public EventStage getStage() {
        return eventStage;
    }
}
