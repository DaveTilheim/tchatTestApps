import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;


public class Client extends Thread
{
	public Socket socket;
	public BufferedReader reciever;
	public PrintWriter sender;
	public boolean connected = false;
	public ClientData data = new ClientData();
	public GuiClient gui;
	public List<FriendRequestData> friendRequests = new ArrayList<>();
	public List<FriendRequestData> friends = new ArrayList<>();
	public  String downloadedFile = "";

	public Client(String serverIp, int port, GuiClient gui) throws Exception
	{
		super();
		this.gui = gui;
		this.socket = new Socket(serverIp, port);
		this.reciever = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		this.sender = new PrintWriter(this.socket.getOutputStream(), true);
	}

	@Override
	public void run()
	{
		System.out.println("client connected");
		while(true)
		{
			messageRedirection(recieveMessage());
		}
	}

	public void disconnect()
	{
		for(FriendRequestData f : friends)
		{
			sendMessage(Message.CLIENT_DISCONNECT_SIG, Message.SERVER_ID, Message.intToSId(data.getId()), f.username);
		}
	}

	public void messageRedirection(String [] message)
	{
		String sig = message[0];
		int dst = Integer.parseInt(message[1]);
		int src = Integer.parseInt(message[2]);
		String data = message[3];
		//System.out.println(data);
		if(sig.compareTo(Message.FRIEND_REQUEST_SIG) == 0)
		{
			friendRequests.add(new FriendRequestData(data, src, "wait"));
			gui.friendRequest();
		}
		else if(sig.compareTo(Message.FRIEND_REQUEST_FAILED_SIG) == 0)
		{
			gui.friendRequestFailed(data);
		}
		else if(sig.compareTo(Message.FRIEND_REQUEST_SUCCESS_SIG) == 0)
		{
			gui.friendRequestSuccess(data);
		}
		else if(sig.compareTo(Message.INIT_FRIEND_STOCK_SIG) == 0)
		{
			initFriendStock(data);
		}
		else if(sig.compareTo(Message.ONLINE_FRIEND_STOCK_SIG) == 0)
		{
			onlineFriendStock(data);
		}
		else if(sig.compareTo(Message.ACCEPT_REQUEST_SIG) == 0)
		{
			initFriendStock(data);
		}
		else if(sig.compareTo(Message.OFFLINE_FRIEND_STOCK_SIG) == 0)
		{
			offlineFriendStock(data);
		}
		else if(sig.compareTo(Message.SEND_TEXT_SIG) == 0)
		{
			gui.mainConvArea.setConvText(data, getFriendUsername(src));
		}
		else if(sig.compareTo(Message.SHARE_FILE_DONE_SIG) == 0)
		{
			gui.mainOptionConv.titleDownloadFileList.add(data);
			//gui.mainOptionConv.alertNewDownloadFile();
		}
		else if(sig.compareTo(Message.DOWNLOAD_TEXT_FILE_SIG) == 0)
		{
			saveDownloadedTextFile(data);
		}
		else if(sig.compareTo(Message.DOWNLOAD_BIN_FILE_SIG) == 0)
		{
			saveDownloadedBinFile(data);
		}
		else if(sig.compareTo(Message.DOWNLOAD_FILE_DONE_SIG) == 0)
		{
			gui.mainOptionConv.alertDownloadedFile();
		}
		else
		{
			System.out.println("message drop: "+sig+" "+dst+" "+src+" "+data);
		}
	}

