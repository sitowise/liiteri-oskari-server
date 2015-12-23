package fi.nls.oskari.arcgis;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.pojo.WFSCustomStyleStore;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.utils.HttpHelper;
import fi.nls.oskari.work.ArcGisMapLayerJob;

import org.apache.commons.codec.binary.Base64;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.map.MapViewport;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.sld.SLDConfiguration;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import fi.nls.oskari.log.Logger;
import fi.nls.oskari.pojo.ArcGisFeature;
import fi.nls.oskari.pojo.ArcGisLayerStore;
import fi.nls.oskari.pojo.Location;
import fi.nls.oskari.pojo.Tile;
import fi.nls.oskari.pojo.WFSLayerStore;
import fi.nls.oskari.pojo.style.CustomStyleStore;

/**
 * Image drawing for WFS layers 
 */
public class ArcGisImage {
    private static final Logger log = LogFactory.getLogger(ArcGisImage.class);

    // Maybe hazardous because static ImageIO setter (changes this setting for all!)
    // NOT using disk for cache [ http://docs.oracle.com/javase/7/docs/api/javax/imageio/ImageIO.html#setUseCache(boolean) ]
    static {
        ImageIO.setUseCache(false);
    }

    public static final String KEY = "WFSImage_";
    public static final String PREFIX_CUSTOM_STYLE = "oskari_custom";

    public static final String STYLE_DEFAULT = "default";
    public static final String STYLE_HIGHLIGHT = "highlight";

    public static final String GEOM_TYPE_PLACEHOLDER = "wfsGeometryType";

    private Location location; // location of the tile (modified if not map)
    private ArrayList<ArcGisFeature> features;
    private int imageWidth = 0;
    private int imageHeight = 0;
    private double bufferSize = 0.0d;
    private int bufferedImageWidth = 0;
    private int bufferedImageHeight = 0;

    CustomStyleStore customStyle;
    private boolean isHighlight = false;
    private boolean isTile = false;
    private WFSLayerStore layer;
    private ArcGisLayerStore arcGisLayer;
    private List<ArcGisLayerStore> arcGisLayers;
    private String token;

    /**
     * Constructor for image of certain layer and style
     *
     * @param layer
     * @param styleName
     */
    public ArcGisImage(WFSLayerStore layer, 
    		ArcGisLayerStore arcGisLayer,
    		List<ArcGisLayerStore> arcGisLayers, 
    		String client, 
    		String styleName, 
    		String highlightStyleName,
    		String token) {
        if(layer == null || styleName == null) {
            log.error("Failed to construct image (undefined params)");
            return;
        }
        
        this.layer = layer;
        this.arcGisLayer = arcGisLayer;
        this.arcGisLayers = arcGisLayers;
        this.token = token;

        // check if tile buffer is given
        String tileBufferKey;
        if(styleName.startsWith(PREFIX_CUSTOM_STYLE)) {
            tileBufferKey = PREFIX_CUSTOM_STYLE;
        } else {
            tileBufferKey = styleName;
        }
        if(layer.getTileBuffer() != null && layer.getTileBuffer().containsKey(tileBufferKey)) {
            bufferSize = layer.getTileBuffer().get(tileBufferKey);
        }
        log.debug(tileBufferKey, "=", bufferSize);

        // TODO: possibility to change the custom style store key to sessionID (it is hard without connection to get client)
        if(styleName.startsWith(PREFIX_CUSTOM_STYLE) && client != null) {
            try {
                this.customStyle = CustomStyleStore.create(client, layer.getLayerId());
                if(this.customStyle == null) {
                    log.error("CustomStyleStore not created", client, layer.getLayerId());
                    return;
                }
                this.customStyle.setGeometry(layer.getGMLGeometryProperty().replaceAll("^[^_]*:", "")); // set the geometry name

                if(highlightStyleName == null) {
                } else {
                    isHighlight = true;
                }
            } catch(Exception e) {
                log.error(e, "JSON parsing failed for WFSCustomStyleStore");
                return;
            }
        } else if(highlightStyleName == null) {
        } else {
            isHighlight = true;
        }
    }

    /**
     * Gets bufferedImage from cache (persistant)
     *
     * @param layerId
     * @param srs
     * @param bbox
     * @param zoom
     * @return buffered image from cache
     */
    public static BufferedImage getCache(String layerId, String styleName, String srs, Double[] bbox, long zoom) {
        return getCache(layerId, styleName, srs, bbox, zoom, true);
    }

