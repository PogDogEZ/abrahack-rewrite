package ez.pogdog.yescom.managers;

import ez.pogdog.yescom.IManager;
import me.iska.jserver.JServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages trusted handlers.
 */
@SuppressWarnings("FieldCanBeLocal")
public class TrustedManager implements IManager {

    private final File TRUSTED_FILE = Paths.get("trusted.txt").toFile();

    private final Logger logger = JServer.getLogger();

    private final Map<byte[], byte[]> trustedHandlers = new HashMap<>();

    public TrustedManager() {
        if (!TRUSTED_FILE.exists() || TRUSTED_FILE.isDirectory()) {
            logger.fine("Creating trusted file as it does not exist.");
            try {
                if (!TRUSTED_FILE.createNewFile()) throw new IOException("Couldn't create file due to unknown.");
            } catch (IOException error) {
                logger.warning("Couldn't create trusted file.");
                logger.throwing(TrustedManager.class.getSimpleName(), "TrustedManager", error);
            }

        } else {
            logger.fine("Reading trusted file...");
            try {
                Files.readAllLines(TRUSTED_FILE.toPath()).forEach(line -> trustedHandlers.put(Base64.getDecoder().decode(line), null));
            } catch (IOException error) {
                logger.warning("Couldn't read trusted file.");
                logger.throwing(TrustedManager.class.getSimpleName(), "TrustedManager", error);
                return;
            }
            logger.fine(String.format("Read %d trusted handlers.", trustedHandlers.size()));
        }
    }

    /**
     * Checks whether the given hash is trusted.
     * @param handlerHash The handler hash.
     * @param publicKey The handler's public key.
     * @return Whether the given hash is trusted.
     */
    public boolean isTrusted(byte[] handlerHash, byte[] publicKey) {
        boolean trusted = false;
        byte[] trustedPublicKey = null;

        for (Map.Entry<byte[], byte[]> entry : trustedHandlers.entrySet()) { // FUCK Java arrays
            if (Arrays.equals(entry.getKey(), handlerHash)) {
                trusted = true;
                trustedPublicKey = entry.getValue();
            }
        }

        if (!trusted) return false;

        if (trustedPublicKey == null) {
            trustedHandlers.put(handlerHash, publicKey);
            return true;
        } else {
            return Arrays.equals(trustedPublicKey, publicKey);
        }
    }
}
