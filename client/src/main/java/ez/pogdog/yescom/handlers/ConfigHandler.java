package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IsLoadedQuery;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ConfigHandler implements IHandler {

    /* ------------------------ Config ------------------------ */

    public boolean DONT_SHOW_EMAILS = true;

    public int RSA_KEY_SIZE = 2048;
    public String IDENTITY_DIRECTORY = "identity";

    public String HOST_NAME = "localhost";
    public int HOST_PORT = 5001;
    public String USERNAME = "node";
    public String GROUP_NAME = "local";
    public String PASSWORD = "t";

    /**
     * How long to wait before logging in.
     */
    public int LOGIN_TIME = 8000;

    /**
     * How long to wait, while connected, before sending a packet for a given account.
     */
    public int MIN_TIME_CONNECTED = 5000;

    /**
     * When to puase packet sending due to lack of server response.
     */
    public int MAX_PACKET_TIME = 1000;

    public double QUERIES_PER_TICK = 1.0f;
    public int MAX_FINISHED_CACHE = 50;

    public int LOGIN_CACHE_TIME = 5000;
    public int LOGOUT_CACHE_TIME = 30000;

    /**
     * Health logout stuff.
     */
    public double LOG_OUT_HEALTH = 3.0f;
    public int HEALTH_RELOG_TIME = 120000;

    public IsLoadedQuery.Type TYPE = IsLoadedQuery.Type.INVALID_MOVE;

    /**
     * Invalid move query options.
     */
    public boolean ARZI_MODE = false;
    public boolean ARZI_MODE_NO_WID_RESYNC = true; // WID resync is brokey :( <- wrong it's worky now just keep QPT <= 1
    public int REOPEN_TIME = 2000;
    public int MAX_OPEN_ATTEMPTS = 5;
    public int INVALID_MOVE_TIMEOUT = 2000;

    /**
     * Digging query options.
     */
    public int DIGGING_TIMEOUT = 2000;

    /**
     * The render distance of the server * 2 + 1.
     */
    public int RENDER_DISTANCE = 13;

    public int MAX_RESTART_CHECK_TIME = 5000;

    /**
     * How long to wait since the last loaded chunk before checking if the player is online. Recommended lower if
     * inverted is on.
     */
    public int BASIC_TRACKER_ONLINE_CHECK_TIME = 1000;
    /**
     * How far to check from known chunks.
     */
    public int BASIC_TRACKER_DIST = 5;

    public double ADAPTIVE_TRACKER_PHASE_CHANGE_NORMAL = -0.01;
    public double ADAPTIVE_TRACKER_PHASE_CHANGE_GROW = 0.5;
    public double ADAPTIVE_TRACKER_PHASE_CHANGE_SHRINK = -0.1;

    public double ADAPTIVE_TRACKER_MIN_EXPECTED = 0.6;
    public double ADAPTIVE_TRACKER_MAX_EXPECTED = 0.95;

    public int ADAPTIVE_TRACKER_EARLY_SAMPLES = 1;
    public int ADAPTIVE_TRACKER_LATE_SAMPLES = 12;

    /**
     * The data directories for saving things like raw chunk records + player records.
     */
    public String MAIN_DIRECTORY = "data";
    public String RAW_DIRECTORY = "data/raw";
    public String PLAYER_DIRECTORY = "data/players";
    public String TRACKERS_DIRECTORY = "data/trackers";

    public int FOUND_LOADED_SAVE_CACHE = 500;

    /* ------------------------ Other Fields ------------------------ */

    private final YesCom yesCom = YesCom.getInstance();

    private final File configFile;

    private long autoSaveTime;

    public ConfigHandler(String configFilePath) {
        this.configFile = Paths.get(configFilePath).toFile();

        try {
            readConfig();
        } catch (IOException error) {
            yesCom.logger.warn("Error while reading config:");
            yesCom.logger.error(error.toString());
        }

        autoSaveTime = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        if (System.currentTimeMillis() - autoSaveTime > 30000) {
            try {
                dumpConfig();
            } catch (IOException error) {
                yesCom.logger.warn("Couldn't save config due to:");
                yesCom.logger.error(error.toString());
                autoSaveTime = System.currentTimeMillis() + 30000; // Let's keep increasing this to not spam console
                return;
            }

            autoSaveTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onExit() {
    }

    public void readConfig() throws IOException {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
        dumperOptions.setPrettyFlow(true);

        Yaml yaml = new Yaml(dumperOptions);

        if (!configFile.exists() || configFile.isDirectory()) {
            yesCom.logger.warn("Config file does not exist, creating a new one.");
            dumpConfig();
        }

        yesCom.logger.debug("Reading config...");

        InputStream inputStream = new FileInputStream(configFile);
        Map<String, Object> rules = yaml.load(new InputStreamReader(inputStream));
        inputStream.close();

        AtomicInteger rulesSet = new AtomicInteger(0);
        AtomicBoolean requiresRewrite = new AtomicBoolean(false);

        rules.forEach((name, value) -> {
            try {
                Field field = ConfigHandler.class.getField(name.toUpperCase(Locale.ROOT));
                field.setAccessible(true);
                field.set(this, value);
                rulesSet.addAndGet(1);
            } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException error) {
                yesCom.logger.warn(String.format("Couldn't set field \"%s\":", name));
                yesCom.logger.error(error.toString());

                requiresRewrite.set(true);
            }
        });

        yesCom.logger.debug(String.format("Done, %d fields set.", rulesSet.get()));

        if (requiresRewrite.get()) {
            yesCom.logger.info("Rewriting config...");
            dumpConfig();
        }
    }

    public void dumpConfig() throws IOException {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
        dumperOptions.setPrettyFlow(true);

        Yaml yaml = new Yaml(dumperOptions);

        if ((!configFile.exists() || configFile.isDirectory()) &&
                !configFile.createNewFile()) throw new IOException("Couldn't create the config file due to unknown.");
        // ProxyCon.logger.info("Done.");

        yesCom.logger.debug("Dumping config...");

        Map<String, Object> config = new LinkedHashMap<>();

        for (Field field : ConfigHandler.class.getFields()) {
            field.setAccessible(true);
            try {
                config.put(field.getName().toLowerCase(Locale.ROOT), field.get(this));
            } catch (IllegalAccessException | IllegalArgumentException ignored) {
            }
        }

        yesCom.logger.debug(String.format("Found %d fields.", config.size()));

        OutputStream outputStream = new FileOutputStream(configFile);
        yaml.dump(config, new OutputStreamWriter(outputStream));
        outputStream.close();

        yesCom.logger.debug("Dumped config.");
    }

    public File getConfigFile() {
        return configFile;
    }
}
