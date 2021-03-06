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
 * Represents a property declaration from a stylesheet.
 */
class PropertyDeclaration
{
	private Property p;
	private int specificity;
	private int filePos;
	private String[] required;
	private PropertyData value;
	
	PropertyDeclaration(Property p,String[] required,PropertyData value,int lineNumber,int columnNumber,int specificity)
	{
		this.p=p;
		this.required=required;
		this.value=value;
		this.filePos=lineNumber*10000+columnNumber;
		this.specificity=specificity;
	}
	
	public String getLastElement()
	{
		String last=required[required.length-1];
		if(last==null) 
			return "*";
		else 
			return last;
	}
	
	public Property getProperty()
	{
		return p;
	}
	
	public PropertyData getValue()
	{
		return value;
	}
	
	/**
	 * @return the lineNumber
	 */
	public int getFilePos()
	{
		return filePos;
	}

	/**
	 * @return the specificity
	 */
	public int getSpecificity()
	{
		return specificity;
	}

	boolean matches(String[] context)
	{
		return matches(0,context,0);
	}
	
	private boolean matches(int requiredPos,String[] context,int contextPos)
	{
		// This recursive match algorithm is probably horrifically slow. Time
		// will tell.
		if(requiredPos==required.length && contextPos==context.length) return true;
		if(requiredPos>=required.length || contextPos>=context.length) return false; 
		
		if(required[requiredPos]==Stylesheet.WILDCARD)
		{
			return matches(requiredPos+1,context,contextPos) ||
				matches(requiredPos,context,contextPos+1);
		}
		else if(required[requiredPos]==null)
			return matches(requiredPos+1,context,contextPos+1);
		else
		{
			// Note that we are stripping attributes here; this is where we could
			// implement checks on them if we wanted.
			if(!StyleContext.stripAttributes(context[contextPos]).equals(required[requiredPos]))
				return false;
			return matches(requiredPos+1,context,contextPos+1);
		}
	}
}