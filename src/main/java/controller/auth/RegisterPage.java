package controller.auth;

import controller.SceneManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import service.RegisterCallBack;
import service.AuthService;

import java.io.IOException;

public class RegisterPage {
    @FXML private TextField username;
    @FXML private TextField displayName;
    @FXML private PasswordField password;

    private final AuthService authService = new AuthService();

    @FXML public void back(ActionEvent event) throws IOException {
        SceneManager.changeScene(event, "/fxml/auth/landingPage.fxml");
    }
    @FXML public void submit(){
        authService.register(username.getText(), displayName.getText(), password.getText(), new RegisterCallBack() {
            @Override
            public void onSuccess(String message) {
                Platform.runLater(() -> System.out.println("Register success: " + message));
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> System.out.println(message));
            }
        });
    }
}
