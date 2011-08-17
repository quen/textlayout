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

import textlayout.stylesheet.*;

/** 
 * The root node for a layout. Basically just a vertical holder block with
 * a background colour and synchronization. 
 */
class Layout extends VerticalHolderBlock
{
	Layout()
	{
		super(null,null);
	}

  /** Background colour, if any */
  private Color cBackground=null;

	/** Constant used within setHighlight to indicate that layout blocks should
	 * show highlight all the way to the end. */
	final static int HIGHLIGHT_TOEND=100000000;
	/** Constant used within setHighlight to indicate that layout blocks should
	 * show highlight all the way to the beginning. */
	final static int HIGHLIGHT_TOSTART=-100000000;

  void resolveStyle(StyleContext sc)
  {  		
    try
		{
			cBackground=sc.getRGB(Property.BACKGROUND_RGB,new String[] { "_root" });
		}
		catch(LayoutException e)
		{
			throw new Error(e);
		}
  }
  
  @Override
	public synchronized void updateStyle(StyleContext sc) throws LayoutException
  {
  	super.updateStyle(sc);
  }
  
  @Override
	public synchronized void reflow(int width)
  {
  	super.reflow(width);
  }
  
  void setWidth(int width)
  {
		reflow(width);
  }
  
  /**
   * Returns current height (reflows if needed).
   * @param width Specified width
   * @return Height
   */
  int getHeight(int width)
  {
  	if(width<=0) 
  	{
  		return 0;
  	}
    reflow(width);
    return getHeight();
  }
  
  /** Clear all blocks */
  @Override
	synchronized void clear()
  {
		super.clear();
  }

  @Override
	synchronized void addBlock(StyleContext sc,LayoutBlock lb) throws LayoutException
  {
		super.addBlock(sc,lb);
  }

  synchronized void paint(Graphics2D g2,int iScrX,int iScrY,int iWidth,int iStart,int iTargetHeight)
  {
  	reflow(iWidth);
  		
    // Clear background
    if(cBackground!=null)
    {
      g2.setColor(cBackground);
      g2.fillRect(iScrX,iScrY,iWidth,iTargetHeight);
    }

    super.paint(g2,iScrX,iScrY,iStart,iTargetHeight);
  }

	/**
	 * Sets the highlight display, clearing any existing highlight as appropriate.
	 * Efficient enough to call every time mouse moves. All co-ordinates are 
	 * relative to the layout. For this version of the method, end can be before start.
	 * After calling this method it will be necessary to repaint.
	 * @param iStartX X co-ordinate of starting position
	 * @param iStartY Y co-ordinate of starting position
	 * @param iEndX X co-ordinate of end position
	 * @param iEndY Y co-ordinate of end position
	 */  
  @Override
	public synchronized void setHighlight(int iStartX,int iStartY,int iEndX,int iEndY)
  {
  	// Order co-ordinates
  	int iLowX,iLowY,iHighX,iHighY;
  	if(iStartY > iEndY || (iStartY==iEndY && iStartX > iEndX))
  	{
  		iLowX=iEndX; iLowY=iEndY; iHighX=iStartX; iHighY=iStartY;
  	}
  	else
  	{
			iLowX=iStartX; iLowY=iStartY; iHighX=iEndX; iHighY=iEndY;	  
  	}
  	super.setHighlight(iLowX,iLowY,iHighX,iHighY);
  }
  
  public synchronized void highlightAll()
  {
		super.setHighlight(Layout.HIGHLIGHT_TOSTART,Layout.HIGHLIGHT_TOSTART,
			Layout.HIGHLIGHT_TOEND,Layout.HIGHLIGHT_TOEND);
  }
}