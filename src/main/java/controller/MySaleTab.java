package controller;

import javafx.fxml.FXML;

import java.io.IOException;

// Note: Seller tab entry point.
public class MySaleTab {
    @FXML public void create() throws IOException {
        SceneManager.changeContent("/fxml/createPage.fxml");
    }
}
