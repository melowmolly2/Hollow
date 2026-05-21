package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import model.response.BaseItemResponse;
import service.ItemCallback;
import service.ItemService;

import java.io.IOException;

public class CreatePage {
    @FXML private TextField title;
    @FXML private TextArea description;
    @FXML private TextField endTime;
    @FXML private TextField startPrice;
    @FXML private TextField increment;
    @FXML private TextField buyItNow;

    private final ItemService itemService = new ItemService();

    @FXML public void back() throws IOException {
        SceneManager.changeContent("/fxml/mySaleTab.fxml");
    }
    @FXML public void create(){
        itemService.createItem(
                title.getText(),
                description.getText(),
                endTime.getText(),
                startPrice.getText(),
                increment.getText(),
                buyItNow.getText(),
                new ItemCallback() {
                    @Override
                    public void onSuccess(BaseItemResponse response) {
                        Platform.runLater(() -> {
                            System.out.println("Created item: " + response.item.title);
                            try {
                                SceneManager.changeContent("/fxml/mySaleTab.fxml");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Platform.runLater(() -> {
                            System.out.println(message);
                        });
                    }
                }
        );
    }
}
