package pl.sito.liiteri.arcgis;

public class ArcgisServerConfiguration
{
	private String username;
	private String password;
	private String url;	

	public ArcgisServerConfiguration(String username, String password,
			String url)
	{
		super();
		this.username = username;
		this.password = password;
		this.url = url;
	}
	
	public String getUsername()
	{
		return username;
	}
	public void setUsername(String username)
	{
		this.username = username;
	}
	public String getPassword()
	{
		return password;
	}
	public void setPassword(String password)
	{
		this.password = password;
	}
	public String getUrl()
	{
		return url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}		
	public String getTokenUrl() {
		return getUrl() + "/arcgis/tokens/generateToken";
	}
	
	
}
