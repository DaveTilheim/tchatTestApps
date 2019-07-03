import java.io.*;

public interface Utils
{
	public static void createDirectory(String path) throws Exception
	{
		File dir = new File(path);
		dir.mkdir();
	}

	public static boolean createFile(String path) throws Exception
	{
		File file = new File(path);
		return file.createNewFile();
	}

	public static int countCharInString(String s, char c)
	{
		int count = 0;
		for(int i = 0; i < s.length(); i++)
		{
			if(s.charAt(i) == c)
			{
				count++;
			}
		}
		return count;
	}
}
