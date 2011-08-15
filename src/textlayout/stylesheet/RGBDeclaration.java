package textlayout.stylesheet;

import java.awt.Color;

/** Defines a colour keyword */
public class RGBDeclaration
{
	RGBDeclaration(String keyword,Color c,String description,String defaultKeyword)
	{
		this.keyword=keyword;
		this.c=c;
		this.description=description;
		this.defaultKeyword=defaultKeyword;
	}
	
	private String keyword;
	private Color c;
	private String description;
	private String defaultKeyword;
	
	/**
	 * @return Colour, or null if using default
	 */
	public Color getRGB()
	{
		return c;
	}
	/**
	 * @return Default keyword, or null if using fixed colour
	 */
	public String getDefaultKeyword()
	{
		return defaultKeyword;
	}
	/**
	 * @return Description (for user display)
	 */
	public String getDescription()
	{
		return description;
	}
	/**
	 * @return Keyword
	 */
	public String getKeyword()
	{
		return keyword;
	}
}