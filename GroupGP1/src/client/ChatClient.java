// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

package client;

import com.lloseng.ocsf.client.*;
import common.*;
import java.io.*;
import java.util.ArrayList;

/**
 * This class overrides some of the methods defined in the abstract
 * superclass in order to give more functionality to the client.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;
 * @author Fran&ccedil;ois B&eacute;langer
 * @version July 2000
 */
public class ChatClient extends AbstractClient
{
  //Instance variables **********************************************
	
  /**
   * The interface type variable.  It allows the implementation of 
   * the display method in the client.
   */
  private ChatIF clientUI;
  private String loginId;
  private Boolean connected;
  public ArrayList<String> Blocked;
  public ArrayList<String> BlockedMe;
  
  //Constructors ****************************************************
  
  /**
   * Constructs an instance of the chat client.
   *
   * @param host The server to connect to.
   * @param port The port number to connect on.
   * @param clientUI The interface type variable.
   */
  
  public ChatClient(ChatIF UI){
	  super("localhost",5555);
	  clientUI = UI;
	  connected = false;
  }
  
  public ChatClient(String id, String host, int port, ChatIF UI) 
  throws IOException 
  {
    super(host, port); //Call the superclass constructor
    clientUI = UI;
    loginId = id;
    Blocked = new ArrayList<String>();
    BlockedMe = new ArrayList<String>();    
    openConnection();
    try {
		sendToServer("#login " + loginId);
	} catch (IOException e) {
		clientUI.display("ERROR - No login ID specified. Connection aborted.");
	}
  }
  
  
  //Instance methods ************************************************
    
  /**
   * This method handles all data that comes in from the server.
   *
   * @param msg The message from the server.
   */
  public void handleMessageFromServer(Object msg) 
  {
	  int endIdIndex = msg.toString().indexOf('>');
	  if(endIdIndex > 0){
		  String id = msg.toString().substring(0, endIdIndex);
		  if(!Blocked.contains(id))
			  //sender is not blocked, display msg
			  clientUI.display(msg.toString());
	  } else if(msg.toString().contains("will be blocked.")){
		  int endIndex = msg.toString().indexOf('w');
		  String id = msg.toString().substring(14, endIndex-1);
		  Blocked.add(id);
		  clientUI.display(msg.toString());
	  }
	  else {
		  clientUI.display(msg.toString());
	  }
  }

  /**
   * This method handles all data coming from the UI            
   *
   * @param message The message from the UI.    
   */
  public void handleMessageFromClientUI(String message)
  {
	if(!message.startsWith("#"))//message
	{
		try {
		    sendToServer(message);
		} catch(IOException e) {
		    clientUI.display("Could not send message to server. Terminating client.");
		    quit();
		}
	} else { //command
		int cmdEnd = message.indexOf(' ');
		if (cmdEnd < 1) 
			cmdEnd = message.length();
		String cmd = message.substring(1, cmdEnd);

		//Switch based on user command
		switch (cmd) {
			case "quit" :
				quit();
				break;
			case "logoff" :
				if(!connected)
					clientUI.display("You are already logged off.");
				else {
					try {
						closeConnection();
						connected = false;
						clientUI.display("Connection closed.");
					} catch (IOException e) {
						clientUI.display("Unable to logoff.");
					}
				}
				break;
			case "sethost" :
				if(connected)
					clientUI.display("Cannot set host while connected to server.");
				else {
					String host = message.substring(cmdEnd +1, message.length());
					if(host.length() > 0 ) {
						setHost(host);
						clientUI.display("Host set to: " + host);
					} else {
						clientUI.display("Host could not be set");
					}
				}
				break;
			case "setport" :
				if(connected)
					clientUI.display("Cannot set port while connected to server.");
				else {
					try{
						int port = Integer.parseInt(message.substring(cmdEnd +1, message.length()));
						setPort(port);
						clientUI.display("Port set to: " + port);
					}catch (NumberFormatException e){
						clientUI.display("Port could not be set");
					}					
				}
				break;
			case "gethost" :
				clientUI.display("Current Host: " + getHost());				
				break;
			case "getport" :
				clientUI.display("Current Port: " + getPort());				
				break;
			case "block" :
				try{
					String blockee = message.substring(cmdEnd+1, message.length());
					if(Blocked != null && Blocked.size() > 0 && Blocked.contains(blockee)){
						clientUI.display("Messages from " + blockee + " were already blocked.");
					} else{
						sendToServer(message);
					}
				} catch (IOException e) {
					clientUI.display("Messages could not be blocked.");
				}
				break;
			case "unblock" :
				try {
					sendToServer(message);
				} catch (IOException e) {
					clientUI.display("Messages could not be unblocked.");
				}
				break;
			case "whoiblock" :
				if(Blocked != null){
					String blocks = "Blocked Users: ";
					for(int i = 0; i < Blocked.size(); i ++){
						blocks += Blocked.get(i) + " ";
					}
					clientUI.display(blocks);
				}
				else{
					clientUI.display("No users currently blocked.");
				}
				break;
			case "whoblocksme" :
				try {
					sendToServer(message);
				} catch (IOException e) {
					clientUI.display("Block list could not be retrived.");
				}
				break;
			
			default: 
				clientUI.display("Command not recognized.");
				}
		}
	}


  /**
   * Called when the connection to the server is closed.
   */
  protected void connectionException(){
	  connected = false;
	  clientUI.display("Abnormal termination of connection");
  }
  
  /**
   * Called when the connection to the server is closed.
   */
  protected void connectionClosed(){
	try {
		closeConnection();
	} catch (IOException e) {

	}
	  connected = false;
  }
  
  protected void connectionEstablished(){	  
	  connected = true;	  
  }
  
  /**
   * This method terminates the client.
   */
  public void quit()
  {
	  if(connected){
		  try
		    {
		      closeConnection();
		      connected = false;
		    }
		    catch(IOException e) {}
	  }
	  System.exit(0);
  }
}
//End of ChatClient class
