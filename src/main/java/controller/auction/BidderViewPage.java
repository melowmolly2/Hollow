package controller.auction;

import controller.app.AppPopup;
import controller.app.SceneManager;
import dto.account.BalanceResponse;
import dto.auction.BidPostResponse;
import dto.auction.ItemResponse;
import dto.auction.ItemStatusResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.AccountSession;
import service.account.AccountService;
import service.account.BalanceCallback;
import service.auction.ItemService;
import service.auction.ItemStatusCallback;
import service.auction.BidCallback;
import service.auction.BidService;
import service.auction.LocalBidHistoryStore;
import network.PriceStreamListener;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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

    private final ItemService itemService = new ItemService();
    private final BidService bidService = new BidService();
    private final AccountService accountService = new AccountService();
    private final LocalBidHistoryStore localBidHistoryStore = new LocalBidHistoryStore();
    private final PriceStreamListener priceStreamListener = new PriceStreamListener();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ItemResponse item;
    private volatile boolean active;

    public void setItem(ItemResponse item) {
        this.item = item;
        active = true;
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

        bidService.placeBid(item.itemId, bidAmountField.getText(), new BidCallback() {
            @Override
            public void onSuccess(BidPostResponse response) {
                if (response != null && response.bid != null && response.bid.bidAmount != null) {
                    localBidHistoryStore.addBidPoint(item.itemId, response.bid.bidAmount, System.currentTimeMillis(), true);
                }
                Platform.runLater(() -> {
                    if (!active) {
                        return;
                    }
                    bidAmountField.clear();
                    renderLastBid();
                    AppPopup.info(response.message);
                });
                if (active) {
                    loadItemStatus();
                    refreshBalance();
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

    private void refreshBalance() {
        accountService.getBalance(new BalanceCallback() {
            @Override
            public void onSuccess(BalanceResponse response) {
                Platform.runLater(() -> {
                    if (active) {
                        AccountSession.setBalance(response.balance);
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
            currentPriceLabel.setText("Current price: " + formatMoney(price));
            localBidHistoryStore.addPoint(item.itemId, price, System.currentTimeMillis());
            loadItemStatus();
            refreshBalance();
        });
    }

    private void renderStatus(ItemStatusResponse.ItemStatusData status) {
        if (status == null) {
            return;
        }

        currentPriceLabel.setText("Current price: " + formatMoney(status.currentPrice));
        highestBidderLabel.setText("Highest bidder: " + valueOrNone(status.highestBidUser));
        startingPriceLabel.setText("Starting price: " + formatMoney(status.startingPrice));
        bidIncrementLabel.setText("Bid increment: " + formatMoney(status.bidIncrement));
        startTimeLabel.setText("Start time: " + formatTime(status.startTime));
        endTimeLabel.setText("End time: " + formatTime(status.endTime));

        double minimumBid = safeMoney(status.currentPrice) + safeMoney(status.bidIncrement);
        minimumBidLabel.setText("Minimum bid: " + formatMoney(minimumBid));
        bidAmountField.setPromptText(String.format("%.2f", minimumBid));
    }

    private void renderLastBid() {
        if (item == null || item.itemId == null) {
            lastBidLabel.setText("Your last bid: none");
            return;
        }

        localBidHistoryStore.getPoints(item.itemId).stream()
                .filter(point -> point.ownBid && point.amount != null)
                .reduce((first, second) -> second)
                .ifPresentOrElse(
                        point -> lastBidLabel.setText("Your last bid: " + formatMoney(point.amount)),
                        () -> lastBidLabel.setText("Your last bid: none")
                );
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
