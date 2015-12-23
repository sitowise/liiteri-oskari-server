package pl.sito.liiteri.groupings.domain;

import java.util.ArrayList;
import java.util.List;

public class UserTheme
{
	private long id;
	private String name;
	private List<Long> layerIds;
	
	public UserTheme() {
		layerIds = new ArrayList<Long>();
	}
	
	public long getId()
	{
		return id;
	}
	public void setId(long id)
	{
		this.id = id;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public List<Long> getLayerIds()
	{
		return layerIds;
	}
	public void setLayerIds(List<Long> layerIds)
	{
		this.layerIds = layerIds;
	}
}
