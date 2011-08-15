package textlayout.stylesheet;

/**
 * Stores properties that return their data as plain strings.
 */
public class NumberPropertyData extends PropertyData
{
	private boolean absolute,def;
	private int absoluteValue;
	private float relativeValue;
	NumberPropertyData(float relativeValue)
	{
		this.relativeValue=relativeValue;
		absolute=false;
	}
	NumberPropertyData(int absoluteValue)
	{
		this.absoluteValue=absoluteValue;
		absolute=true;
	}
	NumberPropertyData(boolean def)
	{
		assert(def);
		this.def=true;
	}
	boolean isAbsolute()
	{
		return absolute;
	}
	boolean isDefault()
	{
		return def;
	}
	int getAbsoluteValue()
	{
		return absoluteValue;
	}
	int getRelativeValue(int relativeFontSize)
	{
		return Math.round(relativeValue*(float)relativeFontSize);
	}
	@Override
	public String toString()
	{
		return absolute ? absoluteValue+"" : relativeValue+"f";
	}
}