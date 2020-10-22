import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import pojo.Chat;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ChatController implements Initializable {

    @FXML
    public JFXListView<Chat> SubsChatList,PrivatechatList,AllchatList;
    @FXML
    private JFXTextArea Alltextarea_id,Privatetextarea_id,Substextarea_id;
    @FXML
    private JFXButton Subssend_btn,Privatesend_btn,Allsend_btn;
    @FXML
    public JFXSpinner Allprogress_id,Subsprogress_id,Privateprogress_id;
    @FXML
    private JFXTabPane Tabpane;
    @FXML
    private Tab Subschat_Tab;
    @FXML
    private Label statusAll_id,statusSubs_id,statusPrivate_id;
    public int Allcount=0,Subscount=0,Privatecount=0;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        mongoLogger.setLevel(Level.SEVERE);
        Allprogress_id.setVisible(true);

        Timeline fiveSecondsWonder = new Timeline(
                new KeyFrame(Duration.seconds(5.5),
                        event -> {

                        new Thread(){
                            Future<HttpResponse> future = null;
                            @Override
                            public void run() {
                                super.run();
                                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                                client.start();
                                HttpGet request = new HttpGet(Main.Connectingurl+"/isSubscribed/"+MainPageController.displayedUname_id+"/"+LoginController.curr_username);
                                future= client.execute(request, null);
                                while(!future.isDone());
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if(future.get().getStatusLine().getStatusCode() == 200){
                                                String jsonString = EntityUtils.toString(future.get().getEntity());
                                                if(jsonString.equals("1")){
                                                    Subschat_Tab.setDisable(false);
                                                    initSubs(MainPageController.displayedUname_id);
                                                }
                                                else{
                                                    Subschat_Tab.setDisable(true);
                                                }
                                            }
                                            else{
                                                System.out.println("Error");
                                            }

                                        } catch (InterruptedException | ExecutionException | IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }.start();
                        initPrivate(MainPageController.displayedUname_id);
                        initAll(MainPageController.displayedUname_id);

                }));
        fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
        fiveSecondsWonder.play();

    }

    private void initSubs(String owner) {
        new Thread(){
            HttpResponse res = null;
            @Override
            public void run() {
                super.run();
                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpGet request = new HttpGet(Main.Connectingurl+"/chatSubs/"+owner+"/"+LoginController.curr_username);
                Future<HttpResponse> future = client.execute(request, null);

                while(!future.isDone());
                try {
                    res = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        statusSubs_id.setText("");
                        ObservableList<Chat> list= FXCollections.observableArrayList();
                        JSONArray ResponseList = null;
                        try {
                            String jsonList = null;
                            if(res.getStatusLine().getStatusCode()==200) {
                                jsonList = EntityUtils.toString(res.getEntity());
                                ResponseList = new JSONArray(jsonList);
                                if (ResponseList.length() > 0)
                                    for (int i = 0; i < ResponseList.length(); i++) {
                                        Chat chat = null;
                                        try {
                                            String time = ResponseList.getJSONObject(i).getString("timestamp");
                                            Instant timestamp = Instant.parse(time);
                                            ZonedDateTime indiaTime = timestamp.atZone(ZoneId.of("Asia/Kolkata"));
                                            String date = indiaTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                                            String timeshow = indiaTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                                            chat = new Chat(ResponseList.getJSONObject(i).getString("uname"), ResponseList.getJSONObject(i).getString("message"), date, timeshow);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        list.add(chat);
                                    }
                                SubsChatList.setItems(list);
                                SubsChatList.setCellFactory(chat -> new ChatCellController());
                                if (Subscount != list.size()) {
                                    SubsChatList.scrollTo(list.size() - 1);
                                    Subscount = list.size();
                                }
                            }
                            else{
                                statusSubs_id.setText("No data found in this Chat");
                                SubsChatList.setItems(list);
                                SubsChatList.setCellFactory(chat -> new ChatCellController());
                            }
                        } catch (IOException | JSONException e) {
                            System.out.println(e.getMessage());
                        }

                    }
                });
            }
        }.start();
        Subsprogress_id.setVisible(false);
    }

    private void initPrivate(String owner){
        new Thread(){
            HttpResponse res = null;
            @Override
            public void run() {
                super.run();
                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpGet request = new HttpGet(Main.Connectingurl+"/privateChatView/"+owner+"/"+LoginController.curr_username);
                Future<HttpResponse> future = client.execute(request, null);

                while(!future.isDone());
                try {
                    res = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        statusPrivate_id.setText("");
                        ObservableList<Chat> list= FXCollections.observableArrayList();
                        JSONArray ResponseList = null;
                        try {
                            String jsonList = null;
                            if(res.getStatusLine().getStatusCode()==200) {
                                jsonList = EntityUtils.toString(res.getEntity());
                                ResponseList = new JSONArray(jsonList);
                                if (ResponseList.length() > 0)
                                    for (int i = 0; i < ResponseList.length(); i++) {
                                        Chat chat = null;
                                        try {
                                            String time = ResponseList.getJSONObject(i).getString("timestamp");
                                            Instant timestamp = Instant.parse(time);
                                            ZonedDateTime indiaTime = timestamp.atZone(ZoneId.of("Asia/Kolkata"));
                                            String date = indiaTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                                            String timeshow = indiaTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                                            chat = new Chat(ResponseList.getJSONObject(i).getString("from"), ResponseList.getJSONObject(i).getString("msg"), date, timeshow);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        list.add(chat);
                                    }
                                PrivatechatList.setItems(list);
                                PrivatechatList.setCellFactory(chat -> new ChatCellController());
                                if (Privatecount!= list.size()) {
                                    PrivatechatList.scrollTo(list.size() - 1);
                                    Privatecount = list.size();
                                }
                            }
                            else{
                                statusPrivate_id.setText("No data found in this Chat");
                                PrivatechatList.setItems(list);
                                PrivatechatList.setCellFactory(chat -> new ChatCellController());
                            }
                        } catch (IOException | JSONException e) {
                            System.out.println(e.getMessage());
                        }


                    }
                });
            }
        }.start();

    }

    private void initAll(String owner)  {
        new Thread(){
            HttpResponse res = null;
            @Override
            public void run() {
                super.run();
                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpGet request = new HttpGet(Main.Connectingurl+"/chatAll/"+owner+"/"+LoginController.curr_username);
                Future<HttpResponse> future = client.execute(request, null);

                while(!future.isDone());
                try {
                    res = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        statusAll_id.setText("");
                        ObservableList<Chat> list= FXCollections.observableArrayList();
                        JSONArray ResponseList = null;
                        try {
                            String jsonList = null;
                            if(res.getStatusLine().getStatusCode()==200) {
                                jsonList = EntityUtils.toString(res.getEntity());
                                ResponseList = new JSONArray(jsonList);
                                if (ResponseList.length() > 0)
                                    for (int i = 0; i < ResponseList.length(); i++) {
                                        Chat chat = null;
                                        try {
                                            String time = ResponseList.getJSONObject(i).getString("timestamp");
                                            Instant timestamp = Instant.parse(time);
                                            ZonedDateTime indiaTime = timestamp.atZone(ZoneId.of("Asia/Kolkata"));
                                            String date = indiaTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                                            String timeshow = indiaTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                                            chat = new Chat(ResponseList.getJSONObject(i).getString("uname"), ResponseList.getJSONObject(i).getString("message"), date, timeshow);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        list.add(chat);
                                    }
                                AllchatList.setItems(list);
                                AllchatList.setCellFactory(chat -> new ChatCellController());
                                if (Allcount != list.size()) {
                                    AllchatList.scrollTo(list.size() - 1);
                                    Allcount = list.size();
                                }
                            }
                            else{
                                statusAll_id.setText("No data found in this Chat");
                                AllchatList.setItems(list);
                                AllchatList.setCellFactory(chat -> new ChatCellController());
                            }
                        } catch (IOException | JSONException e) {
                            System.out.println(e.getMessage());
                        }

                    }
                });
            }
        }.start();
        Allprogress_id.setVisible(false);
    }
    public void SendSubs(){
        if (Substextarea_id.getText().length()>0) {
            Subsprogress_id.setVisible(true);
            Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
            new Thread(){
                Future<HttpResponse> future=null;
                @Override
                public void run() {
                    super.run();
                    var values = new HashMap<String, String>() {{
                        put("owner",MainPageController.displayedUname_id);
                        put("uname", LoginController.curr_username);
                        put("message",Substextarea_id.getText());
                        put("subscribermsg", "true");
                    }};

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
                    HttpPost request = new HttpPost(Main.Connectingurl+"/chatPost");
                    request.setEntity(entity);
                    request.setHeader("Content-Type", "application/json; charset=UTF-8");
                    Future<HttpResponse> future = client.execute(request, null);

                    while(!future.isDone());
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if(future.isDone()) {
                                Subsprogress_id.setVisible(false);
                                Substextarea_id.setText("");
                                initSubs(MainPageController.displayedUname_id);
                            }
                        }
                    });
                }
            }.start();

        }
    }
    public void SendPrivate(){
        if (Privatetextarea_id.getText().length()>0) {
            System.out.println("calll");
            Privateprogress_id.setVisible(true);
            new Thread(){
                Future<HttpResponse> future=null;
                @Override
                public void run() {
                    super.run();
                    var values = new HashMap<String, String>() {{
                        put("to",MainPageController.displayedUname_id);
                        put("from", LoginController.curr_username);
                        put("msg",Privatetextarea_id.getText());
                    }};

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
                    HttpPost request = new HttpPost(Main.Connectingurl+"/privateChatPost");
                    request.setEntity(entity);
                    request.setHeader("Content-Type", "application/json; charset=UTF-8");
                    Future<HttpResponse> future = client.execute(request, null);

                    while(!future.isDone());
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if(future.isDone()) {
                                Privateprogress_id.setVisible(false);
                                Privatetextarea_id.setText("");
                                initPrivate(MainPageController.displayedUname_id);
                            }
                        }
                    });
                }
            }.start();

        }

    }

    public void sendAll() {
        if (Alltextarea_id.getText().length()>0) {
            Allprogress_id.setVisible(true);
            Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
            new Thread(){
                Future<HttpResponse> future=null;
                @Override
                public void run() {
                    super.run();
                    var values = new HashMap<String, String>() {{
                        put("owner",MainPageController.displayedUname_id);
                        put("uname", LoginController.curr_username);
                        put("message",Alltextarea_id.getText());
                        put("subscribermsg", "false");
                    }};

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
                    HttpPost request = new HttpPost(Main.Connectingurl+"/chatPost");
                    request.setEntity(entity);
                    request.setHeader("Content-Type", "application/json; charset=UTF-8");
                    Future<HttpResponse> future = client.execute(request, null);

                    while(!future.isDone());
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if(future.isDone()) {
                                Allprogress_id.setVisible(false);
                                Alltextarea_id.setText("");
                                initAll(MainPageController.displayedUname_id);
                            }
                        }
                    });
                }
            }.start();

        }
    }
}
