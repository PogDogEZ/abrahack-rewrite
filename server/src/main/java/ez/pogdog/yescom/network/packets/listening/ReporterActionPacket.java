package ez.pogdog.yescom.network.packets.listening;

import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.network.handlers.YCReporter;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.types.EnumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sent by the server to inform the client of available reporters, as well as allowing the client to select a current
 * reporter.
 */
@Packet.Info(name="reporter_action", id=YCRegistry.ID_OFFSET + 12, side=Packet.Side.BOTH)
public class ReporterActionPacket extends Packet {

    private final EnumType<Action> ACTION = new EnumType<>(Action.class);

    private Action action;
    private int reporterID;
    private String reporterName;

    public ReporterActionPacket(Action action, int reporterID, String reporterName) {
        this.action = action;
        this.reporterID = reporterID;
        this.reporterName = reporterName;
    }

    public ReporterActionPacket(Action action, YCReporter reporter) {
        this(action, reporter.getID(), reporter.getName());
    }

    public ReporterActionPacket(Action action, int reporterID) {
        this(action, reporterID, "");
    }

    public ReporterActionPacket() {
        this(Action.ADD, 0, "");
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = ACTION.read(inputStream);
        reporterID = Registry.SHORT.read(inputStream);

        if (action == Action.ADD) reporterName = Registry.STRING.read(inputStream);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        ACTION.write(action, outputStream);
        Registry.SHORT.write((short)reporterID, outputStream);

        if (action == Action.ADD) Registry.STRING.write(reporterName, outputStream);
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

    /**
     * @return The ID of the reporter.
     */
    public int getReporterID() {
        return reporterID;
    }

    public void setReporterID(int reporterID) {
        this.reporterID = reporterID;
    }

    /**
     * @return The name of the reporter.
     */
    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public enum Action {
        ADD, REMOVE,
        SELECT;
    }
}
