import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import org.bson.Document;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfileController implements Initializable {
    @FXML
    private TextField username_id,firstname_id,lastname_id,country_id,instagram_id,twitter_id,intro_id;
    @FXML
    private Button update_btn;
    @FXML
    private Label followers_id,status_id;
    @FXML
    private CheckBox instagram_check,twitter_check;
    public void InputChange(ActionEvent actionEvent){
        System.out.println(actionEvent.getEventType().getName());
    }

    public void UpdateData(ActionEvent actionEvent){
        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);

        try(MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
            MongoDatabase database = mongoClient.getDatabase("Softa");
            MongoCollection<Document> collection = database.getCollection("users");
            collection.updateOne(Filters.eq("username",username_id.getText()), Updates.set("firstname",firstname_id.getText()));
            collection.updateOne(Filters.eq("username",username_id.getText()),Updates.set("lastname",lastname_id.getText()));
            collection.updateOne(Filters.eq("username",username_id.getText()),Updates.set("country_of_origin",country_id.getText()));
            collection.updateOne(Filters.eq("username",username_id.getText()),Updates.set("insta_sub_only",instagram_check.isSelected()));
            collection.updateOne(Filters.eq("username",username_id.getText()),Updates.set("twitter_sub_only",twitter_check.isSelected()));
            collection.updateOne(Filters.eq("username",username_id.getText()),Updates.set("intro",intro_id.getText()));
            collection.updateOne(Filters.eq("username",username_id.getText()),Updates.set("insta_id",instagram_id.getText()));
            collection.updateOne(Filters.eq("username",username_id.getText()),Updates.set("twitter_id",twitter_id.getText()));
            status_id.setText("Updated Successfully");
            status_id.setTextFill(Color.GREEN);
        }
        catch (Exception e){
            status_id.setText(e.getMessage());
            System.out.println(e.getMessage()+"$$");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        update_btn.setDisable(true);
        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
        try(MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
            MongoDatabase database = mongoClient.getDatabase("Softa");
            MongoCollection<Document> collection = database.getCollection("users");
            Document query = new Document("username",LoginController.curr_username);
            Document curr_info=collection.find(query).first();
            username_id.setText(LoginController.curr_username);
            firstname_id.setText(curr_info.getString("firstname"));
            lastname_id.setText(curr_info.getString("lastname"));
            if(curr_info.containsKey("intro")){
                intro_id.setText(curr_info.getString("intro"));
            }
            if(curr_info.containsKey("insta_id")){
                instagram_id.setText(curr_info.getString("insta_id"));
            }
            if(curr_info.containsKey("twitter_id")){
                twitter_id.setText(curr_info.getString("twitter_id"));
            }
            if(curr_info.containsKey("followers")){
                followers_id.setText(curr_info.getString("followers")+" followers");
            }
            else{
                followers_id.setText("0 followers");
            }
            if(curr_info.containsKey("country_of_origin")){
                country_id.setText(curr_info.getString("country_of_origin"));
            }
            if(curr_info.containsKey("insta_sub_only")){
                if(curr_info.getBoolean("insta_sub_only")){
                    instagram_check.setSelected(true);
                }
                else{
                    instagram_check.setSelected(false);
                }
            }
            else{
                instagram_check.setSelected(false);
            }
            if(curr_info.containsKey("twitter_sub_only")){
                if(curr_info.getBoolean("twitter_sub_only")){
                    twitter_check.setSelected(true);
                }
                else{
                    twitter_check.setSelected(false);
                }
            }
            else{
                twitter_check.setSelected(false);
            }
        }
        catch (MongoException e){
            System.out.println(e.getMessage()+"**");
            status_id.setText(e.getMessage());
        }
        firstname_id.textProperty().addListener((observable, oldValue, newValue) -> {
            update_btn.setDisable(false);
        });
        lastname_id.textProperty().addListener((observable, oldValue, newValue) -> {
            update_btn.setDisable(false);
        });
        instagram_id.textProperty().addListener((observable, oldValue, newValue) -> {
            update_btn.setDisable(false);
        });
        twitter_id.textProperty().addListener((observable, oldValue, newValue) -> {
            update_btn.setDisable(false);
        });
        intro_id.textProperty().addListener((observable, oldValue, newValue) -> {
            update_btn.setDisable(false);
        });
        country_id.textProperty().addListener((observable, oldValue, newValue) -> {
            update_btn.setDisable(false);
        });

    }
}
