package textlayout;

import java.awt.*;
import java.util.*;
import java.util.List;

import org.w3c.dom.*;

import textlayout.stylesheet.*;
import util.StringUtils;
import util.xml.XML;

/** Handles the borders etc. to paint a block, which may contain text or 
 * something else */
class SurroundedBlock implements LayoutBlock
{
	/** Contained block */
	private LayoutBlock innerBlock=null;
	
  /** Current set width */
  private int width=-1;

  /** Styles for this block */
  private String[] context=null;

  /** Margin */
  private Insets margin=new Insets(0,0,0,0);

  /** Padding */
  private Insets padding=new Insets(0,0,0,0);

  /** Border */
  private Insets border=new Insets(0,0,0,0);

  /** Background or null */
  private Color backgroundColour=null;

  /** Border or null */
  private Color borderColour=null;

  /** True if this is already current */
  private boolean current=false;

	/** True if there is any highlight at present */
	private boolean hasHighlight=false;
	
  /**
   * Creates block with the given block style.
   * @param innerBlock Block that goes inside the surround
   * @param context Element context
   * @param sc Styles
   * @throws LayoutException
   */
  SurroundedBlock(LayoutBlock innerBlock,String[] context,StyleContext sc) throws LayoutException
  {
  		this.innerBlock=innerBlock;
    this.context=context;
    resolveStyle(sc);
  }

	/**
	 * Updates style using a new StyleContext.
	 * @param sc Styles
	 * @throws LayoutException 
	 */
	@Override
	public void updateStyle(StyleContext sc) throws LayoutException
	{
		resolveStyle(sc);
		innerBlock.updateStyle(sc);
		int oldWidth=width;
		width=-1;
		reflow(oldWidth);		
	}

  SurroundedBlock()
  {
    this.context=null;
  }

  void resolveStyle(StyleContext sc) throws LayoutException
  {
    if(context!=null)
    {
      String[] modifiedContext=context;
      if(current)
      {
        List<String> l=new LinkedList<String>();
        l.add("_current");
        l.addAll(Arrays.asList(context));
        modifiedContext = l.toArray(new String[l.size()]);
      }

      margin=sc.getInsets(Property.I_GAP,modifiedContext);
      padding=sc.getInsets(Property.I_PAD,modifiedContext);
      border=sc.getInsets(Property.I_BORDER,modifiedContext);
      backgroundColour=sc.getRGB(Property.BACKGROUND_RGB,modifiedContext);
      if(backgroundColour==RGBPropertyData.TRANSPARENT) backgroundColour=null;
      borderColour=sc.getRGB(Property.BORDER_RGB,modifiedContext);
      if(borderColour==RGBPropertyData.TRANSPARENT) borderColour=null;
    }
  }
  
  /**
   * Recalculates block for given width.
   * @param width Width in pixels
   */
  @Override
	public void reflow(int width)
  {  	
    this.width=width;
    
    int innerWidth=width
      -margin.left-margin.right
      -padding.left-padding.right
      -border.left-border.right;
    innerWidth = Math.max(1, innerWidth);
		innerBlock.reflow(innerWidth);
  }

  /**
   * Gets height at last specified width.
   * @return Height in pixels
   */
  @Override
	public int getHeight()
  {
    return innerBlock.getHeight() +
      margin.top+margin.bottom+padding.top+padding.bottom+
      border.top+border.bottom;
  }

  @Override
	public int getUsedWidth()
  {
  		return margin.left+margin.right+
  			padding.left+padding.right+
  			border.left+border.right+
  			innerBlock.getUsedWidth(); 
  }
  
  /**
   * Paints into given graphics context at given start location.
   * @param g Context
   * @param x X position
   * @param y Y position
   */
  @Override
	public void paint(Graphics2D g,int x,int y)
  {
    int innerHeight=innerBlock.getHeight();
    
    if(borderColour!=null)
    {
      g.setColor(borderColour);

      g.fillRect(
        x+margin.left,
        y+margin.top,
        width-(margin.left+margin.right),
        border.top);
      g.fillRect(
        x+margin.left,
        y+margin.top+border.top+padding.top+innerHeight+padding.bottom,
        width-(margin.left+margin.right),
        border.bottom);
      g.fillRect(
        x+margin.left,
        y+margin.top+border.top,
        border.left,
        innerHeight+padding.top+padding.bottom);
      g.fillRect(x+width-(margin.right+border.right),
        y+margin.top+border.top,
        border.right,
        innerHeight+padding.top+padding.bottom);
    }

    if(backgroundColour!=null)
    {
      g.setColor(backgroundColour);
      g.fillRect(x+margin.left+border.left,y+margin.top+border.top,
        width-(margin.left+margin.right+border.left+border.right),
        innerHeight+(padding.top+padding.bottom));
    }

    innerBlock.paint(
      g,
      x+margin.left+padding.left+border.left,
      y+margin.top+padding.top+border.top);
  }
  
