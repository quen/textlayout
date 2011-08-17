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
 * Psuedo-data value for nonspecified properties
 */
public class InheritPropertyData extends PropertyData
{
	/**
	 * Single value
	 */
	public static InheritPropertyData VALUE = new InheritPropertyData();
	
	private InheritPropertyData()
	{
	}
	
	@Override
	public String toString()
	{
		return "inherit";
	}
	
	@Override
	public boolean inherit()
	{
		return true;
	}
}