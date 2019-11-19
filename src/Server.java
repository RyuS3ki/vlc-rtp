/* ------------------
  usage: java Server [RTSP listening port]
---------------------- */

import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class Server extends JFrame {

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
          System.out.println("Exception starting server");
        }
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
    RTSPsocket = listenSocket.accept();
    ClientIPAddr = RTSPsocket.getInetAddress(); //Get Client IP address
    listenSocket.close();

    //Initiate RTSPstate
    state = INIT;

    //GUI initialization
    this.setTitle("Server");
    mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
    this.setContentPane(mediaPlayerComponent);
    this.setLocation(600, 100);
    this.setSize(500, 500);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    //Set input and output stream filters:
    RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()));
    RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()));

    //loop to handle RTSP requests
    while(true) {
      int request_type = parse_request(); //blocking

      if ((request_type == SETUP) && (state == INIT)) {
        //send response
        send_response();

        //show GUI:
        this.setVisible(true);

        //update state
        //state = ...
        //System.out.println("New RTSP state: ... ");

      } else if ((request_type == PLAY) && (state == READY)) {
        //send response
        send_response();

        //start media player
        //...

        //update state
        //state = ...
        //System.out.println("New RTSP state: ... ");

      } //else if ((request_type == ...

      //else ... fail gracefully
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
      //System.out.println("RTSP Server - Received from Client:");
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
      //System.out.println("RTSP Server - Sent response to Client.");
    } catch (Exception ex) {
      System.out.println("Exception caught 3: " + ex);
      System.exit(0);
    }
  }

} //end of Server
