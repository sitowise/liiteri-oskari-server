package fi.nls.oskari.wfs;

import java.util.List;

import org.json.JSONObject;

import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.cache.JedisManager;

import fi.nls.oskari.domain.map.wfs.WFSSLDStyle;

import javax.xml.namespace.QName;

/**
 * Handles layer's configuration
 *
 * Similar WFSLayerStore class can be found in transport.
 */
public class WFSLayerConfiguration {
	public final static String KEY = "WFSLayer_";

    private final static String LAYER_ID = "layerId";
    private final static String NAME_LOCALES = "nameLocales";
    private final static String URL_PARAM = "URL";
    private final static String USERNAME = "username";
    private final static String PASSWORD = "password";

	private final static String LAYER_NAME = "layerName";

    private final static String GML_GEOMETRY_PROPERTY = "GMLGeometryProperty";
    private final static String SRS_NAME = "SRSName";
    private final static String GML_VERSION = "GMLVersion";
    private final static String GML2_SEPARATOR = "GML2Separator";
    private final static String WFS_VERSION = "WFSVersion";
    private final static String MAX_FEATURES = "maxFeatures";
    private final static String FEATURE_NAMESPACE = "featureNamespace";
    private final static String FEATURE_NAMESPACE_URI = "featureNamespaceURI";
    private static final String GEOMETRY_NAMESPACE_URI = "geometryNamespaceURI";
    private final static String FEATURE_ELEMENT = "featureElement";
    private final static String OUTPUT_FORMAT = "outputFormat";

    private final static String FEATURE_TYPE = "featureType";
    private final static String SELECTED_FEATURE_PARAMS = "selectedFeatureParams";
    private final static String FEATURE_PARAMS_LOCALES = "featureParamsLocales";
    private final static String GEOMETRY_TYPE = "geometryType";
    private final static String GET_MAP_TILES = "getMapTiles";
    private static final String GET_HIGHLIGHT_IMAGE = "getHighlightImage";
    private final static String GET_FEATURE_INFO = "getFeatureInfo";
    private final static String TILE_REQUEST = "tileRequest";
    private final static String TILE_BUFFER = "tileBuffer";
    private final static String WMS_LAYER_ID = "WMSLayerId";
    // for json, if needed private final static String WPS_PARAMS = "wps_params";
    private final static String CUSTOM_PARSER = "customParser";
    private final static String TEST_LOCATION = "testLocation";
    private final static String TEST_ZOOM = "testZoom";

    private final static String MIN_SCALE = "minScale";
    private final static String MAX_SCALE = "maxScale";


    private final static String TEMPLATE_NAME = "templateName";
    private final static String TEMPLATE_DESCRIPTION = "templateDescription";
    private final static String TEMPLATE_TYPE = "templateType";
    private final static String REQUEST_TEMPLATE = "requestTemplate";
    private final static String RESPONSE_TEMPLATE = "responseTemplate";

    private final static String SELECTION_SLD_STYLE = "selectionSLDStyle";

    private final static String STYLES = "styles";
    private final static String ID = "id";
    private final static String NAME = "name";
    private final static String SLD_STYLE = "SLDStyle";

	private String layerId;
	private String nameLocales;
	private String URL;
	private String username;
	private String password;

    private String layerName;

	private String GMLGeometryProperty;
	private String SRSName;
	private String GMLVersion;
    private boolean GML2Separator; // if srs url is in old format (# => :)
	private String WFSVersion;
	private int maxFeatures;
	private String featureNamespace;
	private String featureNamespaceURI;
    private String geometryNamespaceURI;
	private String featureElement;
    private String outputFormat;

	private String featureType;
	private String selectedFeatureParams; // if needed?
	private String featureParamsLocales;
	private String geometryType; // 2D/3D
	private boolean getMapTiles; // if tile images are drawn and send
    private boolean getHighlightImage; // if highlight image is drawn and send
	private boolean getFeatureInfo; // if feature json is send
    private boolean tileRequest;
    private String tileBuffer;
	private String WMSLayerId;
    private String wps_params;  // WPS params for WFS layer eg {input_type:gs_vector}
    private boolean customParser;
    private String testLocation;
    private int testZoom;

	private double minScale;
	private double maxScale;

	// Template Model
	private String templateName;
	private String templateDescription;
	private String templateType;
	private String requestTemplate;
	private String responseTemplate;

