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