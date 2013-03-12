
import java.io.*;
import common.*;

public class ServerConsole implements ChatIF{

	//Class variables *************************************************
	final public static int DEFAULT_PORT = 5555;

	//Instance variables **********************************************
	EchoServer server;

	//Constructors **********************************************
	public ServerConsole(){
		server = new EchoServer(DEFAULT_PORT, this);
	}

	public ServerConsole(int port) 
	{
		server = new EchoServer(port, this);
	}
	
	//Instance methods ************************************************
	@Override
	public void display(String message) {
		System.out.println(message);
	}

	/**
	 * This method waits for input from the console.  Once it is 
	 * received, it sends it to the client's message handler.
	 */
	public void accept() 
	{
		try
		{
			BufferedReader fromConsole = new BufferedReader(new InputStreamReader(System.in));
			String message;

			while (true) 
			{
				message = fromConsole.readLine();
				server.handleMessageFromServerUI(message);
			}
		} 
		catch (Exception ex) 
		{
			System.out.println
			("Unexpected error while reading from console!");
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