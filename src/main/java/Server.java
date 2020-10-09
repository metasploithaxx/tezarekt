import com.github.sarxos.webcam.Webcam;
import javafx.animation.KeyFrame;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.awt.image.*;
import java.util.Timer;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import javax.sound.sampled.*;

public class Server implements Runnable{

    //RTP variables:
    //----------------
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
    DatagramPacket senddp; //UDP packet containing the video frames

    InetAddress ClientIPAddr;   //ClientVideo IP address
    int RTP_dest_port = 0;      //destination port for RTP packets  (given by the RTSP ClientVideo)
    int RTSP_dest_port = 0;


    //Video variables:
    //----------------
    int imagenb ; //image nb of the image currently transmitted
//    VideoStream video; //VideoStream object used to access video frames
    static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    static int MPA_TYPE = 14; //RTP payload type for MPA audio
    int FRAME_PERIOD ; //Frame period of the video to stream, in ms
    int VIDEO_LENGTH ; //length of the video in frames
    int SAMPLE_RATE; //sample rate of audio
    int AUDIO_CHANNELS;

    Timer timer;    //timer used to send the images at the video frame rate
//    byte[] buf;
    FrameType fbuf;                // buffer used to store the images to send to the client
    int sendDelay;  //the delay to send images over the wire. Ideally should be
                    //equal to the frame rate of the video file, but may be
                    //adjusted when congestion is detected.

    protected TargetDataLine line;
    protected final static Logger logger = LoggerFactory.getLogger(Server.class);
    protected final Webcam webcam;
    protected final Dimension dimension;

    protected final H264StreamEncoder h264StreamEncoder;
    protected final H264StreamEncoder secondEncoder;
//    protected volatile boolean isStreaming;
    protected ScheduledExecutorService timeWorker;
    protected ExecutorService encodeWorker,encodeAudioWorker;
    protected ScheduledFuture<?> imageGrabTaskFuture,audioGrabFuture;

    //RTSP variables
    //----------------
    //rtsp states
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    //rtsp message types
    final static int SETUP = 3;
    final static int PLAY = 4;
    final static int PAUSE = 5;
    final static int TEARDOWN = 6;
    final static int DESCRIBE = 7;

    int state; //RTSP Server state == INIT or READY or PLAY
    Socket RTSPsocket; //socket used to send/receive RTSP messages
    //input and output stream filters
    BufferedReader RTSPBufferedReader;
    BufferedWriter RTSPBufferedWriter;
    String VideoFileName; //video file requested from the client
    String RTSPid = UUID.randomUUID().toString(); //ID of the RTSP session
    int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session


    //RTCP variables
    //----------------
    int RTCP_RCV_PORT = 19001; //port where the client will receive the RTP packets
    static int RTCP_PERIOD = 400;     //How often to check for control events
    DatagramSocket RTCPsocket;
    RtcpReceiver rtcpReceiver;
    int congestionLevel;

    //Performance optimization and Congestion control
    ImageTranslator imgTranslator;
//    CongestionController cc;

    final static String CRLF = "\r\n";


    Server(Socket socket, Webcam webcam, Dimension dimension, H264StreamEncoder h264StreamEncoder, H264StreamEncoder secondEncoder, TargetDataLine line) {

        FRAME_PERIOD=1000/60;
        //init RTP sending Timer
        sendDelay = FRAME_PERIOD;

        imagenb=0;
        fbuf=new FrameType();


        //init the RTCP packet receiver
        rtcpReceiver = new RtcpReceiver(RTCP_PERIOD);



        //Video encoding and quality
        imgTranslator = new ImageTranslator(0.8f);


        RTSPsocket=socket;

        this.webcam =webcam;
        this.dimension =dimension;

        this.h264StreamEncoder =h264StreamEncoder;
        this.secondEncoder =secondEncoder;
        this.encodeWorker = Executors.newSingleThreadExecutor();
        this.encodeAudioWorker= Executors.newSingleThreadExecutor();
        this.timeWorker = new ScheduledThreadPoolExecutor(5);

        this.line = line;
    }

