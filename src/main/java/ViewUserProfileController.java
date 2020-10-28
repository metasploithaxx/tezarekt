import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
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

    public Label status_subs;
    public PasswordField pwd_id;
    public JFXButton proceed_btn;
    @FXML
    private Label online_status,uname_id, fname_id, lname_id,status_id,subcount_id,cost_id;
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
                SubsCount();
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
    public Label getCost_id(){
        return cost_id;
    }
    public ImageView getImage_view_id(){
        return image_view_id;
    }
    public Label getOnline_status(){
        return online_status;
    }

    public void SubsCount(){
        new Thread(){
            Future<HttpResponse> future=null;
            @Override
            public void run() {
                super.run();
                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpGet request = new HttpGet(Main.Connectingurl+"/getSubscriptionCount/"+uname_id.getText());
                future = client.execute(request, null);
                while(!future.isDone());

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String jsonString = EntityUtils.toString(future.get().getEntity());
                            if (future.get().getStatusLine().getStatusCode() == 200){
                                subcount_id.setText(jsonString);
                            }
                        } catch (IOException | ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }.start();


    }

    public void initProceed()
    {

    }

    public void Subscribe(ActionEvent actionEvent) throws IOException {

        subs_spinner_id.setVisible(true);
        FXMLLoader loader=new FXMLLoader(getClass().getResource("SubscribeConfirmation.fxml"));
        Parent root =loader.load();
        Scene scene = new Scene(root,600,400);
        Stage primaryStage = new Stage();
        primaryStage.initModality(Modality.APPLICATION_MODAL);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Confirmation Page");


        SubscribeConfirmationController Sbcc = loader.getController();
        status_subs=Sbcc.getLable_id();
        pwd_id=Sbcc.getPwd_id();
        proceed_btn=Sbcc.getButton_id();

        String fg=subscribe_btn.getText().equals("Subscribe") ? "true" : "false";

        if(fg=="true")
            status_subs.setText("Are you sure you want to Subcribe to "+uname_id.getText()+"\nThis will cost you : "+cost_id.getText());
        else
            status_subs.setText("Are you sure you want to Unsubcribe to "+uname_id.getText()+"\nYou will not get any refund");

        proceed_btn.setOnAction(new EventHandler<ActionEvent>() {


            @Override
            public void handle(ActionEvent event) {

               Future<Boolean> flag =  Sbcc.checkPassword(LoginController.curr_username);
               while(!flag.isDone());

                if(flag.isDone()){
                    Task<HttpResponse> task = new Task<HttpResponse>() {
                        @Override
                        protected HttpResponse call() throws Exception {

                            var values = new HashMap<String, String>() {{
                                put("from", uname_id.getText());
                                put("to", LoginController.curr_username);
                                put("flag", (subscribe_btn.getText().equals("Subscribe") ? "true" : "false"));

                            }};

                            var objectMapper = new ObjectMapper();
                            String payload =
                                    objectMapper.writeValueAsString(values);

                            StringEntity entity = new StringEntity(payload,
                                    ContentType.APPLICATION_JSON);

                            CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                            client.start();
                            HttpPost request = new HttpPost(Main.Connectingurl + "/subscribe");
                            request.setEntity(entity);
                            request.setHeader("Content-Type", "application/json; charset=UTF-8");
                            Future<HttpResponse> future = client.execute(request, null);

                            while (!future.isDone()) ;
                            return future.get();
                        }
                    };
                    Thread thread = new Thread(task);
                    thread.start();

                    task.setOnSucceeded(res -> {
                        subs_spinner_id.setVisible(false);
                        if (task.isDone()) {
                            try {
                                String jsonString = EntityUtils.toString(task.get().getEntity());
                                if (task.get().getStatusLine().getStatusCode() == 200) {
                                    status_id.setText(jsonString);
                                    status_id.setTextFill(Color.GREEN);
                                } else {
                                    status_id.setText(jsonString);
                                    status_id.setTextFill(Color.RED);
                                }
                                isSubscribe();
                            } catch (IOException | InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }

                        }
                    });

                }
                else{

                    subs_spinner_id.setVisible(false);
                    isSubscribe();
                    status_id.setText("Wrong password");
                    status_id.setTextFill(Color.RED);
                }
                primaryStage.close();
            }
        });


        primaryStage.showAndWait();


    }
}
