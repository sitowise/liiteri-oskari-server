package fi.nls.oskari.arcgis;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.pojo.WFSCustomStyleStore;
import fi.nls.oskari.pojo.style.CustomStyleStore;
import fi.nls.oskari.pojo.style.GroupCustomStyleStore;
import fi.nls.oskari.pojo.style.UniqueValueCustomStyleStore;

public class ArcGisStyleMapper {
	private static final Logger log = LogFactory.getLogger(ArcGisStyleMapper.class);
	public static final String POLYGON_GEOMETRY = "esriGeometryPolygon";
	public static final String POLYLINE_GEOMETRY = "esriGeometryPolyline";
	public static final String POINT_GEOMETRY = "esriGeometryPoint";
	
	public static final String MARKER_SYMBOL_TYPE = "esriSMS";
	public static final String LINE_SYMBOL_TYPE = "esriSLS";
	public static final String FILL_SYMBOL_TYPE = "esriSFS";

    public static final String RENDERER_TYPE = "type";

    public static final String RENDERER_TYPE_SIMPLE = "simple";
    public static final String RENDERER_TYPE_UNIQUE = "uniqueValue";
    public static final String RENDERER_TYPE_GROUP = "group";
    
    @SuppressWarnings("unchecked")
    public static JSONObject mapStyleToRenderer(CustomStyleStore customStyle, String geometryType, String layerName) {
        JSONObject renderer = new JSONObject();

        if (customStyle.getType().equals(RENDERER_TYPE_SIMPLE)) {
            renderer.put(RENDERER_TYPE, RENDERER_TYPE_SIMPLE);
            renderer.put("label", "yncepynce");
            renderer.put("description", "");
            JSONObject symbol = ArcGisStyleMapper.mapStyleToSymbol((WFSCustomStyleStore) customStyle, geometryType);
            renderer.put("symbol", symbol);
            renderer.put("label", layerName);
        } else if (customStyle.getType().equals(RENDERER_TYPE_UNIQUE)) {
            UniqueValueCustomStyleStore uniqueValueStyle = (UniqueValueCustomStyleStore) customStyle;
            renderer.put(RENDERER_TYPE, RENDERER_TYPE_UNIQUE);
            renderer.put("field1", uniqueValueStyle.getField());

            JSONArray uniqueValueItems = new JSONArray();
            for (String value : uniqueValueStyle.getUniqueValuesInfo().keySet()) {
                JSONObject uniqueValueItem = new JSONObject();
                uniqueValueItem.put("value", value);
                JSONObject symbol = ArcGisStyleMapper
                        .mapStyleToSymbol(uniqueValueStyle.getUniqueValuesInfo().get(value), geometryType);
                uniqueValueItem.put("symbol", symbol);
                uniqueValueItem.put("label", uniqueValueStyle.getUniqueValuesInfo().get(value).getName());
                uniqueValueItems.add(uniqueValueItem);
            }

            renderer.put("uniqueValueInfos", uniqueValueItems);
        } else if (customStyle.getType().equals(RENDERER_TYPE_GROUP)) {
            GroupCustomStyleStore groupStyle = (GroupCustomStyleStore) customStyle;
            CustomStyleStore specificStyle = groupStyle.getSubStyles().get(layerName);
            if (specificStyle != null) {
                return mapStyleToRenderer(specificStyle, geometryType, layerName);
            }
        }

        return renderer;
    }
    

