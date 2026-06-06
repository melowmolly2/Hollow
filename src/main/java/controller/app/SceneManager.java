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
    private static Runnable mySaleNavigationSelector;
    private static Runnable contentCleanup;

    public static void changeScene(ActionEvent event, String fxml) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxml));
        Scene currentScene = stage.getScene();
        Scene scene = new Scene(loader.load(), currentScene.getWidth(), currentScene.getHeight());

        disposeCurrentContent();
        stage.setScene(scene);
        stage.show();
    }

    public static void setContentArea(StackPane area) {
        contentArea = area;
    }

    public static void setMySaleNavigationSelector(Runnable selector) {
        mySaleNavigationSelector = selector;
    }

    public static void selectMySaleNavigation() {
        if (mySaleNavigationSelector != null) {
            mySaleNavigationSelector.run();
        }
    }

    public static void changeContent(String fxml) throws IOException {
        if (contentArea == null) {
            throw new IllegalStateException("Content area has not been initialized");
        }

        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(SceneManager.class.getResource(fxml)));
        Parent view = loader.load();
        Object controller = loader.getController();
        Runnable cleanup = controller instanceof ContentLifecycle lifecycle ? lifecycle::dispose : null;
        changeContent(view, cleanup);
    }

    public static void changeContent(Parent view) {
        changeContent(view, null);
    }

    public static void changeContent(Parent view, Runnable cleanup) {
        if (contentArea == null) {
            throw new IllegalStateException("Content area has not been initialized");
        }

        disposeCurrentContent();
        contentArea.getChildren().setAll(view);
        contentCleanup = cleanup;
    }

    private static void disposeCurrentContent() {
        if (contentCleanup == null) {
            return;
        }

        Runnable cleanup = contentCleanup;
        contentCleanup = null;
        cleanup.run();
    }
}
