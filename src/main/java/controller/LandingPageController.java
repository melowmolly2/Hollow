package controller;

import javafx.fxml.FXML;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

public class LandingPageController {
    @FXML
    public void login(ActionEvent event) throws Exception {
        SceneManager.changeScene("/fxml/login.fxml",event);
    }
    @FXML
    public void register(ActionEvent event) throws Exception {
        SceneManager.changeScene("/fxml/register.fxml",event);
    }

}
