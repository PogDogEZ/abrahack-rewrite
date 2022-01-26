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
@Packet.Info(name="reporter_action", id=YCRegistry.ID_OFFSET + 13, side=Packet.Side.BOTH)
public class ReporterActionPacket extends Packet {

    private final EnumType<Action> ACTION = new EnumType<>(Action.class);

    private Action action;
    private int reporterID;
    private String reporterName;
    private String reporterHost;
    private int reporterPort;

    public ReporterActionPacket(Action action, int reporterID, String reporterName, String reporterHost, int reporterPort) {
        this.action = action;
        this.reporterID = reporterID;
        this.reporterName = reporterName;
        this.reporterHost = reporterHost;
        this.reporterPort = reporterPort;
    }

    public ReporterActionPacket(Action action, YCReporter reporter) {
        this(action, reporter.getID(), reporter.getName(), reporter.getMinecraftHost(), reporter.getMinecraftPort());
    }

    public ReporterActionPacket(Action action, int reporterID) {
        this(action, reporterID, "", "", 0);
    }

    public ReporterActionPacket() {
    }

    @Override
    public void read(InputStream inputStream) throws IOException {
        action = ACTION.read(inputStream);
        reporterID = Registry.SHORT.read(inputStream);

        if (action == Action.ADD) {
            reporterName = Registry.STRING.read(inputStream);
            reporterHost = Registry.STRING.read(inputStream);
            reporterPort = Registry.UNSIGNED_SHORT.read(inputStream);
        }
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        ACTION.write(action, outputStream);
        Registry.SHORT.write((short)reporterID, outputStream);

        if (action == Action.ADD) {
            Registry.STRING.write(reporterName, outputStream);
            Registry.STRING.write(reporterHost, outputStream);
            Registry.UNSIGNED_SHORT.write(reporterPort, outputStream);
        }
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

    /**
     * @return The IP address of the Minecraft server that the reporter is connected to.
     */
    public String getReporterHost() {
        return reporterHost;
    }

    public void setReporterHost(String reporterHost) {
        this.reporterHost = reporterHost;
    }

    /**
     * @return The port of Minecraft server the reporter is connected to.
     */
    public int getReporterPort() {
        return reporterPort;
    }

    public void setReporterPort(int reporterPort) {
        this.reporterPort = reporterPort;
    }

    public enum Action {
        ADD, REMOVE,
        SELECT;
    }
}
