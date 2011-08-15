package textlayout;

import java.awt.Graphics2D;
import java.util.*;

import org.w3c.dom.*;

import textlayout.stylesheet.StyleContext;
import util.StringUtils;

/**
 * Manages a list of blocks that appear one below the other.
 */
class VerticalHolderBlock implements LayoutBlock
{
  /** Inner blocks */
  private LinkedList<LayoutBlock> blocks=new LinkedList<LayoutBlock>();
  
  /** Style context */
  private String[] context;

  /** Current set width and calculated height */
  private int width=-1,height=-1;

  /**
   * Creates block with the given block style.
   * @param blocks Blocks that go inside this one, or null if none
   * @param context Element context
   */
  VerticalHolderBlock(Collection<LayoutBlock> blocks,String[] context)
  {
  		this.context=context;
  		if(blocks!=null) this.blocks.addAll(blocks);
  }

	/**
	 * Updates style using a new context.
	 * @param sc Styles
	 * @throws LayoutException 
	 */
	@Override
	public void updateStyle(StyleContext sc) throws LayoutException
	{
		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
		{
			LayoutBlock lb=(LayoutBlock)i.next();
			lb.updateStyle(sc);
		}
		width=-1;
	}
	
  /** Clear all blocks */
  void clear()
  {
    blocks.clear();
    int oldWidth=width;
    width=-1;
    reflow(oldWidth);
  }
  
  /** @return Number of blocks */
  int getNumBlocks()
  {
  		return blocks.size();
  }

  /** 
   * Deletes blocks from the start.
   * @param count Number of blocks to delete
   * @return Pixel difference in height
   */
  int deleteFirstBlocks(int count)
  {
  		for(int i=0;i<count && !blocks.isEmpty();i++)
  			blocks.removeFirst();
    int oldWidth=width;
    int oldHeight=height;
    width=-1;
    reflow(oldWidth);
    return oldHeight-height;
  }
  
  int getWidth()
  {
  		return width;
  }

  /**
   * Adds another block to the bottom.
   * @param sc Styles
   * @param lb Block to add
   * @throws LayoutException
   */
  void addBlock(StyleContext sc,LayoutBlock lb) throws LayoutException
  {
    if(!blocks.isEmpty())
    {
      LayoutBlock lbLast=(LayoutBlock)blocks.getLast();
      if(lbLast.setCurrent(sc,false) && width!=-1)
      {
        int iBefore=lbLast.getHeight();
        lbLast.reflow(width);
        height+=(lbLast.getHeight()-iBefore);
      }
    }
    lb.setCurrent(sc,true);
    blocks.add(lb);

    if(width!=-1)
    {
      lb.reflow(width);
      height+=lb.getHeight();
    }
  }
		
  /**
   * Recalculates block for given width.
   * @param width Width in pixels
   */
  @Override
	public void reflow(int width)
  {
    if(this.width==width || width<=0) return;
    this.width = width;

    height = 0; 
    int leftHeight = 0;
		for(Iterator<LayoutBlock> i=blocks.iterator(); i.hasNext();)
		{
			LayoutBlock lb = (LayoutBlock)i.next();
      lb.reflow(width);
      switch(lb.getFlowCategory())
      {
      case FLOWCATEGORY_NORMAL:
    		height += lb.getHeight();
    		break;
      case FLOWCATEGORY_LEFTMARGIN:
    		leftHeight += lb.getHeight();
    		break;
    	default:
    		assert(false);
      }
    }
    
    height = Math.max(height, leftHeight);
  }
  
  @Override
	public int getFirstBaseline()
  {
		if(blocks.size()==0) return 0;
		return ((LayoutBlock)blocks.getFirst()).getFirstBaseline();
  }

  /**
   * Gets height at last-specified width.
   * @return int Height in pixels
   */
  @Override
	public int getHeight()
  {
    if(height==-1) 
    {
    	throw new Error("Must reflow before calling getHeight");
    }
    return height;
  }
  
