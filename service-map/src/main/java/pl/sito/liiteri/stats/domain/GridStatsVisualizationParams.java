package pl.sito.liiteri.stats.domain;

import com.esri.core.geometry.Envelope;

public class GridStatsVisualizationParams
{
	public enum GridStatsVisualizationType {
		Square,
		Circle
	}
	
	private Envelope bbox;
	private int width;
	private int height;
	private int gridSize;
	private GridStatsVisualizationType visualizationType;
	private String[] colors;
	
	public Envelope getBbox()
	{
		return bbox;
	}
	public void setBbox(Envelope bbox)
	{
		this.bbox = bbox;
	}
	public int getWidth()
	{
		return width;
	}
	public void setWidth(int width)
	{
		this.width = width;
	}
	public int getHeight()
	{
		return height;
	}
	public void setHeight(int height)
	{
		this.height = height;
	}
	public int getGridSize()
	{
		return gridSize;
	}
	public void setGridSize(int gridSize)
	{
		this.gridSize = gridSize;
	}
	public GridStatsVisualizationType getVisualizationType()
	{
		return visualizationType;
	}
	public void setVisualizationType(GridStatsVisualizationType visualizationType)
	{
		this.visualizationType = visualizationType;
	}
	public String[] getColors()
	{
		return colors;
	}
	public void setColors(String[] colors)
	{
		this.colors = colors;
	}
}
