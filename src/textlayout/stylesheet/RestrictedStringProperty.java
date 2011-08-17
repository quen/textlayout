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