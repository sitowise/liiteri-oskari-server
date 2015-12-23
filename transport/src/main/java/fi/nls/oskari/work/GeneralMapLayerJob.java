package fi.nls.oskari.work;

import fi.nls.oskari.arcgis.ArcGisCommunicator;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.pojo.*;
import fi.nls.oskari.transport.TransportService;
import fi.nls.oskari.utils.HttpHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Job for WFS Map Layer
 */
public class GeneralMapLayerJob extends Job {
	
	private static final Logger log = LogFactory.getLogger(GeneralMapLayerJob.class);
    private static final List<List<Object>> EMPTY_LIST = new ArrayList();

    public static final String OUTPUT_LAYER_ID = "layerId";
    public static final String OUTPUT_ONCE = "once";
    public static final String OUTPUT_MESSAGE = "message";
    public static final String OUTPUT_FEATURES = "features";
    public static final String OUTPUT_FEATURE = "feature";
    public static final String OUTPUT_FIELDS = "fields";
    public static final String OUTPUT_LOCALES = "locales";
    public static final String OUTPUT_KEEP_PREVIOUS = "keepPrevious";
    public static final String OUTPUT_STYLE = "style";

    public static final String OUTPUT_IMAGE_SRS = "srs";
    public static final String OUTPUT_IMAGE_BBOX = "bbox";
    public static final String OUTPUT_IMAGE_ZOOM = "zoom";
    public static final String OUTPUT_IMAGE_TYPE = "type";
    public static final String OUTPUT_IMAGE_WIDTH = "width";
    public static final String OUTPUT_IMAGE_HEIGHT= "height";
    public static final String OUTPUT_IMAGE_URL = "url";
    public static final String OUTPUT_IMAGE_DATA = "data";
    public static final String OUTPUT_BOUNDARY_TILE = "boundaryTile";

    public static final String BROWSER_MSIE = "msie";
    
    public static final String PROCESS_STARTED = "Started";
    public static final String PROCESS_ENDED = "Ended";

	// process information
	private TransportService service;
	private SessionStore session;
    private Layer sessionLayer;
	private WFSLayerStore layer;
	private String layerId;
	private boolean layerPermission;
	private boolean reqSendFeatures;
	private boolean reqSendImage;
    private boolean reqSendHighlight;
	private MapLayerJobType type;
	
	// API
	private static final String PERMISSIONS_API = "GetLayerIds";
	private static final String LAYER_CONFIGURATION_API = "GetWFSLayerConfiguration&id=";

    // COOKIE
    public static final String ROUTE_COOKIE_NAME = "ROUTEID=";
    
    private IDetailedMapLayerJob _innerMapLayerJob = null;
    private DetailedMapLayerJobFactory _factory = new DetailedMapLayerJobFactory();

	/**
	 * Creates a new runnable job with own Jedis instance
	 * 
	 * Parameters define client's service (communication channel), session and layer's id.
	 * Sends all resources that the layer configuration allows.
	 * 
	 * @param service
	 * @param store
	 * @param layerId
	 */
	public GeneralMapLayerJob(TransportService service, MapLayerJobType type, SessionStore store, String layerId) {
		this(service, type, store, layerId, true, true, true);
    }
	
	/**
	 * Creates a new runnable job with own Jedis instance
	 * 
	 * Parameters define client's service (communication channel), session and layer's id.
	 * Also sets resources that will be sent if the layer configuration allows.
	 * 
	 * @param service
	 * @param store
	 * @param layerId
	 * @param reqSendFeatures
	 * @param reqSendImage
     * @param reqSendHighlight
	 */
	public GeneralMapLayerJob(TransportService service, MapLayerJobType type, SessionStore store, String layerId,
			boolean reqSendFeatures, boolean reqSendImage, boolean reqSendHighlight) {
		this.service = service;
        this.type = type;
		this.session = store;
        this.layerId = layerId;
        this.sessionLayer = this.session.getLayers().get(this.layerId);
		this.layer = null;
		this.layerPermission = false;
		this.reqSendFeatures = reqSendFeatures;
		this.reqSendImage = reqSendImage;
        this.reqSendHighlight = reqSendHighlight;
    }

    /**
     * Gets service path for local API
     *
     * Path for Layer configuration and permissions request
     *
     * @param sessionId
     * @return URL
     */
    public static String getAPIUrl(String sessionId) {
        String session = "";
        if(TransportService.SERVICE_URL_SESSION_PARAM != null) {
            session = ";" + TransportService.SERVICE_URL_SESSION_PARAM + "=" + sessionId;
        }
        return TransportService.SERVICE_URL + TransportService.SERVICE_URL_PATH + session + TransportService.SERVICE_URL_LIFERAY_PATH;
    }


