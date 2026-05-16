package controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.event.ActionEvent;


public class SceneManager {
    public static void changeScene(String fxml, ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxml));
        Stage stage = (Stage)
                ((Node) event.getSource())
                        .getScene()
                        .getWindow();
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);

    }
}
