package fi.nls.oskari.work;

import fi.nls.oskari.arcgis.ArcGisCommunicator;
import fi.nls.oskari.arcgis.ArcGisFilter;
import fi.nls.oskari.arcgis.ArcGisImage;
import fi.nls.oskari.arcgis.ArcGisTokenService;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.pojo.*;
import fi.nls.oskari.pojo.style.CustomStyleStore;
import fi.nls.oskari.transport.TransportService;
import fi.nls.oskari.utils.HttpHelper;
import fi.nls.oskari.wfs.WFSFilter;
import fi.nls.oskari.wfs.WFSParser;

import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.OperatorExportToWkt;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.json.simple.JSONValue;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.InternationalString;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Job for WFS Map Layer
 */
public class ArcGisMapLayerJob implements IDetailedMapLayerJob {
	
	private static final Logger log = LogFactory.getLogger(ArcGisMapLayerJob.class);
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
	TransportService service;
	private SessionStore session;
    private Layer sessionLayer;
	private WFSLayerStore layer;
	private ArcGisLayerStore arcGisLayer;
	private ArcGisLayerStore arcGisLayerScaled;
	private List<ArcGisLayerStore> arcGisLayers = new ArrayList<ArcGisLayerStore>();	
	
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
	private ArrayList<ArcGisFeature> features;
    private List<List<Object>> featureValuesList;
    private List<List<Object>> geomValuesList;
    private List<String> processedFIDs = new ArrayList<String>();
    private ArcGisImage image = null;
    private Units units = new Units();
    private Job parentJob = null;
    private String token = null;

    // COOKIE
    public static final String ROUTE_COOKIE_NAME = "ROUTEID=";
    
    public ArcGisMapLayerJob() {}

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

    public static BufferedReader sendQueryRequest(MapLayerJobType type, 
    		WFSLayerStore layer, ArcGisLayerStore arcGisLayer, 
    		SessionStore session, List<Double> bounds, 
    		String token) {
        BufferedReader response = null;
        if(layer.getTemplateType() == null) { // default
            String payload = ArcGisCommunicator.createQueryRequestPayload(type, layer, session, bounds, token);              
            String url = layer.getURL() + "/" + arcGisLayer.getId() + "/query?";
            log.debug("Request data\n", url, "\n", payload);                
            response = HttpHelper.postRequestReader(url, "application/x-www-form-urlencoded", payload, layer.getUsername(), layer.getPassword());
        } else {
            log.warn("Failed to make a request because of undefined layer type", layer.getTemplateType());
        }

        return response;
    }   
    
    private BufferedReader sendIdentifyRequest(WFSLayerStore layer, List<ArcGisLayerStore> layers, 
    		SessionStore session, List<Double> bounds, 
    		String token) {
    	BufferedReader response = null;
    	
    	String payload = ArcGisCommunicator.createIdentifyRequestPayload(layers, session, bounds, token);
    	String url = layer.getURL() + "/identify?";
    	log.debug("Request data\n", url, "\n", payload);
    	response = HttpHelper.getRequestReader(url + payload, "", layer.getUsername(), layer.getPassword());
    	
    	return response;
    }
    
    public static ArcGisLayerStore getArcGisLayerConfiguration(String layerId, WFSLayerStore layer, String token) {
    	log.info("Getting configuration for layer", layerId);
    	ArcGisLayerStore result = null;
        String json = ArcGisLayerStore.getCache(layerId);
        boolean fromCache = (json != null);
        if(!fromCache) {
            log.info("Getting configuration from server for layer", layerId);                        
            result = loadArcGisLayerConfigurationFromServer(layerId, layer, token);
                    
            if(result == null) {
            	return null;	
            }
            else {
            	result.save();
            	return result;
            }
        }
        else {
        	try {
                return ArcGisLayerStore.setJSON(layerId, json);                
            } catch (Exception e) {
                log.error(e, "JSON parsing failed for WFSLayerStore \n" + json);
            }
        }        

        return null;
    }
    
