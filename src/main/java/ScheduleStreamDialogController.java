import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.*;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScheduleStreamDialogController implements Initializable {
    @FXML
    private JFXDatePicker datepicker;
    @FXML
    private JFXTimePicker timepicker;
    @FXML
    private JFXTextArea info_id;
    @FXML
    private AnchorPane rootPane;
    @FXML
            private JFXSpinner loader;

    final Delta dragDelta=new Delta();

    JFXSnackbar snackbar;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
//        datepicker.setOnAction(new EventHandler() {
//            public void handle(Event t) {
//                LocalDate date = datepicker.getValue();
//                System.err.println("Selected date: " + date);
//            }
//        });
//
//        timepicker.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                LocalTime t=timepicker.getValue();
//                System.err.println("Selected time: " + t);
//            }
//        });
        snackbar=new JFXSnackbar(rootPane);
    }
    public void Schedule(){
        Task<HttpResponse> task =new Task<>() {
            @Override
            protected HttpResponse call() throws Exception {
                var values = new HashMap<String, String>() {{
                    put("owner", LoginController.curr_username);
                    put("time", timepicker.getValue().toString());
                    put("date", datepicker.getValue().toString());
                    put("description", info_id.getText());
                }};
                System.out.println("abc");
                var objectMapper = new ObjectMapper();
                String payload =
                        objectMapper.writeValueAsString(values);

                StringEntity entity = new StringEntity(payload,
                        ContentType.APPLICATION_JSON);

                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpPost request = new HttpPost(Main.Connectingurl+"/schedule/add");
                request.setEntity(entity);
                request.setHeader("Content-Type", "application/json; charset=UTF-8");
                Future<HttpResponse> future = client.execute(request, null);

                while(!future.isDone());
                return future.get();
            }
        };
        loader.setVisible(true);
        Thread th=new Thread(task);
        th.start();
        task.setOnSucceeded(res->{
                loader.setVisible(false);
            if(task.isDone()) {
                try {

                    if (task.get().getStatusLine().getStatusCode() == 200) {
                        ((Stage)rootPane.getScene().getWindow()).close();

                    } else {
                        System.out.println("Error");
                        snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Error")));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            else{
                System.out.println("Error");
                snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Error")));
            }
        });
    }

    public void exit(){
        ((Stage)rootPane.getScene().getWindow()).close();
    }

}
