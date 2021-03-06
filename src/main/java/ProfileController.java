import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.*;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ProfileController implements Initializable {
    @FXML
    private TextField firstname_id,lastname_id,instagram_id,twitter_id,subsrate_id;
    @FXML
    private Label username_id;
    @FXML
    private JFXTextArea intro_id;
    @FXML
    private JFXButton update_btn;

    private JFXSnackbar status_id;
    @FXML
    private CheckBox instagram_check,twitter_check;
    @FXML
    private JFXSpinner loader_id;
    @FXML
    private Circle image_id;
    @FXML
    private AnchorPane rootPane;


    public void UpdateData(ActionEvent actionEvent){

        if(! (username_id.getText().length()>0 && firstname_id.getText().length()>0 && lastname_id.getText().length()>0 && subsrate_id.getText().length()>0 )){
            status_id.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Fill all Details")));
            return ;
        }
        loader_id.setVisible(true);
        Task<HttpResponse> task =new Task<>() {
            @Override
            protected HttpResponse call() throws Exception {
                var values = new HashMap<String, String>() {{
                    put("uname", LoginController.curr_username);
                    put("fname",firstname_id.getText());
                    put("lname",lastname_id.getText());
                    put("instaid",instagram_id.getText());
                    put("twitterid",twitter_id.getText());
                    put("subsrate",subsrate_id.getText());
                    put("bio",intro_id.getText());
                    put("isinstaidpublic", String.valueOf(instagram_check.isSelected()));
                    put("istwitteridpublic",String.valueOf(twitter_check.isSelected()));
                }};

                var objectMapper = new ObjectMapper();
                String payload =
                        objectMapper.writeValueAsString(values);

                StringEntity entity = new StringEntity(payload,
                        ContentType.APPLICATION_JSON);

                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpPost request = new HttpPost(Main.Connectingurl+"/updateUserProfile");
                request.setEntity(entity);
                request.setHeader("Content-Type", "application/json; charset=UTF-8");
                Future<HttpResponse> future = client.execute(request, null);
                while(!future.isDone());
                return future.get();
            }
        };
        Thread th=new Thread(task);
        th.start();
        task.setOnSucceeded(res->{
            loader_id.setVisible(false);
            if(task.isDone()) {
                try {
                    String jsonString = EntityUtils.toString(task.get().getEntity());
                    status_id.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout(jsonString)));
                } catch (InterruptedException | ExecutionException | IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                status_id.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Unsuccessful attempt")));
            }
        });
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        update_btn.setDisable(true);
        loader_id.setVisible(true);
        status_id=new JFXSnackbar(rootPane);
        Task<HttpResponse> task =new Task<>() {
            @Override
            protected HttpResponse call() throws Exception {

                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpGet request = new HttpGet(Main.Connectingurl+"/profile/self/"+LoginController.curr_username);
                Future<HttpResponse> future = client.execute(request, null);
                while(!future.isDone());
                return future.get();
            }
        };
            Thread th=new Thread(task);
            th.start();
        task.setOnSucceeded(res->{
            loader_id.setVisible(false);
            if(task.isDone()) {
                try {
                    String jsonString = EntityUtils.toString(task.get().getEntity());
                    JSONObject myResponse = new JSONObject(jsonString);
                    if (task.get().getStatusLine().getStatusCode() == 200) {
                        status_id.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Successfully Loaded")));
                        username_id.setText(myResponse.getString("uname"));
                        firstname_id.setText(myResponse.getString("fname"));
                        lastname_id.setText(myResponse.getString("lname"));
                        instagram_id.setText(myResponse.getString("instaid"));
                        twitter_id.setText(myResponse.getString("twitterid"));
                        subsrate_id.setText(myResponse.getString("subsrate"));
                        intro_id.setText(myResponse.getString("bio"));

                        if(!myResponse.getString("isinstaidpublic").equals("null")){
                            if(myResponse.getString("isinstaidpublic").equals("true")){
                                instagram_check.setSelected(true);
                            }
                            else{
                                instagram_check.setSelected(false);
                            }
                        }
                        else{
                            instagram_check.setSelected(false);
                        }
                        if(!myResponse.getString("istwitteridpublic").equals("null")){
                            if(myResponse.getString("istwitteridpublic").equals("true")){
                                twitter_check.setSelected(true);
                            }
                            else{
                                twitter_check.setSelected(false);
                            }
                        }
                        else{
                            twitter_check.setSelected(false);
                        }
                    } else {

                        System.out.println(myResponse.getString("detail"));
                        status_id.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("statusCode- "+myResponse.getString("detail"))));
                    }
                } catch (InterruptedException | ExecutionException | IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                status_id.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Unsuccessful attempt")));
            }
        });

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
        subsrate_id.textProperty().addListener((observable, oldValue, newValue) -> {
            update_btn.setDisable(false);
        });
        update_btn.setDisable(true);

    }

    public void setImage_id(Paint image) {
        this.image_id.setFill(image);
    }

    public void changePhoto() throws IOException{
        Parent root= FXMLLoader.load(getClass().getResource("ProfileImage.fxml"));
        Scene scene = new Scene(root);
        Stage primaryStage = new Stage();
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
