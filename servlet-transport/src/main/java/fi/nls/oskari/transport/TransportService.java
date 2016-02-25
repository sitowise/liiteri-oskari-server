package fi.nls.oskari.transport;

import com.vividsolutions.jts.geom.Coordinate;
import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.pojo.*;
import fi.nls.oskari.service.OskariComponentManager;
import fi.nls.oskari.pojo.style.CustomStyleStore;
import fi.nls.oskari.pojo.style.CustomStyleStoreFactory;
import fi.nls.oskari.util.ConversionHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.utils.GeometryJSONOutputModule;
import fi.nls.oskari.wfs.CachingSchemaLocator;
import fi.nls.oskari.wfs.WFSImage;
import fi.nls.oskari.wfs.pojo.WFSLayerStore;
import fi.nls.oskari.wfs.util.HttpHelper;
import fi.nls.oskari.work.*;
import fi.nls.oskari.work.hystrix.HystrixJobQueue;
import fi.nls.oskari.worker.Job;
import fi.nls.oskari.worker.JobQueue;
import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.AbstractService;
import org.cometd.server.JacksonJSONContextServer;
import org.cometd.server.JettyJSONContextServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Handles all incoming requests (channels) and manages Job queues
 *
 * @see org.cometd.server.AbstractService
 */
public class TransportService extends AbstractService {
    static {
        // populate properties before initializing logger since logger
        // implementation is configured in properties
        PropertyUtil.loadProperties("/oskari.properties");
        PropertyUtil.loadProperties("/transport.properties");
        PropertyUtil.loadProperties("/oskari-ext.properties");
        PropertyUtil.loadProperties("/transport-ext.properties");
    }
    private static Logger log = LogFactory.getLogger(TransportService.class);

	// params
	public static final String PARAM_ID = "id"; // skipped param - coming from cometd
    public static final String PARAM_UUID = "uuid"; //
	public static final String PARAM_CHANNEL = "channel"; // skipped param - coming from cometd
	public static final String PARAM_DATA = "data"; // own json data under this
	public static final String PARAM_SESSION = "session";
    public static final String PARAM_ROUTE = "route";
	public static final String PARAM_LANGUAGE = "language";
	public static final String PARAM_CLIENT = "client";
	public static final String PARAM_BROWSER = "browser";
	public static final String PARAM_BROWSER_VERSION = "browserVersion";
	public static final String PARAM_LOCATION = "location";
	public static final String PARAM_LOCATION_SRS = "srs";
	public static final String PARAM_LOCATION_BBOX = "bbox";
	public static final String PARAM_LOCATION_ZOOM = "zoom";
    public static final String PARAM_TILES = "tiles";
	public static final String PARAM_TILE_SIZE = "tileSize";
	public static final String PARAM_MAP_SIZE = "mapSize";
	public static final String PARAM_WIDTH = "width";
	public static final String PARAM_HEIGHT = "height";
	public static final String PARAM_MAP_SCALES = "mapScales";
	public static final String PARAM_LAYERS = "layers";
	public static final String PARAM_LAYER_ID = "layerId";
    public static final String PARAM_REQUEST_ID = "reqId";
	public static final String PARAM_LAYER_STYLE = "styleName";
	public static final String PARAM_LONGITUDE = "longitude";
	public static final String PARAM_LATITUDE = "latitude";
	public static final String PARAM_LAYER_VISIBLE = "visible";
	public static final String PARAM_FEATURE_IDS = "featureIds";
	public static final String PARAM_KEEP_PREVIOUS = "keepPrevious";
    public static final String PARAM_GEOM_REQUEST = "geomRequest";

