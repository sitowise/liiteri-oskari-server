package fi.nls.oskari.pojo.style;

import java.util.ArrayList;
import java.util.Map;

import fi.nls.oskari.pojo.WFSCustomStyleStore;

public class CustomStyleStoreFactory {
	
	
	public static CustomStyleStore createFromJson(String layerId, String client, Map<String, Object> style) {
		CustomStyleStore result = null;
		
		String type = style.get("type").toString();
		
		if (type.equals("simple")) {				        
			Map<String, Object> symbolStyle = (Map<String, Object>) style.get("symbol");			
			WFSCustomStyleStore customStyle = createCustomStyle(symbolStyle);	        
	        result = customStyle;
		}
		else if (type.equals("uniqueValue")) {
			UniqueValueCustomStyleStore customStyle = new UniqueValueCustomStyleStore();			
			ArrayList infos = (ArrayList) style.get("uniqueValueInfos");
			
			customStyle.setField(style.get("field").toString());
			
			for (Object itemInfo : infos) {
				Map<String, Object> mapItemInfo = (Map<String, Object>) itemInfo;
				WFSCustomStyleStore itemStyle = createCustomStyle((Map<String, Object>) mapItemInfo.get("symbol"));
				String value = mapItemInfo.get("value").toString();
				customStyle.AddUniqueValuesInfo(value, itemStyle);
			}
			
			result = customStyle;
		}
		else if (type.equals("group")) {
			GroupCustomStyleStore customStyle = new GroupCustomStyleStore();
			ArrayList infos = (ArrayList) style.get("subStyles");
			for (Object itemInfo : infos) {
				Map<String, Object> mapItemInfo = (Map<String, Object>) itemInfo;
				WFSCustomStyleStore itemStyle = createCustomStyle((Map<String, Object>) mapItemInfo.get("symbol"));
				String value = mapItemInfo.get("value").toString();
				customStyle.addSubStyle(value, itemStyle);
			}
			result = customStyle;
		}
		
		if (result != null) {
			result.setLayerId(layerId);
			result.setClient(client);				
		}		
		
		return result;
	}
	
	private static WFSCustomStyleStore createCustomStyle(Map<String, Object> symbolStyle) {
		WFSCustomStyleStore customStyle = new WFSCustomStyleStore();	        
		
        customStyle.setFillColor((String)symbolStyle.get(WFSCustomStyleStore.PARAM_FILL_COLOR));
        customStyle.setFillPattern((int) symbolStyle.get(WFSCustomStyleStore.PARAM_FILL_PATTERN));
        customStyle.setBorderColor((String)symbolStyle.get(WFSCustomStyleStore.PARAM_BORDER_COLOR));
        customStyle.setBorderLinejoin((String)symbolStyle.get(WFSCustomStyleStore.PARAM_BORDER_LINEJOIN));
        customStyle.setBorderDasharray((String)symbolStyle.get(WFSCustomStyleStore.PARAM_BORDER_DASHARRAY));
        customStyle.setBorderWidth((int) symbolStyle.get(WFSCustomStyleStore.PARAM_BORDER_WIDTH));

        customStyle.setStrokeLinecap((String)symbolStyle.get(WFSCustomStyleStore.PARAM_STROKE_LINECAP));
        customStyle.setStrokeColor((String)symbolStyle.get(WFSCustomStyleStore.PARAM_STROKE_COLOR));
        customStyle.setStrokeLinejoin((String)symbolStyle.get(WFSCustomStyleStore.PARAM_STROKE_LINEJOIN));
        customStyle.setStrokeDasharray((String)symbolStyle.get(WFSCustomStyleStore.PARAM_STROKE_DASHARRAY));
        customStyle.setStrokeWidth((int) symbolStyle.get(WFSCustomStyleStore.PARAM_STROKE_WIDTH));

        customStyle.setDotColor((String)symbolStyle.get(WFSCustomStyleStore.PARAM_DOT_COLOR));
        customStyle.setDotShape((int) symbolStyle.get(WFSCustomStyleStore.PARAM_DOT_SHAPE));
        customStyle.setDotSize((int) symbolStyle.get(WFSCustomStyleStore.PARAM_DOT_SIZE));
        
        return customStyle;
	}
}