  /** @return True if the content has already been flowed, false otherwise */
  public boolean isReflowed()
  {
  	  return height!=-1;
  }
  
  @Override
	public int getUsedWidth()
  {
    if(height==-1) throw new Error("Must reflow before calling getUsedWidth");
    
    int usedWidth=0;
		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
		{
			LayoutBlock lb=(LayoutBlock)i.next();
      usedWidth=Math.max(usedWidth,lb.getUsedWidth());
    }
    return usedWidth;
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
  		paint(g,x,y,0,1000000);
  }
  
  public void paint(Graphics2D g,int x,int y,int start,int targetHeight)
  {
    if(height==-1) throw new Error("Must reflow before calling paint");

    int leftMarginY=y,normalY=y;
		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
		{
			LayoutBlock lb=(LayoutBlock)i.next();
			int thisY,newY;
      switch(lb.getFlowCategory())
      {
      case FLOWCATEGORY_NORMAL:
      		thisY=normalY;
      		newY=thisY+lb.getHeight();
      		normalY=newY;
      		break;
      case FLOWCATEGORY_LEFTMARGIN:
      		thisY=leftMarginY;
      		newY=thisY+lb.getHeight();
      		leftMarginY=newY;
      		break;
      	default:
      		throw new Error("Unexpected flow category");
      }

      if(newY>=start) lb.paint(g,x,thisY-start);
      if(newY>start+targetHeight) break;
    }
  }

  @Override
	public boolean setCurrent(StyleContext sc,boolean current) throws LayoutException
  {
  		boolean change=false;
  		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
  		{
  			LayoutBlock lb=(LayoutBlock)i.next();
      change|=lb.setCurrent(sc,current);
  		}
    return change;
  }
  
	/** Clears any highlight */
	@Override
	public void clearHighlight()
	{
		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
		{
			LayoutBlock lb=(LayoutBlock)i.next();
			lb.clearHighlight();
		}
	}
  
	/**
	 * Sets highlight. Values are relative to block's own co-ordinates.
	 * @param lowX Start (upper/left) position X, or Layout.HIGHLIGHT_TOSTART
	 * @param lowY Start (upper/left) position Y, or Layout.HIGHLIGHT_TOSTART
	 * @param highX End (lower/right) position X, or Layout.HIGHLIGHT_TOEND
	 * @param highY End (lower/right) position Y, or Layout.HIGHLIGHT_TOEND
	 */
	@Override
	public void setHighlight(int lowX,int lowY,int highX,int highY)
	{
		boolean inHighlight=false,doneHighlight=false;
		int normalY=0,leftMarginY=0;
		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
		{
			LayoutBlock lb=(LayoutBlock)i.next();
			int y,newY;
      switch(lb.getFlowCategory())
      {
      case FLOWCATEGORY_NORMAL:
      		y=normalY;
        newY=y+lb.getHeight();
        normalY=newY;
      		break;
      case FLOWCATEGORY_LEFTMARGIN:
      		y=leftMarginY;
        newY=y+lb.getHeight();
        leftMarginY=newY;
      		break;
      	default:
      		throw new Error("Unknown flow category");
      }
			
      boolean 
      		startInBlock=
      			(lowY>=y && lowY<newY) && 	(lowX>=lb.getMinX() && lowX<lb.getMaxX()),
  				endInBlock=
      			(highY>=y && highY<newY) && 	(highX>=lb.getMinX() && highX<lb.getMaxX());
      
      if(!doneHighlight && !inHighlight && (startInBlock || lowY==Layout.HIGHLIGHT_TOSTART))
      {
  				inHighlight=true;
  				doneHighlight=true; // Prevents it starting again!
  				if(endInBlock) // Contains entire range
  				{
					lb.setHighlight(lowX,lowY-y,highX,highY-y);
  				}
  				else
  				{
					lb.setHighlight(lowX,lowY-y,Layout.HIGHLIGHT_TOEND,Layout.HIGHLIGHT_TOEND);
  				}  				
      }
      else if(inHighlight)
      {
    			if(highY!=Layout.HIGHLIGHT_TOEND && endInBlock) // Last block in highlight
      		{
  					lb.setHighlight(Layout.HIGHLIGHT_TOSTART,Layout.HIGHLIGHT_TOSTART,highX,highY-y);
  					inHighlight=false;
      		}
      		else if(highY!=Layout.HIGHLIGHT_TOEND && y>highY) // Gone past block
      		{
      			lb.clearHighlight();
      			inHighlight=false;
      		}
      		else
      		{
  					lb.setHighlight(Layout.HIGHLIGHT_TOSTART,Layout.HIGHLIGHT_TOSTART,Layout.HIGHLIGHT_TOEND,Layout.HIGHLIGHT_TOEND);
      		}
      }
      else
      {
      		lb.clearHighlight();
      }
		}
		
	}
	
