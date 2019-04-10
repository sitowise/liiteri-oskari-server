package fi.nls.oskari.printout.output.map;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MapProducerAdditionalData
{
	private List<URL> legendUrls = new Vector<URL>();
	private Map<String, String> xClientInfo = new HashMap<>();
	
	public MapProducerAdditionalData()
	{
	}

	public List<URL> getLegendUrls()
	{
		return legendUrls;
	}

	public void setLegendUrls(List<URL> legendUrls)
	{
		this.legendUrls = legendUrls;
	} 
	
	public Map<String, String> getxClientInfo() {
		return xClientInfo;
	}
	
	public void setxClientInfo(Map<String, String> xClientInfo) {
		this.xClientInfo = xClientInfo;
	}
	
	public void merge(MapProducerAdditionalData data) {
		legendUrls.addAll(data.getLegendUrls());
		if (data.xClientInfo != null) {
			xClientInfo.putAll(data.xClientInfo);
		}
	}
}
