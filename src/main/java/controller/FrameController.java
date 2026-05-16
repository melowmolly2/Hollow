package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

public class FrameController {
    @FXML
    private VBox content;

    @FXML
    public void initialize() throws Exception{
        for (int i = 1; i<=10; i++){
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auctionCard.fxml"));
            Parent root = loader.load();

            CardController card = loader.getController();
            card.setDetail("Name: " + i);

            content.getChildren().add(root);
        }
    }
}