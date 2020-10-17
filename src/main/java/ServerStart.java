import com.github.sarxos.webcam.Webcam;

import javax.sound.sampled.*;
import java.awt.*;

public class ServerStart {
    public static void main(String[] args) {
        Webcam.setAutoOpenMode(true);
        Webcam webcam=Webcam.getDefault();
        webcam.setViewSize(new Dimension(640,480));
        webcam.open();
        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0F, 16, 2, 4, 44100, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class,
                format);
        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Error, line not supported");
        }
        TargetDataLine line=null;
        try {

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }

        line.start();

        Thread serverFactory=new Thread(new ServerFactory(webcam,line));
//        serverFactory.setDaemon(true);
        serverFactory.start();
        while(true);

    }
}
