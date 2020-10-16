import com.jfoenix.controls.JFXDrawer;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.transitions.hamburger.HamburgerBasicCloseTransition;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainPage implements Initializable {
    @FXML
    private JFXHamburger hamburger_id;
    @FXML
    private JFXDrawer drawer_id,chatdrawer_id;
    @FXML
    private ToggleButton chat_btn;
    @FXML
    private JFXSpinner load_id;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        load_id.setVisible(false);
        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
        chatdrawer_id.open();

        try {
            VBox toolbar = FXMLLoader.load(getClass().getResource("Home.fxml"));
            drawer_id.setSidePane(toolbar);

            HamburgerBasicCloseTransition meth = new HamburgerBasicCloseTransition(hamburger_id);
            meth.setRate(-1);
            hamburger_id.addEventHandler(MouseEvent.MOUSE_CLICKED, (Event event) -> {
                meth.setRate(meth.getRate() * -1);
                meth.play();
                if (drawer_id.isClosed()) {
                    drawer_id.open();
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
                    chatdrawer_id.open();
                    chatdrawer_id.setMinWidth(225);
                } else {
                    chatdrawer_id.close();
                    chatdrawer_id.setMaxWidth(0);
                }
            });
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
