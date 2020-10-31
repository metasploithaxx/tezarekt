import com.github.sarxos.webcam.Webcam;
import com.jfoenix.controls.JFXToggleButton;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import stream.H264StreamEncoder;
import stream.RTPpacket;
import stream.Server;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ResourceBundle;
import java.util.concurrent.*;


public class StreamerHubController implements Initializable {
    public JFXToggleButton desktopToggle;
    public JFXToggleButton audioToggle;
    public JFXToggleButton videoToggle;
    public ImageView loading;
    @FXML
    private ImageView display;

    private RotateTransition rt;
    private Webcam webcam;
    private TargetDataLine line;
    private Robot robot;
    private Rectangle area;
    private int videoStatus,audioStatus;
    protected final H264StreamEncoder h264StreamEncoder=new H264StreamEncoder(new Dimension(640,480));
    protected final H264StreamEncoder audioEncoder=new H264StreamEncoder(new Dimension(640,480));
    protected boolean isStreaming;
    protected ScheduledExecutorService timeWorker;
    protected ScheduledFuture<?> imageGrabTaskFuture,audioGrabFuture;
    protected ExecutorService encodeWorker,encodeAudioWorker;

    private DatagramSocket multicastAudioSocket,multicastVideoSocket;
    private InetAddress multicastGroup;

