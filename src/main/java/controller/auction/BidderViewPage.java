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

    public void setItem(ItemResponse item) {
        this.item = item;
        active = true;
        observedEndTime = null;
        endPopupShown = false;
        setBiddingDisabled(false);
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

        if (!validateBidAmount()) {
            return;
        }

        bidService.placeBid(item.itemId, bidAmountField.getText(), new BidCallback() {
            @Override
            public void onSuccess(BidPostResponse response) {
                Platform.runLater(() -> {
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
                if (active) {
                    AppPopup.error(message);
                }
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

        bidService.autoBid(item.itemId, maxBidLimitField.getText(), new BaseResponseCallback() {
            @Override
            public void onSuccess(BaseResponse response) {
                Platform.runLater(() -> {
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
                    if (active) {
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
        boolean ended = isEnded(status);
        setBiddingDisabled(ended);
        if (!ended || endPopupShown) {
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

    private void setBiddingDisabled(boolean disabled) {
        bidAmountField.setDisable(disabled);
        maxBidLimitField.setDisable(disabled);
        confirmBidButton.setDisable(disabled);
        autoBidButton.setDisable(disabled);
    }

    private boolean validateBidAmount() {
        if (latestStatus == null) {
            AppPopup.error("Auction status is still loading");
            return false;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(bidAmountField.getText());
        } catch (NumberFormatException e) {
            AppPopup.error("Bid amount must be a valid number");
            return false;
        }

        double minimumBid = minimumBid(latestStatus);
        if (bidAmount < minimumBid) {
            AppPopup.error("Bid amount must be at least " + formatMoney(minimumBid));
            return false;
        }

        return true;
    }

    private Double validateMaxBidLimit() {
        if (latestStatus == null) {
            AppPopup.error("Auction status is still loading");
            return null;
        }

        double maxBidLimit;
        try {
            maxBidLimit = Double.parseDouble(maxBidLimitField.getText());
        } catch (NumberFormatException e) {
            AppPopup.error("Max bid limit must be a valid number");
            return null;
        }

        double minimumBid = minimumBid(latestStatus);
        if (maxBidLimit < minimumBid) {
            AppPopup.error("Max bid limit must be at least " + formatMoney(minimumBid));
            return null;
        }

        return maxBidLimit;
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
