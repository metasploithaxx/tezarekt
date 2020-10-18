import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.controls.JFXSnackbarLayout;
import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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

import stream.*;
public class ClientVideoController implements Initializable {
    public AnchorPane rootPane;
    //GUI
    //----
    @FXML
    ImageView video;
    @FXML
    JFXButton playBtn;
    @FXML
    JFXButton pauseBtn;
    @FXML
    JFXButton stopBtn;

    private JFXSnackbar snackbar;


    //RTP variables:
    //----------------
    DatagramPacket rcvdp;            //UDP packet received from the server
    DatagramSocket RTPsocket;        //socket to be used to send and receive UDP packets
    int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets

    Timeline timer; //timer used to receive data from the UDP socket
    byte[] buf;  //buffer used to store data received from the server

    //RTSP variables
    //----------------
    //rtsp states
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    int state;            //RTSP state == INIT or READY or PLAYING
    Socket RTSPsocket;           //socket used to send/receive RTSP messages
    private InetAddress ServerIPAddr;

    //input and output stream filters
    BufferedReader RTSPBufferedReader;
    BufferedWriter RTSPBufferedWriter;
    private String VideoFileName; //video file to request to the server
    int RTSPSeqNb = 0;           //Sequence number of RTSP messages within the session
    String RTSPid;              // ID of the RTSP session (given by the RTSP stream.Server)

    final static String CRLF = "\r\n";
    final static String DES_FNAME = "session_info.txt";

    //RTCP variables
    //----------------
    DatagramSocket RTCPsocket;          //UDP socket for sending RTCP packets
    int RTCP_RCV_PORT = 19001;   //port where the client will receive the RTP packets
    static int RTCP_PERIOD = 400;       //How often to send RTCP packets
    RtcpSender rtcpSender;


    //Statistics variables:
    //------------------
    double statDataRate;        //Rate of video data received in bytes/s
    int statTotalBytes;         //Total number of bytes received in a session
    double statStartTime;       //Time in milliseconds when start is pressed
    double statTotalPlayTime;   //Time in milliseconds of video playing since beginning
    float statFractionLost;     //Fraction of RTP data packets from sender lost since the prev packet was sent
    int statCumLost;            //Number of packets lost
    int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
    int statHighSeqNb;          //Highest sequence number received in session

