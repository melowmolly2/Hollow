package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class CardController {
    @FXML
    private Label nameLabel;

    public void setDetail(String name){
        nameLabel.setText(name);
    }

    @FXML
    public Button bidScreen(ActionEvent event){

    }
}
