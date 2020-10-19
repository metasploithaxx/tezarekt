import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDrawer;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.transitions.hamburger.HamburgerBasicCloseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainPageController implements Initializable {
    @FXML
    private JFXHamburger hamburger_id;
    @FXML
    private JFXDrawer drawer_id,chatdrawer_id;
    @FXML
    private ToggleButton chat_btn;
    @FXML
    private JFXSpinner load_id;
    @FXML
    private AnchorPane content,onlineUsers;
    private JFXButton profile_btn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatdrawer_id.open();
        load_id.setVisible(false);Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        mongoLogger.setLevel(Level.SEVERE);

        try {
            FXMLLoader loaderOnlineUsers = new FXMLLoader(getClass().getResource("OnlineUsersList.fxml"));

            Parent rtUsers=loaderOnlineUsers.load();
            onlineUsers.getChildren().setAll(rtUsers);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SideDrawer.fxml"));
            VBox toolbar = loader.load();
            drawer_id.setSidePane(toolbar);
            SideDrawerController sdc = loader.getController();
            profile_btn =sdc.getProfile_page();
            profile_btn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    FXMLLoader loader1=new FXMLLoader((getClass().getResource("Profile.fxml")));
                    Parent rt= null;
                    try {
                        rt = loader1.load();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    content.getChildren().setAll(rt);
                }
            });
            HamburgerBasicCloseTransition meth = new HamburgerBasicCloseTransition(hamburger_id);
            meth.setRate(-1);
            hamburger_id.addEventHandler(MouseEvent.MOUSE_CLICKED, (Event event) -> {
                meth.setRate(meth.getRate() * -1);
                meth.play();
                if (drawer_id.isClosed()) {
                    drawer_id.open();
                    drawer_id.toFront();
                    drawer_id.setMaxWidth(180);
                } else {

                    drawer_id.close();
                    drawer_id.setMaxWidth(0);
                }
            });
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        try {
            VBox chatbox = FXMLLoader.load(getClass().getResource("Chat.fxml"));
            chatdrawer_id.setSidePane(chatbox);
            chat_btn.addEventHandler(MouseEvent.MOUSE_CLICKED, (Event event) -> {

                if (!chat_btn.isSelected()) {
                    chatdrawer_id.toBack();
                    chatdrawer_id.open();
                    chatdrawer_id.setMinWidth(225);
                } else {
                    chatdrawer_id.toFront();
                    chatdrawer_id.close();
                    chatdrawer_id.setMaxWidth(0);
                }
            });
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    public void onExit(){
        load_id.setVisible(true);
        Task<HttpResponse> task =new Task<>() {
            @Override
            protected HttpResponse call() throws Exception {
                var values = new HashMap<String, String>() {{
                    put("uname", LoginController.curr_username);
                }};

                var objectMapper = new ObjectMapper();
                String payload =
                        objectMapper.writeValueAsString(values);

                StringEntity entity = new StringEntity(payload,
                        ContentType.APPLICATION_JSON);

                CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
                client.start();
                HttpPost request = new HttpPost(Main.Connectingurl+"/signout");
                request.setEntity(entity);
                request.setHeader("Content-Type", "application/json; charset=UTF-8");
                Future<HttpResponse> future = client.execute(request, null);

                while(!future.isDone());
                return future.get();
            }
        };
        Thread thread = new Thread(task);
        thread.start();
        task.setOnSucceeded(res->{
            load_id.setVisible(false);
            System.exit(0);
        });
    }
}
