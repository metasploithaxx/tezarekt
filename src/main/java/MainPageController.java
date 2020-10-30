import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.*;
import com.jfoenix.transitions.hamburger.HamburgerBasicCloseTransition;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pojo.Chat;
import pojo.Notification;
import pojo.OnlineUser;

import javax.imageio.ImageIO;
import javax.security.auth.login.AccountNotFoundException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
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
    @FXML
    private Label status_id;
    @FXML
    private JFXComboBox<Notification> notification_id;


    public Label uname_id, fname_id, lname_id, cost_id,online_status;

    public ImageView image_view_id;

    private JFXButton profile_btn,startStreamBtn;

    public JFXTextArea bio_id;

    private AutoCompletionBinding <String> autoCompletionBinding;

    public static String displayedUname_id="Global";

    private Set<String> possibleSuggestion = new HashSet<>();


    public void initNotify(){
        new Thread(){
            Future<HttpResponse> future2 = null;
            @Override
            public void run() {
                super.run();
                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpGet request2 = new HttpGet(Main.Connectingurl+"/getNotification/"+LoginController.curr_username);
                future2 = client.execute(request2,null);
                while(!future2.isDone());
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        ObservableList<Notification> list= FXCollections.observableArrayList();
                        try{
                            if(future2.get().getStatusLine().getStatusCode()==200) {
                                String jsonNotifiList = EntityUtils.toString(future2.get().getEntity());
                                JSONArray jsonArray = new JSONArray(jsonNotifiList);
                                Notification notify =null;
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    String time = jsonArray.getJSONObject(i).getString("timestamp");
                                    notify=new Notification(time,jsonArray.getJSONObject(i).getString("msg"),jsonArray.getJSONObject(i).getInt("index"));
                                    list.add(notify);
                                }

                                notification_id.setItems(list);
                                notification_id.setCellFactory(new Callback<ListView<Notification>, ListCell<Notification>>() {
                                    @Override
                                    public ListCell<Notification> call(ListView<Notification> param) {
                                        return new ListCell<>(){


                                            @Override
                                            public void updateSelected(boolean selected) {
                                                super.updateSelected(selected);
                                                new Thread(){
                                                    @Override
                                                    public void run() {
                                                        super.run();
                                                        var values = new HashMap<String, String>() {{
                                                            put("index", String.valueOf(itemProperty().get().getindex()));
                                                        }};
                                                        System.out.println(itemProperty().get().getTimestamp());
                                                        var objectMapper = new ObjectMapper();
                                                        String payload = null;
                                                        try {
                                                            payload = objectMapper.writeValueAsString(values);
                                                        } catch (JsonProcessingException e) {
                                                            e.printStackTrace();
                                                        }

                                                        StringEntity entity = new StringEntity(payload,
                                                                ContentType.APPLICATION_JSON);

                                                        CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                                                        client.start();
                                                        HttpPost request = new HttpPost(Main.Connectingurl+"/delNotification");
                                                        request.setEntity(entity);
                                                        request.setHeader("Content-Type", "application/json; charset=UTF-8");
                                                        Future<HttpResponse> future = client.execute(request, null);

                                                        while(!future.isDone());
                                                        try {
                                                            System.out.println(future.get().getStatusLine());
                                                        } catch (InterruptedException | ExecutionException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }.start();

                                            }

                                            @Override
                                            protected void updateItem(Notification item, boolean empty) {
                                                super.updateItem(item, empty);
                                                if (item == null || empty) {
                                                    setGraphic(null);
                                                } else {

                                                    Instant timestamp = Instant.parse(item.getTimestamp());
                                                    ZonedDateTime indiaTime = timestamp.atZone(ZoneId.of("Asia/Kolkata"));
                                                    String date = indiaTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                                                    String timeshow = indiaTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                                                    setText(date+" "+timeshow+"\n"+item.getMsg());
                                                    setGraphic(null);
                                                }
                                            }
                                        };
                                    }
                                });
                            }
                        } catch (InterruptedException | ExecutionException | IOException | JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }.start();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        displayedUname_id="Global";
        chatdrawer_id.open();
        load_id.setVisible(true);
        Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        mongoLogger.setLevel(Level.SEVERE);
        load_id.setVisible(true);
        notification_id.valueProperty().addListener((e) -> {
            ;
        });
        Timeline fiveSecondsWonder = new Timeline(
                new KeyFrame(Duration.seconds(5.5),
                        event -> {
                    initNotify();
                }));
        fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
        fiveSecondsWonder.play();
        new Thread(){
            Future<HttpResponse> future = null;
            @Override
            public void run() {
                super.run();
                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpGet request = new HttpGet(Main.Connectingurl+"/getAllUsers");

                future = client.execute(request, null);

                while(!future.isDone());

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            String jsonList = EntityUtils.toString(future.get().getEntity());
                            JSONArray jsonArray = new JSONArray(jsonList);
                            for(int i=0;i< jsonArray.length();i++){
                                possibleSuggestion.add(jsonArray.getJSONObject(i).getString("uname"));
                            }
                            autoCompletionBinding = TextFields.bindAutoCompletion(search_uname,possibleSuggestion);
                        } catch (IOException | InterruptedException | ExecutionException | JSONException e) {
                            e.printStackTrace();
                        }
                        finally {
                            load_id.setVisible(false);
                        }
                    }
                });
            }
        }.start();
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
            startStreamBtn=sdc.getStart_stream();
            search_uname.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    switch(event.getCode()){
                        case ENTER:
                            status_id.setText("");
                            if(search_uname.getText().length()>0) {
                                SearchUser(search_uname.getText());
                            }

                            break;
                        default:
                            break;
                    }
                }
            });
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
            startStreamBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    FXMLLoader loader1=new FXMLLoader((getClass().getResource("StreamerHub.fxml")));
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
                    drawer_id.toBack();
                    drawer_id.close();
                    drawer_id.setMaxWidth(0);
                }
            });
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        try {
            FXMLLoader loader= new FXMLLoader(getClass().getResource("Chat.fxml"));
            VBox chatbox = loader.load();
            ChatController chatController = loader.getController();
            chatdrawer_id.setSidePane(chatbox);
            chat_btn.addEventHandler(MouseEvent.MOUSE_CLICKED, (Event event) -> {

                if (!chat_btn.isSelected()) {
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
        load_id.setVisible(false);
    }
    private void SearchUser(String uname){

        if(uname.length()>0) {

            load_id.setVisible(true);
            Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
            mongoLogger.setLevel(Level.SEVERE);
            new Thread(){
                ViewUserProfileController viewUserProfileController=null;

                Image image=null;
                Future<HttpResponse> future=null;
                @Override
                public void run() {
                    super.run();
                    try{
                        try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
                            MongoDatabase database = mongoClient.getDatabase("Photos");
                            GridFSBucket gridBucket = GridFSBuckets.create(database);
                            GridFSDownloadStream gdifs = gridBucket.openDownloadStream(search_uname.getText());
                            byte[] data = gdifs.readAllBytes();
                            ByteArrayInputStream input = new ByteArrayInputStream(data);
                            BufferedImage image1 = ImageIO.read(input);
                            image = SwingFXUtils.toFXImage(image1, null);
                        }
                        catch (Exception e){
                            image=null;
                        }
                        CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                        client.start();
                        HttpGet request = new HttpGet(Main.Connectingurl + "/profile/user/" + search_uname.getText());
                        future = client.execute(request, null);
                        while (!future.isDone()) ;
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                FXMLLoader loaderView = new FXMLLoader(getClass().getResource("viewUserProfile.fxml"));
                                Parent rtview = null;
                                try {
                                    String jsonString = null;

                                    if (future.get().getStatusLine().getStatusCode() == 200) {
                                        jsonString = EntityUtils.toString(future.get().getEntity());
                                        JSONObject myResponse = new JSONObject(jsonString);
                                        try {
                                            rtview = loaderView.load();
                                            viewUserProfileController= loaderView.getController();
                                            bio_id = viewUserProfileController.getBio_id();
                                            uname_id = viewUserProfileController.getUname_id();
                                            fname_id = viewUserProfileController.getFname_id();
                                            lname_id = viewUserProfileController.getLname_id();
                                            cost_id = viewUserProfileController.getCost_id();
                                            image_view_id = viewUserProfileController.getImage_view_id();
                                            online_status = viewUserProfileController.getOnline_status();

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        if(image!=null) {
                                            image_view_id.setImage(image);
                                        }
                                        uname_id.setText(myResponse.getString("uname"));
                                        fname_id.setText(myResponse.getString("fname"));
                                        lname_id.setText(myResponse.getString("lname"));
                                        cost_id.setText(myResponse.getString("subsrate"));
                                        bio_id.setText(myResponse.getString("bio"));
                                        if(!myResponse.getString("isonline").equals("null")){
                                            if (myResponse.getBoolean("isonline") == true) {
                                                online_status.setText("User is Online");
                                            } else {
                                                String time = myResponse.getString("lastseen");
                                                Instant timestamp = Instant.parse(time);
                                                ZonedDateTime indiaTime = timestamp.atZone(ZoneId.of("Asia/Kolkata"));
                                                String date = indiaTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                                                String timeshow = indiaTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                                                online_status.setText("Last Seen \nDate :- " + date + "\n time :- " + timeshow);
                                            }
                                        }
                                        content.getChildren().setAll(rtview);
                                        displayedUname_id=uname_id.getText();
                                        viewUserProfileController.isSubscribe();
                                    }
                                    else{
                                        status_id.setText("No such User found!!!");
                                        status_id.setTextFill(Color.RED);
                                        load_id.setVisible(false);
                                    }
                                } catch (IOException | InterruptedException | ExecutionException | JSONException e) {
                                    e.printStackTrace();
                                    status_id.setText("No such User found!!!");
                                    status_id.setTextFill(Color.RED);
                                    load_id.setVisible(false);
                                }
                                load_id.setVisible(false);
                            }
                        });
                    }
                    catch (Exception e){
                        System.out.println(e.getMessage());
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                status_id.setText("No such User found!!!");
                                status_id.setTextFill(Color.RED);
                                load_id.setVisible(false);
                            }
                        });
                    }

                }
            }.start();
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
