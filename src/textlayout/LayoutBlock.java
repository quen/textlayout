package textlayout;

import java.awt.*;
import java.util.Map;

import org.w3c.dom.Node;

import textlayout.stylesheet.StyleContext;

/**
 * A block that can go within a Layout component.
 */
interface LayoutBlock extends LayoutThing
{
  /**
	 * Recalculates block for given width.
	 * @param width Width
	 */
  public void reflow(int width);
  
  /**
	 * Updates style using a new OutputConverter.
   * @param sc Context
   * @throws LayoutException 
	 */
  public void updateStyle(StyleContext sc) throws LayoutException;
  
  /**
	 * @return Used width (basically = width unless it's a single line when maybe it's less)
	 */
  public int getUsedWidth();

  /**
	 * @return Height at last-specified width
	 */
  public int getHeight();
  
  /**
	 * @return Baseline of first line within this block
	 */
  public int getFirstBaseline();  
  
  /**
	 * @return Minimum used X position (at given width), inclusive e.g. 0 for 100px widge
	 */
  public int getMinX();
  
  /**
	 * @return Maximum used X position (at given width), exclusive e.g. 100 for 100px wide
	 */
  public int getMaxX();

  /**
   * @return A FLOWCATEGORY_xx constant
   */
  public int getFlowCategory();
  
  /**
	 * Normal blocks that push down others
	 */
  public final static int FLOWCATEGORY_NORMAL=0;
  
  /**
	 * Extra category for blocks that go in left margin and are positioned separately
	 */
  public final static int FLOWCATEGORY_LEFTMARGIN=1;

  /**
	 * Paint into given graphics context at given start location
   * @param g Context
   * @param x X
   * @param y Y
	 */
  public void paint(Graphics2D g,int x,int y);

  /**
	 * Set !current style flag
   * @param sc Context
   * @param current True to set flag on, false for off
   * @return True if it changed
   * @throws LayoutException 
	 */
  public boolean setCurrent(StyleContext sc,boolean current) throws LayoutException;
  
  /**
	 * Clear any highlight
	 */
  public void clearHighlight();
  
  /**
   * Set highlight. Values are relative to block's own co-ordinates.
   * @param lowX Start (upper/left) position X, or Layout.HIGHLIGHT_TOSTART
   * @param lowY Start (upper/left) position Y, or Layout.HIGHLIGHT_TOSTART
   * @param highX End (lower/right) position X, or Layout.HIGHLIGHT_TOEND
   * @param highY End (lower/right) position Y, or Layout.HIGHLIGHT_TOEND
   */
  public void setHighlight(int lowX,int lowY,int highX,int highY);
  
  /**
   * Obtain the inline block and position at the given pixel co-ordinates.
   * @param targetX X (relative to block's own co-ordinates)
   * @param targetY Y (relative to block's own co-ordinates)
   * @param l List of blocks from top to bottom
   * @return Node position within the deepest LayoutInline, or null if it isn't
   *   within a LayoutInline
   */
  public LayoutInline.NodePos getNodePos(int targetX, int targetY,
  	java.util.List<LayoutThing> l);
  
	/** @return The currently highlighted string from this block */
	public String getHighlightText();
	
	/**
	 * @param indent Indent text to add to start of each line
	 * @return Debug of hierarchy from here
	 */
	public String debugDisplay(String indent);

  /**
   * Builds XML version of this block.
   * @param parent Parent node (may be Document)
   * @param translation Map from LayoutInline to 
   *   LayoutInline.NodePos
   */
	public void buildXML(Node parent,
		Map<LayoutInline, LayoutInline.NodePos> translation);
}