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
import service.auction.SellerListingCallback;

import java.io.IOException;

public class Card {
    @FXML private Label itemLabel;
    @FXML private Label priceLabel;
    @FXML private Label timeLabel;
    @FXML private Label infoLabel;

    private final ItemService itemService = new ItemService();

    private ItemResponse item;

    public void setItem(ItemResponse item) {
        this.item = item;
        itemLabel.setText("Item: " + item.title);
        infoLabel.setText("Info: " + item.description);
        priceLabel.setText("Price: loading...");
        timeLabel.setText("Time: loading...");
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
