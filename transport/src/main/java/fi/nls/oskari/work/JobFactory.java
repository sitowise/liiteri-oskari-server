package fi.nls.oskari.work;

import fi.nls.oskari.pojo.SessionStore;
import fi.nls.oskari.transport.TransportService;

public class JobFactory {

	public JobFactory() {
		
	}
	
	public Job newJob(TransportService service, MapLayerJobType type, SessionStore store, String layerId) {
		Job job = new GeneralMapLayerJob(service, type, store, layerId);    	
    	return job;
	}
	
	public Job newJob(TransportService service, MapLayerJobType type, SessionStore store, String layerId,
			boolean reqSendFeatures, boolean reqSendImage, boolean reqSendHighlight) {
		Job job = new GeneralMapLayerJob(service, type, store, layerId, reqSendFeatures, reqSendImage, reqSendHighlight);
    	
    	return job;
	}
	
}
