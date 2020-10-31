import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;




public class SubscribeConfirmationController {
    @FXML
    private Label lable_id;

    @FXML
    private JFXPasswordField pwd_id;

    @FXML
    private JFXButton button_id;

    public Label getLable_id() {
        return lable_id;
    }

    public JFXButton getButton_id() {
        return button_id;
    }

    public JFXPasswordField getPwd_id() {
        return pwd_id;
    }
    public Boolean flag=false;

    public Boolean checkPassword(String user)
    {
        if(pwd_id.getText().length()>0){
            Task<HttpResponse> task =new Task<>() {
                @Override
                protected HttpResponse call() throws Exception {
                    var values = new HashMap<String, String>() {{
                        put("uname", user);
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
                            flag = true;
                            System.out.println("$$$");
                        }

                    } catch (InterruptedException | ExecutionException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            System.out.println("^^^");
            return flag;
        }
        else{
            return false;
        }

    }

}
