package network;

import javafx.application.Platform;

import model.TokenStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class BalanceStreamListener {
    private Thread thread;
    private volatile boolean running;
    private HttpURLConnection connection;

    public synchronized void start(String username, Consumer<Double> onBalance) {
        stop();

        if (username == null || username.isBlank()) {
            return;
        }

        running = true;
        thread = new Thread(() -> listen(username, onBalance));
        thread.setDaemon(true);
        thread.setName("balance-stream-" + username);
        thread.start();
    }

    public synchronized void stop() {
        running = false;

        if (connection != null) {
            connection.disconnect();
            connection = null;
        }

        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
        thread = null;
    }

    private void listen(String username, Consumer<Double> onBalance) {
        HttpURLConnection activeConnection = null;
        try {
            URL url = new URL(ApiClient.BASE_URL + URLEncoder.encode(username, StandardCharsets.UTF_8) + "/balance/stream");
            activeConnection = (HttpURLConnection) url.openConnection();
            connection = activeConnection;
            activeConnection.setRequestMethod("GET");
            activeConnection.setRequestProperty("Accept", "text/event-stream");
            String authorization = TokenStorage.authorizationHeader();
            if (authorization != null) {
                activeConnection.setRequestProperty("Authorization", authorization);
            }
            activeConnection.setConnectTimeout(5000);
            activeConnection.setReadTimeout(0);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(activeConnection.getInputStream())
            )) {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    handleLine(line, onBalance);
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (activeConnection != null) {
                activeConnection.disconnect();
            }
            synchronized (this) {
                if (connection == activeConnection) {
                    connection = null;
                }
                if (thread == Thread.currentThread()) {
                    thread = null;
                }
            }
        }
    }

    private void handleLine(String line, Consumer<Double> onBalance) {
        if (!line.startsWith("data:")) {
            return;
        }

        String value = line.substring("data:".length()).trim();
        try {
            double balance = Double.parseDouble(value);
            Platform.runLater(() -> {
                if (running) {
                    onBalance.accept(balance);
                }
            });
        } catch (NumberFormatException ignored) {
        }
    }
}
