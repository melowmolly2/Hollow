package controller.auction;

import controller.app.AppPopup;
import controller.app.SceneManager;
import dto.auction.BidHistoryResponse;
import dto.auction.ItemResponse;
import dto.auction.ItemStatusResponse;
import dto.common.BaseResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import network.PriceStreamListener;
import service.auction.BaseResponseCallback;
import service.auction.BidHistoryCallback;
import service.auction.BidService;
import service.auction.ItemService;
import service.auction.ItemStatusCallback;
import service.auction.LocalBidHistoryStore;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SellerViewPage {
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label bidIncrementLabel;
    @FXML private Label buyNowPriceLabel;
    @FXML private Label startTimeLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label maxEndTimeLabel;
    @FXML private Label actionHelpLabel;
    @FXML private Label bidHistorySummaryLabel;
    @FXML private Button editAuctionButton;
    @FXML private Button endAuctionButton;
    @FXML private LineChart<Number, Number> bidHistoryChart;

    private final ItemService itemService = new ItemService();
    private final BidService bidService = new BidService();
    private final LocalBidHistoryStore localBidHistoryStore = new LocalBidHistoryStore();
    private final PriceStreamListener priceStreamListener = new PriceStreamListener();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ItemResponse item;
    private volatile boolean active;

    public void initialize() {
        editAuctionButton.setDisable(true);
        editAuctionButton.setTooltip(new Tooltip("Backend does not provide an auction update endpoint."));
        bidHistoryChart.setCreateSymbols(true);
    }

    public void setItem(ItemResponse item) {
        if (item == null) {
            AppPopup.error("Missing item");
            return;
        }

        this.item = item;
        active = true;
        titleLabel.setText(valueOrNone(item.title));
        descriptionLabel.setText(valueOrNone(item.description));
        loadItemStatus();
        loadBidHistory();
        startPriceStream();
    }

    @FXML
    public void back() throws IOException {
        active = false;
        priceStreamListener.stop();
        SceneManager.changeContent("/fxml/mySaleTab.fxml");
    }

    @FXML
    public void editAuction() {
        AppPopup.info("Edit Auction is unavailable because the backend has no update endpoint.");
    }

    @FXML
    public void endAuction() {
        if (item == null || item.itemId == null) {
            AppPopup.error("Missing item");
            return;
        }

        endAuctionButton.setDisable(true);
        itemService.cancelItem(item.itemId, new BaseResponseCallback() {
            @Override
            public void onSuccess(BaseResponse response) {
                Platform.runLater(() -> {
                    if (!active) {
                        return;
                    }
                    AppPopup.info(response.message);
                    loadItemStatus();
                    loadBidHistory();
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    if (!active) {
                        return;
                    }
                    endAuctionButton.setDisable(false);
                    AppPopup.error(message);
                });
            }
        });
    }

    private void loadItemStatus() {
        if (item == null || item.itemId == null) {
            return;
        }

        itemService.getItemStatus(item.itemId, new ItemStatusCallback() {
            @Override
            public void onSuccess(ItemStatusResponse response) {
                Platform.runLater(() -> {
                    if (active) {
                        renderStatus(response.itemStatus);
                    }
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    if (active) {
                        AppPopup.error(message);
                    }
                });
            }
        });
    }

    private void loadBidHistory() {
        if (item == null || item.itemId == null) {
            return;
        }

        bidService.getBidHistory(item.itemId, 0, 20, new BidHistoryCallback() {
            @Override
            public void onSuccess(BidHistoryResponse response) {
                if (!active) {
                    return;
                }
                cacheBackendBidHistory(response);
                Platform.runLater(() -> {
                    if (active) {
                        renderBidHistory(response);
                    }
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    if (!active) {
                        return;
                    }
                    bidHistorySummaryLabel.setText("Unable to load bid history");
                    AppPopup.error(message);
                });
            }
        });
    }

    private void startPriceStream() {
        if (item == null || item.itemId == null) {
            return;
        }

        priceStreamListener.start(item.itemId, price -> {
            if (!active) {
                return;
            }
            localBidHistoryStore.addPoint(item.itemId, price, System.currentTimeMillis());
            currentPriceLabel.setText("Current price: " + formatMoney(price));
            renderBidHistory(null);
            loadItemStatus();
        });
    }

    private void renderStatus(ItemStatusResponse.ItemStatusData status) {
        if (status == null) {
            return;
        }

        String itemStatus = valueOrNone(status.itemStatus);
        statusLabel.setText("Status: " + itemStatus);
        currentPriceLabel.setText("Current price: " + formatMoney(status.currentPrice));
        highestBidderLabel.setText("Highest bidder: " + valueOrNone(status.highestBidUser));
        startingPriceLabel.setText("Starting price: " + formatMoney(status.startingPrice));
        bidIncrementLabel.setText("Bid increment: " + formatMoney(status.bidIncrement));
        buyNowPriceLabel.setText("Buy now price: " + formatMoney(status.buyItNowPrice));
        startTimeLabel.setText("Start time: " + formatTime(status.startTime));
        endTimeLabel.setText("End time: " + formatTime(status.endTime));
        maxEndTimeLabel.setText("Max end time: " + formatTime(status.maxEndTime));

        boolean cancelable = "ACTIVE".equalsIgnoreCase(itemStatus) && !hasEnded(status.endTime);
        endAuctionButton.setDisable(!cancelable);
        actionHelpLabel.setText(cancelable
                ? "End Auction cancels this active auction through the backend."
                : "End Auction is unavailable because this auction is not active.");
    }

    private void renderBidHistory(BidHistoryResponse response) {
        bidHistoryChart.getData().clear();

        List<LocalBidHistoryStore.BidPoint> bids = mergedBidHistory(response);
        if (bids.isEmpty()) {
            bidHistorySummaryLabel.setText("No bids yet");
            return;
        }

        long firstBidTime = bids.getFirst().time;
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (LocalBidHistoryStore.BidPoint bid : bids) {
            double secondsFromStart = (bid.time - firstBidTime) / 1000.0;
            XYChart.Data<Number, Number> point = new XYChart.Data<>(secondsFromStart, safeMoney(bid.amount));
            point.setExtraValue(formatChartTime(bid.time));
            series.getData().add(point);
        }

        bidHistoryChart.getData().add(series);

        long bidRecords = bids.stream().filter(point -> point.bidRecord).count();
        bidHistorySummaryLabel.setText(bidRecords + " bid record" + (bidRecords == 1 ? "" : "s")
                + ", " + bids.size() + " price point" + (bids.size() == 1 ? "" : "s"));
    }

    private void cacheBackendBidHistory(BidHistoryResponse response) {
        if (item == null || item.itemId == null || response == null || response.entity == null
                || response.entity.content == null) {
            return;
        }

        List<LocalBidHistoryStore.BidPoint> backendPoints = response.entity.content.stream()
                .filter(bid -> bid.bidAmount != null && bid.time != null)
                .map(bid -> new LocalBidHistoryStore.BidPoint(bid.bidAmount, bid.time, true, false))
                .toList();
        localBidHistoryStore.addPoints(item.itemId, backendPoints);
    }

    private List<LocalBidHistoryStore.BidPoint> mergedBidHistory(BidHistoryResponse response) {
        if (item == null || item.itemId == null) {
            return List.of();
        }

        List<LocalBidHistoryStore.BidPoint> points = new ArrayList<>(localBidHistoryStore.getPoints(item.itemId));
        if (response != null && response.entity != null && response.entity.content != null) {
            response.entity.content.stream()
                    .filter(bid -> bid.bidAmount != null && bid.time != null)
                    .map(bid -> new LocalBidHistoryStore.BidPoint(bid.bidAmount, bid.time, true, false))
                    .forEach(points::add);
        }

        Map<String, LocalBidHistoryStore.BidPoint> uniquePoints = new LinkedHashMap<>();
        points.stream()
                .filter(point -> point.amount != null && point.time != null)
                .sorted(Comparator.comparingLong(point -> point.time))
                .forEach(point -> mergePoint(uniquePoints, point));

        return new ArrayList<>(uniquePoints.values());
    }

    private void mergePoint(Map<String, LocalBidHistoryStore.BidPoint> uniquePoints,
                            LocalBidHistoryStore.BidPoint point) {
        String key = point.time + ":" + point.amount;
        LocalBidHistoryStore.BidPoint existing = uniquePoints.get(key);
        if (existing == null) {
            uniquePoints.put(key, point);
            return;
        }

        existing.bidRecord = existing.bidRecord || point.bidRecord;
        existing.ownBid = existing.ownBid || point.ownBid;
    }

    private boolean hasEnded(Long endTime) {
        return endTime != null && endTime <= System.currentTimeMillis();
    }

    private String formatMoney(Double value) {
        return String.format("%.2f", safeMoney(value));
    }

    private double safeMoney(Double value) {
        return value == null ? 0.0 : value;
    }

    private String valueOrNone(String value) {
        return value == null || value.isBlank() ? "None" : value;
    }

    private String formatTime(Long epochMillis) {
        if (epochMillis == null) {
            return "N/A";
        }

        return timeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }

    private String formatChartTime(Long epochMillis) {
        if (epochMillis == null) {
            return "N/A";
        }

        return DateTimeFormatter.ofPattern("HH:mm:ss")
                .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }
}
