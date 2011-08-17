/*
This file is part of leafdigital textlayout.

textlayout is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

textlayout is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with textlayout. If not, see <http://www.gnu.org/licenses/>.

Copyright 2011 Samuel Marshall.
*/
package textlayout.stylesheet;

import java.util.*;

/** 
 * Represents a stylesheet property definition. There is one of these for
 * each named property that the system supports.
 */
public abstract class Property
{
	/** Property name */
	private String name;
	/** Default value */
	private PropertyData defaultValue;

	/**
	 * Constructs property (only called by subclasses)
	 * @param name Property name
	 * @param defaultValue Default value if property isn't specified
	 */
	protected Property(String name, PropertyData defaultValue)
	{
		this.name=name;
		this.defaultValue=defaultValue;
	}
	
	/**
	 * @return Property name
	 */
	String getName()
	{
		return name;
	}
	
	PropertyData getDefaultValue()
	{
		return defaultValue;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
	
	/**
	 * Validates input from parser.
	 * @param data Input data as array of string tokens
	 * @param lineNumber Line number in case an exception needs throwing
	 * @return Data for property
	 * @throws StylesheetException If it is invalid
	 */
	abstract PropertyData validate(String[] data,int lineNumber) throws StylesheetException;
	
	/** Map of all current properties */
	private static Map<String, Property> properties =
		new HashMap<String, Property>();
	
	/**
	 * @param name Name of desired property definition
	 * @param lineNumber Line number for use in error messages
	 * @return Property definition
	 * @throws StylesheetException If property doesn't exist
	 */
	static Property get(String name,int lineNumber) throws StylesheetException
	{
		Property p = properties.get(name);
		if(p==null)
			throw new StylesheetException(lineNumber,"Unknown property: "+name);
		return p;
	}
	
	/**
	 * @param name Name of desired property definition
	 * @return Property definition
	 * @throws Error If property doesn't exist
	 */
	static Property get(String name)
	{
		Property p = properties.get(name);
		if(p==null)
			throw new Error("Unknown property: "+name);
		return p;
	}
	
	/** Single constant used for inherit */
	private static PropertyData INHERIT=InheritPropertyData.VALUE;

	/** Property value: type=inline */
	public static String V_TYPE_INLINE="inline";
	/** Property value: type=block */
	public static String V_TYPE_BLOCK="block";
	/** Property value: type=unknown */
	public static String V_TYPE_UNKNOWN="unknown";
	/** Property value: type=margin-block */
	public static String V_TYPE_MARGIN_BLOCK="margin-block";
	/** Property value: font-weight=normal */
	public static String V_FONT_WEIGHT_NORMAL="normal";
	/** Property value: font-weight=bold */
	public static String V_FONT_WEIGHT_BOLD="bold";
	/** Property value: font-style=normal */
	public static String V_FONT_STYLE_NORMAL="normal";
	/** Property value: font-style=italic */
	public static String V_FONT_STYLE_ITALIC="italic";
	/** Property value: font-underline=none */
	public static String V_FONT_UNDERLINE_NONE="none";
	/** Property value: font-underline=solid */
	public static String V_FONT_UNDERLINE_SOLID="solid";
	/** Property value: text-align=left */
	public static String V_TEXT_ALIGN_LEFT="left";
	/** Property value: text-align=centre */
	public static String V_TEXT_ALIGN_CENTRE="centre";
	/** Property value: text-align=right */
	public static String V_TEXT_ALIGN_RIGHT="right";
	/** Property value: wrap-style=normal */
	public static String V_WRAP_STYLE_NORMAL="normal";
	/** Property value: wrap-style=none */
	public static String V_WRAP_STYLE_NONE="none";
	
	/** Property: type */
	public static Property TYPE = new RestrictedStringProperty("type",new String[] {V_TYPE_INLINE,V_TYPE_BLOCK,V_TYPE_MARGIN_BLOCK,V_TYPE_UNKNOWN},new StringPropertyData(V_TYPE_UNKNOWN));
	/** Property: font-name */
	public static Property FONT_NAME = new FontProperty("font-name",INHERIT);
	/** Property: font-size */
	public static Property FONT_SIZE = new NumberProperty("font-size",new NumberPropertyData(1.0f),true);
	/** Property: font-weight */
	public static Property FONT_WEIGHT = new RestrictedStringProperty("font-weight",new String[] {V_FONT_WEIGHT_NORMAL,V_FONT_WEIGHT_BOLD},INHERIT);
	/** Property: font-style */
	public static Property FONT_STYLE = new RestrictedStringProperty("font-style",new String[] {V_FONT_STYLE_NORMAL,V_FONT_STYLE_ITALIC},INHERIT);
	/** Property: font-underline */
	public static Property FONT_UNDERLINE = new RestrictedStringProperty("font-underline",new String[] {V_FONT_UNDERLINE_NONE,V_FONT_UNDERLINE_SOLID},INHERIT);
	/** Property: text-rgb */
	public static Property TEXT_RGB = new RGBProperty("text-rgb",INHERIT);
	/** Property: text-align */
	public static Property TEXT_ALIGN = new RestrictedStringProperty("text-align",new String[] {V_TEXT_ALIGN_LEFT,V_TEXT_ALIGN_CENTRE,V_TEXT_ALIGN_RIGHT},INHERIT);
	/** Property: text-indent */
	public static Property TEXT_INDENT = new NumberProperty("text-indent",new NumberPropertyData(0));
	/** Property: text-first-indent */
	public static Property TEXT_FIRST_INDENT = new NumberProperty("text-first-indent",new NumberPropertyData(0));
	/** Property: background-rgb */
	public static Property BACKGROUND_RGB = new RGBProperty("background-rgb",new RGBPropertyData(true));
	/** Property: text-background-rgb */
	public static Property TEXT_BACKGROUND_RGB = new RGBProperty("text-background-rgb",INHERIT);
	/** Property: border-rgb */
	public static Property BORDER_RGB = new RGBProperty("border-rgb",new RGBPropertyData("fg",255));
	/** Property: gap-left */
	public static Property GAP_LEFT = new NumberProperty("gap-left",new NumberPropertyData(0));
	/** Property: gap-right */
	public static Property GAP_RIGHT = new NumberProperty("gap-right",new NumberPropertyData(0));
	/** Property: gap-top */
	public static Property GAP_TOP = new NumberProperty("gap-top",new NumberPropertyData(0));
	/** Property: gap-bottom */
	public static Property GAP_BOTTOM = new NumberProperty("gap-bottom",new NumberPropertyData(0));
	/** Property: pad-left */
	public static Property PAD_LEFT = new NumberProperty("pad-left",new NumberPropertyData(0));
	/** Property: pad-right */
	public static Property PAD_RIGHT = new NumberProperty("pad-right",new NumberPropertyData(0));
	/** Property: pad-top */
	public static Property PAD_TOP = new NumberProperty("pad-top",new NumberPropertyData(0));
	/** Property: pad-bottom */
	public static Property PAD_BOTTOM = new NumberProperty("pad-bottom",new NumberPropertyData(0));
	/** Property: border-left */
	public static Property BORDER_LEFT = new NumberProperty("border-left",new NumberPropertyData(0));
	/** Property: border-right */
	public static Property BORDER_RIGHT = new NumberProperty("border-right",new NumberPropertyData(0));
	/** Property: border-top */
	public static Property BORDER_TOP = new NumberProperty("border-top",new NumberPropertyData(0));
	/** Property: border-bottom */
	public static Property BORDER_BOTTOM = new NumberProperty("border-bottom",new NumberPropertyData(0));
	/** Property: width */
	public static Property WIDTH = new NumberProperty("width",new NumberPropertyData(-1));
	/** Property: wrap-style */
	public static Property WRAP_STYLE = new RestrictedStringProperty("wrap-style",new String[] {V_WRAP_STYLE_NONE,V_WRAP_STYLE_NORMAL},INHERIT);
	/** Property: margin-width */
	public static Property MARGIN_WIDTH = new NumberProperty("margin-width",new NumberPropertyData(-1));
	/** Property: match-baseline */
	public static Property MATCH_BASELINE = new NumberProperty("match-baseline",new NumberPropertyData(0));
	/** Property: outline */
	public static Property OUTLINE = new RGBProperty("outline",new RGBPropertyData(true));
	
	/** Inset groups: gap on each side. */
	public static Property[] I_GAP={GAP_TOP,GAP_LEFT,GAP_BOTTOM,GAP_RIGHT};
	/** Inset groups: pad on each side. */
	public static Property[] I_PAD={PAD_TOP,PAD_LEFT,PAD_BOTTOM,PAD_RIGHT};
	/** Inset groups: border on each side. */
	public static Property[] I_BORDER={BORDER_TOP,BORDER_LEFT,BORDER_BOTTOM,BORDER_RIGHT};
	
	/** Set up properties */
	static	
	{
		Property[] propertyArray=
		{
			TYPE,
			FONT_NAME,
			FONT_SIZE,
			FONT_WEIGHT,
			FONT_STYLE,
			FONT_UNDERLINE,
			TEXT_RGB,
			TEXT_ALIGN,
			TEXT_INDENT,
			TEXT_FIRST_INDENT,
			BACKGROUND_RGB,
			TEXT_BACKGROUND_RGB,
			BORDER_RGB,
			GAP_LEFT,
			GAP_RIGHT,
			GAP_TOP,
			GAP_BOTTOM,
			PAD_LEFT,
			PAD_RIGHT,
			PAD_TOP,
			PAD_BOTTOM,
			BORDER_LEFT,
			BORDER_RIGHT,
			BORDER_TOP,
			BORDER_BOTTOM,
			WIDTH,
			WRAP_STYLE,
			MARGIN_WIDTH,
			MATCH_BASELINE,
			OUTLINE
		};
		for(int i=0;i<propertyArray.length;i++)
		{
			properties.put(propertyArray[i].getName(),propertyArray[i]);
		}
	}
}