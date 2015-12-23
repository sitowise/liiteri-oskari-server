package fi.nls.oskari.work;

import fi.nls.oskari.pojo.SessionStore;
import fi.nls.oskari.pojo.WFSLayerStore;
import fi.nls.oskari.transport.TransportService;

public class DetailedMapLayerJobFactory {
	
	public DetailedMapLayerJobFactory () {}
	
	public IDetailedMapLayerJob newInneMapLayerJob(Job parentJob, TransportService service, 
			MapLayerJobType type, SessionStore store, String layerId,
			boolean layerPermission, WFSLayerStore layer,
			boolean reqSendFeatures, boolean reqSendImage, boolean reqSendHighlight) {
		
		IDetailedMapLayerJob job = null;
		if (layer.getFeatureNamespace().equals("arcgis"))
			job = new ArcGisMapLayerJob();
		else
			job = new WFSMapLayerJob();
		
		job.init(parentJob, service, type, store, layerId, layerPermission, layer, reqSendFeatures, reqSendImage, reqSendHighlight);								
		
		return job;
	}
}
