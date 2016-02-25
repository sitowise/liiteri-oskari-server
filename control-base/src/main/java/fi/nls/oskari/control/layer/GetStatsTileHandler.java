package fi.nls.oskari.control.layer;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.control.view.GetAppSetupHandler;
import fi.nls.oskari.domain.map.stats.StatsVisualization;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.stats.VisualizationService;
import fi.nls.oskari.map.stats.VisualizationServiceIbatisImpl;
import fi.nls.oskari.util.ConversionHelper;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.PropertyUtil;
import org.apache.axiom.om.OMElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;

@OskariActionRoute("GetStatsTile")
public class GetStatsTileHandler extends ActionHandler {

    private static final Logger log = LogFactory.getLogger(GetStatsTileHandler.class);

    private final VisualizationService service = new VisualizationServiceIbatisImpl();

    final private static String PARAM_VISUALIZATION_ID = "VIS_ID";
    final private static String PARAM_VISUALIZATION_NAME = "VIS_NAME"; // name=ows:Kunnat2013
    final private static String PARAM_VISUALIZATION_FILTER_PROPERTY = "VIS_ATTR"; // attr=Kuntakoodi
    final private static String PARAM_VISUALIZATION_CLASSES = "VIS_CLASSES"; // classes=020,091|186,086,982|111,139,740
    final private static String PARAM_VISUALIZATION_VIS = "VIS_COLORS"; // vis=choro:ccffcc|99cc99|669966

    private String geoserverUrl = null;
    private String geoserverUser = null;
    private String geoserverPass = null;
    private String sldServerUrl = null;

    @Override
    public void init() {
        super.init();
        geoserverUrl = PropertyUtil.getOptional("statistics.geoserver.wms.url");
        geoserverUser = PropertyUtil.get("statistics.user");
        geoserverPass = PropertyUtil.get("statistics.password");
        sldServerUrl = PropertyUtil.getOptional("statistics.sld.server");
    }

