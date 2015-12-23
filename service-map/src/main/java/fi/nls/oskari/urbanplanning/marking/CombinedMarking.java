package fi.nls.oskari.urbanplanning.marking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.urbanplanning.params.CommonParameter;
import fi.nls.oskari.urbanplanning.params.IdParameter;
import fi.nls.oskari.util.JSONHelper;

public class CombinedMarking extends Marking
{
	private static final Logger log = LogFactory.getLogger(CombinedMarking.class);
	
	private ArrayList<Marking> _markings;
	
	public CombinedMarking() {
		_markings = new ArrayList<Marking>();
	}
	
	public void addMarking(Marking marking) {
		_markings.add(marking);
	}
	
	@Override
	protected String getName() {
		ArrayList<String> names = new ArrayList<String>();
		
		for (Marking marking : _markings) {
			names.add(marking.getName());
		}
		Collections.sort(names);

		return StringUtils.join(names, ",");
	}
	
	@Override
	public String getData(List<CommonParameter> params, CommonParameter type,
			IdParameter municipalityId, CommonParameter mainMarkName) throws ServiceException {
		
		ArrayList<String> results = new ArrayList<String>();
		
		for (Marking marking : _markings) {
			String result = marking.getData(params, type, municipalityId, mainMarkName);
			results.add(result);
		}
		
		
		JSONArray resultArray = new JSONArray();
		for (String result : results) {
			JSONArray array = JSONHelper.createJSONArray(result);
			for (int i = 0; i < array.length(); i++) {
				try
				{
					resultArray.put(array.get(i));
				} catch (JSONException e)
				{
					log.warn(e);
				}
	        }
		}
		
		return resultArray.toString();
	}
}
