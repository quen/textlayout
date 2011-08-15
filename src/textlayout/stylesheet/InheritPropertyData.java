package textlayout.stylesheet;

/**
 * Psuedo-data value for nonspecified properties
 */
public class InheritPropertyData extends PropertyData
{
	/**
	 * Single value
	 */
	public static InheritPropertyData VALUE = new InheritPropertyData();
	
	private InheritPropertyData()
	{
	}
	
	@Override
	public String toString()
	{
		return "inherit";
	}
	
	@Override
	public boolean inherit()
	{
		return true;
	}
}