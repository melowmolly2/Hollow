package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class BidController {
    @FXML
    public void back(ActionEvent event) throws Exception {
        SceneManager.changeScene("/fxml/landingPage.fxml", event);
    }
}
