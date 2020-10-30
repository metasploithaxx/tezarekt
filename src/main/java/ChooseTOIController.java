import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXSpinner;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
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
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ChooseTOIController implements Initializable {

    @FXML
    private JFXCheckBox cb1,cb2,cb3,cb4,cb5,cb6;
    @FXML
    private Label status_id;

    @FXML
    private JFXSpinner loader_id;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new Thread(){
            Future<HttpResponse> future = null;
            @Override
            public void run() {
                super.run();
                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpGet request = new HttpGet(Main.Connectingurl + "/getToi/"+ LoginController.curr_username);

                future = client.execute(request, null);
                while (!future.isDone()) ;

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        String jsonString = null;
                        try {
                            jsonString = EntityUtils.toString(future.get().getEntity());
                            JSONObject myResponse = new JSONObject(jsonString);
                            if (future.get().getStatusLine().getStatusCode() == 200) {
                                JSONArray jsonArray = myResponse.getJSONArray("toi");
                                ArrayList<String> arrayList = new ArrayList<>() ;
                                for(int i=0;i<jsonArray.length();i++){
                                    arrayList.add(jsonArray.getString(i));
                                }
                                for(int i=0;i<arrayList.size();i++){
                                    if(arrayList.get(i).equals("Among Us")){
                                        cb1.setSelected(true);
                                    }
                                    if(arrayList.get(i).equals("Fortnite")){
                                        cb2.setSelected(true);
                                    }
                                    if(arrayList.get(i).equals("GTA V")){
                                        cb3.setSelected(true);
                                    }
                                    if(arrayList.get(i).equals("PUBG")){
                                        cb4.setSelected(true);
                                    }
                                    if(arrayList.get(i).equals("Strategic Games")){
                                        cb5.setSelected(true);
                                    }
                                    if(arrayList.get(i).equals("Fun Time")){
                                        cb6.setSelected(true);
                                    }
                                }
                            }
                            else{
                                System.out.println(jsonString);
                            }

                        } catch (IOException | InterruptedException | ExecutionException | JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        }.start();

    }

    public void Update_Btn(ActionEvent actionEvent){
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.clear();
        if(cb1.isSelected()){
            arrayList.add("Among Us");
        }
        if(cb2.isSelected()){
            arrayList.add("Fortnite");
        }
        if(cb3.isSelected()){
            arrayList.add("GTA V");
        }
        if(cb4.isSelected()){
            arrayList.add("PUBG");
        }
        if(cb5.isSelected()){
            arrayList.add("Strategic Games");
        }
        if(cb6.isSelected()){
            arrayList.add("Fun Time");
        }
        loader_id.setVisible(true);
        Task<HttpResponse> task =new Task<>() {
            @Override
            protected HttpResponse call() throws Exception {
                var values = new HashMap<String, Object>() {{
                    put("uname", LoginController.curr_username);
                    put("toi",arrayList);
                }};

                var objectMapper = new ObjectMapper();
                String payload =
                        objectMapper.writeValueAsString(values);

                StringEntity entity = new StringEntity(payload,
                        ContentType.APPLICATION_JSON);

                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpPost request = new HttpPost(Main.Connectingurl+"/setTOI");
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
                    status_id.setText(jsonString);
                    System.out.println(jsonString);
                } catch (InterruptedException | ExecutionException | IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                status_id.setText("UnSuccesfull Attempt");
                status_id.setTextFill(Color.RED);
            }
        });
    }
}
