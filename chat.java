
import java.util.*;
import java.net.*;
import java.io.*;


/*CS 470- Project 1- A Chat Application
 * ==========
 * Commands
 * ==========
 * - help
 * - myip
 * - myport
 * - connect (IP Address) (Port Number)
 * - list 
 * - terminate (Connection ID)
 * - send (Connection ID) (Message)
 * - exit */
 
 
 /*AFTER DEMOING: after we finished demoing, we noticed that they do not check to see if commands need a integer, so
 *you can just parse the integer without a try catch.  This will solve some problems saying that it is not a valid integer.*/
 
 

public class chat {
	/* 
	 * Lists that are used to hold the data
	 */
	private static ArrayList<String> iPAddressList = new ArrayList<String>();
	private static ArrayList<Integer> portNumberList = new ArrayList<Integer>();
	private static ArrayList<Integer> idList = new ArrayList<Integer>();
	private static ArrayList<ClientHandler> clientList = new ArrayList<ClientHandler>();

	private static int portNumber; 
	private static String ipAddress;
	private static int peerID = 0;
	private static ServerSocket serverSocket;
	private static int numberOfConnections = 0;


	//------------------------------MAIN------------------------------

	public static void main(String[] args) {
		//if there are more than 1 arguments taken
		if (args.length != 1) {
			System.out.println("This program only accepts 1 argument.");
			System.out.println("System will now close.");
			System.exit(1);
		}

		//if the argument taken is not a number/cannnot be parsed into a digit
		try{
			portNumber = Integer.parseInt(args[0]);
		}catch(Exception e) {
			System.out.println("The input that you have entered is not a number. System will now exit.");
			System.exit(1);
		}
		//if the port number is not in range, display error. 
		if (!isPortInRange(portNumber)){
			System.out.println("The port number is not in range. The system will now close.");
			System.exit(1);
		}
		//display your port number
		System.out.println("Your port number: " + portNumber);
		setIP();

		//start up server and take in input. 
		try {
			startServer();
		} catch (IOException e) {
			System.out.println("An error occured. Someone may have disconnected from the server. ");
		}
		takeInInput();
	}


	/*------------------------------INPUT------------------------------
	 * takeInInput(): Arguments: None
	 * creates a Scanner to read input and calls the checkOptions
	 */
	public static void takeInInput() {
		while (true){
			System.out.print("Client: ");
			Scanner scan = new Scanner(System.in);
			String input = scan.nextLine();
			if (input != null) {
				checkOptions(input);
			}
		}
	}

	/*------------------------------OPTIONS------------------------------
	 * CheckOptions() Arguments: input- whatever user types in
	 * Depending on what the user types, executes the commands. If user has entered invalid input, display error
	 */
	public static void checkOptions(String input) {
		//each space from the input will be in the array
		String[] command = input.split(" ");
		String first_command = command[0];

		// #1 help
		if (first_command.equals("help") && command.length == 1) {
			displayHelp();
		}

		// #2 myip
		else if (first_command.equals("myip") && command.length == 1) {
			try {
				System.out.println(getIPAddress());
			} catch (UnknownHostException e) {
				System.out.println("The IP Address cannot be found.");
			}
		}

		// #3 myport
		else if (first_command.equals("myport") && command.length == 1) {
			displayPort();
		}

		// #4 connect 
		else if (first_command.equals("connect") && command.length == 3){
			boolean ifInList = false;
			String desIP = command[1];
			int portNum = 0;
			for (int i = 0; i < iPAddressList.size(); i++) {
				if (desIP.equals(iPAddressList.get(i))) {
					ifInList = true;
				}
			}
			if (numberOfConnections < 3) {
				if(!desIP.contains(".")) {
					System.out.println("The second argument is not a valid IP Address");
				}
				else if (desIP.equals(ipAddress)) {
					System.out.println("You cannot connect to yourself. ");
				}
				else if (ifInList) {
					System.out.println("You cannot connect to a peer that has already been connected.");
				}
				else {
					try{
						portNum = Integer.parseInt(command[2]);
					}catch(Exception e) {
						System.out.println("The Port you have entered is not an Integer.");
					}
					if (!isPortInRange(portNum)) {
						System.out.println("the port number you have entered is not in the range");
					}
					else {
						System.out.println("Attempting to connect... Please wait.");
						connectToPeer(desIP, portNum);
					}
				}
			}
			else {
				System.out.println("You cannot connect to too many peers. 3 is enough. ");
			}
		}

		// #5 list
		else if (first_command.equals("list") && command.length == 1) {
			displayList();
		}

		// #6 terminate
		else if (first_command.equals("terminate") && command.length == 2){
			int connectionId = 0;
			//check arraylist for the ID, if has then
			if (numberOfConnections == 0) {
				System.out.println("There are no connections to terminate");
			}
			else {
				try{
					connectionId = Integer.parseInt(command[1]);
				}catch(Exception e) {
					System.out.println("This is not a valid ID");
				}

				if (idList.contains(connectionId)) {
					terminateConnection(connectionId);
				}
				else {
					System.out.println("There is no such connection id in the list.");
				}
			}
		} 

		// #7 send
		else if (first_command.equals("send") && command.length >= 3) {
			int peerId = 0;
			try{
				peerId = Integer.parseInt(command[1]);
				if (idList.contains(peerId)) {
					String messageToBeSent = "";
					String space = " ";

					for(int msgIndex = 2; msgIndex < command.length; msgIndex++){
						messageToBeSent += space;
						messageToBeSent += command[msgIndex];
					}

					System.out.println();
					talkWithPeer(peerId, messageToBeSent);
				}
				else {
					System.out.println("There is no such ID in the list.");
				}
			}catch(Exception e) {
				System.out.println("Invalid arguement. That is not an integer");
			}

		}

		// #8 exit
		else if (first_command.equals("exit") && command.length == 1){
			terminateEverything();
		}

		// invalid command
		else {
			System.out.println("Invalid command.");
		}
	}

