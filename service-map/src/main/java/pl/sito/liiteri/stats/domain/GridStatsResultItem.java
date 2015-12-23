package pl.sito.liiteri.stats.domain;

public class GridStatsResultItem
{
	private long id;
	private int northing;
	private int easting;
	private double value;
	
	public GridStatsResultItem() {
		
	}
	
	public long getId()
	{
		return id;
	}
	public void setId(long id)
	{
		this.id = id;
	}
	public int getNorthing()
	{
		return northing;
	}
	public void setNorthing(int northing)
	{
		this.northing = northing;
	}
	public int getEasting()
	{
		return easting;
	}
	public void setEasting(int easting)
	{
		this.easting = easting;
	}
	public double getValue()
	{
		return value;
	}
	public void setValue(double value)
	{
		this.value = value;
	}

}
