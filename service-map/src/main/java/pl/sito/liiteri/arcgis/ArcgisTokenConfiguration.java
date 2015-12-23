package pl.sito.liiteri.arcgis;

public class ArcgisTokenConfiguration
{
	public enum TokenType {
		Referer,
		Request,
		Ip,
	}
	
	private TokenType tokenType;
	private String referer;
	private String ip;
	
	private ArcgisTokenConfiguration() {
		
	}
	
	public TokenType getTokenType()
	{
		return tokenType;
	}
	private void setTokenType(TokenType tokenType)
	{
		this.tokenType = tokenType;
	}
	public String getReferer()
	{
		return referer;
	}
	private void setReferer(String referer)
	{
		this.referer = referer;
	}
	public String getIp()
	{
		return ip;
	}
	private void setIp(String ip)
	{
		this.ip = ip;
	}
	
	@Override
	public String toString() {
		return getTokenType().toString() + ":" + getReferer() + ":" + getIp();
	}
	
	public static ArcgisTokenConfiguration createRequestConfiguration() {
		ArcgisTokenConfiguration result = new ArcgisTokenConfiguration();
		result.setTokenType(TokenType.Request);
		
		return result;
	}
	
	public static ArcgisTokenConfiguration createRefererConfiguration(String referer) {
		ArcgisTokenConfiguration result = new ArcgisTokenConfiguration();
		result.setTokenType(TokenType.Referer);
		result.setReferer(referer);
		
		return result;
	}
	
	public static ArcgisTokenConfiguration createIpConfiguration(String ip) {
		ArcgisTokenConfiguration result = new ArcgisTokenConfiguration();
		result.setTokenType(TokenType.Ip);
		result.setIp(ip);
		
		return result;
	}
}
