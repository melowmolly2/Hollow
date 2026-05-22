package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import model.response.ItemResponse;

// Note: Small reusable view for one auction item.
public class Card {
    @FXML private Label itemLabel;
    @FXML private Label priceLabel;
    @FXML private Label timeLabel;
    @FXML private Label infoLabel;
    @FXML private Button viewButton;

    private Long itemId;

    public void initialize() {
        viewButton.setDisable(true);
    }

    public void setItem(ItemResponse item) {
        // Note: Static item data is shown first; live status is filled later.
        itemId = item.itemId;
        itemLabel.setText("Item: " + valueOrFallback(item.title, "Untitled")
                + " | ID " + item.itemId
                + " | " + valueOrFallback(item.description, "No description"));
        priceLabel.setText("Price: loading");
        timeLabel.setText("Time: loading");
        infoLabel.setText("Info: loading");
    }

    public void setStatus(String price, String time, String info) {
        // Note: Status text is kept simple for quick scanning.
        priceLabel.setText("Price: " + price);
        timeLabel.setText("Time: " + time);
        infoLabel.setText("Info: " + info);
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
