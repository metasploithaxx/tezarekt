import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.controls.JFXSnackbarLayout;
import com.jfoenix.controls.JFXToggleButton;
import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import javafx.animation.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import stream.RTCPpacket;
import stream.RTPpacket;
import stream.Server;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

import stream.*;
public class ClientVideoController implements Initializable {
    public AnchorPane rootPane;
    public ImageView playPauseImg;
    public ImageView loading;
    public JFXToggleButton audioToggle;
    public JFXButton playPauseBtn;

    //GUI
    //----
    @FXML
    private ImageView video;
    private JFXSnackbar snackbar;


    //RTP variables:
    //----------------
    DatagramPacket rcvdp;            //UDP packet received from the server
    MulticastSocket multicastAudioSocket,multicastVideoSocket;        //socket to be used to send and receive UDP packets

    Timeline videoTimer; //timer used to receive data from the UDP socket
    protected ScheduledExecutorService timeWorker;
    protected ScheduledFuture<?> audioGrabFuture,syncer;
    byte[] buf;  //buffer used to store data received from the server
    private int playing;
    private boolean playingAudio,startedPlaying;
//    protected ExecutorService decodeVideoWorker,decodeAudioWorker;

    private InetAddress multicastGroup;
    FrameSynchronizer fsynch;
    private SourceDataLine soundLine;
    int currTime,currStream,lastTime; //Used to sync frames

