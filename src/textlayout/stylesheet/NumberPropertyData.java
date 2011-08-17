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