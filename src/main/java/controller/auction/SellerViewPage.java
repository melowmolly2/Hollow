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
        loadBidHistory();
        startPriceStream();
    }

    @FXML
    public void back() throws IOException {
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
                    AppPopup.info(response.message);
                    loadItemStatus();
                    loadBidHistory();
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
            currentPriceLabel.setText("Current price: " + formatMoney(price));
            loadItemStatus();
            loadBidHistory();
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

        if (response == null || response.entity == null || response.entity.content == null
                || response.entity.content.isEmpty()) {
            bidHistorySummaryLabel.setText("No bids yet");
            return;
        }

        List<BidHistoryResponse.BidData> bids = response.entity.content.stream()
                .filter(bid -> bid.bidAmount != null && bid.time != null)
                .sorted(Comparator.comparingLong(bid -> bid.time))
                .toList();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (BidHistoryResponse.BidData bid : bids) {
            series.getData().add(new XYChart.Data<>(formatChartTime(bid.time), safeMoney(bid.bidAmount)));
        }

        bidHistoryChart.getData().add(series);
        bidHistorySummaryLabel.setText(bids.size() + " bid" + (bids.size() == 1 ? "" : "s") + " shown");
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
