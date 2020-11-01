import com.fasterxml.jackson.core.JsonProcessingException;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
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
    private AnchorPane rootPane;
    @FXML
    private Label online_status,uname_id, name_id,subcount_id,about_id,ins_id,subrate_id,tw_id,bal_id;
    @FXML
    private JFXTextArea bio_id;
    @FXML
    private Circle image_view_id;
    @FXML
    private JFXButton subscribe_btn;
    @FXML
    private JFXSpinner subs_spinner_id;
    @FXML
    private Circle online_circle;
    private String cost;
    private JFXSnackbar status_id;
    private Stage primaryStage=null;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        subscribe_btn.setDisable(true);
        image_view_id.setFill(new ImagePattern(new Image(getClass().getResource("photo/noimage.jpg").toString())));
        status_id=new JFXSnackbar(rootPane);
    }

    public String insta,twitter;

    public void isSubscribe(){
        if(uname_id.getText().equals(LoginController.curr_username))
        {
            subscribe_btn.setVisible(false);
        }
        else
        {
            bal_id.setVisible(false);
            if(subscribe_btn.getText().equals("Subscribe") && insta.equals("false"))
            {
                ins_id.setVisible(false);
            }
            if(subscribe_btn.getText().equals("Subscribe") && twitter.equals("false"))
            {
                tw_id.setVisible(false);
            }
        }
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
    public Label getName_id(){
        return name_id;
    }
    public Label getBal_id() {
        return bal_id;
    }
    public Label getTw_id() {
        return tw_id;
    }
    public Label getIns_id() {
        return ins_id;
    }
    public Label getSubrate_id() {
        return subrate_id;
    }

    public void setCost(String s){
        cost=s;
    }
    public Circle getImage_view_id(){
        return image_view_id;
    }
    public Label getOnline_status(){
        return online_status;
    }
    public Circle getOnline_circle() {
        return online_circle;
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
                                subcount_id.setText(jsonString+" subscribers");
                                about_id.setText("About "+uname_id.getText());
                            }
                        } catch (IOException | ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }.start();


    }

    public void Notif(String s)
    {
        new Thread(){
            Future<HttpResponse> future=null;
            @Override
            public void run() {
                super.run();

                var values_Notify = new HashMap<String, String>() {{
                    put("owner",uname_id.getText());
                    put("msg",LoginController.curr_username+s);
                }};

                var objectMapper = new ObjectMapper();
                String payloadNotify=null;

                try {
                    payloadNotify = objectMapper.writeValueAsString(values_Notify);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                StringEntity entity1 = new StringEntity(payloadNotify,ContentType.APPLICATION_JSON);

                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();

                HttpPost requestNotify = new HttpPost(Main.Connectingurl+"/setNotification");
                requestNotify.setEntity(entity1);
                requestNotify.setHeader("Content-Type", "application/json; charset=UTF-8");
                Future<HttpResponse> futureNotify = client.execute(requestNotify, null);
                while (!futureNotify.isDone());
            }
        }.start();
    }

    public void initProceed()
    {

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
                        System.out.println(jsonString);
                        if (task.get().getStatusLine().getStatusCode() == 200) {

                            status_id.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout(jsonString)));
                            if(subscribe_btn.getText().equals("Subscribe"))
                                Notif(" has Subscribed you");
                            else
                                Notif(" has Unsubscribed you");
                        } else {
                            status_id.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout(jsonString)));
                        }
                        isSubscribe();
                    } catch (IOException | InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                }
            });

        primaryStage.close();
    }

    public void Subscribe(ActionEvent actionEvent) throws IOException {

        subs_spinner_id.setVisible(true);
        FXMLLoader loader=new FXMLLoader(getClass().getResource("SubscribeConfirmation.fxml"));
        Parent root =loader.load();
        Scene scene = new Scene(root,600,400);
        primaryStage = new Stage();
        primaryStage.initModality(Modality.APPLICATION_MODAL);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Confirmation Page");
        primaryStage.initStyle(StageStyle.UNDECORATED);


        SubscribeConfirmationController Sbcc = loader.getController();
        status_subs=Sbcc.getLable_id();
        pwd_id=Sbcc.getPwd_id();
        proceed_btn=Sbcc.getButton_id();

        String fg=subscribe_btn.getText().equals("Subscribe") ? "true" : "false";

        if(fg=="true")
            status_subs.setText("Are you sure you want to Subcribe to "+uname_id.getText()+"\nThis will cost you : "+cost);
        else
            status_subs.setText("Are you sure you want to Unsubcribe to "+uname_id.getText()+"\nYou will not get any refund");

        proceed_btn.setOnAction(new EventHandler<ActionEvent>() {


            @Override
            public void handle(ActionEvent event) {
                if(pwd_id.getText().length()>0){
                    Task<HttpResponse> task =new Task<>() {
                        @Override
                        protected HttpResponse call() throws Exception {
                            var values = new HashMap<String, String>() {{
                                put("uname", LoginController.curr_username);
                                put("passhash", pwd_id.getText());
                            }};

                            var objectMapper = new ObjectMapper();
                            String payload =
                                    objectMapper.writeValueAsString(values);

                            StringEntity entity = new StringEntity(payload,
                                    ContentType.APPLICATION_JSON);

                            CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                            client.start();
                            HttpPost request = new HttpPost(Main.Connectingurl+"/checkPassword");
                            request.setEntity(entity);
                            request.setHeader("Content-Type", "application/json; charset=UTF-8");
                            Future<HttpResponse> future = client.execute(request, null);

                            while(!future.isDone());
                            return future.get();
                        }
                    };
                    Thread th=new Thread(task);
                    th.start();
                    task.setOnSucceeded(res->{
                        if(task.isDone()) {
                            try {
                                String jsonString = EntityUtils.toString(task.get().getEntity());
                                System.out.println(jsonString);
                                if (jsonString.equals("true")){
                                    initProceed();
                                    System.out.println("$$$");
                                }
                                else{
                                    status_id.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Wrong Password")));
                                    subs_spinner_id.setVisible(false);
                                    primaryStage.close();
                                }

                            } catch (InterruptedException | ExecutionException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    System.out.println("^^^");
                }
            }
        });


        primaryStage.showAndWait();


    }


}
