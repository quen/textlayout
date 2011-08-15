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
