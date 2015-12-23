package fi.nls.oskari.work;

import java.util.List;

public interface IInnerMapLayerJob {
	
	public boolean normalHandlers(List<Double> bounds, boolean first);
	
	public boolean requestHandler(List<Double> bounds);
	public void propertiesHandler();
	public void featuresHandler();
	
	public void imagesHandler(List<List<Double>> grid, String cacheStyleName);	
	public void highlightImageHandler();
	
	public List<List<Object>> getFeatureValuesList();
}