    public static final String CHANNEL_INIT = "/service/wfs/init";
	public static final String CHANNEL_ADD_MAP_LAYER = "/service/wfs/addMapLayer";
	public static final String CHANNEL_REMOVE_MAP_LAYER = "/service/wfs/removeMapLayer";
	public static final String CHANNEL_SET_LOCATION = "/service/wfs/setLocation";
	public static final String CHANNEL_SET_MAP_SIZE = "/service/wfs/setMapSize";
	public static final String CHANNEL_SET_MAP_LAYER_STYLE = "/service/wfs/setMapLayerStyle";
    public static final String CHANNEL_SET_MAP_LAYER_CUSTOM_STYLE = "/service/wfs/setMapLayerCustomStyle";
	public static final String CHANNEL_SET_MAP_CLICK = "/service/wfs/setMapClick";
	public static final String CHANNEL_SET_FILTER = "/service/wfs/setFilter";
    public static final String CHANNEL_SET_PROPERTY_FILTER = "/service/wfs/setPropertyFilter";
	public static final String CHANNEL_SET_MAP_LAYER_VISIBILITY = "/service/wfs/setMapLayerVisibility";
	public static final String CHANNEL_HIGHLIGHT_FEATURES = "/service/wfs/highlightFeatures";

	public static final String CHANNEL_DISCONNECT = "/meta/disconnect";
    private Map<String, MapLayerJobProvider> mapLayerJobProviders;

	public static final String CHANNEL_FEATURE_GEOMETRIES = "/wfs/featureGeometries";
	public static final String CHANNEL_DEFAULT_STYLE = "/wfs/defaultStyle";
	public static final String CHANNEL_STATUS = "/wfs/status";

    
    // Error messages    
    public static String ERROR_CONNECTION_NOT_AVAILABLE = "connection_not_available";
    public static String ERROR_CONNECTION_BROKEN = "connection_broken";
    public static String ERROR_NO_PERMISSIONS = "wfs_no_permissions";
    public static String ERROR_CONFIGURATION_FAILED = "wfs_configuring_layer_failed";
    public static String ERROR_REST_CONFIGURATION_FAILED = "arcgis_configuring_layer_failed";
    public static String ERROR_WFS_REQUEST_FAILED = "wfs_request_failed";
    public static String ERROR_REST_REQUEST_FAILED = "arcgis_request_failed";
    public static String ERROR_FEATURE_PARSING = "features_parsing_failed";
    public static String ERROR_WFS_IMAGE_PARSING = "wfs_image_parsing_failed";
    public static String ERROR_REST_IMAGE_PARSING = "arcgis_image_parsing_failed";
    
    public static String JOB_FINISHED_INFO = "job_finished";

    // action user uid API
    private static final String UID_API = "GetCurrentUser";
    private static final String KEY_UID = "currentUserUid";

	// server transport info
	private BayeuxServer bayeux;
	private ServerSession local;

	// JobQueue singleton
	private static JobQueue jobs;

