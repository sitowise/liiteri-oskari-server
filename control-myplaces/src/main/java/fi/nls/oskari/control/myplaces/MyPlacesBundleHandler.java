package fi.nls.oskari.control.myplaces;

import fi.mml.map.mapwindow.service.db.MyPlacesService;
import fi.mml.map.mapwindow.service.db.MyPlacesServiceIbatisImpl;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.*;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.MyPlaceCategory;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.myplaces.domain.ProxyRequest;
import fi.nls.oskari.map.myplaces.service.GeoServerProxyService;
import fi.nls.oskari.map.userowndata.GisDataDbService;
import fi.nls.oskari.map.userowndata.GisDataDbServiceImpl;
import fi.nls.oskari.map.userowndata.GisDataRoleSettingsDbService;
import fi.nls.oskari.map.userowndata.GisDataRoleSettingsDbServiceImpl;
import fi.nls.oskari.map.userowndata.GisDataService;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.*;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import java.io.IOException;
import java.util.*;

/**
 * Handles and validates WFS-T traffic from frontend myplaces2 bundle to geoserver.
 */
@OskariActionRoute("MyPlaces")
public class MyPlacesBundleHandler extends ActionHandler {

    private final static Logger log = LogFactory.getLogger(MyPlacesBundleHandler.class);
    private static final String TAG_TRANSACTION = "Transaction";
    private static final String TAG_GETFEATURE = "GetFeature";
    private static final String TAG_INSERT = "Insert";
    private static final String TAG_UPDATE = "Update";
    private static final String TAG_DELETE = "Delete";
    private static final String TAG_TRANSACTION_RESPONSE = "TransactionResponse";
    private static final String TAG_FEATURE_COLLECTION = "FeatureCollection";
    private static final QName ATTR_TYPENAME = new QName("typeName");
    private static final QName ATTR_FID = new QName("fid");

    private static final String TYPE_CATEGORY = "feature:categories";
    private static final String TYPE_PLACE = "feature:my_places";

    private AXIOMXPath XPATH_MYPLACE_INSERT_CATEGORYID = null;
    private AXIOMXPath XPATH_MYPLACE_INSERT_UUID = null;
    private AXIOMXPath XPATH_MYPLACE = null;
    private AXIOMXPath XPATH_CATEGORY = null;

    private AXIOMXPath XPATH_MODIFY_FEATURE = null;
    private AXIOMXPath XPATH_GETFEATURE_PROPERTY_FILTER = null;

    private MyPlacesService service = null;
    private static GeoServerProxyService proxyService = null;
    
    //TODO: remove direct dependency to database
    private final static GisDataDbService gisDataService = new GisDataDbServiceImpl();
    private final GisDataService gisService = GisDataService.getInstance();

    private final String MY_PLACES_NAMESPACE = PropertyUtil.get("myplaces.xmlns", "http://www.oskari.org");

    private final Set<String> ALLOWED_MAIN_LEVEL_TAGS = new HashSet<String>();

    public void setMyPlacesService(final MyPlacesService service) {
        this.service = service;
    }
    public void setGeoServerProxyService(final GeoServerProxyService proxyService) {
        this.proxyService = proxyService;
    }
    
