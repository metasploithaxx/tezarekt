import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.*;
import com.jfoenix.transitions.hamburger.HamburgerBasicCloseTransition;
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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
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
import javax.security.auth.login.AccountNotFoundException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainPageController implements Initializable {
    @FXML
    private JFXHamburger hamburger_id;
    @FXML
    private JFXDrawer drawer_id,chatdrawer_id;
    @FXML
    private ToggleButton chat_btn;
    @FXML
    private JFXSpinner load_id;
    @FXML
    private AnchorPane content,onlineUsers;
    @FXML
    private TextField search_uname;
    @FXML
    private JFXButton search_btn;
    public Label uname_id, fname_id, lname_id, subscost_id;
    public ImageView image_view_id;
    private JFXButton profile_btn;
    public JFXTextArea bio_id;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatdrawer_id.open();
        load_id.setVisible(false);
        Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        mongoLogger.setLevel(Level.SEVERE);

        try {
            FXMLLoader loaderOnlineUsers = new FXMLLoader(getClass().getResource("OnlineUsersList.fxml"));
            Parent rtUsers=loaderOnlineUsers.load();
            OnlineUsersListController obj = loaderOnlineUsers.getController();
            obj.setContent(content);
            obj.setLoaderID(load_id);
            onlineUsers.getChildren().setAll(rtUsers);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SideDrawer.fxml"));
            VBox toolbar = loader.load();
            drawer_id.setSidePane(toolbar);
            SideDrawerController sdc = loader.getController();
            profile_btn =sdc.getProfile_page();
            profile_btn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    FXMLLoader loader1=new FXMLLoader((getClass().getResource("Profile.fxml")));
                    Parent rt= null;
                    try {
                        rt = loader1.load();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    content.getChildren().setAll(rt);
                }
            });
            HamburgerBasicCloseTransition meth = new HamburgerBasicCloseTransition(hamburger_id);
            meth.setRate(-1);
            hamburger_id.addEventHandler(MouseEvent.MOUSE_CLICKED, (Event event) -> {
                meth.setRate(meth.getRate() * -1);
                meth.play();
                if (drawer_id.isClosed()) {
                    drawer_id.open();
                    drawer_id.toFront();
                    drawer_id.setMaxWidth(180);
                } else {

                    drawer_id.close();
                    drawer_id.setMaxWidth(0);
                }
            });
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        try {
            VBox chatbox = FXMLLoader.load(getClass().getResource("Chat.fxml"));
            chatdrawer_id.setSidePane(chatbox);
            chat_btn.addEventHandler(MouseEvent.MOUSE_CLICKED, (Event event) -> {

                if (!chat_btn.isSelected()) {
                    chatdrawer_id.toBack();
                    chatdrawer_id.open();
                    chatdrawer_id.setMinWidth(225);
                } else {
                    chatdrawer_id.toFront();
                    chatdrawer_id.close();
                    chatdrawer_id.setMaxWidth(0);
                }
            });
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    private void SearchUser(String uname){
        if(uname.length()>0) {
            FXMLLoader loaderView = new FXMLLoader(getClass().getResource("viewUserProfile.fxml"));
            Parent rtview = null;
            load_id.setVisible(true);
            try {
                rtview = loaderView.load();
                ViewUserProfileController viewUserProfileController = loaderView.getController();
                bio_id = viewUserProfileController.getBio_id();
                uname_id = viewUserProfileController.getUname_id();
                fname_id = viewUserProfileController.getFname_id();
                lname_id = viewUserProfileController.getLname_id();
                subscost_id = viewUserProfileController.getSubscost_id();
                image_view_id = viewUserProfileController.getImage_view_id();
                content.getChildren().setAll(rtview);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
            mongoLogger.setLevel(Level.SEVERE);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
                        MongoDatabase database = mongoClient.getDatabase("Photos");
                        GridFSBucket gridBucket = GridFSBuckets.create(database);
                        GridFSDownloadStream gdifs = gridBucket.openDownloadStream(search_uname.getText());
                        byte[] data = gdifs.readAllBytes();
                        ByteArrayInputStream input = new ByteArrayInputStream(data);
                        BufferedImage image1 = ImageIO.read(input);
                        Image image = SwingFXUtils.toFXImage(image1, null);
                        image_view_id.setImage(image);
                    }
                    catch (Exception e){
                        System.out.println(e.getMessage());
                    }

                }
            });
            Task<HttpResponse> task = new Task<HttpResponse>() {
                @Override
                protected HttpResponse call() throws Exception {
                    CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                    client.start();
                    HttpGet request = new HttpGet(Main.Connectingurl + "/profile/user/" + search_uname.getText());
                    Future<HttpResponse> future = client.execute(request, null);
                    while (!future.isDone()) ;
                    return future.get();
                }
            };
            Thread th = new Thread(task);
            th.start();
            task.setOnSucceeded(res -> {
                load_id.setVisible(false);
                if (task.isDone()) {

                    String jsonString = null;
                    try {
                        jsonString = EntityUtils.toString(task.get().getEntity());
                        JSONObject myResponse = new JSONObject(jsonString);
                        if (task.get().getStatusLine().getStatusCode() == 200) {
                            uname_id.setText(myResponse.getString("uname"));
                            fname_id.setText(myResponse.getString("fname"));
                            lname_id.setText(myResponse.getString("lname"));
                            bio_id.setText(myResponse.getString("bio"));

                        }
                    } catch (IOException | InterruptedException | ExecutionException | JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    }

    public void OnSearch(ActionEvent actionEvent){
        if(search_uname.getText().length()>0) {
            SearchUser(search_uname.getText());
        }
    }
    public AnchorPane getAnchorContent(){
        return content;
    }
    public JFXSpinner getLoad_id(){
        return load_id;
    }

    public void onExit(){
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
            System.exit(0);
        });
    }
}