	public static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Constructs TransportService with BayeuxServer instance
	 *
	 * Hooks all channels to processRequest() and creates singletons for JobQueue and JedisManager.
	 * Also initializes Jedis client for this thread.
	 *
	 * @param bayeux
	 */
    public TransportService(BayeuxServer bayeux)
    {
        super(bayeux, "transport");

        Object jsonContext = bayeux.getOption("jsonContext");
        if( jsonContext instanceof JettyJSONContextServer) {

        } else if( jsonContext instanceof JacksonJSONContextServer) {
            // CometD uses older version of Jackson so transport uses 1.x versions for this on purpose
            ObjectMapper transportMapper =  ((JacksonJSONContextServer) jsonContext).getObjectMapper();
            transportMapper.registerModule(new GeometryJSONOutputModule());
        }
        int workerCount = ConversionHelper.getInt(PropertyUtil
                .get("workerCount"), 10);

        mapLayerJobProviders = OskariComponentManager.getComponentsOfType(MapLayerJobProvider.class);
        log.debug("Transport STARTED with worker count", workerCount, "with providers for maplayer job types:", mapLayerJobProviders.keySet() );

        this.bayeux = bayeux;
        this.local = getServerSession();
        //this.jobs = new JobQueue(workerCount);
        this.jobs = new HystrixJobQueue(workerCount);

        // init jedis
        JedisManager.connect(workerCount + 2,
                PropertyUtil.get("redis.hostname"),
                PropertyUtil.getOptional("redis.port", 6379));

        CachingSchemaLocator.init(); // init schemas

        addService(CHANNEL_DISCONNECT, "disconnect");
        addService(CHANNEL_INIT, "processRequest");
        addService(CHANNEL_ADD_MAP_LAYER, "processRequest");
        addService(CHANNEL_REMOVE_MAP_LAYER, "processRequest");
        addService(CHANNEL_SET_LOCATION, "processRequest");
        addService(CHANNEL_SET_MAP_SIZE, "processRequest");
        addService(CHANNEL_SET_MAP_LAYER_STYLE, "processRequest");
        addService(CHANNEL_SET_MAP_LAYER_CUSTOM_STYLE, "processRequest");
        addService(CHANNEL_SET_MAP_CLICK, "processRequest");
        addService(CHANNEL_SET_FILTER, "processRequest");
        addService(CHANNEL_SET_PROPERTY_FILTER, "processRequest");
        addService(CHANNEL_SET_MAP_LAYER_VISIBILITY, "processRequest");
        addService(CHANNEL_HIGHLIGHT_FEATURES, "processRequest");
    }

    public static JobQueue getQueue() {
        return jobs;
    }

    /**
     * Removes Sessions and releases Jedis
     *
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
    	// clear Sessions
    	JedisManager.delAll(SessionStore.KEY);
        log.debug("DESTROYED");
        super.finalize();
    }

    /**
     * Tries to get session from cache with given key or creates a new
     * SessionStore
     *
     * @param client
     * @return session object
     */
    public SessionStore getStore(String client) {
        String json = SessionStore.getCache(client);
        if (json == null) {
            log.debug("Created a new session for user (" + client + ")");
            return new SessionStore(client);
        }
        SessionStore store = null;
        try {
            store = SessionStore.setJSON(json);
        } catch (IOException e) {
            log.error(e, "JSON parsing failed for SessionStore \n" + json);
        }
        if (store == null) {
            return new SessionStore(client);
        }
        return store;
    }

    /**
     * Reset session
     */
    private void save(SessionStore store) {
        if (!store.save()) {
            TransportResultProcessor.send(local, bayeux, store.getClient(), ResultProcessor.CHANNEL_RESET, "reset");
        }
    }
	
	private void saveAndSendErrorIfFailed(SessionStore store) {
        if (!store.save()) {			
			log.warn("Request failed because init parameters are invalid");
			Map<String, Object> output = new HashMap<String, Object>();
            output.put("once", false);
            output.put("message", "parameters_init_invalid");
			TransportResultProcessor.send(local, bayeux, store.getClient(), ResultProcessor.CHANNEL_ERROR, output);
		}	
    }

    /**
     * Removes client's session
     *
     * @param client
     * @param message
     */
    public void disconnect(ServerSession client, Message message)
    {
        String json = SessionStore.getCache(client.getId());
        if(json != null) {
            SessionStore store;
            try {
                store = SessionStore.setJSON(json);
                JedisManager.del(WFSLayerPermissionsStore.KEY + store.getSession());
            } catch (IOException e) {
                log.error(e, "JSON parsing failed for SessionStore \n" + json);
            }
        }
        JedisManager.del(SessionStore.KEY + client.getId());
        JedisManager.delAll(WFSCustomStyleStore.KEY + client.getId());

        // TODO: remove styles from map

    	log.debug("Session & permission deleted: " + client);
    }

