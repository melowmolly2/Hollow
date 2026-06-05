package controller.auction;

import controller.app.SceneManager;
import dto.auction.ItemResponse;
import dto.auction.ItemStatusResponse;
import dto.auction.SellerListingResponse;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.TokenStorage;
import service.auction.ItemService;
import service.auction.ItemStatusCallback;
import service.auction.SellerListingCallback;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MySaleTab {
    @FXML private Label completedSalesLabel;
    @FXML private Label totalEarnedLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<SaleRow> salesHistoryTable;
    @FXML private TableColumn<SaleRow, String> saleTitleColumn;
    @FXML private TableColumn<SaleRow, String> saleStateColumn;
    @FXML private TableColumn<SaleRow, String> saleWinnerColumn;
    @FXML private TableColumn<SaleRow, String> saleEarnedColumn;
    @FXML private TableColumn<SaleRow, String> saleEndedColumn;

    private final ItemService itemService = new ItemService();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void initialize() {
        configureSalesHistoryTable();
        refresh();
    }

    private void configureSalesHistoryTable() {
        saleTitleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().title));
        saleStateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().state));
        saleWinnerColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().winner));
        saleEarnedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatMoney(cell.getValue().earned)));
        saleEndedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatTime(cell.getValue().endTime)));
        salesHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        salesHistoryTable.setPlaceholder(new Label("No past auctions yet."));
    }

    @FXML
    public void create() throws IOException {
        SceneManager.changeContent("/fxml/createPage.fxml");
    }

    @FXML
    public void refresh() {
        completedSalesLabel.setText("-");
        totalEarnedLabel.setText("-");
        statusLabel.setText("Loading sales history...");
        salesHistoryTable.setItems(FXCollections.observableArrayList());
        salesHistoryTable.setPlaceholder(new Label("Loading sales history..."));

        String username = TokenStorage.username;
        if (username == null || username.isBlank()) {
            completedSalesLabel.setText("0");
            totalEarnedLabel.setText("0.00");
            statusLabel.setText("Login required");
            salesHistoryTable.setItems(FXCollections.observableArrayList());
            salesHistoryTable.setPlaceholder(new Label("Login to view your sales history."));
            return;
        }

        loadSellerListingPage(username, 0, new ArrayList<>());
    }

    private void loadSellerListingPage(String username, int page, List<ItemResponse> listings) {
        itemService.getSellerListings(username, page, 20, new SellerListingCallback() {
            @Override
            public void onSuccess(SellerListingResponse response) {
                if (response != null && response.entity != null && response.entity.content != null) {
                    listings.addAll(response.entity.content);
                }

                boolean lastPage = response == null || response.entity == null || response.entity.last
                        || page + 1 >= response.entity.totalPages;
                if (lastPage) {
                    loadStatuses(username, listings);
                    return;
                }

                loadSellerListingPage(username, page + 1, listings);
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    completedSalesLabel.setText("N/A");
                    totalEarnedLabel.setText("N/A");
                    statusLabel.setText("Unable to load seller listings");
                    salesHistoryTable.setItems(FXCollections.observableArrayList());
                    salesHistoryTable.setPlaceholder(new Label("Unable to load sales history."));
                });
            }
        });
    }

    private void loadStatuses(String username, List<ItemResponse> listings) {
        if (listings.isEmpty()) {
            Platform.runLater(() -> renderSales(List.of()));
            return;
        }

        AtomicInteger pending = new AtomicInteger(listings.size());
        List<SaleRow> rows = java.util.Collections.synchronizedList(new ArrayList<>());

        for (ItemResponse item : listings) {
            if (item == null || item.itemId == null) {
                renderIfDone(pending, rows);
                continue;
            }

            itemService.getItemStatus(item.itemId, new ItemStatusCallback() {
                @Override
                public void onSuccess(ItemStatusResponse response) {
                    ItemStatusResponse.ItemStatusData status = response == null ? null : response.itemStatus;
                    if (status != null && isPastAuction(status)) {
                        rows.add(toSaleRow(username, item, status));
                    }
                    renderIfDone(pending, rows);
                }

                @Override
                public void onError(String message) {
                    renderIfDone(pending, rows);
                }
            });
        }
    }

    private void renderIfDone(AtomicInteger pending, List<SaleRow> rows) {
        if (pending.decrementAndGet() == 0) {
            Platform.runLater(() -> renderSales(rows));
        }
    }

    private boolean isPastAuction(ItemStatusResponse.ItemStatusData status) {
        boolean timeEnded = status.endTime != null && status.endTime <= System.currentTimeMillis();
        boolean statusClosed = status.itemStatus != null && !"ACTIVE".equalsIgnoreCase(status.itemStatus);
        return timeEnded || statusClosed;
    }

    private SaleRow toSaleRow(String username, ItemResponse item, ItemStatusResponse.ItemStatusData status) {
        boolean sold = isSold(username, status);
        double earned = sold ? safeMoney(status.currentPrice) : 0.0;
        String state = statusLabel(username, status, sold);
        return new SaleRow(
                valueOrDefault(item.title, "Untitled auction"),
                state,
                valueOrDefault(status.highestBidUser, "None"),
                earned,
                status.endTime
        );
    }

    private boolean isSold(String username, ItemStatusResponse.ItemStatusData status) {
        return safeMoney(status.currentPrice) > 0
                && status.highestBidUser != null
                && !status.highestBidUser.isBlank()
                && !status.highestBidUser.equals(username);
    }

    private String statusLabel(String username, ItemStatusResponse.ItemStatusData status, boolean sold) {
        if (status.itemStatus != null && "CANCELED".equalsIgnoreCase(status.itemStatus)) {
            return "Canceled";
        }
        if (sold) {
            return "Sold";
        }
        return "Unsold";
    }

    private void renderSales(List<SaleRow> rows) {
        List<SaleRow> sortedRows = rows.stream()
                .sorted((left, right) -> Long.compare(safeTime(right.endTime), safeTime(left.endTime)))
                .toList();

        long completedSales = sortedRows.stream()
                .filter(row -> "Sold".equals(row.state))
                .count();
        double totalEarned = sortedRows.stream()
                .mapToDouble(row -> row.earned)
                .sum();

        completedSalesLabel.setText(String.valueOf(completedSales));
        totalEarnedLabel.setText(formatMoney(totalEarned));

        if (sortedRows.isEmpty()) {
            statusLabel.setText("No past auctions");
            salesHistoryTable.setItems(FXCollections.observableArrayList());
            salesHistoryTable.setPlaceholder(new Label("No past auctions yet."));
            return;
        }

        salesHistoryTable.setItems(FXCollections.observableArrayList(sortedRows));
        statusLabel.setText("Sales history loaded");
    }

    private String formatMoney(double value) {
        return String.format("%.2f", value);
    }

    private String formatTime(Long epochMillis) {
        if (epochMillis == null) {
            return "Time N/A";
        }
        return timeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }

    private double safeMoney(Double value) {
        return value == null ? 0.0 : value;
    }

    private long safeTime(Long value) {
        return value == null ? 0L : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static class SaleRow {
        private final String title;
        private final String state;
        private final String winner;
        private final double earned;
        private final Long endTime;

        private SaleRow(String title, String state, String winner, double earned, Long endTime) {
            this.title = title;
            this.state = state;
            this.winner = winner;
            this.earned = earned;
            this.endTime = endTime;
        }
    }
}
