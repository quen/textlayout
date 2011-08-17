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
package textlayout;

import java.awt.Graphics2D;
import java.util.*;

import org.w3c.dom.*;

import textlayout.stylesheet.*;
import util.StringUtils;

/**
 * Layout block with margins.
 */
class MarginBlock implements LayoutBlock
{
	private String[] context;
	
  /**
	 * Inner blocks
	 */
  private LayoutBlock innerBlock;

  /**
	 * Width of margin (default 100)
	 */
  private int marginWidth=100; 

  /**
	 * Creates block with the given block style.
   * @param context Tag stack
   * @param innerBlock Inner block
   * @param sc Context
   * @throws LayoutException 
	 */
  MarginBlock(String[] context,LayoutBlock innerBlock,StyleContext sc) throws LayoutException
  {
  	this.context=context;
    this.innerBlock=innerBlock;
    updateStyle(sc);
  }

	@Override
	public void updateStyle(StyleContext sc) throws LayoutException
	{
		innerBlock.updateStyle(sc);
		marginWidth=sc.getNumber(Property.MARGIN_WIDTH,context);
		if(marginWidth==-1) marginWidth=100;
		innerBlock.reflow(marginWidth);
	}

  @Override
	public void paint(Graphics2D g,int iX,int iY)
  {
  		innerBlock.paint(g,iX-marginWidth,iY);
  }

  @Override
	public int getFirstBaseline()
  {
  		return innerBlock.getFirstBaseline();
  }
  
  @Override
	public boolean setCurrent(StyleContext sc,boolean current) throws LayoutException
  {
  		return innerBlock.setCurrent(sc,current);
  }
  
	@Override
	public void clearHighlight()
	{
		innerBlock.clearHighlight();
	}
  
	@Override
	public void setHighlight(int lowX,int lowY,int highX,int highY)
	{
		innerBlock.setHighlight(
			lowX==Layout.HIGHLIGHT_TOSTART ? lowX : lowX+marginWidth,lowY,
			highX==Layout.HIGHLIGHT_TOEND ? highX : highX+marginWidth,highY);
	}
	
	@Override
	public LayoutInline.NodePos getNodePos(int targetX, int targetY,
		List<LayoutThing> l)
	{
		return innerBlock.getNodePos(targetX+marginWidth,targetY,l);
	}
  
	@Override
	public String getHighlightText()
	{
		return innerBlock.getHighlightText();
	}
	
  @Override
	public String debugDisplay(String indent)
  {
  	  StringBuffer sb=new StringBuffer();
  	  sb.append(indent+"MarginBlock ["+StringUtils.join("/",context)+"]\n");
  	  sb.append(innerBlock.debugDisplay(indent+"  "));
  	  return sb.toString();
  }

  @Override
	public void buildXML(Node parent,Map<LayoutInline, LayoutInline.NodePos> translation)
  {
		innerBlock.buildXML(SurroundedBlock.buildXML(parent,translation,context),translation);  
  }

	@Override
	public int getFlowCategory()
	{
		return FLOWCATEGORY_LEFTMARGIN;
	}

	@Override
	public int getHeight()
	{
		return innerBlock.getHeight();
	}

	@Override
	public int getMaxX()
	{
		return 0;
	}

	@Override
	public int getMinX()
	{
		return -marginWidth;
	}

	@Override
	public int getUsedWidth()
	{
		return marginWidth;
	}

	@Override
	public void reflow(int width)
	{
		innerBlock.reflow(marginWidth);
	}  
}