    private String dbOperation;
    private String deleteDataId;
    
    
    @Override
    public void init() {
        super.init();
        if(service == null) {
            setMyPlacesService(new MyPlacesServiceIbatisImpl());
        }
        if(proxyService == null) {
            setGeoServerProxyService(new GeoServerProxyService());
        }
        ALLOWED_MAIN_LEVEL_TAGS.add("GetFeature");
        ALLOWED_MAIN_LEVEL_TAGS.add("Transaction");
        try {
            XPATH_MYPLACE_INSERT_CATEGORYID = new AXIOMXPath("//wfs:Transaction/wfs:Insert/feature:my_places/feature:category_id");
            XPATH_MYPLACE_INSERT_CATEGORYID.addNamespace("wfs", "http://www.opengis.net/wfs");
            XPATH_MYPLACE_INSERT_CATEGORYID.addNamespace("feature", MY_PLACES_NAMESPACE);

            XPATH_MYPLACE_INSERT_UUID = new AXIOMXPath("//wfs:Transaction/wfs:Insert/feature:my_places/feature:uuid");
            XPATH_MYPLACE_INSERT_UUID.addNamespace("wfs", "http://www.opengis.net/wfs");
            XPATH_MYPLACE_INSERT_UUID.addNamespace("feature", MY_PLACES_NAMESPACE);

            XPATH_MYPLACE = new AXIOMXPath("//wfs:Transaction/*/feature:my_places");
            XPATH_MYPLACE.addNamespace("wfs", "http://www.opengis.net/wfs");
            XPATH_MYPLACE.addNamespace("feature", MY_PLACES_NAMESPACE);

            XPATH_CATEGORY = new AXIOMXPath("//wfs:Transaction/*/feature:categories");
            XPATH_CATEGORY.addNamespace("wfs", "http://www.opengis.net/wfs");
            XPATH_CATEGORY.addNamespace("feature", MY_PLACES_NAMESPACE);

            XPATH_MODIFY_FEATURE = new AXIOMXPath("//wfs:Transaction/*/ogc:Filter/ogc:FeatureId[@fid]");
            XPATH_MODIFY_FEATURE.addNamespace("wfs", "http://www.opengis.net/wfs");
            XPATH_MODIFY_FEATURE.addNamespace("ogc", "http://www.opengis.net/ogc");

            // wfs:GetFeature/wfs:Query/ogc:Filter/ogc:PropertyIsEqualTo <- might also have "Filter/And/Prop"
            XPATH_GETFEATURE_PROPERTY_FILTER = new AXIOMXPath("//ogc:PropertyIsEqualTo");
            XPATH_GETFEATURE_PROPERTY_FILTER.addNamespace("ogc", "http://www.opengis.net/ogc");

        } catch (Exception ex) {
            log.error(ex, "Error creating xpath");
        }
    }

