package me.iska.jserver.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A listener for an event.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Listener {

    /**
     * The priority of which the listener is given the event. (Lower numbers mean it is given the event before other
     * listeners).
     */
    int priority() default 1000;
}