    /**
     * Gets layer permissions (uses cache)
     *
     * @param layerId
     * @param sessionId
     * @param route
     * @return <code>true</code> if rights to use the layer; <code>false</code>
     *         otherwise.
     */
    private boolean getPermissions(String layerId, String sessionId, String route) {
        String json = WFSLayerPermissionsStore.getCache(sessionId);
        boolean fromCache = (json != null);
        if(!fromCache) {
            log.debug("Getting permission from service ", getAPIUrl(sessionId) + PERMISSIONS_API);
            String cookies = null;
            if(route != null && !route.equals("")) {
                cookies = ROUTE_COOKIE_NAME + route;
            }
            json = HttpHelper.getRequest(getAPIUrl(sessionId) + PERMISSIONS_API, cookies);
            if(json == null)
                return false;
        }
        try {
            WFSLayerPermissionsStore permissions = WFSLayerPermissionsStore.setJSON(json);
            return permissions.isPermission(layerId);
        } catch (IOException e) {
            log.error(e, "JSON parsing failed for WFSLayerPermissionsStore \n" + json);
        }

        return false;
    }

    /**
     * Gets layer configuration (uses cache)
     *
     * @param layerId
     * @param sessionId
     * @param route
     * @return layer
     */
    private WFSLayerStore getLayerConfiguration(String layerId, String sessionId, String route) {
        String json = WFSLayerStore.getCache(layerId);
        boolean fromCache = (json != null);
        if(!fromCache) {
            log.debug("Getting WFS layer configuration from service ", getAPIUrl(sessionId) + LAYER_CONFIGURATION_API + layerId);
            String cookies = null;
            if(route != null && !route.equals("")) {
                cookies = ROUTE_COOKIE_NAME + route;
            }
            HttpHelper.getRequest(getAPIUrl(sessionId) + LAYER_CONFIGURATION_API + layerId, cookies);
            json = WFSLayerStore.getCache(layerId);
            if(json == null)
                return null;
        }
        try {
            return WFSLayerStore.setJSON(json);
        } catch (Exception e) {
            log.error(e, "JSON parsing failed for WFSLayerStore \n" + json);
        }

        return null;
    }

    /**
	 * Releases all when removed
	 */
    @Override
    protected void finalize() throws Throwable {
    	super.finalize();
    }

    /**
     * Unique key definition 
     */
	@Override
	public String getKey() {
		return this.getClass().getSimpleName() + "_" + this.session.getClient() + "_" + this.layerId + "_" + this.type;
	}

    /**
	 * Process of the job
	 * 
	 * Worker calls this when starts the job.
	 *
	 */
	@Override
	public final void run() {
        log.debug(PROCESS_STARTED, getKey());

        if(!this.validateType()) {
            log.warn("Not enough information to continue the task (" +  this.type + ")");
            return;
        }

    	if(!goNext()) return;

        this.layerPermission = getPermissions(layerId, this.session.getSession(), this.session.getRoute());
        if(!this.layerPermission) {
            log.warn("Session (" +  this.session.getSession() + ") has no permissions for getting the layer (" + this.layerId + ")");
            Map<String, Object> output = new HashMap<String, Object>();
            output.put(OUTPUT_LAYER_ID, this.layerId);
            output.put(OUTPUT_ONCE, true);
            output.put(OUTPUT_MESSAGE, TransportService.ERROR_NO_PERMISSIONS);
            this.service.send(session.getClient(), TransportService.CHANNEL_ERROR, output);
            return;
        }

    	if(!goNext()) return;
    	this.layer = getLayerConfiguration(this.layerId, this.session.getSession(), this.session.getRoute());
        if(this.layer == null) {
            log.warn("Layer (" +  this.layerId + ") configurations couldn't be fetched");
            Map<String, Object> output = new HashMap<String, Object>();
            output.put(OUTPUT_LAYER_ID, this.layerId);
            output.put(OUTPUT_ONCE, true);
            output.put(OUTPUT_MESSAGE, TransportService.ERROR_CONFIGURATION_FAILED);
            this.service.send(session.getClient(), TransportService.CHANNEL_ERROR, output);
            return;
        }           
        
        this._innerMapLayerJob = _factory.newInneMapLayerJob(this, service, type, session, layerId, layerPermission, layer, reqSendFeatures, reqSendImage, reqSendHighlight);
        this._innerMapLayerJob.run();
        
        Layer layer = this.session.getLayers().get(this.layerId);
        if (this.type == MapLayerJobType.NORMAL && layer != null && layer.getTiles() != null && layer.getTiles().size() > 0) {
            log.info("Layer (" +  this.layerId + ") sending job finish info");
            Map<String, Object> output = new HashMap<String, Object>();
            output.put(OUTPUT_LAYER_ID, this.layerId);
            output.put(OUTPUT_MESSAGE, TransportService.JOB_FINISHED_INFO);
        	this.service.send(session.getClient(), TransportService.CHANNEL_STATUS, output);
        }
        
        log.debug(PROCESS_ENDED, getKey());
	}
    
    /**
     * Checks if enough information for running the task type
     *
     * @return <code>true</code> if enough information for type; <code>false</code>
     *         otherwise.
     */
    private boolean validateType() {
        if(this.type == MapLayerJobType.HIGHLIGHT) {
            if(this.sessionLayer.getHighlightedFeatureIds() != null &&
                    this.sessionLayer.getHighlightedFeatureIds().size() > 0) {
                return true;
            }
        } else if(this.type == MapLayerJobType.MAP_CLICK) {
            if(session.getMapClick() != null) {
                return true;
            }
        } else if(this.type == MapLayerJobType.GEOJSON) {
            if(session.getFilter() != null) {
                return true;
            }
        } else if(this.type == MapLayerJobType.NORMAL) {
            return true;
        }
        return false;
    }

}