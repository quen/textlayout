package textlayout.stylesheet;

/**
 * Stores properties that return their data as plain strings.
 */
public class StringPropertyData extends PropertyData
{
	private String value;
	StringPropertyData(String value)
	{
		this.value=value;
	}
	String getValue()
	{
		return this.value;
	}
	@Override
	public String toString()
	{
		return this.value;
	}
}