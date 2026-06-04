package controller.auction;

import controller.app.AppPopup;
import controller.app.SceneManager;
import dto.auction.SellerListingResponse;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import dto.auction.ItemResponse;
import model.TokenStorage;
import service.auction.ItemService;
import service.auction.ItemStatusCallback;
import service.auction.SellerListingCallback;
import dto.auction.ItemStatusResponse;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public class Card {
    @FXML private Label itemLabel;
    @FXML private Label priceLabel;
    @FXML private Label timeLabel;
    @FXML private Label infoLabel;

    private final ItemService itemService = new ItemService();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.US);

    private ItemResponse item;

    public void initialize() {
        currencyFormat.setMaximumFractionDigits(2);
    }

    public void setItem(ItemResponse item) {
        this.item = item;
        itemLabel.setText("Item: " + item.title);
        infoLabel.setText("Info: " + item.description);
        priceLabel.setText("Price: loading...");
        timeLabel.setText("Time: loading...");
        loadStatus();
    }

    public void setStatus(String price, String time, String info) {
        priceLabel.setText("Price: " + price);
        timeLabel.setText("Time: " + time);
        infoLabel.setText("Info: " + info);
    }

    private void loadStatus() {
        if (item == null || item.itemId == null) {
            renderStatus(null);
            return;
        }

        Long itemId = item.itemId;
        itemService.getItemStatus(itemId, new ItemStatusCallback() {
            @Override
            public void onSuccess(ItemStatusResponse response) {
                Platform.runLater(() -> {
                    if (item != null && itemId.equals(item.itemId)) {
                        renderStatus(response.itemStatus);
                    }
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    if (item != null && itemId.equals(item.itemId)) {
                        renderStatus(null);
                    }
                });
            }
        });
    }

    private void renderStatus(ItemStatusResponse.ItemStatusData status) {
        if (status == null) {
            priceLabel.setText("Price: unavailable");
            timeLabel.setText("Time: unavailable");
            return;
        }

        if (status.currentPrice == 0){
            priceLabel.setText(("Price: " + formatMoney(status.startingPrice)));
        } else {
            priceLabel.setText("Price: " + formatMoney(status.currentPrice));
        }

        timeLabel.setText("Time: " + formatTimeLeft(status.endTime));
    }

    private String formatMoney(Double value) {
        if (value == null) {
            return "-";
        }
        return currencyFormat.format(value);
    }

    private String formatTimeLeft(Long endTime) {
        if (endTime == null) {
            return "-";
        }

        long millisLeft = endTime - Instant.now().toEpochMilli();
        if (millisLeft <= 0) {
            return "Ended";
        }

        Duration duration = Duration.ofMillis(millisLeft);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return Math.max(minutes, 1) + "m";
    }

    @FXML
    public void seeDetails() {
        if (item == null) {
            AppPopup.error("Missing item");
            return;
        }

        if (TokenStorage.username == null || TokenStorage.username.isBlank()) {
            openBidderView();
            return;
        }

        checkSellerListingPage(0);
    }

    private void checkSellerListingPage(int page) {
        itemService.getSellerListings(TokenStorage.username, page, 20, new SellerListingCallback() {
            @Override
            public void onSuccess(SellerListingResponse response) {
                Platform.runLater(() -> {
                    if (isSellerItem(response)) {
                        openSellerView();
                    } else if (isLastPage(response)) {
                        openBidderView();
                    } else {
                        checkSellerListingPage(page + 1);
                    }
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    AppPopup.error(message);
                    openBidderView();
                });
            }
        });
    }

    private boolean isLastPage(SellerListingResponse response) {
        return response == null || response.entity == null || response.entity.last;
    }

    private boolean isSellerItem(SellerListingResponse response) {
        if (response == null || response.entity == null || response.entity.content == null || item.itemId == null) {
            return false;
        }

        return response.entity.content.stream()
                .anyMatch(listing -> item.itemId.equals(listing.itemId));
    }

    private void openBidderView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/bidderViewPage.fxml"));
            Parent view = loader.load();

            BidderViewPage controller = loader.getController();
            controller.setItem(item);

            SceneManager.changeContent(view);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openSellerView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sellerViewPage.fxml"));
            Parent view = loader.load();

            SellerViewPage controller = loader.getController();
            controller.setItem(item);

            SceneManager.changeContent(view);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
