import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

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

public class Server extends JFrame{

    InetAddress ClientIPAddr; //Client IP address
    int RTP_PORT = 0; //destination port for RTP packets (given by the RTSP Client)

    //GUI
    private final JFrame frame;
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;

    //rtsp states
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    static int state; //RTSP Server state == INIT or READY or PLAYING

    //rtsp message types
    final static int SETUP = 3;
    final static int PLAY = 4;
    final static int PAUSE = 5;
    final static int TEARDOWN = 6;

    Socket RTSPsocket; //socket used to send/receive RTSP messages
    static BufferedReader RTSPBufferedReader; //input and output stream filters
    static BufferedWriter RTSPBufferedWriter;
    static String VideoFileName; //video file requested from the client
    static int RTSP_ID = 123456; //ID of the RTSP session
    int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session
    int first_time = 0; //0 if the video is not load yet

    final static String CRLF = "\r\n";


    public static void main(final String[] args) {
        new NativeDiscovery().discover();
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            try {
              new Server(args);
            } catch (Exception e) {
              System.out.println(e);
            } /* "Exception starting server" */
          }
        });
    }

    //--------------------------------
    //Constructor
    //--------------------------------
    public Server(String[] args) throws Exception{
      //Initiate TCP connection with the client for the RTSP session
      int RTSP_PORT = Integer.parseInt(args[0]);
      ServerSocket listenSocket = new ServerSocket(RTSP_PORT);
      InetAddress serverAddress = InetAddress.getLocalHost();
      RTSPsocket = listenSocket.accept();
      ClientIPAddr = RTSPsocket.getInetAddress(); //Get Client IP address
      listenSocket.close();

      //Initiate RTSPstate
      state = INIT;

      //GUI initialization
      this.setTitle("Server");
      /*
      mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
      MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory(args);
      HeadlessMediaPlayer mediaPlayer = mediaPlayerFactory.newHeadlessMediaPlayer();
      */
      MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory(args);
      HeadlessMediaPlayer mediaPlayer = mediaPlayerFactory.newHeadlessMediaPlayer();
      /*
      this.setContentPane(mediaPlayerComponent);
      this.setLocation(600, 100);
      this.setSize(500, 500);
      */
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      //Set input and output stream filters:
      RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()));
      RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()));



      //loop to handle RTSP requests
      while(true) {
        int request_type = parse_request(); //blocking
        System.out.println(request_type);

        if ((request_type == SETUP) && (state == INIT)) {
          //send response
          send_response();

          //show GUI:
          //this.setVisible(true);
          //TODO: El problema esta aqui:

          //update state
          state = READY;
          System.out.println("New RTSP state: READY");

        } else if ((request_type == PLAY) && (state == READY)) {
          //send response
          send_response();

          //start media player
          String opt = formatRtpStream("230.0.0.1", RTP_PORT);
          System.out.println("RTP Port:"+RTP_PORT);
          mediaPlayer.playMedia("movie.mp4", opt, ":no-sout-rtp-sap",
                  ":no-sout-standard-sap",
                  ":sout-all",
                  ":sout-keep");

          //update state
          state = PLAYING;
          System.out.println("New RTSP state: PLAYING");

        } else if ((request_type == TEARDOWN) && (state == READY)) {
            //send response
            send_response();

            //stop media player
            mediaPlayer.stop();

            //update state
            state = TEARDOWN;
            System.out.println("New RTSP state: TEARDOWN");

        } else if ((request_type == PAUSE) && (state == PLAYING)) {
            //send response
            send_response();

            //pause media player
            mediaPlayer.pause();

            //update state
            state = READY;
            System.out.println("New RTSP state: READY");

        } else if ((request_type == TEARDOWN) && (state == PLAYING)) {
            //send response
            send_response();

            //stop media player
            mediaPlayer.stop();

            //update state
            state = TEARDOWN;
            System.out.println("New RTSP state: TEARDOWN");

         } else {
        	 // ... fail gracefully
        	 System.out.println("Wrong operation received");
         }
        // Don't exit
        Thread.currentThread().join();
      }
    }

    //------------------------------------
    //Parse RTSP Request
    //------------------------------------
    private int parse_request() {
      int request_type = -1;
      try {
        //parse request line and extract the request_type:
        String RequestLine = RTSPBufferedReader.readLine();
        System.out.println("RTSP Server - Received from Client:");
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

        if (request_type == SETUP) {
          //extract RTP_PORT from LastLine
          tokens = new StringTokenizer(LastLine);
          for (int i=0; i<3; i++)
            tokens.nextToken(); //skip unused stuff
          RTP_PORT = Integer.parseInt(tokens.nextToken());
        } //else LastLine will be the SessionId line ... do not check for now.

      } catch (Exception ex) {
        System.out.println("Exception caught 2: " + ex);
        System.exit(0);
      }

      return(request_type);
    }

    //------------------------------------
    //Send RTSP Response
    //------------------------------------
    private void send_response() {
      try {
        RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
        RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
        RTSPBufferedWriter.write("Session: "+RTSP_ID+CRLF);
        RTSPBufferedWriter.flush();
        System.out.println("RTSP Server - Sent response to Client.");
      } catch (Exception ex) {
        System.out.println("Exception caught 3: " + ex);
        System.exit(0);
      }
    }

    /* Piece of code taken from https://github.com/caprica/vlcj */
    private static String formatRtpStream(String serverAddress, int RTP_PORT) {
        StringBuilder sb = new StringBuilder(60);
        sb.append(":sout=#rtp{dst=");
        sb.append(serverAddress);
        sb.append(",port=");
        sb.append(RTP_PORT);
        sb.append(",mux=ts}");
        return sb.toString();
    }

}
