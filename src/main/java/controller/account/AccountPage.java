package controller.account;

import controller.app.AppPopup;
import controller.app.SceneManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.AccountSession;
import dto.account.BalanceResponse;
import service.account.AccountService;
import service.account.BalanceCallback;
import service.auth.AuthService;
import service.auth.LogoutCallback;

import java.io.IOException;


public class AccountPage {
    @FXML private Label balanceLabel;
    @FXML private TextField addField;

    private final AccountService accountService = new AccountService();
    private final AuthService authService = new AuthService();

    public void initialize() {
        balanceLabel.textProperty().bind(Bindings.format("Your balance: %.2f", AccountSession.balanceProperty()));
        refreshBalance();
    }

    @FXML public void back() throws IOException {
        SceneManager.changeContent("/fxml/dashboardTab.fxml");
    }

    @FXML public void add(){
        accountService.deposit(addField.getText(), new BalanceCallback() {
            @Override
            public void onSuccess(BalanceResponse response) {
                Platform.runLater(() -> {
                    AccountSession.setBalance(response.balance);
                    addField.clear();
                    AppPopup.info(response.message);
                });
            }

            @Override
            public void onError(String message) {
                AppPopup.error(message);
            }
        });
    }

    @FXML
    public void logout() {
        authService.logout(new LogoutCallback() {
            @Override
            public void onComplete() {
                Platform.runLater(() -> {
                    AccountSession.setBalance(0.0);
                    goToLandingPage();
                    AppPopup.info("Logged out");
                });
            }
        });
    }

    private void refreshBalance() {
        accountService.getBalance(new BalanceCallback() {
            @Override
            public void onSuccess(BalanceResponse response) {
                Platform.runLater(() -> AccountSession.setBalance(response.balance));
            }

            @Override
            public void onError(String message) {
                AppPopup.error(message);
            }
        });
    }

    private void goToLandingPage() {
        try {
            Stage stage = (Stage) balanceLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/landingPage.fxml"));
            Scene scene = new Scene(loader.load(), stage.getScene().getWidth(), stage.getScene().getHeight());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