    Dimension dimension;
    protected final IStreamCoder iStreamCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_H264);
    protected final IStreamCoder iAudioStreamCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_AAC);
    protected final ConverterFactory.Type type = ConverterFactory.findRegisteredConverter(ConverterFactory.XUGGLER_BGR_24);

    private RotateTransition rt;

    private int videoNullCount=0,audioNullCount=0;

    @Override
    public void initialize(URL url, ResourceBundle rb){
        try {
            videoTimer=new Timeline(
                    new KeyFrame(
                            Duration.millis(15),
                            new videoFrameListener()
                    )

            );
            videoTimer.setCycleCount(Timeline.INDEFINITE);
            this.timeWorker = new ScheduledThreadPoolExecutor(15);

            snackbar = new JFXSnackbar(rootPane);

            buf = new byte[63800];
            multicastVideoSocket=new MulticastSocket(StreamerHubController.VIDEO_PORT);
            multicastAudioSocket=new MulticastSocket(StreamerHubController.AUDIO_PORT);
            multicastGroup=InetAddress.getByName("230.0.0.0");  //230.0.0.0 is our multicast server
            multicastVideoSocket.joinGroup(multicastGroup);
            multicastAudioSocket.joinGroup(multicastGroup);
            multicastVideoSocket.setSoTimeout(5);
            multicastAudioSocket.setSoTimeout(5);
            multicastVideoSocket.setTimeToLive(0);
            multicastAudioSocket.setTimeToLive(0);
            //create the frame synchronizer
            dimension=new Dimension(640,480);

            iStreamCoder.open(null, null);
            iAudioStreamCoder.open(null, null);

            AudioFormat audioFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100.0F, 16, 2, 4, 44100, false);

            AudioFormat format = new AudioFormat(44100,
                    16,
                    2,
                    true, /* xuggler defaults to signed 16 bit samples */
                    false);
            final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            soundLine = (SourceDataLine) AudioSystem.getLine(info);
            soundLine.open(audioFormat);
            soundLine.start();

            audioToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {

                if(!newValue) {
                    playingAudio=false;
                    System.err.println("MUTED AUDIO");
                    soundLine.flush();
//                    if(playing==1)
//                    stopAudio();
                }
                else {
                    System.err.println("PLAYING AUDIO");
                    playingAudio=true;
//                    if(playing==1)
//                    startAudio();
                }

            });


        }catch(Exception ex){ex.printStackTrace();}

        rt=new RotateTransition(Duration.millis(6000), loading);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.playFromStart();

        playing=0;
        startedPlaying=false;
        currTime=lastTime=0;
        currStream=0;
    }

    public void exit(){
        ((Stage)playPauseBtn.getScene().getWindow()).close();
    }

    public void stopAll(){
        loading.setVisible(true);
        stopSync();
        videoTimer.stop();
        stopAudio();
        timeWorker.shutdown();
        try {
            multicastAudioSocket.leaveGroup(multicastGroup);
            multicastVideoSocket.leaveGroup(multicastGroup);
            multicastAudioSocket.close();
            multicastVideoSocket.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        iStreamCoder.close();
        iAudioStreamCoder.close();
        audioToggle.setDisable(true);
        playPauseBtn.setDisable(true);
    }



    /*
    ------------------------------------
    Handler for buttons
    ------------------------------------
    TODO:Handler for Play/Pause button
    -----------------------
    */
    public void playPause(){
        if(playing==0) {
            if(!startedPlaying){
                videoTimer.play();
                startAudio();
                startSync();
                startedPlaying=true;
            }
            rcvdp = new DatagramPacket(buf, buf.length);


                loading.setVisible(false);

                playPauseImg.setImage(new Image(getClass().getResource("photo/pause.png").toString()));
                playing=1;
            System.err.println("PLAYED");


        }
        else{
//            videoTimer.pause();
//            if(playingAudio)
//                stopAudio();
            playPauseImg.setImage(new Image(getClass().getResource("photo/play-button.png").toString()));
            loading.setVisible(true);
            playing=0;

            System.err.println("PAUSED");
        }
    }

    public void startAudio(){
        audioGrabFuture=timeWorker.scheduleAtFixedRate(new audioFrameListener(),
                0,
                8,
                TimeUnit.MILLISECONDS
        );
    }

    public void stopAudio(){
        audioGrabFuture.cancel(true);
    }

    public void startSync(){
        syncer=timeWorker.scheduleWithFixedDelay(new FrameSynchronizer(),
                0,
                6,
                TimeUnit.MILLISECONDS
        );
    }

    public void stopSync(){
        syncer.cancel(true);
    }


    //------------------------------------
    //Handler for timer
    //------------------------------------
    class videoFrameListener implements EventHandler<ActionEvent>  {


        @Override
        public void handle(ActionEvent e){

            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(buf, buf.length);

            try {
                //receive the DP from the socket, save time for stats
                multicastVideoSocket.receive(rcvdp);

                //create an stream.RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                int seqNb = rtp_packet.getsequencenumber();
                currStream=1;
                lastTime=currTime;
                currTime=rtp_packet.gettimestamp();
                //this is the highest seq num received

                //print important header fields of the RTP packet received:
                System.out.println("Got Video RTP packet with SeqNum # " + seqNb
                        + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
                        + rtp_packet.getpayloadtype());

                //print header bitstream:
//                rtp_packet.printheader();
                if(rtp_packet.getpayloadtype()==StreamerHubController.MJPEG_TYPE) {
                    //get the payload bitstream from the stream.RTPpacket object
                    int payload_length = rtp_packet.getpayload_length();
//                System.out.println(payload_length);
                    byte[] payload = new byte[payload_length];
                    rtp_packet.getpayload(payload);
                System.out.println(audioGrabFuture.isCancelled());
                    System.out.println(audioGrabFuture.isDone());
                    //get an Image object from the payload bitstream
                    if(playing==1)
                    decodeImage(payload, payload_length);
//                    fsynch.addFrame(image, seqNb);
//                    videoBuffer.add(image);
//                    bufEnd++;
//                    if (bufEnd - bufStart + 1 > bufferSize) {
//                        videoBuffer.remove(0);
//                        bufStart++;
//                    }

                }

            }
            catch (InterruptedIOException iioe) {
                videoNullCount++;
                System.out.println("No video to read");

//                }
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
                snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Streamer is unavailable at this moment")));
                stopAll();
            }
        }

        void decodeImage(byte[] buf,int len) {

            IPacket iPacket = IPacket.make(IBuffer.make(null, buf, 0, len));


//            logger.info("packet stream index: " + iPacket.getFlags());

            if (iPacket.getByteBuffer().get() != -1) {
                IVideoPicture picture = IVideoPicture.make(IPixelFormat.Type.YUV420P,
                        dimension.width, dimension.height);
                try {
                    // decode the packet into the video picture
                    int postion = 0;
                    int packageSize = iPacket.getSize();
                    while (postion < packageSize) {
                        postion += iStreamCoder.decodeVideo(picture, iPacket, postion);
                        if (postion < 0)
                            throw new RuntimeException("error "
                                    + " decoding video");
                        // if this is a complete picture, dispatch the picture
                        if (picture.isComplete()) {
                            IConverter converter = ConverterFactory.createConverter(type
                                    .getDescriptor(), picture);
                            BufferedImage image = converter.toImage(picture);
                            BufferedImage convertedImage = stream.ImageUtils.convertToType(image, BufferedImage.TYPE_3BYTE_BGR);
                            //here ,put out the image
                            converter.delete();
                            //clean the picture and reuse it
                            picture.getByteBuffer().clear();
                            //display the image as an Image object

                            video.setImage(SwingFXUtils.toFXImage(convertedImage,null));

                        } else {
                            picture.delete();
                            iPacket.delete();
                            return;
                        }

                    }
                } finally {
                    if (picture != null)
                        picture.delete();
                    iPacket.delete();
                    // ByteBufferUtil.destroy(data);
                }
            }
        }
    }

    class audioFrameListener implements Runnable{

        @Override
        public void run() {
            rcvdp = new DatagramPacket(buf, buf.length);

            try {
                //receive the DP from the socket, save time for stats
                multicastAudioSocket.receive(rcvdp);

                //create an stream.RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                int seqNb = rtp_packet.getsequencenumber();
                currStream=0;
                lastTime=currTime;
                currTime=rtp_packet.gettimestamp();
                //this is the highest seq num received

                //print important header fields of the RTP packet received:
                System.out.println("Got Audio RTP packet with SeqNum # " + seqNb
                        + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
                        + rtp_packet.getpayloadtype());

                //print header bitstream:
//                rtp_packet.printheader();
                if(rtp_packet.getpayloadtype()==StreamerHubController.MJPEG_TYPE){
                    System.err.println("EEERROORR");
                    System.out.println(audioGrabFuture.isCancelled());
                    System.out.println(audioGrabFuture.isDone());
                    System.out.println(soundLine.isOpen());
                    System.out.println(soundLine.isRunning());
                    System.out.println(soundLine.isActive());
                }
                else {
                    //get the payload bitstream from the stream.RTPpacket object
                    int payload_length = rtp_packet.getpayload_length();
//                System.out.println(payload_length);
                    byte[] payload = new byte[payload_length];
                    rtp_packet.getpayload(payload);

                    //get an AudioSample object from the payload bitstream
                    if(playingAudio)
                        soundLine.write(payload, 0, payload_length);


                }

            }
            catch (InterruptedIOException iioe) {
                System.out.println("No audio to read");
                audioNullCount++;

            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
                snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Streamer is unavailable at this moment")));
                stopAll();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }




    //------------------------------------
    //Synchronize frames
    //------------------------------------
    class FrameSynchronizer implements Runnable{

        @Override
        public void run() {
            if(audioNullCount==100||videoNullCount==50){
                snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Streaming hasn't started or has already ended")));
                stopAll();
            }
            if(currTime<lastTime){
                videoTimer.pause();
                stopAudio();
                loading.setVisible(true);
                if(currStream==0){ //Audio is lagging, forward it to current
                    while(currTime<lastTime) {
                        try {
                            //receive the DP from the socket, save time for stats
                            multicastAudioSocket.receive(rcvdp);

                            //create an stream.RTPpacket object from the DP
                            RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                            currTime = rtp_packet.gettimestamp();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                else{ //Video is lagging
                    while(currTime<lastTime) {
                        try {
                            //receive the DP from the socket, save time for stats
                            multicastVideoSocket.receive(rcvdp);

                            //create an stream.RTPpacket object from the DP
                            RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                            currTime = rtp_packet.gettimestamp();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                loading.setVisible(false);
                videoTimer.play();
                startAudio();
            }
        }
    }
}
