package ez.pogdog.yescom.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.queries.IsLoadedQuery;
import ez.pogdog.yescom.util.*;
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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ConfigHandler implements IHandler {

    /* ------------------------ Config ------------------------ */

    @Unsettable
    public boolean DONT_SHOW_EMAILS = true;

    @Unsettable
    public int RSA_KEY_SIZE = 2048;
    @Ungettable
    @Unsettable
    public String IDENTITY_DIRECTORY = "identity";

    @Ungettable
    @Unsettable
    public String HOST_NAME = "localhost";
    @Ungettable
    @Unsettable
    public int HOST_PORT = 5001;
    @Ungettable
    @Unsettable
    public String USERNAME = "node";
    @Ungettable
    @Unsettable
    public String GROUP_NAME = "local";
    @Ungettable
    @Unsettable
    public String PASSWORD = "t";

    @Unsettable
    public boolean BROADCAST_CHUNK_STATES = true;
    @Unsettable
    public boolean BROADCAST_TRACKED_PLAYERS = false; // The viewer should request these when trackers get added anyway

    /**
     * How long to wait before logging in.
     */
    public int LOGIN_TIME = 8000;

    /**
     * How long to wait, while connected, before sending a packet for a given account.
     */
    public int MIN_TIME_CONNECTED = 5000;

    /**
     * When to pause packet sending due to lack of server response.
     */
    public int MAX_PACKET_TIME = 1000;

    public double LOADED_QUERIES_PER_TICK = 1.0;
    public double BLOCK_STATE_QUERIES_PER_TICK = 1.0;

    /**
     * How often to update numerical data such as tickrate.
     */
    public int NUMERICAL_DATA_UPDATE_INTERVAL = 2000;

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

    /**
     * The minimum amount of time (in milliseconds) to wait before updating an AFK tracker.
     */
    public int MIN_AFK_UPDATE = 1000;

    /**
     * How many single chunk checks to make, before resolving the render distance fully.
     */
    public int AFK_RESOLVE_COUNT = 5;

    /**
     * The maximum amount of time (in milliseconds) to wait before updating an AFK tracker.
     */
    public int MAX_AFK_UPDATE = 60000;

    /**
     * The data directories for saving things like raw chunk records + player records.
     */
    @Ungettable
    @Unsettable
    public String MAIN_DIRECTORY = "data";
    @Ungettable
    @Unsettable
    public String RAW_DIRECTORY = "data/raw";
    @Ungettable
    @Unsettable
    public String PLAYER_DIRECTORY = "data/players";
    @Ungettable
    @Unsettable
    public String TRACKERS_DIRECTORY = "data/trackers";

    /* ------------------------ Other Fields ------------------------ */

    private final YesCom yesCom = YesCom.getInstance();

    private final File configFile;

    private long autoSaveTime;

    public ConfigHandler(String configFilePath) {
        this.configFile = Paths.get(configFilePath).toFile();

        try {
            readConfig();
        } catch (IOException error) {
            yesCom.logger.warning("Error while reading config:");
            yesCom.logger.throwing(ConfigHandler.class.getSimpleName(), "ConfigHandler", error);
        }

        autoSaveTime = System.currentTimeMillis();
    }

    @Override
    public void tick() {
        if (System.currentTimeMillis() - autoSaveTime > 180000) {
            try {
                dumpConfig();
            } catch (IOException error) {
                yesCom.logger.warning("Couldn't save config due to:");
                yesCom.logger.throwing(ConfigHandler.class.getSimpleName(), "onTick", error);
                autoSaveTime = System.currentTimeMillis() + 180000; // Let's keep increasing this to not spam console
                return;
            }

            autoSaveTime = System.currentTimeMillis();
        }
    }

    @Override
    public void exit() {
    }

    /* ----------------------------- Private methods ----------------------------- */

    private DataType getDataTypeFromClass(Class<?> clazz) {
        if (Position.class.isAssignableFrom(clazz)) { // FIXME: Wow this is bad
            return DataType.POSITION;
        } else if (Angle.class.isAssignableFrom(clazz)) {
            return DataType.ANGLE;
        } else if (ChunkPosition.class.isAssignableFrom(clazz)) {
            return DataType.CHUNK_POSITION;
        } else if (String.class.isAssignableFrom(clazz)) {
            return DataType.STRING;
        } else if (int.class.isAssignableFrom(clazz)) {
            return DataType.INTEGER;
        } else if (double.class.isAssignableFrom(clazz)) {
            return DataType.FLOAT;
        } else if (boolean.class.isAssignableFrom(clazz)) {
            return DataType.BOOLEAN;
        }

        return null;
    }

    private ConfigRule getConfigRuleFromField(Field field) {
        String name = field.getName().toLowerCase(Locale.ROOT);
        Class<?> clazz = field.getType();

        boolean enumValue = false;
        DataType dataType;
        List<String> enumValues = new ArrayList<>();

        try {
            if (Enum.class.isAssignableFrom(clazz)) {
                dataType = DataType.STRING;
                enumValue = true;

                // :vomit:
                for (Enum<?> constant : ((Enum<?>)field.get(this)).getDeclaringClass().getEnumConstants())
                    enumValues.add(constant.name());
            } else {
                dataType = getDataTypeFromClass(clazz);
            }

            if (dataType != null) {
                if (!enumValue) {
                    return new ConfigRule(name, dataType);
                } else {
                    return new ConfigRule(name, enumValues);
                }
            }

        } catch (IllegalAccessException | IllegalArgumentException error) {
            yesCom.logger.warning(String.format("Couldn't get config rule for field %s.", field.getName()));
            yesCom.logger.throwing(ConfigHandler.class.getName(), "getConfigRuleFromField", error);
        }

        return null;
    }

    public Object getValueFromField(Field field) {
        try {
            Class<?> clazz = field.getType();

            if (Enum.class.isAssignableFrom(clazz)) { // Just a few special cases
                return ((Enum<?>)field.get(this)).name();
            } else if (double.class.isAssignableFrom(clazz)) {
                return (float)field.getDouble(this);
            } else {
                return field.get(this);
            }

        } catch (IllegalAccessException error) {
            yesCom.logger.warning(String.format("Couldn't get value for field %s.", field.getName()));
            yesCom.logger.throwing(ConfigHandler.class.getName(), "getValueFromField", error);
        }

        return null;
    }

    /* ----------------------------- Reading and writing ----------------------------- */

    public void readConfig() throws IOException {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
        dumperOptions.setPrettyFlow(true);

        Yaml yaml = new Yaml(dumperOptions);

        if (!configFile.exists() || configFile.isDirectory()) {
            yesCom.logger.warning("Config file does not exist, creating a new one.");
            dumpConfig();
        }

        yesCom.logger.fine("Reading config...");

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
                yesCom.logger.warning(String.format("Couldn't set field \"%s\":", name));
                yesCom.logger.throwing(ConfigHandler.class.getSimpleName(), "readConfig", error);

                requiresRewrite.set(true);
            }
        });

        yesCom.logger.fine(String.format("Done, %d fields set.", rulesSet.get()));

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

        yesCom.logger.fine("Dumping config...");

        Map<String, Object> config = new LinkedHashMap<>();

        for (Field field : ConfigHandler.class.getFields()) {
            field.setAccessible(true);
            try {
                config.put(field.getName().toLowerCase(Locale.ROOT), field.get(this));
            } catch (IllegalAccessException | IllegalArgumentException ignored) {
            }
        }

        yesCom.logger.fine(String.format("Found %d fields.", config.size()));

        OutputStream outputStream = new FileOutputStream(configFile);
        yaml.dump(config, new OutputStreamWriter(outputStream));
        outputStream.close();

        yesCom.logger.fine("Dumped config.");
    }

    /* ----------------------------- Config rules ----------------------------- */

    /**
     * @return The config rules in a serializable format.
     */
    public Map<ConfigRule, Object> getConfigRules() {
        Map<ConfigRule, Object> rules = new LinkedHashMap<>();

        for (Field field : ConfigHandler.class.getFields()) {
            field.setAccessible(true);

            ConfigRule configRule = getConfigRuleFromField(field);
            Object value = getValueFromField(field);
            if (configRule != null && value != null) rules.put(configRule, value);
        }

        return rules;
    }

    /**
     * Gets a serializable config rule given its name.
     * @param name The name of the config rule.
     * @return The config rule in a serializable format.
     */
    public ConfigRule getConfigRule(String name) {
        for (Field field : ConfigHandler.class.getFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Ungettable.class)) continue;

            if (field.getName().equalsIgnoreCase(name)) return getConfigRuleFromField(field);
        }

        return null;
    }

    public Object getConfigRuleValue(ConfigRule configRule) {
        for (Field field : ConfigHandler.class.getFields()) {
            field.setAccessible(true);

            if (field.getName().equalsIgnoreCase(configRule.getName())) return getValueFromField(field);
        }

        return null;
    }

    public Object getConfigRuleValue(String name) {
        return getConfigRuleValue(getConfigRule(name));
    }

    /**
     * Sets a config rule to a certain value.
     * @param configRule The rule to set.
     * @param value The value to set the rule to.
     * @throws IllegalArgumentException If the given value is not valid for the rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setConfigRuleValue(ConfigRule configRule, Object value) throws IllegalArgumentException {
        for (Field field : ConfigHandler.class.getFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Unsettable.class)) continue; // Throw an exception instead?

            if (field.getName().equalsIgnoreCase(configRule.getName())) {
                Class<?> clazz = field.getType();

                try {
                    if (Enum.class.isAssignableFrom(clazz)) {
                        field.set(this, Enum.valueOf((Class<? extends Enum>)clazz, value.toString()));
                    } else if (double.class.isAssignableFrom(clazz)) {
                        field.setDouble(this, ((Float)value).doubleValue());
                    } else {
                        field.set(this, value);
                    }

                    return;

                } catch (Exception error) {
                    yesCom.logger.warning(String.format("Couldn't set config rule value for field %s.", field.getName()));
                    yesCom.logger.throwing(ConfigHandler.class.getSimpleName(), "setConfigRuleValue", error);
                    throw new IllegalArgumentException(error.getMessage());
                }
            }
        }

        throw new IllegalArgumentException("No such config rule.");
    }

    /* ----------------------------- Setters and getters ----------------------------- */

    public File getConfigFile() {
        return configFile;
    }

    /* ----------------------------- Classes ----------------------------- */

    /**
     * Used to denote that a field cannot be obtained reflectively (from the server/viewers).
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ungettable {
    }

    /**
     * Used to denote that a field cannot be updated reflectively.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Unsettable {
    }

    /**
     * Can't believe I'm making this class, but it's for client compatibility if you're wondering.
     */
    public static class ConfigRule {

        private final List<String> enumValues = new ArrayList<>();

        private final String name;
        private final DataType dataType;
        private final boolean enumValue;

        public ConfigRule(String name, DataType dataType) {
            this.name = name;
            this.dataType = dataType;

            enumValue = false;
        }

        public ConfigRule(String name, List<String> enumValues) {
            this.name = name;
            this.enumValues.addAll(enumValues);

            dataType = DataType.STRING; // We'll assume it's a string cos I'm lazy
            enumValue = true;
        }

        @Override
        public String toString() {
            return String.format("ConfigRule(name=%s, dataType=%s)", name, dataType);
        }

        public List<String> getEnumValues() {
            return enumValues;
        }

        public String getName() {
            return name;
        }

        public DataType getDataType() {
            return dataType;
        }

        public boolean isEnumValue() {
            return enumValue;
        }
    }
}
