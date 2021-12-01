package ez.pogdog.yescom.network.packets.listening;

import ez.pogdog.yescom.network.YCRegistry;
import ez.pogdog.yescom.util.ConfigRule;
import ez.pogdog.yescom.util.Player;
import ez.pogdog.yescom.util.Tracker;
import ez.pogdog.yescom.util.task.ActiveTask;
import ez.pogdog.yescom.util.task.RegisteredTask;
import ez.pogdog.yescom.util.task.parameter.ParamDescription;
import ez.pogdog.yescom.util.task.parameter.Parameter;
import me.iska.jserver.network.packet.Packet;
import me.iska.jserver.network.packet.Registry;
import me.iska.jserver.network.packet.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sent by the server to synchronise a selected reporter.
 */
@Packet.Info(name="reporter_sync", id=YCRegistry.ID_OFFSET + 13, side=Packet.Side.SERVER)
public class ReporterSyncPacket extends Packet {

    private final Map<ConfigRule, Object> configRules = new HashMap<>();
    private final List<RegisteredTask> registeredTasks = new ArrayList<>();
    private final List<ActiveTask> activeTasks = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
    private final List<Tracker> trackers = new ArrayList<>();

    private boolean hasReporter;
    private int reporterID;
    private String reporterName;

    public ReporterSyncPacket(boolean hasReporter, int reporterID, String reporterName, Map<ConfigRule, Object> configRules,
                              List<RegisteredTask> registeredTasks, List<ActiveTask> activeTasks, List<Player> players,
                              List<Tracker> trackers) {
        this.hasReporter = hasReporter;
        this.reporterID = reporterID;
        this.reporterName = reporterName;

        this.configRules.putAll(configRules);
        this.registeredTasks.addAll(registeredTasks);
        this.activeTasks.addAll(activeTasks);
        this.players.addAll(players);
        this.trackers.addAll(trackers);
    }

    public ReporterSyncPacket(int reporterID, String reporterName, Map<ConfigRule, Object> configRules,
                              List<RegisteredTask> registeredTasks, List<ActiveTask> activeTasks, List<Player> players,
                              List<Tracker> trackers) {
        this(true, reporterID, reporterName, configRules, registeredTasks, activeTasks, players, trackers);
    }

