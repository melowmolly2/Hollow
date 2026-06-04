import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class AuctionLauncher extends Application{
    public void start(Stage stage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/landingPage.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 980, 620);

        stage.setScene(scene);
        stage.setTitle("Ponzi Auction");
        stage.setMinWidth(900);
        stage.setMinHeight(580);
        stage.getIcons().add(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/appIcon.png")))
        );
        stage.show();
    }

}
