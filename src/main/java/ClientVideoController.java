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
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import stream.*;
public class ClientVideoController implements Initializable {
    public AnchorPane rootPane;
    public ImageView playPauseImg;
    public ImageView loading;
    public JFXToggleButton audioToggle;
    //GUI
    //----
    @FXML
    ImageView video;

    private JFXSnackbar snackbar;


    //RTP variables:
    //----------------
    DatagramPacket rcvdp;            //UDP packet received from the server
    MulticastSocket multicastAudioSocket,multicastVideoSocket;        //socket to be used to send and receive UDP packets

    Timeline videoTimer; //timer used to receive data from the UDP socket
    protected ScheduledExecutorService timeWorker;
    protected ScheduledFuture<?> audioGrabFuture;
    byte[] buf;  //buffer used to store data received from the server
    private int playing;
    private boolean playingAudio;

    private InetAddress multicastGroup;
    FrameSynchronizer fsynch;
    private SourceDataLine soundLine;

    Dimension dimension;
    protected final IStreamCoder iStreamCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_H264);
    protected final IStreamCoder iAudioStreamCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_AAC);
    protected final ConverterFactory.Type type = ConverterFactory.findRegisteredConverter(ConverterFactory.XUGGLER_BGR_24);

    private RotateTransition rt;

    @Override
    public void initialize(URL url, ResourceBundle rb){
        try {
            videoTimer=new Timeline(
                    new KeyFrame(
                            Duration.millis(20),
                            new videoFrameListener()
                    )

            );
            videoTimer.setCycleCount(Timeline.INDEFINITE);
            this.timeWorker = new ScheduledThreadPoolExecutor(5);

            snackbar = new JFXSnackbar(rootPane);

            buf = new byte[63800];
            multicastVideoSocket=new MulticastSocket(StreamerHubController.VIDEO_PORT);
            multicastAudioSocket=new MulticastSocket(StreamerHubController.AUDIO_PORT);
            multicastGroup=InetAddress.getByName("230.0.0.0");  //230.0.0.0 is our multicast server
            multicastVideoSocket.joinGroup(multicastGroup);
            multicastAudioSocket.joinGroup(multicastGroup);
            multicastVideoSocket.setSoTimeout(5);
            multicastAudioSocket.setSoTimeout(5);
            //create the frame synchronizer
            fsynch = new FrameSynchronizer(100);
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
                    if(playing==1)
                    stopAudio();
                }
                else {
                    playingAudio=true;
                    if(playing==1)
                    startAudio();
                }

            });


        }catch(Exception ex){ex.printStackTrace();}

        rt=new RotateTransition(Duration.millis(6000), loading);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.playFromStart();

        playing=0;

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
            rcvdp = new DatagramPacket(buf, buf.length);

            try {
                multicastVideoSocket.receive(rcvdp);
                loading.setVisible(false);
                videoTimer.play();
                if(playingAudio)
                    startAudio();
                playPauseImg.setImage(new Image(getClass().getResource("photo/pause.png").toString()));
                playing=1;
            } catch (Exception e) {
                System.out.println("Streaming is over");
            }

        }
        else{
            videoTimer.pause();
            if(playingAudio)
                stopAudio();
            playPauseImg.setImage(new Image(getClass().getResource("photo/play-button.png").toString()));
            loading.setVisible(true);
            playing=0;
        }
    }

    public void startAudio(){
        audioGrabFuture=timeWorker.scheduleWithFixedDelay(new audioFrameListener(),
                0,
                StreamerHubController.AUDIO_FRAME_PERIOD,
                TimeUnit.MILLISECONDS
        );
    }

    public void stopAudio(){
        audioGrabFuture.cancel(true);
    }


    //------------------------------------
    //Handler for timer
    //------------------------------------
    class videoFrameListener implements EventHandler<ActionEvent>  {
        private int nullCount=0;

        @Override
        public void handle(ActionEvent e){

            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(buf, buf.length);

            try {
                //receive the DP from the socket, save time for stats
                multicastVideoSocket.receive(rcvdp);
                nullCount=0;

                //create an stream.RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                int seqNb = rtp_packet.getsequencenumber();

                //this is the highest seq num received

                //print important header fields of the RTP packet received:
                System.out.println("Got Video RTP packet with SeqNum # " + seqNb
                        + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
                        + rtp_packet.getpayloadtype());

                //print header bitstream:
                rtp_packet.printheader();

                //get the payload bitstream from the stream.RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                System.out.println(payload_length);
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);
                System.out.println(audioGrabFuture.isCancelled());
                    //get an Image object from the payload bitstream
                    decodeImage(payload,payload_length);
//                    fsynch.addFrame(image, seqNb);
//                    videoBuffer.add(image);
//                    bufEnd++;
//                    if (bufEnd - bufStart + 1 > bufferSize) {
//                        videoBuffer.remove(0);
//                        bufStart++;
//                    }



            }
            catch (InterruptedIOException iioe) {
                nullCount++;
                System.out.println("Nothing to read");
                if(nullCount==50) {
                    snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Streaming ended or was interrupted")));
                    System.exit(0);
//                stopBtn.fire();  TODO: Handle end of stream
                }
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
                snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Streamer is unavailable at this moment")));
//                stopBtn.fire();   TODO: Handle end of stream
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
                            //BufferedImage convertedImage = stream.ImageUtils.convertToType(image, BufferedImage.TYPE_3BYTE_BGR);
                            //here ,put out the image
                            converter.delete();
                            //clean the picture and reuse it
                            picture.getByteBuffer().clear();
                            //display the image as an Image object

                            video.setImage(SwingFXUtils.toFXImage(image,null));

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

                //this is the highest seq num received

                //print important header fields of the RTP packet received:
                System.out.println("Got Audio RTP packet with SeqNum # " + seqNb
                        + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
                        + rtp_packet.getpayloadtype());

                //print header bitstream:
                rtp_packet.printheader();

                //get the payload bitstream from the stream.RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                System.out.println(payload_length);
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                //get an AudioSample object from the payload bitstream
                decodeAndPlay(payload,payload_length);
//                    fsynch.addFrame(image, seqNb);
//                    videoBuffer.add(image);
//                    bufEnd++;
//                    if (bufEnd - bufStart + 1 > bufferSize) {
//                        videoBuffer.remove(0);
//                        bufStart++;
//                    }



            }
            catch (InterruptedIOException iioe) {
                iioe.printStackTrace();

            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
                snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Streamer is unavailable at this moment")));
//                stopBtn.fire();   TODO: Handle end of stream
            }
        }

        void decodeAndPlay(byte[] payload,int len){
            IPacket iPacket = IPacket.make(IBuffer.make(null, payload, 0, len));

            IAudioSamples samples = IAudioSamples.make(1024, 1);

            /*
             * A packet can actually contain multiple sets of samples (or frames of samples
             * in audio-decoding speak).  So, we may need to call decode audio multiple
             * times at different offsets in the packet's data.  We capture that here.
             */
            int offset = 0;

            /*
             * Keep going until we've processed all data
             */
            while(offset < iPacket.getSize()) {
                int bytesDecoded = iAudioStreamCoder.decodeAudio(samples, iPacket, offset);
                if (bytesDecoded < 0)
                    throw new RuntimeException("got error decoding audio in stream");

                offset += bytesDecoded;

                /*
                 * Some decoder will consume data in a packet, but will not be able to construct
                 * a full set of samples yet.  Therefore you should always check if you
                 * got a complete set of samples from the decoder
                 */
                if (samples.isComplete()) {
                    byte[] rawBytes = samples.getData().getByteArray(0, samples.getSize());
                    soundLine.write(rawBytes, 0, rawBytes.length);
                    return;
                }
            }
        }
    }




    //------------------------------------
    //Synchronize frames
    //------------------------------------
    class FrameSynchronizer {

        private ArrayDeque<Image> queue;
        private int bufSize;
        private int curSeqNb;
        private Image lastImage;

        public FrameSynchronizer(int bsize) {
            curSeqNb = 1;
            bufSize = bsize;
            queue = new ArrayDeque<Image>(bufSize);
        }

        //synchronize frames based on their sequence number
        public void addFrame(Image image, int seqNum) {
            if (seqNum < curSeqNb) {
                queue.add(lastImage);
            }
            else if (seqNum > curSeqNb) {
                for (int i = curSeqNb; i < seqNum; i++) {
                    queue.add(lastImage);
                }
                queue.add(image);
            }
            else {
                queue.add(image);
            }

        }

        //get the next synchronized frame
        public Image nextFrame() {
            curSeqNb++;
            lastImage = queue.peekLast();
            return queue.remove();
        }
    }
}
