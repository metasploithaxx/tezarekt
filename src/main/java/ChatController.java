import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextArea;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

class Sortbyroll implements Comparator<Chat>
{

    @Override
    public int compare(Chat o1, Chat o2) {
        if(o1.getDate().compareTo(o2.getDate())==0){
            return o1.getTime().compareTo(o2.getTime());
        }
        else{
            return o1.getDate().compareTo(o2.getDate());
        }
    }
}
public class ChatController implements Initializable {

    public JFXListView<Chat> chatList;
    @FXML
    private JFXTextArea textarea_id;
    @FXML
    private JFXSpinner progress_id;
    public int count=0;
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
        progress_id.setVisible(true);
        Timeline fiveSecondsWonder = new Timeline(
                new KeyFrame(Duration.seconds(4.5),
                        (EventHandler<ActionEvent>) event -> {init();}));
        fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
        fiveSecondsWonder.play();

    }

    private void init() {
//                progress_id.setVisible(true);
//                ObservableList<Chat> list= FXCollections.observableArrayList();
//                try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
//                    MongoDatabase database = mongoClient.getDatabase("Softa");
//                    MongoCollection<Document> collection = database.getCollection("global_chat");
//                    MongoCursor<Document> cursor=collection.find().cursor();
//
//                    while(cursor.hasNext()==true){
//                        Document db=cursor.next();
//                        Chat temp=new Chat(db.getString("username"),db.getString("msg"),db.getString("date"),db.getString("time"));
//                        list.add(temp);
//                    }
//                    list.sort(new Sortbyroll());
//                    chatList.setItems(list);
//                    chatList.setCellFactory(chat-> new ChatCellController());
//                    if(count!=list.size()) {
//                        chatList.scrollTo(list.size() - 1);
//                        count=list.size();
//                    }
//                    } catch (Exception e) {
//                        System.out.println(e.getMessage());
//                    }
//                    progress_id.setVisible(false);

    }

    public void send() {
//        if (textarea_id.getText().length()>0) {
//            Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
//            Task<Boolean> task = new Task<Boolean>() {
//
//                @Override
//                protected Boolean call() throws Exception {
//                    progress_id.setVisible(true);
//                    try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
//                        MongoDatabase database = mongoClient.getDatabase("Softa");
//                        MongoCollection<Document> collection = database.getCollection("global_chat");
//                        Document doc = new Document();
//                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//                        LocalDateTime now = LocalDateTime.now();
//                        DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm:ss");
//                        LocalDateTime timenow = LocalDateTime.now();
//                        doc.append("username", LoginController.curr_username)
//                                .append("msg", textarea_id.getText())
//                                .append("date", dtf.format(now))
//                                .append("time", time.format(timenow));
//                        collection.insertOne(doc);
//                        return true;
//                    } catch (Exception e) {
//                        System.out.println(e.getMessage());
//                        return true;
//                    }
//                }
//            };
//            Thread th = new Thread(task);
//            th.start();
//            task.setOnSucceeded(res -> {
//                progress_id.setVisible(false);
//                if (task.getValue()) {
//                    textarea_id.setText("");
//                }
//            });
//        }
    }
}