	/*------------------------------SERVER------------------------------
	 * StartServer() - Arguments: None
	 * Starts the server part of the socket. 
	 * */
	@SuppressWarnings("resource")
	public static void startServer() throws IOException {
		System.out.println("Starting server...");
		serverSocket = new ServerSocket(portNumber);
		if(serverSocket != null){
			System.out.println("You are listening on port: " + serverSocket.getLocalPort());
			new Thread(() -> {
				while(true){
					try {
						Socket clientSocket = serverSocket.accept();
						ClientHandler ch = new ClientHandler(clientSocket);
						Thread cThread = new Thread(ch);
						cThread.start();
					} catch (Exception e) {
						System.out.println("An error occured with the server.");
						//e.printStackTrace();
					}
				}				
			}).start();
		}

	}


	//------------------------------METHODS------------------------------


	/*#1. help - display user interface options.*/
	public static void displayHelp(){
		try{
			String fileName = "help.txt";
			FileReader helpFile = new FileReader(fileName);
			BufferedReader bReader = new BufferedReader(helpFile);
			String line;

			while((line = bReader.readLine()) != null){
				System.out.println(line);
			}
			helpFile.close();
		} catch(IOException ex){
			System.out.println("Missing help.txt file.");
		}
	}

	/*#2. myip - myip command - show the IP address of the laptop that runs the program.*/
	public static String getIPAddress() throws UnknownHostException {
		return ipAddress;
	}

	/*#3. myport - myport command: should display the port # that the program is running on.*/
	public static void displayPort() {
		System.out.println("The program runs on port number " + portNumber);
	}

	/*#4. connect - connect command: should connect to a max of 3 peers and success message
	should be displayed.*/
	public static void connectToPeer(String desIP, int portNum) {
		try { 
			Socket connectedPeer = new Socket(desIP, portNum);
			if(connectedPeer.isConnected()){
				System.out.println("The connection to peer " + desIP + " is succesfully established!.");
			}

			iPAddressList.add(desIP);
			portNumberList.add(portNum);
			idList.add(peerID);
			clientList.add(new ClientHandler(connectedPeer));
			DataOutputStream db = new DataOutputStream(connectedPeer.getOutputStream());
			int index= idList.indexOf(peerID);
			clientList.get(index).dos.writeBytes("The connection to " + getIPAddress() + " on port" + portNumber +
			" is successfully established.\n");
			clientList.get(index).dos.flush();
			peerID++;
			numberOfConnections++;
			System.out.println("You have connected to " + numberOfConnections + " peers.");
		} catch (IOException e) {
			System.out.println("Failed to connect. This failure can be due to an faulty IP or too many connections. Try again later.");
			//e.printStackTrace();
		}
	}

	/*#5. list - list Display a numbered list of all the connections this process is part of. This numbered list will include
	connections initiated by this process and connections initiated by other processes. The output should
	display the IP address and the listening port of all the peers the process is connected to. */
	public static void displayList() {
		System.out.println("id:\tIP Address\tPort Num.");
		if (!idList.isEmpty()) {
			for (int i = 0; i < idList.size(); i++) {
				System.out.println(idList.get(i) + "\t" + iPAddressList.get(i) + "\t" + portNumberList.get(i));
			}
		}
		else{
			System.out.println("The list is empty. ");
		}
	}

	/*#6. terminate <connection id.> - terminate command: should terminate a connection.*/	
	public static void terminateConnection(int iDNumber){
		if (idList.contains(iDNumber)) {
			int indexRemove = idList.indexOf(iDNumber);
			String peerIP = iPAddressList.get(indexRemove);
			iPAddressList.remove(indexRemove);
			portNumberList.remove(indexRemove);
			idList.remove(indexRemove);

			String terminatingMessage = "";
			try {
				terminatingMessage = "Peer " + getIPAddress() + " terminates the connection.";
			} catch (UnknownHostException e) {
				System.out.println("Failed to get the IP Address.");
			}

			try {
				clientList.get(indexRemove).dos.writeBytes(terminatingMessage + "\n");
			} catch (IOException e) {
				System.out.println("Failed to send termination message to peer.");
			}

			clientList.get(indexRemove).closeReader();
			clientList.get(indexRemove).closeStream();
			clientList.get(indexRemove).closeSocket();
			clientList.remove(indexRemove);
			numberOfConnections--;

			System.out.println("Sucessfully terminated connection from " + peerIP);
		}
		else {
			System.out.println("ID number " + iDNumber + " is not in your list.");
		}
	}

