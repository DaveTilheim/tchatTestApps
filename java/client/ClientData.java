import java.util.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class ClientData
{
	private String username = "unknown";
	private String password = "unknown";
	private ClientLocalisation address = new ClientLocalisation();
	private String firstConnection = "unknown";
	private String lastConnection = "unknown";
	private int id = -1;

	public int getId(){ return id; }
	public String getUsername(){ return username; }
	public String getPassword(){ return password; }
	public ClientLocalisation getAddress(){ return address; }
	public String getFirstConnection(){ return firstConnection; }
	public String getLastConnection(){ return lastConnection; }

	public boolean setId(int id)
	{
		this.id = id;

		return true;
	}

	public boolean setUsername(final String username)
	{
		this.username = username;

		return true;
	}

	public boolean setPassword(final String password)
	{
		this.password = password;

		return true;
	}

	public boolean setAddress(final ClientLocalisation address)
	{
		this.address = address;

		return true;
	}

	public boolean setFirstConnection(final String firstConnection)
	{
		this.firstConnection = firstConnection;

		return true;
	}

	public boolean setLastConnection(final String lastConnection)
	{
		this.lastConnection = lastConnection;

		return true;
	}

	public boolean same(ClientData cd)
	{
		if(this.username.compareTo(cd.username) == 0)
			return true;
		return false;
	}

	public static ClientData getDataFromFile(String username)
	{
		ClientData client = new ClientData();
		try
		{
			File file = new File("database/"+username+"/"+"info.txt");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String buf;
			List<String> att = new ArrayList<>();
			while((buf = br.readLine()) != null)
			{
				att.add(buf);
			}
			client.username = att.get(0);
			client.password = att.get(1);
			client.address.setLandName(att.get(2));
			client.address.setCityName(att.get(3));
			client.address.setStreetName(att.get(4));
			client.address.setNumber(Integer.parseInt(att.get(5)));
			client.firstConnection = att.get(6);
			client.lastConnection = att.get(7);
			client.id = Integer.parseInt(att.get(8));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

		return client;
	}

	public void setDataToFile()
	{
		try
		{
			PrintWriter pw = new PrintWriter(new BufferedWriter (new FileWriter("database/"+this.username+"/info.txt", false)));
			pw.println(this.username);
			pw.println(this.password);
			pw.println(this.address.getLandName());
			pw.println(this.address.getCityName());
			pw.println(this.address.getStreetName());
			pw.println(Integer.toString(this.address.getNumber()));
			pw.println(this.firstConnection);
			pw.println(this.lastConnection);
			pw.println(Integer.toString(this.id));
			pw.close();
		}catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	public class ClientLocalisation
	{
		private String landName = "unknown";
		private String cityName = "unknown";
		private String streetName = "unknown";
		private int number = -1;

		public String getLandName(){ return landName; }
		public String getCityName(){ return cityName; }
		public String getStreetName(){ return streetName; }
		public int getNumber(){ return number; }

		public boolean setLandName(final String landName)
		{
			//traitement
			this.landName = landName;

			return true;
		}

		public boolean setCityName(final String cityName)
		{
			this.cityName = cityName;

			return true;
		}

		public boolean setStreetName(final String streetName)
		{
			this.streetName = streetName;

			return true;
		}

		public boolean setNumber(final int number)
		{
			this.number = number;

			return true;
		}
	}
}
