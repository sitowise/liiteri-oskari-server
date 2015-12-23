package pl.sito.liiteri.stats.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GridStatsVisualization
{
	private HashMap<String, List<GridStatsResultItem>> _lookup;
	private String[] _descriptions;
		
	public GridStatsVisualization() {
		_lookup = new HashMap<String, List<GridStatsResultItem>>();
	}
	
	public void setDescriptions(String[] descriptions) {
		this._descriptions = descriptions;
		for (String description : descriptions)
		{
			_lookup.put(description, new ArrayList<GridStatsResultItem>());
		}
	}
	
	public String[] getDescriptions() {
		return this._descriptions;
	}
	
	public void classify(int index, GridStatsResultItem item) {
		String color = _descriptions[index];
		
		_lookup.get(color).add(item);
	}
	
	public List<GridStatsResultItem> getItems(String description) {
		return _lookup.get(description);
	}
}
