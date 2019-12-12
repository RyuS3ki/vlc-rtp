import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.SwingUtilities;

import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;

public class Client {
    
	Scanner kb = new Scanner(System.in);
	
    private final JFrame frame;
    
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    
    private final RTSP stream;
    
    public static void main(final String[] args) {
        new NativeDiscovery().discover();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Client(args);
            }
        });
    }
    
    public Client(String[] args) {
        
    	// Get options
        System.out.println("Enter server name: ");
        String serverName = kb.nextLine();
        System.out.println("Enter RTP port: ");
        int serverRTPPort = kb.nextInt();
        String VideoFileName = "./movie.mp4";
        int serverRTSPPort = 10649;
        
        stream = new RTSP(serverName, serverRTPPort, serverRTSPPort, VideoFileName);
    	
        frame = new JFrame("Media Player");
        
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        
        //TO DO! choose the correct arguments for the methods below. Add more method calls as necessary
        frame.setLocation(100, 100);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mediaPlayerComponent.release();
                System.exit(0);
            }
        });
        
        contentPane.add(mediaPlayerComponent, BorderLayout.CENTER);
        
        
        JPanel controlsPane = new JPanel();
        JButton setupButton = new JButton("Setup");
        controlsPane.add(setupButton);
        JButton playButton = new JButton("Play");
        controlsPane.add(playButton);
        JButton pauseButton = new JButton("Pause");
        controlsPane.add(pauseButton);
        JButton stopButton = new JButton("Stop");
        controlsPane.add(stopButton);
        contentPane.add(controlsPane, BorderLayout.SOUTH);
        
        //Handler for PLAY button
        //-----------------------
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	stream.send_request("PLAY");
            }
        });
        
        //TO DO! implement a PAUSE button to pause video playback.
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stream.send_request("PAUSE");
            }
        });
        
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	stream.send_request("TEARDOWN");
            }
        });
        
        setupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	stream.send_request("SETUP");
            }
        });
        
        final String mrl = formatRtpStream(serverName, serverRTPPort);
        
        //Makes visible the window
        frame.setContentPane(contentPane);
        frame.setVisible(true);
        mediaPlayerComponent.getMediaPlayer().playMedia(VideoFileName, mrl);
        
    }
    private static String formatRtpStream(String serverName, int serverRTPPort) {
        StringBuilder sb = new StringBuilder(60);
        sb.append(":sout=#rtp{dst=");
        sb.append(serverName);
        sb.append(':');
        sb.append(serverRTPPort);
        sb.append("}");
        return sb.toString();
    }
}

