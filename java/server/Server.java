import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Server extends Thread
{
	public List<ClientThread> clients = new ArrayList<>();

	@Override
	public void run()
	{
		try
		{
			ServerSocket ss = new ServerSocket(1234, 100, InetAddress.getByName("localhost"));
			while(true)
			{
				Socket s = ss.accept();
				ClientThread ct = new ClientThread(s);
				ct.start();
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static int generateNewClientId()
	{
		int id = 0;
		try
		{
			File file = new File("database/idcount.txt");
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String numid = br.readLine();
			numid = numid.substring(0, numid.length());
			id = Integer.parseInt(numid.substring(0, numid.length()));
			id++;
			PrintWriter pw = new PrintWriter(new BufferedWriter (new FileWriter("database/idcount.txt", false)));
			pw.write(Integer.toString(id));
			pw.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		return id;
	}

	public static int getIdFromUsername(String username)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(new File("database/users.txt")));
			String buf;
			while((buf = br.readLine()) != null)
			{
				String [] parts = buf.split(" ");
				if(parts[0].compareTo(username) == 0)
				{
					return Integer.parseInt(parts[2]);
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		return -1;
	}

	public static String getUsernameFromId(int id)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(new File("database/users.txt")));
			String buf;
			while((buf = br.readLine()) != null)
			{
				String [] parts = buf.split(" ");
				if(Integer.parseInt(parts[2]) == id)
				{
					return parts[0];
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		return "";
	}

	public ClientThread getClientThreadFromId(int id)
	{
		int i = 0;
		for(ClientThread c : clients)
		{
			if(c.clientData.getId() == id)
			{
				return clients.get(i);
			}
			i++;
		}
		return null;
	}

	public static void addFriendRequestToFile(String dst, String newf, int newid)
	{
		try
		{
			PrintWriter pw = new PrintWriter(new BufferedWriter (new FileWriter("database/"+dst+"/friends.txt", true)));
			pw.println(newf+" "+newid+" wait");
			pw.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static void main(String[] args)
	{
		Server server = new Server();
		server.start();
	}
/*———————————————————————————————————————————————————*/
	public class ClientThread extends Thread
	{
		public BufferedReader reciever;
		public PrintWriter sender;
		public Socket socket;
		public String ip;
		public ClientData clientData;
		public String [] clientsSharedFileDst;
		public String sharedFileTitle;

		public ClientThread(Socket socket)
		{
			super();
			try
			{
				this.socket = socket;
				this.reciever = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				this.sender = new PrintWriter(this.socket.getOutputStream(), true);
				this.ip = socket.getRemoteSocketAddress().toString();
			}
			catch(IOException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			System.out.println(this.ip + " connected");
		}

		@Override
		public void run()
		{
			this.connProtocol();
			clients.add(this);
			String [] msg;
			sendDownloadContent();
			while(true)
			{
				msg = recieveMessage();
				if(msg != null)
				{
					messageRedirection(msg);
				}
				else
				{
					System.out.println("nb clients before: "+clients.size());
					clients.remove(this);
					System.out.println("nb clients after: "+clients.size());
					break;
				}
			}
			System.out.println("fin thread "+clients.size());
		}

		private void messageRedirection(String [] message)
		{
			String sig = message[0];
			int dst = Integer.parseInt(message[1]);
			int src = Integer.parseInt(message[2]);
			String data = message[3];
			if(sig.equals(Message.FRIEND_REQUEST_SIG))
			{
				friendRequest(src, data);
			}
			else if(sig.equals(Message.ACCEPT_REQUEST_SIG))
			{
				acceptFriendRequest(src, data);
			}
			else if(sig.equals(Message.CLIENT_DISCONNECT_SIG))
			{
				clientDisconnection(Server.getUsernameFromId(src), data);
			}
			else if(sig.equals(Message.SEND_TEXT_SIG))
			{
				setConvFile(dst, src, data);
				sendConvToDst(dst, src, data);
			}
			else if(sig.equals(Message.GET_CONV_SIG))
			{
				getConvAndSend(src, data);
			}
			else if(sig.equals(Message.SHARE_FILE_DST_SIG))
			{
				clientsSharedFileDst = data.split(" ");
			}
			else if(sig.equals(Message.SHARE_FILE_TITLE_SIG))
			{
				sharedFileTitle = data;
				createSharedFile(src);
			}
			else if(sig.equals(Message.SHARE_TEXT_FILE_SIG))
			{
				shareTextFile(data, src);
			}
			else if(sig.equals(Message.SHARE_BIN_FILE_SIG))
			{
				shareBinFile(data, src);
			}
			else if(sig.equals(Message.SHARE_FILE_DONE_SIG))
			{
				preventFriendToDownloadFile();
			}
			else if(sig.equals(Message.DOWNLOAD_TEXT_FILE_SIG))
			{
				uploadTextFile(data);
			}
			else if(sig.equals(Message.DOWNLOAD_BIN_FILE_SIG))
			{
				uploadBinFile(data);
			}
			else
			{
				System.out.println("message drop: "+sig+" "+dst+" "+src+" "+data);
			}
		}

		private void uploadTextFile(String fileName)
		{
			try
			{
				BufferedReader br = new BufferedReader(new FileReader(new File("database/"+clientData.getUsername()+"/conversations/friends/sharedFiles/"+fileName)));
				String buf = "";
				while((buf = br.readLine()) != null)
				{
					if(buf.length() > 1024)
					{
						while(buf.length() > 1024)
						{
							sendMessage(Message.DOWNLOAD_TEXT_FILE_SIG, Message.intToSId(clientData.getId()), Message.SERVER_ID, buf.substring(0,1024));
							buf = buf.substring(1024,buf.length());
						}
					}
					else
					{
						sendMessage(Message.DOWNLOAD_TEXT_FILE_SIG, Message.intToSId(clientData.getId()), Message.SERVER_ID, buf);
					}
				}
			}
			catch(IOException e)
			{

			}
			sendMessage(Message.DOWNLOAD_FILE_DONE_SIG, Message.intToSId(clientData.getId()), Message.SERVER_ID, "done");
		}

		private void uploadBinFile(String fileName)
		{
			try
			{
				InputStream inputStream = new FileInputStream(new File("database/"+clientData.getUsername()+"/conversations/friends/sharedFiles/"+fileName));
				int byteRead;
				String bdata="";
				int i = 0;
				while((byteRead = inputStream.read()) != -1)
				{
					bdata += Integer.toString(byteRead);
					i++;
					if(bdata.length() >= 1000)
					{
						sendMessage(Message.DOWNLOAD_BIN_FILE_SIG, Message.intToSId(clientData.getId()), Message.SERVER_ID, bdata);
						bdata = "";
					}
					else
					{
						bdata+=" ";
					}
				}
				if(bdata.length() != 0)
				{
					sendMessage(Message.DOWNLOAD_BIN_FILE_SIG, Message.intToSId(clientData.getId()), Message.SERVER_ID, bdata);
				}
			}
			catch(IOException e)
			{

			}
			sendMessage(Message.DOWNLOAD_FILE_DONE_SIG, Message.intToSId(clientData.getId()), Message.SERVER_ID, "done");
		}

		private void sendDownloadContent()
		{
			try
			{
				BufferedReader br = new BufferedReader(new FileReader(new File("database/"+clientData.getUsername()+"/conversations/friends/sharedFiles/content.txt")));
				String buf;
				while((buf = br.readLine()) != null)
				{
					sendMessage(Message.SHARE_FILE_DONE_SIG, Message.intToSId(clientData.getId()), Message.SERVER_ID, buf);
				}
			}
			catch(IOException e)
			{
				
			}
		}

		private void preventFriendToDownloadFile()
		{
			for(int i = 0; i < clientsSharedFileDst.length; i++)
			{
				if(friendIsConnectedByUsername(clientsSharedFileDst[i]))
				{
					getClientThreadFromId(Server.getIdFromUsername(clientsSharedFileDst[i])).sendMessage(Message.SHARE_FILE_DONE_SIG, Message.intToSId(Server.getIdFromUsername(clientsSharedFileDst[i])), Message.intToSId(clientData.getId()), sharedFileTitle);
				}
			}
			sendMessage(Message.SHARE_FILE_DONE_SIG, Message.intToSId(clientData.getId()), Message.intToSId(clientData.getId()), sharedFileTitle);
		}

		private void createSharedFile(int srcId)
		{
			try
			{
				Utils.createFile("database/"+Server.getUsernameFromId(srcId)+"/conversations/friends/sharedFiles/"+sharedFileTitle);
				PrintWriter pw = new PrintWriter(new BufferedWriter (new FileWriter(new File("database/"+Server.getUsernameFromId(srcId)+"/conversations/friends/sharedFiles/content.txt"), true)));
				pw.println(sharedFileTitle);
				pw.close();
				for(int i = 0; i < clientsSharedFileDst.length; i++)
				{
					pw = new PrintWriter(new BufferedWriter (new FileWriter(new File("database/"+clientsSharedFileDst[i]+"/conversations/friends/sharedFiles/content.txt"), true)));
					pw.println(sharedFileTitle);
					pw.close();
					Utils.createFile("database/"+clientsSharedFileDst[i]+"/conversations/friends/sharedFiles/"+sharedFileTitle);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		private void shareBinFile(String data, int srcId)
		{
			try
			{
				String [] bytes = data.split(" ");
				File f = new File("database/"+Server.getUsernameFromId(srcId)+"/conversations/friends/sharedFiles/"+sharedFileTitle);
				OutputStream os = new FileOutputStream(f, true);
				for(int j = 0; j < bytes.length; j++)
				{
					os.write((byte)Integer.parseInt(bytes[j]));
				}
				os.close();
				for(int i = 0; i < clientsSharedFileDst.length; i++)
				{
					f = new File("database/"+clientsSharedFileDst[i]+"/conversations/friends/sharedFiles/"+sharedFileTitle);
					os = new FileOutputStream(f, true);
					for(int j = 0; j < bytes.length; j++)
					{
						os.write((byte)Integer.parseInt(bytes[j]));
					}
					os.close();
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		private void shareTextFile(String data, int srcId)
		{
			try
			{
				File f = new File("database/"+Server.getUsernameFromId(srcId)+"/conversations/friends/sharedFiles/"+sharedFileTitle);
				PrintWriter pw = new PrintWriter(new BufferedWriter (new FileWriter(f, true)));
				pw.println(data);
				pw.close();
				for(int i = 0; i < clientsSharedFileDst.length; i++)
				{
					f = new File("database/"+clientsSharedFileDst[i]+"/conversations/friends/sharedFiles/"+sharedFileTitle);
					pw = new PrintWriter(new BufferedWriter (new FileWriter(f, true)));
					pw.println(data);
					pw.close();
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		private void getConvAndSend(int recieverId, String friend)
		{
			try
			{
				String recieverUsername = Server.getUsernameFromId(recieverId);
				File f = new File("database/"+recieverUsername+"/conversations/friends/"+friend+".txt");
				if(f.exists())
				{
					BufferedReader br = new BufferedReader(new FileReader(f));
					String header, text;
					while((header = br.readLine()) != null && (text = br.readLine()) != null)
					{
						sendMessage(Message.SEND_TEXT_SIG, Message.intToSId(recieverId), Message.intToSId(Integer.parseInt(header)), text);
					}
				}
			}
			catch(Exception e)
			{
				//e.printStackTrace();
				System.out.println("not connected");
				//System.exit(0);
			}
		}

		private void sendConvToDst(int dst, int src, String data)
		{
			ClientThread ct = getClientThreadFromId(dst);
			if(ct != null)
			{
				ct.sendMessage(Message.SEND_TEXT_SIG, Message.intToSId(dst), Message.intToSId(src), data);
			}
		}

		private void setConvFile(int dst, int src, String data)
		{
			try
			{
				File f = new File("database/"+getUsernameFromId(src)+"/conversations/friends/"+getUsernameFromId(dst)+".txt");
				if(!f.exists())
				{
					Utils.createFile("database/"+getUsernameFromId(src)+"/conversations/friends/"+getUsernameFromId(dst)+".txt");
				}
				PrintWriter pw = new PrintWriter(new BufferedWriter (new FileWriter(f, true)));
				pw.println(src);
				pw.println(data);
				pw.close();
				f = new File("database/"+getUsernameFromId(dst)+"/conversations/friends/"+getUsernameFromId(src)+".txt");
				if(!f.exists())
				{
					Utils.createFile("database/"+getUsernameFromId(dst)+"/conversations/friends/"+getUsernameFromId(src)+".txt");
				}
				pw = new PrintWriter(new BufferedWriter (new FileWriter(f, true)));
				pw.println(src);
				pw.println(data);
				pw.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.out.println("double access to file");
			}
		}

		private void clientDisconnection(String clientName, String dstClient)
		{
			ClientThread cpy = findClientByUsername(dstClient);
			if(cpy != null)
				cpy.sendMessage(Message.OFFLINE_FRIEND_STOCK_SIG, Message.intToSId(getIdFromUsername(dstClient)), Message.SERVER_ID, clientName);
		}

		private ClientThread findClientByUsername(String username)
		{
			for(ClientThread c : clients)
			{
				if(c.clientData.getUsername().equals(username))
					return c;
			}
			return null;
		}

		private void acceptFriendRequest(int srcId, String dstUsername)
		{
			int idDst = Server.getIdFromUsername(dstUsername);
			boolean conn = false;
			for(ClientThread c : clients)
			{
				if(c.clientData.getUsername().compareTo(dstUsername) == 0)
				{
					c.sendMessage(Message.ACCEPT_REQUEST_SIG, Message.intToSId(idDst), Message.SERVER_ID, Server.getUsernameFromId(srcId)+" "+srcId+" online");
					sendMessage(Message.ONLINE_FRIEND_STOCK_SIG, Message.intToSId(srcId), Message.SERVER_ID, dstUsername);
					conn = true;
				}
			}
			answerFriendRequestViaFile(srcId, dstUsername);
			if(!conn)
				sendMessage(Message.OFFLINE_FRIEND_STOCK_SIG, Message.intToSId(srcId), Message.SERVER_ID, dstUsername);
		}

		private void answerFriendRequestViaFile(int srcId, String dstUsername)
		{
			try
			{
				String srcUsername = Server.getUsernameFromId(srcId);
				PrintWriter pw = new PrintWriter(new BufferedWriter (new FileWriter("database/"+dstUsername+"/friends.txt", true)));
				pw.println(srcUsername+" "+srcId+" complete");
				pw.close();
				File file = new File("database/"+Server.getUsernameFromId(srcId)+"/friends.txt");
				BufferedReader br = new BufferedReader(new FileReader(file));
				Utils.createFile("database/"+srcUsername+"/friends-tmp.txt");
				pw = new PrintWriter(new BufferedWriter (new FileWriter("database/"+srcUsername+"/friends-tmp.txt", true)));
				String buf;
				while((buf = br.readLine()) != null)
				{
					String [] parts = buf.split(" ");
					if(dstUsername.compareTo(parts[0]) == 0)
					{
						pw.println(parts[0]+" "+parts[1]+" complete");
					}
					else
					{
						pw.println(buf);
					}
				}
				pw.close();
				file.delete();
				new File("database/"+srcUsername+"/friends-tmp.txt").renameTo(new File("database/"+srcUsername+"/friends.txt"));
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		public void friendRequest(int srcId, String dstUsername)
		{
			try
			{
				if(searchUserOnDatabase(dstUsername).compareTo("") == 0)
				{
					System.out.println(dstUsername+" not in database");
					this.sendMessage(Message.FRIEND_REQUEST_FAILED_SIG, Message.intToSId(srcId), Message.SERVER_ID, dstUsername);
				}
				else
				{
					int dstId = Server.getIdFromUsername(dstUsername);
					String srcUsername = Server.getUsernameFromId(srcId);
					ClientThread c = getClientThreadFromId(dstId);
					if(c == null)
					{
						System.out.println(dstUsername+" is not connected");
						Server.addFriendRequestToFile(dstUsername, srcUsername, srcId);
					}
					else
					{
						Server.addFriendRequestToFile(dstUsername, srcUsername, srcId);
						c.sendMessage(Message.FRIEND_REQUEST_SIG, Message.intToSId(dstId), Message.intToSId(srcId), srcUsername);
					}
					this.sendMessage(Message.FRIEND_REQUEST_SUCCESS_SIG, Message.intToSId(srcId), Message.SERVER_ID, dstUsername);
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			
		}

		private void sendMessage(String sig, String dst, String src, String data)
		{
			try
			{
				String message = Message.encapsulation(sig, dst, src, data);
				this.sender.println(message);
			}
			catch(Message.BadFormatException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		private String [] recieveMessage()
		{
			try
			{
				String msg = this.reciever.readLine();
				if(msg != null)
				{
					String [] frame = Message.desencapsulation(msg);
					return frame;
				}
				throw new IOException();
			}
			catch(IOException e)
			{
				System.out.println("fin de connection avec "+clientData.getUsername());
			}

			return null;
		}

		private void connProtocol()
		{
			boolean authenticate = false;
			while(!authenticate)
			{
				try
				{
					String [][] clientCoor = new String[2][4];
					System.out.println("en attente de username");
					clientCoor[0] = Message.desencapsulation(this.reciever.readLine());
					System.out.println("en attente de password");
					clientCoor[1] = Message.desencapsulation(this.reciever.readLine());
					String passwordDB = this.searchUserOnDatabase(clientCoor[0][3]);
					if(clientCoor[0][0].compareTo(Message.LOGIN_SIG) == 0)
					{
						System.out.println("LOG IN");
						if(clientCoor[1][3].compareTo(passwordDB) == 0)
						{
							this.clientData = ClientData.getDataFromFile(clientCoor[0][3]);
							this.updateLastConnexion();
							try
							{
								this.sender.println(Message.encapsulation(Message.GOOD_AUTHENTICATION_SIG,"0000","0000","_"));
							}catch(Message.BadFormatException e)
							{
								e.printStackTrace();
								System.exit(0);
							}
							this.sendClientData();
							this.sendFriendDataAtConnection();
							authenticate = true;
						}
						else
						{
							try
							{
								this.sender.println(Message.encapsulation(Message.BAD_AUTHENTICATION_SIG,"0000","0000","_"));
							}
							catch(Message.BadFormatException e)
							{
								e.printStackTrace();
								System.exit(0);
							}
							authenticate = false;
						}
					}
					else
					{
						System.out.println("SIGN UP "+clientCoor[1][3]);
						if(passwordDB.length() == 0 && clientCoor[1][3].length() != 0)
						{
							System.out.println("create directories");
							this.createClientDataDirectories(clientCoor[0][3], clientCoor[1][3]);
							try
							{
								System.out.println("send answer");
								this.sender.println(Message.encapsulation(Message.GOOD_AUTHENTICATION_SIG,"0000","0000","_"));
							}
							catch(Message.BadFormatException e)
							{
								e.printStackTrace();
								System.exit(0);
							}
							
							this.sendClientData();
							authenticate = true;
						}
						else
						{
							try
							{
								this.sender.println(Message.encapsulation(Message.BAD_AUTHENTICATION_SIG,"0000","0000","_"));
							}
							catch(Message.BadFormatException e)
							{
								e.printStackTrace();
								System.exit(0);
							}
							
							authenticate = false;
						}
					}
				}
				catch(IOException e)
				{
					e.printStackTrace();
					System.exit(0);
				}
			}
		}

		private void sendFriendDataAtConnection() throws IOException
		{
			String username = this.clientData.getUsername();
			File file = new File("database/"+username+"/friends.txt");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String buf;
			while((buf = br.readLine()) != null)
			{
				buf = friendIsConnected(buf);
				System.out.println("data: "+buf);
				sendMessage(Message.INIT_FRIEND_STOCK_SIG, Message.intToSId(clientData.getId()), Message.SERVER_ID, buf);
				iAmConnected(buf);
			}
		}

		private void iAmConnected(String data)
		{
			String [] parts = data.split(" ");
			for(ClientThread c : clients)
			{
				if(parts[0].compareTo(c.clientData.getUsername()) == 0)
				{
					System.out.println(c.clientData.getUsername()+" I ("+clientData.getUsername()+" am connected!");
					c.sendMessage(Message.ONLINE_FRIEND_STOCK_SIG, Message.intToSId(Integer.parseInt(parts[1])), Message.SERVER_ID, clientData.getUsername());
				}
			}
		}

		private String friendIsConnected(String data)
		{
			String [] parts = data.split(" ");
			if(parts[2].compareTo("wait") != 0)
			{
				for(ClientThread c : clients)
				{
					if(c.clientData.getUsername().compareTo(parts[0]) == 0)
					{
						return parts[0]+" "+parts[1]+" "+"online";
					}
				}
				return parts[0]+" "+parts[1]+" "+"offline";
			}
			return data;
		}

		private boolean friendIsConnectedByUsername(String username)
		{
			for(ClientThread c : clients)
			{
				if(c.clientData.getUsername().compareTo(username) == 0)
				{
					return true;
				}
			}
			return false;
		}

		private void sendClientData()
		{
			this.sender.println(this.clientData.getUsername());
			this.sender.println(this.clientData.getPassword());
			this.sender.println(this.clientData.getAddress().getLandName());
			this.sender.println(this.clientData.getAddress().getCityName());
			this.sender.println(this.clientData.getAddress().getStreetName());
			this.sender.println(this.clientData.getAddress().getNumber());
			this.sender.println(this.clientData.getFirstConnection());
			this.sender.println(this.clientData.getLastConnection());
			this.sender.println(this.clientData.getId());
		}

		private void createClientDataDirectories(String username, String password)
		{
			try
			{
				Utils.createDirectory("database/"+username);
				Utils.createFile("database/"+username+"/info.txt");
				Utils.createFile("database/"+username+"/friends.txt");
				Utils.createDirectory("database/"+username+"/conversations");
				Utils.createDirectory("database/"+username+"/conversations/friends");
				Utils.createDirectory("database/"+username+"/conversations/friends/sharedFiles");
				Utils.createFile("database/"+username+"/conversations/friends/sharedFiles/content.txt");
				System.out.println("directories created");
				this.clientData = new ClientData();
				this.clientData.setUsername(username);
				this.clientData.setPassword(password);
				this.clientData.setId(Server.generateNewClientId());
				this.updateLastConnexion();
				this.addUserToDatabase();
				System.out.println("client created");
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		private void addUserToDatabase()
		{
			try
			{
				PrintWriter pw = new PrintWriter(new BufferedWriter (new FileWriter("database/users.txt", true)));
				pw.println(this.clientData.getUsername()+" "+this.clientData.getPassword()+" "+this.clientData.getId());
				pw.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}

		private String searchUserOnDatabase(String username) throws IOException
		{
			if(username.length() == 0)
				throw new IOException("coordinates cannot be empty");
			File file = new File("database/users.txt");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String buf;
			while((buf = br.readLine()) != null)
			{
				String [] parts = buf.split(" ");
				if(parts[0].compareTo(username) == 0)
				{
					return parts[1];
				}
			}
			return "";
		}

		private void updateLastConnexion()
		{
			clientData.setLastConnection(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
			clientData.setDataToFile();
		}
	}
}
