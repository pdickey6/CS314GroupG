// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.io.*;
import java.util.ArrayList;

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
  private ChatIF serverUI;
  ArrayList<String> users;
  ArrayList<ArrayList<String>> blocked;
  
  //Constructor ****************************************************
   
  public EchoServer(int port, ChatIF serverConsole) 
  {
    super(port);
    serverUI = serverConsole;
    users = new ArrayList<String>();
    //setupBlockList();    
    try {
		this.listen();
	} catch (IOException e) {
		serverUI.display("Unable to start listening!");
	} 
  }
    
  //Instance methods ************************************************
  
  private void setupBlockList() {
	  //blocked = new ArrayList<ArrayList<String>>();	
}

/**
   * This method handles any messages received from the client.
   *
   * @param msg The message received from the client.
   * @param client The connection from which the message originated.
   */
  public void handleMessageFromClient(Object msg, ConnectionToClient client)
  {
	  String message = msg.toString();
	  serverUI.display("Message received: " + message + " from " + client.getInfo("loginId"));
	  if(!message.startsWith("#")){ //message	
		  sendToAllClients(client.getInfo("loginId") + "> " + msg);
	  } else { //command
		  	int cmdEnd = message.indexOf(' ');
			if (cmdEnd < 1) 
				cmdEnd = message.length();
			String cmd = message.substring(1, cmdEnd);			
						
			if(cmd.equals("login")){
				String id = message.substring(cmdEnd+1, message.length());
				client.setInfo("loginId", id);
				users.add(id);
				SetupUserBlockList(id);
				sendToAllClients(id + " has logged on.");
				serverUI.display(id + " has logged on.");				
			} else if (cmd.equals("block")){
				String blockee = message.substring(cmdEnd+1, message.length());
				NewBlock(client,blockee);
			} else if (cmd.equals("unblock")){
				
			} else if (cmd.equals("whoiblock")){
				
			} else if (cmd.equals("whoblocksme")){
				
			}
	  }	
  }
    
  

private void SetupUserBlockList(String id) {
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
	  String msg = "A new client is attempting to connect to the server.";
	  serverUI.display(msg);
  }
  
    /**
   * This method overrides the one in the superclass.  Called
   * when a client disconnects.
   */
  protected void clientDisconnected(ConnectionToClient client){
	  String msg = client.getInfo("loginId") + " has disconnected!";
	  removeUser((String) client.getInfo("loginId"));
	  serverUI.display(msg);
	  sendToAllClients(msg);
  }
  
  //Class methods ***************************************************
  
  	private void removeUser(String id) {
		for(int i= 0; i < users.size(); i++){
			if(users.get(i).equals(id)){
				users.remove(i);
			}
		}
	}
  	
  	private void NewBlock(ConnectionToClient client, String blockee) {
  		String blocker = client.getInfo("loginId").toString();
  		if(!users.contains(blockee)){
  			try {
				client.sendToClient("User " + blockee + " does not exist.");
			} catch (IOException e) {
				serverUI.display("ERROR - Failed to send message to client " + blocker);
			}
  		} else if(blockee.equals(blocker)){
  			try {
				client.sendToClient("You cannot block the sending of messages to yourself.");
			} catch (IOException e) {
				serverUI.display("ERROR - Failed to send message to client " + blocker);
			}
  		} else if (blocker.length() > 0){
  			try {			
  				client.sendToClient("Messages from " + blockee + " will be blocked.");
  			} catch (IOException e) {
				serverUI.display("ERROR - Failed to send message to client " + blocker);
				}
  			}
  		}

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
							sendToAllClients("SERVER SHUTTING DOWN! DISCONNECTING!");
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
						try{
							int port = Integer.parseInt(message.substring(cmdEnd +1, message.length()));
							setPort(port);
							serverUI.display("Port set to: " + port);
						}catch (NumberFormatException e){
							serverUI.display("Port could not be set");
						}	
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
			port = Integer.parseInt(args[0]); 
		} catch(Throwable t) {
			port = DEFAULT_PORT; //Set port to 5555
		}
		
		ServerConsole server = new ServerConsole(port);
		server.accept();  //Wait for console data
	}
  }
//End of EchoServer class
