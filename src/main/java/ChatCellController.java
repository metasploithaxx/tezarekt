import com.jfoenix.controls.JFXListCell;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import pojo.Chat;

import java.io.IOException;

public class ChatCellController extends JFXListCell<Chat> {
    @FXML
    Label name,msg,date_id,time_id;
    @FXML
    VBox rootPane;
    @FXML
    private ImageView image_id;
    private FXMLLoader loader;

    @Override
    protected void updateItem(Chat item,boolean empty){
        super.updateItem(item,empty);
        if(empty||item==null){}
        else{
            if (loader == null) {
                loader = new FXMLLoader(getClass().getResource("ChatCell.fxml"));
                loader.setController(this);

                try {
                    loader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
                if(item.getSender().equals(LoginController.curr_username)){
                    name.setText("You");
                    rootPane.setStyle("-fx-background-color:#bec5fa;");
                }
                else {
                    name.setText(item.getSender());
                    rootPane.setStyle("-fx-background-color:#FFFF;");
                }
                    msg.setText(item.getMsg());
                    date_id.setText(item.getDate());
                    time_id.setText(item.getTime());
                    rootPane.setPrefWidth(msg.getPrefWidth());
            setText(null);
            setGraphic(rootPane);
        }
    }
}