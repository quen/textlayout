package textlayout.stylesheet;

import util.StringUtils;

class NumberProperty extends Property
{
	private boolean allowDefault=false;
	
	NumberProperty(String name,PropertyData defaultValue)
	{
		this(name,defaultValue,false);
	}
	
	NumberProperty(String name,PropertyData defaultValue,boolean allowDefault)
	{
		super(name, defaultValue);
		this.allowDefault=allowDefault;
	}
	
	@Override
	PropertyData validate(String[] data,int lineNumber) throws StylesheetException
	{
		if(data.length!=1)
			throw new StylesheetException(lineNumber,getName()+": "+StringUtils.join(" ",data)+" - requires a single numeric value e.g 14, 0.1f");
		if(allowDefault && data[0].equals("default"))
		{
			return new NumberPropertyData(true);
		}
		if(data[0].matches(".*f$"))
		{
			try
			{
				return new NumberPropertyData(Float.parseFloat(data[0].substring(0,data[0].length()-1)));
			}
			catch(NumberFormatException nfe)
			{
				throw new StylesheetException(lineNumber,getName()+": "+data[0]+" - could not parse as number, e.g. 0.1f");
			}
		}
		else
		{
			try
			{
				return new NumberPropertyData(Integer.parseInt(data[0]));
			}
			catch(NumberFormatException nfe)
			{
				throw new StylesheetException(lineNumber,getName()+": "+data[0]+" - could not parse as number, e.g. 14");
			}
		}
	}
}