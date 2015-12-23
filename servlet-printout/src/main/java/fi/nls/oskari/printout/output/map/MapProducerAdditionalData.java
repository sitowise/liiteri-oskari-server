package fi.nls.oskari.printout.output.map;

import java.net.URL;
import java.util.List;
import java.util.Vector;

public class MapProducerAdditionalData
{
	private List<URL> legendUrls = new Vector<URL>();
	
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
	
	public void merge(MapProducerAdditionalData data) {		
		legendUrls.addAll(data.getLegendUrls());
	}
}
