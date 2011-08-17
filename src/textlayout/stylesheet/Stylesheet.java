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
import java.io.*;
import java.util.*;

import textlayout.*;
import textlayout.grammar.*;
import util.GraphicsUtils;

/**
 * A stylesheet. (Note: these stylesheets are almost, but not quite, entirely
 * unlike CSS.)
 */
public class Stylesheet
{
	static final String RGBKEYWORD="([a-z][a-z0-9]*)|(_[a-zA-Z]+)";
	
	/**
	 * @param sheetData Stylesheet as string
	 * @throws LayoutException
	 */
	public Stylesheet(String sheetData) throws LayoutException
	{
		this(new ByteArrayInputStream(getBytes(sheetData)));
	}
	
	private static byte[] getBytes(String s)
	{
		try
		{
			return s.getBytes("UTF-8");
		}
		catch(UnsupportedEncodingException e)
		{
			throw new Error(e);
		}		
	}

	/**
	 * @param is Stylesheet as input string
	 * @throws LayoutException
	 */
	public Stylesheet(InputStream is) throws LayoutException
	{
		try
		{
			CSSGrammar cssg=new CSSGrammar(is);
			SimpleNode[] snChildren=cssg.Stylesheet().getChildren();
			for(int i=0;snChildren!=null && i<snChildren.length;i++)
			{
				SimpleNode snChild=snChildren[i];
				if(snChild.getRule().equals("Ruleset"))
					addRuleSet(snChild);
				else if(snChild.getRule().equals("AtRule"))
					addAtRule(snChild);
				else assert(false);
			}
		}
		catch(ParseException e)
		{
			throw new LayoutException(e);
		}
	}
	
	/** All properties declared in this stylesheet */
	private List<PropertyDeclaration> propertyDeclarations=new LinkedList<PropertyDeclaration>();

	/** All colours defined in this stylesheet */
	private List<RGBDeclaration> colours=new LinkedList<RGBDeclaration>();
	
	private void addAtRule(SimpleNode sn) throws StylesheetException
	{
		if(!sn.getName().equals("@rgb"))
			throw new StylesheetException(sn.getLine(),
				"@-rules other than @rgb are not supported"); 
		
		SimpleNode[] children=sn.getChildren();
		if(children.length<3)
			throw new StylesheetException(sn.getLine(),
				"Insufficient parameters for @rgb: expecting @rgb keyword 'description' <colour>;"); 
		String keyword=children[0].getName();
		if(!keyword.matches(RGBKEYWORD))
			throw new StylesheetException(sn.getLine(),
				"@rgb keyword must contain only lower-case letters and numbers");
		String description=children[1].getName();
		int maxLength=3;
		String colourStart=children[2].getName();
		Color c=null;
		String defaultKeyword=null;
		if(colourStart.startsWith("#"))
		{
			try
			{
				c=GraphicsUtils.parseColour(colourStart);
			}
			catch(NumberFormatException nfe)
			{
				throw new StylesheetException(sn.getLine(), nfe.getMessage());
			}
		}
		else if(colourStart.equals("rgb"))
		{
			if(children.length<10 || !
				(children[3].getName()+children[5].getName()+children[7].getName()+children[9].getName()).equals("(,,)"))
			{
				throw new StylesheetException(sn.getLine(),
					"Invalid colour syntax for @rgb; expecting rgb(r,g,b)");
			}
			
			try
			{
				c=new Color(
					Integer.parseInt(children[4].getName()),
					Integer.parseInt(children[6].getName()),
					Integer.parseInt(children[8].getName()));
			}
			catch(NumberFormatException nfe)
			{
				throw new StylesheetException(sn.getLine(),
					"Invalid number within @rgb; expecting rgb(r,g,b)");
			}
			
			maxLength=10;
		}
		else if(colourStart.matches(RGBKEYWORD)) 
		{
			defaultKeyword=colourStart; 
			// The actual colour will be set null
		}
		else
			throw new StylesheetException(sn.getLine(),
				"Invalid colour syntax for @rgb; expecting rgb(r,g,b) or #rgb or #rrggbb or keyword");
		
		if(children.length>maxLength)
			throw new StylesheetException(sn.getLine(),
				"Invalid syntax for @rgb: extra words at end");
		
		colours.add(new RGBDeclaration(keyword,c,description,defaultKeyword));
	}
	
