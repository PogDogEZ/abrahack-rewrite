package me.iska.jserver;

public interface IManager {

    /**
     * Called every synchronous system tick.
     */
    void update();

    /**
     * Called on system shutdown.
     */
    void exit();
}
