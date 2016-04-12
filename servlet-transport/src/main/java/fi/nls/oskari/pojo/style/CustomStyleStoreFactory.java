package fi.nls.oskari.pojo.style;

import java.util.List;
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
			Object[] infos = (Object[]) style.get("uniqueValueInfos");
			
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
			Object[] infos = (Object[]) style.get("subStyles");
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
		
        customStyle.setFillColor(symbolStyle.get(WFSCustomStyleStore.PARAM_FILL_COLOR).toString());
        customStyle.setFillPattern((int) symbolStyle.get(WFSCustomStyleStore.PARAM_FILL_PATTERN));
        customStyle.setBorderColor(symbolStyle.get(WFSCustomStyleStore.PARAM_BORDER_COLOR).toString());
        customStyle.setBorderLinejoin(symbolStyle.get(WFSCustomStyleStore.PARAM_BORDER_LINEJOIN).toString());
        customStyle.setBorderDasharray(symbolStyle.get(WFSCustomStyleStore.PARAM_BORDER_DASHARRAY).toString());
        customStyle.setBorderWidth((int) symbolStyle.get(WFSCustomStyleStore.PARAM_BORDER_WIDTH));

        customStyle.setStrokeLinecap(symbolStyle.get(WFSCustomStyleStore.PARAM_STROKE_LINECAP).toString());
        customStyle.setStrokeColor(symbolStyle.get(WFSCustomStyleStore.PARAM_STROKE_COLOR).toString());
        customStyle.setStrokeLinejoin(symbolStyle.get(WFSCustomStyleStore.PARAM_STROKE_LINEJOIN).toString());
        customStyle.setStrokeDasharray(symbolStyle.get(WFSCustomStyleStore.PARAM_STROKE_DASHARRAY).toString());
        customStyle.setStrokeWidth((int) symbolStyle.get(WFSCustomStyleStore.PARAM_STROKE_WIDTH));

        customStyle.setDotColor(symbolStyle.get(WFSCustomStyleStore.PARAM_DOT_COLOR).toString());
        customStyle.setDotShape((int) symbolStyle.get(WFSCustomStyleStore.PARAM_DOT_SHAPE));
        customStyle.setDotSize((int) symbolStyle.get(WFSCustomStyleStore.PARAM_DOT_SIZE));
        
        return customStyle;
	}
}
