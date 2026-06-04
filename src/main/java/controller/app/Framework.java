package controller.app;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import model.AccountSession;
import dto.account.BalanceResponse;
import service.account.AccountService;
import service.account.BalanceCallback;

import java.io.IOException;

public class Framework {
    @FXML private StackPane contentArea;
    @FXML private Label balanceLabel;

    private final AccountService accountService = new AccountService();

    @FXML public void dashboard() throws IOException {
        SceneManager.changeContent("/fxml/dashboardTab.fxml");
    }
    @FXML public void browse() throws IOException {
        SceneManager.changeContent("/fxml/browseTab.fxml");
    }
    @FXML public void mySale() throws IOException {
        SceneManager.changeContent("/fxml/mySaleTab.fxml");
    }
    @FXML public void watchlist() throws IOException {
        SceneManager.changeContent("/fxml/watchlistTab.fxml");
    }
    @FXML public void account() throws IOException {
        SceneManager.changeContent("/fxml/accountPage.fxml");
    }

    public void initialize() throws IOException {
        SceneManager.setContentArea(contentArea);
        balanceLabel.textProperty().bind(Bindings.format("Your balance: %.2f", AccountSession.balanceProperty()));
        SceneManager.changeContent("/fxml/dashboardTab.fxml");
        refreshBalance();
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
