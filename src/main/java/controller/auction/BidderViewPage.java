package controller.auction;

import controller.app.AppPopup;
import controller.app.SceneManager;
import dto.auction.BidPostResponse;
import dto.auction.ItemResponse;
import dto.auction.ItemStatusResponse;
import dto.common.BaseResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.TokenStorage;
import service.auction.ItemService;
import service.auction.ItemStatusCallback;
import service.auction.BaseResponseCallback;
import service.auction.BidCallback;
import service.auction.BidService;
import network.PriceStreamListener;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class BidderViewPage {
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label bidIncrementLabel;
    @FXML private Label startTimeLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label minimumBidLabel;
    @FXML private Label lastBidLabel;
    @FXML private TextField bidAmountField;
    @FXML private TextField maxBidLimitField;
    @FXML private Label autoBidStatusLabel;
    @FXML private Button confirmBidButton;
    @FXML private Button autoBidButton;

    private final ItemService itemService = new ItemService();
    private final BidService bidService = new BidService();
    private final PriceStreamListener priceStreamListener = new PriceStreamListener();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<Long, Double> lastBidByItem = new HashMap<>();

    private ItemResponse item;
    private volatile boolean active;
    private ItemStatusResponse.ItemStatusData latestStatus;
    private Long observedEndTime;
    private boolean endPopupShown;
    private boolean auctionEnded;
    private boolean bidSubmitting;
    private boolean autoBidSubmitting;

    public void setItem(ItemResponse item) {
        this.item = item;
        active = true;
        observedEndTime = null;
        endPopupShown = false;
        auctionEnded = false;
        bidSubmitting = false;
        autoBidSubmitting = false;
        refreshBiddingControls();
        titleLabel.setText(item.title);
        descriptionLabel.setText(item.description);
        renderLastBid();
        loadItemStatus();
        startPriceStream();
    }

    @FXML
    public void back() throws IOException {
        active = false;
        priceStreamListener.stop();
        SceneManager.changeContent("/fxml/browseTab.fxml");
    }

    @FXML
    public void confirmBid() {
        if (item == null) {
            AppPopup.error("Missing item");
            return;
        }

        Double bidAmount = validateBidAmount();
        if (bidAmount == null) {
            return;
        }

        bidSubmitting = true;
        refreshBiddingControls();
        bidService.placeBid(item.itemId, String.valueOf(bidAmount), new BidCallback() {
            @Override
            public void onSuccess(BidPostResponse response) {
                Platform.runLater(() -> {
                    bidSubmitting = false;
                    refreshBiddingControls();
                    if (!active) {
                        return;
                    }
                    rememberLastBid(response);
                    renderLastBid();
                    bidAmountField.clear();
                    AppPopup.info(response.message);
                });
                if (active) {
                    loadItemStatus();
                }
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    bidSubmitting = false;
                    refreshBiddingControls();
                    if (active) {
                        handleAuthError(message);
                        AppPopup.error(message);
                    }
                });
            }
        });
    }

    @FXML
    public void submitAutoBid() {
        if (item == null) {
            AppPopup.error("Missing item");
            return;
        }

        Double maxBidLimit = validateMaxBidLimit();
        if (maxBidLimit == null) {
            return;
        }

        autoBidSubmitting = true;
        refreshBiddingControls();
        bidService.autoBid(item.itemId, String.valueOf(maxBidLimit), new BaseResponseCallback() {
            @Override
            public void onSuccess(BaseResponse response) {
                Platform.runLater(() -> {
                    autoBidSubmitting = false;
                    refreshBiddingControls();
                    if (!active) {
                        return;
                    }
                    autoBidStatusLabel.setText("Autobid: enabled, max bid limit = " + formatMoney(maxBidLimit));
                    maxBidLimitField.clear();
                    AppPopup.info(response.message);
                });
                if (active) {
                    loadItemStatus();
                }
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    autoBidSubmitting = false;
                    refreshBiddingControls();
                    if (active) {
                        handleAuthError(message);
                        autoBidStatusLabel.setText("Autobid: disabled");
                        AppPopup.error(message);
                    }
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
                if (active) {
                    AppPopup.error(message);
                }
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
            currentPriceLabel.setText("Current price: " + formatMoney(displayCurrentPrice(price)));
            loadItemStatus();
        });
    }

    private void renderStatus(ItemStatusResponse.ItemStatusData status) {
        if (status == null) {
            return;
        }

        latestStatus = status;

        currentPriceLabel.setText("Current price: " + formatMoney(displayCurrentPrice(status)));
        highestBidderLabel.setText("Highest bidder: " + valueOrNone(status.highestBidUser));
        startingPriceLabel.setText("Starting price: " + formatMoney(status.startingPrice));
        bidIncrementLabel.setText("Bid increment: " + formatMoney(status.bidIncrement));
        startTimeLabel.setText("Start time: " + formatTime(status.startTime));
        endTimeLabel.setText("End time: " + formatTime(status.endTime));

        double minimumBid = minimumBid(status);
        minimumBidLabel.setText("Minimum bid: " + formatMoney(minimumBid));
        bidAmountField.setPromptText(String.format("%.2f", minimumBid));

        renderAntiSniping(status.endTime);
        renderEndedState(status);
    }

    private void renderAntiSniping(Long endTime) {
        if (endTime == null) {
            return;
        }

        if (observedEndTime != null && endTime > observedEndTime) {
            AppPopup.info("Auction extended by anti-sniping rule");
        }
        observedEndTime = endTime;
    }

    private void renderEndedState(ItemStatusResponse.ItemStatusData status) {
        auctionEnded = isEnded(status);
        refreshBiddingControls();
        if (!auctionEnded || endPopupShown) {
            return;
        }

        endPopupShown = true;
        if (TokenStorage.username != null && TokenStorage.username.equals(status.highestBidUser)) {
            AppPopup.info("Auction ended, you've won");
        } else {
            AppPopup.info("Auction ended, you did not win");
        }
    }

    private boolean isEnded(ItemStatusResponse.ItemStatusData status) {
        if (status == null) {
            return false;
        }
        boolean timeEnded = status.endTime != null && status.endTime <= System.currentTimeMillis();
        boolean statusEnded = status.itemStatus != null && !"ACTIVE".equalsIgnoreCase(status.itemStatus);
        return timeEnded || statusEnded;
    }

    private void refreshBiddingControls() {
        bidAmountField.setDisable(auctionEnded || bidSubmitting);
        confirmBidButton.setDisable(auctionEnded || bidSubmitting);
        confirmBidButton.setText(bidSubmitting ? "Submitting..." : "Confirm");

        maxBidLimitField.setDisable(auctionEnded || autoBidSubmitting);
        autoBidButton.setDisable(auctionEnded || autoBidSubmitting);
        autoBidButton.setText(autoBidSubmitting ? "Setting..." : "Set auto-bid");
    }

    private Double validateBidAmount() {
        if (latestStatus == null) {
            AppPopup.error("Auction status is still loading");
            return null;
        }

        Double bidAmount = parseMoney(bidAmountField, "Bid amount must be a valid number");
        if (bidAmount == null) {
            return null;
        }

        double minimumBid = minimumBid(latestStatus);
        if (bidAmount < minimumBid) {
            AppPopup.error("Bid amount must be at least " + formatMoney(minimumBid));
            bidAmountField.requestFocus();
            return null;
        }

        return bidAmount;
    }

    private Double validateMaxBidLimit() {
        if (latestStatus == null) {
            AppPopup.error("Auction status is still loading");
            return null;
        }

        Double maxBidLimit = parseMoney(maxBidLimitField, "Max bid limit must be a valid number");
        if (maxBidLimit == null) {
            return null;
        }

        double minimumBid = minimumBid(latestStatus);
        if (maxBidLimit < minimumBid) {
            AppPopup.error("Max bid limit must be at least " + formatMoney(minimumBid));
            maxBidLimitField.requestFocus();
            return null;
        }

        return maxBidLimit;
    }

    private Double parseMoney(TextField field, String message) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isBlank()) {
            AppPopup.error(message);
            field.requestFocus();
            return null;
        }

        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) {
                AppPopup.error(message);
                field.requestFocus();
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            AppPopup.error(message);
            field.requestFocus();
            return null;
        }
    }

    private void handleAuthError(String message) {
        if (message != null && message.toLowerCase().contains("session expired")) {
            TokenStorage.clear();
        }
    }

    private double minimumBid(ItemStatusResponse.ItemStatusData status) {
        double currentPrice = safeMoney(status.currentPrice);
        double startingPrice = safeMoney(status.startingPrice);
        double bidIncrement = safeMoney(status.bidIncrement);

        if (currentPrice < startingPrice) {
            return startingPrice;
        }

        return currentPrice + bidIncrement;
    }

    private double displayCurrentPrice(ItemStatusResponse.ItemStatusData status) {
        return Math.max(safeMoney(status.currentPrice), safeMoney(status.startingPrice));
    }

    private Double displayCurrentPrice(Double streamedPrice) {
        double startingPrice = latestStatus == null ? 0.0 : safeMoney(latestStatus.startingPrice);
        return Math.max(safeMoney(streamedPrice), startingPrice);
    }

    private String formatMoney(Double value) {
        return String.format("%.2f", safeMoney(value));
    }

    private void rememberLastBid(BidPostResponse response) {
        if (item == null || item.itemId == null || response == null || response.bid == null
                || response.bid.bidAmount == null) {
            return;
        }

        lastBidByItem.put(item.itemId, response.bid.bidAmount);
    }

    private void renderLastBid() {
        if (lastBidLabel == null || item == null || item.itemId == null) {
            return;
        }

        Double lastBid = lastBidByItem.get(item.itemId);
        lastBidLabel.setText(lastBid == null ? "Your last bid: None" : "Your last bid: " + formatMoney(lastBid));
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
