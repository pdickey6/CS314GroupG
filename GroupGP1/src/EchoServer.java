// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.io.*;

import com.lloseng.ocsf.server.*;
import common.ChatIF;

/**
 * This class overrides some of the methods in the abstract 
 * superclass in order to give more functionality to the server.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;re
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Paul Holden
 * @version July 2000
 */
public class EchoServer extends AbstractServer 
{
  //Class variables *************************************************
  
  /**
   * The default port to listen on.
   */
  final public static int DEFAULT_PORT = 5555;
  
  //Instance variables *************************************************
  /**
   * The interface type variable.  It allows the implementation of the display method in the client.
   */
  ChatIF serverUI;
  
  //Constructor ****************************************************
   
  public EchoServer(int port, ChatIF serverConsole) 
  {
    super(port);
    serverUI = serverConsole;
    try {
		this.listen();
	} catch (IOException e) {
		serverUI.display("Unable to start listening!");
	} 
  }
    
  //Instance methods ************************************************
  
  /**
   * This method handles any messages received from the client.
   *
   * @param msg The message received from the client.
   * @param client The connection from which the message originated.
   */
  public void handleMessageFromClient
    (Object msg, ConnectionToClient client)
  {
    serverUI.display("Message received: " + msg + " from " + client);
    this.sendToAllClients(msg);
  }
    
  /**
   * This method overrides the one in the superclass.  Called
   * when the server starts listening for connections.
   */
  protected void serverStarted()
  {
	  serverUI.display("Server listening for connections on port " + getPort());
  }
  
  /**
   * This method overrides the one in the superclass.  Called
   * when the server stops listening for connections.
   */
  protected void serverStopped()
  {
	  serverUI.display("Server has stopped listening for connections.");
	  sendToAllClients("WARNING - The server has stopped listening for connections");
  }
  
  /**
   * This method overrides the one in the superclass.  Called
   * when a client connects.
   */
  protected void clientConnected(ConnectionToClient client){
	  String msg ="A new client has connected!";
	  serverUI.display(msg);
	  sendToAllClients(msg);
  }
  
    /**
   * This method overrides the one in the superclass.  Called
   * when a client disconnects.
   */
  protected void clientDisconnected(ConnectionToClient client){
	  String msg ="A client has disconnected!";
	  serverUI.display(msg);
	  sendToAllClients(msg);
  }
  
  //Class methods ***************************************************
  
  public void handleMessageFromServerUI(String message) {
	  if(!message.startsWith("#")) {//Server Msg
		    serverUI.display(message);
		    sendToAllClients("SERVER MSG> " + message);
	  } else {
		  	int cmdEnd = message.indexOf(' ');
			if (cmdEnd < 1) 
				cmdEnd = message.length();
			String cmd = message.substring(1, cmdEnd);
	
			//Switch based on user command
			switch (cmd) {
				case "quit" :
					if(!isClosed()){
						try {
							close();
						} catch (IOException e) {
							serverUI.display("Unable to close.");
						}
					}
					System.exit(0);
					
					break;
				case "stop" :
					if(!isListening())
						serverUI.display("Server is already stopped.");
					else {
						stopListening();
					}
					break;
				case "close" :
					if(isClosed())
						serverUI.display("Server is already closed");
					else {
						try{
							sendToAllClients("SERVER SHUTTING DOWN! DISCONNECTING");
							sendToAllClients("Abnormal termination of connection");
							close();
						} catch (IOException e){
							serverUI.display("Unable to close.");
						}
					}
					break;
				case "setport" :
					if(!isClosed())
						serverUI.display("Can not set port untill the server is closed.");
					else {
						String temp = message.substring(cmdEnd +1, message.length());
						setPort( Integer.parseInt(message.substring(cmdEnd +1, message.length())));
					}
					break;
				case "start" :
					if(isListening())
						serverUI.display("Server is already listening.");
					else {
						try {
							listen();
						} catch (IOException e) {
							serverUI.display("Unable to start listening.");
						}
					}
					break;
				case "getport" :
					serverUI.display("Current Port: " + getPort());				
					break;					
				default: 
					serverUI.display("Command not recognized.");
			}		
	  }
	}
	public static void main(String[] args) 
	{
		//Get port
		int port = 0;
		try {
			port = Integer.parseInt(args[1]); 
		} catch(Throwable t) {
			port = DEFAULT_PORT; //Set port to 5555
		}
		
		ServerConsole server = new ServerConsole(port);
		server.accept();  //Wait for console data
	}
  }
//End of EchoServer class
