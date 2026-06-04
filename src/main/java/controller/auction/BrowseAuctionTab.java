package controller.auction;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import dto.auction.GetItemPageResponse;
import dto.auction.ItemResponse;
import dto.auction.ItemStatusResponse;
import network.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

// Note: Loads active auctions and handles simple page navigation.
public class BrowseAuctionTab {
    private static final int PAGE_SIZE = 10;

    @FXML private VBox cardList;
    @FXML private Label statusLabel;
    @FXML private Label pageLabel;
    @FXML private Button previousButton;
    @FXML private Button nextButton;

    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.US);
    private int currentPage = 0;
    private int totalPages = 0;

    public void initialize() {
        currencyFormat.setMaximumFractionDigits(2);
        loadPage(0);
    }

    @FXML
    private void previousPage() {
        if (currentPage > 0) {
            loadPage(currentPage - 1);
        }
    }

    @FXML
    private void nextPage() {
        if (currentPage + 1 < totalPages) {
            loadPage(currentPage + 1);
        }
    }

    @FXML
    private void refresh() {
        loadPage(currentPage);
    }

    private void loadPage(int page) {
        setLoadingState(true, "Loading active auctions...");
        // Note: API callback returns on a worker thread, so UI updates use Platform.runLater.
        ApiClient.api.getItems(page, PAGE_SIZE).enqueue(new Callback<GetItemPageResponse>() {
            @Override
            public void onResponse(Call<GetItemPageResponse> call, Response<GetItemPageResponse> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().pages == null) {
                    Platform.runLater(() -> showError("Cannot load auctions. HTTP " + response.code()));
                    return;
                }

                Platform.runLater(() -> renderPage(response.body().pages));
            }

            @Override
            public void onFailure(Call<GetItemPageResponse> call, Throwable throwable) {
                Platform.runLater(() -> showError("Network error: " + throwable.getMessage()));
            }
        });
    }

    private void renderPage(GetItemPageResponse.PageData page) {
        currentPage = page.number;
        totalPages = page.totalPages;
        cardList.getChildren().clear();

        List<ItemResponse> items = page.content;
        if (items == null || items.isEmpty()) {
            statusLabel.setText("No active auctions found.");
        } else {
            statusLabel.setText(items.size() + " active auctions");
            for (ItemResponse item : items) {
                addCard(item);
            }
        }

        pageLabel.setText("Page " + (currentPage + 1) + " of " + Math.max(totalPages, 1));
        previousButton.setDisable(currentPage <= 0);
        nextButton.setDisable(currentPage + 1 >= totalPages);
    }

    private void addCard(ItemResponse item) {
        try {
            // Note: Each auction row is rendered from the shared card component.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/card.fxml"));
            VBox card = loader.load();
            Card controller = loader.getController();
            controller.setItem(item);
            cardList.getChildren().add(card);
            loadStatus(item.itemId, controller);
        } catch (IOException exception) {
            statusLabel.setText("Cannot load auction card.");
        }
    }

    private void openBidderView(ItemResponse item) {
        AuctionViewContext.selectedItem = item;
        AuctionViewContext.selectedMode = AuctionViewContext.Mode.BIDDER;
    }

    private void loadStatus(Long itemId, Card card) {
        if (itemId == null) {
            updateStatusLabels(card, null);
            return;
        }

        // Note: Status is loaded separately so the list can render first.
        ApiClient.api.getItemStatus(itemId).enqueue(new Callback<ItemStatusResponse>() {
            @Override
            public void onResponse(Call<ItemStatusResponse> call, Response<ItemStatusResponse> response) {
                ItemStatusResponse.ItemStatusData itemStatus = response.body() == null ? null : response.body().itemStatus;
                Platform.runLater(() -> updateStatusLabels(card, itemStatus));
            }

            @Override
            public void onFailure(Call<ItemStatusResponse> call, Throwable throwable) {
                Platform.runLater(() -> updateStatusLabels(card, null));
            }
        });
    }

    private void updateStatusLabels(Card card, ItemStatusResponse.ItemStatusData status) {
        if (status == null) {
            card.setStatus("unavailable", "unavailable", "unavailable");
            return;
        }

        card.setStatus(
                formatMoney(displayCurrentPrice(status)),
                formatTimeLeft(status.endTime),
                valueOrFallback(status.itemStatus, "Unknown") + " | Step " + formatMoney(status.bidIncrement)
        );
    }

    private Double displayCurrentPrice(ItemStatusResponse.ItemStatusData status) {
        if (status == null) {
            return null;
        }
        return Math.max(safeMoney(status.currentPrice), safeMoney(status.startingPrice));
    }

    private double safeMoney(Double value) {
        return value == null ? 0.0 : value;
    }

    private String formatMoney(Double value) {
        if (value == null) {
            return "-";
        }
        return currencyFormat.format(value);
    }

    private String formatTimeLeft(Long endTime) {
        if (endTime == null) {
            return "-";
        }

        // Note: Backend stores end time as epoch milliseconds.
        long millisLeft = endTime - Instant.now().toEpochMilli();
        if (millisLeft <= 0) {
            return "Ended";
        }

        Duration duration = Duration.ofMillis(millisLeft);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return Math.max(minutes, 1) + "m";
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void setLoadingState(boolean loading, String message) {
        statusLabel.setText(message);
        previousButton.setDisable(loading || currentPage <= 0);
        nextButton.setDisable(loading || currentPage + 1 >= totalPages);
    }

    private void showError(String message) {
        cardList.getChildren().clear();
        statusLabel.setText(message);
        pageLabel.setText("Page -");
        previousButton.setDisable(true);
        nextButton.setDisable(true);
    }
}
