package pl.sito.liiteri.stats.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.sito.liiteri.stats.domain.ClassificationParams.DataTransformationType;

public class ClassificationDescription
{
	private HashMap<String, double[]> _lookup = new HashMap<String, double[]>();
	private HashMap<Double, String> _descriptionLookup = new HashMap<Double, String>();
	private DataTransformationType _dataTransformation;
	
	public void add(String description, double min, double max) {
		_lookup.put(description, new double[] { min, max});
		_descriptionLookup.put(min, description);
	}
	
	public void setDataTransformation(DataTransformationType dataTransformation)
	{
		_dataTransformation = dataTransformation;	
	}
	
	public DataTransformationType getDataTransformation() {
		return _dataTransformation;
	}
	
	public String[] getDescriptions() {
		List<Double> keys = new ArrayList<Double>(_descriptionLookup.keySet());
		Collections.sort(keys);
		List<String> result = new ArrayList<String>();
		
		for (Double key : keys)
		{
			result.add(_descriptionLookup.get(key));
		}
		
		return result.toArray(new String[0]);
	}
	
	public double getMinValue() {
		double result = Double.MAX_VALUE;
		
		for (double[] array : _lookup.values())
		{
			if (array[0] < result)
				result = array[0];
		}
		
		return result;
	}
	
	public double getMaxValue() {
		double result = Double.MIN_VALUE;
		
		for (double[] array : _lookup.values())
		{
			if (array[1] > result)
				result = array[1];
		}
		
		return result;
	}
	
	public int getNumberOfDescriptions() {
		return getDescriptions().length;
	}
	
	public double getMin(String description) {
		return _lookup.get(description)[0];
	}
	
	public double getMax(String description) {
		return _lookup.get(description)[1];
	}
	
	public String toJSONString() {
		
		JSONObject json = new JSONObject();
		JSONArray array = new JSONArray();
		try
		{
			String[] descriptions = getDescriptions();
			json.put("dataTransformation", getDataTransformation().getValue());
			for (String description : descriptions)
			{
				JSONObject itemJson = new JSONObject();
				itemJson.put("description", description);
				itemJson.put("ranges", new JSONArray(new double[] {_lookup.get(description)[0], _lookup.get(description)[1]} ));	
				array.put(itemJson);
			}
			json.put("items", array);
			
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
	
		return json.toString();
	}
	
	public static ClassificationDescription fromJSON(String content) {
		ClassificationDescription result = new ClassificationDescription();
		
		try
		{
			JSONObject json = new JSONObject(content);
			int dataTransformationCode = json.getInt("dataTransformation");
			switch (dataTransformationCode)
			{
			case 1:
				result.setDataTransformation(DataTransformationType.AbsoluteValues);
				break;
			default:
				result.setDataTransformation(DataTransformationType.None);
				break;
			}
			
			JSONArray array = json.getJSONArray("items");
	    	for (int i = 0; i < array.length(); i++)
			{
				JSONObject item = array.getJSONObject(i);
				String description = item.getString("description");
				JSONArray ranges = item.getJSONArray("ranges");
				double min = ranges.getDouble(0);
	    		double max = ranges.getDouble(1);
	    		result.add(description, min, max);
			}
			
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
    	
    	return result;
	}


}
