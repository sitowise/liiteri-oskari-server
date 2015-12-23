package fi.nls.oskari.work;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.pojo.*;
import fi.nls.oskari.transport.TransportService;
import fi.nls.oskari.utils.HttpHelper;
import fi.nls.oskari.wfs.WFSCommunicator;
import fi.nls.oskari.wfs.WFSFilter;
import fi.nls.oskari.wfs.WFSImage;
import fi.nls.oskari.wfs.WFSParser;
import fi.nls.oskari.wfs.extension.UserLayerFilter;

import org.codehaus.jackson.map.ObjectMapper;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import org.opengis.filter.Filter;
import org.opengis.referencing.operation.MathTransform;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.util.*;

/**
 * Job for WFS Map Layer
 */
public class WFSMapLayerJob implements IDetailedMapLayerJob {
	
	private static final Logger log = LogFactory.getLogger(WFSMapLayerJob.class);
    private static final List<List<Object>> EMPTY_LIST = new ArrayList();



    public static final String OUTPUT_LAYER_ID = "layerId";
    public static final String OUTPUT_ONCE = "once";
    public static final String OUTPUT_MESSAGE = "message";
    public static final String OUTPUT_FEATURES = "features";
    public static final String OUTPUT_GEOMETRIES = "geometries";
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
	private WFSLayerPermissionsStore permissions;
	private String layerId;
	private boolean layerPermission;
	private boolean reqSendFeatures;
	private boolean reqSendImage;
    private boolean reqSendHighlight;
	private boolean sendFeatures;
	private boolean sendImage;
    private boolean sendHighlight;
    private MathTransform transformService;
    private MathTransform transformClient;
	private MapLayerJobType type;
	private FeatureCollection<SimpleFeatureType, SimpleFeature> features;
    private List<List<Object>> featureValuesList;
    private List<List<Object>> geomValuesList;
    private List<String> processedFIDs = new ArrayList<String>();
    private WFSImage image = null;
    private Units units = new Units();
    private Job parentJob = null;

    // COOKIE
    public static final String ROUTE_COOKIE_NAME = "ROUTEID=";

	public WFSMapLayerJob() {
    }
	
    public void init(Job parentJob, TransportService service, MapLayerJobType type, SessionStore store, 
			String layerId,
			boolean layerPermission, WFSLayerStore layer,
			boolean reqSendFeatures, boolean reqSendImage, boolean reqSendHighlight) {
    	
    	this.parentJob = parentJob;
    	this.service = service;
        this.type = type;
		this.session = store;
        this.layerId = layerId;
        this.sessionLayer = this.session.getLayers().get(this.layerId);
		this.layer = layer;
		this.layerPermission = layerPermission;
		this.reqSendFeatures = reqSendFeatures;
		this.reqSendImage = reqSendImage;
        this.reqSendHighlight = reqSendHighlight;
        this.transformService = null;
        this.transformClient = null;
    }
    
    private boolean goNext(boolean a) {
    	return this.parentJob.goNext(a);
    }
    
    private boolean goNext() {
    	return this.parentJob.goNext();
    }
    
