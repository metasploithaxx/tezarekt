import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextArea;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RegisterController {
    @FXML
    private TextField username_id,password_id,confirmpd_id,firstname_id,lastname_id;
    @FXML
    private JFXTextArea briefinfo_id;
    @FXML
    private Label status_id;
    @FXML
    private JFXSpinner loader_id;
    public void server_reg(){
        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
        loader_id.setVisible(true);
        Task<HttpResponse> task =new Task<>() {
            @Override
            protected HttpResponse call() throws Exception {
                var values = new HashMap<String, String>() {{
                    put("passHash", BCrypt.withDefaults().hashToString(4, password_id.getText().toCharArray()));
                    put("username", username_id.getText());
                    put("firstname", firstname_id.getText());
                    put("lastname", lastname_id.getText());
                    put("subsrate", "0");
                    put("info",(briefinfo_id.getText().length()>0)?briefinfo_id.getText():"");
                }};

                var objectMapper = new ObjectMapper();
                String payload =
                        objectMapper.writeValueAsString(values);

                StringEntity entity = new StringEntity(payload,
                        ContentType.APPLICATION_JSON);

                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpPost request = new HttpPost(Main.Connectingurl+"/register");
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
            loader_id.setVisible(false);
            if(task.isDone()) {
                try {
                    String jsonString = EntityUtils.toString(task.get().getEntity());
                    JSONObject myResponse = new JSONObject(jsonString);
                    System.out.println(jsonString);
                    if (task.get().getStatusLine().getStatusCode() == 200) {
                        status_id.setText("Successfully Registered");
                        status_id.setTextFill(Color.GREEN);
                        Stage stage = (Stage) status_id.getScene().getWindow();
                        stage.close();

                    } else {

                        System.out.println(myResponse.getString("detail"));
                        status_id.setText("statusCode- "+myResponse.getString("detail"));
                        status_id.setTextFill(Color.RED);
                    }
                } catch (InterruptedException | ExecutionException | IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                status_id.setText("Unsuccesfull Attempt");
                status_id.setTextFill(Color.RED);
            }
        });
    }
    public void Register(ActionEvent event)  {
        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
        System.out.println("enter Register");
        if(username_id.getText().length()>0 && password_id.getText().length()>0 && confirmpd_id.getText().length()>0 && firstname_id.getText().length()>0 && lastname_id.getText().length()>0 ){
            if(!confirmpd_id.getText().equals(password_id.getText())){
                status_id.setText("Confirm Password and Password don't match");
                status_id.setTextFill(Color.RED);
            }
            else if(username_id.getText().equals("Global")){
                status_id.setText("Can't set Global as a Username");
                status_id.setTextFill(Color.RED);
            }
            else{
                server_reg();
            }
        }
        else{
            status_id.setText("Fill all the Details");
            status_id.setTextFill(Color.RED);
        }
    }
    public void exit(){
        ((Stage)status_id.getScene().getWindow()).close();
    }
}
