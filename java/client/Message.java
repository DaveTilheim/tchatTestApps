import java.io.*;

public interface Message
{
	public final String VOID_SIG = "_";
	public final String LOGIN_SIG = "a";
	public final String SIGNUP_SIG = "b";
	public final String GOOD_AUTHENTICATION_SIG = "c";
	public final String BAD_AUTHENTICATION_SIG = "d";
	public final String FRIEND_REQUEST_SIG = "e";
	public final String FRIEND_REQUEST_FAILED_SIG = "f";
	public final String FRIEND_REQUEST_SUCCESS_SIG = "i";
	public final String INIT_FRIEND_STOCK_SIG = "j";
	public final String ONLINE_FRIEND_STOCK_SIG = "k";
	public final String OFFLINE_FRIEND_STOCK_SIG = "l";
	public final String ACCEPT_REQUEST_SIG = "m";
	public final String REFUSE_REQUEST_SIG = "n";
	public final String CLIENT_DISCONNECT_SIG = "o";
	public final String SEND_TEXT_SIG = "p";
	public final String GET_CONV_SIG = "q";
	public final String SHARE_TEXT_FILE_SIG = "r";
	public final String SHARE_FILE_DST_SIG = "s";
	public final String SHARE_FILE_TITLE_SIG = "t";
	public final String SHARE_BIN_FILE_SIG = "u";
	public final String SHARE_FILE_DONE_SIG = "v";
	public final String DOWNLOAD_TEXT_FILE_SIG = "w";
	public final String DOWNLOAD_BIN_FILE_SIG = "x";
	public final String DOWNLOAD_FILE_DONE_SIG = "y";
	public final String GROUP_CREATION_SIG = "z";
	public final String SERVER_ID = "0000";
	public final long MAXSIZEOF_DATA = 80000000;
	
	public class BadFormatException extends Exception
	{
		public BadFormatException()
		{
			super("bad message format");
		}
	}

	public static String encapsulation(final String signal, final String dst, final String src, final String data) throws BadFormatException
	{
		if(signal.length() != 1 || dst.length() != 4 || src.length() != 4 || data.length() > Message.MAXSIZEOF_DATA)
			throw new BadFormatException();
		//System.out.println("message encapsulé: "+signal+dst+src+data);
		return signal+dst+src+data;
	}

	public static String [] desencapsulation(final String message)
	{
		//System.out.println(message);
		String [] desencapsulateMessage = new  String[4];
		desencapsulateMessage[0] = message.substring(0,1);
		desencapsulateMessage[1] = message.substring(1,5);
		desencapsulateMessage[2] = message.substring(5,9);
		desencapsulateMessage[3] = message.substring(9,message.length());
		//System.out.println("message désencapsulé: "+message);
		return desencapsulateMessage;
	}

	public static String intToSId(final int id)
	{
		String sid = String.format("%04d", id);
		return sid;
	}

	public static String getSignalSharingFile(String fileName)
	{
		if(fileName.indexOf(".jpg") != -1 || fileName.indexOf(".gif") != -1 || fileName.indexOf(".png") != -1)
			return Message.SHARE_BIN_FILE_SIG;
		return Message.SHARE_TEXT_FILE_SIG;
	}

	public static String getSignalDownloadingFile(String fileName)
	{
		if(fileName.indexOf(".jpg") != -1 || fileName.indexOf(".gif") != -1 || fileName.indexOf(".png") != -1)
			return Message.DOWNLOAD_BIN_FILE_SIG;
		return Message.DOWNLOAD_TEXT_FILE_SIG;
	}
}
