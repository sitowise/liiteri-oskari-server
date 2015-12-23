package fi.nls.oskari.urbanplanning.area;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.nls.oskari.util.JSONHelper;

public class AreaWrapper {

	private Long id;
	private JSONObject ob;
	private boolean isEmpty;
	private String conservationTypeName ="";
	private List<AreaWrapper> children = new ArrayList<AreaWrapper>();
	private Long changeCount;
	private Long buildingCount;
	private Double floorSpace;
	private Double changeFloorSpace;
	public AreaWrapper(Long id, JSONObject ob) {
		this.id = id;
		this.ob = ob;
	}

	public Long getId() {
		return id;
	}
	
	public boolean getIsEmpty()
	{
		return isEmpty;
	}
	
	public void setIsEmpty(boolean isEmpty)
	{
		this.isEmpty = isEmpty;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public JSONObject getJSONObject() {
		if (children.size() != 0) {
			JSONArray arr = new JSONArray();
			for (AreaWrapper c : children) {
				arr.put(c.getJSONObject());
			}
			JSONHelper.putValue(ob, "data", arr);
		}
		return ob;
	}

	public void setJSONObject(JSONObject ob) {
		this.ob = ob;
	}

	public List<AreaWrapper> getChildren() {
		return children;
	}

	public void setJSONObject(List<AreaWrapper> children) {
		this.children = children;
	}

	public String getConservationTypeName()
	{
		return conservationTypeName;
	}
	
	public void setConservationTypeName(String conservationTypeName)
	{
		 this.conservationTypeName = conservationTypeName;
	}
	
	public Long getBuildingCount()
	{
		return buildingCount;
	}
	public void setBuildingCount(Long buildingCount)
	{
		 this.buildingCount = buildingCount;
	}
	
	public Long getChangeCount()
	{
		return changeCount;
	}
	public void setChangeCount(Long changeCount)
	{
		 this.changeCount = changeCount;
	}
	
	public Double getFloorSpace()
	{
		return floorSpace;
	}
	public void setFloorSpace(Double floorSpace)
	{
		 this.floorSpace = floorSpace;
	}
	
	public Double getChangeFloorSpace()
	{
		return changeFloorSpace;
	}
	public void setChangeFloorSpace(Double changeFloorSpace)
	{
		 this.changeFloorSpace = changeFloorSpace;
	}

}
