import com.jfoenix.controls.JFXListCell;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import pojo.OnlineUser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class OnlineUsersListCellController extends JFXListCell<OnlineUser> {
    private FXMLLoader loader;
    @FXML
    VBox rootPane;
    @FXML
    Label uname_id;
    @FXML
    ImageView image_id;

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
                new Thread(){
                    Image image=null;
                    @Override
                    public void run() {
                        super.run();
                        try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
                            MongoDatabase database = mongoClient.getDatabase("Photos");
                            GridFSBucket gridBucket = GridFSBuckets.create(database);
                            GridFSDownloadStream gdifs = gridBucket.openDownloadStream(item.getUname());
                            byte[] data = gdifs.readAllBytes();
                            ByteArrayInputStream input = new ByteArrayInputStream(data);
                            BufferedImage image1 = ImageIO.read(input);
                            image = SwingFXUtils.toFXImage(image1, null);
                        }
                        catch (Exception e){
                            image=null;
                        }
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                if(image!=null)
                                    image_id.setImage(image);
                            }
                        });
                    }
                }.start();
            if(item.getUname().equals(LoginController.curr_username)) {
                uname_id.setText("You("+LoginController.curr_username+")");
                rootPane.setStyle("-fx-background-color:#bec5fa;");
            }
            else
                uname_id.setText(item.getUname());
            setText(null);
            setGraphic(rootPane);
            }
        }
}