	/**
	 * @return All declarations from stylesheet
	 */
	public PropertyDeclaration[] getPropertyDeclarations()
	{
		return propertyDeclarations.toArray(
			new PropertyDeclaration[propertyDeclarations.size()]);
	}
	
	/**
	 * @return All colours from stylesheet
	 */
	public RGBDeclaration[] getColours()
	{
		return colours.toArray(new RGBDeclaration[colours.size()]);
	}
	
	static String WILDCARD="!any";
	
	private void addRuleSet(SimpleNode sn) throws StylesheetException
	{
		SimpleNode[] children=sn.getChildren();

		// Get selector rules for each selector
		List<SelectorData> selectorList=new LinkedList<SelectorData>();
		for(int i=0;i<children.length;i++)
		{
			SimpleNode child=children[i];
			if(child.getRule().equals("Selector"))
			{
				selectorList.add(getRuleSetSelector(child));
			}
		}
		SelectorData[] selectors = selectorList.toArray(new SelectorData[selectorList.size()]);
		
		// For each property, add to list
		for(int i=0;i<children.length;i++)
		{
			SimpleNode child=children[i];
			if(child.getRule().equals("Declaration"))
			{
				int line=child.getLine();
				Property p=Property.get(child.getChildren()[0].getName(),line);
				SimpleNode value=child.getChildren()[1];
				SimpleNode[] valueNodes=value.getChildren();
				String[] values=new String[valueNodes.length];
				for(int j=0;j<values.length;j++)
				{
					values[j]=valueNodes[j].getName();
				}
				PropertyData pd=p.validate(values,line);
				
				// Add property to each selector
				for(int j=0;j<selectors.length;j++)
				{
					propertyDeclarations.add(new PropertyDeclaration(p,
						selectors[j].parts,pd,line,child.getColumn(),selectors[j].specificity));
				}
			}
		}
	}
	
	private static class SelectorData
	{
		String[] parts;
		int specificity;
	}
	
	private SelectorData getRuleSetSelector(SimpleNode sn) throws StylesheetException
	{
		SelectorData sd=new SelectorData();
		
		// Calculate specificity. Note that this is a simplified version of the 
		// CSS rules because we don't have any pseudo-elements or pseudo-classes
		// (in fact we don't support various other things yet, but anyway)
		sd.specificity=0;
		Set<String> s=new HashSet<String>(Arrays.asList(new String[] {
			"ElementName",	"Attrib","Id","Class"			
		}));
		SimpleNode[] children=sn.getChildren(s);
		for(int i=0;i<children.length;i++)
		{
			SimpleNode child=children[i];
			String rule=child.getRule();
			if(rule.equals("Id"))
			{
				sd.specificity+=256*256;
			}
			else if(rule.equals("Attrib") || rule.equals("Class"))
			{
				sd.specificity+=256;
			}
			else if(!child.getName().equals("*")) // ElementName
			{	
				sd.specificity++;
			}
		}

		// Process each selector part
		List<String> parts=new LinkedList<String>();
		parts.add(WILDCARD); // Can always start with any number of things
		children=sn.getChildren();
		boolean direct=false;
		for(int i=0;i<children.length;i++)
		{
			SimpleNode child=children[i];
			if(child.getRule().equals("Combinator"))
			{
				if(child.getName().equals(">"))
					direct=true;
				else
					throw new StylesheetException(child.getLine(),"Combinator "+child.getName()+" is not currently supported");
				continue;
			}
			// Must be a SelectorPart
			SimpleNode[] grandChildren=child.getChildren();
			for(int j=0;j<grandChildren.length;j++)
			{
				SimpleNode grandChild=grandChildren[j];
				String rule=grandChild.getRule();
				if(rule.equals("ElementName"))
				{
					String sName=grandChild.getName();
					if(!direct) parts.add(WILDCARD);
					parts.add(sName=="*" ? null : sName);
					direct=false;
				}
				else if(rule.equals("Pseudo"))
					throw new StylesheetException(grandChild.getLine(),"Psuedo-classes are not currently supported");
				else if(rule.equals("Attrib"))
					throw new StylesheetException(grandChild.getLine(),"Attributes are not currently supported");
				else if(rule.equals("Id"))
					throw new StylesheetException(grandChild.getLine(),"#IDs are not currently supported");
				else if(rule.equals("Class"))
					throw new StylesheetException(grandChild.getLine(),".classes are not currently supported");
				else assert(false);
			}
		}
		
		sd.parts = parts.toArray(new String[parts.size()]);
		return sd;
	}
	
}
