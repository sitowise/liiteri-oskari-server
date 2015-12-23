package pl.sito.liiteri.arcgis.domain;

public class ArcgisLayer
{
	private String url;
	private int layerId;
	
	public ArcgisLayer(String url, int layerId) {
		setLayerId(layerId);
		setMapServerUrl(url);
	}
	
	public int getLayerId()
	{
		return layerId;
	}
	public void setLayerId(int layerId)
	{
		this.layerId = layerId;
	}
	
	public String getMapServerUrl()
	{
		return url;
	}
	public void setMapServerUrl(String url)
	{
		this.url = url;
	}
	
	public String getMetadataUrl() 
	{
		return getMapServerUrl() + "/" + getLayerId() + "?f=json";
	}
	
}
