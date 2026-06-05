package controller.auction;

import dto.auction.BidHistoryResponse;
import dto.auction.GetItemPageResponse;
import dto.auction.ItemResponse;
import dto.auction.ItemStatusResponse;
import dto.auction.MyWinsResponse;
import dto.auction.SellerListingResponse;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.TokenStorage;
import service.auction.BidService;
import service.auction.ItemPageCallback;
import service.auction.ItemService;
import service.auction.ItemStatusCallback;
import service.auction.MyWinsCallback;
import service.auction.SellerListingCallback;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DashboardTab {
    @FXML private Label ongoingAuctionsLabel;
    @FXML private Label yourActiveAuctionsLabel;
    @FXML private Label yourWinsLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<WinRow> winningHistoryTable;
    @FXML private TableColumn<WinRow, String> winTitleColumn;
    @FXML private TableColumn<WinRow, String> winBidColumn;
    @FXML private TableColumn<WinRow, String> winTimeColumn;

    private final ItemService itemService = new ItemService();
    private final BidService bidService = new BidService();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void initialize() {
        configureWinningHistoryTable();
        refresh();
    }

    private void configureWinningHistoryTable() {
        winTitleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().title));
        winBidColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().bidAmount));
        winTimeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().bidTime));
        winningHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        winningHistoryTable.setPlaceholder(new Label("No winning history yet."));
    }

    @FXML
    public void refresh() {
        ongoingAuctionsLabel.setText("-");
        yourActiveAuctionsLabel.setText("-");
        yourWinsLabel.setText("-");
        winningHistoryTable.setItems(FXCollections.observableArrayList());
        winningHistoryTable.setPlaceholder(new Label("Loading winning history..."));
        statusLabel.setText("Loading dashboard...");

        loadOngoingAuctions();
        loadYourActiveAuctions();
        loadWinningHistory();
    }

    private void loadOngoingAuctions() {
        itemService.getItems(0, 1, new ItemPageCallback() {
            @Override
            public void onSuccess(GetItemPageResponse response) {
                Platform.runLater(() -> ongoingAuctionsLabel.setText(String.valueOf(totalItems(response))));
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    ongoingAuctionsLabel.setText("N/A");
                    statusLabel.setText("Unable to load ongoing auctions");
                });
            }
        });
    }

    private long totalItems(GetItemPageResponse response) {
        if (response == null || response.pages == null) {
            return 0;
        }
        if (response.pages.totalElements > 0) {
            return response.pages.totalElements;
        }
        return response.pages.totalPages;
    }

    private void loadYourActiveAuctions() {
        String username = TokenStorage.username;
        if (username == null || username.isBlank()) {
            yourActiveAuctionsLabel.setText("0");
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
                    countActiveListings(listings);
                    return;
                }

                loadSellerListingPage(username, page + 1, listings);
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    yourActiveAuctionsLabel.setText("N/A");
                    statusLabel.setText("Unable to load your active auctions");
                });
            }
        });
    }

    private void countActiveListings(List<ItemResponse> listings) {
        if (listings.isEmpty()) {
            Platform.runLater(() -> yourActiveAuctionsLabel.setText("0"));
            return;
        }

        AtomicInteger pending = new AtomicInteger(listings.size());
        AtomicInteger activeCount = new AtomicInteger(0);

        for (ItemResponse item : listings) {
            if (item == null || item.itemId == null) {
                if (pending.decrementAndGet() == 0) {
                    Platform.runLater(() -> yourActiveAuctionsLabel.setText(String.valueOf(activeCount.get())));
                }
                continue;
            }

            itemService.getItemStatus(item.itemId, new ItemStatusCallback() {
                @Override
                public void onSuccess(ItemStatusResponse response) {
                    if (isActive(response == null ? null : response.itemStatus)) {
                        activeCount.incrementAndGet();
                    }
                    renderActiveCountIfDone(pending, activeCount);
                }

                @Override
                public void onError(String message) {
                    renderActiveCountIfDone(pending, activeCount);
                }
            });
        }
    }

    private void renderActiveCountIfDone(AtomicInteger pending, AtomicInteger activeCount) {
        if (pending.decrementAndGet() == 0) {
            Platform.runLater(() -> yourActiveAuctionsLabel.setText(String.valueOf(activeCount.get())));
        }
    }

    private boolean isActive(ItemStatusResponse.ItemStatusData status) {
        if (status == null || status.endTime == null) {
            return false;
        }
        return "ACTIVE".equalsIgnoreCase(status.itemStatus)
                && status.endTime > System.currentTimeMillis();
    }

    private void loadWinningHistory() {
        bidService.getMyWins(new MyWinsCallback() {
            @Override
            public void onSuccess(MyWinsResponse response) {
                Platform.runLater(() -> renderWinningHistory(response));
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    yourWinsLabel.setText("N/A");
                    winningHistoryTable.setItems(FXCollections.observableArrayList());
                    winningHistoryTable.setPlaceholder(new Label("Unable to load winning history"));
                    statusLabel.setText("Unable to load winning history");
                });
            }
        });
    }

    private void renderWinningHistory(MyWinsResponse response) {
        List<MyWinsResponse.WinData> wins = response == null || response.entity == null
                ? List.of()
                : response.entity;

        yourWinsLabel.setText(String.valueOf(wins.size()));

        if (wins.isEmpty()) {
            winningHistoryTable.setItems(FXCollections.observableArrayList());
            winningHistoryTable.setPlaceholder(new Label("No winning history yet."));
            statusLabel.setText("Dashboard loaded");
            return;
        }

        List<WinRow> rows = wins.stream()
                .map(this::toWinRow)
                .toList();
        winningHistoryTable.setItems(FXCollections.observableArrayList(rows));
        statusLabel.setText("Dashboard loaded");
    }

    private WinRow toWinRow(MyWinsResponse.WinData win) {
        String title = win == null || win.item == null || isBlank(win.item.title)
                ? "Untitled auction"
                : win.item.title;
        BidHistoryResponse.BidData bid = win == null ? null : win.bid;
        return new WinRow(
                title,
                formatMoney(bid == null ? null : bid.bidAmount),
                formatTime(bid == null ? null : bid.time)
        );
    }

    private String formatMoney(Double value) {
        return value == null ? "N/A" : String.format("%.2f", value);
    }

    private String formatTime(Long epochMillis) {
        if (epochMillis == null) {
            return "Time N/A";
        }
        return timeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static class WinRow {
        private final String title;
        private final String bidAmount;
        private final String bidTime;

        private WinRow(String title, String bidAmount, String bidTime) {
            this.title = title;
            this.bidAmount = bidAmount;
            this.bidTime = bidTime;
        }
    }
}
