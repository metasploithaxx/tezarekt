import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextArea;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pojo.OnlineUser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OnlineUsersListController implements Initializable {
    public JFXListView<OnlineUser> onlineuserslist;
    public Label uname_id, fname_id, lname_id, subscost_id;
    public ImageView image_view_id;
    private JFXButton profile_btn;
    public JFXTextArea bio_id;
    private JFXSpinner mainPageLoader;
    public AnchorPane content;
    @FXML
    private JFXSpinner loader_id;
    Parent rtview = null;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loader_id.setVisible(true);
        Timeline fiveSecondsWonder = new Timeline(
                new KeyFrame(Duration.seconds(4.5),
                        event -> {init();}));
        fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
        fiveSecondsWonder.play();
    }
    public void setContent(AnchorPane content){
        this.content = content;
    }
    public void setLoaderID(JFXSpinner load_id){
        this.mainPageLoader = load_id;
    }
    private void SearchUser(String uname){
        System.out.println(uname+"profile page");

        if(uname.length()>0) {
            mainPageLoader.setVisible(true);
            FXMLLoader loaderView = new FXMLLoader(getClass().getResource("viewUserProfile.fxml"));

            try {
                rtview = loaderView.load();
                ViewUserProfileController viewUserProfileController = loaderView.getController();
                bio_id = viewUserProfileController.getBio_id();
                uname_id = viewUserProfileController.getUname_id();
                fname_id = viewUserProfileController.getFname_id();
                lname_id = viewUserProfileController.getLname_id();
                subscost_id = viewUserProfileController.getSubscost_id();
                image_view_id = viewUserProfileController.getImage_view_id();

            } catch (IOException e) {
                e.printStackTrace();
            }
            Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
            mongoLogger.setLevel(Level.SEVERE);


            new Thread(){
                String jsonString = null;
                Future<HttpResponse> future = null;
                public void run(){
                    try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
                        MongoDatabase database = mongoClient.getDatabase("Photos");
                        GridFSBucket gridBucket = GridFSBuckets.create(database);
                        GridFSDownloadStream gdifs = gridBucket.openDownloadStream(uname);
                        byte[] data = gdifs.readAllBytes();
                        ByteArrayInputStream input = new ByteArrayInputStream(data);
                        BufferedImage image1 = ImageIO.read(input);
                        Image image = SwingFXUtils.toFXImage(image1, null);
                        image_view_id.setImage(image);
                        CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                        client.start();
                        HttpGet request = new HttpGet(Main.Connectingurl + "/profile/user/"+ uname);

                        future = client.execute(request, null);
                        while (!future.isDone()) ;

                        jsonString = EntityUtils.toString(future.get().getEntity());

                    }
                    catch (Exception e){
                        System.out.println(e.getMessage());
                    }
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
//                            mainPageLoader.visibleProperty().bind(isSaving);
                            try {

                                JSONObject myResponse = new JSONObject(jsonString);
                                if (future.get().getStatusLine().getStatusCode() == 200) {
                                    uname_id.setText(myResponse.getString("uname"));
                                    fname_id.setText(myResponse.getString("fname"));
                                    lname_id.setText(myResponse.getString("lname"));
                                    bio_id.setText(myResponse.getString("bio"));
                                    content.getChildren().setAll(rtview);

                                }
                                else{
                                    System.out.println(future.get().getStatusLine());
                                }

                            } catch ( InterruptedException | ExecutionException | JSONException e) {
                                e.printStackTrace();
                            }
                            mainPageLoader.setVisible(false);
                        }
                    });
                }
            }.start();

        }
    }
    private void init(){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpGet request = new HttpGet(Main.Connectingurl+"/onlineUsers");
                Future<HttpResponse> future = client.execute(request, null);
                HttpResponse res = null;
                while(!future.isDone());
                try {
                    res = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                ObservableList<OnlineUser> list= FXCollections.observableArrayList();
                String jsonList = null;
                try {
                    jsonList = EntityUtils.toString(res.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                JSONArray ResponseList = null;
                try {
                    ResponseList = new JSONArray(jsonList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                for(int i=0;i< ResponseList.length();i++) {
                     OnlineUser users = null;
                    try {
                        users = new OnlineUser(ResponseList.getJSONObject(i).getString("uname"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    list.add(users);
                }

                onlineuserslist.setItems(list);
                onlineuserslist.setCellFactory(userListView->{
                    OnlineUsersListCellController onlineUsersListCellController = new OnlineUsersListCellController();
                    onlineUsersListCellController.setOnMouseClicked(event -> {
                        System.out.println("Mouse Clicked");
                        if(onlineUsersListCellController.uname_id.getText().equals("You")){
                            SearchUser(LoginController.curr_username);
                        }
                        else{
                            SearchUser(onlineUsersListCellController.uname_id.getText());
                        }

                    });
                    return onlineUsersListCellController;
                });
                loader_id.setVisible(false);
            }
        });
    }
}
