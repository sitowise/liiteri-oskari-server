package pl.sito.liiteri.sharing;

import org.json.JSONObject;

public interface ISharingMappingService
{
	public SharingItem Map(JSONObject json);
	public JSONObject Map(SharingItem item);
}
