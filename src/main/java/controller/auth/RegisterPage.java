package controller.auth;

import controller.app.AppPopup;
import controller.app.SceneManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import service.auth.RegisterCallBack;
import service.auth.AuthService;

import java.io.IOException;

public class RegisterPage {
    @FXML private TextField username;
    @FXML private TextField displayName;
    @FXML private PasswordField password;
    @FXML private Button submitButton;

    private final AuthService authService = new AuthService();

    @FXML public void back(ActionEvent event) throws IOException {
        SceneManager.changeScene(event, "/fxml/landingPage.fxml");
    }
    @FXML public void submit(ActionEvent event){
        String usernameValue = username.getText() == null ? "" : username.getText();
        String displayNameValue = displayName.getText() == null ? "" : displayName.getText().trim();
        String passwordValue = password.getText() == null ? "" : password.getText();

        if (usernameValue.trim().isBlank()) {
            AppPopup.error("Username is required");
            username.requestFocus();
            return;
        }
        if (containsWhitespace(usernameValue)) {
            AppPopup.error("Username can't have space");
            username.requestFocus();
            return;
        }
        if (displayNameValue.isBlank()) {
            AppPopup.error("Display name is required");
            displayName.requestFocus();
            return;
        }
        if (passwordValue.isBlank()) {
            AppPopup.error("Password is required");
            password.requestFocus();
            return;
        }
        if (containsWhitespace(passwordValue)) {
            AppPopup.error("Password can't have space");
            password.requestFocus();
            return;
        }

        setSubmitting(true);
        authService.register(usernameValue, displayNameValue, passwordValue, new RegisterCallBack() {
            @Override
            public void onSuccess(String message) {
                Platform.runLater(() -> {
                    try {
                        setSubmitting(false);
                        AppPopup.info(message);
                        SceneManager.changeScene(event, "/fxml/landingPage.fxml");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
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
        submitButton.setText(submitting ? "Creating account..." : "Submit");
    }

    private boolean containsWhitespace(String value) {
        return value != null && value.chars().anyMatch(Character::isWhitespace);
    }
}
