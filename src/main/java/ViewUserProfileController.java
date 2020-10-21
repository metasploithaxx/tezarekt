import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextArea;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
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

public class ViewUserProfileController implements Initializable {

    @FXML
    private Label uname_id, fname_id, lname_id, subscost_id, status_id;
    @FXML
    private JFXTextArea bio_id;
    @FXML
    private ImageView image_view_id;
    @FXML
    private JFXButton subscribe_btn;
    @FXML
    private JFXSpinner subs_spinner_id;
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        subscribe_btn.setDisable(true);
    }
    public void isSubscribe(){
        if(uname_id.getText().equals(LoginController.curr_username))
            subscribe_btn.setVisible(false);
        subs_spinner_id.setVisible(true);
        subscribe_btn.setDisable(true);
        new Thread(){
            Future<HttpResponse> future = null;
            @Override
            public void run() {
                super.run();
                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpGet request = new HttpGet(Main.Connectingurl+"/isSubscribed/"+uname_id.getText()+"/"+LoginController.curr_username);
                future= client.execute(request, null);
                while(!future.isDone());

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            subscribe_btn.setDisable(false);
                            if(future.get().getStatusLine().getStatusCode() == 200){
                                String jsonString = EntityUtils.toString(future.get().getEntity());
                                if(jsonString.equals("1")){
                                    subscribe_btn.setText("Unsubscribe");
                                    subscribe_btn.setStyle("-fx-background-color:#a09ea9;");
                                }
                                else{
                                    subscribe_btn.setText("Subscribe");
                                    subscribe_btn.setStyle("-fx-background-color:#dc0808;");
                                }
                            }
                            else{
                                subscribe_btn.setText("error");
                            }

                        } catch (InterruptedException | ExecutionException | IOException e) {
                            e.printStackTrace();
                        } finally {
                            subs_spinner_id.setVisible(false);
                        }
                    }
                });
            }
        }.start();
    }
    public JFXTextArea getBio_id(){
        return bio_id;
    }
    public Label getUname_id(){
        return uname_id;
    }
    public Label getFname_id(){
        return fname_id;
    }
    public Label getLname_id(){
        return lname_id;
    }
    public Label getSubscost_id(){
        return subscost_id;
    }
    public ImageView getImage_view_id(){
        return image_view_id;
    }

    public void Subscribe(ActionEvent actionEvent){
        subs_spinner_id.setVisible(true);
        Task<HttpResponse> task = new Task<HttpResponse>() {
            @Override
            protected HttpResponse call() throws Exception {

                var values = new HashMap<String, String>() {{
                    put("from", uname_id.getText());
                    put("to", LoginController.curr_username);
                    put("flag",(subscribe_btn.getText().equals("Subscribe")?"true":"false"));

                }};

                var objectMapper = new ObjectMapper();
                String payload =
                        objectMapper.writeValueAsString(values);

                StringEntity entity = new StringEntity(payload,
                        ContentType.APPLICATION_JSON);

                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpPost request = new HttpPost(Main.Connectingurl+"/subscribe");
                request.setEntity(entity);
                request.setHeader("Content-Type", "application/json; charset=UTF-8");
                Future<HttpResponse> future = client.execute(request, null);

                while(!future.isDone());
                return future.get();
            }
        };
        Thread thread = new Thread(task);
        thread.start();

        task.setOnSucceeded(res -> {
            subs_spinner_id.setVisible(false);
            if(task.isDone()){
                try {
                        String jsonString = EntityUtils.toString(task.get().getEntity());
                        if (task.get().getStatusLine().getStatusCode() == 200)
                        {
                            status_id.setText(jsonString);
                            status_id.setTextFill(Color.GREEN);
                        }
                        else
                        {
                            status_id.setText(jsonString);
                            status_id.setTextFill(Color.RED);
                        }
                        isSubscribe();
                    }
                catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

            }
        });
    }
}
