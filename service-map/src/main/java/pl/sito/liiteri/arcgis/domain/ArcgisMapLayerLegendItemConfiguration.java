package pl.sito.liiteri.arcgis.domain;

import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class ArcgisMapLayerLegendItemConfiguration {	
	
    private static final Logger log = LogFactory.getLogger(ArcgisMapLayerLegendItemConfiguration.class);
    public static ObjectMapper mapper = new ObjectMapper();    
    
    private static final String LABEL = "label";
    private static final String URL = "url";
    private static final String IMAGE_DATA = "imageData";
    private static final String CONTENT_TYPE = "contentType";    
    private static final String HEIGHT = "height";
    private static final String WIDTH = "width";    
        
    private String label;
    private String url;
    private String contentType;
    private String imageData;    
    private int width;
    private int height;            
    
    public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getContentType()
	{
		return contentType;
	}

	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	public String getImageData()
	{
		return imageData;
	}

	public void setImageData(String imageData)
	{
		this.imageData = imageData;
	}

	public int getWidth()
	{
		return width;
	}

	public void setWidth(int width)
	{
		this.width = width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(int heigth)
	{
		this.height = heigth;
	}

	@JsonIgnore
    public String getAsJSON() {
        try {
            return ArcgisMapLayerLegendItemConfiguration.mapper.writeValueAsString(this);
        } catch (JsonMappingException e) {
            log.error(e, "Mapping from Object to JSON String failed");
        } catch (IOException e) {
            log.error(e, "IO failed");
        }
        return null;
    }
    
    @JsonIgnore
    public static ArcgisMapLayerLegendItemConfiguration setJSON(JSONObject jsonObj) throws IOException 
    {
    	ArcgisMapLayerLegendItemConfiguration store = new ArcgisMapLayerLegendItemConfiguration();    	
    	                
        store.setLabel(jsonObj.get(LABEL).toString());
        store.setUrl(jsonObj.get(URL).toString());
        store.setImageData(jsonObj.get(IMAGE_DATA).toString());
        store.setContentType(jsonObj.get(CONTENT_TYPE).toString());
        store.setHeight(Integer.parseInt(jsonObj.get(HEIGHT).toString()));
        store.setWidth(Integer.parseInt(jsonObj.get(WIDTH).toString()));

        return store;
    }
    
    @JsonIgnore
    public static ArcgisMapLayerLegendItemConfiguration setJSON(String json) throws IOException {
    	JSONObject jsonObj = (JSONObject) JSONValue.parse(json);
    	return ArcgisMapLayerLegendItemConfiguration.setJSON(jsonObj);
    }

}