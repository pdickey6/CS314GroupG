// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

package client;

import com.lloseng.ocsf.client.*;
import common.*;
import java.io.*;

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
  ChatIF clientUI; 
  private Boolean connected;

  
  //Constructors ****************************************************
  
  /**
   * Constructs an instance of the chat client.
   *
   * @param host The server to connect to.
   * @param port The port number to connect on.
   * @param clientUI The interface type variable.
   */
  
  public ChatClient(String host, int port, ChatIF clientUI) 
    throws IOException 
  {
    super(host, port); //Call the superclass constructor
    this.clientUI = clientUI;
    openConnection();
  }

  
  //Instance methods ************************************************
    
  /**
   * This method handles all data that comes in from the server.
   *
   * @param msg The message from the server.
   */
  public void handleMessageFromServer(Object msg) 
  {
    clientUI.display(msg.toString());
  }

  /**
   * This method handles all data coming from the UI            
   *
   * @param message The message from the UI.    
   */
  public void handleMessageFromClientUI(String message)
  {
	if(message.startsWith("#"))//command not message
	{
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
					} catch (IOException e) {
						clientUI.display("Unable to logoff.");
					}
				}
				break;
			case "sethost" :
				if(connected)
					clientUI.display("Cannot set host while connected to server.");
				else {
					String temp = message.substring(cmdEnd +1, message.length());
					setHost( message.substring(cmdEnd +1, message.length()));
				}
				break;
			case "setport" :
				if(connected)
					clientUI.display("Cannot set port while connected to server.");
				else {
					String temp = message.substring(cmdEnd +1, message.length());
					setPort( Integer.parseInt(message.substring(cmdEnd +1, message.length())));
				}
				break;
			case "login" :
				if(connected)
					clientUI.display("You are already logged in.");
				else {
					try {
						openConnection();
						connected = true;
					} catch (IOException e) {
						clientUI.display("Unable to login.");
					}
				}
				break;
			case "gethost" :
				clientUI.display("Current Host: " + getHost());				
				break;
			case "getport" :
				clientUI.display("Current Port: " + getPort());				
				break;
				
			default: 
				clientUI.display("Command not recognized.");
		}		
	} else {
		try {
		    sendToServer(message);
		} catch(IOException e) {
		    clientUI.display("Could not send message to server.  Terminating client.");
		    quit();
		}
	}
  }
  

  /**
   * Called when the connection to the server is closed.
   */
  protected void connectionException(){
	  clientUI.display("Connection to server has been closed.");
  }
  
  /**
   * Called when the connection to the server is closed.
   */
  protected void connectionClosed(){
	  connected = false;
	  clientUI.display("Connection to server has been closed.");
  }
  
  protected void connectionEstablished(){
	  connected = true;
  }
  
  /**
   * This method terminates the client.
   */
  public void quit()
  {
    try
    {
      closeConnection();
    }
    catch(IOException e) {}
    System.exit(0);
  }
}
//End of ChatClient class