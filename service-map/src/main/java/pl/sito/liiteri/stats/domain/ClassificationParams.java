package pl.sito.liiteri.stats.domain;

public class ClassificationParams
{
	public enum ClassificationMode {
		Default,
		Distinct,
		Discontinuous,
	}
	
	public enum ClassificationType {
		Jenks,
		Quantile,
		Equal,
		Manual,
	}
	
	public enum DataTransformationType {
		None(0),
		AbsoluteValues(1);
		
		private final int value;
	    private DataTransformationType(int value) {
	        this.value = value;
	    }

	    public int getValue() {
	        return value;
	    }
	}
	
	private ClassificationType _classificationType;
	private int _numberOfClasses;
	private String[] _colors;
	private DataTransformationType _dataTransformation;
	private ClassificationMode _classificationMode;
		
	public ClassificationType getClassificationType()
	{
		return _classificationType;
	}
	public void setClassificationType(ClassificationType classificationType)
	{
		this._classificationType = classificationType;
	}
	public int getNumberOfClasses()
	{
		return _numberOfClasses;
	}
	public void setNumberOfClasses(int numberOfClasses)
	{
		this._numberOfClasses = numberOfClasses;
	}
	public String[] getColors()
	{
		return _colors;
	}
	public void setColors(String[] colors)
	{
		this._colors = colors;
	}
	public DataTransformationType getDataTransformation()
	{
		return _dataTransformation;
	}
	public void setDataTransformation(DataTransformationType dataTransformation)
	{
		this._dataTransformation = dataTransformation;
	}
	public ClassificationMode getClassificationMode()
	{
		return _classificationMode;
	}
	public void setClassificationMode(ClassificationMode classificationMode)
	{
		this._classificationMode = classificationMode;
	}
}
