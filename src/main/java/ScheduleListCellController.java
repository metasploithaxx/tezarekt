import com.jfoenix.controls.JFXListCell;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import pojo.OnlineUser;
import pojo.Schedule;

import java.io.IOException;

public class ScheduleListCellController extends JFXListCell<Schedule> {

    private FXMLLoader loader;

    public Label dateLabel,timeLabel,descLabel;

    public VBox rootPane;

    @Override
    protected void updateItem(Schedule item, boolean empty){
        super.updateItem(item,empty);
        if(empty||item==null){
            setText(null);
            setGraphic(null);
        }
        else{
            if (loader == null) {
                loader = new FXMLLoader(getClass().getResource("ScheduleListCell.fxml"));
                loader.setController(this);

                try {
                    loader.load();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            dateLabel.setText(item.getDate());
            timeLabel.setText(item.getTime());
            descLabel.setText(item.getDescription());
            System.out.println(dateLabel.getText()+" "+descLabel.getText());
            setText(null);
            setGraphic(rootPane);
        }
    }
}
