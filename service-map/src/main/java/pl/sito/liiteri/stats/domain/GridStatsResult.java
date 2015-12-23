package pl.sito.liiteri.stats.domain;

import java.util.ArrayList;
import java.util.List;

public class GridStatsResult
{
	private List<GridStatsResultItem> _items;
	
	public GridStatsResult() {
		_items = new ArrayList<GridStatsResultItem>();
	}
	
	public void AddItem(GridStatsResultItem item) {
		_items.add(item);
	}
	
	public boolean IsEmpty() {
		return _items.isEmpty();	
	}
	
	public List<GridStatsResultItem> getItems() {
		return _items;
	}
}
