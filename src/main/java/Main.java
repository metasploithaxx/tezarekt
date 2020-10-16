import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class Main extends Application {
    public static String Connectingurl="http://[::1]:3000";
//    public static String MongodbId="mongodb+srv://manishkumar13899:manu@cluster0.xjkfy.mongodb.net/test?retryWrites=true&w=majority";
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root= FXMLLoader.load(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(root,500,325);
        scene.getStylesheets().add(getClass().getResource("css/stylesheet.css").toString());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Login page");
        primaryStage.show();

    }
}
