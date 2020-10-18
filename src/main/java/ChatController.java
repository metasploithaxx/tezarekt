import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextArea;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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

    public JFXListView<Chat> chatList;
    @FXML
    private JFXTextArea textarea_id;
    @FXML
    private JFXSpinner progress_id;
    public int count=0;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        mongoLogger.setLevel(Level.SEVERE);
        progress_id.setVisible(true);
        Timeline fiveSecondsWonder = new Timeline(
                new KeyFrame(Duration.seconds(4.5),
                        event -> {init();}));
        fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
        fiveSecondsWonder.play();

    }

    private void init()  {
        progress_id.setVisible(false);
        CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
        client.start();
        HttpGet request = new HttpGet(Main.Connectingurl+"/chat/Global/"+LoginController.curr_username);
        Future<HttpResponse> future = client.execute(request, null);
        HttpResponse res = null;
        while(!future.isDone());
        try {
            res = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        ObservableList<Chat> list= FXCollections.observableArrayList();
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
            Chat chat = null;
            try {
                String time = ResponseList.getJSONObject(i).getString("timestamp");
                Instant timestamp = Instant.parse(time);
                ZonedDateTime indiaTime = timestamp.atZone(ZoneId.of("Asia/Kolkata"));

                String date = indiaTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                String timeshow = indiaTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                chat = new Chat(ResponseList.getJSONObject(i).getString("uname"),ResponseList.getJSONObject(i).getString("message"),date,timeshow);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            list.add(chat);
            }
            chatList.setItems(list);
            chatList.setCellFactory(chat-> new ChatCellController());
            if(count!=list.size()) {
                chatList.scrollTo(list.size() - 1);
                count=list.size();
            }
            progress_id.setVisible(false);
    }

    public void send() {
        if (textarea_id.getText().length()>0) {
            progress_id.setVisible(true);
            Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
            Task<HttpResponse> task =new Task<>() {
                @Override
                protected HttpResponse call() throws Exception {
                    var values = new HashMap<String, String>() {{
                        put("owner","Global");
                        put("uname", LoginController.curr_username);
                        put("message", textarea_id.getText());
                        put("subscribermsg", "true");
                    }};

                    var objectMapper = new ObjectMapper();
                    String payload =
                            objectMapper.writeValueAsString(values);

                    StringEntity entity = new StringEntity(payload,
                            ContentType.APPLICATION_JSON);

                    CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                    client.start();
                    HttpPost request = new HttpPost("http://[::1]:3000/chatPost");
                    request.setEntity(entity);
                    request.setHeader("Content-Type", "application/json; charset=UTF-8");
                    Future<HttpResponse> future = client.execute(request, null);

                    while(!future.isDone());
                    return future.get();
                }
            };
            Thread th=new Thread(task);
            th.start();
            task.setOnSucceeded(res -> {
                progress_id.setVisible(false);
                textarea_id.setText("");
                init();
            });
        }
    }
}
