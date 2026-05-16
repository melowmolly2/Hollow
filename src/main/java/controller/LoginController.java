package controller;

import javafx.fxml.FXML;

import javafx.event.ActionEvent;

public class LoginController {
    @FXML
    public void login(ActionEvent event) throws Exception {
        SceneManager.changeScene("/fxml/frame.fxml", event);
    }
    @FXML
    public void back(ActionEvent event) throws Exception {
        SceneManager.changeScene("/fxml/landingPage.fxml", event);
    }
}
