import com.mongodb.client.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

public class MainController implements Initializable {

    @FXML
    private Label status_id;
    @FXML
    private Button login_btn,register_btn;
    @FXML
    private TextField username_id,password_id;
    @FXML
    private CheckBox check_id;
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

        try(MongoClient mongoClient = MongoClients.create(Main.MongodbId)){
            MongoDatabase database = mongoClient.getDatabase("Softa");
            MongoCollection<Document> collection = database.getCollection("users");
            Document query =new Document("username",username_id.getText().toString());
            FindIterable<Document> cursor=collection.find(query);
            Iterator it = cursor.iterator();
            if(it.hasNext()){
                Document found = collection.find(query).first();
                String val= found.get("password").toString();
                final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
                final byte[] hashbytes = digest.digest(
                        password_id.getText().getBytes(StandardCharsets.UTF_8));
                String sha3Hex = Base64.getEncoder().encodeToString(hashbytes);
                if(sha3Hex.equals(val)){
                    status_id.setText("Login Successful");
                    System.out.println("correct login");
                     preferences = Preferences.userRoot();
                    if(check_id.isSelected()){
                        preferences.put("username",username_id.getText().toString());
                        preferences.put("password",password_id.getText().toString());
                    }
                    status_id.setText("Login Successful");
                }
                else{
                    status_id.setText("Incorrect Login");
                    System.out.println("Incorrect Login");
                }
            }
            else{
                status_id.setText("No such username Exits");
                System.out.println("No such user exits");
            }
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        preferences = Preferences.userRoot();
        username_id.setText(preferences.get("username",""));
        password_id.setText(preferences.get("password",""));
    }
}