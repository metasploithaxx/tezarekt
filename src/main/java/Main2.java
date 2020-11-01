import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main2 extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        Parent root= FXMLLoader.load(getClass().getResource("ScheduleStreamDialog.fxml"));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("css/stylesheet.css").toString());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Video");
        primaryStage.show();

    }
}