    public ReporterSyncPacket() {
        this(false, 0, "", new HashMap<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(InputStream inputStream) throws IOException {
        hasReporter = Registry.BOOLEAN.read(inputStream);

        if (hasReporter) {
            reporterID = Registry.SHORT.read(inputStream);
            reporterName = Registry.STRING.read(inputStream);

            configRules.clear();
            registeredTasks.clear();
            activeTasks.clear();
            players.clear();
            trackers.clear();

            int configRulesToRead = Registry.UNSIGNED_SHORT.read(inputStream);
            for (int index = 0; index < configRulesToRead; index++) {
                ConfigRule configRule = YCRegistry.CONFIG_RULE.read(inputStream);
                Type<Object> type;
                try {
                    type = (Type<Object>)Registry.KNOWN_TYPES.get(configRule.getDataType().getClazz()).newInstance();
                } catch (InstantiationException | IllegalAccessException error) {
                    throw new IOException(error);
                }
                Object value = type.read(inputStream);

                configRules.put(configRule, value);
            }

            int registeredTasksToRead = Registry.UNSIGNED_SHORT.read(inputStream);
            for (int index = 0; index < registeredTasksToRead; ++index) {
                String taskName = Registry.STRING.read(inputStream);
                String taskDescription = Registry.STRING.read(inputStream);

                List<ParamDescription> paramDescriptions = new ArrayList<>();
                int paramDescriptionsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                for (int index1 = 0; index1 < paramDescriptionsToRead; ++index1) paramDescriptions.add(YCRegistry.PARAM_DESCRIPTION.read(inputStream));

                registeredTasks.add(new RegisteredTask(taskName, taskDescription, paramDescriptions));
            }

            int activeTasksToRead = Registry.UNSIGNED_SHORT.read(inputStream);
            for (int index = 0; index < activeTasksToRead; ++index) {
                int taskID = Registry.UNSIGNED_SHORT.read(inputStream);
                String taskName = Registry.STRING.read(inputStream);

                List<Parameter> parameters = new ArrayList<>();
                int parametersToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                for (int index1 = 0; index1 < parametersToRead; ++index1) parameters.add(YCRegistry.PARAMETER.read(inputStream));

                float progress = Registry.FLOAT.read(inputStream);
                int timeElapsed = Registry.INTEGER.read(inputStream);

                List<String> results = new ArrayList<>();
                int resultsToRead = Registry.UNSIGNED_SHORT.read(inputStream);
                for (int index1 = 0; index1 < resultsToRead; ++index1) results.add(Registry.STRING.read(inputStream));

                registeredTasks.stream() // Lol this is a very big hack but it works
                        .filter(task -> task.getName().equals(taskName))
                        .findFirst()
                        .ifPresent(registeredTask -> activeTasks.add(new ActiveTask(registeredTask, taskID, parameters,
                                progress, timeElapsed, results)));

            }

            int playersToRead = Registry.UNSIGNED_SHORT.read(inputStream);
            for (int index = 0; index < playersToRead; ++index) players.add(YCRegistry.PLAYER.read(inputStream));

            int trackersToRead = Registry.UNSIGNED_SHORT.read(inputStream);
            for (int index = 0; index < trackersToRead; ++index) trackers.add(YCRegistry.TRACKER.read(inputStream));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(OutputStream outputStream) throws IOException {
        Registry.BOOLEAN.write(hasReporter, outputStream);

        if (hasReporter) {
            Registry.SHORT.write((short)reporterID, outputStream);
            Registry.STRING.write(reporterName, outputStream);

            Registry.UNSIGNED_SHORT.write(configRules.size(), outputStream);
            for (Map.Entry<ConfigRule, Object> entry : configRules.entrySet()) {
                Type<Object> type;
                try {
                    type = (Type<Object>)Registry.KNOWN_TYPES.get(entry.getKey().getDataType().getClazz()).newInstance();
                } catch (InstantiationException | IllegalAccessException error) {
                    throw new IOException(error);
                }

                YCRegistry.CONFIG_RULE.write(entry.getKey(), outputStream);
                type.write(entry.getValue(), outputStream);
            }

            Registry.UNSIGNED_SHORT.write(registeredTasks.size(), outputStream);
            for (RegisteredTask registeredTask : registeredTasks) {
                Registry.STRING.write(registeredTask.getName(), outputStream);
                Registry.STRING.write(registeredTask.getDescription(), outputStream);

                Registry.UNSIGNED_SHORT.write(registeredTask.getParamDescriptions().size(), outputStream);
                for (ParamDescription paramDescription : registeredTask.getParamDescriptions()) YCRegistry.PARAM_DESCRIPTION.write(paramDescription, outputStream);
            }

            Registry.UNSIGNED_SHORT.write(activeTasks.size(), outputStream);
            for (ActiveTask activeTask : activeTasks) {
                Registry.UNSIGNED_SHORT.write(activeTask.getID(), outputStream);
                Registry.STRING.write(activeTask.getRegisteredTask().getName(), outputStream);

                Registry.UNSIGNED_SHORT.write(activeTask.getParameters().size(), outputStream);
                for (Parameter parameter : activeTask.getParameters()) YCRegistry.PARAMETER.write(parameter, outputStream);

                Registry.FLOAT.write(activeTask.getProgress(), outputStream);
                Registry.INTEGER.write(activeTask.getTimeElapsed(), outputStream);

                Registry.UNSIGNED_SHORT.write(activeTask.getResults().size(), outputStream);
                for (String result : activeTask.getResults()) Registry.STRING.write(result, outputStream);
            }

            Registry.UNSIGNED_SHORT.write(players.size(), outputStream);
            for (Player player : players) YCRegistry.PLAYER.write(player, outputStream);

            Registry.UNSIGNED_SHORT.write(trackers.size(), outputStream);
            for (Tracker tracker : trackers) YCRegistry.TRACKER.write(tracker, outputStream);
        }
    }

    /**
     * @return The config rules and their values.
     */
    public Map<ConfigRule, Object> getRules() {
        return new HashMap<>(configRules);
    }

    public void putRule(ConfigRule rule, Object value) {
        configRules.put(rule, value);
    }

    public void setRules(Map<ConfigRule, Object> rules) {
        this.configRules.clear();
        this.configRules.putAll(rules);
    }

    public void addRules(Map<ConfigRule, Object> rules) {
        this.configRules.putAll(rules);
    }

    public void removeRule(ConfigRule rule) {
        configRules.remove(rule);
    }

    /**
     * @return The registered tasks that are available to be run.
     */
    public List<RegisteredTask> getRegisteredTasks() {
        return new ArrayList<>(registeredTasks);
    }

    public void addRegisteredTask(RegisteredTask registeredTask) {
        registeredTasks.add(registeredTask);
    }

    public void setRegisteredTasks(List<RegisteredTask> registeredTasks) {
        this.registeredTasks.clear();
        this.registeredTasks.addAll(registeredTasks);
    }

    public void addRegisteredTasks(List<RegisteredTask> registeredTasks) {
        this.registeredTasks.addAll(registeredTasks);
    }

    public void removeRegisteredTask(RegisteredTask registeredTask) {
        registeredTasks.remove(registeredTask);
    }

    /**
     * @return The currently active tasks.
     */
    public List<ActiveTask> getActiveTasks() {
        return new ArrayList<>(activeTasks);
    }

    public void addActiveTask(ActiveTask activeTask) {
        activeTasks.add(activeTask);
    }

    public void setActiveTasks(List<ActiveTask> activeTasks) {
        this.activeTasks.clear();
        this.activeTasks.addAll(activeTasks);
    }

    public void addActiveTasks(List<ActiveTask> activeTasks) {
        this.activeTasks.addAll(activeTasks);
    }

    public void removeActiveTask(ActiveTask activeTask) {
        activeTasks.remove(activeTask);
    }

    /**
     * @return The players.
     */
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void setPlayers(List<Player> players) {
        this.players.clear();
        this.players.addAll(players);
    }

    public void addPlayers(List<Player> players) {
        this.players.addAll(players);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    /**
     * @return The trackers.
     */
    public List<Tracker> getTrackers() {
        return new ArrayList<>(trackers);
    }

    public void addTracker(Tracker tracker) {
        trackers.add(tracker);
    }

    public void setTrackers(List<Tracker> trackers) {
        this.trackers.clear();
        this.trackers.addAll(trackers);
    }

    public void addTrackers(List<Tracker> trackers) {
        this.trackers.addAll(trackers);
    }

    public void removeTracker(Tracker tracker) {
        trackers.remove(tracker);
    }

    /**
     * @return Whether or not the packet contains a reporter.
     */
    public boolean getHasReporter() {
        return hasReporter;
    }

    public void setHasReporter(boolean hasReporter) {
        this.hasReporter = hasReporter;
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
}