    /**
     * Preprocesses every service channel
     *
     * Gets parameters and session and gives processing to a channel specific
     * method.
     *
     * @param client
     * @param message
     */
    public void processRequest(ServerSession client, Message message)
    {
        log.debug("Serving client:", client.getId());
    	Map<String, Object> output = new HashMap<String, Object>();
    	Map<String, Object> params = message.getDataAsMap();
    	String json = message.getJSON();

        if(params == null) {
            log.warn("Request failed because parameters were not set");
            output.put("once", false);
            output.put("message", "parameters_not_set");
            client.deliver(local, ResultProcessor.CHANNEL_ERROR, output, null);
            return;
        }

        // get session
        SessionStore store = getStore(client.getId());

        // channel processing
        String channel = message.getChannel();
        log.debug("Processing request on channel:", channel, "- payload:", json);
        if (channel.equals(CHANNEL_INIT)) {
            processInit(client, store, json);
        } else if (channel.equals(CHANNEL_ADD_MAP_LAYER)) {
            addMapLayer(store, params);
        } else if (channel.equals(CHANNEL_REMOVE_MAP_LAYER)) {
            removeMapLayer(store, params);
        } else if (channel.equals(CHANNEL_HIGHLIGHT_FEATURES)) {
            highlightMapLayerFeatures(store, params);
        } else if (channel.equals(CHANNEL_SET_LOCATION)) {
            setLocation(store, params);
        } else if (channel.equals(CHANNEL_SET_MAP_SIZE)) {
            setMapSize(store, params);
        } else if (channel.equals(CHANNEL_SET_MAP_LAYER_STYLE)) {
            setMapLayerStyle(store, params);
        } else if (channel.equals(CHANNEL_SET_MAP_LAYER_CUSTOM_STYLE)) {
            setMapLayerCustomStyle(store, params);
        } else if (channel.equals(CHANNEL_SET_MAP_CLICK)) {
            setMapClick(store, json, params);
        } else if (channel.equals(CHANNEL_SET_FILTER)) {
            setFilter(store, json, params);
        } else if (channel.equals(CHANNEL_SET_PROPERTY_FILTER)) {
            setPropertyFilter(store, json, params);
        } else if (channel.equals(CHANNEL_SET_MAP_LAYER_VISIBILITY)) {
            setMapLayerVisibility(store, params);
        }
    }

    private long parseRequestId(final Map<String, Object> params) {
        if(params == null) {
            log.debug("parseRequestId - params null");
            return -1;
        }
        final Object obj = params.get(PARAM_REQUEST_ID);
        if(obj instanceof Number) {
            log.debug("parseRequestId - success", obj);
            return ((Number) obj).longValue();
        }
        log.debug("parseRequestId - id not a number", obj);
        return -1;
    }

    /**
     * Parses init's json for session and adds jobs for the selected layers
     *
     * @param client
     * @param store
     * @param json
     */
    public void processInit(ServerSession client, SessionStore store, String json) {
		
		//log.info("Init " + json);
			
        try {
            store = SessionStore.setParamsJSON(json);
        } catch (IOException e) {
            log.error(e, "Session creation failed");
        }
        store.setClient(client.getId());
        store.setUuid(getOskariUid(store));
			
        this.saveAndSendErrorIfFailed(store);			

        // layers
        Map<String, Layer> layers = store.getLayers();
        for (Layer layer : layers.values()) {
            layer.setTiles(store.getGrid().getBounds()); // init bounds to tiles (render all)
        	initMapLayerJob(-1, store, layer.getId(), false);
        }
    }

    /**
     * Adds map layer to session and adds a job for the layer
     *
     * @param store
     * @param layer
     */
    private void addMapLayer(SessionStore store, Map<String, Object> layer) {
        if (!layer.containsKey(PARAM_LAYER_ID)
                || !layer.containsKey(PARAM_LAYER_STYLE)) {
            log.warn("Failed to add a map layer");
    		return;
    	}

    	String layerId = layer.get(PARAM_LAYER_ID).toString();
    	String layerStyle = (String)layer.get(PARAM_LAYER_STYLE);

    	if(!store.containsLayer(layerId)) {
            Layer tmpLayer = new Layer(layerId, layerStyle);
    		store.setLayer(layerId, tmpLayer);    		
        	this.save(store);
    	}
    	
    	store.setSendDefaultStyle(true);
    }

