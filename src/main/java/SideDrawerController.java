import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class SideDrawerController implements Initializable {
    @FXML
    private ImageView image_id;
    @FXML
    private JFXSpinner load_id;
    @FXML
    private JFXButton profile_page;
    @FXML
    private Label username_id;
    @FXML
    private boolean ProfilePic() throws ClassNotFoundException {
        try {
            Parent root= FXMLLoader.load(getClass().getResource("ProfileImage.fxml"));
            Scene scene = new Scene(root,540,400);
            Stage primaryStage = new Stage();
            primaryStage.setScene(scene);
            primaryStage.setTitle("ProfilePictureImage");
            primaryStage.show();
            return true;
        }
        catch(Exception e){
            System.out.println(e.getMessage());
            return false;
        }

    }
    public JFXButton getProfile_page(){
        return profile_page;
    }
    public void ProfilePic1(ActionEvent actionEvent) throws IOException {
        Parent root= FXMLLoader.load(getClass().getResource("ProfileImage.fxml"));
        Scene scene = new Scene(root,540,400);
        Stage primaryStage = new Stage();
        primaryStage.setScene(scene);
        primaryStage.setTitle("ProfilePictureImage");
        primaryStage.show();
    }

    public void init(){
        load_id.setVisible(true);
        Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        new Thread(){
            Image image=null;
            @Override
            public void run() {
                super.run();
                try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
                    MongoDatabase database = mongoClient.getDatabase("Photos");
                    GridFSBucket gridBucket = GridFSBuckets.create(database);
                    GridFSDownloadStream gdifs = gridBucket.openDownloadStream(LoginController.curr_username);
                    byte[] data = gdifs.readAllBytes();
                    ByteArrayInputStream input = new ByteArrayInputStream(data);
                    BufferedImage image1 = ImageIO.read(input);
                    image = SwingFXUtils.toFXImage(image1, null);

                }
                catch (Exception e){
                    System.out.println(e.getMessage());
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        image_id.setImage(image);
                        load_id.setVisible(false);
                    }
                });
            }
        }.start();

    }
    public void Logout(ActionEvent actionEvent) throws IOException {
        Preferences preferences ;
        preferences = Preferences.userRoot();
        preferences.put("username", "");
        preferences.put("password","");
        Stage stage = (Stage) image_id.getScene().getWindow();

        Parent root= FXMLLoader.load(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(root,500,325);
        scene.getStylesheets().add(getClass().getResource("css/stylesheet.css").toString());
        Stage primaryStage = new Stage();
        primaryStage.setScene(scene);
        primaryStage.setTitle("Login page");
        primaryStage.show();
        stage.close();
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        mongoLogger.setLevel(Level.SEVERE);
        username_id.setText("Hello "+LoginController.curr_username);
        username_id.setTextFill(Color.BLUEVIOLET);
        init();
    }
}
