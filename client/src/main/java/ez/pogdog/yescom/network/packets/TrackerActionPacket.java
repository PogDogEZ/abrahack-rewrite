package ez.pogdog.yescom.network.packets;

import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.tracking.ITracker;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;
import me.iska.jclient.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sent by the client/server to report a change in the state of a tracker.
 */
@Packet.Info(name="tracker_action", id=YCRegistry.ID_OFFSET + 8, side=Packet.Side.BOTH)
public class TrackerActionPacket extends Packet {

    private final EnumType<Action> ACTION = new EnumType<>(Action.class);

    private final List<BigInteger> trackedPlayerIDs = new ArrayList<>();

    private Action action;
    private long actionID;
    private ITracker tracker;
    private long trackerID;

    public TrackerActionPacket(Action action, long actionID, ITracker tracker, long trackerID, List<BigInteger> trackedPlayerIDs) {
        this.action = action;
        this.actionID = actionID;
        this.tracker = tracker;
        this.trackerID = trackerID;
        this.trackedPlayerIDs.addAll(trackedPlayerIDs);
    }

    public TrackerActionPacket(Action action, long actionID, ITracker tracker) {
        this(action, actionID, tracker, tracker.getTrackerID(),
                tracker.getTrackedPlayers().stream().map(TrackedPlayer::getID).collect(Collectors.toList()));
    }

    public TrackerActionPacket(Action action, ITracker tracker) {
        this(action, -1, tracker);
    }

    public TrackerActionPacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = ACTION.read(inputStream);
        actionID = Registry.LONG.read(inputStream); // FIXME: This prolly only needs to be read with the UNTRACK action

        switch (action) {
            case ADD: {
                tracker = YCRegistry.TRACKER.read(inputStream);
                break;
            }
            case REMOVE:
            case UNTRACK: {
                trackerID = Registry.LONG.read(inputStream);
                break;
            }
            case UPDATE: {
                trackerID = Registry.LONG.read(inputStream);

                int IDsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                for (int index = 0; index < IDsToRead; ++index) trackedPlayerIDs.add(Registry.VAR_INTEGER.read(inputStream));
                break;
            }
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        ACTION.write(action, outputStream);
        Registry.LONG.write(actionID, outputStream);

        switch (action) {
            case ADD: {
                YCRegistry.TRACKER.write(tracker, outputStream);
                break;
            }
            case REMOVE:
            case UNTRACK: {
                Registry.LONG.write(trackerID, outputStream);
                break;
            }
            case UPDATE: {
                Registry.LONG.write(trackerID, outputStream);

                Registry.UNSIGNED_SHORT.write(trackedPlayerIDs.size(), outputStream);
                for (BigInteger trackedPlayerID : trackedPlayerIDs) Registry.VAR_INTEGER.write(trackedPlayerID, outputStream);
                break;
            }
        }
    }

    public List<BigInteger> getTrackedPlayerIDs() {
        return new ArrayList<>(trackedPlayerIDs);
    }

    public void addTrackedPlayerID(BigInteger trackedPlayerID) {
        trackedPlayerIDs.add(trackedPlayerID);
    }

    public void setTrackedPlayerIDs(List<BigInteger> trackedPlayerIDs) {
        this.trackedPlayerIDs.clear();
        this.trackedPlayerIDs.addAll(trackedPlayerIDs);
    }

    public void addTrackedPlayerIDs(List<BigInteger> trackedPlayerIDs) {
        this.trackedPlayerIDs.addAll(trackedPlayerIDs);
    }

    public void removeTrackedPlayerID(BigInteger trackedPlayerID) {
        trackedPlayerIDs.remove(trackedPlayerID);
    }

    /**
     * @return The action being performed.
     */
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public long getActionID() {
        return actionID;
    }

    public void setActionID(long actionID) {
        this.actionID = actionID;
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
        ADD, REMOVE, UPDATE,
        UNTRACK;
    }
}
