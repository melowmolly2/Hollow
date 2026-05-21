package controller;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class Framework {
    @FXML private StackPane contentArea;

    @FXML public void dashboard() throws IOException {
        SceneManager.changeContent("/fxml/dashboardTab.fxml");
    }
    @FXML public void browse(){

    }
    @FXML public void mySale() throws IOException {
        SceneManager.changeContent("/fxml/mySaleTab.fxml");
    }
    @FXML public void watchlist() throws IOException {
        SceneManager.changeContent("/fxml/watchlistTab.fxml");
    }
    @FXML public void account(){

    }

    public void initialize() throws IOException {
        SceneManager.setContentArea(contentArea);
        SceneManager.changeContent("/fxml/dashboardTab.fxml");
    }
}
