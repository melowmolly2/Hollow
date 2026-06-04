package network;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class PriceStreamListener {
    private Thread thread;
    private volatile boolean running;
    private HttpURLConnection connection;

    public synchronized void start(Long itemId, Consumer<Double> onPrice) {
        stop();

        running = true;
        thread = new Thread(() -> listen(itemId, onPrice));
        thread.setDaemon(true);
        thread.setName("price-stream-" + itemId);
        thread.start();
    }

    public synchronized void stop() {
        running = false;

        HttpURLConnection connectionToClose = connection;
        connection = null;

        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
        thread = null;

        if (connectionToClose == null) {
            return;
        }

        if (Platform.isFxApplicationThread()) {
            Thread disconnectThread = new Thread(connectionToClose::disconnect);
            disconnectThread.setDaemon(true);
            disconnectThread.setName("price-stream-disconnect");
            disconnectThread.start();
        } else {
            connectionToClose.disconnect();
        }
    }

    private void listen(Long itemId, Consumer<Double> onPrice) {
        HttpURLConnection activeConnection = null;
        try {
            URL url = new URL("http://localhost:8080/items/stream/" + itemId);
            activeConnection = (HttpURLConnection) url.openConnection();
            connection = activeConnection;
            activeConnection.setRequestMethod("GET");
            activeConnection.setRequestProperty("Accept", "text/event-stream");
            activeConnection.setConnectTimeout(5000);
            activeConnection.setReadTimeout(0);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(activeConnection.getInputStream())
            )) {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    handleLine(line, onPrice);
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

    private void handleLine(String line, Consumer<Double> onPrice) {
        if (!line.startsWith("data:")) {
            return;
        }

        String value = line.substring("data:".length()).trim();
        try {
            double price = Double.parseDouble(value);
            Platform.runLater(() -> {
                if (running) {
                    onPrice.accept(price);
                }
            });
        } catch (NumberFormatException ignored) {
        }
    }
}
