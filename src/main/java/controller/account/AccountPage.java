package controller.account;

import controller.app.AppPopup;
import controller.app.SceneManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.AccountSession;
import dto.account.BalanceResponse;
import service.account.AccountService;
import service.account.BalanceCallback;
import service.account.BalanceStreamManager;
import service.auth.AuthService;
import service.auth.LogoutCallback;

import java.io.IOException;


public class AccountPage {
    @FXML private Label balanceLabel;
    @FXML private TextField addField;
    @FXML private Button addButton;

    private final AccountService accountService = new AccountService();
    private final AuthService authService = new AuthService();

    public void initialize() {
        balanceLabel.textProperty().bind(Bindings.format("Your balance: %.2f", AccountSession.balanceProperty()));
        refreshBalance();
    }

    @FXML public void back(ActionEvent event) throws IOException {
        SceneManager.changeScene(event, "/fxml/framework.fxml");
    }

    @FXML public void add(){
        Double amount = validateDepositAmount();
        if (amount == null) {
            return;
        }

        setDepositing(true);
        accountService.deposit(String.valueOf(amount), new BalanceCallback() {
            @Override
            public void onSuccess(BalanceResponse response) {
                Platform.runLater(() -> {
                    setDepositing(false);
                    AccountSession.setBalance(response.balance);
                    addField.clear();
                    AppPopup.info(response.message);
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    setDepositing(false);
                    AppPopup.error(message);
                });
            }
        });
    }

    @FXML public void logout(ActionEvent event) {
        authService.logout(new LogoutCallback() {
            @Override
            public void onSuccess(String message) {
                Platform.runLater(() -> {
                    try {
                        BalanceStreamManager.stop();
                        AccountSession.setBalance(0.0);
                        SceneManager.changeScene(event, "/fxml/landingPage.fxml");
                        AppPopup.info(message);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> AppPopup.error(message));
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
                Platform.runLater(() -> AppPopup.error(message));
            }
        });
    }

    private Double validateDepositAmount() {
        String value = addField.getText() == null ? "" : addField.getText().trim();
        if (value.isBlank()) {
            AppPopup.error("Amount is required");
            addField.requestFocus();
            return null;
        }

        double amount;
        try {
            amount = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            AppPopup.error("Amount must be a valid number");
            addField.requestFocus();
            return null;
        }

        if (!Double.isFinite(amount)) {
            AppPopup.error("Amount must be a valid number");
            addField.requestFocus();
            return null;
        }
        if (amount <= 0) {
            AppPopup.error("Amount must be positive");
            addField.requestFocus();
            return null;
        }

        return amount;
    }

    private void setDepositing(boolean depositing) {
        addButton.setDisable(depositing);
        addButton.setText(depositing ? "Adding..." : "Add");
    }

}
