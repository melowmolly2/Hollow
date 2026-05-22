package controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.response.AuthResponse;
import service.AuthCallback;
import service.AuthService;

import java.io.IOException;

// Note: Handles login and opens the main app after success.
public class LoginPage {
    @FXML private TextField username;
    @FXML private PasswordField password;

    private final AuthService authService = new AuthService();

    @FXML public void back(ActionEvent event) throws IOException {
        SceneManager.changeScene(event, "/fxml/landingPage.fxml");
    }
    @FXML public void submit(ActionEvent event){
        // Note: Auth runs async; JavaFX UI changes stay on the UI thread.
        authService.login(username.getText(), password.getText(), new AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                Platform.runLater(() -> {
                    System.out.println("Login success: " + response.message);
                            try {
                                SceneManager.changeScene(event, "/fxml/framework.fxml");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    System.out.println(message);
                });
            }
        });
    }
}
