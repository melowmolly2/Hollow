package controller.auth;

import controller.app.AppPopup;
import controller.app.SceneManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import dto.auth.AuthResponse;
import service.auth.LoginCallback;
import service.auth.AuthService;

import java.io.IOException;

public class LoginPage {
    @FXML private TextField username;
    @FXML private PasswordField password;
    @FXML private Button submitButton;

    private final AuthService authService = new AuthService();

    @FXML public void back(ActionEvent event) throws IOException {
        SceneManager.changeScene(event, "/fxml/landingPage.fxml");
    }
    @FXML public void submit(ActionEvent event){
        String usernameValue = username.getText() == null ? "" : username.getText().trim();
        String passwordValue = password.getText() == null ? "" : password.getText();

        if (usernameValue.isBlank()) {
            AppPopup.error("Username is required");
            username.requestFocus();
            return;
        }
        if (passwordValue.isBlank()) {
            AppPopup.error("Password is required");
            password.requestFocus();
            return;
        }

        setSubmitting(true);
        authService.login(usernameValue, passwordValue, new LoginCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                Platform.runLater(() -> {
                            try {
                                String targetPage = isAdmin(usernameValue)
                                        ? "/fxml/adminPage.fxml"
                                        : "/fxml/framework.fxml";
                                SceneManager.changeScene(event, targetPage);
                                AppPopup.info(response.message);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            } finally {
                                setSubmitting(false);
                            }
                        }
                );
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    setSubmitting(false);
                    AppPopup.error(message);
                });
            }
        });
    }

    private void setSubmitting(boolean submitting) {
        submitButton.setDisable(submitting);
        submitButton.setText(submitting ? "Logging in..." : "Submit");
    }

    private boolean isAdmin(String username) {
        return "admin".equalsIgnoreCase(username);
    }
}