    public void handleAction(ActionParameters params) throws ActionException {

    	String sharedParam = params.getHttpParam("shared", "none");
    	String ownParam = params.getHttpParam("own", "none");
    	dbOperation = "";
    	deleteDataId = "";
    	
        final HttpServletRequest request = params.getRequest();
        final OMElement doc = XmlHelper.parseXML(readPayload(request));

        // check permissions
        boolean isGet = validateRequest(params, doc);
        
        //code related to shared...
        OMElement filteredWFSRequest = null;
        if (isGet && !ownParam.equals("my_places")) {
	        if (sharedParam.equals("none")) {
	        	String typeParam = "categories";
	        	if (ownParam.equals("my_places")) {
	        		typeParam = "my_place";
	        	}
	        	List<UserGisData> unexpiredCategories = service.getUnexpiredMyPlaceLayers(params.getUser().getId());
	        	filteredWFSRequest = addFiltersToWFSRequest(doc, unexpiredCategories, typeParam);
	        } else {
	        	List<UserGisData> sharedCategories = service.getSharedMyPlaceLayers(params.getUser().getId());
	        	filteredWFSRequest = addFiltersToWFSRequest(doc, sharedCategories, sharedParam);
	        }
        }
        
        // create proxy request
        final ProxyRequest req = new ProxyRequest();
        final Enumeration<String> parmNames = request.getParameterNames();
        while (parmNames.hasMoreElements()) {
            final String key = RequestHelper.cleanString(parmNames.nextElement());
            req.addParam(key, params.getHttpParam(key));
        }
        
        // myplaces needs geoserver auth
        req.setUrl(PropertyUtil.get("myplaces.ows.url"));
        req.setUserName(PropertyUtil.get("myplaces.user"));
        req.setPassword(PropertyUtil.get("myplaces.password"));

        final String methodName = request.getMethod();
        req.setMethod(methodName);
        if ("POST".equals(methodName)) {
            for (Enumeration<String> e = request.getHeaderNames(); e
                    .hasMoreElements();) {
                final String key = e.nextElement().toString();
                final String value = request.getHeader(key);
                req.addHeader(key,  Jsoup.clean(value, Whitelist.none()));
            }
            
            if (isGet && !ownParam.equals("my_places")) {
            	req.setPostData(XmlHelper.toString(filteredWFSRequest));
            } else {
            	req.setPostData(XmlHelper.toString(doc));
            }
        }
        try {
        	String response = "";
        	if (isGet && !ownParam.equals("my_places") && filteredWFSRequest == null) {
        		response = "";
        	} else {
        		response = proxyService.proxy(req);
        	}
        	
        	if(dbOperation.equals("INSERT_CATEGORY")) {
        		//create reference to user layer in oskari_user_gis_data table
                String dataId = getDataIdFromResponse(response);
                try {
        			gisService.insertUserGisData(params.getUser(), dataId, "MY_PLACES", null);
        		} catch (Exception e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
        	} else if (dbOperation.equals("DELETE_CATEGORY")) {
        		//remove rows related to this analysis in oskari_user_gis_data table
                gisDataService.deleteGisData(deleteDataId, "MY_PLACES", params.getUser().getId(), "false");
        	} else if (dbOperation.equals("GET_CATEGORIES") && !response.equals("")) {
        		//add download_service_url parameter to categories in the response
        		response = addDownloadServiceUrlParam(response);
        	}
        	
        	//set up the charset for proper encoding special Finnish characters
        	params.getResponse().setContentType("text/xml; subtype=gml/2.1.2; charset=UTF-8");
        	
            ResponseHelper.writeResponse(params, response);
        } catch (Exception e) {
            throw new ActionException("Couldn't proxy request to GeoServer", e);
        }
    }
    
    private String addDownloadServiceUrlParam(String responseInput) {
		String responseOutput = responseInput;
    	
    	final OMElement doc = XmlHelper.parseXML(responseInput);
    	if (doc != null && TAG_FEATURE_COLLECTION.equals(doc.getLocalName())) {
    		
    		Iterator featureMembersIterator = doc.getChildrenWithLocalName("featureMembers");
    		
    		if (featureMembersIterator.hasNext()) {
    			OMElement featureMembers = (OMElement) doc.getChildrenWithLocalName("featureMembers").next();
    			
	    		Iterator categories = featureMembers.getChildrenWithLocalName("categories");
	    		while(categories.hasNext()){
	    			OMElement category = (OMElement)categories.next();
	    			String categoryId = category.getAttributeValue(new QName("http://www.opengis.net/gml", "id", "gml"));
	    			
	    			int index = categoryId.indexOf(".");
	    			String id = categoryId.substring(index + 1);
	    			
	    			UserGisData userGisData = new UserGisData();
					try {
						userGisData = gisDataService.findGisDataByDataId(id, "MY_PLACES");
					} catch (ServiceException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	
					if (userGisData != null) {
		    			OMFactory factory = OMAbstractFactory.getOMFactory();
		    			OMNamespace oskariNamespace = factory.createOMNamespace("http://www.oskari.org", "oskari");
		    			OMElement downloadServiceUrlElement = factory.createOMElement("download_service_url", oskariNamespace);
		    			downloadServiceUrlElement.setText(userGisData.getDownloadServiceUrl());
		    			category.addChild(downloadServiceUrlElement);
					}
	    		}
	    		
	    		responseOutput = doc.toString();
    		}
    	}
		
		return responseOutput;
	}
	private String getDataIdFromResponse(String response) throws ActionException {
    	String dataId = "0";
    	
    	try {
	    	final OMElement doc = XmlHelper.parseXML(response);
	
	    	final String rootTag = doc.getLocalName();
	    	if (TAG_TRANSACTION_RESPONSE.equals(rootTag)) {
	    		final OMElement insertResultsElement = (OMElement) doc.getChildrenWithLocalName("InsertResults").next();
	    		
	    		final OMElement featureElement = insertResultsElement.getFirstElement();
	    		final OMElement featureIdElement = featureElement.getFirstElement();
	    		final String fid = featureIdElement.getAttributeValue(ATTR_FID);
	
	    		final Long id = ConversionHelper.getLong(fid.substring(fid.indexOf('.') + 1), -1);
	    		
	    		dataId = id.toString();
	    	}
    	} catch (Exception e) {
    		throw new ActionException("Error during getting dataid. Content of the response: " + response, e);
    	}
        
		return dataId;
	}
	private OMElement addFiltersToWFSRequest(OMElement root, List<UserGisData> categories, String param) throws ActionException {
    	
        try {
            List<OMElement> propertyNodes = (List<OMElement>) XPATH_GETFEATURE_PROPERTY_FILTER.selectNodes(root);
            OMContainer filterElement = propertyNodes.get(0).getParent();
            //removing all existing filters
            filterElement.removeChildren();
            
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMNamespace ns = factory.createOMNamespace("ogc","ogc");
            
            if (categories.isEmpty()) {
            	return null;
            }
            for (UserGisData category : categories) {
            	if (param.equals("categories")) {
	            	OMElement featureNode = factory.createOMElement("FeatureId", ns);
	            	OMAttribute att = factory.createOMAttribute("fid", ns, "categories." + category.getDataId());
	            	featureNode.addAttribute(att);
	            	filterElement.addChild(featureNode);
            	} else {
            		OMElement orNode = factory.createOMElement(new QName("Or"));
            		OMElement propertyIsEqualToNode = factory.createOMElement(new QName("PropertyIsEqualTo"));
            		OMElement propertyNameNode = factory.createOMElement(new QName("PropertyName"));
            		propertyNameNode.setText("category_id");
            		propertyIsEqualToNode.addChild(propertyNameNode);
            		OMElement literalNode = factory.createOMElement(new QName("Literal"));
            		literalNode.setText("" + category.getDataId());
            		propertyIsEqualToNode.addChild(literalNode);
            		orNode.addChild(propertyIsEqualToNode);
            		filterElement.addChild(orNode);
            	}
            }
        } catch (Exception ex) {
            if(ex instanceof ActionException) {
                throw (ActionException) ex;
            }
            throw new ActionException("Error parsing payload", ex);
        }
        
    	return root;
    }

    private String readPayload(final HttpServletRequest request) throws ActionException {
        try {
            return IOHelper.readString(request.getInputStream());
        } catch (IOException e) {
            throw new ActionException("Couldn't read POST data from request", e);
        }
    }

    // wfs:Transaction/wfs:Insert/feature:my_places
    // - feature:category_id
    // - feature:uuid
    private boolean validateRequest(final ActionParameters params, final OMElement payload) throws ActionException {
    	boolean isGet = false;
        if(payload == null) {
            throw new ActionParamsException("Invalid request: no payload");
        }
        if(!ALLOWED_MAIN_LEVEL_TAGS.contains(payload.getLocalName())) {
            log.warn("Myplaces request denied:", payload);
            throw new ActionDeniedException("Invalid request: " + payload.getLocalName());
        }
        final User user = params.getUser();
        if(user.isGuest()) {
            // if not transaction -> fail immediately
            if(!TAG_TRANSACTION.equals(payload.getLocalName())) {
                throw new ActionDeniedException("Session expired");
            }
            else {
                validateGuestTransaction(payload, params.getUser());
            }
        }
        else {
            final String rootTag = payload.getLocalName();
            if(TAG_GETFEATURE.equals(rootTag)) {
                validateGetFeature(payload, params.getUser());
                
                OMElement queryElement = (OMElement) payload.getChildElements().next();
                final String typeName = queryElement.getAttributeValue(ATTR_TYPENAME);
                if (typeName.equals("feature:categories")) {
                	dbOperation = "GET_CATEGORIES";
                }// else {
                //	dbOperation = "GET_MY_PLACES";
                //}
                isGet = true;
            }
            else if(TAG_TRANSACTION.equals(rootTag)) {
                validateUserTransaction(payload, params.getUser());
            }
        }
        return isGet;
    }

    private List<Long> getCategoryIdsFromMyplace(final OMElement root) throws ActionException {
        List<Long> list = new ArrayList<Long>();
        if(XPATH_MYPLACE_INSERT_CATEGORYID == null) {
            throw new ActionParamsException("Xpath definition missing");
        }
        try {
            List<OMElement> nodeList = XPATH_MYPLACE_INSERT_CATEGORYID.selectNodes(root);
            for(OMElement node : nodeList) {
                long catId = ConversionHelper.getLong(node.getText(), -1);
                if(catId != -1) {
                    list.add(catId);
                }
            }
        } catch (Exception ex) {
            log.info(ex, "Error parsing category ids from payload");
            throw new ActionParamsException("Error parsing payload");
        }
        if(list.isEmpty()) {
            throw new ActionParamsException("Error parsing payload");
        }
        return list;
    }
    public void validateGuestTransaction(final OMElement root, final User user) throws ActionException {

        // restrict guest users to insert transactions
        final boolean onlyInserts = XmlHelper.containsOnlyDirectChildrenOfName(root, TAG_INSERT);
        if(!onlyInserts) {
            throw new ActionParamsException("Invalid request: " + ((OMElement)root.getChildElements().next()).getLocalName());
        }
        // check that guest can insert into the listed categories if we get this far
        validateMyplaceInsertCategories(root, user);
    }

    private void validateMyplaceInsertCategories(final OMElement root, final User user) throws ActionException {
        if(XPATH_MYPLACE_INSERT_UUID == null) {
            throw new ActionParamsException("Xpath definitions missing");
        }
        // check that user can insert into the listed categories
        List<Long> categoryIds = getCategoryIdsFromMyplace(root);
        for(Long id: categoryIds) {
            if(!service.canInsert(user, id)) {
                throw new ActionDeniedException("Tried to insert feature into category: " + id);
            }
        }
        try {
            // setup place uuid from the first category (there should be only one anyways)
            // this way the place will be visible both in map and in the layer owners personal data
            final OMElement uuidNode = (OMElement) XPATH_MYPLACE_INSERT_UUID.selectSingleNode(root);
            final List<MyPlaceCategory> categories = service.getMyPlaceLayersById(categoryIds);
            if(categories.isEmpty()) {
                throw new ActionParamsException("Couldn't load categories");
            }
            uuidNode.setText(categories.get(0).getUuid());
        } catch (Exception ex) {
            if(ex instanceof ActionException) {
                throw (ActionException) ex;
            }
            throw new ActionException("Error parsing payload", ex);
        }
    }

    public void validateUserTransaction(final OMElement root, final User user) throws ActionException {
        if(user.isGuest()) {
            throw new ActionDeniedException("Session expired");
        }
        if(XPATH_MYPLACE == null || XPATH_CATEGORY == null) {
            throw new ActionParamsException("Xpath definitions missing");
        }
        try {
            final boolean onlyInserts = XmlHelper.containsOnlyDirectChildrenOfName(root, TAG_INSERT);
            if(onlyInserts) {
            	
            	//check limitations
            	if (!gisService.canUserAddNewDataset(user)) {
            		throw new ActionException("server_error_key_limit");
            	}
            	
                // only supporting single feature operations for now
                final OMElement categoryNode = (OMElement) XPATH_CATEGORY.selectSingleNode(root);
                if(categoryNode != null) {
                    // inserting category - if not throwing exception request has same uuid as current user
                    validateUuid(categoryNode, user);
                    
                    dbOperation = "INSERT_CATEGORY";
                }

                final OMElement placeNode = (OMElement) XPATH_MYPLACE.selectSingleNode(root);
                if(placeNode != null) {
                    // inserting place - if not throwing exception request has same uuid as current user
                    validateUuid(placeNode, user);
                    // check that the place is being inserted in a category where user can insert places (own/drawlayer in published)
                    validateMyplaceInsertCategories(root, user);
                }
                // insert request ok, proceed as requested
                return;
            }
            // update or delete  <wfs:Update typeName="feature:my_places" or <wfs:Delete typeName="feature:categories"
            final OMElement operation = (OMElement) root.getChildElements().next();
            final String typeName = operation.getAttributeValue(ATTR_TYPENAME);
            log.debug("Typename is:", typeName);

            final OMElement featureIdElement = (OMElement) XPATH_MODIFY_FEATURE.selectSingleNode(root);
            final String fid = featureIdElement.getAttributeValue(ATTR_FID);

            final long id = ConversionHelper.getLong(fid.substring(fid.indexOf('.') + 1), -1);
            // NOTE! uuid can be change as well, not checking it here
            // more thoroughly could be checked by looping properties for <name> uuid and category_id
            // List<OMElement> properties = (List<OMElement>)featureIdElement.getParent().getChildrenWithLocalName("Property");
            if(TYPE_CATEGORY.equals(typeName)) {
                // modify/delete category <ogc:FeatureId fid="categories.nn"></ogc:FeatureId>
                log.debug("Modifying category. Fid:", fid, "- id:", id);
                if(!service.canModifyCategory(user, id)) {
                    throw new ActionDeniedException("Tried to modify category: " + id);
                }
                
                if (XmlHelper.containsOnlyDirectChildrenOfName(root, TAG_UPDATE)) {
                	dbOperation = "UPDATE_CATEGORY";
                } else if (XmlHelper.containsOnlyDirectChildrenOfName(root, TAG_DELETE)) {
                	dbOperation = "DELETE_CATEGORY";
                	deleteDataId = "" + id;
                }
                
                // category modify/delete request ok, proceed as requested
                return;
            }
            else if(TYPE_PLACE.equals(typeName)) {
                // modify/delete place <ogc:FeatureId fid="my_places.nn"></ogc:FeatureId>
                log.debug("Modifying place. Fid:", fid, "- id:", id);
                if(!service.canModifyPlace(user, id)) {
                    throw new ActionDeniedException("Tried to modify place: " + id);
                }
                // place modify/delete request ok, proceed as requested
                return;
            }
            throw new ActionParamsException("Unknown feature. Not a place or category.");
        } catch (Exception ex) {
            if(ex instanceof ActionException) {
                throw (ActionException) ex;
            }
            throw new ActionException("Error parsing payload", ex);
        }
    }

    private void validateUuid(final OMElement feature, final User user) throws ActionException {
        OMElement uuid = (OMElement) feature.getChildrenWithLocalName("uuid").next();
        if(uuid == null) {
            throw new ActionDeniedException("Uuid mismatch. Uuid is <null>");
        }
        if(!user.getUuid().equals(uuid.getText())) {
            throw new ActionDeniedException("Uuid mismatch. Filter uuid " + uuid.getText());
        }
    }

    /*
    <wfs:GetFeature xmlns:wfs="http://www.opengis.net/wfs" service="WFS" version="1.1.0" xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <wfs:Query typeName="feature:categories" srsName="EPSG:3067" xmlns:feature="http://www.oskari.org">
    <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
    <ogc:PropertyIsEqualTo matchCase="true">
    <ogc:PropertyName>uuid</ogc:PropertyName>
    <ogc:Literal>USERS-UUID-SOMETHING-LIKE-THIS</ogc:Literal>
    </ogc:PropertyIsEqualTo>
    </ogc:Filter>
    </wfs:Query>
    </wfs:GetFeature>
    Might also have "ogc:And" tag under Filter with FeatureId as second filter
    */
    public void validateGetFeature(final OMElement root, final User user) throws ActionException {
        log.debug("Validating GetFeature request");
        if(user.isGuest()) {
            throw new ActionDeniedException("Session expired");
        }
        if(XPATH_GETFEATURE_PROPERTY_FILTER == null) {
            throw new ActionParamsException("Xpath definition missing");
        }
        try {
            List<OMElement> propertyNodes = (List<OMElement>) XPATH_GETFEATURE_PROPERTY_FILTER.selectNodes(root);
            for(OMElement elem : propertyNodes) {
                OMElement name = (OMElement) elem.getChildrenWithLocalName("PropertyName").next();
                if(name.getText().equals("uuid")) {
                    OMElement val = (OMElement) elem.getChildrenWithLocalName("Literal").next();
                    if(!user.getUuid().equals(val.getText())) {
                        // found uuid filter but it isn't a match!
                        throw new ActionDeniedException("Filter mismatch. Filter uuid " + val.getText());
                    }
                    // found a matching uuid, request is ok
                    return;
                }
            }
            throw new ActionDeniedException("Filter mismatch. No uuid specified!");
        } catch (Exception ex) {
            if(ex instanceof ActionException) {
                throw (ActionException) ex;
            }
            throw new ActionException("Error parsing payload", ex);
        }
    }
     
}