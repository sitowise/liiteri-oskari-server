package pl.sito.liiteri.arcgis;

import org.json.JSONException;
import org.json.JSONObject;

public class ArcgisToken
{
	public final static ArcgisToken EMPTY = new ArcgisToken();
	
	private String token;
	private long expiration;
	
	public ArcgisToken() {
		
	}
	
	public ArcgisToken(String jsonString) throws JSONException {
		JSONObject json = new JSONObject(jsonString);
		setToken(json.getString("token"));
		setExpiration(json.getLong("expires"));
	}
	
	public long getExpiration()
	{
		return expiration;
	}
	
	private void setExpiration(long expiration)
	{
		this.expiration = expiration;
	}
	
	public String getToken()
	{
		return token;
	}
	
	private void setToken(String token)
	{
		this.token = token;
	}
	
	public boolean isEmpty() {
		return this.token == null;
	}
	
	public String toJSONString() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("token", getToken());
		json.put("expires", getExpiration());		
		return json.toString(1);
	}
}