    /**
     * Starts a new job for given layer
     *
     * @param store
     * @param layerId
     */
    private void initMapLayerJob(final long requestId, SessionStore store, String layerId, boolean refresh) {
        initMapLayerJob(requestId, store, layerId, JobType.NORMAL, refresh);
    }

    private void initMapLayerJob(final long requestId, SessionStore store, String layerId, JobType type, boolean refresh) {
        final Job job = createOWSMapLayerJob(
                createResultProcessor(requestId),
                type,
                store,
                layerId,
                refresh);
        jobs.add(job);
    }

    private String getOskariUid(SessionStore store) {
        String sessionId = store.getSession();
        String route = store.getRoute();
        log.warn( JobHelper.getAPIUrl() + UID_API);
        return HttpHelper.getHeaderValue(JobHelper.getAPIUrl() + UID_API,
                JobHelper.getCookiesValue(sessionId, route), KEY_UID);
    }
    /**
     * Removes map layer from session and jobs
     *
     * @param store
     * @param params
     */
    private void removeMapLayer(SessionStore store, Map<String, Object> params) {
        if (!params.containsKey(PARAM_LAYER_ID)) {
            log.warn("Failed to remove a map layer");
            return;
        }
        // Layer id may have prefix
        String layerId = params.get(PARAM_LAYER_ID).toString(); //(Long) layer.get(PARAM_LAYER_ID);
        if (store.containsLayer(layerId)) {
            // first remove from jobs then from store
            Job job = createOWSMapLayerJob(
                    createResultProcessor(parseRequestId(params)),
                    JobType.NORMAL,
                    store,
                    layerId,
                    false);
            jobs.remove(job);

            store.removeLayer(layerId);
            this.save(store);
        }
    }

    /**
     * Sets location into session and starts jobs for selected layers with given
     * location
     *
     * @param store
     * @param params
     */

	private void setLocation(SessionStore store, Map<String, Object> params) {
    	if (params == null ||
                !params.containsKey(PARAM_LAYER_ID) ||
                !params.containsKey(PARAM_LOCATION_SRS) ||
    			!params.containsKey(PARAM_LOCATION_BBOX) ||
    			!params.containsKey(PARAM_LOCATION_ZOOM) ||
    			!params.containsKey(MessageParseHelper.PARAM_GRID) ||
                !params.containsKey(PARAM_TILES)) {
            log.warn("Failed to set location");
    		return;
    	}

        List<Double> bbox = MessageParseHelper.parseBbox(params.get(PARAM_LOCATION_BBOX));

    	Location mapLocation = new Location();
    	mapLocation.setSrs((String)params.get(PARAM_LOCATION_SRS));
    	mapLocation.setBbox(bbox);

    	mapLocation.setZoom(((Number)params.get(PARAM_LOCATION_ZOOM)).longValue());
    	store.setLocation(mapLocation);

    	Grid grid = MessageParseHelper.parseGrid(params);
    	store.setGrid(grid);

    	this.save(store);

        String layerId = params.get(PARAM_LAYER_ID).toString();
        boolean refresh = params.containsKey(JobHelper.PARAM_MANUAL_REFRESH);

        List<List<Double>> tiles = MessageParseHelper.parseBounds(params.get(PARAM_TILES));

        Layer layer = store.getLayers().get(layerId);
        if(layer != null && layer.isVisible()) {
            layer.setTiles(tiles); // selected tiles to render
            initMapLayerJob(parseRequestId(params), store, layerId, refresh);
        }
    }