	/*#7. send <connection id> <message> - send command: should send the exact message as typed by the user to another
	peer as specified in the send command. The received message should be displayed
	with relevant information as specified.*/
	public static void talkWithPeer(int connectionId, String message) {
		try {
			String sendFrom = "Message received from " + getIPAddress() + "\n";
			String senderPort = "Sender's Port: " + getPortNumber() + "\n";
			String sendMessage = "Message: " + message;
			clientList.get(connectionId).dos.writeBytes(sendFrom + senderPort + sendMessage + "\n");
			clientList.get(connectionId).dos.flush();
			System.out.println("Message sent.");
		} catch (IOException e) {
			System.out.println("An error occured when sending to a peer. ");
			//e.printStackTrace();
		}
	}

	/*#8. exit - exit command: should quit the program. On exiting, the user terminates all the
	connections. The other peers update their connection list by removing the exit peer.*/
	public static void terminateEverything(){			
		for (int i = 0; i < peerID; i++) {
			if (idList.contains(i)) {
				terminateConnection(i);
			}
		}
		System.exit(0);
		
	}

	/*
	 * getPortNumber() Arguemnts: None
	 * Returns the Port Number/Argument inputted in the console when running
	 */
	public static int getPortNumber(){
		return portNumber;
	}

	/*
	 * SetIP() Arguments: None 
	 * Sets the IP address; if not found, display error
	 */
	public static void setIP() {
		try{
			ipAddress = Inet4Address.getLocalHost().getHostAddress();
		} catch(UnknownHostException e){
			System.out.println("The IP Address cannot be found.");
		}
	}

	/*
	 * isPortInRange() Arguments: number- number for the port
	 * checks to see if the port is in the range.
	 * If the port is not in range, return false
	 * If the port is in range, return true/
	 */
	public static boolean isPortInRange(int number) {
		if(number > 0 && number <= 65536) {
			return true;
		}
		else{
			return false;
		}
	}
	

	/*----------------------------------------------------------------
	 *                            ClientHandler Class
	 * ---------------------------------------------------------------
	 * 
	 * Functions: Handler for the clients: Thread, Socket, Streams, Readers, Input
	 * Methods: ClientHandler() - constructor
	 * 			run() - handles thread function
	 * 			closeReader() - close Reader
	 * 			closeStream() - close Stream
	 * 			closeSocket() - close Socket
	 * 
	 */
	static class ClientHandler implements Runnable{
		private Socket clientSocket;
		private BufferedReader br = null;
		private DataOutputStream dos = null;

		ClientHandler(Socket connectedSocket){
			clientSocket = connectedSocket;
			try {
				br = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
			} catch (IOException e) {
				System.out.println("Input stream failed to load in the ClientHandler class.");
				//e.printStackTrace();
			}
			try {
				dos = new DataOutputStream(clientSocket.getOutputStream());
			} catch (IOException e) {
				System.out.println("Outputstream failed to load in the ClientHandler class");
				//e.printStackTrace();
			}
		}

		/*
		 * run() Arguments: None
		 * Continuously listens for input from peer. This is for the thread.
		 */
		@Override
		public void run() {
			try {
				while(true){
					String message = br.readLine();
					if(message != null){
						if(message.contains("terminates")){
							System.out.println(message);
							//remove the port and ip and peerid from the list. just say the 
							break;
						}
					//else if(message.contains("established!.")) {
						//string split " "
						// add the message[3] and message[6] which are the IP and port into the list. 
						//so its a 2 way tcp. Also increment the peerId
					//}
						else {
							System.out.println("Message: " + message);
						}
					}
				}
				closeSocket();
			} catch (IOException e1) {
				System.out.println("Something wrong happened to the client thread. ");
				//e1.printStackTrace();
			}
		}

		public void closeReader(){
			try {
				this.br.close();
			} catch (IOException e) {
				System.out.println("Error occured when closing the Reader in the ClientHandler");
				//e.printStackTrace();
			}
		}

		public void closeStream(){
			try {
				this.dos.close();
			} catch (IOException e) {
				System.out.println("Error occured when closing the Stream in the ClientHandler");
				//e.printStackTrace();
			}
		}
		

		public void closeSocket(){
			try {
				this.clientSocket.close();
			} catch (IOException e) {
				System.out.println("Error occured when closing the Socket in the ClientHandler");
				//e.printStackTrace();
			}
		}
	}

}
