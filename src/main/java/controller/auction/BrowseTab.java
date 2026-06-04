package controller.auction;

import controller.app.AppPopup;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import dto.auction.GetItemPageResponse;
import dto.auction.ItemResponse;
import service.auction.ItemPageCallback;
import service.auction.ItemService;

import java.io.IOException;

public class BrowseTab {
    @FXML private FlowPane cardContainer;
    @FXML private Label pageLabel;
    @FXML private Button nextButton;
    @FXML private Button prevButton;

    private final ItemService itemService = new ItemService();

    private int page = 0;
    private int totalPages = 1;

    public void initialize(){
        loadPage();
    }

    @FXML
    public void prev() {
        if (page > 0) {
            page--;
            loadPage();
        }
    }

    @FXML
    public void next() {
        if (page + 1 < totalPages) {
            page++;
            loadPage();
        }
    }

    private void loadPage(){
        int size = 2;
        itemService.getItems(page, size, new ItemPageCallback() {
            @Override
            public void onSuccess(GetItemPageResponse response) {
                Platform.runLater(() -> renderPage(response));
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    pageLabel.setText("Unable to load items");
                    AppPopup.error(message);
                });
            }
        });
    }

    private void renderPage(GetItemPageResponse response){
        cardContainer.getChildren().clear();
        if (response.pages == null || response.pages.content == null) {
            pageLabel.setText("No data");
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            return;
        }
        totalPages = response.pages.totalPages;

        for (ItemResponse item : response.pages.content) {
            cardContainer.getChildren().add(loadCard(item));
        }

        pageLabel.setText("Page " + (page + 1) + " / " + Math.max(totalPages, 1));
        prevButton.setDisable(page == 0);
        nextButton.setDisable(page + 1 >= totalPages);
    }

    private Parent loadCard(ItemResponse item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/card.fxml"));
            Parent root = loader.load();

            Card controller = loader.getController();
            controller.setItem(item);

            return root;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
