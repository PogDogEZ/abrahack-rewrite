package ez.pogdog.yescom.handlers;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import ez.pogdog.yescom.YesCom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class responsible for providing and logging in cached accounts. These cached accounts are used by the
 * ConnectionHandler.
 */
public class AccountHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final Map<String, String> userMap = new HashMap<>();
    private final List<AuthenticationService> accountCache = new ArrayList<>();

    private final Pattern accountPattern = Pattern.compile("[A-Za-z0-9_.]+@([A-Za-z]+.[A-Za-z]+)+:.+");

    private final File accountsFile;

    public AccountHandler(String filePath) {
        accountsFile = Paths.get(filePath).toFile();

        yesCom.logger.fine(String.format("Accounts file: %s.", accountsFile));

        try {
            parseAccounts();
        } catch (IOException error) {
            yesCom.logger.warning("Couldn't load / parse accounts file due to:");
            yesCom.logger.throwing(AccountHandler.class.getSimpleName(), "AccountHandler", error);
        }

        loginAccounts();
    }

    @Override
    public void onTick() {
    }

    @Override
    public void onExit() {
    }

    /* ------------------------ Private Methods ------------------------ */

    private synchronized void parseAccounts() throws IOException {
        yesCom.logger.info("Parsing accounts...");

        userMap.clear();

        if (!accountsFile.exists()) {
            yesCom.logger.warning("Accounts file does not exist, creating one.");
            if (!accountsFile.createNewFile()) throw new IOException("Couldn't create the accounts file due to unknown.");
            yesCom.logger.info("Done.");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(accountsFile)));

        while (reader.ready()) {
            String line = reader.readLine();
            Matcher lineMatcher = accountPattern.matcher(line);

            if (lineMatcher.matches()) {
                String username = lineMatcher.group().split(":")[0];
                String password = lineMatcher.group().split(":", 2)[1];
                userMap.put(username, password);
                yesCom.logger.fine(String.format("Found account %s.",
                        yesCom.configHandler.DONT_SHOW_EMAILS ? "[EMAIL REDACTED]" : username));
            }
        }

        if (userMap.isEmpty()) {
            yesCom.logger.warning("Couldn't find any accounts, continuing anyway.");
        } else {
            yesCom.logger.info(String.format("Done, %s account(s) found.", userMap.size()));
        }
    }

    private synchronized void loginAccounts() {
        yesCom.logger.info("Authenticating accounts...");

        accountCache.clear();

        userMap.forEach((username, password) -> {
            try {
                legacyLogin(username, password);
            } catch (RequestException error) {
                yesCom.logger.warning(String.format("Error while logging in account: %s.",
                        yesCom.configHandler.DONT_SHOW_EMAILS ? "[EMAIL REDACTED]" : username));
                yesCom.logger.throwing(AccountHandler.class.getSimpleName(), "loginAccounts", error);
            }
        });

        if (accountCache.isEmpty()) {
            yesCom.logger.warning("Didn't authenticate any accounts, continuing anyway.");
        } else {
            yesCom.logger.info(String.format("Done, %s account(s) authenticated.", accountCache.size()));
        }
    }

    /* ------------------------ Public Methods ------------------------ */

    /**
     * Logs in an account.
     * @param username The account username (the email).
     * @param password The password for the account.
     * @throws RequestException If you fail to login, this is thrown.
     */
    public synchronized void legacyLogin(String username, String password) throws RequestException {
        if (accountCache.stream().anyMatch(authService -> authService.getUsername().equalsIgnoreCase(username)))
            return;

        AuthenticationService authService = new AuthenticationService();
        authService.setUsername(username);
        authService.setPassword(password);

        authService.login();

        if (authService.isLoggedIn()) {
            yesCom.logger.fine(String.format("Successfully authenticated account with username: %s.",
                    authService.getSelectedProfile().getName()));
            userMap.put(username, password);
            accountCache.add(authService);

            if (yesCom.ycHandler != null)
                yesCom.ycHandler.onPlayerAdded(authService.getUsername(), authService.getSelectedProfile().getId(),
                        authService.getSelectedProfile().getName());
        }
    }

    public synchronized void newLogin(String username, String accessToken, String clientToken) throws RequestException {
        if (accountCache.stream().anyMatch(authService -> authService.getUsername().equalsIgnoreCase(username)))
            return;

        AuthenticationService authService = new AuthenticationService(clientToken);
        authService.setUsername(username);
        authService.setAccessToken(accessToken);

        authService.login();

        if (authService.isLoggedIn()) {
            yesCom.logger.fine(String.format("Successfully authenticated account with username: %s.",
                    authService.getSelectedProfile().getName()));
            // userMap.put(username, accessToken);
            accountCache.add(authService);

            if (yesCom.ycHandler != null)
                yesCom.ycHandler.onPlayerAdded(authService.getUsername(), authService.getSelectedProfile().getId(),
                        authService.getSelectedProfile().getName());
        }
    }

    /**
     * Logs an account out.
     * @param username The username of the account (not the display name).
     */
    public synchronized void logout(String username) {
        Optional<AuthenticationService> foundAuthService = accountCache.stream()
                .filter(authService -> authService.getUsername().equals(username))
                .findAny();

        foundAuthService.ifPresent(authService -> {
            userMap.remove(username);
            accountCache.remove(authService);
            yesCom.connectionHandler.logout(authService.getSelectedProfile().getId());

            if (yesCom.ycHandler != null) yesCom.ycHandler.onPlayerRemoved(username);
        });
    }

    /**
     * Gets a cached account from their username.
     * @param username The username of the account (the email).
     * @return The cached account, null if not found.
     */
    public synchronized AuthenticationService getAccount(String username) {
        return accountCache.stream().filter(authService -> authService.getUsername().equals(username)).findFirst().orElse(null);
    }

    public synchronized List<AuthenticationService> getAccounts() {
        return new ArrayList<>(accountCache);
    }

    /* ------------------------ Refresh Stuff ------------------------ */

    public void refreshFile() {
        new Thread(() -> {
            try {
                parseAccounts();
            } catch (IOException error) {
                yesCom.logger.warning("Couldn't refresh accounts file:");
                yesCom.logger.throwing(AccountHandler.class.getSimpleName(), "refreshFile", error);
            }
        }).start();
    }

    public void refreshTokens() {
        new Thread(this::loginAccounts).start();
    }
}
