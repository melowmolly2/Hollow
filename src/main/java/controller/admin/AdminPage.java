package controller.admin;

import controller.app.AppPopup;
import controller.app.SceneManager;
import dto.auction.ItemListResponse;
import dto.auction.ItemResponse;
import dto.common.BaseResponse;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import model.AccountSession;
import service.account.BalanceStreamManager;
import service.admin.AdminService;
import service.admin.ItemListCallback;
import service.auction.BaseResponseCallback;
import service.auth.AuthService;
import service.auth.LogoutCallback;

import java.io.IOException;
import java.util.List;

public class AdminPage {
    @FXML private TextField banUsernameField;
    @FXML private Button banButton;
    @FXML private TextField unbanUsernameField;
    @FXML private PasswordField unbanPasswordField;
    @FXML private Button unbanButton;
    @FXML private TextField endAuctionItemIdField;
    @FXML private Button endAuctionButton;
    @FXML private Button logoutButton;
    @FXML private TableView<ItemResponse> itemTable;
    @FXML private TableColumn<ItemResponse, String> itemIdColumn;
    @FXML private TableColumn<ItemResponse, String> itemTitleColumn;
    @FXML private TableColumn<ItemResponse, String> itemSellerColumn;
    @FXML private TableColumn<ItemResponse, String> itemDescriptionColumn;
    @FXML private Label itemTableStatusLabel;
    @FXML private Button refreshItemsButton;

    private final AdminService adminService = new AdminService();
    private final AuthService authService = new AuthService();

    public void initialize() {
        configureTables();
        refreshItems();
    }

    @FXML
    public void banUser() {
        String username = text(banUsernameField).trim();
        if (username.isBlank()) {
            reject(banUsernameField, "Username is required");
            return;
        }
        if ("admin".equalsIgnoreCase(username)) {
            reject(banUsernameField, "Admin account cannot be banned from the client");
            return;
        }

        setButtonLoading(banButton, true, "Banning...");
        adminService.banUser(username, callback(
                banButton,
                "Ban",
                banUsernameField::clear
        ));
    }

    @FXML
    public void unbanUser() {
        String username = text(unbanUsernameField).trim();
        String password = unbanPasswordField.getText() == null ? "" : unbanPasswordField.getText();

        if (username.isBlank()) {
            reject(unbanUsernameField, "Username is required");
            return;
        }
        if (password.isBlank()) {
            reject(unbanPasswordField, "Password is required");
            return;
        }

        setButtonLoading(unbanButton, true, "Unbanning...");
        adminService.unbanUser(username, password, callback(
                unbanButton,
                "Unban",
                () -> {
                    unbanUsernameField.clear();
                    unbanPasswordField.clear();
                }
        ));
    }

    @FXML
    public void endAuction() {
        Long itemId = parseItemId();
        if (itemId == null) {
            return;
        }

        setButtonLoading(endAuctionButton, true, "Ending...");
        adminService.endAuction(itemId, callback(
                endAuctionButton,
                "End auction",
                () -> {
                    endAuctionItemIdField.clear();
                    refreshItems();
                }
        ));
    }

    @FXML
    public void refreshItems() {
        refreshItemsButton.setDisable(true);
        itemTableStatusLabel.setText("Loading items...");
        adminService.getItems(new ItemListCallback() {
            @Override
            public void onSuccess(ItemListResponse response) {
                Platform.runLater(() -> {
                    List<ItemResponse> items = response.items == null ? List.of() : response.items;
                    itemTable.setItems(FXCollections.observableArrayList(items));
                    itemTableStatusLabel.setText(items.size() + " active auction" + (items.size() == 1 ? "" : "s"));
                    refreshItemsButton.setDisable(false);
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    itemTable.setItems(FXCollections.observableArrayList());
                    itemTableStatusLabel.setText("Unable to load items: " + message);
                    refreshItemsButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    public void logout(ActionEvent event) {
        setButtonLoading(logoutButton, true, "Logging out...");
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
                    } finally {
                        setButtonLoading(logoutButton, false, "Logout");
                    }
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    setButtonLoading(logoutButton, false, "Logout");
                    AppPopup.error(message);
                });
            }
        });
    }

    private BaseResponseCallback callback(Button button, String idleText, Runnable afterSuccess) {
        return new BaseResponseCallback() {
            @Override
            public void onSuccess(BaseResponse response) {
                Platform.runLater(() -> {
                    setButtonLoading(button, false, idleText);
                    afterSuccess.run();
                    AppPopup.info(response.message);
                });
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    setButtonLoading(button, false, idleText);
                    AppPopup.error(message);
                });
            }
        };
    }

    private Long parseItemId() {
        String value = text(endAuctionItemIdField).trim();
        if (value.isBlank()) {
            reject(endAuctionItemIdField, "Item id is required");
            return null;
        }

        try {
            long itemId = Long.parseLong(value);
            if (itemId <= 0) {
                reject(endAuctionItemIdField, "Item id must be positive");
                return null;
            }
            return itemId;
        } catch (NumberFormatException e) {
            reject(endAuctionItemIdField, "Item id must be a whole number");
            return null;
        }
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText();
    }

    private void reject(javafx.scene.Node field, String message) {
        AppPopup.error(message);
        field.requestFocus();
    }

    private void setButtonLoading(Button button, boolean loading, String text) {
        button.setDisable(loading);
        button.setText(text);
    }

    private void configureTables() {
        itemIdColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().itemId == null ? "" : String.valueOf(data.getValue().itemId)
        ));
        itemTitleColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrBlank(data.getValue().title)));
        itemSellerColumn.setCellValueFactory(data -> new SimpleStringProperty(sellerName(data.getValue())));
        itemDescriptionColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrBlank(data.getValue().description)));
    }

    private String sellerName(ItemResponse item) {
        if (item == null) {
            return "";
        }
        if (item.sellerUsername != null && !item.sellerUsername.isBlank()) {
            return item.sellerUsername;
        }
        if (item.user != null && item.user.username != null && !item.user.username.isBlank()) {
            return item.user.username;
        }
        return "N/A";
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }

    private String formatMoney(Double value) {
        return value == null ? "" : String.format("%.2f", value);
    }
}
