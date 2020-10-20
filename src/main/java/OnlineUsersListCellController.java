import com.jfoenix.controls.JFXListCell;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import pojo.OnlineUser;

import java.io.IOException;

public class OnlineUsersListCellController extends JFXListCell<OnlineUser> {
    private FXMLLoader loader;
    @FXML
    VBox rootPane;
    @FXML
    Label uname_id;

    @Override
    protected void updateItem(OnlineUser item,boolean empty){
            super.updateItem(item,empty);
            if(empty||item==null){}
            else{
            if (loader == null) {
                loader = new FXMLLoader(getClass().getResource("OnlineUsersListCell.fxml"));
                loader.setController(this);

                try {
                    loader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(item.getUname().equals(LoginController.curr_username))
                uname_id.setText("You");
            else
                uname_id.setText(item.getUname());
            setText(null);
            setGraphic(rootPane);
            }
        }
}
