package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

// Note: Central helper for full scene changes and inner tab swaps.
public class SceneManager  {
    private static StackPane contentArea;

    public static void changeScene(ActionEvent event, String fxml) throws IOException {
        // Note: Used by landing/login/register where the whole window changes.
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxml));
        Scene scene = new Scene(loader.load());

        stage.setScene(scene);
        stage.show();
    }

    public static void setContentArea(StackPane area) {
        contentArea = area;
    }

    public static void changeContent(String fxml) throws IOException {
        // Note: Used after login to replace only the main content panel.
        if (contentArea == null) {
            throw new IllegalStateException("Content area has not been initialized");
        }

        Parent view = FXMLLoader.load(Objects.requireNonNull(SceneManager.class.getResource(fxml)));
        contentArea.getChildren().setAll(view);
    }
}