    /**
     * Sets map size into session and starts jobs for selected layers with given
     * map size if got bigger
     *
     * @param store
     * @param mapSize
     */
    private void setMapSize(SessionStore store, Map<String, Object> mapSize) {
        if (mapSize == null || !mapSize.containsKey(PARAM_WIDTH)
                || !mapSize.containsKey(PARAM_HEIGHT)) {
            log.warn("Failed to set map size");
            return;
        }

        Tile newMapSize = new Tile();
        newMapSize.setWidth(((Number) mapSize.get(PARAM_WIDTH)).intValue());
        newMapSize.setHeight(((Number) mapSize.get(PARAM_HEIGHT)).intValue());
        store.setMapSize(newMapSize);

        this.save(store);
    }

    /**
     * Sets layer style into session and starts job for the layer
     *
     * @param store
     * @param params
     */
    private void setMapLayerStyle(SessionStore store, Map<String, Object> params) {
    	if(!params.containsKey(PARAM_LAYER_ID) || !params.containsKey(PARAM_LAYER_STYLE)) {
            log.warn("Failed to set map layer style");
    		return;
    	}

    	String layerId = params.get(PARAM_LAYER_ID).toString();
    	String layerStyle = (String)params.get(PARAM_LAYER_STYLE);

    	if(store.containsLayer(layerId)) {
            Layer tmpLayer = store.getLayers().get(layerId);

            if(!tmpLayer.getStyleName().equals(layerStyle) || layerStyle.startsWith(WFSImage.PREFIX_CUSTOM_STYLE)) {
                tmpLayer.setStyleName(layerStyle);
                this.save(store);
                if(tmpLayer.isVisible()) {
                    // init bounds to tiles (render all)
                    tmpLayer.setTiles(store.getGrid().getBounds());
                    // only update normal tiles
                    Job job = createOWSMapLayerJob(createResultProcessor(parseRequestId(params)), JobType.NORMAL, store, layerId, false, false, true, false);
                    jobs.add(job);
                }
            }
    	}
    }

    /**
     * Sets layer style into session and starts job for the layer
     *
     * @param store
     * @param style
     */
    private void setMapLayerCustomStyle(SessionStore store, Map<String, Object> style) {
        /*if(!style.containsKey(PARAM_LAYER_ID) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_FILL_COLOR) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_FILL_PATTERN) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_BORDER_COLOR) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_BORDER_LINEJOIN) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_BORDER_DASHARRAY) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_BORDER_WIDTH) ||

                !style.containsKey(WFSCustomStyleStore.PARAM_STROKE_LINECAP) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_STROKE_COLOR) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_STROKE_LINEJOIN) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_STROKE_DASHARRAY) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_STROKE_WIDTH) ||

                !style.containsKey(WFSCustomStyleStore.PARAM_DOT_COLOR) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_DOT_SHAPE) ||
                !style.containsKey(WFSCustomStyleStore.PARAM_DOT_SIZE)) {
            log.warn("Failed to set map layer custom style");
            return;
        }*/

        String layerId = style.get(PARAM_LAYER_ID).toString();
        
        CustomStyleStore customStyle = CustomStyleStoreFactory.createFromJson(layerId, store.getClient(), style);
        customStyle.save();
    }

    /**
     * Click isn't saved in session. Set click will be request just once.
     *
     * Sends only feature json.
     *
     * @param store
     * @param params
     */
    private void setMapClick(SessionStore store, String json, Map<String, Object> params) {
        // functionality change - geojson instead of point coordinate
        GeoJSONFilter filter = GeoJSONFilter.setParamsJSON(json);

        if (filter.getFeatures() == null &&
                (!params.containsKey(PARAM_LONGITUDE) || !params.containsKey(PARAM_LATITUDE))){
            log.warn("Failed to set a map click", params);
            return;
        }

        // stores geojson, but doesn't save
        store.setFilter(filter);

        double longitude;
        double latitude;
        boolean keepPrevious = false;

        if (params.get(PARAM_LONGITUDE) instanceof Double) {
            longitude = (Double) params.get(PARAM_LONGITUDE);
        } else {
            longitude = ((Number) params.get(PARAM_LONGITUDE)).doubleValue();
        }
        if (params.get(PARAM_LATITUDE) instanceof Double) {
            latitude = (Double) params.get(PARAM_LATITUDE);
        } else {
            latitude = ((Number) params.get(PARAM_LATITUDE)).doubleValue();
        }

        if(params.containsKey(PARAM_KEEP_PREVIOUS)) {
            keepPrevious = (Boolean) params.get(PARAM_KEEP_PREVIOUS);
        }

        // stores click, but doesn't save
        store.setMapClick(new Coordinate(longitude, latitude));
        store.setKeepPrevious(keepPrevious);

        for (Entry<String, Layer> e : store.getLayers().entrySet()) {
            if (e.getValue().isVisible()) {
                // job without image drawing
                Job job = createOWSMapLayerJob(createResultProcessor(parseRequestId(params)),
                        JobType.MAP_CLICK,
                        store,
                        e.getValue().getId(), false, true, false, false);
                jobs.add(job);
            }
        }
    }

