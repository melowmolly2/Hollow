package controller.auction;

import controller.app.AppPopup;
import controller.app.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import dto.auction.BaseItemResponse;
import service.auction.ItemCallback;
import service.auction.ItemService;

import java.io.IOException;

public class CreatePage {
    @FXML private TextField title;
    @FXML private TextArea description;
    @FXML private TextField endTime;
    @FXML private TextField startPrice;
    @FXML private TextField increment;
    @FXML private TextField buyItNow;
    @FXML private Button createButton;

    private final ItemService itemService = new ItemService();

    @FXML public void back() throws IOException {
        SceneManager.changeContent("/fxml/mySaleTab.fxml");
    }
    @FXML public void create(){
        FormValues values = validateForm();
        if (values == null) {
            return;
        }

        setSubmitting(true);
        itemService.createItem(
                values.title,
                values.description,
                String.valueOf(values.durationMinutes),
                String.valueOf(values.startingPrice),
                String.valueOf(values.bidIncrement),
                String.valueOf(values.buyItNowPrice),
                new ItemCallback() {
                    @Override
                    public void onSuccess(BaseItemResponse response) {
                        Platform.runLater(() -> {
                            try {
                                SceneManager.changeContent("/fxml/mySaleTab.fxml");
                                String itemTitle = response.item == null ? values.title : response.item.title;
                                AppPopup.info("Created item: " + itemTitle);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            } finally {
                                setSubmitting(false);
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
                }
        );
    }

    private FormValues validateForm() {
        String titleValue = text(title).trim();
        String descriptionValue = text(description).trim();

        if (titleValue.isBlank()) {
            return reject(title, "Title is required");
        }
        if (descriptionValue.isBlank()) {
            return reject(description, "Description is required");
        }

        Long durationMinutes = parseDuration(text(endTime), endTime, "Duration must be a whole number of minutes");
        if (durationMinutes == null) {
            return null;
        }
        if (durationMinutes <= 0) {
            return reject(endTime, "Duration must be positive");
        }

        Double startingPrice = parseMoney(text(startPrice), startPrice, "Starting price must be a valid number");
        if (startingPrice == null) {
            return null;
        }
        Double bidIncrement = parseMoney(text(increment), increment, "Bid increment must be a valid number");
        if (bidIncrement == null) {
            return null;
        }
        Double buyItNowPrice = parseMoney(text(buyItNow), buyItNow, "Buy it now price must be a valid number");
        if (buyItNowPrice == null) {
            return null;
        }

        if (startingPrice <= 0) {
            return reject(startPrice, "Starting price must be positive");
        }
        if (bidIncrement <= 0) {
            return reject(increment, "Bid increment must be positive");
        }
        if (buyItNowPrice <= 0) {
            return reject(buyItNow, "Buy it now price must be positive");
        }
        if (buyItNowPrice < startingPrice) {
            return reject(buyItNow, "Buy it now price must be at least the starting price");
        }

        return new FormValues(titleValue, descriptionValue, durationMinutes, startingPrice, bidIncrement, buyItNowPrice);
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText();
    }

    private String text(TextArea field) {
        return field.getText() == null ? "" : field.getText();
    }

    private Long parseDuration(String value, TextField field, String message) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return reject(field, message);
        }
    }

    private Double parseMoney(String value, TextField field, String message) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (!Double.isFinite(parsed)) {
                return reject(field, message);
            }
            return parsed;
        } catch (NumberFormatException e) {
            return reject(field, message);
        }
    }

    private <T> T reject(javafx.scene.Node field, String message) {
        AppPopup.error(message);
        field.requestFocus();
        return null;
    }

    private void setSubmitting(boolean submitting) {
        createButton.setDisable(submitting);
        createButton.setText(submitting ? "Creating..." : "Create auction");
    }

    private record FormValues(
            String title,
            String description,
            long durationMinutes,
            double startingPrice,
            double bidIncrement,
            double buyItNowPrice
    ) {
    }
}
