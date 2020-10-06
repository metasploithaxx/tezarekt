import com.mongodb.client.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.bson.Document;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class LoginController implements Initializable {
    public static String curr_username;
    @FXML
    private Label status_id;
    @FXML
    private Button login_btn,register_btn;
    @FXML
    private TextField username_id,password_id;
    @FXML
    private CheckBox check_id;
    @FXML
    private ProgressIndicator loader_id;
    public static Preferences preferences;
    public void Register() throws IOException {
        Parent root= FXMLLoader.load(getClass().getResource("Register.fxml"));
        Scene scene = new Scene(root,600,400);
        Stage primaryStage = new Stage();
        primaryStage.setScene(scene);
        primaryStage.setTitle("Register page");
        primaryStage.show();
    }

    public void Login(javafx.event.ActionEvent actionEvent) {
        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
        loader_id.setVisible(true);
        Task<Boolean> task =new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
                    MongoDatabase database = mongoClient.getDatabase("Softa");
                    MongoCollection<Document> collection = database.getCollection("users");
                    Document query = new Document("username", username_id.getText().toString());
                    FindIterable<Document> cursor = collection.find(query);
                    Iterator it = cursor.iterator();
                    if (it.hasNext()) {
                        Document found = collection.find(query).first();
                        String val = found.get("password").toString();
                        final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
                        final byte[] hashbytes = digest.digest(
                                password_id.getText().getBytes(StandardCharsets.UTF_8));
                        String sha3Hex = Base64.getEncoder().encodeToString(hashbytes);
                        if (sha3Hex.equals(val)) {
                            System.out.println("correct login");
                            curr_username = username_id.getText();
                            preferences = Preferences.userRoot();
                            if (check_id.isSelected()) {
                                preferences.put("username", username_id.getText().toString());
                                preferences.put("password", password_id.getText().toString());
                            }
                            return true;
                        } else {
                            System.out.println("Incorrect Password");
                            return false;
                        }
                    } else {
                        System.out.println("Incorrect Username");
                        return false;
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    return false;
                }
            }
        };
        Thread thread = new Thread(task);
        thread.start();

        task.setOnSucceeded(res-> {
            loader_id.setVisible(false);
            if(task.getValue()==true){
                Parent root = null;
                try {
                    Stage stage = (Stage) login_btn.getScene().getWindow();
                    stage.close();
                    root = FXMLLoader.load(getClass().getResource("MainPage.fxml"));
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
                Scene scene = new Scene(root, 1050, 750);
                Stage primaryStage = new Stage();
                primaryStage.setScene(scene);
                primaryStage.setTitle("Home page");
                primaryStage.show();
            }
            else{
                System.out.println("@@");
                status_id.setText("Incorrect username or Password");
                status_id.setTextFill(Color.RED);
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        preferences = Preferences.userRoot();
        username_id.setText(preferences.get("username",""));
        password_id.setText(preferences.get("password",""));
    }
}