    public String getKey() {
		return this.parentJob.getKey();
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
     * Makes request
     *
     * @param type
     * @param layer
     * @param session
     * @param bounds
     * @param transformService
     * @return response
     */
    public static BufferedReader request(MapLayerJobType type, WFSLayerStore layer, SessionStore session, List<Double> bounds, MathTransform transformService) {
        BufferedReader response = null;
        if(layer.getTemplateType() == null) { // default
            String payload = WFSCommunicator.createRequestPayload(type, layer, session, bounds, transformService);
            log.debug("Request data\n", layer.getURL(), "\n", payload);
            response = HttpHelper.postRequestReader(layer.getURL(), "", payload, layer.getUsername(), layer.getPassword());
        } else {
            log.warn("Failed to make a request because of undefined layer type", layer.getTemplateType());
        }

        return response;
    }

    /**
     * Parses response to features
     *
     * @param layer
     * @param response
     * @return features
     */
    public static FeatureCollection<SimpleFeatureType, SimpleFeature> response(WFSLayerStore layer, BufferedReader response) {
        FeatureCollection<SimpleFeatureType, SimpleFeature> features;

        if(layer.isCustomParser()) {
            log.debug("Custom parser layer id: ", layer.getLayerId());
            WFSParser parser = new WFSParser(response, layer);
            features = parser.parse();
        } else {
            features = WFSCommunicator.parseSimpleFeatures(response, layer);
        }

        return features;
    }

    /**
	 * Process of the job
	 * 
	 * Worker calls this when starts the job.
	 *
	 */
	@Override
	public final void run() {

		setResourceSending();

		if(!validateMapScales()) {
            log.debug("Map scale was not valid for layer",  this.layerId);
			return;
		}

        // if different SRS, create transforms for geometries
        if(!this.session.getLocation().getSrs().equals(this.layer.getSRSName())) {
            this.transformService = this.session.getLocation().getTransformForService(this.layer.getCrs(), true);
            this.transformClient = this.session.getLocation().getTransformForClient(this.layer.getCrs(), true);
        }

        String cacheStyleName = this.session.getLayers().get(this.layerId).getStyleName();
        if(cacheStyleName.startsWith(WFSImage.PREFIX_CUSTOM_STYLE)) {
            cacheStyleName += "_" + this.session.getSession();
        }

        // init enlarged envelope
        List<List<Double>> grid = this.session.getGrid().getBounds();
        if(grid.size() > 0) {
            this.session.getLocation().setEnlargedEnvelope(grid.get(0));
        }

        if(!goNext()) return;

        log.debug(this.type);

        if(this.type == MapLayerJobType.NORMAL) { // tiles for grid
            if(!this.layer.isTileRequest()) { // make single request
                if(!this.normalHandlers(null, true)) {
                    return;
                }
            }

            log.debug("normal tile images handling");

            boolean first = true;
			int index = 0;
			for(List<Double> bounds : grid) {
                if(this.layer.isTileRequest()) { // make a request per tile
                    if(!this.normalHandlers(bounds, first)) {
                        return;
                    }
                }

				if(!goNext()) return;
				
				if(this.sendImage && this.sessionLayer.isTile(bounds)) { // check if needed tile
		   	 		Double[] bbox = new Double[4];
		   	 		for (int i = 0; i < bbox.length; i++) {
			   	 		bbox[i] = bounds.get(i);
		   	 		}
		   	 		
					// get from cache
				    BufferedImage bufferedImage = getImageCache(bbox);
			    	boolean fromCache = (bufferedImage != null);
                    boolean isboundaryTile = this.session.getGrid().isBoundsOnBoundary(index);

			    	if(!fromCache) {
                        if(this.image == null) {
                            this.image = new WFSImage(this.layer,
                                    this.session.getClient(),
                                    this.session.getLayers().get(this.layerId).getStyleName(),
                                    null);
                        }
					    bufferedImage = this.image.draw(this.session.getTileSize(),
                                this.session.getLocation(),
                                bounds,
                                this.features);
                        if(bufferedImage == null) {
                            this.imageParsingFailed();
                            return;
                        }

					    // set to cache
						if(!isboundaryTile) {
                            setImageCache(bufferedImage, cacheStyleName, bbox, true);
						} else { // non-persistent cache - for ie
                            setImageCache(bufferedImage, cacheStyleName, bbox, false);
						}
					}

		   	 		String url = createImageURL(this.session.getLayers().get(this.layerId).getStyleName(), bbox);
					this.sendWFSImage(url, bufferedImage, bbox, true, isboundaryTile);
				}

				if(first) {
					first = false;
					this.session.setKeepPrevious(true); // keep the next tiles
				}
				index++;
			}
		} else if(this.type == MapLayerJobType.HIGHLIGHT) {
            if(this.sendHighlight) {
                if(!this.requestHandler(null)) {
                    return;
                }
                this.featuresHandler();
                if(!goNext()) return;
                
             // Send geometries, if requested as well
                if(this.session.isGeomRequest()){
                	this.sendWFSFeatureGeometries(this.geomValuesList, TransportService.CHANNEL_FEATURE_GEOMETRIES);
                }

                log.debug("highlight image handling", this.features.size());

                // IMAGE HANDLING
                log.debug("sending");
                Location location = this.session.getLocation();
                if(this.image == null) {
                    this.image = new WFSImage(this.layer,
                            this.session.getClient(),
                            this.session.getLayers().get(this.layerId).getStyleName(),
                            MapLayerJobType.HIGHLIGHT.toString());
                }
                BufferedImage bufferedImage = this.image.draw(this.session.getMapSize(),
                        location,
                        this.features);
                if(bufferedImage == null) {
                    this.imageParsingFailed();
                    return;
                }

                Double[] bbox = location.getBboxArray();

                // cache (non-persistant)
                setImageCache(bufferedImage, MapLayerJobType.HIGHLIGHT.toString() + "_" + this.session.getSession(), bbox, false);

                String url = createImageURL(MapLayerJobType.HIGHLIGHT.toString(), bbox);
                this.sendWFSImage(url, bufferedImage, bbox, false, false);
            }
        } else if(this.type == MapLayerJobType.MAP_CLICK) {
            if(!this.requestHandler(null)) {
                this.sendWFSFeatures(EMPTY_LIST, TransportService.CHANNEL_MAP_CLICK);
                return;
            }
            this.featuresHandler();
            if(!goNext()) return;
            if(this.sendFeatures) {
                log.debug("Feature values list", this.featureValuesList);
                this.sendWFSFeatures(this.featureValuesList, TransportService.CHANNEL_MAP_CLICK);
            } else {
                log.debug("No feature data!");
                this.sendWFSFeatures(EMPTY_LIST, TransportService.CHANNEL_MAP_CLICK);
            }
        } else if(this.type == MapLayerJobType.GEOJSON) {
            if(!this.requestHandler(null)) {
                return;
            }
            this.featuresHandler();
            if(!goNext()) return;
            if(this.sendFeatures) {
                this.sendWFSFeatures(this.featureValuesList, TransportService.CHANNEL_FILTER);
            }
        } else {
            log.error("Type is not handled", this.type);
        }

        log.debug(PROCESS_ENDED, getKey());
	}

    /**
     * Wrapper for normal type job's handlers
     */
    private boolean normalHandlers(List<Double> bounds, boolean first) {
        if(!this.requestHandler(bounds)) {
            log.debug("Cancelled by request handler");
            return false;
        }
        if(first) {
            propertiesHandler();
            if(!goNext()) return false;
        }
        if(!goNext()) return false;
        this.featuresHandler();
        if(!goNext()) return false;
        return true;
    }

	/**
	 * Makes request and parses response to features
	 *
     * @param bounds
	 * @return <code>true</code> if thread should continue; <code>false</code>
	 *         otherwise.
	 */
    private boolean requestHandler(List<Double> bounds) {

        // make a request
        BufferedReader response = request(type, layer, session, bounds, transformService);

        Map<String, Object> output = new HashMap<String, Object>();
        try {

        // request failed
		if(response == null) {
            log.warn("Request failed for layer", layer.getLayerId());
	   	 	output.put(OUTPUT_LAYER_ID, layer.getLayerId());
	   	 	output.put(OUTPUT_ONCE, true);
	   	 	output.put(OUTPUT_MESSAGE, TransportService.ERROR_WFS_REQUEST_FAILED);
	    	this.service.send(session.getClient(), TransportService.CHANNEL_ERROR, output);
	        log.debug(PROCESS_ENDED, getKey());
			return false;
		}

		// parse response
        this.features = response(layer, response);

		// parsing failed
		if(this.features == null) {
            log.warn("Parsing failed for layer",  this.layerId);
	   	 	output.put(OUTPUT_LAYER_ID, this.layerId);
	   	 	output.put(OUTPUT_ONCE, true);
	   	 	output.put(OUTPUT_MESSAGE, TransportService.ERROR_FEATURE_PARSING);
	    	this.service.send(session.getClient(), TransportService.CHANNEL_ERROR, output);
	        log.debug(PROCESS_ENDED, getKey());
			return false;
		}

        // 0 features found - send size
        if(this.type == MapLayerJobType.MAP_CLICK && this.features.size() == 0) {
            log.debug("Empty result for map click",  this.layerId);
            output.put(OUTPUT_LAYER_ID, this.layerId);
            output.put(OUTPUT_FEATURES, "empty");
            output.put(OUTPUT_KEEP_PREVIOUS, this.session.isKeepPrevious());
            this.service.send(session.getClient(), TransportService.CHANNEL_MAP_CLICK, output);
            log.debug(PROCESS_ENDED, getKey());
            return false;
        } else if(this.type == MapLayerJobType.GEOJSON && this.features.size() == 0) {
            log.debug("Empty result for filter",  this.layerId);
            output.put(OUTPUT_LAYER_ID, this.layerId);
            output.put(OUTPUT_FEATURES, "empty");
            this.service.send(session.getClient(), TransportService.CHANNEL_FILTER, output);
            log.debug(PROCESS_ENDED, getKey());
            return false;
        } else {
            if(this.features.size() == 0) {
                log.debug("Empty result",  this.layerId);
                output.put(OUTPUT_LAYER_ID, this.layerId);
                output.put(OUTPUT_FEATURE, "empty");
                this.service.send(session.getClient(), TransportService.CHANNEL_FEATURE, output);
                log.debug(PROCESS_ENDED, getKey());
                return false;
            } else if(this.features.size() == layer.getMaxFeatures()) {
                log.debug("Max feature result",  this.layerId);
                output.put(OUTPUT_LAYER_ID, this.layerId);
                output.put(OUTPUT_FEATURE, "max");
                this.service.send(session.getClient(), TransportService.CHANNEL_FEATURE, output);
            }
        }

        log.debug("Features count", this.features.size());
        }
        catch (Exception ee)
        {
            log.error("Unhandled exception during request: ", ee);
	   	 	output.put(OUTPUT_LAYER_ID, this.layerId);
	   	 	output.put(OUTPUT_ONCE, true);
	   	 	output.put(OUTPUT_MESSAGE, TransportService.ERROR_WFS_REQUEST_FAILED);
	    	this.service.send(session.getClient(), TransportService.CHANNEL_ERROR, output);
	        log.debug(PROCESS_ENDED, getKey());
            return false;
        }

        return true;
    }

    /**
     * Parses features properties and sends to appropriate channels
     */
    private void propertiesHandler() {
        if(!this.sendFeatures) {
            return;
        }

        log.debug("properties handler");

        List<String> selectedProperties = new ArrayList<String>();
        List<String> layerSelectedProperties = layer.getSelectedFeatureParams(session.getLanguage());

        // selected props
        if(layerSelectedProperties != null && layerSelectedProperties.size() != 0) {
            selectedProperties.addAll(this.layer.getSelectedFeatureParams(this.session.getLanguage()));
        } else { // all properties
            for(Property prop : this.features.features().next().getProperties()) {
                String field = prop.getName().toString();
                if(!this.layer.getGMLGeometryProperty().equals(field)) { // don't add geometry
                    selectedProperties.add(field);
                }
            }
        }

        this.sendWFSProperties(selectedProperties, this.layer.getFeatureParamsLocales(this.session.getLanguage()));
    }

    /**
     * Parses features values
     */
    private void featuresHandler() {
        log.debug("features handler");

        // create filter of screen area
        Filter screenBBOXFilter = WFSFilter.initBBOXFilter(this.session.getLocation(), this.layer);

        // send feature info
        FeatureIterator<SimpleFeature> featuresIter =  this.features.features();

        this.featureValuesList = new ArrayList<List<Object>>();
        this.geomValuesList = new ArrayList<List<Object>>();
        while(goNext(featuresIter.hasNext())) {
            SimpleFeature feature = featuresIter.next();

            // if is not in shown area -> skip
            if(!screenBBOXFilter.evaluate(feature)) {
                //log.debug("Not selected");
                continue;
            }

            List<Object> values = new ArrayList<Object>();

            String fid = feature.getIdentifier().getID();
            if (!this.processedFIDs.contains(fid)) {
                // __fid value
                values.add(fid);
                this.processedFIDs.add(fid);

                // get feature geometry (transform if needed) and get geometry center
                Geometry geometry = WFSParser.getFeatureGeometry(feature, this.layer.getGMLGeometryProperty(), this.transformClient);

                // Add geometry property, if requested
                if (this.session.isGeomRequest())
                {
                	List<Object> gvalues = new ArrayList<Object>();
                	gvalues.add(fid);
                	gvalues.add(geometry); //feature.getAttribute(this.layer.getGMLGeometryProperty()));
                	this.geomValuesList.add(gvalues);
                }
                
                // send values
                if(this.sendFeatures) {
                    Point centerPoint = WFSParser.getGeometryCenter(geometry);

                    // selected values
                    List<String> selectedProperties = layer.getSelectedFeatureParams(session.getLanguage());
                    if(selectedProperties != null && selectedProperties.size() != 0) {
                        for(String attr : selectedProperties) {
                            if (this.layer.getLayerId().startsWith(UserLayerFilter.USERLAYER_PREFIX)) {
                                Object prop = feature.getAttribute(attr);
                                try {
                                    HashMap<String, Object> propMap = new ObjectMapper().readValue(prop.toString(), HashMap.class);
                                    values.add(propMap);
                                } catch(Exception e) {
                                    values.add(prop);
                                }
                            } else {
                                values.add(feature.getAttribute(attr));
                            }
                        }
                    } else { // all values
                        for(Property prop : this.features.features().next().getProperties()) {
                            String field = prop.getName().toString();
                            if(!this.layer.getGMLGeometryProperty().equals(field)) { // don't add geometry
                                values.add(feature.getAttribute(field));
                            }
                        }
                    }

                    // center position (must be in properties also)
                    if(centerPoint != null) {
                        values.add(centerPoint.getX());
                        values.add(centerPoint.getY());
                    } else {
                        values.add(null);
                        values.add(null);
                    }

                    WFSParser.parseValuesForJSON(values);

                    if(this.type == MapLayerJobType.NORMAL) {
                        this.sendWFSFeature(values);
                    } else {
                        this.featureValuesList.add(values);
                    }
                }
            } else {
                log.warn("Found duplicate feature ID", fid);
            }
        }
	}

    /**
     * Gets image from cache
     *
     * @param bbox
     */
    private BufferedImage getImageCache(Double[] bbox) {
        return WFSImage.getCache(
                this.layerId,
                this.session.getLayers().get(this.layerId).getStyleName(),
                this.session.getLocation().getSrs(),
                bbox,
                this.session.getLocation().getZoom()
        );
    }

    /**
     * Sets image to cache
     *
     * @param bufferedImage
     * @param style
     * @param bbox
     * @param persistent
     */
    private void setImageCache(BufferedImage bufferedImage, final String style, Double[] bbox, boolean persistent) {
        WFSImage.setCache(
                bufferedImage,
                this.layerId,
                style,
                this.session.getLocation().getSrs(),
                bbox,
                this.session.getLocation().getZoom(),
                persistent
        );
    }

    /**
     * Send image parsing error
     */
    private void imageParsingFailed() {
        log.error("Image parsing failed");
        Map<String, Object> output = new HashMap<String, Object>();
        output.put(OUTPUT_LAYER_ID, this.layerId);
        output.put(OUTPUT_ONCE, true);
        output.put(OUTPUT_MESSAGE, TransportService.ERROR_WFS_IMAGE_PARSING);
        this.service.send(session.getClient(), TransportService.CHANNEL_ERROR, output);
    }

	/**
	 * Sets which resources will be sent (features, image)
	 */
	private void setResourceSending() {
		// layer configuration is the default
		this.sendFeatures = layer.isGetFeatureInfo();
		this.sendImage = layer.isGetMapTiles();
        this.sendHighlight = layer.isGetHighlightImage();

		// if request defines false and layer configuration allows
		if(!this.reqSendFeatures && this.sendFeatures)
			this.sendFeatures = false;
		if(!this.reqSendImage && this.sendImage)
			this.sendImage = false;
        if(!this.reqSendHighlight && this.sendHighlight)
            this.sendHighlight = false;

        log.debug("send - features:", this.sendFeatures, "image:", this.sendImage, "highlight:", this.sendHighlight);
	}

	/**
	 * Checks if the map scale is valid
	 * 
	 * @return <code>true</code> if map scale is valid; <code>false</code>
	 *         otherwise.
	 */
	private boolean validateMapScales() {
		double scale = this.session.getMapScales().get((int)this.session.getLocation().getZoom());
        double minScaleInMapSrs = units.getScaleInSrs(layer.getMinScale(), layer.getSRSName(), session.getLocation().getSrs());
        double maxScaleInMapSrs = units.getScaleInSrs(layer.getMaxScale(), layer.getSRSName(), session.getLocation().getSrs());

		log.debug("Scale in:", layer.getSRSName(), scale, "[", layer.getMaxScale(), ",", layer.getMinScale(), "]");
        log.debug("Scale in:", session.getLocation().getSrs(), scale, "[", maxScaleInMapSrs, ",", minScaleInMapSrs, "]");
		if(minScaleInMapSrs >= scale && maxScaleInMapSrs <= scale) // min == biggest value
			return true;
		return false;
	}

    /**
     * Creates image url
     *
     * @param style
     * @param bbox
     */
    private String createImageURL(final String style, Double[] bbox) {
        return "/image" +
                "?" + OUTPUT_LAYER_ID + "=" + this.layerId +
                "&" + OUTPUT_STYLE + "=" + style +
                "&" + OUTPUT_IMAGE_SRS + "=" + this.session.getLocation().getSrs() +
                "&" + OUTPUT_IMAGE_BBOX + "=" + bbox[0] +
                "," + bbox[1] +
                "," + bbox[2] +
                "," + bbox[3] +
                "&" + OUTPUT_IMAGE_ZOOM + "=" + this.session.getLocation().getZoom();
    }

    /**
     * Sends properties (fields and locales)
     * 
     * @param fields
     * @param locales
     */
    private void sendWFSProperties(List<String> fields, List<String> locales) {    
    	if(fields == null || fields.size() == 0) {
            log.warn("Failed to send properties");
    		return;
    	}

    	fields.add(0, "__fid");
    	fields.add("__centerX");
    	fields.add("__centerY");

    	if(locales != null) {
        	locales.add(0, "ID");	
        	locales.add("x");
        	locales.add("y");
    	} else {
    		locales = new ArrayList<String>();
    	}
    	
    	Map<String, Object> output = new HashMap<String, Object>();
   	 	output.put(OUTPUT_LAYER_ID, this.layerId);
   	 	output.put(OUTPUT_FIELDS, fields);
   	 	output.put(OUTPUT_LOCALES, locales);

    	this.service.send(this.session.getClient(), TransportService.CHANNEL_PROPERTIES, output);
    }
    
    /**
     * Sends one feature
     * 
     * @param values
     */
    private void sendWFSFeature(List<Object> values) {    	
    	if(values == null || values.size() == 0) {
            log.warn("Failed to send feature");
    		return;   	
    	}
    	
    	Map<String, Object> output = new HashMap<String, Object>();
   	 	output.put(OUTPUT_LAYER_ID, this.layerId);
   	 	output.put(OUTPUT_FEATURE, values);

    	this.service.send(this.session.getClient(), TransportService.CHANNEL_FEATURE, output);
    }

    /**
     * Sends list of features
     * 
     * @param features
     * @param channel
     */
    private void sendWFSFeatures(List<List<Object>> features, String channel) {
    	if(features == null || features.size() == 0) {
            log.warn("Failed to send features");
    		return;   	
    	}
    	
    	Map<String, Object> output = new HashMap<String, Object>();
   	 	output.put(OUTPUT_LAYER_ID, this.layerId);
   	 	output.put(OUTPUT_FEATURES, features);
   	 	if(channel.equals(TransportService.CHANNEL_MAP_CLICK)) {
   	 		output.put(OUTPUT_KEEP_PREVIOUS, this.session.isKeepPrevious());
   	 	}

    	this.service.send(this.session.getClient(), channel, output);
    }
    
    /**
     * Sends list of feature geometries
     *
     * @param geometries
     * @param channel
     */
    private void sendWFSFeatureGeometries(List<List<Object>> geometries, String channel) {
    	if(geometries == null || geometries.size() == 0) {
    		log.warn("Failed to send feature geometries");
    		return;
    	}
    	Map<String, Object> output = new HashMap<String, Object>();
    	output.put(OUTPUT_LAYER_ID, this.layerId);
    	output.put(OUTPUT_GEOMETRIES, geometries);
    	output.put(OUTPUT_KEEP_PREVIOUS, this.session.isKeepPrevious());

    	this.service.send(this.session.getClient(), channel, output);
    }

    /**
     * Sends image as an URL to IE 8 & 9, base64 data for others
     *
     * @param url
     * @param bufferedImage
     * @param bbox
     * @param isTiled
     */
    private void sendWFSImage(String url, BufferedImage bufferedImage, Double[] bbox, boolean isTiled, boolean isboundaryTile) {
    	if(bufferedImage == null) {
            log.warn("Failed to send image");
    		return;
    	}
        
    	Map<String, Object> output = new HashMap<String, Object>();
   	 	output.put(OUTPUT_LAYER_ID, this.layerId);

   	 	Location location = this.session.getLocation();

   	 	Tile tileSize = null;
   	 	if(isTiled) {
   	 		tileSize = this.session.getTileSize();
   	 	} else {
   	 		tileSize = this.session.getMapSize();
   	 	}

        output.put(OUTPUT_IMAGE_SRS, location.getSrs());
   	 	output.put(OUTPUT_IMAGE_BBOX, bbox);
   	 	output.put(OUTPUT_IMAGE_ZOOM, location.getZoom());
   	 	output.put(OUTPUT_IMAGE_TYPE, this.type); // "normal" | "highlight"
   	 	output.put(OUTPUT_KEEP_PREVIOUS, this.session.isKeepPrevious());
        output.put(OUTPUT_BOUNDARY_TILE, isboundaryTile);
   	 	output.put(OUTPUT_IMAGE_WIDTH, tileSize.getWidth());
   	 	output.put(OUTPUT_IMAGE_HEIGHT, tileSize.getHeight());
   	 	output.put(OUTPUT_IMAGE_URL, url);

        byte[] byteImage = WFSImage.imageToBytes(bufferedImage);
        String base64Image = WFSImage.bytesToBase64(byteImage);
        int base64Size = (base64Image.length()*2)/1024;

        // IE6 & IE7 doesn't support base64, max size in base64 for IE8 is 32KB
   	 	if(!(this.session.getBrowser().equals(BROWSER_MSIE) && this.session.getBrowserVersion() < 8 ||
   	 			this.session.getBrowser().equals(BROWSER_MSIE) && this.session.getBrowserVersion() == 8 &&
   	 			base64Size >= 32)) {
       	 	output.put(OUTPUT_IMAGE_DATA, base64Image);
    	}

    	this.service.send(this.session.getClient(), TransportService.CHANNEL_IMAGE, output);
    }
}