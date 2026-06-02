package controller.app;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class SceneManager  {
    private static StackPane contentArea;

    public static void changeScene(ActionEvent event, String fxml) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxml));
        Scene currentScene = stage.getScene();
        Scene scene = new Scene(loader.load(), currentScene.getWidth(), currentScene.getHeight());

        stage.setScene(scene);
        stage.show();
    }

    public static void setContentArea(StackPane area) {
        contentArea = area;
    }

    public static void changeContent(String fxml) throws IOException {
        if (contentArea == null) {
            throw new IllegalStateException("Content area has not been initialized");
        }

        Parent view = FXMLLoader.load(Objects.requireNonNull(SceneManager.class.getResource(fxml)));
        changeContent(view);
    }

    public static void changeContent(Parent view) {
        if (contentArea == null) {
            throw new IllegalStateException("Content area has not been initialized");
        }

        contentArea.getChildren().setAll(view);
    }
}
