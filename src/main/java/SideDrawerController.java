import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXToggleButton;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class SideDrawerController implements Initializable {
    @FXML
    private Circle image_id;
    @FXML
    private JFXSpinner load_id;
    @FXML
    private JFXButton profile_page,start_stream;
    @FXML
    private Label username_id;
    @FXML
    private JFXToggleButton toggle_id;
    private Image image;
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
    public JFXButton getStart_stream() {
        return start_stream;
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

            Future<HttpResponse> future=null;
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
                    CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                    client.start();
                    HttpGet request = new HttpGet(Main.Connectingurl+"/getStatus/"+LoginController.curr_username);
                    future = client.execute(request, null);
                    while(!future.isDone());
                }
                catch (Exception e){
                    System.out.println(e.getMessage());
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        image_id.setFill(new ImagePattern(image));
                        load_id.setVisible(false);

                        try {
                            String jsonString = EntityUtils.toString(future.get().getEntity());
                            if (future.get().getStatusLine().getStatusCode() == 200){
                                if(jsonString.equals("true")){
                                    toggle_id.setSelected(true);
                                    toggle_id.setText("Online");

                                    toggle_id.setTextFill(Color.GREEN);
                                }
                                else{
                                    toggle_id.setText("Offline");
                                    toggle_id.setTextFill(Color.GRAY);
                                    toggle_id.setSelected(false);
                                }

                            }
                        } catch (IOException | ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }.start();
    }
    public void setStatus(){
        new Thread(){
            Future<HttpResponse> future=null;
            @Override
            public void run() {
                super.run();
                var values = new HashMap<String, String>() {{
                    put("uname",LoginController.curr_username);
                }};

                var objectMapper = new ObjectMapper();
                String payload =
                        null;
                try {
                    payload = objectMapper.writeValueAsString(values);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                StringEntity entity = new StringEntity(payload,
                        ContentType.APPLICATION_JSON);

                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpPost request = new HttpPost(Main.Connectingurl+"/setStatus");
                request.setEntity(entity);
                request.setHeader("Content-Type", "application/json; charset=UTF-8");
                future = client.execute(request, null);

                while(!future.isDone());
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if(future.isDone()){
                            String jsonString = null;
                            try {
                                jsonString = EntityUtils.toString(future.get().getEntity());
                                if (future.get().getStatusLine().getStatusCode() == 200) {
                                    if(jsonString.equals("true")){
                                        toggle_id.setText("Online");
                                        toggle_id.setTextFill(Color.GREEN);
                                    }
                                    else{

                                        toggle_id.setTextFill(Color.GRAY);
                                        toggle_id.setText("Offline");
                                    }
                                }
                            } catch (IOException | InterruptedException | ExecutionException  e) {
                                e.printStackTrace();
                            }
                            System.out.println(jsonString);

                        }
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
        load_id.setVisible(true);
            Task<HttpResponse> task =new Task<>() {
                @Override
                protected HttpResponse call() throws Exception {
                    var values = new HashMap<String, String>() {{
                        put("uname", LoginController.curr_username);
                    }};

                    var objectMapper = new ObjectMapper();
                    String payload =
                            objectMapper.writeValueAsString(values);

                    StringEntity entity = new StringEntity(payload,
                            ContentType.APPLICATION_JSON);

                    CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                    client.start();
                    HttpPost request = new HttpPost(Main.Connectingurl+"/signout");
                    request.setEntity(entity);
                    request.setHeader("Content-Type", "application/json; charset=UTF-8");
                    Future<HttpResponse> future = client.execute(request, null);

                    while(!future.isDone());
                    return future.get();
                }
            };
            Thread thread = new Thread(task);
            thread.start();
            task.setOnSucceeded(res->{
                load_id.setVisible(false);
                Stage stage = (Stage) image_id.getScene().getWindow();

                Parent root= null;
                try {
                    root = FXMLLoader.load(getClass().getResource("Login.fxml"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("css/stylesheet.css").toString());
                Stage primaryStage = new Stage(StageStyle.UNDECORATED);
                primaryStage.setScene(scene);
                primaryStage.setResizable(false);
                primaryStage.show();
                stage.close();
            });

    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        mongoLogger.setLevel(Level.SEVERE);
        username_id.setText("Hello "+LoginController.curr_username);
//        username_id.setTextFill(Color.BLUEVIOLET);
        init();
        toggle_id.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                setStatus();
            }
        });
    }


    public void viewTopicofInterest(ActionEvent actionEvent){
        FXMLLoader loader=new FXMLLoader(getClass().getResource("ChooseTOI.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scene scene = new Scene(root,600,400);
        Stage primaryStage = new Stage();
        primaryStage.initModality(Modality.APPLICATION_MODAL);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Topic of Interest Page");
        primaryStage.showAndWait();
    }

    public javafx.scene.paint.Paint getImage(){
        return image_id.getFill();
    }

}
