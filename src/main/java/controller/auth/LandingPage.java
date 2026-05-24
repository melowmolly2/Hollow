package controller.auth;

import controller.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;

public class LandingPage {
    @FXML public void login(ActionEvent event) throws IOException {
        SceneManager.changeScene(event, "/fxml/auth/loginPage.fxml");
    }
    @FXML public void register(ActionEvent event) throws IOException {
        SceneManager.changeScene(event, "/fxml/auth/registerPage.fxml");
    }
}
