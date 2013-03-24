// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 
//Edited by Patrick Dickey and Sean Jergensen

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

	private ChatIF serverUI;
	ArrayList<String> users;
	ArrayList<String> serverMuteUsers;
	ArrayList<String> blockedClients;

	//Constructor ****************************************************

	public EchoServer(int port, ChatIF serverConsole) 
	{
		super(port);
		serverUI = serverConsole;
		users = new ArrayList<String>();
		serverMuteUsers = new ArrayList<String>();
		blockedClients = new ArrayList<String>();
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
	public void handleMessageFromClient(Object msg, ConnectionToClient client)
	{
		String message = msg.toString();
		if(blockedClients.contains(client.getInfo("loginId")) && !message.startsWith("#")) {
			return;
		}
		else{
			serverUI.display("Message received: " + message + " from " + client.getInfo("loginId"));
		}

		if(!message.startsWith("#")){ 
			//message
			SelectiveSendToClients(client.getInfo("loginId") + "> " + msg, client);
		} else { 
			//command
			String cmd = getCommand(message);

			if(cmd.equals("login")){
				String id = message.substring(message.indexOf(' ')+1, message.length());
				boolean validLogin = loginRecived(client, id);						
			} else if (cmd.equals("block")){
				String blockee = message.substring(message.indexOf(' ')+1, message.length());
				NewBlock(client,blockee);
			} else if (cmd.equals("unblock")){
				UnblockCmd(client, message);
			} else if (cmd.equals("whoiblock")){
				WhoIBlockCmd(client);
			} else if (cmd.equals("whoblocksme")){
				WhoBlocksMeCmd(client);
			} else if (cmd.equals("setchannel")){
				SetChannelCmd(client, message);
			} else if (cmd.equals("private")){
				SendPvtMsg(client, message);
			}
		}	
	}

	/**
	 * parses out command message and sends private message
	 * @param sender
	 * @param message
	 */

	private void SendPvtMsg(ConnectionToClient sender, String message) {
		int startIndex = message.indexOf(' ');
		int endIndex =  message.indexOf(' ', startIndex +1);
		String recipient = message.substring(startIndex +1, endIndex);
		String msg = message.substring(endIndex +1);
		SendMessageToClient(sender, recipient, msg);		
	}

	private void SendMessageToClient(ConnectionToClient sender, String recipient, String msg) {
		ArrayList<Thread> blockedSender = getBlockedMe(sender);
		Thread[] clientList = getClientConnections();

		for (int i=0; i<clientList.length; i++)
		{
			ConnectionToClient client= (ConnectionToClient) clientList[i];
			if (!blockedSender.contains(client) && client.getInfo("loginId").equals(recipient)) {
				try {
					//Not blocked and specified recipient
					client.sendToClient(sender.getInfo("loginId") + "> (Private) " + msg);
					sender.sendToClient(sender.getInfo("loginId") + "> (Private) " + msg);
				} catch (IOException e) {
					serverUI.display("Message could not be sent to the client.");
				}
			}
		}
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
		client.setInfo("Blocked", new ArrayList<ArrayList<String>>());
		String msg = "A new client is attempting to connect to the server.";
		serverUI.display(msg);
	}

	/**
	 * This method overrides the one in the superclass.  Called
	 * when a client disconnects.
	 */
	protected void clientDisconnected(ConnectionToClient client){
		String msg = client.getInfo("loginId") + " has disconnected!";
		//removeUser((String) client.getInfo("loginId"));
		if(client.getInfo("loginId") != null){
			serverUI.display(msg);
			sendToAllClients(msg);		
		}
	}

	//Class methods ***************************************************

	/**
	 * Handles login requests from clients
	 */
	private boolean loginRecived(ConnectionToClient client, String id) {
		String clientOrigLogin = (String) client.getInfo("loginId");
		if(clientOrigLogin != null){
			try {
				client.sendToClient("ERROR- You have already logged in with user id: " + clientOrigLogin + ".");				
			} catch (IOException e1) {
				serverUI.display("ERROR- Unable to send login error message to client: " + clientOrigLogin);
			}
			return false;
		}
		try{
			Thread[] clients = this.getClientConnections();
			for(int i = 0; i < clients.length; i++){
				if(((ConnectionToClient)clients[i]).getInfo("loginId").equals(id)){
					//User is already logged in
					try {
						client.sendToClient("ERROR- A user with the id: " + id + " has is already online. Awaiting Command");
						serverUI.display("A client with duplicate loginId: " + id + " has tried to login and was refused.");
						client.close();
					} catch (IOException e) {
						serverUI.display("ERROR- Unable to send login error message to client: " + id);
					}
					return false;
				}
			}
		} catch (RuntimeException e){} //Catches when there are no clients to getClientConnections
		//first unique login for client
		client.setInfo("loginId", id);
		users.add(id);
		
		//Initially put all users into public chat
		client.setInfo("channel", "public");

		//TODO: Need to update this to sent messages to only people in same channel, online, not blocked ect...
		sendToAllClients(id + " has logged on.");
		serverUI.display(id + " has logged on.");

		return  true;
	}

	/**
	 * This method adds the blockee to the clients block list
	 */
	private void NewBlock(ConnectionToClient client, String blockee) {
		String blocker = client.getInfo("loginId").toString();
		ArrayList<String> blocked = (ArrayList<String>) client.getInfo("Blocked");	

		if(!users.contains(blockee) && !blockee.equals("server")){
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
		} else if(blocked.contains(blockee)){
			try {
				client.sendToClient("Messages from " + blockee + " were already blocked.");
			} catch (IOException e) {
				serverUI.display("ERROR - Failed to send message to client " + blocker);
			}
		} else if (blocker.length() > 0){
			try {				
				client.sendToClient("Messages from " + blockee + " will be blocked.");
				if(blockee.equals("server")){
					serverMuteUsers.add((String) client.getInfo("loginId"));
					ArrayList<String> clientBlocked = (ArrayList<String>)client.getInfo("Blocked");
					clientBlocked.add("server");
					client.setInfo("Blocked", clientBlocked);
				}else {

					blocked.add(blockee);
					client.setInfo("Blocked", blocked);
				}
			} catch (IOException e) {
				serverUI.display("ERROR - Failed to send message to client " + blocker);
			}
		}
	}

	/**
	 * Handles unblock commands from clients
	 */
	private void UnblockCmd(ConnectionToClient client, String message) {
		String cmd = getCommand(message);
		int cmdEnd = message.indexOf(' ');

		if (message.trim().equals("#"+cmd)) { //#unblock all command
			if (getBlocks(client).isEmpty()) {
				try {
					client.sendToClient("No blocking is in effect.");
				} catch (IOException e) {
					serverUI.display("Message could not be sent to the client.");
				}
			}else { //
				ArrayList<String> blocks = getBlocks(client);
				for (int i = 0; i < blocks.size(); i++) {
					try {
						client.sendToClient("Messages from " + blocks.get(i) + " will now be displayed.");
						if( blocks.get(i).equals("server"))
							serverMuteUsers.remove(client.getInfo("loginId"));
					} catch (IOException e) {
						serverUI.display("Message could not be sent to the client.");
					}
				}
				client.setInfo("Blocked", new ArrayList<String>());
			}
		}else { //#unblock user command
			serverMuteUsers.remove(client.getInfo("loginId"));
			String unBlockee = message.substring(cmdEnd+1, message.length());
			if(Unblock(client,unBlockee)){
				try {
					client.sendToClient("Messages from " + unBlockee + " will now be displayed.");
					if( unBlockee.equals("server"))
						serverMuteUsers.remove("server");
				} catch (IOException e) {
					serverUI.display("Message could not be sent to the client.");
				}
			}else {
				try {
					client.sendToClient("Messages from " + unBlockee + " were not blocked");
				} catch (IOException e) {
					serverUI.display("Message could not be sent to the client.");
				}
			}
		}		
	}

	/**
	 * displays to client list of users the client blocks
	 */
	private void WhoIBlockCmd(ConnectionToClient client) {
		ArrayList<String> iBlocked = getBlocks(client);
		if (iBlocked.isEmpty()) {
			try {
				client.sendToClient("No blocking is in effect.");
			} catch (IOException e) {
				serverUI.display("Message could not be sent to the client.");
			}
		}else {
			for (int i = 0; i < iBlocked.size(); i++) {
				try {
					client.sendToClient("Messages from " + iBlocked.get(i) + " are blocked.");
				} catch (IOException e) {
					serverUI.display("Message could not be sent to the client.");
				}
			}
		}

	}

	/**
	 * displays to client list of users that block the client
	 */
	private void WhoBlocksMeCmd(ConnectionToClient client) {
		ArrayList<Thread> blockedMe = getBlockedMe(client);
		for (int i = 0; i < blockedMe.size(); i++) {
			try {
				client.sendToClient("Messages to " + ((ConnectionToClient) (blockedMe.get(i))).getInfo("loginId") + " are being blocked.");
			} catch (IOException e) {
				serverUI.display("Message could not be sent to the client.");
			}
		}		
	}

	/**
	 * Parses out and returns the command from a string message
	 */
	private String getCommand(String message) {
		int cmdEnd = message.indexOf(' ');
		if (cmdEnd < 1) 
			cmdEnd = message.length();
		return message.substring(1, cmdEnd).toLowerCase();
	}

	/**
	 * This method gets the clients blocked users
	 */
	private ArrayList<String> getBlocks(ConnectionToClient client) {
		ArrayList<String> blocks = (ArrayList<String>) client.getInfo("Blocked");
		return blocks;
	}

	/**
	 * This method gets the users that have blocked the client
	 */
	private ArrayList<Thread> getBlockedMe(ConnectionToClient client) {
		Thread[] clientThreadList = getClientConnections();
		ArrayList<Thread> blockedMe = new ArrayList<>();
		for (int i=0; i<clientThreadList.length; i++)
		{
			ConnectionToClient conn= (ConnectionToClient) clientThreadList[i];
			ArrayList<String> blocked = (ArrayList<String>) conn.getInfo("Blocked");
			if (blocked.contains(client.getInfo("loginId").toString())) {
				blockedMe.add(conn);
			}
		}
		return blockedMe;
	}

	/**
	 * This method sends a message to all unblocked clients
	 */
	private void SelectiveSendToClients(Object msg, ConnectionToClient client){
		String channel = (String) client.getInfo("channel");
		ArrayList<Thread> blockedMe = getBlockedMe(client);
		Thread[] clientThreadList = getClientConnections();

		for (int i=0; i<clientThreadList.length; i++)
		{
			ConnectionToClient recipClient= (ConnectionToClient) clientThreadList[i];
			String recipChannel = (String) recipClient.getInfo("channel");
			if (!blockedMe.contains(recipClient) && recipChannel.equals(channel)) {
				try {
					//Not blocked and in same channel
					recipClient.sendToClient(msg);
				} catch (IOException e) {
					serverUI.display("Message could not be sent to the client.");
				}
			}
		}

	}

	/**
	 * This method sends server messages to clients that have not blocked the server.
	 */
	private void SendToServerFriendlyClients(Object msg){

		Thread[] clientThreadList = getClientConnections();

		for (int i=0; i<clientThreadList.length; i++)
		{
			ConnectionToClient conn= (ConnectionToClient) clientThreadList[i];
			if (!serverMuteUsers.contains(conn.getInfo("loginId"))) {
				try {
					conn.sendToClient(msg);
				} catch (IOException e) {
					serverUI.display("Message could not be sent to the client.");
				}
			}
		}
	}

	/**
	 * This method unblocks the unBlockee from the clients block list
	 */
	private boolean Unblock(ConnectionToClient client, String unBlockee) {
		ArrayList<String> blocks = getBlocks(client);
		if (blocks.contains(unBlockee)) {
			blocks.remove(unBlockee); 
			return true;
		} 
		return false;
	}

	/**
	 * Sets the clients chat channel
	 */
	private void SetChannelCmd(ConnectionToClient client, String message) {
		String newChannel = message.substring(message.indexOf(' ')+1, message.length());
		client.setInfo("channel", newChannel);
		try {
			client.sendToClient("Channel has been set to: " + newChannel);
		} catch (IOException e) {
			serverUI.display("ERROR- Unable to send message to client: " + client.getInfo("loginId"));
		}
	}
	
	/**
	 * This method removes a user from the user list
	 */
	private void removeUser(String id) {
		for(int i= 0; i < users.size(); i++){
			if(users.get(i).equals(id)){
				users.remove(i);
			}
		}
	}

	/**
	 * This method handles incoming messages from the server UI
	 */
	public void handleMessageFromServerUI(String message) {
		if(!message.startsWith("#")) {//Server Msg			
			serverUI.display(message);
			SendToServerFriendlyClients("SERVER MSG> " + message);
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
						//send msg before closing
						sendToAllClients("WARNING - The server has closed. Awaiting command.");
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
			case "block" :
				String blockee = message.substring(cmdEnd+1, message.length());				

				if(!users.contains(blockee)){
					serverUI.display("User " + blockee + " does not exist.");
				} else if(blockedClients.contains(blockee)){
					serverUI.display("Messages from " + blockee + " were already blocked.");					
				} else {
					serverUI.display("Messages from " + blockee + " will be blocked.");
					blockedClients.add(blockee);	
				}
				break;
			case "unblock" :
				if (message.trim().equals("#"+cmd)) { //#unblock all command
					if (blockedClients.isEmpty()) {
						serverUI.display("No blocking is in effect.");
					}else { //
						for (int i = 0; i < blockedClients.size(); i++) {
							serverUI.display("Messages from " + blockedClients.get(i) + " will now be displayed.");
						}
						blockedClients.clear();
					}
				} else {
					String unBlockee = message.substring(cmdEnd+1, message.length());	
					if(blockedClients.contains(unBlockee)){
						blockedClients.remove(unBlockee);
						serverUI.display("Messages from " + unBlockee + " will now be displayed" );
					}
					else{
						serverUI.display("Messages from " + unBlockee + " were not blocked");
					}
				}

				break;
			case "whoiblock" :
				if (blockedClients.isEmpty()) {
					serverUI.display("No blocking is in effect.");
				}else {
					for (int i = 0; i < blockedClients.size(); i++) {
						serverUI.display("Messages from " + blockedClients.get(i) + " are blocked.");
					}
				}
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