	public static JSONObject mapStyleToSymbol(WFSCustomStyleStore customStyle, String geometryType) {
		
		if (geometryType == null) {
			log.error("Empty geometry type");
		}
		else if (geometryType.equals(POLYGON_GEOMETRY)) {
			return mapPolygonStyle(customStyle);
		} else if (geometryType.equals(POLYLINE_GEOMETRY)) {
			return mapPolylineStyle(customStyle);
		} else if (geometryType.equals(POINT_GEOMETRY)) {
			return mapPointStyle(customStyle);
		}
		else {
			log.error("Not supported geometry type", geometryType);
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static JSONObject mapPolygonStyle(WFSCustomStyleStore customStyle) 
	{
		JSONObject symbol = new JSONObject();
		symbol.put("type", FILL_SYMBOL_TYPE);
		symbol.put("style", mapFillStyle(customStyle.getFillPattern()));
		JSONArray colorArray = new JSONArray();
		colorArray.addAll(mapColor(customStyle.getFillColor()));
		symbol.put("color", colorArray);		
		
		JSONObject outline = new JSONObject();
		outline.put("type", LINE_SYMBOL_TYPE);
		outline.put("style", mapLineStyle(customStyle.getBorderDasharray()));
		JSONArray outlineColorArray = new JSONArray();
		outlineColorArray.addAll(mapColor(customStyle.getBorderColor()));
		outline.put("color", outlineColorArray);
		outline.put("width", customStyle.getBorderWidth());
		
		symbol.put("outline", outline);
		
		return symbol;
	}
	
	@SuppressWarnings("unchecked")
	private static JSONObject mapPolylineStyle(WFSCustomStyleStore customStyle) {
		
		JSONObject outline = new JSONObject();
		outline.put("type", LINE_SYMBOL_TYPE);
		outline.put("style", mapLineStyle(customStyle.getStrokeDasharray()));
		JSONArray outlineColorArray = new JSONArray();
		outlineColorArray.addAll(mapColor(customStyle.getStrokeColor()));
		outline.put("color", outlineColorArray);
		outline.put("width", customStyle.getStrokeWidth());	
		
		return outline;
	}
	
	@SuppressWarnings("unchecked")
	private static JSONObject mapPointStyle(WFSCustomStyleStore customStyle) {
		
		JSONObject outline = new JSONObject();
		outline.put("type", MARKER_SYMBOL_TYPE);
		outline.put("style", mapPointStyle(customStyle.getDotShape()));
		JSONArray outlineColorArray = new JSONArray();
		outlineColorArray.addAll(mapColor(customStyle.getDotColor()));
		outline.put("color", outlineColorArray);
		outline.put("size", customStyle.getDotSize());	
		
		return outline;
	}
	
	private static String mapPointStyle(int code) {
		String result;
		
		switch (code) {
		case 1:
			result = "esriSMSSquare";
			break;
		case 5:
			result = "esriSMSCircle";
			break;
		default:
			result = "esriSMSCircle";
			break;
		}
		
		return result;
	}
	
	private static String mapLineStyle(String dasharray) {
		String result = "esriSLSSolid";
		
		if (dasharray.equals("5 2"))
			result = "esriSLSDash";
		
		return result;
	}
	
	private static String mapFillStyle(int code) {
		String result = "esriSFSSolid";
		
		if (code == 0 || code == 1) 
			result = "esriSFSBackwardDiagonal";
		else if (code == 2 || code == 3)
			result = "esriSFSHorizontal";
		else if (code == 4) 
			result = "esriSFSNull";
		
		return result;
	}
	
	private static ArrayList<Integer> mapColor(final String color) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		result.add(255);
		result.add(255);
		result.add(255);
        if(color == null) {
            result.add(0);
            return result;
        }
        else
            result.add(255);
		String tmpColor = color;
		
		if (tmpColor.startsWith("#"))
			tmpColor = tmpColor.substring(1);
		
		int index = 0;
		while (tmpColor.length() >= 2 && index < 4) {
			result.set(index, Integer.parseInt(tmpColor.substring(0, 2), 16));
			tmpColor = tmpColor.substring(2);
			index++;
		}
		
		return result;
	}

    public static CustomStyleStore mapRendererToStyle(JSONObject renderer) {
        CustomStyleStore style = null;

        if (renderer == null)
            return style;

        String rendererType = renderer.get(RENDERER_TYPE).toString();

        if (rendererType.equals(RENDERER_TYPE_SIMPLE)) {
            WFSCustomStyleStore concreteStyle = mapSymbolToStyle((JSONObject) renderer.get("symbol"));
            concreteStyle.setName(renderer.get("label").toString());
            style = concreteStyle;
        } else if (rendererType.equals(RENDERER_TYPE_UNIQUE)) {
            UniqueValueCustomStyleStore concreteStyle = new UniqueValueCustomStyleStore();
            concreteStyle.setField(renderer.get("field1").toString());
            JSONArray uniqueValueInfos = (JSONArray) renderer.get("uniqueValueInfos");

            for (Object uniqueValueInfoObj : uniqueValueInfos) {
                JSONObject item = (JSONObject) uniqueValueInfoObj;
                String value = item.get("value").toString();
                String label = item.get("label").toString();
                WFSCustomStyleStore itemStyle = mapSymbolToStyle((JSONObject) item.get("symbol"));
                itemStyle.setName(label);
                concreteStyle.AddUniqueValuesInfo(value, itemStyle);
            }

            style = concreteStyle;
        }

        return style;
    }

    public static WFSCustomStyleStore mapSymbolToStyle(JSONObject symbol) {
        WFSCustomStyleStore result = new WFSCustomStyleStore();
        String symbolType = symbol.get("type").toString();

        if (symbolType.equals(MARKER_SYMBOL_TYPE)) {
            result = mapToPointStyle(symbol);
        } else if (symbolType.equals(LINE_SYMBOL_TYPE)) {
            result = mapToPolylineStyle(symbol);
        } else if (symbolType.equals(FILL_SYMBOL_TYPE)) {
            result = mapToPolygonStyle(symbol);
        } else {
            log.error("Not supported symbolType", symbolType);
        }

        return result;
    }

    private static WFSCustomStyleStore getDefaultStyle() {
        WFSCustomStyleStore style = new WFSCustomStyleStore();

        style.setFillPattern(-1);
        style.setFillColor("#000000");

        style.setBorderWidth(0);
        style.setBorderColor("#000000");
        style.setBorderDasharray("");
        style.setBorderLinejoin("mitre");

        style.setDotColor("#000000");
        style.setDotShape(1);
        style.setDotSize(3);

        style.setStrokeColor("#000000");
        style.setStrokeDasharray("");
        style.setStrokeLinecap("butt");
        style.setStrokeLinejoin("mitre");
        style.setStrokeWidth(1);

        return style;
    }

    @SuppressWarnings("unchecked")
    private static WFSCustomStyleStore mapToPolygonStyle(JSONObject symbol) {
        WFSCustomStyleStore style = getDefaultStyle();

        style.setFillPattern(mapToFillStyle(symbol.get("style").toString()));
        style.setFillColor(mapColor((List<Object>) symbol.get("color")));

        JSONObject outlineSymbol = (JSONObject) symbol.get("outline");
        style.setBorderWidth((int) Double.parseDouble(outlineSymbol.get("width").toString()));
        style.setBorderColor(mapColor((List<Object>) outlineSymbol.get("color")));
        style.setBorderDasharray(mapToLineStyle(outlineSymbol.get("style").toString()));

        return style;
    }

    @SuppressWarnings("unchecked")
    private static WFSCustomStyleStore mapToPolylineStyle(JSONObject symbol) {
        WFSCustomStyleStore style = getDefaultStyle();

        style.setStrokeColor(mapColor((List<Object>) symbol.get("color")));
        style.setStrokeDasharray(mapToLineStyle(symbol.get("style").toString()));
        style.setStrokeWidth((int) Double.parseDouble(symbol.get("width").toString()));

        return style;
    }

    @SuppressWarnings("unchecked")
    private static WFSCustomStyleStore mapToPointStyle(JSONObject symbol) {

        WFSCustomStyleStore style = getDefaultStyle();

        style.setDotColor(mapColor((List<Object>) symbol.get("color")));
        style.setDotShape(mapToPointStyle(symbol.get("style").toString()));
        style.setDotSize(((Long) symbol.get("size")).intValue());

        return style;
    }

    private static int mapToPointStyle(String code) {
        int result = 1;

        if (code.equals("esriSMSSquare"))
            result = 1;
        else if (code.equals("esriSMSCircle"))
            result = 5;

        return result;
    }

    private static String mapToLineStyle(String code) {
        String result = "";

        if (code.equals("esriSLSDash"))
            result = "5 2";

        return result;
    }

    private static int mapToFillStyle(String code) {
        int result = -1;

        if (code.equals("esriSFSBackwardDiagonal"))
            result = 0;
        else if (code.equals("esriSFSBackwardDiagonal"))
            result = 2;
        else if (code.equals("esriSFSNull"))
            result = 4;

        return result;
    }

    private static String mapColor(final List<Object> code) {
        StringBuilder result = new StringBuilder("#");

        for (Object item : code) {
            String mappedItem = Long.toHexString((Long) item);
            if (mappedItem.length() == 1)
                result.append("0");
            result.append(mappedItem);
        }

        return result.toString();
    }
}
