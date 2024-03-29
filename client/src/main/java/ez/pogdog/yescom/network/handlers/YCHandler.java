package ez.pogdog.yescom.network.handlers;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.ChatMessage;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.handlers.ConfigHandler;
import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.network.packets.*;
import ez.pogdog.yescom.task.ITask;
import ez.pogdog.yescom.data.serializable.ChunkState;
import ez.pogdog.yescom.task.TaskRegistry;
import ez.pogdog.yescom.tracking.ITracker;
import me.iska.jclient.network.Connection;
import me.iska.jclient.network.handler.IHandler;
import me.iska.jclient.network.packet.Packet;
import me.iska.jclient.network.packet.Registry;

import javax.crypto.Cipher;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class YCHandler implements IHandler, ez.pogdog.yescom.handlers.IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final Deque<Packet> highPriorityPackets = new ArrayDeque<>();
    private final Deque<Packet> mediumPriorityPackets = new ArrayDeque<>();
    private final Deque<Packet> lowPriorityPackets = new ArrayDeque<>();

    private final List<ChatMessage> queuedChatMessages = new ArrayList<>();
    private final List<ChunkState> queuedChunkStates = new ArrayList<>();
    private final List<TrackedPlayer> queuedTrackedPlayers = new ArrayList<>();

    private final List<UUID> syncedOnlinePlayers = new ArrayList<>();
    private final Map<UUID, String> queuedJoins = new HashMap<>();
    private final List<UUID> queuedLeaves = new ArrayList<>();

    private byte[] handlerHash;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    private final Connection connection;
    private final String handlerName;

    private boolean initializing;
    private boolean initialized;
    private boolean synced;

    public YCHandler(Connection connection, String handlerName) {
        this.connection = connection;
        this.handlerName = handlerName;

        try {
            loadIdentityData();
        } catch (IOException error) {
            yesCom.logger.warning("An error occurred while loading identity data:");
            yesCom.logger.throwing(YCHandler.class.getName(), "YCHandler", error);
        }

        initializing = false;
        initialized = false;
        synced = false;
    }

    @Override
    public void handlePacket(Packet packet) {
        if (packet instanceof YCInitResponsePacket) {
            if (!initializing && initialized) {
                connection.exit("Got init response when already initialized.");
                yesCom.exit();
                return;
            }

            YCInitResponsePacket initResponse = (YCInitResponsePacket)packet;

            if (initResponse.isExtendedInit()) {
                yesCom.logger.fine("Extended init, verifying identity.");
                byte[] signature = initResponse.getSignature();
                byte[] nonce;

                try {
                    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
                    cipher.init(Cipher.DECRYPT_MODE, privateKey);
                    nonce = cipher.doFinal(signature);

                } catch (GeneralSecurityException error) {
                    yesCom.logger.severe("Couldn't verify identity due to error:");
                    yesCom.logger.throwing(YCHandler.class.getName(), "handlePacket", error);

                    yesCom.exit();
                    return;
                }

                connection.sendPacket(new YCExtendedResponsePacket(nonce));
                yesCom.logger.fine("Waiting for response...");

            } else {
                initializing = false;
                initialized = true;

                if (initResponse.isRejected()) {
                    connection.exit(String.format("Rejected for: %s.", initResponse.getMessage()));
                    yesCom.exit();
                    return;
                }

                yesCom.logger.fine(String.format("We are handler ID %d.", initResponse.getHandlerID()));

                yesCom.logger.info("Successfully initialized YC connection.");
                yesCom.logger.info("Syncing with server...");

                yesCom.logger.finer("Syncing config rules...");
                connection.sendPacket(new ConfigSyncPacket(yesCom.configHandler.getConfigRules()));
                yesCom.logger.finer("Done.");

                yesCom.logger.finer("Syncing registered tasks...");
                connection.sendPacket(new TaskSyncPacket(TaskRegistry.REGISTERED_TASKS));
                yesCom.logger.finer("Done.");

                yesCom.logger.finer("Syncing active tasks...");
                yesCom.taskHandler.getTasks().forEach(this::onTaskAdded);
                yesCom.logger.finer("Done.");

                yesCom.logger.finer("Syncing players...");
                yesCom.accountHandler.getAccounts().forEach(authService -> onPlayerAdded(authService.getUsername(),
                        authService.getSelectedProfile().getId(), authService.getSelectedProfile().getName()));
                yesCom.connectionHandler.getPlayers().forEach(player -> {
                    if (player.isConnected()) {
                        onPlayerLogin(player);
                        // onPlayerToggleCanLogin(player);
                        onPositionChanged(player);
                        onDimensionChanged(player);
                        onHealthChanged(player);
                    }
                });
                yesCom.logger.finer("Done.");

                yesCom.logger.finer("Syncing trackers...");
                // yesCom.trackingHandler.getTrackers().forEach(this::onTrackerAdded);
                yesCom.logger.finer("Done.");

                yesCom.logger.finer("Syncing online players...");
                yesCom.connectionHandler.getOnlinePlayers().forEach(this::onPlayerJoin);
                yesCom.logger.finer("Done.");

                synced = true;

                yesCom.logger.info("Synced.");
            }

        } else if (packet instanceof ConfigActionPacket) {
            ConfigActionPacket configAction = (ConfigActionPacket)packet;

            yesCom.logger.fine("Got config action.");

            switch (configAction.getAction()) {
                case SET_RULE: {
                    // Sneaky mfs tryna crash the server with differently typed config rules
                    ConfigHandler.ConfigRule rule = yesCom.configHandler.getConfigRule(configAction.getRule().getName());
                    Object value = configAction.getValue();

                    if (rule == null) {
                        connection.sendPacket(new ActionResponsePacket(configAction.getActionID(), false, "Rule not found."));
                        return;
                    }

                    yesCom.logger.fine(String.format("Setting rule %s to %s.", rule, value));

                    try {
                        yesCom.configHandler.setConfigRuleValue(rule, value);
                    } catch (IllegalArgumentException error) {
                        connection.sendPacket(new ActionResponsePacket(configAction.getActionID(), false, error.getMessage()));
                        return;
                    }

                    // Broadcast to everyone that the rule has been updated
                    connection.sendPacket(new ConfigActionPacket(ConfigActionPacket.Action.SYNC_RULE, rule, value));
                    connection.sendPacket(new ActionResponsePacket(configAction.getActionID(), true, "Rule updated."));
                    break;
                }
                case GET_RULE: {
                    ConfigHandler.ConfigRule rule = yesCom.configHandler.getConfigRule(configAction.getRuleName());
                    if (rule == null) {
                        connection.sendPacket(new ActionResponsePacket(configAction.getActionID(), false, "Rule not found."));
                        return;
                    }

                    Object value = yesCom.configHandler.getConfigRuleValue(rule);
                    if (value == null) { // Yeah what why would this happen except error on my behalf
                        connection.sendPacket(new ActionResponsePacket(configAction.getActionID(), false,
                                "Rule value is null for some reason lol, I guess I messed something up."));
                        return;
                    }

                    connection.sendPacket(new ConfigActionPacket(ConfigActionPacket.Action.SYNC_RULE, configAction.getActionID(), rule, value));
                    connection.sendPacket(new ActionResponsePacket(configAction.getActionID(), true, "Rule synced."));
                    break;
                }
            }

        } else if (packet instanceof TaskActionPacket) {
            TaskActionPacket taskAction = (TaskActionPacket)packet;

            yesCom.logger.fine("Got task action.");

            switch (taskAction.getAction()) {
                case START: {
                    TaskRegistry.RegisteredTask registeredTask = TaskRegistry.getTask(taskAction.getTaskName());

                    if (registeredTask != null) {
                        ITask task;
                        try {
                            Constructor<? extends ITask> constructor = registeredTask.getTaskClazz().getConstructor(
                                    registeredTask.getParamDescriptions().stream()
                                            .map(paramDescription -> {
                                                switch (paramDescription.getInputType()) {
                                                    default:
                                                    case SINGULAR: {
                                                        return paramDescription.getDataType().getClazz();
                                                    }
                                                    case ARRAY: {
                                                        return List.class;
                                                    }
                                                }
                                            })
                                            .toArray(Class[]::new));
                            task = constructor.newInstance(taskAction.getTaskParameters().stream().map(parameter -> {
                                switch (parameter.getParamDescription().getInputType()) {
                                    default:
                                    case SINGULAR: {
                                        return parameter.getValue();
                                    }
                                    case ARRAY: {
                                        return parameter.getValues();
                                    }
                                }
                            }).toArray());

                        } catch (Exception error) {
                            yesCom.logger.warning(String.format("Couldn't instantiate task %s.", registeredTask));
                            yesCom.logger.throwing(YCHandler.class.getSimpleName(), "handlePacket", error);

                            if (error instanceof InvocationTargetException) {;
                                connection.sendPacket(new ActionResponsePacket(taskAction.getActionID(), false,
                                        ((InvocationTargetException)error).getTargetException().getMessage()));
                            } else {
                                connection.sendPacket(new ActionResponsePacket(taskAction.getActionID(), false, error.getMessage()));
                            }
                            return;
                        }

                        yesCom.taskHandler.addTask(task);
                        connection.sendPacket(new ActionResponsePacket(taskAction.getActionID(), true, "Task started."));

                    } else {
                        connection.sendPacket(new ActionResponsePacket(taskAction.getActionID(), false, "Task not found."));
                    }
                    break;
                }
                case STOP: {
                    ITask task = yesCom.taskHandler.getTask(taskAction.getTaskID());
                    if (task == null) {
                        connection.sendPacket(new ActionResponsePacket(taskAction.getActionID(), false, "Task not found."));
                        return;
                    }

                    yesCom.taskHandler.removeTask(task);
                    connection.sendPacket(new ActionResponsePacket(taskAction.getActionID(), true, "Task stopped."));
                    break;
                }
            }

        } else if (packet instanceof AccountActionPacket) {
            AccountActionPacket accountAction = (AccountActionPacket)packet;

            yesCom.logger.fine("Got account action.");

            switch (accountAction.getAction()) {
                case LOGIN: {
                    boolean success;
                    String message;

                    try {
                        if (accountAction.isLegacy()) {
                            yesCom.accountHandler.legacyLogin(accountAction.getUsername(), accountAction.getPassword());
                        } else {
                            yesCom.accountHandler.newLogin(accountAction.getUsername(), accountAction.getAccessToken(), accountAction.getClientToken());
                        }

                        success = true;
                        message = "Successfully logged in.";
                    } catch (RequestException error) {
                        yesCom.logger.warning(String.format("Error while logging in account: %s.", accountAction.getUsername()));
                        yesCom.logger.throwing(YCHandler.class.getSimpleName(), "handlePacket", error);

                        success = false;
                        message = error.getMessage();
                    }

                    connection.sendPacket(new ActionResponsePacket(accountAction.getActionID(), success, message));
                    break;
                }
                case LOGOUT: {
                    if (yesCom.accountHandler.getAccount(accountAction.getUsername()) == null) {
                        connection.sendPacket(new ActionResponsePacket(accountAction.getActionID(), false, "Account not found."));
                    } else {
                        yesCom.accountHandler.logout(accountAction.getUsername());
                        connection.sendPacket(new ActionResponsePacket(accountAction.getActionID(), true, "Successfully logged out."));
                    }
                    break;
                }
            }

        } else if (packet instanceof TrackerActionPacket) {
            TrackerActionPacket trackerAction = (TrackerActionPacket)packet;

            yesCom.logger.fine("Got tracker action.");

            if (trackerAction.getAction() == TrackerActionPacket.Action.UNTRACK) {
                ITracker tracker = null; // yesCom.trackingHandler.getTracker(trackerAction.getTrackerID());
                if (tracker == null) {
                    connection.sendPacket(new ActionResponsePacket(trackerAction.getActionID(), false, "Tracker not found."));
                    return;
                }

                yesCom.logger.fine(String.format("Untracking %s.", tracker));
                // yesCom.trackingHandler.untrack(tracker);
                // Lol great message copilot
                connection.sendPacket(new ActionResponsePacket(trackerAction.getActionID(), true, "Tracker untracked."));
            }

        } else if (packet instanceof ActionRequestPacket) {
            ActionRequestPacket actionRequest = (ActionRequestPacket)packet;

            ByteArrayInputStream inputStream = new ByteArrayInputStream(actionRequest.getData());

            switch (actionRequest.getAction()) { // It'll prolly have more stuff in the future
                case TOGGLE_LOGIN: {
                    String username;
                    boolean canLogin;
                    try {
                        username = Registry.STRING.read(inputStream);
                        canLogin = Registry.BOOLEAN.read(inputStream);
                    } catch (IOException error) {
                        yesCom.logger.finer("Error while reading toggle login action.");
                        yesCom.logger.throwing(YCHandler.class.getSimpleName(), "handlePacket", error);
                        connection.sendPacket(new ActionResponsePacket(actionRequest.getActionID(), false,
                                "Invalid format toggle login action."));
                        return;
                    }

                    Player player = yesCom.connectionHandler.getPlayer(username);
                    if (player == null) {
                        yesCom.logger.finer(String.format("Attempted to toggle login for invalid username: %s.", username));
                        connection.sendPacket(new ActionResponsePacket(actionRequest.getActionID(), false,
                                "Player not found."));
                        return;
                    }

                    player.setCanLogin(canLogin);
                    connection.sendPacket(new ActionResponsePacket(actionRequest.getActionID(), true,
                            "Successfully toggled login."));
                    break;
                }
                case SEND_CHAT_MESSAGE: {
                    String username;
                    String message;
                    try {
                        username = Registry.STRING.read(inputStream);
                        message = Registry.STRING.read(inputStream);
                    } catch (IOException error) {
                        yesCom.logger.finer("Error while reading chat message action.");
                        yesCom.logger.throwing(YCHandler.class.getSimpleName(), "handlePacket", error);
                        connection.sendPacket(new ActionResponsePacket(actionRequest.getActionID(), false,
                                "Invalid format for chat message."));
                        return;
                    }

                    Player player = yesCom.connectionHandler.getPlayer(username);
                    if (player == null) {
                        yesCom.logger.finer(String.format("Attempted to send chat message with invalid username: %s.", username));
                        connection.sendPacket(new ActionResponsePacket(actionRequest.getActionID(), false,
                                "Player not found."));
                        return;

                    } else if (!player.isConnected()) {
                        yesCom.logger.finer(String.format("Attempted to send chat message with offline player: %s.", player));
                        connection.sendPacket(new ActionResponsePacket(actionRequest.getActionID(), false,
                                "Player is offline."));
                        return;
                    }

                    player.sendChatMessage(message);
                    connection.sendPacket(new ActionResponsePacket(actionRequest.getActionID(), true,
                            "Successfully sent chat message."));
                    break;
                }
                case UNTRACK_PLAYER: {
                    // This is here cos untracking trackers removes the entire tracker, but we want a way to untrack
                    // just the tracked player too
                    BigInteger trackedPlayerID;
                    try {
                        trackedPlayerID = Registry.VAR_INTEGER.read(inputStream);
                    } catch (IOException error) {
                        yesCom.logger.finer("Error while reading untrack player ID.");
                        yesCom.logger.throwing(YCHandler.class.getSimpleName(), "handlePacket", error);
                        connection.sendPacket(new ActionResponsePacket(actionRequest.getActionID(), false,
                                "Invalid format for untracking a player."));
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void exit(String reason) {
        yesCom.restartConnection();
    }

    @Override
    public synchronized void tick() {
        if (!connection.isConnected() || !initialized || !synced) return;

        if (!syncedOnlinePlayers.isEmpty() && !yesCom.connectionHandler.isConnected()) {
            new ArrayList<>(syncedOnlinePlayers).forEach(this::onPlayerLeave);
            syncedOnlinePlayers.clear();
        }

        // Send the general queued packets
        while (!highPriorityPackets.isEmpty()) connection.sendPacket(highPriorityPackets.pop());
        while (!mediumPriorityPackets.isEmpty()) connection.sendPacket(mediumPriorityPackets.pop());
        while (!lowPriorityPackets.isEmpty()) connection.sendPacket(lowPriorityPackets.pop());

        // Broadcast new chat messages
        if (!queuedChatMessages.isEmpty()) {
            connection.sendPacket(new DataExchangePacket(DataExchangePacket.DataType.CHAT, -1, queuedChatMessages,
                    new ArrayList<>()));
            queuedChatMessages.clear();
        }

        // Broadcast new chunk states
        if (!queuedChunkStates.isEmpty())
            connection.sendPacket(new DataExchangePacket(DataExchangePacket.DataType.CHUNK_STATE, -1,
                    queuedChunkStates, new ArrayList<>()));
        queuedChunkStates.clear();

        // Broadcast new tracked players
        if (!queuedTrackedPlayers.isEmpty()) {
            connection.sendPacket(new DataExchangePacket(DataExchangePacket.DataType.TRACKED_PLAYER, -1,
                    queuedTrackedPlayers, new ArrayList<>()));
            queuedTrackedPlayers.clear();
        }

        // Broadcast new players joining the server
        if (!queuedJoins.isEmpty()) {
            connection.sendPacket(new OnlinePlayersActionPacket(OnlinePlayersActionPacket.Action.ADD, queuedJoins));
            queuedJoins.clear();
        }

        // Broadcast players leaving the server
        if (!queuedLeaves.isEmpty()) {
            connection.sendPacket(new OnlinePlayersActionPacket(queuedLeaves));
            queuedLeaves.clear();
        }
    }

    @Override
    public void exit() {
    }

    /* ----------------------------- Identity data ----------------------------- */

    private void loadIdentityData() throws IOException {
        yesCom.logger.fine("Loading identity data...");

        File file = Paths.get(yesCom.configHandler.IDENTITY_DIRECTORY).toFile();
        if (!file.exists() || !file.isDirectory()) {
            yesCom.logger.info("No identity data found, generating new data.");
            saveIdentityData();
        } else {
            List<String> lines = Files.readAllLines(Paths.get(file.getPath(), "hash.txt"));
            if (!lines.isEmpty()) {
                handlerHash = Base64.getDecoder().decode(lines.get(0));
            } else {
                yesCom.logger.warning("Invalid handler hash size, regenerating data...");
                saveIdentityData();
                return;
            }

            yesCom.logger.fine(String.format("Handler hash is %s.", Base64.getEncoder().encodeToString(handlerHash)));

            BufferedReader pubKeyFile = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(file.getPath(), "pubkey.pem"))));
            StringBuilder pubKeyBuilder = new StringBuilder();
            while (pubKeyFile.ready()) pubKeyBuilder.append(pubKeyFile.readLine());
            pubKeyFile.close();
            try {
                publicKey = readPublicKey(pubKeyBuilder.toString());
            } catch (GeneralSecurityException | IllegalArgumentException error) {
                yesCom.logger.warning("Invalid public key format, regenerating data:");
                yesCom.logger.throwing(YCHandler.class.getSimpleName(), "loadIdentityData", error);
                saveIdentityData();
                return;
            }

            BufferedReader privKeyFile = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(file.getPath(), "privkey.pem"))));
            StringBuilder privKeyBuilder = new StringBuilder();
            while (privKeyFile.ready()) privKeyBuilder.append(privKeyFile.readLine());
            privKeyFile.close();
            try {
                privateKey = readPrivateKey(privKeyBuilder.toString());
            } catch (GeneralSecurityException | IllegalArgumentException error) {
                yesCom.logger.warning("Invalid private key format, regenerating data:");
                yesCom.logger.throwing(YCHandler.class.getSimpleName(), "loadIdentityData", error);
                saveIdentityData();
                return;
            }

            yesCom.logger.finer("Private key:");
            yesCom.logger.finer("\n" + writePrivateKey(privateKey));
            yesCom.logger.finer("Public key:");
            yesCom.logger.finer("\n" + writePublicKey(publicKey));

            yesCom.logger.info("Successfully read identity data.");
        }
    }

    private void saveIdentityData() throws IOException {
        yesCom.logger.info("Generating handler hash and RSA keys...");

        try {
            handlerHash = MessageDigest.getInstance("SHA-256").digest(ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array());
            yesCom.logger.fine(String.format("Handler hash is %s.", Base64.getEncoder().encodeToString(handlerHash)));

            yesCom.logger.fine(String.format("Generating %d bit RSA keys...", yesCom.configHandler.RSA_KEY_SIZE));
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(yesCom.configHandler.RSA_KEY_SIZE);

            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();

            yesCom.logger.finer("Private key:");
            yesCom.logger.finer("\n" + writePrivateKey(privateKey));
            yesCom.logger.finer("Public key:");
            yesCom.logger.finer("\n" + writePublicKey(publicKey));

            yesCom.logger.info("Do NOT share the private key (duh).");

        } catch (NoSuchAlgorithmException error) {
            yesCom.logger.severe("An error occurred while generating the data:");
            yesCom.logger.throwing(YCHandler.class.getSimpleName(), "saveIdentityData", error);

            yesCom.exit();
            return;
        }

        yesCom.logger.info("Keys successfully generated, writing...");

        File file = Paths.get(yesCom.configHandler.IDENTITY_DIRECTORY).toFile();
        if (!file.exists() && !file.mkdir()) throw new IOException("Couldn't write due to unknown.");

        OutputStream hashFile = Files.newOutputStream(Paths.get(file.getPath(), "hash.txt"));
        hashFile.write(Base64.getEncoder().encode(handlerHash));
        hashFile.close();

        OutputStream pubKeyFile = Files.newOutputStream(Paths.get(file.getPath(), "pubkey.pem"));
        pubKeyFile.write(writePublicKey(publicKey).getBytes(StandardCharsets.UTF_8));
        pubKeyFile.close();

        OutputStream privKeyFile = Files.newOutputStream(Paths.get(file.getPath(), "privkey.pem"));
        privKeyFile.write(writePrivateKey(privateKey).getBytes(StandardCharsets.UTF_8));
        privKeyFile.close();

        yesCom.logger.info("Done!");
    }

    private PrivateKey readPrivateKey(String data) throws GeneralSecurityException {
        String privateKeyPEM = data
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END RSA PRIVATE KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
    }

    private String writePrivateKey(PrivateKey privateKey) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("-----BEGIN RSA PRIVATE KEY-----");

        String base64Key = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        for (int index = 0; index < base64Key.length(); ++index) {
            if (index % 64 == 0) keyBuilder.append(System.lineSeparator());
            keyBuilder.append(base64Key.charAt(index));
        }

        keyBuilder.append(System.lineSeparator()).append("-----END RSA PRIVATE KEY-----");

        return keyBuilder.toString();
    }

    private PublicKey readPublicKey(String data) throws GeneralSecurityException {
        String publicKeyPEM = data
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PUBLIC KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return keyFactory.generatePublic(keySpec);
    }

    private String writePublicKey(PublicKey publicKey) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("-----BEGIN PUBLIC KEY-----");

        String base64Key = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        for (int index = 0; index < base64Key.length(); ++index) {
            if (index % 64 == 0) keyBuilder.append(System.lineSeparator());
            keyBuilder.append(base64Key.charAt(index));
        }

        keyBuilder.append(System.lineSeparator()).append("-----END PUBLIC KEY-----");

        return keyBuilder.toString();
    }

    /* ----------------------------- Connection stuff ----------------------------- */

    /**
     * Attempts to start a YC connection to the server, if not already initialized.
     */
    public void initConnection() {
        if (!initialized && !initializing) {
            yesCom.logger.fine("Initializing YC connection...");

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey.getEncoded());

            connection.sendPacket(new YCInitRequestPacket(YCInitRequestPacket.ClientType.REPORTING, handlerHash,
                    keySpec.getEncoded(), handlerName, yesCom.connectionHandler.getHost(), yesCom.connectionHandler.getPort()));
            initializing = true;
        }
    }

    /* ----------------------------- Task Events ----------------------------- */

    /**
     * Called when a task is added to the task list.
     * @param task The task that was added.
     */
    public synchronized void onTaskAdded(ITask task) {
        highPriorityPackets.add(new TaskActionPacket(TaskActionPacket.Action.ADD, task));
    }

    /**
     * Called when a task is removed from the task list.
     * @param task The task that was removed.
     */
    public synchronized void onTaskRemoved(ITask task) {
        lowPriorityPackets.add(new TaskActionPacket(TaskActionPacket.Action.REMOVE, task));
    }

    /**
     * Called when a task is updated.
     * @param task The task that was updated.
     */
    public synchronized void onTaskUpdate(ITask task) {
        mediumPriorityPackets.add(new TaskActionPacket(TaskActionPacket.Action.UPDATE, task));
    }

    /**
     * Called when a task has produced a new result.
     * @param task The task that produced the result.
     * @param result The result produced by the task.
     */
    public synchronized void onTaskResult(ITask task, Object result) {
        // mediumPriorityPackets.add(new TaskActionPacket(task, result));
    }

    /* ----------------------------- Player Events ----------------------------- */

    /**
     * Called when a player is added.
     * @param username The username of the player.
     * @param uuid The UUID of the player.
     * @param displayName The display name of the player.
     */
    public synchronized void onPlayerAdded(String username, UUID uuid, String displayName) {
        highPriorityPackets.addFirst(new PlayerActionPacket(username, uuid, displayName));
    }

    /**
     * Called when a player is removed
     * @param username The username of the player that was removed.
     */
    public synchronized void onPlayerRemoved(String username) {
        lowPriorityPackets.addLast(new PlayerActionPacket(username));
    }

    public synchronized void onPlayerToggleCanLogin(Player player) {
        mediumPriorityPackets.addLast(new PlayerActionPacket(PlayerActionPacket.Action.TOGGLE_LOGIN, player));
    }

    /**
     * Called when a player logs in to the server.
     * @param player The player that logged in.
     */
    public synchronized void onPlayerLogin(Player player) {
        highPriorityPackets.addLast(new PlayerActionPacket(PlayerActionPacket.Action.LOGIN, player));
    }

    /**
     * Called when a player logs out of the server.
     * @param player The player.
     * @param reason The reason the player logged out.
     */
    public synchronized void onPlayerLogout(Player player, String reason) {
        lowPriorityPackets.addFirst(new PlayerActionPacket(player, reason));
    }

    /**
     * Called when a player's position is changed.
     * @param player The player.
     */
    public synchronized void onPositionChanged(Player player) {
        mediumPriorityPackets.add(new PlayerActionPacket(PlayerActionPacket.Action.UPDATE_POSITION, player));
    }

    /**
     * Called when a player's dimension is changed.
     * @param player The player.
     */
    public synchronized void onDimensionChanged(Player player) {
        mediumPriorityPackets.add(new PlayerActionPacket(PlayerActionPacket.Action.UPDATE_DIMENSION, player));
    }

    /**
     * Called when a player's health is changed.
     * @param player The player
     */
    public synchronized void onHealthChanged(Player player) {
        mediumPriorityPackets.add(new PlayerActionPacket(PlayerActionPacket.Action.UPDATE_HEALTH, player));
    }

    /* ----------------------------- Chunk Events ----------------------------- */

    /**
     * Called when a chunk state is found.
     * @param chunkState The chunk state.
     */
    public synchronized void onChunkState(ChunkState chunkState) {
        if (yesCom.configHandler.BROADCAST_CHUNK_STATES) queuedChunkStates.add(chunkState);
    }

    /* ----------------------------- Tracked Player Events ----------------------------- */

    /**
     * Called when a tracked player is found.
     * @param trackedPlayer The tracked player.
     */
    public synchronized void onTrackedPlayer(TrackedPlayer trackedPlayer) {
        if (yesCom.configHandler.BROADCAST_TRACKED_PLAYERS) queuedTrackedPlayers.add(trackedPlayer);
    }

    /* ----------------------------- Tracker Events ----------------------------- */

    /**
     * Called when a tracker is added.
     * @param tracker The tracker that was added.
     */
    public synchronized void onTrackerAdded(ITracker tracker) {
        highPriorityPackets.add(new TrackerActionPacket(TrackerActionPacket.Action.ADD, tracker));
    }

    /**
     * Called when a tracker is removed.
     * @param tracker The tracker that was removed.
     */
    public synchronized void onTrackerRemoved(ITracker tracker) {
        lowPriorityPackets.add(new TrackerActionPacket(TrackerActionPacket.Action.REMOVE, tracker));
    }

    /**
     * Called when a tracker is updated.
     * @param tracker The tracker that was updated.
     */
    public synchronized void onTrackerUpdate(ITracker tracker) {
        mediumPriorityPackets.add(new TrackerActionPacket(TrackerActionPacket.Action.UPDATE, tracker));
    }

    /* ----------------------------- Info Events ----------------------------- */

    public synchronized void onLogMessage(String logMessage) {

    }

    /**
     * Called when a chat message is received.
     * @param chatMessage The chat message.
     */
    public synchronized void onChatMessage(ChatMessage chatMessage) {
        queuedChatMessages.add(chatMessage);
    }

    /**
     * Called when updating the current information.
     * @param waitingQueries The number of queries waiting to be processed.
     * @param tickingQueries The number of queries that are currently being processed.
     * @param queryRate The number of queries that are being processed per second.
     * @param tickRate The tick rate of the server.
     * @param serverPing The server ping.
     * @param timeSinceLastPacket The time since the last packet was received.
     */
    public synchronized void onInfoUpdate(int waitingQueries, int tickingQueries, float queryRate, float droppedQueries,
                                          float tickRate, float serverPing, int timeSinceLastPacket) {
        mediumPriorityPackets.add(new InfoUpdatePacket(waitingQueries, tickingQueries, queryRate, droppedQueries, tickRate,
                serverPing, timeSinceLastPacket));
    }

    /**
     * Called when updating the current information, but no server data is available (cos we aren't online).
     * @param waitingQueries The number of queries waiting to be processed.
     * @param tickingQueries The number of queries that are currently being processed.
     * @param queryRate The number of queries that are being processed per second.
     */
    public synchronized void onInfoUpdate(int waitingQueries, int tickingQueries, float queryRate, float droppedQueries) {
        mediumPriorityPackets.add(new InfoUpdatePacket(waitingQueries, tickingQueries, queryRate, droppedQueries));
    }

    /**
     * Called when a player joins the server.
     * @param uuid The UUID of the player that joined.
     * @param name The display name of the player that joined.
     */
    public synchronized void onPlayerJoin(UUID uuid, String name) {
        if (!syncedOnlinePlayers.contains(uuid)) {
            syncedOnlinePlayers.add(uuid);
            queuedJoins.put(uuid, name);
            queuedLeaves.remove(uuid);
        }
    }

    /**
     * Called when a player leaves the server.
     * @param uuid The UUID of the player that left.
     */
    public synchronized void onPlayerLeave(UUID uuid) {
        if (syncedOnlinePlayers.contains(uuid)) {
            syncedOnlinePlayers.remove(uuid);
            if (!queuedLeaves.contains(uuid)) queuedLeaves.add(uuid);
            queuedJoins.remove(uuid);
        }
    }

    /* ----------------------------- Setters and Getters ----------------------------- */

    /**
     * @return Whether or not we have initialized a YC connection with the serve.r
     */
    public boolean isInitialized() {
        return initialized || initializing;
    }

    /**
     * @return Whether or not we have synchronized our data with the server.
     */
    public boolean isSynced() {
        return synced;
    }
}