    @Override
    public void run() {

        Thread thread=new Thread(new Listener());
        thread.setDaemon(true);
        thread.start();
    }

    //------------------------
    //Handler for timer
    //------------------------


    class Listener implements Runnable{

        @Override
        public void run() {
//            Server server = new Server();
//            System.out.println("Listening on port 1051...");
//            //get RTSP socket port from the command line
//            int RTSPport = 1051;
//            RTSP_dest_port = RTSPport;
            try {
            //Initiate TCP connection with the client for the RTSP session
//            ServerSocket listenSocket = new ServerSocket(RTSPport);
//            RTSPsocket = listenSocket.accept();
//            listenSocket.close();

            //Get ClientVideo IP address
            ClientIPAddr = RTSPsocket.getInetAddress();

            //Initiate RTSPstate
            state = INIT;

            //Set input and output stream filters:
            RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()));
            RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()));
            //Wait for the SETUP message from the client
            int request_type;
            boolean done = false;


                while (!done) {
                    request_type = parseRequest(); //blocking

                    if (request_type == SETUP) {
                        done = true;
                        DatagramSocket test=null;
                        while(true){
                            try{
                                test=new DatagramSocket(RTCP_RCV_PORT);
                                test.setReuseAddress(true);
                                break;
                            }catch(SocketException e){}
                            finally {
                                if(test!=null){
                                    test.close();
                                }
                                else
                                    RTCP_RCV_PORT++;
                            }
                        }

                        System.out.println("RTCP port = "+RTCP_RCV_PORT);
                        //update RTSP state
                        state = READY;
                        System.out.println("New RTSP state: READY");

                        //Send response
                        sendResponse();

                        //init RTP and RTCP sockets
                        RTPsocket = new DatagramSocket();
                        RTCPsocket = new DatagramSocket(RTCP_RCV_PORT);
                    }
                }

                //loop to handle RTSP requests
                while (true) {
//                    System.out.println(timer.getStatus());
                    //parse the request
                    request_type = parseRequest(); //blocking

                    if ((request_type == PLAY) && (state == READY)) {
                        //send back response
                        sendResponse();
                        //start timer
                        imageGrabTaskFuture=timeWorker.scheduleWithFixedDelay(new ImageGrabTask(),
                                0,
                                FRAME_PERIOD,
                                TimeUnit.MILLISECONDS);
//                        audioGrabFuture=timeWorker.scheduleWithFixedDelay(new AudioGrabTask(),
//                                0,
//                                FRAME_PERIOD,
//                                TimeUnit.MILLISECONDS);

                        rtcpReceiver.startRcv();
                        //update state
                        state = PLAYING;
                        System.out.println("New RTSP state: PLAYING");
                    } else if ((request_type == PAUSE) && (state == PLAYING)) {
                        //send back response
                        sendResponse();
                        //stop timer

                        imageGrabTaskFuture.cancel(false);
                        audioGrabFuture.cancel(false);
                        timeWorker.shutdown();
                        encodeWorker.shutdown();
                        encodeAudioWorker.shutdown();
                        rtcpReceiver.stopRcv();
                        //update state
                        state = READY;
                        System.out.println("New RTSP state: READY");
                    } else if (request_type == TEARDOWN) {
                        //send back response
                        sendResponse();
                        //stop timer

                        imageGrabTaskFuture.cancel(false);
                        audioGrabFuture.cancel(false);
                        timeWorker.shutdown();
                        encodeWorker.shutdown();
                        encodeAudioWorker.shutdown();
                        rtcpReceiver.stopRcv();
                        //close sockets
                        RTSPsocket.close();
                        RTPsocket.close();
                        break;

//                        System.exit(0);
                    } else if (request_type == DESCRIBE) {
                        System.out.println("Received DESCRIBE request");
                        sendDescribe();
                    }
                }
            }catch (Exception ex){ex.printStackTrace();}
        }
    }

    private class ImageGrabTask implements Runnable{

        @Override
        public void run() {
            logger.info("image grabed ,count :{}",imagenb++);
            BufferedImage bufferedImage = webcam.getImage();

            encodeWorker.execute(new EncodeTask(bufferedImage));
        }

    }

    public class AudioGrabTask implements Runnable {
        @Override
        public void run() {
            int numBytesRead;
            byte[] data = new byte [4096];

            numBytesRead =	line.read(data, 0, 4096);

            encodeAudioWorker.execute(new EncodeAudioTask(data,numBytesRead));

        }
    }


    private class EncodeTask implements Runnable{
        private final BufferedImage image;
//			private final byte[] audioData;
//			private final int audioBytesRead;

        public EncodeTask(BufferedImage image) {
            super();
            this.image = image;
//				this.audioData = audioData;
//				this.audioBytesRead = audioBytesRead;
        }

        @Override
        public void run() {
            try {
                ByteBuffer msg = h264StreamEncoder.encode(image);

                if (msg != null) {
                    //Builds an RTPpacket object containing the frame
                    int size=msg.remaining();
                    byte[] buf=new byte[size];
                    msg.get(buf);
                    RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, buf, size);

                    //get to total length of the full rtp packet to send
                    int packet_length = rtp_packet.getlength();

                    //retrieve the packet bitstream and store it in an array of bytes
                    byte[] packet_bits = new byte[packet_length];
                    rtp_packet.getpacket(packet_bits);

                    //send the packet as a DatagramPacket over the UDP socket
                    senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
                    RTPsocket.send(senddp);

//                        System.out.println("Send frame #" + imagenb + ", Frame size: " + image_length + " (" + buf.length + ")");
                    //print the header bitstream
                    rtp_packet.printheader();


                    System.out.println("Send frame #" + imagenb);
                }


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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

                ByteBuffer msg=null;
                if ( audioBytesRead > 0 ) {
                    msg = secondEncoder.encode(audioData,audioBytesRead);
                }
                if ( msg != null ) {
                    int size=msg.remaining();
                    byte[] buf=new byte[size];
                    msg.get(buf);
                    RTPpacket rtp_packet = new RTPpacket(MPA_TYPE, imagenb, imagenb * FRAME_PERIOD, buf, size);

                    //get to total length of the full rtp packet to send
                    int packet_length = rtp_packet.getlength();

                    //retrieve the packet bitstream and store it in an array of bytes
                    byte[] packet_bits = new byte[packet_length];
                    rtp_packet.getpacket(packet_bits);

                    //send the packet as a DatagramPacket over the UDP socket
                    senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
                    RTPsocket.send(senddp);
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    //------------------------
    //Controls RTP sending rate based on traffic
    //------------------------
//    class CongestionController extends TimerTask {
//        private Timer ccTimer;
//        int interval;   //interval to check traffic stats
//        int prevLevel;  //previously sampled congestion level
//
//        public CongestionController(int interval) {
//            this.interval = interval;
//            ccTimer =new Timer(true);
//            ccTimer.schedule(this,0,interval);
//        }
//        @Override
//        public void run() {
//
//            //adjust the send rate
//            if (prevLevel != congestionLevel) {
//                sendDelay = FRAME_PERIOD + congestionLevel * (int)(FRAME_PERIOD * 0.1);
//                timer.setRate(FRAME_PERIOD/(double)sendDelay);
//                timer.setDelay(Duration.millis(sendDelay));
//                prevLevel = congestionLevel;
//                System.out.println("Send delay changed to: " + sendDelay);
//            }
//        }
//    }

    //------------------------
    //Listener for RTCP packets sent from client
    //------------------------
    class RtcpReceiver extends TimerTask {
        private Timer rtcpTimer;
        private byte[] rtcpBuf;
        int interval;

        public RtcpReceiver(int interval) {
            //set timer with interval for receiving packets
            this.interval = interval;




            //allocate buffer for receiving RTCP packets
            rtcpBuf = new byte[512];
        }
        @Override
        public void run() {
            //Construct a DatagramPacket to receive data from the UDP socket
            DatagramPacket dp = new DatagramPacket(rtcpBuf, rtcpBuf.length);
            float fractionLost;

            try {
                RTCPsocket.receive(dp);   // Blocking
                RTCPpacket rtcpPkt = new RTCPpacket(dp.getData(), dp.getLength());
                System.out.println("[RTCP] " + rtcpPkt);

                //set congestion level between 0 to 4
                fractionLost = rtcpPkt.fractionLost;
                if (fractionLost >= 0 && fractionLost <= 0.01) {
                    congestionLevel = 0;    //less than 0.01 assume negligible
                }
                else if (fractionLost > 0.01 && fractionLost <= 0.25) {
                    congestionLevel = 1;
                }
                else if (fractionLost > 0.25 && fractionLost <= 0.5) {
                    congestionLevel = 2;
                }
                else if (fractionLost > 0.5 && fractionLost <= 0.75) {
                    congestionLevel = 3;
                }
                else {
                    congestionLevel = 4;
                }
            }
            catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
            }
        }

        public void startRcv() {
            rtcpTimer = new Timer(true);
            rtcpTimer.schedule(new RtcpReceiver(interval),0,interval);
        }

        public void stopRcv() {
            rtcpTimer.cancel();
        }
    }

    //------------------------------------
    //Translate an image to different encoding or quality
    //------------------------------------
    class ImageTranslator {

        private float compressionQuality;
        private ByteArrayOutputStream baos;
        private BufferedImage image;
        private Iterator<ImageWriter>writers;
        private ImageWriter writer;
        private ImageWriteParam param;
        private ImageOutputStream ios;

        public ImageTranslator(float cq) {
            compressionQuality = cq;

            try {
                baos =  new ByteArrayOutputStream();
                ios = ImageIO.createImageOutputStream(baos);

                writers = ImageIO.getImageWritersByFormatName("jpeg");
                writer = (ImageWriter)writers.next();
                writer.setOutput(ios);

                param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(compressionQuality);

            } catch (Exception ex) {
                ex.printStackTrace();
//                System.exit(0);
            }
        }

        public byte[] compress(byte[] imageBytes) {
            try {
                baos.reset();
                image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                writer.write(null, new IIOImage(image, null, null), param);
            } catch (Exception ex) {
                ex.printStackTrace();
//                System.exit(0);
            }
            return baos.toByteArray();
        }

        public void setCompressionQuality(float cq) {
            compressionQuality = cq;
            param.setCompressionQuality(compressionQuality);
        }
    }

    //------------------------------------
    //Parse RTSP Request
    //------------------------------------
    private int parseRequest() {
        int request_type = -1;
        try {
            //parse request line and extract the request_type:
            String RequestLine = RTSPBufferedReader.readLine();
            System.out.println("RTSP Server - Received from ClientVideo:");
            System.out.println(RequestLine);

            StringTokenizer tokens = new StringTokenizer(RequestLine);
            String request_type_string = tokens.nextToken();

            //convert to request_type structure:
            if ((new String(request_type_string)).compareTo("SETUP") == 0)
                request_type = SETUP;
            else if ((new String(request_type_string)).compareTo("PLAY") == 0)
                request_type = PLAY;
            else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
                request_type = PAUSE;
            else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
                request_type = TEARDOWN;
            else if ((new String(request_type_string)).compareTo("DESCRIBE") == 0)
                request_type = DESCRIBE;

            if (request_type == SETUP) {
                //extract VideoFileName from RequestLine
                VideoFileName = tokens.nextToken();
            }

            //parse the SeqNumLine and extract CSeq field
            String SeqNumLine = RTSPBufferedReader.readLine();
            System.out.println(SeqNumLine);
            tokens = new StringTokenizer(SeqNumLine);
            tokens.nextToken();
            RTSPSeqNb = Integer.parseInt(tokens.nextToken());

            //get LastLine
            String LastLine = RTSPBufferedReader.readLine();
            System.out.println(LastLine);

            tokens = new StringTokenizer(LastLine);
            if (request_type == SETUP) {
                //extract RTP_dest_port from LastLine
                for (int i=0; i<3; i++)
                    tokens.nextToken(); //skip unused stuff
                RTP_dest_port = Integer.parseInt(tokens.nextToken());
            }
            else if (request_type == DESCRIBE) {
                tokens.nextToken();
                String describeDataType = tokens.nextToken();
            }
            else if(request_type==PLAY){
                tokens.nextToken(); //skip Session:
                RTSPid = tokens.nextToken();
//                tokens.nextToken();
//                int newFrame=Integer.parseInt(tokens.nextToken());
//                video.setFramePos(newFrame);

            }
            else {
                //otherwise LastLine will be the SessionId line
                tokens.nextToken(); //skip Session:
                RTSPid = tokens.nextToken();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
//            System.exit(0);
        }

        return(request_type);
    }

    // Creates a DESCRIBE response string in SDP format for current media
    private String describe() {
        StringWriter writer1 = new StringWriter();
        StringWriter writer2 = new StringWriter();

        // Write the body first so we can get the size later
        writer2.write("v=0" + CRLF);
        writer2.write("m=video " + RTSP_dest_port + " RTP/AVP " + MJPEG_TYPE + CRLF);
        writer2.write("a=control:streamid=" + RTSPid + CRLF);
        writer2.write("a=mimetype:string;\"video/MJPEG\"" + CRLF);
        String body = writer2.toString();

        writer1.write("Content-Base: " + VideoFileName + CRLF);
        writer1.write("Content-Type: " + "application/sdp" + CRLF);
        writer1.write("Content-Length: " + body.length() + CRLF);
        writer1.write(body);

        return writer1.toString();
    }

    //------------------------------------
    //Send RTSP Response
    //------------------------------------
    private void sendResponse() {
        try {
            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
            RTSPBufferedWriter.write("Session: "+RTSPid+CRLF);
            RTSPBufferedWriter.write("Length: "+VIDEO_LENGTH+CRLF);
            RTSPBufferedWriter.write("Period: "+FRAME_PERIOD+CRLF);
            RTSPBufferedWriter.write("S-Rate: "+SAMPLE_RATE+CRLF);
            RTSPBufferedWriter.write("Channels: "+AUDIO_CHANNELS+CRLF);
            RTSPBufferedWriter.write("Port: "+RTCP_RCV_PORT+CRLF);
            RTSPBufferedWriter.flush();
            System.out.println("RTSP Server - Sent response to ClientVideo.");
        } catch(Exception ex) {
            ex.printStackTrace();
//            System.exit(0);
        }
    }

    private void sendDescribe() {
        String des = describe();
        try {
            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
            RTSPBufferedWriter.write(des);
            RTSPBufferedWriter.flush();
            System.out.println("RTSP Server - Sent response to ClientVideo.");
        } catch(Exception ex) {
            ex.printStackTrace();
//            System.exit(0);
        }
    }
}

class FrameType{
    public byte[] videoBuffer,soundBuffer;
    private int videoSize,audioSize;
    private boolean gotVideo,gotAudio;
    public FrameType(){
        videoBuffer=new byte[63800];
        soundBuffer=new byte[63800];
        gotVideo=gotAudio=false;
    }
    public void setVideoBuffer(byte buf[]){
        System.arraycopy(buf,0,videoBuffer,0,buf.length>63800?63800:buf.length);
        videoSize=buf.length>63800?63800:buf.length;
        gotVideo=true;
    }

    public void setSoundBuffer(byte[] buf) {
        System.arraycopy(buf,0,soundBuffer,0,buf.length>63800?63800:buf.length);
        audioSize=buf.length>63800?63800:buf.length;
        gotAudio=true;
    }
    public void unsetVideoBuffer(){
        videoSize=0;
        gotVideo=false;
    }
    public void unsetAudioBuffer(){
        audioSize=0;
        gotAudio=false;
    }

    public boolean isGotVideo() {
        return gotVideo;
    }

    public boolean isGotAudio() {
        return gotAudio;
    }

    public int getVideoSize() {
        return videoSize;
    }

    public int getAudioSize() {
        return audioSize;
    }
}