    public static final int FRAME_PERIOD=1000/60;
    public static final int AUDIO_FRAME_PERIOD=11;
    public static final int AUDIO_PORT=10005;
    public static final int VIDEO_PORT=10006;
    public static final int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    public static final int MPA_TYPE = 14; //RTP payload type for MPA audio
    private int imagenb;
    private int audionb;
    private long initTime;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Animation when streaming is paused
        rt=new RotateTransition(Duration.millis(6000), loading);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.playFromStart();
        //Setting up webcam
        Webcam.setAutoOpenMode(true);
        webcam=Webcam.getDefault();
        webcam.setViewSize(new Dimension(640,480));
        //Setting up audio recorder
        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0F, 16, 2, 4, 44100, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class,
                format);
        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Error, line not supported");
        }
        line=null;
        try {

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
        //Setting up Robot for desktop capture
        try {
            robot = new Robot();
            area = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        }
        catch (AWTException e){
            System.err.println("Error initialising Robot");
        }

        try {
            this.multicastVideoSocket=new DatagramSocket();
            this.multicastAudioSocket=new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }


        videoStatus=0;
        audioStatus=0;


        videoToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(!newValue&&!desktopToggle.isSelected()){
                videoStatus=0;
                webcam.close();

//                stopVideoGrabber();
                display.setVisible(false);
                loading.setVisible(true);
                rt.playFromStart();
            }
            else if(!newValue&&desktopToggle.isSelected()){
                videoStatus=2;
                webcam.close();


            }
            else if(newValue&&desktopToggle.isSelected()){
                try {
                    webcam.open();
                }
                catch (Exception e){
                    Alert a=new Alert(Alert.AlertType.ERROR);
                    a.setContentText("Could not open webcam");
                    videoToggle.fire();
                    a.show();
                }
                videoStatus=3;
            }
            else if(newValue){
                try {
                    webcam.open();
                }
                catch (Exception e){
                    Alert a=new Alert(Alert.AlertType.ERROR);
                    a.setContentText("Could not open webcam");
                    videoToggle.fire();
                    a.show();
                }
                rt.stop();
                loading.setRotate(0);
                display.setVisible(true);
                loading.setVisible(false);

//                if(imageGrabTaskFuture.isCancelled())
//                    startVideoGrabber();
                videoStatus=1;
            }

        });

        desktopToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(!newValue&&!videoToggle.isSelected()){
                videoStatus=0;
//                stopVideoGrabber();
                display.setVisible(false);
                loading.setVisible(true);
                rt.playFromStart();
            }
            else if(!newValue&&videoToggle.isSelected()){
                videoStatus=1;
            }
            else if(newValue&&videoToggle.isSelected()){
                videoStatus=3;
            }
            else if(newValue){
                videoStatus=2;
                rt.stop();
                loading.setRotate(0);
                display.setVisible(true);
                loading.setVisible(false);

//                if(imageGrabTaskFuture.isCancelled())
//                startVideoGrabber();
            }
        });
        audioToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){

                startAudioGrabber();
                audioStatus=1;
            }
            else{

                stopAudioGrabber();
                audioStatus=0;
            }
        });

        this.timeWorker = new ScheduledThreadPoolExecutor(5);

        try {
            this.multicastGroup = InetAddress.getByName("230.0.0.0");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        startVideoGrabber();

    }

    private void startVideoGrabber(){

        imageGrabTaskFuture=timeWorker.scheduleWithFixedDelay(new ImageGrabTask(),
                0,
                FRAME_PERIOD,
                TimeUnit.MILLISECONDS);
    }
    private void stopVideoGrabber(){
        imageGrabTaskFuture.cancel(true);
    }

    private void startAudioGrabber(){
        line.start();
        audioGrabFuture=timeWorker.scheduleWithFixedDelay(new AudioGrabTask(),
                0,
                AUDIO_FRAME_PERIOD,
                TimeUnit.MILLISECONDS
                );
    }
    private void stopAudioGrabber(){
        audioGrabFuture.cancel(true);
        line.stop();
    }

    public void startStream(){
        //TODO:Send notifications
        isStreaming=true;
        imagenb=0;
        audionb=0;
        initTime=System.currentTimeMillis();
        this.encodeWorker = Executors.newSingleThreadExecutor();
        this.encodeAudioWorker= Executors.newSingleThreadExecutor();
        startVideoGrabber();
    }
    public void stopStream(){
        isStreaming=false;
        if(videoToggle.isSelected())videoToggle.fire();
        if(desktopToggle.isSelected())desktopToggle.fire();
        if(audioToggle.isSelected())audioToggle.fire();
        stopVideoGrabber();
        encodeWorker.shutdown();
        encodeAudioWorker.shutdown();
    }
    private class ImageGrabTask implements Runnable{

        @Override
        public void run() {

            BufferedImage img;
            switch (videoStatus){
                case 1:
                    img= webcam.getImage();
                    break;
                case 2:
                    img= captureScreen();
                    break;
                case 3:
                    img=captureScreenWithWebcam();
                    break;
                default:
                    img=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);

            }
//            System.out.println(img.getType());
            display.setImage(SwingFXUtils.toFXImage(img,null));

            if(isStreaming)
            encodeWorker.execute(new EncodeVideoTask(img));
        }
        private BufferedImage captureScreen(){
            BufferedImage img= robot.createScreenCapture(area),finImg=new BufferedImage(640, 480, img.getType());;
            Graphics2D g=img.createGraphics(),fg=finImg.createGraphics();
            try {
                java.awt.Image cursor = ImageIO.read(getClass().getResource("photo/cursor.png"));
                int x = MouseInfo.getPointerInfo().getLocation().x;
                int y = MouseInfo.getPointerInfo().getLocation().y;
                g.drawImage(cursor,x,y,16,16,null);
                g.dispose();
                fg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                fg.drawImage(img, 0, 0, 640, 480, 0, 0, img.getWidth(),
                        img.getHeight(), null);
                fg.dispose();
            }catch (IOException e){e.printStackTrace();}
            return finImg;
        }
        private BufferedImage captureScreenWithWebcam(){
            BufferedImage img=captureScreen();
            Graphics2D g=img.createGraphics();
            BufferedImage camCap=webcam.getImage();
            g.drawImage(camCap,480,360,160,120,null);
            g.dispose();
            return img;
        }
    }

    private class EncodeVideoTask implements Runnable{
        private final BufferedImage image;

        public EncodeVideoTask(BufferedImage image) {
            super();
            this.image = image;
        }

        @Override
        public void run() {
            try {
                imagenb++;
//                System.out.println(imagenb+":"+image);
                ByteBuffer msg = h264StreamEncoder.encode(image);
//                System.out.println("Encoded "+imagenb+":"+msg);
                if(msg==null)
                    System.err.println("%%%%%%%%%%%%%%%ERROR%%%%%%%%%%%%%%%%%%");
                if (msg != null) {
                    //Builds an stream.RTPpacket object containing the frame
                    int size=msg.remaining();
                    byte[] buf=new byte[size];
                    msg.get(buf);
                    RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, (int)(System.currentTimeMillis()-initTime), buf, size);

                    //get to total length of the full rtp packet to send
                    int packet_length = rtp_packet.getlength();

                    //retrieve the packet bitstream and store it in an array of bytes
                    byte[] packet_bits = new byte[packet_length];
                    rtp_packet.getpacket(packet_bits);

                    //send the packet as a DatagramPacket over the UDP socket
                    DatagramPacket senddp = new DatagramPacket(packet_bits, packet_length, multicastGroup, VIDEO_PORT);
                    multicastVideoSocket.send(senddp);

//                        System.out.println("Send frame #" + imagenb + ", Frame size: " + image_length + " (" + buf.length + ")");
                    //print the header bitstream
                    rtp_packet.printheader();


                    System.out.println("Send video frame #" + imagenb);
                }


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class AudioGrabTask implements Runnable{

        @Override
        public void run() {

            int numBytesRead;
            byte[] data = new byte [4096];

            numBytesRead =	line.read(data, 0, 4096);
            if(isStreaming)
            encodeAudioWorker.execute(new EncodeAudioTask(data,numBytesRead));
        }
    }


    private class EncodeAudioTask implements Runnable{
        private final byte[] audioData;
        private final int audioBytesRead;

        public EncodeAudioTask(byte[] audioData, int audioBytesRead) {
            super();
            this.audioData = audioData;
            this.audioBytesRead = audioBytesRead;
        }

        @Override
        public void run() {
            try {
                audionb++;
//                ByteBuffer msg=null;
//                if ( audioBytesRead > 0 ) {
//                    msg = audioEncoder.encode(audioData,audioBytesRead);
//                }
//                if ( msg != null ) {
//                    int size=msg.remaining();
//                    byte[] buf=new byte[size];
//                    msg.get(buf);
//                    RTPpacket rtp_packet = new RTPpacket(MPA_TYPE, audionb, (int)(System.currentTimeMillis()-initTime), buf, size);
//
//                    //get to total length of the full rtp packet to send
//                    int packet_length = rtp_packet.getlength();
//
//                    //retrieve the packet bitstream and store it in an array of bytes
//                    byte[] packet_bits = new byte[packet_length];
//                    rtp_packet.getpacket(packet_bits);
//
//                    //send the packet as a DatagramPacket over the UDP socket
//                    DatagramPacket senddp = new DatagramPacket(packet_bits, packet_length, multicastGroup, AUDIO_PORT);
//                    multicastAudioSocket.send(senddp);
//                    //print the header bitstream
//                    rtp_packet.printheader();
//                    System.out.println("Send audio frame #" + audionb);
//                }

                RTPpacket rtp_packet = new RTPpacket(MPA_TYPE, audionb, (int)(System.currentTimeMillis()-initTime), audioData, audioBytesRead);

                    //get to total length of the full rtp packet to send
                    int packet_length = rtp_packet.getlength();

                    //retrieve the packet bitstream and store it in an array of bytes
                    byte[] packet_bits = new byte[packet_length];
                    rtp_packet.getpacket(packet_bits);

                    //send the packet as a DatagramPacket over the UDP socket
                    DatagramPacket senddp = new DatagramPacket(packet_bits, packet_length, multicastGroup, AUDIO_PORT);
                    multicastAudioSocket.send(senddp);
                    //print the header bitstream
                    rtp_packet.printheader();
                    System.out.println("Send audio frame #" + audionb);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


}
