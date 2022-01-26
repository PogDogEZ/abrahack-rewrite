package ez.pogdog.yescom.handlers;

/**
 * A template for handlers, they take onTick and onExit calls for each respective event. These must be used.
 */
public interface IHandler {
    void tick();
    void exit();
}
