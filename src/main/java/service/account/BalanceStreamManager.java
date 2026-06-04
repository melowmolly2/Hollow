package service.account;

import model.AccountSession;
import network.BalanceStreamListener;

public final class BalanceStreamManager {
    private static final BalanceStreamListener LISTENER = new BalanceStreamListener();
    private static String activeUsername;

    private BalanceStreamManager() {
    }

    public static synchronized void start(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        if (username.equals(activeUsername)) {
            return;
        }

        activeUsername = username;
        Thread startThread = new Thread(
                () -> LISTENER.start(username, AccountSession::setBalance),
                "start-balance-stream"
        );
        startThread.setDaemon(true);
        startThread.start();
    }

    public static synchronized void stop() {
        activeUsername = null;
        Thread stopThread = new Thread(LISTENER::stop, "stop-balance-stream");
        stopThread.setDaemon(true);
        stopThread.start();
    }
}
