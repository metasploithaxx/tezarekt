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
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OnlineUsersListController implements Initializable {

    public JFXListView<OnlineUser> onlineuserslist;

    public Label online_status,uname_id, fname_id, lname_id, cost_id;

    public Circle image_view_id;

    private JFXButton profile_btn;

    public JFXTextArea bio_id;

    private JFXSpinner mainPageLoader;
    private Circle onlineCircle;

    public AnchorPane content;
    @FXML
    private JFXSpinner loader_id;
    Parent rtview = null;
    public  ObservableList<OnlineUser> online_list= FXCollections.observableArrayList();;

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

        if(uname.length()>0) {
            mainPageLoader.setVisible(true);


            new Thread(){
                String jsonString = null;
                Future<HttpResponse> future = null;
                Image image = null;
                @Override
                public void run() {
                    super.run();
                    try{
                    try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
                        MongoDatabase database = mongoClient.getDatabase("Photos");
                        GridFSBucket gridBucket = GridFSBuckets.create(database);
                        GridFSDownloadStream gdifs = gridBucket.openDownloadStream(uname);
                        byte[] data = gdifs.readAllBytes();
                        ByteArrayInputStream input = new ByteArrayInputStream(data);
                        BufferedImage image1 = ImageIO.read(input);
                        image= SwingFXUtils.toFXImage(image1, null);





                    }
                    catch (Exception e){
                        System.out.println(e.getMessage());
                        image=null;
                    }
                        CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                        client.start();
                        HttpGet request = new HttpGet(Main.Connectingurl + "/profile/user/"+ uname);

                        future = client.execute(request, null);
                        while (!future.isDone()) ;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            FXMLLoader loaderView = new FXMLLoader(getClass().getResource("viewUserProfile.fxml"));
                            ViewUserProfileController viewUserProfileController =null;
                            try {
                                rtview = loaderView.load();
                                viewUserProfileController = loaderView.getController();
                                bio_id = viewUserProfileController.getBio_id();
                                uname_id = viewUserProfileController.getUname_id();
                                fname_id = viewUserProfileController.getName_id();
//                                cost_id = viewUserProfileController.getCost_id();
                                image_view_id = viewUserProfileController.getImage_view_id();
                                online_status = viewUserProfileController.getOnline_status();
                                onlineCircle= viewUserProfileController.getOnline_circle();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
                            mongoLogger.setLevel(Level.SEVERE);

                            try {
                                jsonString = EntityUtils.toString(future.get().getEntity());
                                JSONObject myResponse = new JSONObject(jsonString);
                                if (future.get().getStatusLine().getStatusCode() == 200) {
                                    uname_id.setText(myResponse.getString("uname"));
                                    fname_id.setText(myResponse.getString("fname")+" "+myResponse.getString("lname"));
                                    bio_id.setText(myResponse.getString("bio"));
//                                    cost_id.setText(myResponse.getString("subsrate"));
                                    viewUserProfileController.setCost(myResponse.getString("subsrate"));
                                    if(image!=null){
                                        image_view_id.setFill(new ImagePattern(image));
                                    }
                                    if(!myResponse.getString("isonline").equals("null")) {
                                        if (myResponse.getBoolean("isonline")) {
                                            online_status.setText("User is Online");
                                            onlineCircle.setFill(Color.GREEN);
                                        } else {
                                            String time = myResponse.getString("lastseen");
                                            Instant timestamp = Instant.parse(time);
                                            ZonedDateTime indiaTime = timestamp.atZone(ZoneId.of("Asia/Kolkata"));
                                            String date = indiaTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                                            String timeshow = indiaTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                                            online_status.setText("Last Seen on " + date + " at: " + timeshow);
                                            onlineCircle.setFill(Color.RED);
                                        }
                                    }
                                    content.getChildren().setAll(rtview);
                                    MainPageController.displayedUname_id=uname_id.getText();
                                    viewUserProfileController.isSubscribe();
                                }
                                else{
                                    System.out.println(future.get().getStatusLine());
                                }

                            } catch ( InterruptedException | ExecutionException | JSONException | IOException e) {
                                e.printStackTrace();
                            } finally {
                                mainPageLoader.setVisible(false);
                            }


                        }
                    });
                }
                    catch (Exception e){
                        System.out.println(e.getMessage());
                        mainPageLoader.setVisible(false);
                    }

                }
            }.start();
        }
    }
    private void init(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                ObservableList<OnlineUser> list= FXCollections.observableArrayList();
                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpGet request = new HttpGet(Main.Connectingurl+"/getrecommendedusers/"+LoginController.curr_username);
                Future<HttpResponse> future = client.execute(request, null);
                HttpResponse res = null;
                while(!future.isDone());
                try {
                    res = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                String jsonList = null;
                try {
                    jsonList = EntityUtils.toString(res.getEntity());
                    JSONArray ResponseList = null;
                    ResponseList = new JSONArray(jsonList);

                    for(int i=0;i< ResponseList.length();i++) {
                        OnlineUser users = null;
                        try {
                            users = new OnlineUser(ResponseList.getString(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        list.add(users);
                    }
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if(!list.equals(online_list)) {
                                onlineuserslist.setItems(list);
                                onlineuserslist.setCellFactory(userListView -> {
                                    OnlineUsersListCellController onlineUsersListCellController = new OnlineUsersListCellController();
                                    onlineUsersListCellController.setOnMouseClicked(event -> {
                                        if (onlineUsersListCellController.uname_id.getText().startsWith("You")) {
                                            SearchUser(LoginController.curr_username);
                                        } else {
                                            SearchUser(onlineUsersListCellController.uname_id.getText());
                                        }

                                    });
                                    return onlineUsersListCellController;
                                });
                                online_list = list;
                            }
                        }
                    });
                } catch (IOException | JSONException e) {
                    onlineuserslist.setItems(null);
                }
                finally {
                    loader_id.setVisible(false);
                }


            }
        }.start();

    }
}
