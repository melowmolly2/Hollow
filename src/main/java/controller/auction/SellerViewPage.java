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

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    @FXML private LineChart<String, Number> bidHistoryChart;

    private final ItemService itemService = new ItemService();
    private final BidService bidService = new BidService();
    private final PriceStreamListener priceStreamListener = new PriceStreamListener();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ItemResponse item;
    private ItemStatusResponse.ItemStatusData latestStatus;
    private final List<Double> observedBidAmounts = new ArrayList<>();
    private boolean bidHistorySeeded;

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
        titleLabel.setText(valueOrNone(item.title));
        descriptionLabel.setText(valueOrNone(item.description));
        loadItemStatus();
        startPriceStream();
    }

    @FXML
    public void back() throws IOException {
        priceStreamListener.stop();
        SceneManager.changeContent("/fxml/mySaleTab.fxml");
        SceneManager.selectMySaleNavigation();
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
                    AppPopup.info(response.message);
                    loadItemStatus();
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
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
                Platform.runLater(() -> renderStatus(response.itemStatus));
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> AppPopup.error(message));
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
                Platform.runLater(() -> renderBidHistory(response));
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
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
            Double currentPrice = displayCurrentPrice(price);
            currentPriceLabel.setText("Current price: " + formatMoney(currentPrice));
            appendObservedBid(currentPrice);
            loadItemStatus();
        });
    }

    private void renderStatus(ItemStatusResponse.ItemStatusData status) {
        if (status == null) {
            return;
        }

        String itemStatus = valueOrNone(status.itemStatus);
        latestStatus = status;

        statusLabel.setText("Status: " + itemStatus);
        currentPriceLabel.setText("Current price: " + formatMoney(displayCurrentPrice(status)));
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

        loadBidHistory();
    }

    private void renderBidHistory(BidHistoryResponse response) {
        bidHistoryChart.getData().clear();

        double startingPrice = latestStatus == null ? 0.0 : safeMoney(latestStatus.startingPrice);
        if (startingPrice <= 0) {
            bidHistorySummaryLabel.setText("Waiting for starting price");
            return;
        }

        if (response == null || response.entity == null || response.entity.content == null
                || response.entity.content.isEmpty()) {
            if (!bidHistorySeeded && observedBidAmounts.isEmpty()) {
                bidHistorySummaryLabel.setText("No bids yet");
            }
            return;
        }

        List<BidHistoryResponse.BidData> bids = new ArrayList<>(response.entity.content.stream()
                .filter(bid -> bid.bidAmount != null)
                .toList());

        if (bids.isEmpty()) {
            bidHistorySummaryLabel.setText("No valid bid amounts");
            return;
        }

        if (bids.stream().allMatch(bid -> bid.time != null)) {
            bids.sort(Comparator.comparingLong(bid -> bid.time));
        }

        if (!bidHistorySeeded && observedBidAmounts.isEmpty()) {
            observedBidAmounts.clear();
        }
        bids.forEach(bid -> addObservedBidAmount(safeMoney(bid.bidAmount)));
        bidHistorySeeded = true;
        renderObservedBidHistory(startingPrice);
    }

    private void appendObservedBid(Double bidAmount) {
        if (bidAmount == null || latestStatus == null) {
            return;
        }

        double startingPrice = safeMoney(latestStatus.startingPrice);
        if (startingPrice <= 0 || safeMoney(bidAmount) <= startingPrice) {
            return;
        }

        if (addObservedBidAmount(safeMoney(bidAmount))) {
            renderObservedBidHistory(startingPrice);
        }
    }

    private boolean addObservedBidAmount(double bidAmount) {
        if (!observedBidAmounts.isEmpty()) {
            double lastBidAmount = observedBidAmounts.get(observedBidAmounts.size() - 1);
            if (Double.compare(lastBidAmount, bidAmount) == 0) {
                return false;
            }
        }

        observedBidAmounts.add(bidAmount);
        return true;
    }

    private void renderObservedBidHistory(double startingPrice) {
        bidHistoryChart.getData().clear();

        if (observedBidAmounts.isEmpty()) {
            bidHistorySummaryLabel.setText("No valid bid amounts");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int index = 0; index < observedBidAmounts.size(); index++) {
            double percentOfStartingPrice = observedBidAmounts.get(index) / startingPrice * 100.0;
            series.getData().add(new XYChart.Data<>(String.valueOf(index + 1), percentOfStartingPrice));
        }

        bidHistoryChart.getData().add(series);
        int bidCount = observedBidAmounts.size();
        bidHistorySummaryLabel.setText(bidCount + " bid" + (bidCount == 1 ? "" : "s")
                + " shown, base " + formatMoney(startingPrice));
    }

    private boolean hasEnded(Long endTime) {
        return endTime != null && endTime <= System.currentTimeMillis();
    }

    private Double displayCurrentPrice(ItemStatusResponse.ItemStatusData status) {
        if (status == null) {
            return null;
        }
        return Math.max(safeMoney(status.currentPrice), safeMoney(status.startingPrice));
    }

    private Double displayCurrentPrice(Double streamedPrice) {
        double startingPrice = latestStatus == null ? 0.0 : safeMoney(latestStatus.startingPrice);
        return Math.max(safeMoney(streamedPrice), startingPrice);
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

}
