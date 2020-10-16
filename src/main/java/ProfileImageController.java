import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextField;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.bson.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfileImageController {
    @FXML
    private JFXSpinner load_id;
    @FXML
    private JFXTextField text_id;
    @FXML
    private Label status_id;
    @FXML
    private ImageView image_id;
    @FXML
    private JFXButton setpic_btn;
    private File selectedImage=null;
    public void ChooseFile(ActionEvent actionEvent)  {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("PNG (*.png)", "*.png");
        FileChooser.ExtensionFilter extFilter1 =
                new FileChooser.ExtensionFilter("JPEG(*.jpeg)", "*.jpeg");
        FileChooser.ExtensionFilter extFilter2 =
                new FileChooser.ExtensionFilter("JPG(*.jpg)", "*.jpg");
        fc.getExtensionFilters().add(extFilter);
        fc.getExtensionFilters().add(extFilter1);
        fc.getExtensionFilters().add(extFilter2);
        selectedImage = fc.showOpenDialog(null);
        FileInputStream fileinput=null;
        try {
            fileinput =new FileInputStream(selectedImage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(selectedImage!=null){
            text_id.setText(selectedImage.getAbsolutePath());
            try {
                Image image = new Image(fileinput);
                image_id.setImage(image);
                setpic_btn.setDisable(false);
            } catch (Exception e) {
                status_id.setText(e.getMessage());
                status_id.setTextFill(Color.RED);
            }
        }
        else{
            status_id.setText("You have not selected any file");
        }
    }
    public void UploadPic(ActionEvent actionEvent){
        load_id.setVisible(true);
        Logger.getLogger("com.mongodb.driver").setLevel(Level.WARNING);
        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
        Task<Boolean> task =new Task<Boolean>() {

            @Override
            protected Boolean call() throws Exception {
                try (MongoClient mongoClient = MongoClients.create(Main.MongodbId)) {
                    MongoDatabase database = mongoClient.getDatabase("Photos");
                    MongoCollection<Document> collection = database.getCollection("fs.files");
                    MongoCollection<Document> collectionchunks = database.getCollection("fs.chunks");

                    for(Document i:collection.find()){
                        if(i.get("filename").equals(LoginController.curr_username)){
                            collectionchunks.deleteMany(new Document("files_id",i.get("filename")));
                        }
                    }
                    collection.deleteMany(new Document("filename",LoginController.curr_username));
                    GridFSBucket gridBucket = GridFSBuckets.create(database);
                    InputStream inStream = new FileInputStream(selectedImage);
                    GridFSUploadOptions uploadOptions = new GridFSUploadOptions().chunkSizeBytes(1024).metadata(new Document("type", "image").append("content_type", "image/png"));
                    gridBucket.uploadFromStream(LoginController.curr_username, inStream, uploadOptions);
                    return true;
                }
                catch (Exception e){
                    System.out.println(e.getMessage()+"&&&&");
                    return false;
                }
            }
        };
        Thread th=new Thread(task);
        th.start();
        task.setOnSucceeded(res->{
            load_id.setVisible(false);
            if(task.getValue()){
                status_id.setText("Profile Pic Uploaded successfully");
                text_id.setText("");
            }
            else{
                status_id.setText("Some error Occurred while uploading");
            }
        });
    }
}