    /**
     * Filter isn't saved in session. Set filter will be request just once.
     *
     * Sends only feature json.
     *
     * @param store
     * @param json
     */
    private void setFilter(SessionStore store, String json, Map<String, Object> params) {
        GeoJSONFilter filter = GeoJSONFilter.setParamsJSON(json);

        // stores geojson, but doesn't save
        store.setFilter(filter);

        boolean keepPrevious = (Boolean) params.get(PARAM_KEEP_PREVIOUS);
        store.setKeepPrevious(keepPrevious);

        Job job = null;
        for (Entry<String, Layer> e : store.getLayers().entrySet()) {
            if (e.getValue().isVisible()) {
                // job without image drawing
                job = createOWSMapLayerJob(createResultProcessor(parseRequestId(params)), JobType.GEOJSON, store, e.getValue().getId(), false, true, false, false);
                jobs.add(job);
            }
        }
    }
    /**
     * Property filter isn't saved in session. Set filter will be request just once.
     *
     * Sends only feature json.
     *
     * @param store
     * @param json
     */
    private void setPropertyFilter(SessionStore store, String json, Map<String, Object> params) {
        PropertyFilter propertyFilter = PropertyFilter.setParamsJSON(json);
        // stores property filters, but doesn't save
        store.setPropertyFilter(propertyFilter);

        Job job = null;
        for (Entry<String, Layer> e : store.getLayers().entrySet()) {
            if (e.getValue().isVisible()) {
                // job without image drawing
                // only for requested layer
                if (e.getValue().getId().equals(propertyFilter.getLayerId())) {
                    job = createOWSMapLayerJob(createResultProcessor(parseRequestId(params)), JobType.PROPERTY_FILTER, store, e.getValue().getId(), false, true, false, false);
                    jobs.add(job);
                }
            }
        }
    }
    /**
     * Sets layer visibility into session and starts/stops job for the layer
     *
     * @param store
     * @param params
     */
    private void setMapLayerVisibility(SessionStore store,
            Map<String, Object> params) {
        if (!params.containsKey(PARAM_LAYER_ID)
                || !params.containsKey(PARAM_LAYER_VISIBLE)) {
            log.warn("Layer style not defined");
    		return;
    	}

    	String layerId = params.get(PARAM_LAYER_ID).toString();
    	boolean layerVisible = (Boolean)params.get(PARAM_LAYER_VISIBLE);

    	if(store.containsLayer(layerId)) {
    		Layer tmpLayer = store.getLayers().get(layerId);
    		if(tmpLayer.isVisible() != layerVisible) { // only if changed
	    		tmpLayer.setVisible(layerVisible);
	    		this.save(store);
	    		if(layerVisible) {
                    tmpLayer.setTiles(store.getGrid().getBounds()); // init bounds to tiles (render all)
                    initMapLayerJob(parseRequestId(params), store, layerId, false);
	    		}
    		}
    	}
    }

