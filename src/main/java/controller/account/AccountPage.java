package controller.account;

import controller.app.AppPopup;
import controller.app.SceneManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.AccountSession;
import dto.account.BalanceResponse;
import service.account.AccountService;
import service.account.BalanceCallback;

import java.io.IOException;


public class AccountPage {
    @FXML private Label balanceLabel;
    @FXML private TextField addField;

    private final AccountService accountService = new AccountService();

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

}