	private String selectionSLDStyle;

	private List<WFSSLDStyle> SLDStyles; // id, name, xml

    /**
     * Constructs a QName for feature element.
     * @return
     */
    public QName getFeatureElementQName() {
        /*
        old db table        | new db table
        portti_feature_type | portti_wfs_layer field
        --------------------------------------------
        namespace_uri       | feature_namespace_uri
        localpart           | feature_element
        name prefix         | feature_namespace
        */
        return new QName(getFeatureNamespaceURI(), getFeatureElement(), getFeatureNamespace());
    }

    public String getLayerId() {
		return layerId;
	}

	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}
	
    public int getLayerIdInt() {  
    	int result = -1;
    	
    	result = Integer.parseInt(getLayerId());
    	
		return result;
	}

	public void setLayerIdInt(int layerId) {
		this.layerId = "" + layerId;
	}

	public String getNameLocales() {
		return nameLocales;
	}

	public void setNameLocales(String nameLocales) {
		this.nameLocales = nameLocales;
	}

	public String getURL() {
		return URL;
	}

	public void setURL(String uRL) {
		URL = uRL;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getGMLGeometryProperty() {
		return GMLGeometryProperty;
	}

	public void setGMLGeometryProperty(String gMLGeometryProperty) {
		GMLGeometryProperty = gMLGeometryProperty;
	}

	public String getSRSName() {
		return SRSName;
	}

	public void setSRSName(String sRSName) {
		SRSName = sRSName;
	}

	public String getGMLVersion() {
		return GMLVersion;
	}

	public void setGMLVersion(String gMLVersion) {
		GMLVersion = gMLVersion;
	}

    public boolean isGML2Separator() {
        return this.GML2Separator;
    }

    public void setGML2Separator(boolean GML2Separator) {
        this.GML2Separator = GML2Separator;
    }

	public String getWFSVersion() {
		return WFSVersion;
	}

	public void setWFSVersion(String wFSVersion) {
		WFSVersion = wFSVersion;
	}

	public int getMaxFeatures() {
		return maxFeatures;
	}

	public void setMaxFeatures(int maxFeatures) {
		this.maxFeatures = maxFeatures;
	}

	public String getFeatureNamespace() {
		return featureNamespace;
	}

	public void setFeatureNamespace(String featureNamespace) {
		this.featureNamespace = featureNamespace;
	}

	public String getFeatureNamespaceURI() {
		return featureNamespaceURI;
	}

	public void setFeatureNamespaceURI(String featureNamespaceURI) {
		this.featureNamespaceURI = featureNamespaceURI;
	}

    public String getGeometryNamespaceURI() {
        return geometryNamespaceURI;
    }

    public void setGeometryNamespaceURI(String geometryNamespaceURI) {
        this.geometryNamespaceURI = geometryNamespaceURI;
    }

	public String getFeatureElement() {
		return featureElement;
	}

	public void setFeatureElement(String featureElement) {
		this.featureElement = featureElement;
	}

    public String getOutputFormat() { return outputFormat; }

    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

    public String getFeatureType() {
		return featureType;
	}

	public void setFeatureType(String featureType) {
		this.featureType = featureType;
	}

	public String getSelectedFeatureParams() {
		return selectedFeatureParams;
	}

	public void setSelectedFeatureParams(String selectedFeatureParams) {
		this.selectedFeatureParams = selectedFeatureParams;
	}

	public String getFeatureParamsLocales() {
		return featureParamsLocales;
	}

	public void setFeatureParamsLocales(String featureParamsLocales) {
		this.featureParamsLocales = featureParamsLocales;
	}

	public String getGeometryType() {
		return geometryType;
	}

	public void setGeometryType(String geometryType) {
		this.geometryType = geometryType;
	}

	public boolean isGetMapTiles() {
		return this.getMapTiles;
	}

	public void setGetMapTiles(boolean getMapTiles) {
		this.getMapTiles = getMapTiles;
	}

    public boolean isGetHighlightImage() {
    	return this.getHighlightImage;
    }

    public void setGetHighlightImage(boolean getHighlightImage) {
        this.getHighlightImage = getHighlightImage;
    }

	public boolean isGetFeatureInfo() {
		return this.getFeatureInfo;
	}

	public void setGetFeatureInfo(boolean getFeatureInfo) {
		this.getFeatureInfo = getFeatureInfo;
	}

    public boolean isTileRequest() {
        return this.tileRequest;
    }

    public void setTileRequest(boolean tileRequest) {
        this.tileRequest = tileRequest;
    }

    public String getTileBuffer() {
        return tileBuffer;
    }

    public void setTileBuffer(String tileBuffer) {
        this.tileBuffer = tileBuffer;
    }

    public String getWMSLayerId() {
		return WMSLayerId;
	}

	public void setWMSLayerId(String wMSLayerId) {
		WMSLayerId = wMSLayerId;
	}

    /**
     * Get wps params for WFS layer eg {input_type:gs_vector}
     * (default is {})
     * @return
     */
    public String getWps_params() {
        return wps_params;
    }

    /**
     * Set wps_params (basic field value in portti_wfs_layer)
     * @param wps_params
     */
    public void setWps_params(String wps_params) {
        this.wps_params = wps_params;
    }

    public boolean isCustomParser() {
        return this.customParser;
    }

    public void setCustomParser(boolean customParser) {
        this.customParser = customParser;
    }

    public String getTestLocation() {
        return testLocation;
    }

    public void setTestLocation(String testLocation) {
        this.testLocation = testLocation;
    }

    public int getTestZoom() {
        return testZoom;
    }

    public void setTestZoom(int testZoom) {
        this.testZoom = testZoom;
    }

    /**
	 * Gets min scale
	 *
	 * @return min scale
	 */
	public double getMinScale() {
		return minScale;
	}

	/**
	 * Sets min scale
	 *
	 * @param minScale
	 */
	public void setMinScale(double minScale) {
		this.minScale = minScale;
	}

	/**
	 * Gets max scale
	 *
	 * @return max scale
	 */
	public double getMaxScale() {
		return maxScale;
	}

	/**
	 * Sets max scale
	 *
	 * @param maxScale
	 */
	public void setMaxScale(double maxScale) {
		this.maxScale = maxScale;
	}

    /**
	 * Gets template name
	 *
	 * @return template name
	 */
	public String getTemplateName() {
		return templateName;
	}

	/**
	 * Sets template name
	 *
	 * @param templateName
	 */
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	/**
	 * Gets template description
	 *
	 * @return template description
	 */
	public String getTemplateDescription() {
		return templateDescription;
	}

	/**
	 * Sets template description
	 *
	 * @param templateDescription
	 */
	public void setTemplateDescription(String templateDescription) {
		this.templateDescription = templateDescription;
	}

	/**
	 * Gets template type
	 *
	 * @return template type
	 */
	public String getTemplateType() {
		return templateType;
	}

	/**
	 * Sets template type
	 *
	 * @param templateType
	 */
	public void setTemplateType(String templateType) {
		this.templateType = templateType;
	}

	public String getRequestTemplate() {
		return requestTemplate;
	}

	public void setRequestTemplate(String requestTemplate) {
		this.requestTemplate = requestTemplate;
	}

	public String getResponseTemplate() {
		return responseTemplate;
	}

	public void setResponseTemplate(String responseTemplate) {
		this.responseTemplate = responseTemplate;
	}

	public String getSelectionSLDStyle() {
		return selectionSLDStyle;
	}

	public void setSelectionSLDStyle(String selectionSLDStyle) {
		this.selectionSLDStyle = selectionSLDStyle;
	}

	public List<WFSSLDStyle> getSLDStyles() {
		return SLDStyles;
	}

	public void setSLDStyles(List<WFSSLDStyle> sLDStyles) {
		SLDStyles = sLDStyles;
	}


	public void save() {
		JedisManager.setex(KEY + this.layerId, 86400, getAsJSON()); // expire in 1 day
	}

	public void destroy() {
		JedisManager.del(KEY + this.layerId);
	}

	public String getAsJSON() {
		final JSONObject root = new JSONObject();

		JSONHelper.putValue(root, LAYER_ID, this.getLayerId());
		JSONHelper.putValue(root, NAME_LOCALES, JSONHelper.createJSONObject(this.getNameLocales()));
		JSONHelper.putValue(root, URL_PARAM, this.getURL());
		JSONHelper.putValue(root, USERNAME, this.getUsername());
		JSONHelper.putValue(root, PASSWORD, this.getPassword());

		JSONHelper.putValue(root, LAYER_NAME, this.getLayerName());

		JSONHelper.putValue(root, GML_GEOMETRY_PROPERTY, this.getGMLGeometryProperty());
		JSONHelper.putValue(root, SRS_NAME, this.getSRSName());
		JSONHelper.putValue(root, GML_VERSION, this.getGMLVersion());
        JSONHelper.putValue(root, GML2_SEPARATOR, this.isGML2Separator());
		JSONHelper.putValue(root, WFS_VERSION, this.getWFSVersion());
		JSONHelper.putValue(root, MAX_FEATURES, this.getMaxFeatures());
		JSONHelper.putValue(root, FEATURE_NAMESPACE, this.getFeatureNamespace());
		JSONHelper.putValue(root, FEATURE_NAMESPACE_URI, this.getFeatureNamespaceURI());
        JSONHelper.putValue(root, GEOMETRY_NAMESPACE_URI, this.getGeometryNamespaceURI());
		JSONHelper.putValue(root, FEATURE_ELEMENT, this.getFeatureElement());
        JSONHelper.putValue(root, OUTPUT_FORMAT, this.getOutputFormat());

		JSONHelper.putValue(root, FEATURE_TYPE, JSONHelper.createJSONObject(this.getFeatureType()));
		JSONHelper.putValue(root, SELECTED_FEATURE_PARAMS, JSONHelper.createJSONObject(this.getSelectedFeatureParams()));
		JSONHelper.putValue(root, FEATURE_PARAMS_LOCALES, JSONHelper.createJSONObject(this.getFeatureParamsLocales()));
		JSONHelper.putValue(root, GEOMETRY_TYPE, this.getGeometryType());
		JSONHelper.putValue(root, GET_MAP_TILES, this.isGetMapTiles());
        JSONHelper.putValue(root, GET_HIGHLIGHT_IMAGE, this.isGetHighlightImage());
		JSONHelper.putValue(root, GET_FEATURE_INFO, this.isGetFeatureInfo());
        JSONHelper.putValue(root, TILE_REQUEST, this.isTileRequest());
        JSONHelper.putValue(root, TILE_BUFFER, JSONHelper.createJSONObject(this.getTileBuffer()));
		JSONHelper.putValue(root, WMS_LAYER_ID, this.getWMSLayerId());
        JSONHelper.putValue(root, CUSTOM_PARSER, this.isCustomParser());
        if( this.getTestLocation() != null ) {
            JSONHelper.putValue(root, TEST_LOCATION, JSONHelper.createJSONArray(this.getTestLocation()));
        } else {
            JSONHelper.putValue(root, TEST_LOCATION, JSONHelper.createJSONArray("[]"));
        }
        // JSONHelper.putValue(root, TEST_LOCATION, JSONHelper.createJSONArray(this.getTestLocation()));
        JSONHelper.putValue(root, TEST_ZOOM, this.getTestZoom());

		JSONHelper.putValue(root, MIN_SCALE, this.getMinScale());
		JSONHelper.putValue(root, MAX_SCALE, this.getMaxScale());

		JSONHelper.putValue(root, TEMPLATE_NAME, this.getTemplateName());
		JSONHelper.putValue(root, TEMPLATE_DESCRIPTION, this.getTemplateDescription());
		JSONHelper.putValue(root, TEMPLATE_TYPE, this.getTemplateType());
		JSONHelper.putValue(root, REQUEST_TEMPLATE, this.getRequestTemplate());
		JSONHelper.putValue(root, RESPONSE_TEMPLATE, this.getResponseTemplate());

		JSONHelper.putValue(root, SELECTION_SLD_STYLE, this.getSelectionSLDStyle());

    	// styles
		final JSONObject styleList = new JSONObject();
		for (final WFSSLDStyle ls : this.getSLDStyles()) {
	        final JSONObject style = new JSONObject();
	        JSONHelper.putValue(style, ID, ls.getId());
	        JSONHelper.putValue(style, NAME, ls.getName());
	        JSONHelper.putValue(style, SLD_STYLE, ls.getSLDStyle());
	        JSONHelper.putValue(styleList, ls.getId(), style);
		}
		JSONHelper.putValue(root, STYLES, styleList);

		return root.toString();
	}

	public static String getCache(String layerId) {
		return JedisManager.get(KEY + layerId);
	}
}