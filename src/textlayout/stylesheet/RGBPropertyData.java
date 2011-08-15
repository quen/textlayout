package textlayout.stylesheet;

import java.awt.Color;

/**
 * Stores properties that return colours.
 */
public class RGBPropertyData extends PropertyData
{
	/**
	 * Transparent colour
	 */
	public final static Color TRANSPARENT=new Color(0,0,0,0);
	
	int opacity=255;
	
	private Color value;
	private String keyword;
	RGBPropertyData(Color value)
	{
		this.value=value;
	}
	RGBPropertyData(String keyword,int opacity)
	{
		this.keyword=keyword;
		this.opacity=opacity;
	}
	RGBPropertyData(boolean transparent)
	{
		if(!transparent) throw new Error("What?");		
	}
	Color getValue(StyleContext sc)
	{
		if(value!=null)
			return value;
		else if(keyword!=null)
			return sc.getColour(keyword,opacity);
		else 
			return TRANSPARENT;
	}
	@Override
	public String toString()
	{
		if(value!=null)
			return "rgb("+value.getRed()+","+value.getGreen()+","+value.getBlue()+") "+
				(value.getAlpha()==255 ? "" : ((value.getAlpha()*100)/255)+"") +"%";
		else if(keyword!=null)
			return keyword+(opacity!=255 ? " "+((100*opacity+128)/255)+"%" : "");
		else return "transparent";
	}
}