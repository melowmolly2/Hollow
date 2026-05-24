package controller.auth;

import controller.SceneManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.response.AuthResponse;
import service.LoginCallback;
import service.AuthService;

import java.io.IOException;

public class LoginPage {
    @FXML private TextField username;
    @FXML private PasswordField password;

    private final AuthService authService = new AuthService();

    @FXML public void back(ActionEvent event) throws IOException {
        SceneManager.changeScene(event, "/fxml/auth/landingPage.fxml");
    }
    @FXML public void submit(ActionEvent event){
        authService.login(username.getText(), password.getText(), new LoginCallback() {
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