    FrameSynchronizer fsynch;
//    ArrayList<Image> videoBuffer;
//    static int bufferSize=200;
//    int bufStart,bufEnd;
//    boolean playingFromBuffer;
    int VIDEO_LENGTH;
    private int FRAME_PERIOD;
    Timeline tempTimer;
    private int SAMPLE_RATE;
    private SourceDataLine soundLine;
    private int AUDIO_CHANNELS;
    private boolean played;
    Dimension dimension;
    protected final IStreamCoder iStreamCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_H264);
    protected final IStreamCoder iAudioStreamCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_AAC);
    protected final ConverterFactory.Type type = ConverterFactory.findRegisteredConverter(ConverterFactory.XUGGLER_BGR_24);


    @Override
    public void initialize(URL url, ResourceBundle rb){
        try {



//            playBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("src/Graphics/play.png"))));
//            pauseBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("src/Graphics/pause.png"))));
//            stopBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("src/Graphics/stop.png"))));
            playBtn.setOnAction(new playButtonListener());
            pauseBtn.setOnAction(new pauseButtonListener());
            stopBtn.setOnAction(new stopButtonListener());
            timer=new Timeline(
                    new KeyFrame(
                            Duration.millis(20),
                            new timerListener()
                    )

            );
            timer.setCycleCount(Timeline.INDEFINITE);

            snackbar = new JFXSnackbar(rootPane);

//            video.setImage(new Image(getClass().getResourceAsStream("src/Graphics/buffering.jpg")));
            //allocate enough memory for the buffer used to receive data from the server
            buf = new byte[63800];

            //init RTCP packet sender
            rtcpSender = new RtcpSender(400);
            ServerIPAddr=InetAddress.getLocalHost(); //TEMP
            //create the frame synchronizer
            fsynch = new FrameSynchronizer(100);
            dimension=new Dimension(640,480);
            played=false;
            iStreamCoder.open(null, null);
            iAudioStreamCoder.open(null, null);

            initVideo();


        }catch(Exception ex){ex.printStackTrace();}
    }

    public void initVideo(){

        int RTSP_server_port = 1051;
        try {
            //Establish a TCP connection with the server to exchange RTSP messages
            //------------------
            RTSPsocket = new Socket(ServerIPAddr, RTSP_server_port);

            //Establish a UDP connection with the server to exchange RTCP control packets
            //------------------

            //Set input and output stream filters:
            RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()));
            RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()));

            DatagramSocket test = null;

            while (true) {
                try {
                    test = new DatagramSocket(RTP_RCV_PORT);
                    test.setReuseAddress(true);
                    break;
                } catch (SocketException e) {
                } finally {
                    if (test != null) {
                        test.close();
                    } else
                        RTP_RCV_PORT++;
                }
            }

            System.out.println("RTP port = " + RTP_RCV_PORT);


            //Init non-blocking RTPsocket that will be used to receive data
            try {
                //construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT
                RTPsocket = new DatagramSocket(RTP_RCV_PORT);
                //UDP socket for sending QoS RTCP packets
                RTCPsocket = new DatagramSocket();
                //set TimeOut value of the socket to 5msec.
                RTPsocket.setSoTimeout(5);
            } catch (SocketException se) {
                System.out.println("Socket exception: " + se);
                System.exit(0);
            }

            //init RTSP sequence number
            RTSPSeqNb = 1;

            //Send SETUP message to the server
            sendRequest("SETUP");

            //Wait for the response
            if (parseServerResponse() != 200)
                System.out.println("Invalid stream.Server Response");
            else {
                //change RTSP state and print new state
                state = READY;
                System.out.println("New RTSP state: READY");
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
//
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setServerIPAddr(InetAddress serverIPAddr) {
        ServerIPAddr = serverIPAddr;
    }

    public void setVideoFileName(String videoFileName) {
        VideoFileName = videoFileName;
    }


    //------------------------------------
    //Handler for buttons
    //------------------------------------



    //Handler for Play button
    //-----------------------
    class playButtonListener implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent e) {

            System.out.println("Play Button pressed!");
            if(!played){
                played=true;
                initVideo();
            }
            //Start to save the time in stats
            statStartTime = System.currentTimeMillis();

            if (state == READY) {

                    System.out.println("Playing from stream");
                    //increase RTSP sequence number
                    RTSPSeqNb++;

                    //Send PLAY message to the server
                    sendRequest("PLAY");

                    //Wait for the response
                    if (parseServerResponse() != 200) {
                        System.out.println("Invalid stream.Server Response");
                    } else {
                        //change RTSP state and print out new state
                        state = PLAYING;
                        System.out.println("New RTSP state: PLAYING");

                        //start the timer
                        timer.play();
                        rtcpSender.startSend();

                }
            }
            //else if state != READY then do nothing
        }
    }

    //Handler for Pause button
    //-----------------------
    class pauseButtonListener implements EventHandler<ActionEvent>  {

        @Override
        public void handle(ActionEvent e){

            System.out.println("Pause Button pressed!");

            if (state == PLAYING)
            {

                    //increase RTSP sequence number
                    RTSPSeqNb++;

                    //Send PAUSE message to the server
                    sendRequest("PAUSE");

                    //Wait for the response
                    if (parseServerResponse() != 200)
                        System.out.println("Invalid stream.Server Response");
                    else {
                        //change RTSP state and print out new state
                        state = READY;
                        System.out.println("New RTSP state: READY");

                        //stop the timer
                        timer.pause();
                        rtcpSender.stopSend();
                    }

            }
            //else if state != PLAYING then do nothing
        }
    }

    //Handler for Teardown button
    //-----------------------
    class stopButtonListener implements EventHandler<ActionEvent>  {

        @Override
        public void handle(ActionEvent e){

            System.out.println("Stop Button pressed !");

            //increase RTSP sequence number
            RTSPSeqNb++;

            //Send TEARDOWN message to the server
            sendRequest("TEARDOWN");

            //Wait for the response
            if (parseServerResponse() != 200)
                System.out.println("Invalid stream.Server Response");
            else {
                //change RTSP state and print out new state
                state = INIT;
                System.out.println("New RTSP state: INIT");


                    //stop the timer
                    timer.stop();
                    rtcpSender.stopSend();
                    soundLine.close();

            }
        }
    }



    //------------------------------------
    //Handler for timer
    //------------------------------------
    class timerListener implements EventHandler<ActionEvent>  {
        private int nullCount=0;

        @Override
        public void handle(ActionEvent e){

            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(buf, buf.length);

            try {
                //receive the DP from the socket, save time for stats
                RTPsocket.receive(rcvdp);
                nullCount=0;
                double curTime = System.currentTimeMillis();
                statTotalPlayTime += curTime - statStartTime;
                statStartTime = curTime;

                //create an stream.RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                int seqNb = rtp_packet.getsequencenumber();

                //this is the highest seq num received

                //print important header fields of the RTP packet received:
                System.out.println("Got RTP packet with SeqNum # " + seqNb
                        + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
                        + rtp_packet.getpayloadtype());

                //print header bitstream:
                rtp_packet.printheader();

                //get the payload bitstream from the stream.RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                System.out.println(payload_length);
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                //compute stats and update the label in GUI
                statExpRtpNb++;
                if (seqNb > statHighSeqNb) {
                    statHighSeqNb = seqNb;
                }
                if (statExpRtpNb != seqNb) {
                    statCumLost++;
                }
                statDataRate = statTotalPlayTime == 0 ? 0 : (statTotalBytes / (statTotalPlayTime / 1000.0));
                statFractionLost = (float)statCumLost / statHighSeqNb;
                statTotalBytes += payload_length;

                if(rtp_packet.getpayloadtype()== Server.MJPEG_TYPE) {
                    //get an Image object from the payload bitstream
                    BufferedImage img=decodeImage(payload,payload_length);
//                    fsynch.addFrame(image, seqNb);
//                    videoBuffer.add(image);
//                    bufEnd++;
//                    if (bufEnd - bufStart + 1 > bufferSize) {
//                        videoBuffer.remove(0);
//                        bufStart++;
//                    }

                    //display the image as an Image object

                    video.setImage(SwingFXUtils.toFXImage(img,null));
                }
                else{
                    decodeAndPlay(payload,payload_length);
//                    soundLine.write(payload,0,payload_length);
                }
//                seekbar.setValue(bufEnd);
//                System.out.println(seekbar.getValue()+"/"+seekbar.getMax()+" "+VIDEO_LENGTH);
            }
            catch (InterruptedIOException iioe) {
                nullCount++;
                System.out.println("Nothing to read");
                if(nullCount==10) {
                    snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Video ended or was interrupted")));
                stopBtn.fire();
                }
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
                snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Creator is unavailable at this moment")));
                stopBtn.fire();
            }
        }

        BufferedImage decodeImage(byte[] buf,int len) {

            IPacket iPacket = IPacket.make(IBuffer.make(null, buf, 0, len));

            if (!iPacket.isComplete()) {
                return null;
            }
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
                            return image;

                        } else {
                            picture.delete();
                            iPacket.delete();
                            return null;
                        }

                    }
                } finally {
                    if (picture != null)
                        picture.delete();
                    iPacket.delete();
                    // ByteBufferUtil.destroy(data);
                }
            }
            return null;
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
    // Send RTCP control packets for QoS feedback
    //------------------------------------
    class RtcpSender extends TimerTask {

        private Timer rtcpTimer;
        int interval;
        private int nullCount=0;
        // Stats variables
        private int numPktsExpected;    // Number of RTP packets expected since the last RTCP packet
        private int numPktsLost;        // Number of RTP packets lost since the last RTCP packet
        private int lastHighSeqNb;      // The last highest Seq number received
        private int lastCumLost;        // The last cumulative packets lost
        private float lastFractionLost; // The last fraction lost

        Random randomGenerator;         // For testing only

        public RtcpSender(int interval) {
            this.interval = interval;


            randomGenerator = new Random();
        }



        @Override
        public void run() {

            // Calculate the stats for this period
            numPktsExpected = statHighSeqNb - lastHighSeqNb;
            numPktsLost = statCumLost - lastCumLost;
            lastFractionLost = numPktsExpected == 0 ? 0f : (float)numPktsLost / numPktsExpected;
            lastHighSeqNb = statHighSeqNb;
            lastCumLost = statCumLost;

            //To test lost feedback on lost packets
            // lastFractionLost = randomGenerator.nextInt(10)/10.0f;

            RTCPpacket rtcp_packet = new RTCPpacket(lastFractionLost, statCumLost, statHighSeqNb);
            int packet_length = rtcp_packet.getlength();
            byte[] packet_bits = new byte[packet_length];
            rtcp_packet.getpacket(packet_bits);

            try {
                DatagramPacket dp = new DatagramPacket(packet_bits, packet_length, ServerIPAddr, RTCP_RCV_PORT);
                RTCPsocket.send(dp);
                nullCount=0;
            } catch (InterruptedIOException iioe) {
                nullCount++;
                System.out.println("Nothing to read");
                if(nullCount==10) {
                    snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Video ended or was interrupted")));
                stopBtn.fire();
                }
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
                snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout("Creator is unavailable at this moment")));
                stopBtn.fire();
            }
        }

        // Start sending RTCP packets
        public void startSend() {
            rtcpTimer = new Timer(true);

            rtcpTimer.schedule(new RtcpSender(interval),0,interval);
        }

        // Stop sending RTCP packets
        public void stopSend() {
            rtcpTimer.cancel();
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

    //------------------------------------
    //Parse stream.Server Response
    //------------------------------------
    private int parseServerResponse() {
        int reply_code = 0;

        try {
            //parse status line and extract the reply_code:
            String StatusLine = RTSPBufferedReader.readLine();
            System.out.println("RTSP ClientVideo - Received from stream.Server:");
            System.out.println(StatusLine);

            StringTokenizer tokens = new StringTokenizer(StatusLine);
            tokens.nextToken(); //skip over the RTSP version
            reply_code = Integer.parseInt(tokens.nextToken());

            //if reply code is OK get and print the 2 other lines
            if (reply_code == 200) {
                String SeqNumLine = RTSPBufferedReader.readLine();
                System.out.println(SeqNumLine);

                String SessionLine = RTSPBufferedReader.readLine();
                System.out.println(SessionLine);

                tokens = new StringTokenizer(SessionLine);
                String temp = tokens.nextToken();
                //if state == INIT gets the Session Id from the SessionLine
                if (state == INIT && temp.compareTo("Session:") == 0) {
                    RTSPid = tokens.nextToken();
                }
                else if (temp.compareTo("Content-Base:") == 0) {
                    // Get the DESCRIBE lines
                    String newLine;
                    for (int i = 0; i < 6; i++) {
                        newLine = RTSPBufferedReader.readLine();
                        System.out.println(newLine);
                    }
                }
                String VideoSizeLine=RTSPBufferedReader.readLine();
                System.out.println(VideoSizeLine);
                tokens = new StringTokenizer(VideoSizeLine);
                temp = tokens.nextToken();

                    VIDEO_LENGTH = Integer.parseInt(tokens.nextToken());

                String FramePeriodLine=RTSPBufferedReader.readLine();
                System.out.println(FramePeriodLine);
                tokens = new StringTokenizer(FramePeriodLine);
                temp = tokens.nextToken();

                    FRAME_PERIOD = Integer.parseInt(tokens.nextToken());
                 String SampleRateLine=RTSPBufferedReader.readLine();
                System.out.println(SampleRateLine);
                tokens = new StringTokenizer(SampleRateLine);
                tokens.nextToken();
                SAMPLE_RATE=Integer.parseInt(tokens.nextToken());
                String AudioChannelLine=RTSPBufferedReader.readLine();
                System.out.println(AudioChannelLine);
                tokens = new StringTokenizer(AudioChannelLine);
                tokens.nextToken();
                AUDIO_CHANNELS=Integer.parseInt(tokens.nextToken());
                String PortLine=RTSPBufferedReader.readLine();
                System.out.println(PortLine);
                tokens = new StringTokenizer(PortLine);
                tokens.nextToken();
                RTCP_RCV_PORT=Integer.parseInt(tokens.nextToken());
            }
        } catch(Exception ex) {
            System.out.println("Exception caught: "+ex);
//            System.exit(0);
        }

        return(reply_code);
    }



    //------------------------------------
    //Send RTSP Request
    //------------------------------------

    private void sendRequest(String request_type) {
        try {
            //Use the RTSPBufferedWriter to write to the RTSP socket

            //write the request line:
            RTSPBufferedWriter.write(request_type + " " + VideoFileName + " RTSP/1.0" + CRLF);

            //write the CSeq line:
            RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);

            //check if request_type is equal to "SETUP" and in this case write the
            //Transport: line advertising to the server the port used to receive
            //the RTP packets RTP_RCV_PORT
            if (request_type == "SETUP") {
                RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + CRLF);
            }
            else if (request_type == "DESCRIBE") {
                RTSPBufferedWriter.write("Accept: application/sdp" + CRLF);
            }
            else {
                //otherwise, write the Session line from the RTSPid field
                RTSPBufferedWriter.write("Session: " + RTSPid + CRLF);
            }

            RTSPBufferedWriter.flush();
        } catch(Exception ex) {
            System.out.println("Exception caught: "+ex);
//            System.exit(0);
        }
    }
    private void sendRequest(String request_type,int frameNo) {
        try {
            //Use the RTSPBufferedWriter to write to the RTSP socket

            //write the request line:
            RTSPBufferedWriter.write(request_type + " " + VideoFileName + " RTSP/1.0" + CRLF);

            //write the CSeq line:
            RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);

            //check if request_type is equal to "SETUP" and in this case write the
            //Transport: line advertising to the server the port used to receive
            //the RTP packets RTP_RCV_PORT
            //the RTP packets RTP_RCV_PORT
            if (request_type == "PLAY") {
                RTSPBufferedWriter.write("Session: " + RTSPid +" Frame: "+frameNo+ CRLF);

            }
            RTSPBufferedWriter.flush();
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
//            System.exit(0);
        }
    }
}
