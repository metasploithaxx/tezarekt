import com.mongodb.client.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import org.bson.Document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegisterController {
    @FXML
    private TextField username_id,password_id,confirmpd_id,firstname_id,lastname_id,briefinfo_id;
    @FXML
    private Button register_btn;
    @FXML
    private Label status_id;

    public void Register(ActionEvent event) throws Exception {
        System.out.println("enter Register");
        if(!username_id.getText().isEmpty() && !firstname_id.getText().isEmpty() && !lastname_id.getText().isEmpty() && !password_id.getText().isEmpty()){
            Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
            status_id.setText("Starting the registration process");
            status_id.setTextFill(Color.GREEN);
            if(password_id.getText().equals(confirmpd_id.getText())) {
                final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
                final byte[] hashbytes = digest.digest(
                        password_id.getText().getBytes(StandardCharsets.UTF_8));
                String sha3Hex = Base64.getEncoder().encodeToString(hashbytes);

                try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
                    status_id.setText("Please wait while registering");
                    status_id.setTextFill(Color.GREEN);
                    MongoDatabase database = mongoClient.getDatabase("Softa");
                    MongoCollection<Document> collection = database.getCollection("users");
                    Document query =new Document("username",username_id.getText().toString());
                    FindIterable<Document> cursor=collection.find(query);
                    Iterator it = cursor.iterator();
                    if(it.hasNext()){
                        status_id.setText("Username already taken");
                        status_id.setTextFill(Color.RED);
                    }
                    else {
                        Document root = new Document();
                        root.append("username", username_id.getText().toString())
                                .append("firstname", firstname_id.getText().toString())
                                .append("lastname", lastname_id.getText().toString())
                                .append("password", sha3Hex);
                        if(!briefinfo_id.getText().isEmpty()){
                            root.append("intro",briefinfo_id.getText().toString());
                        }
                        collection.insertOne(root);
                        status_id.setText("Successfully Registered");
                        status_id.setTextFill(Color.GREEN);

                    }

                } catch (Exception e) {
                    status_id.setText(e.getMessage());
                    status_id.setTextFill(Color.RED);
                    System.out.println("somet@@!!");
                }
            }
            else{
                System.out.println("somethi!!");
                status_id.setText("You have entered wrong confirmed password");
            }
        }
        else{
            System.out.println("something missing");
            status_id.setText("You have miss something");
        }
    }


}
