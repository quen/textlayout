package textlayout.stylesheet;

import java.awt.GraphicsEnvironment;
import java.util.*;

/** 
 * Font properties contain a comma-separated list of fonts. The value returned
 * is a string naming the first available font from that list.
 */
public class FontProperty extends Property
{
	protected FontProperty(String name,PropertyData defaultValue)
	{
		super(name, defaultValue);
	}

	/** All available font families */
	private static Set<String> availableFonts = new HashSet<String>(Arrays.asList(
		GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
	
	/** Means that platform default UI font should be used */
	public final static String DEFAULT="default";
	
	/** Generic Java font families */
	private static Set<String> genericFamilies = new HashSet<String>(Arrays.asList(
		new String[] {DEFAULT,"Serif","SansSerif","Monospaced","Dialog","DialogInput"}));
	
	@Override
	PropertyData validate(String[] data,int lineNumber)
		throws StylesheetException
	{
		for(int i=1;i<data.length;i+=2)
		{
			if(i==data.length-1 || !data[i].equals(","))
				throw new StylesheetException(lineNumber,
					"Invalid font list - must be comma-separated, and font names quoted");
		}
		if(!genericFamilies.contains(data[data.length-1]))
		{
			throw new StylesheetException(lineNumber,
				"Invalid font list - must end with a Java generic font family, one of: "+
				"Serif, SansSerif, Monospaced, Dialog, DialogInput");			
		}
		
		for(int i=0;i<data.length-1;i+=2)
		{
			if(availableFonts.contains(data[i]) || genericFamilies.contains(data[i]))
				return new StringPropertyData(data[i]);
		}
		return new StringPropertyData(data[data.length-1]);
	}
}
