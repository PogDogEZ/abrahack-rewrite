package ez.pogdog.yescom.jclient.handlers;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.handlers.connection.Player;
import ez.pogdog.yescom.jclient.packets.*;
import ez.pogdog.yescom.task.ILoadedChunkTask;
import ez.pogdog.yescom.task.ITask;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.data.serializable.ChunkState;
import ez.pogdog.yescom.tracking.ITracker;
import me.iska.jclient.network.Connection;
import me.iska.jclient.network.handler.IHandler;
import me.iska.jclient.network.packet.Packet;

import javax.crypto.Cipher;
import java.io.*;
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
import java.util.function.BiConsumer;

public class YCHandler implements IHandler, ez.pogdog.yescom.handlers.IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final Deque<Packet> queuedPackets = new ArrayDeque<>();
    private final List<ChunkState> queuedChunkStates = new ArrayList<>();

    private final List<UUID> syncedOnlinePlayers = new ArrayList<>();
    private final Map<UUID, String> queuedJoins = new HashMap<>();
    private final List<UUID> queuedLeaves = new ArrayList<>();

    private final Map<Integer, byte[]> dataParts = new HashMap<>();

    private byte[] handlerHash;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    private final Connection connection;
    private final String handlerName;

    private boolean initializing;
    private boolean initialized;
    private boolean synced;

    private ByteArrayOutputStream currentUploadData; // Data that has been requested by the server

    private BiConsumer<Boolean, Map<Integer, byte[]>> downloadCallBack;
    private boolean downloading;
    private int chunkSize;
    private int expectedParts;

    private long lastChunkStatesUpdate;

    public YCHandler(Connection connection, String handlerName) {
        this.connection = connection;
        this.handlerName = handlerName;

        try {
            loadIdentityData();
        } catch (IOException error) {
            yesCom.logger.warn("An error occurred while loading identity data:");
            yesCom.logger.error(error.toString());
        }

        initializing = false;
        initialized = false;
        synced = false;

        currentUploadData = null;

        downloading = false;
        chunkSize = 65536;
        expectedParts = 0;

        lastChunkStatesUpdate = System.currentTimeMillis();
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
                yesCom.logger.debug("Extended init, verifying identity.");
                byte[] signature = initResponse.getSignature();
                byte[] nonce;

                try {
                    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
                    cipher.init(Cipher.DECRYPT_MODE, privateKey);
                    nonce = cipher.doFinal(signature);

                } catch (GeneralSecurityException error) {
                    yesCom.logger.fatal("Couldn't verify identity due to error:");
                    yesCom.logger.error(error.toString());

                    yesCom.exit();
                    return;
                }

                connection.sendPacket(new YCExtendedResponsePacket(nonce));
                yesCom.logger.debug("Waiting for response...");

            } else {
                initializing = false;
                initialized = true;

                if (initResponse.isRejected()) {
                    connection.exit(String.format("Rejected for: %s.", initResponse.getMessage()));
                    yesCom.exit();
                    return;
                }

                yesCom.logger.debug(String.format("We are handler ID %d.", initResponse.getHandlerID()));

                yesCom.logger.info("Successfully initialized YC connection.");
                yesCom.logger.info("Syncing with server...");

                yesCom.logger.debug("Syncing registered tasks...");
                connection.sendPacket(new TaskSyncPacket());
                yesCom.logger.debug("Done.");

                yesCom.logger.debug("Syncing active tasks...");
                yesCom.getTasks().forEach(this::onTaskAdded);
                yesCom.logger.debug("Done.");

                yesCom.logger.debug("Syncing players...");
                yesCom.connectionHandler.getPlayers().forEach(this::onPlayerAdded);
                yesCom.logger.debug("Done.");

                yesCom.logger.debug("Syncing trackers...");
                yesCom.trackingHandler.getTrackers().forEach(this::onTrackerAdded);
                yesCom.logger.debug("Done.");

                yesCom.logger.debug("Syncing online players...");
                yesCom.connectionHandler.getOnlinePlayers().forEach(this::onPlayerJoin);
                yesCom.logger.debug("Done.");

                yesCom.logger.info("Synced.");
            }

        } else if (packet instanceof DataRequestPacket) {
            DataRequestPacket dataRequest = (DataRequestPacket)packet;

            switch (dataRequest.getRequestType()) {
                case DOWNLOAD: {
                    DataResponsePacket dataResponse;

                    if (currentUploadData != null) {
                        yesCom.logger.warn("Concurrent data request!");
                        dataResponse = new DataResponsePacket();

                    } else {
                        List<BigInteger> invalidIDs = new ArrayList<>();
                        List<Object> fetchedData = new ArrayList<>();

                        switch (dataRequest.getDataType()) {
                            case CHUNK_STATE: {
                                for (BigInteger dataID : dataRequest.getDataIDs()) {
                                    ChunkState chunkState = yesCom.dataHandler.getChunkState(dataID);

                                    if (chunkState == null) {
                                        invalidIDs.add(dataID);
                                        break; // We don't need to check anymore since we know it's invalid
                                    } else {
                                        fetchedData.add(chunkState);
                                    }
                                }
                                break;
                            }
                            case RENDER_DISTANCE: {
                                for (BigInteger dataID : dataRequest.getDataIDs()) {
                                    RenderDistance renderDistance = yesCom.dataHandler.getRenderDistance(dataID);

                                    if (renderDistance == null) {
                                        invalidIDs.add(dataID);
                                        break;
                                    } else {
                                        fetchedData.add(renderDistance);
                                    }
                                }
                                break;
                            }
                            case TRACKED_PLAYER: {
                                for (BigInteger dataID : dataRequest.getDataIDs()) {
                                    TrackedPlayer trackedPlayer = yesCom.dataHandler.getTrackedPlayer(dataID);

                                    if (trackedPlayer == null) {
                                        invalidIDs.add(dataID);
                                        break;
                                    } else {
                                        fetchedData.add(trackedPlayer);
                                    }
                                }
                                break;
                            }
                            case ONLINE_PLAYER: {
                                break;
                            }
                        }

                        if (!invalidIDs.isEmpty()) {
                            dataResponse = new DataResponsePacket(new ArrayList<>(), invalidIDs);
                        } else {
                            currentUploadData = new ByteArrayOutputStream(); // TODO: Write data
                            dataResponse = new DataResponsePacket();
                        }
                    }

                    connection.sendPacket(dataResponse);
                    break;
                }
                case UPLOAD: {
                    yesCom.logger.warn("Server requested to upload data!");
                    connection.sendPacket(new DataResponsePacket());
                    break;
                }
                case CANCEL: {
                    yesCom.logger.debug("Cancelled current data upload.");
                    currentUploadData = null;
                    break;
                }
            }

        } else if (packet instanceof DataResponsePacket) {
            DataResponsePacket dataResponse = (DataResponsePacket)packet;

            if (dataResponse.isValid()) {
                chunkSize = 0;
                expectedParts = 0;

            } else {
                if (downloadCallBack != null) downloadCallBack.accept(false, new HashMap<>());
                downloading = false;
                dataParts.clear();
            }

        } else if (packet instanceof DataPartPacket) {
            DataPartPacket dataPart = (DataPartPacket)packet;

            dataParts.put(dataPart.getDataPart(), dataPart.getData());
            if (dataParts.size() >= expectedParts) {
                if (downloadCallBack != null) downloadCallBack.accept(true, new HashMap<>(dataParts));
                downloading = false;
            }

        } else if (packet instanceof TaskActionPacket) {
            TaskActionPacket taskAction = (TaskActionPacket)packet;

            yesCom.logger.debug("Got task action.");

            switch (taskAction.getAction()) {
                case START: {
                    yesCom.addTask(taskAction.getTask());
                    break;
                }
                case STOP: {
                    yesCom.removeTask(taskAction.getTaskID());
                    break;
                }
            }

        } else if (packet instanceof AccountActionPacket) {
            AccountActionPacket accountAction = (AccountActionPacket)packet;

            yesCom.logger.debug("Got account action.");

            switch (accountAction.getAction()) {
                case ADD: {
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
                        yesCom.logger.warn(String.format("Error while logging in account: %s.", accountAction.getUsername()));
                        yesCom.logger.error(error.toString());

                        success = false;
                        message = error.getMessage();
                    }

                    connection.sendPacket(new AccountActionResponsePacket(accountAction.getActionID(), success, message));
                    break;
                }
                case REMOVE: {
                    yesCom.accountHandler.logout(accountAction.getUsername());
                    break;
                }
            }
        }
    }

    @Override
    public synchronized void onTick() {
        if (connection.isConnected() && initialized) {
            while (!queuedPackets.isEmpty()) connection.sendPacket(queuedPackets.pop());

            if (System.currentTimeMillis() - lastChunkStatesUpdate > 500) {
                lastChunkStatesUpdate = System.currentTimeMillis();

                if (!queuedChunkStates.isEmpty()) connection.sendPacket(new ChunkStatesPacket(queuedChunkStates));
                queuedChunkStates.clear();

                if (!queuedJoins.isEmpty()) connection.sendPacket(new OnlinePlayersActionPacket(OnlinePlayersActionPacket.Action.ADD, queuedJoins));
                queuedJoins.clear();

                if (!queuedLeaves.isEmpty()) connection.sendPacket(new OnlinePlayersActionPacket(queuedLeaves));
                queuedLeaves.clear();
            }
        }
    }

    @Override
    public void onExit() {
    }

    private void loadIdentityData() throws IOException {
        yesCom.logger.debug("Loading identity data...");

        File file = Paths.get(yesCom.configHandler.IDENTITY_DIRECTORY).toFile();
        if (!file.exists() || !file.isDirectory()) {
            yesCom.logger.info("No identity data found, generating new data.");
            saveIdentityData();
        } else {
            InputStream hashFile = Files.newInputStream(Paths.get(file.getPath(), "hash.txt"));
            byte[] readHandlerHash = new byte[32];
            if (hashFile.read(readHandlerHash) == 32) {
                handlerHash = Base64.getDecoder().decode(readHandlerHash);
            } else {
                yesCom.logger.warn("Invalid handler hash size, regenerating data...");
                saveIdentityData();
                return;
            }
            hashFile.close();

            yesCom.logger.debug(String.format("Handler hash is %s.", Base64.getEncoder().encodeToString(handlerHash)));

            BufferedReader pubKeyFile = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(file.getPath(), "pubkey.pem"))));
            StringBuilder pubKeyBuilder = new StringBuilder();
            while (pubKeyFile.ready()) pubKeyBuilder.append(pubKeyFile.readLine());
            pubKeyFile.close();
            try {
                publicKey = readPublicKey(pubKeyBuilder.toString());
            } catch (GeneralSecurityException | IllegalArgumentException error) {
                yesCom.logger.warn("Invalid public key format, regenerating data:");
                yesCom.logger.error(error.toString());
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
                yesCom.logger.warn("Invalid private key format, regenerating data:");
                yesCom.logger.error(error.toString());
                saveIdentityData();
                return;
            }

            yesCom.logger.debug("Private key:");
            yesCom.logger.debug("\n" + writePrivateKey(privateKey));
            yesCom.logger.debug("Public key:");
            yesCom.logger.debug("\n" + writePublicKey(publicKey));

            yesCom.logger.info("Successfully read identity data.");
        }
    }

    private void saveIdentityData() throws IOException {
        yesCom.logger.info("Generating handler hash and RSA keys...");

        try {
            handlerHash = MessageDigest.getInstance("SHA-256").digest(ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array());
            yesCom.logger.debug(String.format("Handler hash is %s.", Base64.getEncoder().encodeToString(handlerHash)));

            yesCom.logger.debug(String.format("Generating %d bit RSA keys...", yesCom.configHandler.RSA_KEY_SIZE));
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(yesCom.configHandler.RSA_KEY_SIZE);

            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();

            yesCom.logger.debug("Private key:");
            yesCom.logger.debug("\n" + writePrivateKey(privateKey));
            yesCom.logger.debug("Public key:");
            yesCom.logger.debug("\n" + writePublicKey(publicKey));

            yesCom.logger.info("Do NOT share the private key (duh).");

        } catch (NoSuchAlgorithmException error) {
            yesCom.logger.fatal("An error occurred while generating the data:");
            yesCom.logger.error(error.toString());

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

    public void initConnection() {
        yesCom.logger.debug("Initializing YC connection...");

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey.getEncoded());

        connection.sendPacket(new YCInitRequestPacket(YCInitRequestPacket.ClientType.REPORTING, handlerHash,
                keySpec.getEncoded(), handlerName, yesCom.connectionHandler.getHost(), yesCom.connectionHandler.getPort()));
        initializing = true;
    }

    public PrivateKey readPrivateKey(String data) throws GeneralSecurityException {
        String privateKeyPEM = data
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END RSA PRIVATE KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
    }

    public String writePrivateKey(PrivateKey privateKey) {
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

    public void requestDataDownload(DataRequestPacket.DataType dataType, List<BigInteger> dataIDs,
                                    BiConsumer<Boolean, Map<Integer, byte[]>> downloadCallBack) {
        if (!downloading) {
            dataParts.clear();
            this.downloadCallBack = downloadCallBack;
            downloading = true;

            connection.sendPacket(new DataRequestPacket(dataType, dataIDs));
        }
    }

    public void requestChunkStatesDownload(List<BigInteger> dataIDs, BiConsumer<Boolean, Map<Integer, byte[]>> downloadCallBack) {
        requestDataDownload(DataRequestPacket.DataType.CHUNK_STATE, dataIDs, downloadCallBack);
    }

    public void requestChunkStatesDownload(List<BigInteger> dataIDs) {
        requestChunkStatesDownload(dataIDs, null);
    }

    public void requestRenderDistancesDownload(List<BigInteger> dataIDs, BiConsumer<Boolean, Map<Integer, byte[]>> downloadCallBack) {
        requestDataDownload(DataRequestPacket.DataType.RENDER_DISTANCE, dataIDs, downloadCallBack);
    }

    public void requestRenderDistancesDownload(List<BigInteger> dataIDs) {
        requestRenderDistancesDownload(dataIDs, null);
    }

    public void requestTrackedPlayersDownload(List<BigInteger> dataIDs, BiConsumer<Boolean, Map<Integer, byte[]>> downloadCallBack) {
        requestDataDownload(DataRequestPacket.DataType.TRACKED_PLAYER, dataIDs, downloadCallBack);
    }

    public void requestTrackedPlayersDownload(List<BigInteger> dataIDs) {
        requestTrackedPlayersDownload(dataIDs, null);
    }

    public void onUpdateDataIDs(UpdateDataIDsPacket.DataType dataType, BigInteger minDataID, BigInteger maxDataID) {
        queuedPackets.add(new UpdateDataIDsPacket(dataType, minDataID, maxDataID));
    }

    /* ------------------------ Task Events ------------------------ */

    public synchronized void onTaskAdded(ITask task) {
        queuedPackets.add(new TaskActionPacket(task));
    }

    public synchronized void onTaskRemoved(ITask task) {
        queuedPackets.add(new TaskActionPacket(task.getID()));
    }

    public synchronized void onTaskUpdate(ITask task) {
        if (task instanceof ILoadedChunkTask) {
            queuedPackets.add(new TaskActionPacket(task, task.getProgress(), task.getTimeElapsed(),
                    ((ILoadedChunkTask)task).getCurrentPosition()));
        } else {
            queuedPackets.add(new TaskActionPacket(task, task.getProgress(), task.getTimeElapsed()));
        }
    }

    public synchronized void onTaskResult(ITask task, String result) {
        queuedPackets.add(new TaskActionPacket(task, result));
    }

    /* ------------------------ Player Events ------------------------ */

    public synchronized void onPlayerAdded(Player player) {
        queuedPackets.add(new PlayerActionPacket(PlayerActionPacket.Action.ADD, player));
    }

    public synchronized void onPlayerRemoved(Player player, String reason) {
        queuedPackets.add(new PlayerActionPacket(PlayerActionPacket.Action.REMOVE, player));
    }

    public synchronized void onPositionChanged(Player player) {
        queuedPackets.add(new PlayerActionPacket(player.getAuthService().getUsername(), player.getPosition(), player.getAngle()));
    }

    public synchronized void onDimensionChanged(Player player) {
        queuedPackets.add(new PlayerActionPacket(player.getAuthService().getUsername(), player.getDimension()));
    }

    public synchronized void onHealthChanged(Player player) {
        queuedPackets.add(new PlayerActionPacket(player.getAuthService().getUsername(), player.getFoodStats()));
    }

    /* ------------------------ Chunk Events ------------------------ */

    public synchronized void onChunkState(ChunkState chunkState) {
        queuedChunkStates.add(chunkState);
    }

    /* ------------------------ Tracker Events ------------------------ */

    public synchronized void onTrackerAdded(ITracker tracker) {
        queuedPackets.add(new TrackerActionPacket(TrackerActionPacket.Action.ADD, tracker));
    }

    public synchronized void onTrackerRemoved(ITracker tracker) {
        queuedPackets.add(new TrackerActionPacket(TrackerActionPacket.Action.REMOVE, tracker));
    }

    public synchronized void onTrackerUpdate(ITracker tracker) {
        queuedPackets.add(new TrackerActionPacket(TrackerActionPacket.Action.UPDATE, tracker));
    }

    /* ------------------------ Info Events ------------------------ */

    public synchronized void onInfoUpdate(int waitingQueries, int tickingQueries, float queriesPerSecond, float tickRate, int timeSinceLastPacket) {
        queuedPackets.add(new InfoUpdatePacket(waitingQueries, tickingQueries, queriesPerSecond, tickRate, timeSinceLastPacket));
    }

    public synchronized void onInfoUpdate(int waitingQueries, int tickingQueries, float queriesPerSecond) {
        queuedPackets.add(new InfoUpdatePacket(waitingQueries, tickingQueries, queriesPerSecond));
    }

    public synchronized void onPlayerJoin(UUID uuid, String name) {
        if (!syncedOnlinePlayers.contains(uuid)) {
            syncedOnlinePlayers.add(uuid);
            queuedJoins.put(uuid, name);
        }
    }

    public synchronized void onPlayerLeave(UUID uuid) {
        if (syncedOnlinePlayers.contains(uuid)) {
            syncedOnlinePlayers.remove(uuid);
            if (!queuedLeaves.contains(uuid)) queuedLeaves.add(uuid);
        }
    }

    /* ------------------------ Setters and Getters ------------------------ */

    public boolean isInitialized() {
        return initialized || initializing;
    }

    public boolean isSynced() {
        return synced;
    }

    public boolean isDownloading() {
        return downloading;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getExpectedParts() {
        return expectedParts;
    }

    public int getCurrentParts() {
        return dataParts.size();
    }

    public Map<Integer, byte[]> getDataParts() {
        return new HashMap<>(dataParts);
    }
}
