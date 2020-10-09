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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class HomeController implements Initializable {
    @FXML
    private ImageView image_id;
    @FXML
    private JFXSpinner load_id;
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
    public void ProfilePic1(ActionEvent actionEvent) throws IOException {
        Parent root= FXMLLoader.load(getClass().getResource("ProfileImage.fxml"));
        Scene scene = new Scene(root,540,400);
        Stage primaryStage = new Stage();
        primaryStage.setScene(scene);
        primaryStage.setTitle("ProfilePictureImage");
        primaryStage.show();
    }
    public void ProfilePage(ActionEvent actionEvent) throws IOException {
        Parent root= FXMLLoader.load(getClass().getResource("Profile.fxml"));
        Scene scene = new Scene(root,600,400);
        Stage primaryStage = new Stage();
        primaryStage.setScene(scene);
        primaryStage.setTitle("Profile page");
        primaryStage.show();
    }
    public void init(){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
                    MongoDatabase database = mongoClient.getDatabase("Photos");
                    GridFSBucket gridBucket = GridFSBuckets.create(database);
                    GridFSDownloadStream gdifs = gridBucket.openDownloadStream(LoginController.curr_username);
                    byte[] data = gdifs.readAllBytes();
                    ByteArrayInputStream input = new ByteArrayInputStream(data);
                    BufferedImage image1 = ImageIO.read(input);
                    Image image = SwingFXUtils.toFXImage(image1, null);
                    image_id.setImage(image);
                }
                catch (Exception e){
                    System.out.println(e.getMessage());
                }

            }
        });
    }
    public void Logout(ActionEvent actionEvent) throws IOException {
        Preferences preferences ;
        preferences = Preferences.userRoot();
        preferences.put("username", "");
        preferences.put("password","");
        Stage stage = (Stage) image_id.getScene().getWindow();

        Parent root= FXMLLoader.load(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(root,500,325);
        Stage primaryStage = new Stage();
        primaryStage.setScene(scene);
        primaryStage.setTitle("Login page");
        primaryStage.show();
        stage.close();
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        init();
    }
}
