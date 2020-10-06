import com.jfoenix.controls.JFXListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class ChatController implements Initializable {

    public JFXListView<Chat> chatList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObservableList<Chat> list= FXCollections.observableArrayList();
        Chat a=new Chat("Aritra","Hi");
        Chat b=new Chat("Manish","Hello");
        Chat c=new Chat("Chinmay","what's upp");
        list.add(a);
        list.add(b);
        list.add(c);
        chatList.setItems(list);
        chatList.setCellFactory(chat-> new ChatCellController());
    }
}
