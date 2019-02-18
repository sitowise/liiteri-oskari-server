package fi.nls.oskari.control.layer;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.control.szopa.IndicatorClassificationService;
import fi.nls.oskari.control.szopa.IndicatorService;
import fi.nls.oskari.control.twowaystats.TwowayIndicatorService;
import fi.nls.oskari.domain.map.stats.StatsVisualization;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.stats.VisualizationService;
import fi.nls.oskari.util.ConversionHelper;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.PropertyUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.esri.core.geometry.Envelope;

import pl.sito.liiteri.arcgis.ArcgisToken;
import pl.sito.liiteri.arcgis.ArcgisTokenConfiguration;
import pl.sito.liiteri.arcgis.ArcgisTokenService;
import pl.sito.liiteri.arcgis.domain.ArcgisLayer;
import pl.sito.liiteri.map.arcgislayer.service.ArcgisImageService;
import pl.sito.liiteri.map.arcgislayer.service.ArcgisLayerService;
import pl.sito.liiteri.stats.domain.ClassificationDescription;
import pl.sito.liiteri.stats.domain.ClassificationParams;
import pl.sito.liiteri.stats.domain.GridStatsResult;
import pl.sito.liiteri.stats.domain.GridStatsVisualization;
import pl.sito.liiteri.stats.domain.GridStatsVisualizationParams;
import pl.sito.liiteri.stats.domain.GridStatsVisualizationParams.GridStatsVisualizationType;

import java.net.HttpURLConnection;
import java.util.List;

@OskariActionRoute("GetArcGisStatsTile")
public class GetArcGisStatsTileHandler extends ActionHandler {
	
	public enum RequestType { 
		Export,
		Identify,
	}

    private static final Logger log = LogFactory.getLogger(GetArcGisStatsTileHandler.class);
    
    
    final private static String PARAM_LAYER_ID = "LAYERID";
    final private static String PARAM_LAYERS = "LAYERS";
    final private static String PARAM_SCALE = "SCALE";   
    final private static String PARAM_REQUEST = "REQUEST";

    private final VisualizationService service = new VisualizationService();
    private final ArcgisTokenService tokenService = ArcgisTokenService.getInstance();
    private final ArcgisImageService imageService = ArcgisImageService.getInstance();
    private final ArcgisLayerService arcgisLayerService = new ArcgisLayerService();
    private final IndicatorService indicatorService = IndicatorService.getInstance();
	private final TwowayIndicatorService twowayIndicatorService = TwowayIndicatorService.getInstance();
    private final IndicatorClassificationService indicatorClassificationService = IndicatorClassificationService.getInstance();        
    
    private final String TokenServiceUrl = PropertyUtil.get("liiteri.statistics.arcgis.url");
    private final String ExportUrl = PropertyUtil.get("liiteri.statistics.arcgis.url.export");
    private final String IdentifyUrl = PropertyUtil.get("liiteri.statistics.arcgis.url.identify");

    final private static String PARAM_VISUALIZATION_ID = "VIS_ID";
    final private static String PARAM_VISUALIZATION_NAME = "VIS_NAME"; // name=ows:Kunnat2013
    final private static String PARAM_VISUALIZATION_FILTER_PROPERTY = "VIS_ATTR"; // attr=Kuntakoodi
    final private static String PARAM_VISUALIZATION_CLASSES = "VIS_CLASSES"; // classes=020,091|186,086,982|111,139,740
    final private static String PARAM_VISUALIZATION_VIS = "VIS_COLORS"; // vis=choro:ccffcc|99cc99|669966
    final private static String PARAM_VISUALIZATION_METHOD = "VIS_METHOD";
    final private static String PARAM_VISUALIZATION_METHOD_TYPE = "VIS_METHOD_TYPE";
    
    final private static String PARAM_INDICATOR_DATA = "INDICATORDATA";
    final private static String PARAM_CLASSIFY = "CLASSIFY";
    
    final private static String PARAM_BBOX = "BBOX";
    final private static String PARAM_SIZE = "SIZE";
    
