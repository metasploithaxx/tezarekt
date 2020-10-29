import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXSpinner;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class LoginController implements Initializable {
    public static String curr_username;
    public Label loginState;
    @FXML
    private Label status_id;
    @FXML
    private Button login_btn;
    @FXML
    private TextField username_id,password_id;
    @FXML
    private CheckBox check_id;
    @FXML
    private JFXSpinner loader_id;
    public static Preferences preferences;
    public void Register() throws IOException {
        Parent root= FXMLLoader.load(getClass().getResource("Register.fxml"));
        Scene scene = new Scene(root);
        Stage primaryStage = new Stage(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    public void Signin(){
        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
        loginState.setText("Logging you in");
        loader_id.setVisible(true);
        Task<HttpResponse> task =new Task<>() {
            @Override
            protected HttpResponse call() throws Exception {
                String ip;
                try(final DatagramSocket socket = new DatagramSocket()){
                    socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                    ip = socket.getLocalAddress().getHostAddress();
                }
                var values = new HashMap<String, String>() {{
                    put("passhash", password_id.getText());
                    put("uname", username_id.getText());
                    put("ip",ip);
                }};

                var objectMapper = new ObjectMapper();
                String payload =
                        objectMapper.writeValueAsString(values);

                StringEntity entity = new StringEntity(payload,
                        ContentType.APPLICATION_JSON);

                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpPost request = new HttpPost(Main.Connectingurl+"/signin");
                request.setEntity(entity);
                request.setHeader("Content-Type", "application/json; charset=UTF-8");
                Future<HttpResponse> future = client.execute(request, null);

                while(!future.isDone());
                return future.get();
            }
        };
        Thread thread = new Thread(task);
        thread.start();
        task.setOnSucceeded(res-> {
            loader_id.setVisible(false);
            if(task.isDone()){
                try {
                    if (task.get().getStatusLine().getStatusCode() == 200) {
                        status_id.setText("Successfully Login");
                        status_id.setTextFill(Color.GREEN);
                        curr_username=username_id.getText();
                        preferences = Preferences.userRoot();
                        if (check_id.isSelected()) {
                            preferences.put("username", username_id.getText());
                            preferences.put("password", password_id.getText());
                        }
                        Parent root = null;
                        try {
                            Stage stage = (Stage) login_btn.getScene().getWindow();
                            root = FXMLLoader.load(getClass().getResource("MainPage.fxml"));
                            stage.close();

                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                        Scene scene = new Scene(root, 1050, 760);
                        scene.getStylesheets().add(getClass().getResource("css/stylesheet.css").toString());
                        Stage primaryStage = new Stage();
                        primaryStage.setScene(scene);
                        primaryStage.setTitle("Home page");
                        primaryStage.show();
                    } else {
                        String jsonString = EntityUtils.toString(task.get().getEntity());
                        loginState.setText("Please try again");
                        status_id.setText(jsonString);
                        status_id.setTextFill(Color.RED);
                    }
                } catch (InterruptedException | ExecutionException | IOException e) {
                    System.out.println(e.getMessage());
                }

            }
            else{
                loginState.setText("Please try again");
                status_id.setText("Incorrect username or Password");
                status_id.setTextFill(Color.RED);
            }
        });
    }
    public void Login(javafx.event.ActionEvent actionEvent) {
        Signin();
    }

    public void exit(){
        Platform.exit();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
        preferences = Preferences.userRoot();
        username_id.setText(preferences.get("username",""));
        password_id.setText(preferences.get("password",""));
        if(preferences.get("username","").length()>0 && preferences.get("password","").length()>0)
            Signin();
    }
}