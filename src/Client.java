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

public class Client{
    
	Scanner kb = new Scanner(System.in);
	
    private final JFrame frame;
    
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    
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
        
        
        frame = new JFrame("Media Player");
        
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
        
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent() {
        	@Override
            protected MediaPlayerFactory onGetMediaPlayerFactory() {
                System.out.println("Enter server IP: ");
                String serverIP = kb.nextLine();
                String serverPort = "10649";
                String[] opt = {serverIP, serverPort};
                MediaPlayerFactory factory = new MediaPlayerFactory(opt);
            }
        };
        contentPane.add(mediaPlayerComponent, BorderLayout.CENTER);
        
        JPanel controlsPane = new JPanel();
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
                //TO DO!! configure the playback of the video received via RTP, or resume a paused playback.
            	mediaPlayerComponent.getMediaPlayer().play();
            }
        });
        
        //TO DO! implement a PAUSE button to pause video playback.
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //TO DO!!
            	mediaPlayerComponent.getMediaPlayer().pause();
            }
        });
        
        
        //TO DO! implement a STOP button to stop video playback and exit the application.
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //TO DO!!
            	mediaPlayerComponent.getMediaPlayer().stop();
            }
        });
        
        // Get options
        
        
        
        //Makes visible the window
        frame.setContentPane(contentPane);
        frame.setVisible(true);
        mediaPlayerComponent.getMediaPlayer().playMedia(options);
        
    }
    
}