	@Override
	public LayoutInline.NodePos getNodePos(int targetX, int targetY,
		List<LayoutThing> l)
	{
		int normalY=0,leftMarginY=0; 
		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
		{
			LayoutBlock lb=(LayoutBlock)i.next();
			// This code is sort of copied from the equivalent in Layout (I'm not 
			// entirely sure why Layout isn't implemented as a VerticalHolderBlock
			// in the first place, but hey)

			// Get block and calculate its vertical extent
			int y,newY;
      switch(lb.getFlowCategory())
      {
      case FLOWCATEGORY_NORMAL:
      		y=normalY;
        newY=y+lb.getHeight();
        normalY=newY;
      		break;
      case FLOWCATEGORY_LEFTMARGIN:
      		y=leftMarginY;
        newY=y+lb.getHeight();
        leftMarginY=newY;
      		break;
      	default:
      		throw new Error("Unknown flow category");
      }

			if(targetY>=y && targetY<newY && targetX>=lb.getMinX() && targetX<lb.getMaxX())
			{
				return lb.getNodePos(targetX,targetY-y,l);
			}
		}
		return null;
	}
  
	/** @return The currently highlighted string from this block */
	@Override
	public String getHighlightText()
	{
		StringBuffer sb=new StringBuffer();
		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
		{
			LayoutBlock lb=(LayoutBlock)i.next();
			sb.append(lb.getHighlightText());

			if(lb.getFlowCategory()==LayoutBlock.FLOWCATEGORY_LEFTMARGIN)
				sb.append(' ');
			else
				sb.append('\n');
		}
		return removeDoubleLFs(sb.toString());
	}
	
	private static String removeDoubleLFs(String input)
	{
		return input.replaceAll("\n+","\n");
	}	  

  @Override
	public String debugDisplay(String indent)
  {
	  StringBuffer sb=new StringBuffer();
	  sb.append(indent+"VerticalHolderBlock ["+
	  	(context==null ? "" : StringUtils.join("/",context))+"]\n");
		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
		{
			LayoutBlock lb=(LayoutBlock)i.next();
			sb.append(lb.debugDisplay(indent+"  "));
		}
		return sb.toString();
  }

	@Override
	public void buildXML(Node parent,
		Map<LayoutInline, LayoutInline.NodePos> translation)
	{
		Element thisElement=SurroundedBlock.buildXML(parent,translation,context);  
		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
		{
			LayoutBlock lb=(LayoutBlock)i.next();
			lb.buildXML(thisElement,translation);
		}
	}
	
	@Override
	public int getMinX()
	{
		int minX=0;
		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
		{
			LayoutBlock lb=(LayoutBlock)i.next();
			minX=Math.min(minX,lb.getMinX());
		}
		return minX;
	}
	
	@Override
	public int getMaxX()
	{
		int maxX=width;
		for(Iterator<LayoutBlock> i=blocks.iterator();i.hasNext();)
		{
			LayoutBlock lb=(LayoutBlock)i.next();
			maxX=Math.max(maxX,lb.getMaxX());
		}
		return maxX;
	}

	@Override
	public int getFlowCategory()
	{
		return FLOWCATEGORY_NORMAL;
	}
}