    public void handleAction(final ActionParameters params)
            throws ActionException {
        if (log.isDebugEnabled()) {
            printParameters(params);
        }
/*
        // GetMap with post is horrible broken with hard coded GetMap xml.
        // never use it before it's fixed!!
        if (sldServerUrl == null) {
            // if SLD server is not configured, use POSTing a GetMap XML-request
            proxyWithGetMapPost(params);
        } else {
*/
            final HttpURLConnection con = getConnection(params);
            try {
                // we should post complete GetMap XML with the custom SLD to geoserver so it doesn't need to fetch it again
                // Check: http://geo-solutions.blogspot.fi/2012/04/dynamic-wms-styling-with-geoserver-sld.html
                con.setRequestMethod("GET");

                con.setDoOutput(false);
                con.setDoInput(true);
                HttpURLConnection.setFollowRedirects(false);
                con.setUseCaches(false);
                con.connect();
                //IOHelper.writeToConnection(con, SLD_HANDLER.getSLD(params));

                // read the image tile
                final byte[] presponse = IOHelper.readBytes(con.getInputStream());

                final HttpServletResponse response = params.getResponse();
                response.setContentType("image/png");
                response.getOutputStream().write(presponse, 0, presponse.length);
                response.getOutputStream().flush();
                response.getOutputStream().close();
            } catch (Exception e) {
                throw new ActionException("Couldn't proxy request to geoserver",
                        e);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
//        }
    }

    private void proxyWithGetMapPost(final ActionParameters params) throws ActionException {

        try {
            String xml = buildXML(params);

            HttpURLConnection con = IOHelper.getConnection(geoserverUrl, geoserverUser, geoserverPass);
            IOHelper.writeHeader(con, "Content-type", "text/xml");
            IOHelper.writeToConnection(con, xml);
            byte[] presponse = IOHelper.readBytes(con);

            log.debug("Image response length: " + presponse.length);
            final HttpServletResponse response = params.getResponse();
            response.setContentType("image/png");
            response.getOutputStream().write(presponse, 0, presponse.length);
            response.getOutputStream().flush();
            response.getOutputStream().close();

        } catch (Exception e) {
            throw new ActionException("Couldn't post proxy request to geoserver",
                    e);
        }
    }

    private HttpURLConnection getConnection(final ActionParameters params)
            throws ActionException {

        // copy parameters
        final HttpServletRequest httpRequest = params.getRequest();
        final StringBuilder ajaxUrl = new StringBuilder(sldServerUrl);

        ajaxUrl.append(PropertyUtil.get("statistics.sld.server.path",
                PropertyUtil.get(params.getLocale(), GetAppSetupHandler.PROPERTY_AJAXURL)));

        ajaxUrl.append("&action_route=GetStatsLayerSLD");

        StatsVisualization vis = getVisualization(params);
        if (vis == null) {
            log.info("Visualization couldn't be generated - parameters/db data missing", params);
        } else {
            // using prefetched values so we don't need to get them from db again on SLD action
            ajaxUrl.append("&");
            ajaxUrl.append(GetStatsLayerSLDHandler.PARAM_VISUALIZATION_NAME);
            ajaxUrl.append("=");
            ajaxUrl.append(vis.getLayername());

            ajaxUrl.append("&");
            ajaxUrl.append(GetStatsLayerSLDHandler.PARAM_VISUALIZATION_FILTER_PROPERTY);
            ajaxUrl.append("=");
            ajaxUrl.append(vis.getFilterproperty());

            ajaxUrl.append("&");
            ajaxUrl.append(GetStatsLayerSLDHandler.PARAM_VISUALIZATION_CLASSES);
            ajaxUrl.append("=");
            ajaxUrl.append(vis.getClasses());

            ajaxUrl.append("&");
            ajaxUrl.append(GetStatsLayerSLDHandler.PARAM_VISUALIZATION_VIS);
            ajaxUrl.append("=");

            ajaxUrl.append(vis.getVisualization());
            ajaxUrl.append(":");
            ajaxUrl.append(vis.getColors());
        }

        final StringBuffer queryString = new StringBuffer();
        for (Object key : httpRequest.getParameterMap().keySet()) {
            String keyStr = (String) key;
            if(keyStr.equals(GetStatsLayerSLDHandler.PARAM_VISUALIZATION_NAME) ||
                    keyStr.equals(GetStatsLayerSLDHandler.PARAM_VISUALIZATION_FILTER_PROPERTY) ||
                    keyStr.equals(GetStatsLayerSLDHandler.PARAM_VISUALIZATION_CLASSES) ||
                    keyStr.equals(GetStatsLayerSLDHandler.PARAM_VISUALIZATION_VIS)) {
                // ignore SLD-paremeters
                continue;
            }
            queryString.append("&");
            queryString.append(keyStr);
            queryString.append("=");
            queryString.append(params.getHttpParam(keyStr));
        }
        try {
            final String url = geoserverUrl + queryString + "&SLD=" + URLEncoder.encode(ajaxUrl.toString(), "UTF-8");
            log.debug("Getting stats tile from url:", url);
            return IOHelper.getConnection(url, geoserverUser, geoserverPass);
        } catch (Exception e) {
            throw new ActionException(
                    "Couldnt get connection to geoserver", e);
        }
    }

    private StatsVisualization getVisualization(final ActionParameters params) {
        final int visId = ConversionHelper.getInt(
                params.getHttpParam(PARAM_VISUALIZATION_ID), -1);

        log.debug("visid: " + visId);

        log.debug("PARAM_VISUALIZATION_CLASSES: " + params.getHttpParam(PARAM_VISUALIZATION_CLASSES));
        log.debug("PARAM_VISUALIZATION_NAME: " + params.getHttpParam(PARAM_VISUALIZATION_NAME));
        log.debug("PARAM_VISUALIZATION_FILTER_PROPERTY: " + params.getHttpParam(PARAM_VISUALIZATION_FILTER_PROPERTY));
        log.debug("PARAM_VISUALIZATION_VIS: " + params.getHttpParam(PARAM_VISUALIZATION_VIS, ""));

        return service.getVisualization(
                visId,
                params.getHttpParam(PARAM_VISUALIZATION_CLASSES),
                params.getHttpParam(PARAM_VISUALIZATION_NAME),
                params.getHttpParam(PARAM_VISUALIZATION_FILTER_PROPERTY),
                params.getHttpParam(PARAM_VISUALIZATION_VIS, "")
        );

    }

    /**
     * <?xml version="1.0" encoding="UTF-8"?>
     * <ogc:GetMap xmlns:ogc="http://www.opengis.net/ows"
     * xmlns:gml="http://www.opengis.net/gml"
     * version="1.1.1" service="WMS">
     * <StyledLayerDescriptor version="1.0.0">
     * <NamedLayer>
     * <Name>topp:states</Name>
     * <NamedStyle><Name>population</Name></NamedStyle>
     * </NamedLayer>
     * </StyledLayerDescriptor>
     * <BoundingBox srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
     * <gml:coord><gml:X>-130</gml:X><gml:Y>24</gml:Y></gml:coord>
     * <gml:coord><gml:X>-55</gml:X><gml:Y>50</gml:Y></gml:coord>
     * </BoundingBox>
     * <Output>
     * <Format>image/png</Format>
     * <Size><Width>550</Width><Height>250</Height></Size>
     * </Output>
     * </ogc:GetMap>
     * <p/>
     * Params are:
     * - LAYERID:519
     * - LAYERS:oskari:kunnat2013
     * - TRANSPARENT:TRUE
     * - FORMAT:image/png
     * - SERVICE:WMS
     * - VERSION:1.1.1
     * - REQUEST:GetMap
     * - STYLES:
     * - SRS:EPSG:3067
     * - VIS_ID:-1
     * - VIS_NAME:oskari:kunnat2013
     * - VIS_ATTR:kuntakoodi
     * - VIS_CLASSES:035,043,052,060,062,065,097,099,103,151,181,216,231,265,280,291,295,300,312,318,421,435,438,583,601,687,732,759,766,833,838,844,941,981|020,005,010,049,051,061,071,108,139,179,182,202,211,224,232,233,235,241,245,257,276,301,305,399,400,405,408,410,418,420,422,423,426,444,430,433,434,478,494,499,500,505,508,529,535,536,541,543,564,445,599,604,638,615,636,678,680,790,743,746,748,791,753,778,851,853,858,893,895,905,927,977,980,992|016,019,069,075,079,086,111,091,098,106,109,140,142,153,164,165,167,171,176,186,208,213,236,240,244,260,272,290,297,402,425,481,491,507,532,562,577,593,598,609,611,624,710,694,698,704,734,740,749,777,837,859,887,908,092,989|050,077,078,102,148,172,204,205,214,261,263,271,273,285,286,287,316,398,442,560,563,309,578,614,625,681,684,686,758,765,886,915,925,935,976|105,177,620,781,785
     * - VIS_COLORS:choro:eff3ff|bdd7e7|6baed6|3182bd|08519c
     * - BBOX:-621248,6045776,1661248,8454224
     * - WIDTH:1114
     * - HEIGHT:1176
     */
    private String buildXML(final ActionParameters params) {
        // TODO: maybe create GetMap XML somewhere on service level and inject the SLD part here?
        Element getMapElement = null;
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            getMapElement = doc.createElement("ogc:GetMap");

            Attr xmlns_ogc_attr = doc.createAttribute("xmlns:ogc");
            xmlns_ogc_attr.setValue("http://www.opengis.net/ows");
            getMapElement.setAttributeNode(xmlns_ogc_attr);

            Attr xmlns_gml_attr = doc.createAttribute("xmlns:gml");
            xmlns_gml_attr.setValue("http://www.opengis.net/gml");
            getMapElement.setAttributeNode(xmlns_gml_attr);

            Attr xversion_attr = doc.createAttribute("version");
            xversion_attr.setValue(params.getHttpParam("VERSION", "1.1.1"));
            getMapElement.setAttributeNode(xversion_attr);

            Attr service_attr = doc.createAttribute("service");
            service_attr.setValue(params.getHttpParam("SERVICE", "WMS"));
            getMapElement.setAttributeNode(service_attr);

            doc.appendChild(getMapElement);

            // styledLayerDescription elements
            // FIXME: use code from GetStatsLayerSLD/getSLD(params) to create!!
            Element styledLayerDescription = doc.createElement("StyledLayerDescriptor");

            Attr version_attr = doc.createAttribute("version");
            version_attr.setValue("1.0.0");
            styledLayerDescription.setAttributeNode(version_attr);
            getMapElement.appendChild(styledLayerDescription);

            // NamedLayer elements
            Element namedLayer = doc.createElement("NamedLayer");
            styledLayerDescription.appendChild(namedLayer);

            // Name element
            Element name = doc.createElement("Name");
            name.appendChild(doc.createTextNode(params.getRequiredParam("LAYERS")));
            namedLayer.appendChild(name);

            Element namedStyle = doc.createElement("NamedStyle");
            Element name2 = doc.createElement("Name");
            name2.appendChild(doc.createTextNode("polygon"));
            namedStyle.appendChild(name2);
            namedLayer.appendChild(namedStyle);

            Element boundingBox = doc.createElement("BoundingBox");
            Attr srsName_attr = doc.createAttribute("srsName");
            // FIXME: setup SRS correctly!!
            // SRS:EPSG:3067
            srsName_attr.setValue("http://www.opengis.net/gml/srs/epsg.xml#3067");
            boundingBox.setAttributeNode(srsName_attr);


            getMapElement.appendChild(boundingBox);

            // FIXME: setup bbox correctly!!
            // BBOX:-621248,6045776,1661248,8454224
            Element gml_coord = doc.createElement("gml:coord");
            boundingBox.appendChild(gml_coord);

            Element gml_x = doc.createElement("gml:x");
            gml_x.appendChild(doc.createTextNode("-2132672"));
            gml_coord.appendChild(gml_x);

            Element gml_y = doc.createElement("gml:y");
            gml_y.appendChild(doc.createTextNode("5721680"));
            gml_coord.appendChild(gml_y);


            Element gml_coord2 = doc.createElement("gml:coord");
            boundingBox.appendChild(gml_coord2);

            Element gml_x2 = doc.createElement("gml:x");
            gml_x2.appendChild(doc.createTextNode("3172672"));
            gml_coord2.appendChild(gml_x2);

            Element gml_y2 = doc.createElement("gml:y");
            gml_y2.appendChild(doc.createTextNode("8778320"));
            gml_coord2.appendChild(gml_y2);


            Element output = doc.createElement("Output");
            getMapElement.appendChild(output);

            Element format = doc.createElement("Format");
            format.appendChild(doc.createTextNode(params.getHttpParam("FORMAT", "image/png")));
            output.appendChild(format);

            Element size = doc.createElement("Size");
            output.appendChild(size);

            Element width = doc.createElement("Width");
            width.appendChild(doc.createTextNode(params.getRequiredParam("WIDTH")));
            size.appendChild(width);

            Element height = doc.createElement("height");
            height.appendChild(doc.createTextNode(params.getRequiredParam("HEIGHT")));
            size.appendChild(height);

            final Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer();

            final DOMSource source = new DOMSource(doc);
            final StringWriter outWriter = new StringWriter();
            final StreamResult result = new StreamResult(outWriter);
            transformer.transform(source, result);

            String transformedResponse = outWriter.toString();
            log.debug("GetMap.xml: ", transformedResponse);
            return transformedResponse;

        } catch (Exception e) {
            log.error(e, "Couldn't create GetMap XML-request");
        }
        return null;
    }

