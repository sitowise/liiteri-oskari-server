package fi.nls.oskari.domain.groupings;

public class GroupingThemeData {

	private long id;
	private String name;
	private long dataId;
	private int dataType;
	private long oskariGroupingThemeId;
	private String status;
	
	public long getId()
	{
	return id;
	}
	
	public String getName()
	{
	return name;
	}
	
	public long getDataId()
	{
	return dataId;
	}
	
	public int getDataType()
	{
	return dataType;
	}
	
	public long getOskariGroupingThemeId()
	{
	return oskariGroupingThemeId;
	}
	
	public void setId(long id)
	{
	 this.id = id;
	}
	
	public void setName(String name)
	{
	 this.name =name;
	}
	
	public void setDataId(long dataId)
	{
	this.dataId = dataId;
	}
	
	public void setDataType(int dataType)
	{
	 this.dataType = dataType;
	}
	
	public void setOskariGroupingThemeId(long oskariGroupingThemeId)
	{
	 this.oskariGroupingThemeId = oskariGroupingThemeId;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}
	
}
