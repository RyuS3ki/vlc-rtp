import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.*;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class Server {
    
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    String media = "./movie.mp4";
    
    public static void main(final String[] args) {
        new NativeDiscovery().discover();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
					new Server(args);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });
    }
    
    private Server(String[] args) throws Exception {
        JFrame frame = new JFrame("vlcj");
        
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory(args);
        HeadlessMediaPlayer mediaPlayer = mediaPlayerFactory.newHeadlessMediaPlayer();
        
        frame.setContentPane(mediaPlayerComponent);
        
        String opt = formatRtpStream("239.0.0.1", 5004);
        mediaPlayer.playMedia(media, opt, ":sout-all", ":sout-keep");
        
        // Don't exit
        Thread.currentThread().join();
    }
    
    /* Piece of code taken from https://github.com/caprica/vlcj */
    private static String formatRtpStream(String serverAddress, int serverPort) {
        StringBuilder sb = new StringBuilder(60);
        sb.append(":sout=#rtp{dst=");
        sb.append(serverAddress);
        sb.append(",port=");
        sb.append(serverPort);
        sb.append(",mux=ts}");
        return sb.toString();
    }
    
}