	public void saveDownloadedTextFile(String data)
	{
		try
		{
			File f = new File(downloadedFile);
			PrintWriter pw = new PrintWriter(new BufferedWriter (new FileWriter(f, true)));
			pw.println(data);
			pw.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void saveDownloadedBinFile(String data)
	{
		try
		{
			String [] bytes = data.split(" ");
			File f = new File(downloadedFile);
			OutputStream os = new FileOutputStream(f, true);
			for(int j = 0; j < bytes.length; j++)
			{
				os.write((byte)Integer.parseInt(bytes[j]));
			}
			os.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void downloadFileFromServer(String fileName)
	{
		downloadedFile = fileName;
		try{Utils.createFile(fileName);}catch(Exception e){}
		sendMessage(Message.getSignalDownloadingFile(fileName), Message.SERVER_ID, Message.intToSId(data.getId()), fileName);
	}

	public void uploadToServer(ArrayList<String> friends, File file)
	{
		try
		{
			String buf = "";
			for(String s : friends)
				buf += s+" ";
			buf = buf.substring(0,buf.length()-1);
			sendMessage(Message.SHARE_FILE_DST_SIG, Message.SERVER_ID, Message.intToSId(data.getId()), buf);
			sendMessage(Message.SHARE_FILE_TITLE_SIG, Message.SERVER_ID, Message.intToSId(data.getId()), file.getName());
			String signal = Message.getSignalSharingFile(file.getName());
			System.out.println(signal);
			if(signal.equals(Message.VOID_SIG))
				throw new IOException();
			if(signal.equals(Message.SHARE_TEXT_FILE_SIG))
			{
				BufferedReader br = new BufferedReader(new FileReader(file));
				while((buf = br.readLine()) != null)
				{
					if(buf.length() > 1024)
					{
						while(buf.length() > 1024)
						{
							sendMessage(Message.SHARE_TEXT_FILE_SIG, Message.SERVER_ID, Message.intToSId(data.getId()), buf.substring(0,1024));
							buf = buf.substring(1024,buf.length());
						}
					}
					else
					{
						sendMessage(Message.SHARE_TEXT_FILE_SIG, Message.SERVER_ID, Message.intToSId(data.getId()), buf);
					}
				}
			}
			else
			{
				InputStream inputStream = new FileInputStream(file);
				int byteRead;
				String bdata="";
				int i = 0;
				while((byteRead = inputStream.read()) != -1)
				{
					bdata += Integer.toString(byteRead);
					i++;
					if(bdata.length() >= 1000)
					{
						sendMessage(Message.SHARE_BIN_FILE_SIG, Message.SERVER_ID, Message.intToSId(data.getId()), bdata);
						bdata = "";
					}
					else
					{
						bdata+=" ";
					}
				}
				if(bdata.length() != 0)
				{
					sendMessage(Message.SHARE_BIN_FILE_SIG, Message.SERVER_ID, Message.intToSId(data.getId()), bdata);
				}
			}
		}
		catch(IOException e)
		{
			gui.mainOptionConv.sharingFileFailedDialog(file);
			return;
		}
		sendMessage(Message.SHARE_FILE_DONE_SIG, Message.SERVER_ID, Message.intToSId(data.getId()), "done");
		gui.mainOptionConv.fileSharingDone();
	}

	public void getConvFromServer(String friend)
	{
		sendMessage(Message.GET_CONV_SIG, Message.SERVER_ID, Message.intToSId(data.getId()), friend);
	}

	public String getFriendUsername(int id)
	{
		for(FriendRequestData f : friends)
		{
			if(f.id == id)
			{
				return f.username;
			}
		}
		if(data.getId() == id)
			return data.getUsername();
		return "unknow";
	}

	public int getFriendId(String friend)
	{
		for(FriendRequestData f : friends)
		{
			if(f.username.equals(friend))
			{
				return f.id;
			}
		}
		return -1;
	}

	public void sendTextToServer(String friend, String text)
	{
		text = text.replace('\n', '\\');
		System.out.println("TEXT: "+text);
		System.out.println("–––––––");
		sendMessage(Message.SEND_TEXT_SIG, Message.intToSId(getFriendId(friend)), Message.intToSId(data.getId()), text);
	}

	public boolean isAlreadyFriend(String friendName)
	{
		for(FriendRequestData f : friends)
		{
			if(f.username.equals(friendName))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isWaitingRequest(String friendName)
	{
		for(FriendRequestData f : friendRequests)
		{
			if(f.username.equals(friendName))
			{
				return true;
			}
		}
		return false;
	}

	public void offlineFriendStock(String friend)
	{
		gui.friendStockDisconnect(friend);
		for(FriendRequestData f : friends)
		{
			if(f.username.compareTo(friend) == 0)
				f.state = "offline";
		}
	}

	public void onlineFriendStock(String friend)
	{
		gui.friendStockConnect(friend);
		for(FriendRequestData f : friends)
		{
			if(f.username.compareTo(friend) == 0)
				f.state = "online";
		}
	}

	public void connProtocol(String username, String password, String connMode)
	{
		try
		{
			String connState;
			System.out.println("envoi username");
			this.sender.println(Message.encapsulation(connMode,"0000","0000",username));
			System.out.println("envoi password");
			this.sender.println(Message.encapsulation(connMode,"0000","0000",password));
			System.out.println("attente réponse");
			connState = Message.desencapsulation(this.reciever.readLine())[0];
			System.out.println("réponse: "+connState);
			if(connState.compareTo(Message.GOOD_AUTHENTICATION_SIG) == 0)
			{
				this.data.setUsername(this.reciever.readLine());
				this.data.setPassword(this.reciever.readLine());
				this.data.getAddress().setLandName(this.reciever.readLine());
				this.data.getAddress().setCityName(this.reciever.readLine());
				this.data.getAddress().setStreetName(this.reciever.readLine());
				this.data.getAddress().setNumber(Integer.parseInt(this.reciever.readLine()));
				this.data.setFirstConnection(this.reciever.readLine());
				this.data.setLastConnection(this.reciever.readLine());
				this.data.setId(Integer.parseInt(this.reciever.readLine()));
				connected = true;
			}
			else
			{
				connected = false;
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		catch(Message.BadFormatException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void acceptFriendRequest(String friend)
	{
		int i = 0;
		for(FriendRequestData f : friendRequests)
		{
			if(f.username.equals(friend))
			{
				friends.add(f);
				friendRequests.remove(i);
				break;
			}
			i++;
		}
		sendMessage(Message.ACCEPT_REQUEST_SIG, Message.SERVER_ID, Message.intToSId(this.data.getId()), friend);
	}

	public void initFriendStock(String friendData)
	{
		String [] parts = friendData.split(" ");
		if(parts[2].compareTo("wait") == 0)
		{
			System.out.println("wait: "+friendData);
			friendRequests.add(new FriendRequestData(parts[0], Integer.parseInt(parts[1]), parts[2]));
			gui.friendRequest();
		}
		else
		{
			friends.add(new FriendRequestData(parts[0], Integer.parseInt(parts[1]), parts[2]));
			gui.addFriendStock(parts[0]);
		}
		if(parts[2].compareTo("online") == 0)
		{
			gui.friendStockConnect(parts[0]);
		}
		else
		{
			gui.friendStockDisconnect(parts[0]);
		}
	}

	public String [] recieveMessage()
	{
		try
		{
			String data = this.reciever.readLine();
			if(data == null)
			{
				gui.dialogServerDown();
				gui.dispose();
				System.exit(0);
			}
			else
			{
				String [] frame = Message.desencapsulation(data);
				return frame;
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		return null;
	}

	public void sendMessage(String sig, String dst, String src, String data)
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
/*
	public static void main(String[] args)
	{
		Client client = new Client("192.168.1.41", 5566);
		client.start();
	}
*/
}
