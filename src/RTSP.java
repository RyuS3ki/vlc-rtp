import java.io.*;
import java.net.*;
import java.util.*;

public class RTSP {

	private String ServerHost;
	private int RTP_PORT;
	private int RTSP_PORT;
	Socket RTSPsocket; //socket used to send/receive RTSP messages

	//rtsp states
	final static int INIT = 0;
	final static int READY = 1;
	final static int PLAYING = 2;
	static int state; //RTSP state == INIT or READY or PLAYING

	//input and output stream filters
	static BufferedReader RTSPBufferedReader;
	static BufferedWriter RTSPBufferedWriter;

	static String VideoFileName; //video file to request to the server
	int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session
	int RTSPid = 0; //ID of the RTSP session (given by the RTSP Server)

	final static String CRLF = "\r\n";

	//--------------------------
	//Constructor
	//--------------------------
	public RTSP(String ServerHost, int RTSP_PORT, int RTP_PORT, String VideoFileName) {
    this.ServerHost = ServerHost;
    this.RTSP_PORT = RTSP_PORT;
    this.RTP_PORT = RTP_PORT;
    this.VideoFileName = VideoFileName;

    //first state
    state = INIT;

    try {
      //Establish a TCP connection with the server to exchange RTSP messages
    	
      InetAddress ServerIPAddr = InetAddress.getByName(ServerHost);
      System.out.println("Server IP is: "+ServerIPAddr+", Port is: "+RTSP_PORT);
      RTSPsocket = new Socket(ServerIPAddr, RTSP_PORT);
      
      //System.out.println("");
      //Set input and output stream filters:
      RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()) );
      RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()) );
      
      System.out.println("RTSP socket connected");

    } catch (Exception e) {
      System.out.println(e + " " + ServerHost + RTSP_PORT);
    }
  }

	//------------------------------------
	//Parse Server Response
	//------------------------------------
	private int parse_response() {
		int reply_code = 0;

		try {
			//parse status line and extract the reply_code:
			String StatusLine = RTSPBufferedReader.readLine();
			//System.out.println("RTSP Client - Received from Server:");
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

				//if state == INIT gets the Session Id from the SessionLine
				tokens = new StringTokenizer(SessionLine);
				tokens.nextToken(); //skip over the Session:
				RTSPid = Integer.parseInt(tokens.nextToken());
			}

		} catch (Exception ex) {
			System.out.println("Exception caught RTSP: " + ex);
			System.exit(0);
		}

		return(reply_code);
	}

	//------------------------------------
	//Send RTSP Request
	//------------------------------------
	public void send_request(String request_type) {
		try {
			System.out.println("Sending request");
			int save_state = -1;
			String RTPmrl = "rtp://"+ServerHost+"/"+VideoFileName;
			RTSPSeqNb += 1;
			//Check request_type and state variables to see if the RTSP message can be sent
			if(((request_type).compareTo("SETUP") == 0) && (state == INIT)){
				save_state = READY;
				RTSPBufferedWriter.write("SETUP "+RTPmrl+" RTSP/1.0"+CRLF);
			} else if (((request_type).compareTo("PLAY") == 0) && (state == READY)){
				System.out.println("New state desired: PLAYING");
				save_state = PLAYING;
				RTSPBufferedWriter.write("PLAY "+RTPmrl+" RTSP/1.0"+CRLF);
			} else if (((request_type).compareTo("PAUSE") == 0) && (state == READY)){
				save_state = READY;
				RTSPBufferedWriter.write("PAUSE "+RTPmrl+" RTSP/1.0"+CRLF);
			} else if (((request_type).compareTo("TEARDOWN") == 0) && (state == (PLAYING | READY))){
				save_state = INIT;
				RTSPBufferedWriter.write("TEARDOWN "+VideoFileName+" RTSP/1.0"+CRLF);
			}
	
	        if (save_state != -1){
	        	//write the request line:
		        RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
		
		        //check if request_type is equal to "SETUP" and in this case write the Transport: line advertising to the server the port used to receive the RTP packets RTP_PORT
		        if((request_type).compareTo("SETUP") == 0){
			        RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= "+RTP_PORT+CRLF);
		        } else {//otherwise, write the Session line from the RTSPid field
			        RTSPBufferedWriter.write("Session: "+RTSPid+CRLF);
				}
		        RTSPBufferedWriter.flush();
		
		        //Wait for the response and, in case of success, update the state variable
		        if(parse_response() != 0) {
		    	    state = save_state;
		    	    System.out.println(state);
		        }
	        }
	        
		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}
	}

}//end of Class RTSP