    /**
     * Gets bufferedImage from cache
     *
     * @param layerId
     * @param srs
     * @param bbox
     * @param zoom
     * @param persistent
     * @return buffered image from cache
     */
    public static BufferedImage getCache(String layerId,
                                         String styleName,
                                         String srs,
                                         Double[] bbox,
                                         long zoom,
                                         boolean persistent) {
        if(layerId == null ||
                styleName == null ||
                srs == null ||
                bbox.length != 4) {
            log.error("Cache key couldn't be created");
            return null;
        }

        // no persistent cache for custom styles
        if(styleName.startsWith(PREFIX_CUSTOM_STYLE) && persistent) {
            return null;
        }

        String sBbox = bbox[0] + "-" + bbox[1] + "-" + bbox[2]+ "-" + bbox[3];
        String sKey = KEY + layerId + "_" + styleName + "_"  + srs + "_" + sBbox + "_" + zoom;
        if(!persistent) {
            sKey = sKey + "_temp";
        }

        byte[] key = sKey.getBytes();
        byte[] bytes = JedisManager.get(key);
        if(bytes != null)
            return bytesToImage(bytes);
        return null;
    }

    /**
     * Sets bufferedImage to cache
     *
     * @param layerId
     * @param srs
     * @param bbox
     * @param zoom
     * @param persistent
     * @return buffered image from cache
     */
    public static void setCache(BufferedImage bufferedImage,
                                String layerId,
                                String styleName,
                                String srs,
                                Double[] bbox,
                                long zoom,
                                boolean persistent) {
        if(layerId == null ||
                styleName == null ||
                srs == null ||
                bbox.length != 4) {
            log.error("Cache key couldn't be created");
            return;
        }

        // no persistent cache for custom styles
        if(styleName.startsWith(PREFIX_CUSTOM_STYLE)) {
            persistent = false;
        }

        byte[] byteImage = imageToBytes(bufferedImage);
        String sBbox = bbox[0] + "-" + bbox[1] + "-" + bbox[2]+ "-" + bbox[3];
        String sKey = KEY + layerId + "_" + styleName + "_" + srs + "_" + sBbox + "_" + zoom;
        if(!persistent) {
            sKey = sKey + "_temp";
        }

        byte[] key = sKey.getBytes();

        JedisManager.setex(key, 86400, byteImage);
    }

