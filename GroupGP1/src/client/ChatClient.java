// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 
//Edited by Patrick Dickey and Sean Jergensen

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
	private String monitor;
	private Boolean connected;  
	private Boolean isForwarding;	

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
		isForwarding = false;
	}

	public ChatClient(String id, String host, int port, ChatIF UI) 
			throws IOException 
			{
		super(host, port); //Call the superclass constructor
		clientUI = UI;
		loginId = id;
		isForwarding = false;		

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
		String message = msg.toString();
		if(!message.startsWith("#")){
			if(!isForwarding)
				clientUI.display(message);
			else {
				//In meeting so forward msg to monitor
				try {
					sendToServer("#forward_message " + monitor + " " + message);
				} catch (IOException e) {
					clientUI.display("Unable to forward message to server.");
				}
			}
		} else { //command
			int cmdEnd = message.indexOf(' ');
			if (cmdEnd < 1) 
				cmdEnd = message.length();
			String cmd = message.substring(1, cmdEnd);

			//Switch based on user command
			switch (cmd.toLowerCase()) {
			case "meeting":
				monitor = message.substring(cmdEnd+1);
				isForwarding = true;
				clientUI.display("In meeting: " + monitor + " will now receive your messages. When you return type #endmeeting to cancel forwarding.");
				break;
			case "forward":
				monitor = message.substring(cmdEnd+1);
				isForwarding = true;
				clientUI.display("Forwarding: " + monitor + " will now receive your messages. When you return type #stopforward to cancel forwarding.");
				break;
			case "endForward":
				isForwarding = false;
				clientUI.display("No longer forwarding messages to " + monitor + ".");
				monitor = "";
				break;
			}

				
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
			switch (cmd.toLowerCase()) {
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
			case "login" :
				if(connected)
					clientUI.display("You are already logged in.");
				else {
					try {
						openConnection();
						sendToServer("#login " + loginId);
						connected = true;
					} catch (IOException e) {
						clientUI.display("Unable to login.");
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
					sendToServer(message);

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
				try{
					sendToServer(message);
				} catch (IOException e){
					clientUI.display("Could not get list of blocked users.");
				}
				break;
			case "whoblocksme" :
				try {
					sendToServer(message);
				} catch (IOException e) {
					clientUI.display("Block list could not be retrived.");
				}
				break;
			case "setchannel":
				try {sendToServer(message);
				} catch (IOException e) {
					clientUI.display("Channel could not be set.");
				}
				break;
			case "private":
				try {sendToServer(message);
				} catch (IOException e) {
					clientUI.display("Could not send private message.");
				}
				break;
			case "meeting" :
				try {sendToServer(message);
				} catch (IOException e) {
					clientUI.display("Could not initiate meeting.");
				}				
				break;
			case "endmeeting":
				isForwarding = false;
				break;
			case "status":
				try {sendToServer(message);
				} catch (IOException e) {
					clientUI.display("Status could not be set.");
				}
				break;
			case "available":
				try {sendToServer(message);
				} catch (IOException e) {
					clientUI.display("Could not change status.");
				}				
				break;
			case "notavailable":
				try {sendToServer(message);
				} catch (IOException e) {
					clientUI.display("Could not change status.");
				}	
				break;
			case "forward":
				try {sendToServer(message);
				} catch (IOException e) {
					clientUI.display("Could not forward messages.");
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