  @Override
	public int getFirstBaseline()
  {
  		return innerBlock.getFirstBaseline()+margin.top+padding.top+border.top;
  }
  

  /**
   * Sets !current style flag.
   * @param sc Styles
   * @param current True if this block is 'current'
   * @return True if value changed, false if it was already current
   * @throws LayoutException 
   */
  @Override
	public boolean setCurrent(StyleContext sc,boolean current) throws LayoutException
  {
    if(this.current==current) return false;
    this.current=current;
    resolveStyle(sc);
    return true;
  }
  
	/** Clear any highlight */
	@Override
	public void clearHighlight()
	{
		if(hasHighlight)
		{
			innerBlock.clearHighlight();
			hasHighlight=false;
		}
	}
	
	/**
	 * Set highlight. Values are relative to block's own co-ordinates.
	 * @param lowX Start (upper/left) position X, or Layout.HIGHLIGHT_TOSTART
	 * @param lowY Start (upper/left) position Y, or Layout.HIGHLIGHT_TOSTART
	 * @param highX End (lower/right) position X, or Layout.HIGHLIGHT_TOEND
	 * @param highY End (lower/right) position Y, or Layout.HIGHLIGHT_TOEND
	 */
	@Override
	public void setHighlight(int lowX,int lowY,int highX,int highY)
	{
		int iXOffset=margin.left+padding.left+border.left;
		int iYOffset=margin.top+padding.top+border.top;
		
		innerBlock.setHighlight(
			lowX==Layout.HIGHLIGHT_TOSTART ? lowX : lowX-iXOffset,
			lowY==Layout.HIGHLIGHT_TOSTART ? lowY : 	lowY-iYOffset,
		  highX==Layout.HIGHLIGHT_TOEND ? highX : highX-iXOffset,
		  	highY==Layout.HIGHLIGHT_TOEND ? highY : highY-iYOffset);
		  
		hasHighlight=true;
	}
	
	@Override
	public LayoutInline.NodePos getNodePos(int targetX, int targetY,
		java.util.List<LayoutThing> l)
	{
		// Add this block
		l.add(this);
		
		int iXOffset=margin.left+padding.left+border.left;
		int iYOffset=margin.top+padding.top+border.top;
		
		return innerBlock.getNodePos(targetX-iXOffset,targetY-iYOffset,l);
	}
	
	/** @return The currently highlighted string from this block */
	@Override
	public String getHighlightText()
	{
		if(!hasHighlight) return "";
		return innerBlock.getHighlightText();
	}

  @Override
	public String debugDisplay(String indent)
  {
	  StringBuffer sb=new StringBuffer();
	  sb.append(indent+"SurroundedBlock ["+StringUtils.join("/",context)+"]\n");
	  sb.append(innerBlock.debugDisplay(indent+"  "));
	  return sb.toString();
  }
  
  @Override
	public void buildXML(Node parent,Map<LayoutInline,
  	LayoutInline.NodePos> translation)
  {  	
  		innerBlock.buildXML(buildXML(parent,translation,context), translation);  
  }
  
  static Element buildXML(Node parent,Map<?, ?> translation,String[] context)
  {
		int parentDepth=0;
		Node ancestor=parent;
		while(true)
		{
			ancestor=ancestor.getParentNode();
			if(ancestor==null) break;
			parentDepth++;
		}
		
		for(int extra=parentDepth;extra<context.length;extra++)
		{
			parent=addElement(parent,context[extra]);
		}
		
		return (Element)parent;		
  }
  
  static Element addElement(Node parent,String contextElement)
  {
  		String[] parts=contextElement.split("\u0001");
  		
		Element e=XML.createChild(parent,parts[0]);
		for(int i=1;i<parts.length;i++)
		{
			String[] attribute=parts[i].split("=",2);
			e.setAttribute(attribute[0],attribute[1]);
		}
  
		return e;
  }
  
  @Override
	public int getFlowCategory()
  {
  		return FLOWCATEGORY_NORMAL;
  }
  
  @Override
	public int getMinX()
  {
  		return 0;
  }
  
  @Override
	public int getMaxX()
  {
  		return width;
  }
}