    /**
     * Transforms bufferedImage to byte[]
     *
     * @param bufferedImage
     * @return image
     */
    public static byte[] imageToBytes(BufferedImage bufferedImage) {
        if(bufferedImage == null) {
            log.error("No image given");
            return null;
        }

        ByteArrayOutputStream byteaOutput = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "png", byteaOutput);
            byteaOutput.flush();
            byteaOutput.close();
        } catch (Exception e) {
            log.error(e, "Image could not be written into stream");
        }
        return byteaOutput.toByteArray();
    }

    /**
     * Transforms byte[] to BufferedImage
     *
     * @param byteImage
     * @return image
     */
    public static BufferedImage bytesToImage(byte[] byteImage) {
        BufferedImage bufferedImage = null;
        ByteArrayInputStream byteaInput = null;
        if(byteImage != null) {
            byteaInput = new ByteArrayInputStream(byteImage);
            try {
                bufferedImage = ImageIO.read(byteaInput);
                byteaInput.close();
            } catch (Exception e) {
                log.error(e, "Image could not be read into stream");
            }            
        }
        return bufferedImage;
    }
    
    public static BufferedImage streamToImage(InputStream stream) {
        BufferedImage bufferedImage = null;
            try {            
                bufferedImage = ImageIO.read(stream);
            } catch (Exception e) {
                log.error(e, "Image could not be read into stream");
            }            
        return bufferedImage;
    }

    /**
     * Converts byte[] to Base64 formatted String
     *
     * @param byteImage
     * @return base64
     */
    public static String bytesToBase64(byte[] byteImage) {
        return new String(Base64.encodeBase64(byteImage));
    }


    /**
     * Creates a image of the WFS layer's data
     *
     * @param tile
     * @param location
     * @param features
     *
     * @return image
     */
    public BufferedImage draw(Tile tile,
                              Location location,
                              ArrayList<ArcGisFeature> features) {
        return draw(tile, location, null, features);
    }

    /**
     * Creates a image of the WFS layer's data
     *
     * @param tile
     * @param location
     * @param bounds
     * @param features
     *
     * @return image
     */
    public BufferedImage draw(Tile tile,
                              Location location,
                              List<Double> bounds,
                              ArrayList<ArcGisFeature> features) {

        this.imageWidth = tile.getWidth();
        this.imageHeight = tile.getHeight();

        if(bounds == null) {
            this.location = location;
        } else {
            this.location = new Location(location.getSrs());
            this.location.setBbox(bounds);

            // enlarge if tile and buffer is defined
            this.isTile = true;
            if(bufferSize != 0.0d) {
                this.bufferedImageWidth = imageWidth+(int)(imageWidth*bufferSize);
                this.bufferedImageHeight = imageHeight+(int)(imageWidth*bufferSize);
            }
        }

        this.features = features;

        if (imageWidth == 0 ||
                imageHeight == 0 ||
                this.location == null ||
                features == null) {
            log.warn("Not enough information to draw");
            log.warn(imageWidth);
            log.warn(imageHeight);
            log.warn(location);
            log.warn(features.isEmpty());
            return null;
        }

        return this.draw();
    }

    /**
     * Creates a image of the WFS layer's data
     *
     * @return image
     */
    private BufferedImage draw() 
    {
    	//TODO: feature    	
    	
//        MapContent content = new MapContent();
//        MapViewport viewport = new MapViewport();
//
        ReferencedEnvelope bounds = location.getEnvelope();

        Rectangle screenArea;
        if(isTile && bufferSize != 0.0d) {
            double width = (location.getRight() - location.getLeft())/2 * bufferSize;
            double height = (location.getTop() - location.getBottom())/2 * bufferSize;
            bounds = location.createEnlargedEnvelope(width, height);
            screenArea = new Rectangle(0, 0, bufferedImageWidth, bufferedImageHeight);
        } else {
            screenArea = new Rectangle(0, 0, imageWidth, imageHeight); // image size
        }
        
        //SRS
        String mapSrs = location.getSrs();
        String layerSrs = layer.getSRSName();
        //dynamic layers

    	String url = layer.getURL() + "/export";
    	String payload = ArcGisCommunicator.createImageRequestPayload(layer, arcGisLayer, arcGisLayers, screenArea, bounds, mapSrs, token);
    	
    	if (isHighlight) 
    	{
    		WFSCustomStyleStore highlightStyle = getDefaultHighlightStyle();		
    		String stylePayload = ArcGisCommunicator.createHighlightStyleRequestPayload(layer, arcGisLayer, arcGisLayers, features, highlightStyle);
    		payload += stylePayload;
    	}
    	else if (this.customStyle != null)
    	{
        	String stylePayload = ArcGisCommunicator.createStyleRequestPayload(layer, arcGisLayer, arcGisLayers, customStyle);
        	payload += stylePayload;
    	}
    	
    	BufferedInputStream response = HttpHelper.postRequestStream(url, "application/x-www-form-urlencoded", payload, layer.getUsername(), layer.getPassword());
    	//BufferedInputStream response = HttpHelper.getRequestStream(url + payload, "", layer.getUsername(), layer.getPassword());
    	return streamToImage(response);
//
//        viewport.setCoordinateReferenceSystem(crs);
//        viewport.setScreenArea(screenArea);
//        viewport.setBounds(bounds);
//        viewport.setMatchingAspectRatio(true);
//
//        if(features.size() > 0) {
//            Layer featureLayer = new FeatureLayer(features, style);
//            content.addLayer(featureLayer);
//        }
//
//        content.setViewport(viewport);
//
//        return saveImage(content);
    }

    private WFSCustomStyleStore getDefaultHighlightStyle() {
		WFSCustomStyleStore result = new WFSCustomStyleStore();
		
		result.setFillColor("#f5af3c");
		result.setFillPattern(-1);
		
		result.setBorderDasharray("");
		result.setBorderColor("#000000");
		result.setBorderWidth(2);
		
		result.setStrokeColor("#f5af3c");
		result.setStrokeDasharray("");
		result.setStrokeWidth(2);
		
		result.setDotColor("#f5af3c");
		result.setDotSize(2);
		result.setDotShape(5);
		
		return result;
	}    

	/**
     * Draws map content data into image
     *
     * @param content
     * @return image
     */
    private BufferedImage saveImage(MapContent content) {
        BufferedImage image;
        if(isTile && bufferSize != 0.0d) {
            image = new BufferedImage(bufferedImageWidth,
                    bufferedImageHeight,
                    BufferedImage.TYPE_4BYTE_ABGR);
        } else {
            image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_4BYTE_ABGR);
        }

        GTRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(content);

        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if(isTile && bufferSize != 0.0d) {
            renderer.paint(g, new Rectangle(bufferedImageWidth,
                    bufferedImageHeight),
                    content.getViewport().getBounds());
            try {
                image = image.getSubimage((int)(imageWidth*bufferSize)/2,
                        (int)(imageWidth*bufferSize)/2,
                        imageWidth,
                        imageHeight);
            } catch(Exception e) {
                log.error(e, "Image cropping failed");
            }
        } else {
            renderer.paint(g, new Rectangle(imageWidth, imageHeight), content.getViewport().getBounds());
        }

        content.dispose();
        return image;
    }

}