    final public static String PARAM_LANGUAGE = "lang";  

    public void handleAction(final ActionParameters params) throws ActionException 
    {    	
    	//validation
    	final RequestType requestType = parseRequestType(params);
    	    	    	                       
        try {        	         
        	byte[] presponse = null; 
        	    	       
        	if (requestType == RequestType.Export 
        			&& params.getHttpParam(PARAM_VISUALIZATION_METHOD, "").startsWith("grid")) 
        	{
        		int gridSize = parseGridSize(params);
        		
        		String indicatorData = params.getHttpParam(PARAM_INDICATOR_DATA, null);
        		JSONObject indicatorDataJSON = new JSONObject(indicatorData);
        		String indicatorId = indicatorDataJSON.getString("id");
        		String indicatorYear = indicatorDataJSON.getString("year");
        		String indicatorGeometryFilter = indicatorDataJSON.isNull("geometry") ? "" : indicatorDataJSON.getString("geometry");			
    			String indicatorFilter = indicatorDataJSON.isNull("filter") ? "" : indicatorDataJSON.getString("filter");
    			String indicatorType = indicatorDataJSON.isNull("type") ? "" : indicatorDataJSON.getString("type");
    			String indicatorDirection = indicatorDataJSON.isNull("direction") ? "" : indicatorDataJSON.getString("direction");
    			String indicatorGender = indicatorDataJSON.isNull("gender") ? "" : indicatorDataJSON.getString("gender");
    			
        		Envelope bbox = parseBoundingBox(params);
        		Envelope expandedBbox = (Envelope) bbox.copy();
        		expandedBbox.inflate(gridSize, gridSize);
        		
               	long start = System.nanoTime();               	
    			GridStatsResult result = null;
    			int id = Integer.parseInt(indicatorId);
    			if(id >= 0) {
    				result = indicatorService.getGridIndicatorData(indicatorId, new String[] { indicatorYear}, gridSize, indicatorFilter, indicatorGeometryFilter, expandedBbox, params.getUser());
    			} else {
    				result = twowayIndicatorService.getGridIndicatorData(indicatorId, new String[] { indicatorYear}, gridSize, indicatorFilter, indicatorGeometryFilter, expandedBbox, indicatorType, indicatorDirection, indicatorGender, params.getUser());
    			}
    			long end = System.nanoTime();
               	log.info("indicatorService.getGridIndicatorData " + (end - start)/1000000.0 + "ms");
               	
               	GridStatsVisualizationParams visualizationParams = new GridStatsVisualizationParams();
        		int width = parseWidth(params);
        		int height = parseHeight(params);
        		GridStatsVisualizationType visualizationType = parseVisualizationType(params);
        		String[] colors = parseColors(params);
    			visualizationParams.setBbox(bbox);
    			visualizationParams.setWidth(width);
    			visualizationParams.setHeight(height);
    			visualizationParams.setGridSize(gridSize);
    			visualizationParams.setColors(colors);
    			visualizationParams.setVisualizationType(visualizationType);
    			GridStatsVisualization visualization = null;
        		
        		if (!result.IsEmpty()) {           	            	
            		ClassificationDescription classificationDescription = parseClassificationDescription(params);            		              		
        			visualization = indicatorClassificationService.classify(result, classificationDescription);        			        			        			        			                 		
        		}
        		
        		presponse = this.imageService.renderTile(visualizationParams, visualization);
        	}      
        	else 
        	{
        		presponse = requestDataFromServer(requestType, params);     
        	}

            final HttpServletResponse response = params.getResponse();                       
            switch(requestType) {
            	case Export:
            		response.setContentType("image/png");
            		break;
            	case Identify:
            		response.setContentType("application/json");
            		break;
            }            
            
            response.getOutputStream().write(presponse, 0, presponse.length);
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (Exception e) {
            throw new ActionException("Couldn't proxy request to ArcGis server",
                    e);
        }
    }
    
    private byte[] requestDataFromServer(final RequestType requestType, final ActionParameters params) throws Exception {
    	HttpURLConnection con = null;
    	try {
    		String postData = getPostData(params, requestType);     
    		con = getConnection(params, requestType);            
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setDoInput(true);
            HttpURLConnection.setFollowRedirects(false);
            con.setUseCaches(false);
            con.connect();  
            if (!postData.isEmpty())
            	IOHelper.writeToConnection(con, postData);     
            
            byte[] result = IOHelper.readBytes(con.getInputStream());
            
            if (requestType == RequestType.Export) {
                String contentType = con.getContentType();
                if (!contentType.contains("image")) {
                	log.warn("Invalid response" + new String(result, "UTF-8"));
                	throw new Exception("Invalid content type. Expected image");
                }	
            }
            
            return result;
            
    	} finally {
    		if(con != null) {
                con.disconnect();
            }
    	}        
    }
    

    
    private String getMapServerUrl(String url) {
    	int index = url.indexOf("/export?");
    	if (index < 0)
    		return url;
    	
    	return url.substring(0, index);
    }
    
    private String getPostData(final ActionParameters params, final RequestType requestType) throws ActionException 
    {
        final HttpServletRequest httpRequest = params.getRequest();                     
        final StringBuffer queryString = new StringBuffer();
        int layerId = params.getHttpParam(PARAM_LAYERS, -1);
        
        for (Object key : httpRequest.getParameterMap().keySet()) {
            String keyStr = (String) key;
            
            String value = params.getHttpParam(keyStr);
            if (keyStr.equals(PARAM_LAYERS)) 
            {            	            
            	if (requestType == RequestType.Export) 
            	{            		            		                                                      	
            		value = "show:" + layerId;
            	}            	
            }            	
            
            queryString.append("&");
            queryString.append(keyStr);
            queryString.append("=");
            queryString.append(value);
        }
                       
        String rendered = buildRenderer(layerId, params);
        
        if (!rendered.isEmpty()) 
        {
        	queryString.append("&");
            queryString.append("dynamicLayers");
            queryString.append("=");
            queryString.append(rendered);
        }                            
        
        return queryString.toString();        
    }
       
    private HttpURLConnection getConnection(final ActionParameters params, final RequestType requestType)
            throws ActionException {
    	           	    	    	
    	ArcgisToken token = tokenService.getTokenForServer(TokenServiceUrl, ArcgisTokenConfiguration.createRequestConfiguration());	
    	
    	if (token.isEmpty())
    		throw new ActionException("Couldn't get token for ArcGis server");
    	
    	String serverUrl;
    	
    	switch(requestType) {
    		case Export :
    			serverUrl = ExportUrl;
    			break;
    		case Identify :
    			serverUrl = IdentifyUrl;
    			break;
    		default:
    			throw new ActionException("Invalid request type " + requestType);
    	}
                         
        try {            
        	final String url = serverUrl + "token=" + token.getToken();
            log.debug("Getting stats tile from url:", url);
            return IOHelper.getConnection(url);
        } catch (Exception e) {
            throw new ActionException("Couldn't get connection to ArcGis server", e);
        }
    }
    
    private String buildRenderer(final int layerId, final ActionParameters params) throws ActionException {    	
        final StringBuilder styleUrl = new StringBuilder();
        
        StatsVisualization vis = getVisualization(params);
           
		if (vis != null) {
	        String attribute = params.getHttpParam("VIS_ATTR", null);
	        final ArcgisLayer layer = new ArcgisLayer(getMapServerUrl(ExportUrl), layerId);
	        List<Integer> sublayerIds = arcgisLayerService.getArcgisSubLayersIds(layer);	        	   
			return generateDynamicLayers(vis, sublayerIds, attribute);	
		}
        
        return styleUrl.toString();
    }
    
    private String generateDynamicLayers(StatsVisualization vis, List<Integer> layerIds, String attribute) {
    	
    	JSONArray json = new JSONArray();
    	
    	try
		{    		    	
    		JSONObject drawingInfo = generateDrawingInfo(vis, attribute);
    		for (Integer layerId : layerIds)
			{
    			JSONObject dynamicLayer = generateDynamicLayer(layerId, drawingInfo);
    			json.put(dynamicLayer);
			}    		
		} 
    	catch (JSONException e)
		{
			log.error(e.toString());
		}
    	
    	
    	return json.toString();
    }
    
    private JSONObject generateDrawingInfo(StatsVisualization vis, String attribute) throws JSONException {
		JSONObject drawingInfo = new JSONObject(); 
		JSONObject renderer = new JSONObject();
		renderer.put("type", "uniqueValue");
		renderer.put("field1", attribute);
		renderer.put("field2", JSONObject.NULL);
		renderer.put("field3", JSONObject.NULL);
		renderer.put("fieldDelimiter", ", ");
		
		JSONObject defaultSymbol = new JSONObject("{\"type\": \"esriSFS\",\"style\": \"esriSFSNull\",\"color\": [255,255,255,255],\"outline\": {\"type\": \"esriSLS\",\"style\": \"esriSLSSolid\",\"color\": [0,0,0,255],\"width\": 1}}");
		renderer.put("defaultSymbol", defaultSymbol);
		renderer.put("defaultLabel", "");
		
		JSONArray uniqueValues = new JSONArray();
		
		String[] groupColors = vis.getGroupColors();
		String[] groupClasses = vis.getClassGroups();
		int len = groupClasses.length;
		
		for (int i=0; i<len; i++) 
		{
			String color = groupColors[i];
			int[] colorArray = new int[] { 
					Integer.valueOf( color.substring( 0, 2 ), 16 ),
					Integer.valueOf( color.substring( 2, 4 ), 16 ),
					Integer.valueOf( color.substring( 4, 6 ), 16 ),
					255
					};
			
			String classes = groupClasses[i];
			String[] classesArray = new String[0];
			
	        if (classes != null && !classes.isEmpty()) {
	            classesArray = classes.split(",");
	        }
	        
			JSONObject symbolOutline = new JSONObject();
			symbolOutline.put("type", "esriSFS");
			symbolOutline.put("style", "esriSFSSolid");
			symbolOutline.put("color", new JSONArray(new int[] { 0, 0, 0, 255}));
			symbolOutline.put("width", 1);
	        
	        for (String classItem : classesArray)
			{
				JSONObject item = new JSONObject();
				item.put("value", classItem);
				item.put("label", "");
				item.put("description", "");
				
				JSONObject itemSymbol = new JSONObject();
				itemSymbol.put("type", "esriSFS");
				itemSymbol.put("style", "esriSFSSolid");													
				itemSymbol.put("color", new JSONArray(colorArray));
									
				itemSymbol.put("outline", symbolOutline);
				
				item.put("symbol", itemSymbol);
				uniqueValues.put(item);
			}
		}					
		
		renderer.put("uniqueValueInfos", uniqueValues);
		
		drawingInfo.put("renderer", renderer);
		
		return drawingInfo;
    }
    
    private JSONObject generateDynamicLayer(int layerId, JSONObject drawingInfo) throws JSONException 
    {    	
    		JSONObject json = new JSONObject();

			json.put("id", layerId * 10);
			
			JSONObject source = new JSONObject();
			source.put("type", "mapLayer");
			source.put("mapLayerId", layerId);		
			json.put("source", source);
			
			json.put("drawingInfo", drawingInfo);
			return json;
    }

    private StatsVisualization getVisualization(final ActionParameters params) {
        final int statsLayerId = ConversionHelper.getInt(
                params.getHttpParam(PARAM_LAYER_ID), -1);
        final int visId = ConversionHelper.getInt(
                params.getHttpParam(PARAM_VISUALIZATION_ID), -1);
        return service.getVisualization(
                //statsLayerId, 
                visId,
                params.getHttpParam(PARAM_VISUALIZATION_CLASSES),
                params.getHttpParam(PARAM_VISUALIZATION_NAME),
                params.getHttpParam(PARAM_VISUALIZATION_FILTER_PROPERTY),
                params.getHttpParam(PARAM_VISUALIZATION_VIS, "")
                );
        
    }

    private RequestType parseRequestType(final ActionParameters params) {
    	RequestType result = RequestType.Export;    	
    	final String request = params.getRequest().getParameter(PARAM_REQUEST);
    	
    	if (request != null && request.equals("identify")) {
    		result = RequestType.Identify;
    	}
    	
    	return result;
    }
    
    private int parseWidth(final ActionParameters params) {
		int width = 256;
		String sizeString = params.getHttpParam(PARAM_SIZE, null);
		if (sizeString != null) {
			String[] sizeArray = sizeString.split(",");
			width = Integer.valueOf(sizeArray[0]);
		}
		
		return width;
    }
    
    private int parseHeight(final ActionParameters params) {
		int height = 256;
		String sizeString = params.getHttpParam(PARAM_SIZE, null);
		if (sizeString != null) {
			String[] sizeArray = sizeString.split(",");
			height = Integer.valueOf(sizeArray[1]);
		}
		
		return height;
    }
    
    private Envelope parseBoundingBox(final ActionParameters params) throws ActionParamsException {
		Envelope bbox = null;
    	
    	String bboxString = params.getRequiredParam(PARAM_BBOX);
		String[] bboxArray = bboxString.split(",");
		if (bboxArray.length == 4) {
			bbox = new Envelope();
			bbox.setXMin(Double.valueOf(bboxArray[0]));
    		bbox.setYMin(Double.valueOf(bboxArray[1]));
    		bbox.setXMax(Double.valueOf(bboxArray[2]));
    		bbox.setYMax(Double.valueOf(bboxArray[3]));
		}
		
		if (bbox == null)
			throw new ActionParamsException("Invalid parameter format " + PARAM_BBOX);
		
		return bbox;
    }
    
    private int parseGridSize(final ActionParameters params) {
    	int result = 250;
    	int factor = 1;
    	String visualizationMethod = params.getHttpParam(PARAM_VISUALIZATION_METHOD, null);
    	if (visualizationMethod != null && visualizationMethod.startsWith("grid")) {
    		if (visualizationMethod.contains("km")) {
    			factor = 1000;
    		}    	    		
    	}
    	result = Integer.parseInt(visualizationMethod.replace("grid", "").replace("km", "").replace("m", ""));
    	result *= factor;
    	return result;
    }
    
    private ClassificationParams parseClassficiationParams(final ActionParameters params) {
    	ClassificationParams result = new ClassificationParams();
    	
	    String colorParams = params.getHttpParam(PARAM_VISUALIZATION_VIS, null);
	    if (colorParams != null && colorParams.startsWith("choro:")) {
	    	String[] colorArray = colorParams.substring(6).split("\\|");
	    	result.setColors(colorArray);
	    	result.setNumberOfClasses(colorArray.length);
	    }
    	
    	return result;
    }
    
    private ClassificationDescription parseClassificationDescription(final ActionParameters params) throws JSONException {
    	ClassificationDescription result = new ClassificationDescription();
    	
	    String classifyParams = params.getHttpParam(PARAM_CLASSIFY, null);
	    if (classifyParams != null) {
	    	result = ClassificationDescription.fromJSON(classifyParams);
	    }
    	
    	return result;
    }
    
    private GridStatsVisualizationType parseVisualizationType(final ActionParameters params) {
    	GridStatsVisualizationType result = GridStatsVisualizationType.Square;
    	String visMethodType = params.getHttpParam(PARAM_VISUALIZATION_METHOD_TYPE, null);
    	if (visMethodType != null) {
    		if (visMethodType.equals("CIRCLE"))
    			result = GridStatsVisualizationType.Circle;
    	}

    	return result;
    }
    
    private String[] parseColors(final ActionParameters params) {
    	String colors = params.getHttpParam(PARAM_VISUALIZATION_VIS, null);
    	if (colors == null)
    		return new String[0];
    	return colors.split(",");
    }
}