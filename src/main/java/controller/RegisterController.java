package controller;

import javafx.fxml.FXML;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

public class RegisterController {
    @FXML
    public void register(ActionEvent event) throws Exception {
        SceneManager.changeScene("/fxml/frame.fxml", event);
    }
    @FXML
    public void back(ActionEvent event) throws Exception {
        SceneManager.changeScene("/fxml/landingPage.fxml", event);
    }
}
