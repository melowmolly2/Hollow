package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;

// Note: First screen; routes users to login or register.
public class LandingPage {
    @FXML public void login(ActionEvent event) throws IOException {
        SceneManager.changeScene(event, "/fxml/loginPage.fxml");
    }
    @FXML public void register(ActionEvent event) throws IOException {
        SceneManager.changeScene(event, "/fxml/registerPage.fxml");
    }
}
