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

import java.awt.*;
import java.util.Map;

import org.w3c.dom.Node;

import textlayout.stylesheet.StyleContext;

/**
 * An unwrappable inline element.
 */
interface LayoutInline extends LayoutThing
{
  /** 
   * @return Width of item
   */
  public int getWidth();
  
  public final static int UNWRAPPED=-1;
  
  /**
   * Call to inform item that it should wrap itself to the given width if 
   * appropriate.
   * @param lineWidth Width of line or UNWRAPPED
   */
  public void setWrappedIfNecessary(int lineWidth);  
 
  /**
   * If the item needs to wrap, it can return an extra 'overflow' inline item.
   * @return Extra item (null if none)
   */
  public LayoutInline getWrapped();

	/**
	 * @return Width when item is last on line (i.e. not including a terminating
	 *   space)
	 */
	public int getWidthLineFinal();

  /**
	 * @return Ascent above baseline
	 */
  public int getAscent();

  /**
	 * @return Descent below baseline
	 */
  public int getDescent();

	/**
	 * Updates style using a new StyleContext.
	 * @param sc New context
	 * @throws LayoutException
	 */
	public void updateStyle(StyleContext sc) throws LayoutException;

  /**
	 * Paints into given context.
   * @param g2 Graphics context
   * @param x X location
   * @param baselineY Y baseline
	 */
  public void paint(Graphics2D g2,int x,int baselineY);

  /** 
   * @return False if the element must be on the same line as the
   *   one after it (unless that has breakBefore) 
   */
  public boolean breakAfter();

  /** 
   * @return False if the element must be on the same line as the
   *   one before it (unless that has breakAfter) 
   */
  public boolean breakBefore();

  /**
	 * @return True if this shouldn't be displayed at line starts
	 */
  public boolean skipAtLineStart();
  
  /**
	 * Removes highlighting from this item.
	 */
  public void clearHighlight();
  
  /**
   * Sets highlight on this item.
   * @param startX Begining (relative to this item) or Layout.HIGHLIGHT_TOSTART
   * @param endX End (relative to this item) or Layout.HIGHLIGHT_TOEND
   */
  public void setHighlight(int startX,int endX);
  
  /**
   * @return All text of this inline, or empty string if none 
   */
  public String getText();
  
  /**
   * @param x Pixel x
   * @return XML node and character position within it, or null if not inside 
   */
  public NodePos getNodePos(int x);
  
  /**
   * @return The currently highlighted string from this inline 
   */
  public String getHighlightText();
  
  /**
   * Node and position within it.
   */
  public static class NodePos
  {
	  NodePos(LayoutInline li,int layoutPos) { this.li=li; this.layoutPos=layoutPos; }
	  NodePos(Node n,int nodePos) { this.n=n; this.nodePos=nodePos; }
	  private Node n;
	  private LayoutInline li;
	  private int layoutPos,nodePos;
	  public void resolve(Map<LayoutInline, NodePos> translations) throws BugException
	  {
  		NodePos start = translations.get(li);
  		if(start==null)
  			throw new BugException("Requested LayoutInline is not included in XML data");
  		n=start.n;
  		nodePos=start.nodePos+layoutPos;
	  }
	  public Node getNode() throws BugException
	  { 
  		if(n==null) throw new BugException("Must resolve nodepos to get XML data"); 
  		return n; 
  	}
	  public int getPos() 
	  { 
  		return nodePos; 	
  	}
  }
  
	/**
	 * @param indent Indent text to add to start of each line
	 * @return Debug of hierarchy from here
	 */
	public String debugDisplay(String indent);  
	
	/**
	 * @return Context as tag stack
	 */
	public String[] getContext();
}