    private static ArcGisLayerStore loadArcGisLayerConfigurationFromServer(String layerId, WFSLayerStore layer, String token) {
    	ArcGisLayerStore result = null;    	               
        String json = loadLayerConfig(layer.getURL(), layer.getLayerName(), token);
        
        try {
            result = ArcGisLayerStore.setJSON(layerId, json);                
        } catch (Exception e) {
            log.error(e, "JSON parsing failed for WFSLayerStore \n" + json);
        }
        
        if (result != null && result.getType().equals("Group Layer")) {
        	ArrayList<ArcGisLayerStore> subLayers = new ArrayList<ArcGisLayerStore>();
        	for (String subLayerId : result.getSubLayerIds()) {
        		String subLayerJson = loadLayerConfig(layer.getURL(), subLayerId, token);
        		ArcGisLayerStore subLayerStore = null;
        		try {
					subLayerStore = ArcGisLayerStore.setJSON(layerId, subLayerJson);
				} catch (IOException e) {
					log.error(e, "JSON parsing failed for Sub ArcGisLayerStore \n" + json);
				}
        		subLayers.add(subLayerStore);
			}
        	
        	result.setSubLayers(subLayers);
        }
        
        return result;
    }
    
    private static String loadLayerConfig(String layerUrl, String layerId, String token) {
        String url = layerUrl + "/" + layerId + "?f=json&token=" + token;        
        return HttpHelper.getRequest(url, null);
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
		
		log.debug("type:", this.type, "features:", this.sendFeatures, "image:", this.sendImage, "highlight:", this.sendHighlight);

		if(!validateMapScales()) {
            log.debug("Map scale was not valid for layer",  this.layerId);
			return;
		}

        // if different SRS, create transforms for geometries
        if(!this.session.getLocation().getSrs().equals(this.layer.getSRSName())) {
        	log.info("Different SRS. Map:", this.session.getLocation().getSrs(), "Layer:", this.layer.getSRSName());
            this.transformService = this.session.getLocation().getTransformForService(this.layer.getCrs(), true);
            this.transformClient = this.session.getLocation().getTransformForClient(this.layer.getCrs(), true);
        }

        String cacheStyleName = this.session.getLayers().get(this.layerId).getStyleName();
        if(cacheStyleName.startsWith(ArcGisImage.PREFIX_CUSTOM_STYLE)) {
            cacheStyleName += "_" + this.session.getSession();
        }

        // init enlarged envelope
        List<List<Double>> grid = this.session.getGrid().getBounds();
        if(grid.size() > 0) {
            this.session.getLocation().setEnlargedEnvelope(grid.get(0));
        }

        if(!goNext()) return;
        
        this.token = ArcGisTokenService.getInstance().getTokenForLayer(this.layer.getURL());
        
    	this.arcGisLayer = getArcGisLayerConfiguration(this.layerId, this.layer, this.token);
        if(this.arcGisLayer == null) {
            log.warn("Layer (" +  this.layerId + ") arcgis configurations couldn't be fetched");
            Map<String, Object> output = new HashMap<String, Object>();
            output.put(OUTPUT_LAYER_ID, this.layerId);
            output.put(OUTPUT_ONCE, true);
            output.put(OUTPUT_MESSAGE, TransportService.ERROR_REST_CONFIGURATION_FAILED);
            this.service.send(session.getClient(), TransportService.CHANNEL_ERROR, output);
            return;
        }
        
        if (this.session.isSendDefaultStyle() && this.arcGisLayer.hasDefaultStyle()) {
        	this.sendStyleConfiguration(this.arcGisLayer.getAggregatedDefaultStyle());
        }
        
        this.arcGisLayers = getArcGisLayersDependingOnScale();
        this.arcGisLayerScaled = this.arcGisLayers.get(0);
        
        if(this.type == MapLayerJobType.NORMAL) { // tiles for grid        	
        	
            if(!this.layer.isTileRequest()) { // make single request
                if(!this.normalHandlers(null, true)) {
                	log.warn("Canceling single request", layer.getLayerId());
                    return;
                }
            }
            
            log.info("Normal images handling for layer", layer.getLayerId());

            boolean first = true;
			int index = 0;
			for(List<Double> bounds : grid) {
                if(this.layer.isTileRequest()) { // make a request per tile
                    if(!this.normalHandlers(bounds, first)) {
                    	log.warn("Canceling tile request", layer.getLayerId());
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
                            this.image = new ArcGisImage(this.layer,
                            		this.arcGisLayer,
                            		this.arcGisLayers,
                                    this.session.getClient(),
                                    this.session.getLayers().get(this.layerId).getStyleName(),
                                    null,
                                    this.token);
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

                Location location = this.session.getLocation();
                if(this.image == null) {
                    this.image = new ArcGisImage(this.layer,
                    		this.arcGisLayer,
                    		this.arcGisLayers,
                            this.session.getClient(),
                            this.session.getLayers().get(this.layerId).getStyleName(),
                            MapLayerJobType.HIGHLIGHT.toString(),
                            this.token);
                }
                BufferedImage bufferedImage = null;
                bufferedImage = this.image.draw(this.session.getMapSize(),
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
	}

    private List<ArcGisLayerStore> getArcGisLayersDependingOnScale() {    		    
    	List<ArcGisLayerStore> result = new ArrayList<ArcGisLayerStore>();
		List<ArcGisLayerStore> subLayers = this.arcGisLayer.getSubLayers();		
		
		if (subLayers != null && subLayers.size() > 0) {
			double scale = this.session.getMapScales().get((int)this.session.getLocation().getZoom());
			
			for (ArcGisLayerStore subLayer : subLayers) {
				double minScale = subLayer.getMinScale();
				double maxScale = subLayer.getMaxScale();
				
				if (maxScale <= scale && scale <= minScale) {
					result.add(subLayer);
					log.info("Adding other arcgis layer id", this.arcGisLayer.getId(), "=>", this.arcGisLayer.getId());					
				}
			}
		}
		
		if (result.size() == 0)
			result.add(this.arcGisLayer);
		
		return result;
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

    	log.debug("Starting request handler");
    	
//    	BufferedReader response;    	
//    	if (this.type == MapLayerJobType.MAP_CLICK) {
//    		response = sendIdentifyRequest(this.layer, this.arcGisLayers, this.session, bounds, this.token);
//    	}
//    	else {
//    		response = sendQueryRequest(this.type, this.layer, this.arcGisLayerScaled, this.session, bounds, this.token);
    	//}
    	
    	Map<String, Object> output = new HashMap<String, Object>();
    	
    	List<BufferedReader> responses = new ArrayList<BufferedReader>();
    	for (ArcGisLayerStore subLayer : this.arcGisLayers) {
    		BufferedReader response = sendQueryRequest(this.type, this.layer, subLayer, this.session, bounds, this.token);
            // request failed
    		if(response == null) {
                log.warn("Request failed for layer", layer.getLayerId());
    	   	 	output.put(OUTPUT_LAYER_ID, layer.getLayerId());
    	   	 	output.put(OUTPUT_ONCE, true);
    	   	 	output.put(OUTPUT_MESSAGE, TransportService.ERROR_REST_REQUEST_FAILED);
    	    	this.service.send(session.getClient(), TransportService.CHANNEL_ERROR, output);
    	        log.debug(PROCESS_ENDED, getKey());
    			return false;
    		}
    		responses.add(response);
		}

        
        try {

		// parse response
        this.features = ArcGisCommunicator.parseFeatures(responses, this.layer);

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
		
		log.info("Got", this.features.size(), "features for layer", layer.getLayerId());

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
            log.error(ee, "Unhandled exception during request: ");
	   	 	output.put(OUTPUT_LAYER_ID, this.layerId);
	   	 	output.put(OUTPUT_ONCE, true);
	   	 	output.put(OUTPUT_MESSAGE, TransportService.ERROR_REST_REQUEST_FAILED);
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

        log.debug("Starting properties handler");

        List<String> selectedProperties = new ArrayList<String>();
        List<String> layerSelectedProperties = layer.getSelectedFeatureParams(session.getLanguage());

        // selected props
        if(layerSelectedProperties != null && layerSelectedProperties.size() != 0) {
            selectedProperties.addAll(this.layer.getSelectedFeatureParams(this.session.getLanguage()));
        } else { // all properties
        	for (ArcGisFeature feat : this.features) {
        		for (ArcGisProperty prop : feat.getProperties()) {
        			String field = prop.getName().toString();
                    if(!this.layer.getGMLGeometryProperty().equals(field)) { // don't add geometry
                        selectedProperties.add(field);
                    }
        		}
        		break;
        	}
        }
                
        log.info("Got", selectedProperties.size(), "properties for layer", layer.getLayerId());
        
        this.sendWFSProperties(selectedProperties, this.layer.getFeatureParamsLocales(this.session.getLanguage()));
    }

    /**
     * Parses features values
     */
    private void featuresHandler() {
        log.debug("Starting features handler");
                
        Geometry screenGeometry = ArcGisFilter.initBBOXFilter(this.session.getLocation(), this.transformService);        

        this.featureValuesList = new ArrayList<List<Object>>();
        this.geomValuesList = new ArrayList<List<Object>>();
        for (ArcGisFeature feature : this.features) {
            // if is not in shown area -> skip
            if(!screenGeometry.intersects(feature.getGeometry())) {
                //log.debug("Not selected feature ", feature.GetId());
                continue;
            }            

            List<Object> values = new ArrayList<Object>();

            String fid = feature.GetId();
			
            if (fid == null) {            	
				log.warn("Feature has no id. Skipping");
				continue;
			}			
			
            if (!this.processedFIDs.contains(fid)) {
                // __fid value
                values.add(fid);
                this.processedFIDs.add(fid);

                // get feature geometry (transform if needed) and get geometry center
                //Geometry geometry = WFSParser.getFeatureGeometry(feature, this.layer.getGMLGeometryProperty(), this.transformClient);
                Geometry geometry = feature.getGeometry();
                
                if (this.session.isGeomRequest())
                {
                	List<Object> gvalues = new ArrayList<Object>();
                	gvalues.add(fid);
                	gvalues.add(geometry);
                	this.geomValuesList.add(gvalues);
                }
                
                // send values
                if(this.sendFeatures) {
                    Point centerPoint = WFSParser.getGeometryCenter(geometry);

                    // selected values
                    List<String> selectedProperties = layer.getSelectedFeatureParams(session.getLanguage());
                    if(selectedProperties != null && selectedProperties.size() != 0) {
                        for(String attr : selectedProperties) {
//REST layers are not USERLAYERS                        	
//                            if (this.layer.getLayerId().startsWith(UserLayerFilter.USERLAYER_PREFIX)) {
//                                Object prop = feature.getAttribute(attr);
//                                try {
//                                    HashMap<String, Object> propMap = new ObjectMapper().readValue(prop.toString(), HashMap.class);
//                                    values.add(propMap);
//                                } catch(Exception e) {
//                                    values.add(prop);
//                                }
//                            } else {
                                values.add(feature.getPropertyValue(attr));
//                            }
                        }
                    } else { // all values
                    	
                    	
                    	for (ArcGisProperty prop : feature.getProperties()) {
                    		String field = prop.getName().toString();
                    		if(!this.layer.getGMLGeometryProperty().equals(field)) { // don't add geometry
                    			values.add(feature.getPropertyValue(field));
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
        return ArcGisImage.getCache(
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
    	ArcGisImage.setCache(
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
        output.put(OUTPUT_MESSAGE, TransportService.ERROR_REST_IMAGE_PARSING);
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

   	 	log.debug("Sending", fields.size(), "properties");   	 	
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

   	 	//log.debug("Sending feature with", values.size(), "properties");
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

   	 	log.debug("Sending", features.size(), "features");
    	this.service.send(this.session.getClient(), channel, output);
    }
    
    private void sendWFSFeatureGeometries(List<List<Object>> geometries, String channel) {
    	if(geometries == null || geometries.size() == 0) {
    		log.warn("Failed to send feature geometries");
    		return;
    	}
    	Map<String, Object> output = new HashMap<String, Object>();
    	output.put(OUTPUT_LAYER_ID, this.layerId);
    	output.put(OUTPUT_GEOMETRIES, geometries);
    	output.put(OUTPUT_KEEP_PREVIOUS, this.session.isKeepPrevious());

    	log.debug("Sending", geometries.size(), "geometries");
    	this.service.send(this.session.getClient(), channel, output);
    }
    
    private void sendStyleConfiguration(CustomStyleStore style) {
    	if(style == null) {
    		log.warn("Failed to style configuration");
    		return;
    	}
    	Map<String, Object> output = new HashMap<String, Object>();
    	output.put(OUTPUT_LAYER_ID, this.layerId);
    	output.put(OUTPUT_STYLE, JSONValue.parse(style.getAsJSON()));

    	log.debug("Sending style configuration");
    	this.service.send(this.session.getClient(), TransportService.CHANNEL_DEFAULT_STYLE, output);
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

        byte[] byteImage = ArcGisImage.imageToBytes(bufferedImage);
        String base64Image = ArcGisImage.bytesToBase64(byteImage);
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