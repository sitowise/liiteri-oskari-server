package fi.nls.oskari.work;

import fi.nls.oskari.pojo.SessionStore;
import fi.nls.oskari.pojo.WFSLayerStore;
import fi.nls.oskari.transport.TransportService;

public interface IDetailedMapLayerJob {
	
	public void init(Job parentJob, TransportService service, MapLayerJobType type, SessionStore store, 
			String layerId,
			boolean layerPermission, WFSLayerStore layer,
			boolean reqSendFeatures, boolean reqSendImage, boolean reqSendHighlight);
	public void run();
}
