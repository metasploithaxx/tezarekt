import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXSpinner;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.util.Duration;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import pojo.Chat;
import pojo.OnlineUser;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class OnlineUsersListController implements Initializable {
    public JFXListView<OnlineUser> onlineuserslist;
    @FXML
    private JFXSpinner loader_id;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loader_id.setVisible(true);
        Timeline fiveSecondsWonder = new Timeline(
                new KeyFrame(Duration.seconds(4.5),
                        event -> {init();}));
        fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
        fiveSecondsWonder.play();
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
                    System.out.println(users.getUname());
                    list.add(users);
                }

                onlineuserslist.setItems(list);
                onlineuserslist.setCellFactory(userListView-> new OnlineUsersListCellController());
                loader_id.setVisible(false);
            }
        });
    }
}
