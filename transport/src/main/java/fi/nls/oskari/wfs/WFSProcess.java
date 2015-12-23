package fi.nls.oskari.wfs;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.pojo.*;
import fi.nls.oskari.work.MapLayerJobType;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.util.Arrays;
import java.util.List;

public class WFSProcess {
    private static final Logger log = LogFactory.getLogger(WFSProcess.class);

    public static BufferedImage highlight(String session, String layerId, List<String> featureIds, Double[] bbox, String srs, long zoom, long width, long height) {
        BufferedImage bufferedImage;

        String style = MapLayerJobType.HIGHLIGHT.toString();
        MapLayerJobType processType = MapLayerJobType.HIGHLIGHT;

        // get layer configuration
        WFSLayerStore layer = null;
        if(layer == null) {
            log.warn("No layer configuration", layerId);
            return null;
        }

        // create session
        SessionStore store = new SessionStore();
        store.setLanguage(null); // not much to do with highlight image
        store.setLayer(layerId, new Layer(layerId, style));
        store.getLayers().get(layerId).setHighlightedFeatureIds(featureIds);
        List<Double> bounds = Arrays.asList(bbox);
        Location location = new Location();
        location.setSrs(srs);
        location.setBbox(bounds);
        location.setZoom(zoom);
        store.setLocation(location);
        Tile mapSize = new Tile();
        mapSize.setWidth(((Long) width).intValue());
        mapSize.setHeight(((Long) height).intValue());
        store.setMapSize(mapSize);

        // create transformers
        MathTransform transformService = null;
        MathTransform transformClient = null;
        if(!location.getSrs().equals(layer.getSRSName())) {
            transformService = location.getTransformForService(layer.getCrs(), true);
            transformClient = location.getTransformForClient(layer.getCrs(), true);
        }

        // make request
        BufferedReader res = null;
        if(res == null) {
            log.warn("Request failed for layer", layer.getLayerId());
            return null;
        }

        // parse response
        FeatureCollection<SimpleFeatureType, SimpleFeature> features = null;
        if(features == null || features.size() == 0) {
            log.warn("No features", layer.getLayerId());
            return null;
        }

        log.debug("Features count", features.size());

        // edit feature geometries
        FeatureIterator<SimpleFeature> featuresIter =  features.features();
        while(featuresIter.hasNext()) {
            SimpleFeature feature = featuresIter.next();
            WFSParser.getFeatureGeometry(feature, layer.getGMLGeometryProperty(), transformClient);
        }

        // draw
        WFSImage image = new WFSImage(layer, null, style, processType.toString());
        bufferedImage = image.draw(mapSize, location, features);

        if(bufferedImage == null) {
            log.warn("Image parsing failed", layer.getLayerId());
            return null;
        }

        return bufferedImage;
    }

}
