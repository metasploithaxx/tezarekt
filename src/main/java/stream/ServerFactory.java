package stream;

import com.github.sarxos.webcam.Webcam;
import stream.H264StreamEncoder;
import stream.Server;

import javax.sound.sampled.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerFactory implements Runnable{
    final static int RTSPPort = 1051;
    protected final Webcam webcam;
    protected final Dimension dimension;
    protected TargetDataLine line;
    protected final H264StreamEncoder h264StreamEncoder;
    protected final H264StreamEncoder secondEncoder;
    public ServerFactory(Webcam w,TargetDataLine line){
        webcam=w;
        dimension=new Dimension(640,480); //Statically setting dimension for now
//        webcam.setViewSize(dimension);
        h264StreamEncoder=new H264StreamEncoder(dimension);
        secondEncoder=new H264StreamEncoder(dimension);
        this.line = line;

    }
    @Override
    public void run() {

        try {
            ServerSocket ss=new ServerSocket(RTSPPort);
            Socket sock;
            while(true){
                sock=ss.accept();
                new Thread(new Server(sock,webcam,dimension,h264StreamEncoder,secondEncoder,line)).start();
//                ss.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
