import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class WelcomePageController implements Initializable {
    @FXML
    private Label user_id;
    @FXML
    private JFXButton streaming_btn;
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
    public void setUser_id(String name){
        user_id.setText("Hi, "+name);
    }
    public JFXButton getStreamBtn(){
        return streaming_btn;
    }

}
