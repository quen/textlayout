package textlayout.stylesheet;

import java.awt.Color;

import util.*;

class RGBProperty extends Property
{
	RGBProperty(String name,PropertyData defaultValue)
	{
		super(name, defaultValue);
	}
	@Override
	PropertyData validate(String[] data,int lineNumber) throws StylesheetException
	{
		// Check opacity
		int opacity=255;
		if(data.length==2 || data.length==9)
		{
			String percent=data[data.length-1];
			if(!percent.matches("[0-9]+%"))
			{
				throw new StylesheetException(lineNumber,getName()+": "+StringUtils.join(" ",data)+
				" - expecting a colour keyword or numeric colour #rgb, #rrggbb, rgb(r,g,b), optionally followed by nn%");
			}
			opacity=(255*Integer.parseInt(percent.substring(0,percent.length()-1))+50)/100;
		}
		
		if(data.length==1 || data.length==2)
		{
			if(data[0].startsWith("#"))
			{
				try
				{
					return new RGBPropertyData(GraphicsUtils.combineOpacity(GraphicsUtils.parseColour(data[0]),opacity));
				}
				catch(NumberFormatException nfe)
				{
					throw new StylesheetException(lineNumber,getName()+": "+data[0]+" - "+nfe.getMessage());
				}
			}
			else if(data[0].equals("transparent"))
			{
				return new RGBPropertyData(true);
			}
			else if(data[0].matches(Stylesheet.RGBKEYWORD))
			{
				return new RGBPropertyData(data[0],opacity);
			}
			else
				throw new StylesheetException(lineNumber,getName()+": "+StringUtils.join(" ",data)+
				" - expecting a colour keyword or numeric colour #rgb, #rrggbb, rgb(r,g,b), optionally followed by nn%");
		}
		else if(data.length==8 || data.length==9)
		{
			if(!(data[0]+data[1]+data[3]+data[5]+data[7]).equals("rgb(,,)"))
				throw new StylesheetException(lineNumber,getName()+": "+StringUtils.join(" ",data)+" - expecting rgb(r,g,b)");
			try
			{
				return new RGBPropertyData(new Color(
					Integer.parseInt(data[2]),	Integer.parseInt(data[4]),
					Integer.parseInt(data[6]),opacity));
			}
			catch(NumberFormatException nfe)
			{
				throw new StylesheetException(lineNumber,getName()+": "+StringUtils.join(" ",data)+" - expecting rgb(r,g,b)");
			}
		}
		else
			throw new StylesheetException(lineNumber,getName()+": "+StringUtils.join(" ",data)+
			" - expecting a colour keyword or numeric colour #rgb, #rrggbb, rgb(r,g,b), optionally followed by nn%");
	}
}