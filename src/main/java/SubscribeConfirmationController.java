import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

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

}
