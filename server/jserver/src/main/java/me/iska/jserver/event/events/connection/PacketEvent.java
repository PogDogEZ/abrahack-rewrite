package me.iska.jserver.event.events.connection;

import me.iska.jserver.event.EventStage;
import me.iska.jserver.event.EventStageable;

/**
 * Base class for any event involving packets really.
 */
public abstract class PacketEvent extends EventStageable {
    public PacketEvent(EventStage eventStage) {
        super(eventStage);
    }
}
