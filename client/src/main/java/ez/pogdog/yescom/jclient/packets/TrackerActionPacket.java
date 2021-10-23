package ez.pogdog.yescom.jclient.packets;

import ez.pogdog.yescom.jclient.YCRegistry;
import ez.pogdog.yescom.tracking.ITracker;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Packet.Info(name="tracker_action", id=YCRegistry.ID_OFFSET + 14, side=Packet.Side.CLIENT)
public class TrackerActionPacket extends Packet {

    private final EnumType<Action> ACTION = new EnumType<>(Action.class);

    private Action action;
    private ITracker tracker;
    private long trackerID;

    public TrackerActionPacket(Action action, ITracker tracker, long trackerID) {
        this.action = action;
        this.tracker = tracker;
        this.trackerID = trackerID;
    }

    public TrackerActionPacket(Action action, ITracker tracker) {
        this(action, tracker, tracker.getTrackerID());
    }

    public TrackerActionPacket() {
        this(Action.ADD, null, 0L);
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = ACTION.read(inputStream);

        switch (action) {
            case ADD: {
                tracker = YCRegistry.TRACKER.read(inputStream);
                break;
            }
            case REMOVE:
            case UPDATE: {
                trackerID = Registry.LONG.read(inputStream);
                break;
            }
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        ACTION.write(action, outputStream);

        switch (action) {
            case ADD: {
                YCRegistry.TRACKER.write(tracker, outputStream);
                break;
            }
            case REMOVE:
            case UPDATE: {
                Registry.LONG.write(trackerID, outputStream);
                break;
            }
        }
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public ITracker getTracker() {
        return tracker;
    }

    public void setTracker(ITracker tracker) {
        this.tracker = tracker;
    }

    public long getTrackerID() {
        return trackerID;
    }

    public void setTrackerID(long trackerID) {
        this.trackerID = trackerID;
    }

    public enum Action {
        ADD, REMOVE, UPDATE;
    }
}