    private String getSLD(final ActionParameters params)
            throws ActionException {

        final String PARAM_LANGUAGE = "lang";
        final String PARAM_MODE = "mode";
        final String MODE_XML = "XML";

        printParameters(params);

        final String lang = params.getHttpParam(PARAM_LANGUAGE, params
                .getLocale().getLanguage());

        final boolean modeXML = MODE_XML.equals(params.getHttpParam(PARAM_MODE,
                "").toUpperCase());

        final StatsVisualization vis = getVisualization(params);

        if (vis == null) {
            throw new ActionParamsException("Couldn't get requested visualization");
        }

        log.debug("Found visualization:", vis);
        final OMElement xml = service.getXML(vis, lang);
        try {
            if (modeXML) {
                return xml.toString();
            } else {
                String xmlString = service.transform(xml, service.getDefaultXSLT());
                log.debug("xmlString: " + xmlString);
                return xmlString;
            }
        } catch (Exception e) {
            throw new ActionException("Unable to create SLD", e);
        }
    }

    private void printParameters(ActionParameters params) {
        Enumeration e = params.getRequest().getParameterNames();

        log.debug("GetStatsTile parameters:");
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            log.debug("Key: " + key);
            String[] values = params.getRequest().getParameterValues(key);

            for (String value : values) {
                log.debug("  value: " + value);
            }
        }
    }

}