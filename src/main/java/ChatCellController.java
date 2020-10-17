import com.jfoenix.controls.JFXListCell;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

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


//                    Platform.runLater(new Runnable() {
//                        @Override
//                        public void run() {
//                            Image image = null;
//                            try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
//                                MongoDatabase database1 = mongoClient.getDatabase("Photos");
//                                GridFSBucket gridBucket = GridFSBuckets.create(database1);
//                                GridFSDownloadStream gdifs = gridBucket.openDownloadStream(item.getSender());
//                                if (!gdifs.equals(null)) {
//                                    byte[] data = gdifs.readAllBytes();
//                                    ByteArrayInputStream input = new ByteArrayInputStream(data);
//                                    BufferedImage image1 = ImageIO.read(input);
//                                    image = SwingFXUtils.toFXImage(image1, null);
//
//                                    image_id.setImage(image);
//                                }
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    });

            name.setText(item.getSender());
                    msg.setText(item.getMsg());
                    date_id.setText(item.getDate());
                    time_id.setText(item.getTime());

            setText(null);
            setGraphic(rootPane);
        }
    }
}