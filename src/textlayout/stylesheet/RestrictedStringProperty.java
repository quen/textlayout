package textlayout.stylesheet;

import java.util.*;

import util.StringUtils;

class RestrictedStringProperty extends Property
{
	private Set<String> allowed=new HashSet<String>();
	
	RestrictedStringProperty(String name,String[] allowed,PropertyData defaultValue)
	{
		super(name, defaultValue);
		this.allowed.addAll(Arrays.asList(allowed));
	}
	
	@Override
	PropertyData validate(String[] data,int lineNumber) throws StylesheetException
	{
		if(data.length!=1)
			throw new StylesheetException(lineNumber,getName()+": "+StringUtils.join(" ",data)+" - requires a single string value");
		if(!allowed.contains(data[0]))
			throw new StylesheetException(lineNumber,getName()+": "+StringUtils.join(" ",data)+" - not a permitted value");
		return new StringPropertyData(data[0]);
	}
}