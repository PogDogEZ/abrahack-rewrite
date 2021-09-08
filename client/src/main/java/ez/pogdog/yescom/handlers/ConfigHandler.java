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
import java.util.concurrent.atomic.AtomicInteger;

public class ConfigHandler implements IHandler {

    /* ------------------------ Config ------------------------ */

    public boolean DONT_SHOW_EMAILS = true;

    /**
     * ConnectionHandler stuff.
     * LOGIN_TIME - how long to wait before logging in.
     * MIN_TIME_CONNECTED - how long to wait, whilst connected, before sending a packet for a given account.
     * MAX_PACKET_TIME - when to pause packet sending due to lack of response from the server.
     */
    public int LOGIN_TIME = 8000;
    public int MIN_TIME_CONNECTED = 5000;
    public int MAX_PACKET_TIME = 1000;

    public double QUERIES_PER_TICK = 2.2f;
    public int MAX_FINISHED_CACHE = 50;

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
    public int REOPEN_TIME = 2000;
    public int MAX_OPEN_ATTEMPTS = 5;
    public int INVALID_MOVE_TIMEOUT = 2000;

    /**
     * Digging query options.
     */
    public int DIGGING_TIMEOUT = 2000;

    /**
     * The data directories for saving things like raw chunk records + player records.
     */
    public String MAIN_DIRECTORY = "data";
    public String RAW_DIRECTORY = "data/raw";
    public String PLAYER_DIRECTORY = "data/players";

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

        rules.forEach((name, value) -> {
            try {
                Field field = ConfigHandler.class.getField(name.toUpperCase(Locale.ROOT));
                field.setAccessible(true);
                field.set(this, value);
                rulesSet.addAndGet(1);
            } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException error) {
                yesCom.logger.warn(String.format("Couldn't set field \"%s\":", name));
                yesCom.logger.error(error.toString());
            }
        });

        yesCom.logger.debug(String.format("Done, %d fields set.", rulesSet.get()));
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