    /**
     * FeatureIds aren't stored in session. Sets highlighted features
     *
     * Sends only image json.
     *
     * @param store
     * @param params
     */
    private void highlightMapLayerFeatures(SessionStore store,
            Map<String, Object> params) {
        if (!params.containsKey(PARAM_LAYER_ID)
                || !params.containsKey(PARAM_FEATURE_IDS)
                || !params.containsKey(PARAM_KEEP_PREVIOUS)
                || !params.containsKey(PARAM_GEOM_REQUEST)) {
            log.warn("Layer features not defined");
    		return;
    	}

    	String layerId = params.get(PARAM_LAYER_ID).toString();
    	List<String> featureIds = new ArrayList<String>();
    	boolean keepPrevious;
        boolean geomRequest;

    	Object[] tmpfids = MessageParseHelper.getArray(params.get(PARAM_FEATURE_IDS));
    	for(Object obj : tmpfids) {
			featureIds.add((String) obj);
    	}

    	keepPrevious = (Boolean)params.get(PARAM_KEEP_PREVIOUS);
    	store.setKeepPrevious(keepPrevious);
        geomRequest = (Boolean) params.get(PARAM_GEOM_REQUEST);
        store.setGeomRequest(geomRequest);

    	if(store.containsLayer(layerId)) {
    		store.getLayers().get(layerId).setHighlightedFeatureIds(featureIds);
    		if(store.getLayers().get(layerId).isVisible()) {
            	// job without feature sending
    			Job job = createOWSMapLayerJob(
                        createResultProcessor(parseRequestId(params)),
                            JobType.HIGHLIGHT,
                            store, layerId, false, false, true, true);
	        	jobs.add(job);
    		}
    	}
    }

    /**
     * Creates a new runnable job with own Jedis instance
     *
     * Parameters define client's service (communication channel), session and
     * layer's id. Sends all resources that the layer configuration allows.
     *
     * @param service
     * @param store
     * @param layerId
     * @return
     */
    public Job createOWSMapLayerJob(ResultProcessor service, JobType type,
            SessionStore store, String layerId, boolean refresh) {

        return createOWSMapLayerJob(service, type, store, layerId, refresh, true, true, true);
    }

    private ResultProcessor createResultProcessor(final long requestId) {
        return new TransportResultProcessor(local, bayeux, requestId);
    }

    /**
     * Creates a new runnable job with own Jedis instance
     *
     * Parameters define client's service (communication channel), session and
     * layer's id. Also sets resources that will be sent if the layer
     * configuration allows.
     *
     * @param service
     * @param store
     * @param layerId
     * @param refresh  if true then manual refresh layer is rendered (SetLocation)
     * @param reqSendFeatures
     * @param reqSendImage
     * @param reqSendHighlight
     */
    public Job createOWSMapLayerJob(ResultProcessor service, JobType type,
            SessionStore store, String layerId, boolean refresh, boolean reqSendFeatures,
            boolean reqSendImage, boolean reqSendHighlight) {
        final WFSLayerStore layer = JobHelper.getLayerConfiguration(layerId, store.getSession(), store.getRoute());

        MapLayerJobProvider provider = null;
        if(layer.getJobType() != null) {
            provider = mapLayerJobProviders.get(layer.getJobType());
        }
        Job job = null;
        if(provider != null) {
            job = provider.createJob(service, type, store, layer,
                    reqSendFeatures, reqSendImage, reqSendHighlight);
        }
        if(job == null) {
            job = new WFSMapLayerJob(service, type, store, layer,
                    reqSendFeatures, reqSendImage, reqSendHighlight);
        }
        // Wfs manual refresh layer is skipped in NORMAL case, if no refresh flag on
        if(type.equals(JobType.NORMAL) && layer.getAttributes().has(JobHelper.PARAM_MANUAL_REFRESH) && !refresh) {
            //Notify started, because of nop  and front status
            ((OWSMapLayerJob) job).notifyStart();
            ((OWSMapLayerJob) job).notifyCompleted(true);
            return null;
        }
        return job;
    }
}
