package fi.nls.oskari.pojo.style;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonSubTypes.Type; 
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.JsonMappingException;

import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.pojo.WFSCustomStyleStore;
import fi.nls.oskari.transport.TransportService;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
	@Type(value = WFSCustomStyleStore.class, name = "simple"),
	@Type(value = UniqueValueCustomStyleStore.class, name = "uniqueValue"),
	@Type(value = GroupCustomStyleStore.class, name = "group")	
})
public abstract class CustomStyleStore {

	    private static final Logger log = LogFactory.getLogger(CustomStyleStore.class);

	    public static final String KEY = "CustomStyle_";

	    protected static final String TYPE = "type";
	    
	    public static final String HIGHLIGHT_FILL_COLOR = "#FAEBD7";
	    public static final String HIGHLIGHT_BORDER_COLOR = "#000000";
	    public static final String HIGHLIGHT_STROKE_COLOR = "#FAEBD7";
	    public static final String HIGHLIGHT_DOT_COLOR = "#FAEBD7";

	    public static final String GEOMETRY = "defaultGeometry";
	    public static final String FILL_PATTERN_PARTIAL = "fill_pattern_partial";
	    public static final String LINE_STYLE_PARTIAL = "line_style_partial";
	    public static final String BORDER_STYLE_PARTIAL = "border_style_partial";

	    public static final String LINE_STYLE_PARTIAL_LINECAP = "<CssParameter name=\"stroke-linecap\">stroke_linecap</CssParameter>";
	    public static final String LINE_STYLE_PARTIAL_DASHARRAY = "<CssParameter name=\"stroke-dasharray\">5 2</CssParameter>";
	    public static final String BORDER_STYLE_PARTIAL_DASHARRAY = "<CssParameter name=\"stroke-dasharray\">5 2</CssParameter>";

	    public static final String FILL_PATTERN_PARTIAL_DEFAULT = "<CssParameter name=\"fill\">fill_color</CssParameter>";
	    public static final String FILL_PATTERN_PARTIAL_MARK = "<GraphicFill>\n" +
	            "    <Graphic>\n" +
	            "        <Mark>\n" +
	            "            <WellKnownName>shape://fill_mark</WellKnownName>\n" +
	            "            <Stroke>\n" +
	            "                <CssParameter name=\"stroke\">fill_color</CssParameter>\n" +
	            "                <CssParameter name=\"stroke-width\">fill_stroke_width</CssParameter>\n" +
	            "            </Stroke>\n" +
	            "        </Mark>\n" +
	            "        <Size>fill_size</Size>\n" +
	            "    </Graphic>\n" +
	            "</GraphicFill>";


	    private String client;
	    private String layerId;
	    private String type;	   
	    protected String geometry;

	    public String getClient() {
	        return client;
	    }

	    public void setClient(String client) {
	        this.client = client;
	    }

	    public String getLayerId() {
	        return layerId;
	    }

	    public void setLayerId(String layerId) {
	        this.layerId = layerId;
	    }
	    
	    public String getGeometry() {
	        return geometry;
	    }

	    public void setGeometry(String geometry) {
	        this.geometry = geometry;
	    }
	    
	    public String getType() {
	        return type;
	    }

	    public void setType(String type) {
	        this.type = type;
	    }

	    /**
	     * Saves into redis
	     *
	     * @return <code>true</code> if saved a valid session; <code>false</code>
	     *         otherwise.
	     */
	    public final void save() {
	        JedisManager.setex(KEY + client + "_" + layerId, 86400, getAsJSON());
	    }

	    /**
	     * Transforms object to JSON String
	     *
	     * @return JSON String
	     */
	    @JsonIgnore
	    public final String getAsJSON() {
	        try {	        	
	            return TransportService.mapper.writeValueAsString(this);
	        } catch (JsonGenerationException e) {
	            log.error(e, "JSON Generation failed");
	        } catch (JsonMappingException e) {
	            log.error(e, "Mapping from Object to JSON String failed");
	        } catch (IOException e) {
	            log.error(e, "IO failed");
	        }
	        return null;
	    }

	    /**
	     * Creates store from cache
	     *
	     * @param client
	     * @param layerId
	     * @return object
	     */
	    @JsonIgnore
	    public static CustomStyleStore create(String client, String layerId)
	            throws IOException {
	        String json = getCache(client, layerId);
	        if(json == null) {
	            return null;
	        }

	        return setJSON(json);
	    }

	    /**
	     * Transforms JSON String to object
	     *
	     * @param json
	     * @return object
	     */
	    @JsonIgnore
	    public static CustomStyleStore setJSON(String json)
	            throws IOException {
	        return TransportService.mapper.readValue(json,
	        		CustomStyleStore.class);
	    }

	    /**
	     * Gets saved session from redis
	     *
	     * @param client
	     * @param layerId
	     * @return style as JSON String
	     */
	    @JsonIgnore
	    public static String getCache(String client, String layerId) {
	        return JedisManager.get(KEY + client + "_" + layerId);
	    }



}
