package textlayout;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import org.w3c.dom.*;

import textlayout.stylesheet.*;
import util.PlatformUtils;
import util.xml.*;

/** Scrolling text view, with scrollbar and layout. */
public class ScrollingLayout extends JComponent
{
  private JScrollBar sb;
  private LayoutViewer lv;
  private StyleContext sc;
  
  JScrollBar getScrollBar() { return sb; }
  
	final static int DRAGSCROLL_DELAY=100;  
	
	private void updateBackground(StyleContext sc)
	{
		try
		{
			lv.setBackground(sc.getRGB(Property.BACKGROUND_RGB,new String[] {"_scrolling"}));
		}
		catch(LayoutException e)
		{
			throw new Error(e);
		}
	}

	/**
	 * Handles copy, page down and page up.
	 * @param e Key event
	 */
	public void handleKey(KeyEvent e)
	{
		if(e.getKeyChar()=='c' && 
			e.getModifiers()==PlatformUtils.getActionKeyMask())
		{
			copy();
			clearHighlight();
		}
		else if(e.getKeyCode()==KeyEvent.VK_PAGE_DOWN)
			pageDown();
		else if(e.getKeyCode()==KeyEvent.VK_PAGE_UP)
			pageUp();
	}
  
	/**
	 * @param iPreferredWidth Preferred width
	 * @param iPreferredHeight Preferred height
	 * @param sc Styles
	 */
	public ScrollingLayout(int iPreferredWidth,int iPreferredHeight,StyleContext sc)
  {
		this.sc=sc;
    setLayout(new BorderLayout());
    setOpaque(true);

    sb=new JScrollBar(JScrollBar.VERTICAL,0,0,0,0);
    add(sb,BorderLayout.EAST);

    lv=new LayoutViewer(this);
    updateBackground(sc);
    add(lv,BorderLayout.CENTER);

		setFocusable(true);
		addFocusListener(new FocusAdapter() 
		{
			@Override
			public void focusGained(FocusEvent e)
			{
				if(linked!=null)
				{
					if(PlatformUtils.isMac() && linked instanceof JTextField)
					{
						// On Mac, it selects all in the box, but we don't want it to do
						// that, so let's preserve the existing selection
						final JTextField linkedText = (JTextField)linked;
						final int startBefore = linkedText.getSelectionStart(),
							endBefore = linkedText.getSelectionEnd();
						inMacHack = true;
						linkedText.requestFocus();
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								linkedText.setSelectionStart(startBefore);
								linkedText.setSelectionEnd(endBefore);
								inMacHack = false;
							}
						});
					}
					else
					{
						linked.requestFocus();
					}
				}
			}
		});
		
		addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
				handleKey(e);
			}
		});
		
		setPreferredSize(new Dimension(iPreferredWidth,iPreferredHeight));

    addComponentListener(new ComponentAdapter()
    {
      @Override
			public void componentResized(ComponentEvent ce)
      {
        resized();
      }
    });

    sb.addAdjustmentListener(new AdjustmentListener()
    {
      @Override
			public void adjustmentValueChanged(AdjustmentEvent ae)
      {
    		scrollbarChanged();
      }
    });
  }

	/**
	 * Update margins.
	 * @param leftMargin Left margin (pixels)
	 * @param rightMargin Right margin (pixels)
	 */
	public void setMargins(int leftMargin,int rightMargin)
	{
		lv.setMargins(leftMargin,rightMargin);
	}

	protected void scrollbarChanged()
	{
    lv.updateScrollbar(sb);
	}
	
	protected void fixScrollbar(int startY)
	{
		if(sb.getMaximum()-sb.getVisibleAmount() < startY)
			sb.setMaximum(startY+sb.getVisibleAmount());
		sb.setValue(startY);
	}
	
	/**
	 * Change the stylesheet or flag an update in the current one.
	 * @param sc New style context
	 * @throws LayoutException
	 */
	public void updateStyle(StyleContext sc) throws LayoutException
	{
		this.sc=sc;
		updateBackground(sc);
	  boolean bEnd=sb.getValue()==(sb.getMaximum()-sb.getVisibleAmount());
	  
		lv.l.updateStyle(sc);
		lv.updateScrollbar(sb);
		
		if(bEnd) scrollToEnd();
		
		repaint();
	}
		
	/**
	 * Scrolls to the end of the viewer.
	 */
	public void scrollToEnd()
  {		
    sb.setValue(Math.max(0,sb.getMaximum()-sb.getVisibleAmount()));
  }
	
	/**
	 * @return True if we're scrolled to the end.
	 */
	public boolean isAtEnd()
	{
		return getDistanceFromEnd()==0;
	}
	
	/**
	 * @return Distance in pixels from current scroll position to end
	 */
	public int getDistanceFromEnd()
	{
		return Math.max(0,sb.getMaximum()-sb.getVisibleAmount())-sb.getValue();
	}
  
  void scroll(int iDelta)
  {
  	int iCurrent=sb.getValue();
  	iCurrent+=iDelta;
  	if(iCurrent<sb.getMinimum()) iCurrent=sb.getMinimum();
  	if(iCurrent>sb.getMaximum()) iCurrent=sb.getMaximum();  	
  	sb.setValue(iCurrent);
  }

  /**
   * @return True if scrollbar is required for current size of document
   */
	public boolean isUsingScrollbar()
  {
    int iWidth=lv.getWidth();
    if(iWidth<=0) return false;
    return lv.l.getHeight(iWidth) > lv.getHeight();
  }

  /** @return True if paging down puts us at end, false otherwise */
	public boolean pageDown()
  {
    int iNewValue=sb.getValue()+sb.getBlockIncrement();
    int iRealMax=sb.getMaximum()-sb.getVisibleAmount();
    if(iNewValue>=iRealMax)
    {
      sb.setValue(iRealMax);
      return true;
    }
    else
    {
      sb.setValue(iNewValue);
      return false;
    }
  }

  /** @return True if paging up puts us at start, false otherwise */
	public boolean pageUp()
  {
    int iNewValue=sb.getValue()-sb.getBlockIncrement();
    if(iNewValue<0)
    {
      sb.setValue(0);
      return true;
    }
    else
    {
      sb.setValue(iNewValue);
      return false;
    }
  }

  @Override
	public void setEnabled(boolean bEnabled)
  {
    sb.setEnabled(bEnabled);
    super.setEnabled(bEnabled);
  }

  private void resized()
  {
    lv.updateScrollbar(sb);
  }

  void resolveStyle() 
  {
    lv.l.resolveStyle(sc);
  }

  /**
   * Adds a layout block.
   * @param lb Block to add
   * @throws LayoutException
   */
	public void addBlock(LayoutBlock lb) throws LayoutException
  {
    lv.l.addBlock(sc,lb);
    lv.updateScrollbar(sb);
    repaint();
  }

	/**
	 * Adds a number of blocks.
	 * @param alb Layout blocks to add
	 * @throws LayoutException
	 */
  public void addBlocks(LayoutBlock[] alb) throws LayoutException
  {
    for(int i=0;i<alb.length;i++)
      lv.l.addBlock(sc,alb[i]);
    lv.updateScrollbar(sb);
    repaint();
  }
  
  /**
   * @return Number of blocks in layout
   */
  public int getNumBlocks()
  {
		return lv.l.getNumBlocks();
  }
  
  /**
   * Deletes some blocks from start.
   * @param count Number of blocks to delete
   * @return Pixel difference in height after deleting
   */
  public int deleteFirstBlocks(int count)
  {
		int heightChange=lv.l.deleteFirstBlocks(count);
		lv.updateScrollbar(sb);
		repaint();
		return heightChange;
  }
  
  /**
   * Adds some blocks from XML document. 
   * @param e XML element containing data to add
   * @throws LayoutException
   */
  public void addBlocks(Element e) throws LayoutException
  {
		addBlocks(LayoutGenerator.getLayout(sc,	e));
  }
  
  private List<ChangeListener> lChangeListeners=new LinkedList<ChangeListener>();
  /**
   * Start listening to state changes (eg highlight).
   * @param cl New listener
   */
	public void addChangeListener(ChangeListener cl)
  {
		lChangeListeners.add(cl);
  }
  /**
   * Stop listening to state changes (eg highlight).
   * @param cl Old listener
   */
	public void removeChangeListener(ChangeListener cl)
  {
		lChangeListeners.remove(cl);
  }
  void fireChangeEvent()
  {
  	ChangeEvent ce=new ChangeEvent(this);
  	for(ChangeListener cl : lChangeListeners)
  	{
  		cl.stateChanged(ce);
  	}
  }

	/** Copy to clipboard */
	public void copy()
  {
		lv.copy();
  }
  
	/** Clear highlighted text */
	public void clearHighlight()
  {
		lv.clearHighlight();
  }
	
	/** Clear highlighted text */
	public void highlightAll()
  {
		lv.highlightAll();
  }
	
  /** @return True if there's something to copy */
	public boolean hasHighlight()
  {
		return lv.hasHighlight();
  }
  
	/**
	 * Override to return whether something'll happen if user clicks.
	 * @param n XML node their pointer is in
	 * @param iCharacter Character offset within text node
	 * @return True if it's an action
	 */
  protected boolean isAction(Node n,int iCharacter)
  {
	  return false;
  }
  /**
   * Override to do something when user clicks.
   * @param n XML node they clicked on
   * @param iCharacter Character offset within text node
   * @param me Mouse event
   */
  protected void doAction(Node n,int iCharacter,MouseEvent me)
  {
  }
  
  /**
   * Override to change popup menu.
   * @param n XML node they clicked on or null if none
   * @param character Character offset within text node
   * @return A new JPopupMenu with any added items
   */
  protected JPopupMenu buildMenu(Node n,int character)
  {
		return new JPopupMenu();
  }
  
  /**
   * Removes all blocks.
   */
  public void clear()
  {
		lv.l.clear();
		scrollbarChanged();
		repaint();
  }
  
  /**
   * Called to paint behind the text contents. Called after the background
   * is cleared. Default does nothing
   * @param g Graphics context
   * @param width Width of entire area (including margins)
   * @param height Height of area
   * @param startY Starting Y position (how much it's scrolled)
   */
  protected void paintBehind(Graphics g,int width,int height,int startY)
  {
  }

  /** @return Height of layout */
  public int getLayoutHeight()
  {
	  if(!lv.l.isReflowed())
  	  lv.reflow();
	  if(!lv.l.isReflowed()) 
  		return 0;
		return lv.l.getHeight();
  }
   
  /** Linked component */
  private JComponent linked=null;
  
  /** Caret listener for linked JTextField */
  private CaretListener linkedListener = null;
  
  /** True if currently in mac hack */
  private boolean inMacHack = false;
  
  /** 
   * Sets an editfield (or whatever) that receives focus in favour of this 
   * @param linked Linked component, or null for none 
   */
	public void informLinked(JComponent linked)
	{
		if(this.linkedListener != null)
		{
			((JTextField)this.linked).removeCaretListener(linkedListener);
		}
		this.linked=linked;
		if(linked instanceof JTextField)
		{
			linkedListener = new CaretListener()
			{
				@Override
				public void caretUpdate(CaretEvent e)
				{
					if(inMacHack)
					{
						return;
					}
					if(e.getDot() != e.getMark())
					{
						lv.clearHighlight();
					}
				}
			};
			((JTextField)linked).addCaretListener(linkedListener);
		}
	}		
	
	/**
	 * @return Linked component or null if none
	 */
	public JComponent getLinked()
	{
		return linked;
	}
}

class LayoutViewer extends JComponent implements ClipboardOwner
{
	Layout l;

  private int iStartY=0;
  private ScrollingLayout slParent;

	private int 
		iHighlightStartX,iHighlightStartY;
	
	private int leftMargin=0,rightMargin=0;
		
	private String sHighlightText="";
	
	/** Thread that makes drag-to-scroll work */
	private DragScrollThread dst=null;
	
	private Document lastXML;
	private Map<LayoutInline, LayoutInline.NodePos> lastXMLTranslation;
	private LayoutBlock lastXMLBlock;
	
	private static boolean DEBUGCLICKS = false;
	
	/**
	 * Return a resolved (with XML data) node position in document.
	 * @param me Mouse position or null if not on a word
	 * @return Node position
	 * @throws BugException
	 */
	private LayoutInline.NodePos getResolvedNodePos(MouseEvent me) throws BugException
	{
		// Get position in layout objects
		LinkedList<LayoutThing> ll = new LinkedList<LayoutThing>();
		LayoutInline.NodePos np=l.getNodePos(me.getX()-leftMargin,me.getY()+iStartY,ll);
		if(np==null) return null;
		
		// Build XML document from this block (if it isn't already cached)
		LayoutBlock root=(LayoutBlock)ll.getFirst();
		if(lastXMLBlock!=root)
		{
			try
			{
				lastXML=XML.newDocument();
			}
			catch(XMLException e)
			{
				throw new BugException(e);
			}
			lastXMLTranslation=new HashMap<LayoutInline, LayoutInline.NodePos>();
			lastXMLBlock=root;
			root.buildXML(lastXML,lastXMLTranslation);
		}
		np.resolve(lastXMLTranslation);
		
		// Debug display a [] around where they clicked		
		if(DEBUGCLICKS && me.getClickCount()!=0)
		{			
			Node n=np.getNode();
			String oldText=n.getNodeValue();
			int pos=np.getPos();
			n.getParentNode().insertBefore(lastXML.createTextNode(
				oldText.substring(0,pos)+"["+oldText.substring(pos,pos+1)+"]"+
				oldText.substring(pos+1)),n);
			XML.remove(n);
			TextLayoutTest.debugOutput(lastXML.getDocumentElement());					
			XML.remove(lastXML.getDocumentElement());
			root.buildXML(lastXML,lastXMLTranslation);
		}
		
		return np;
	}
	
  LayoutViewer(ScrollingLayout sl)
  {
 		slParent=sl;
    l=new Layout();
    setOpaque(true);
    
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent me)
			{
				if(me.getButton()==MouseEvent.BUTTON1)
					startHighlight(me.getX(),me.getY());
					
				if(me.isPopupTrigger()) triggerPopup(me);				
			}
			@Override
			public void mouseClicked(MouseEvent me)
			{
				if(me.getButton()==MouseEvent.BUTTON1)
				{
					try
					{
						LayoutInline.NodePos np=getResolvedNodePos(me);
						if(np!=null) slParent.doAction(np.getNode(),np.getPos(),me);
					}
					catch(BugException e)
					{
						e.printStackTrace();
					}
					l.clearHighlight();
					aCopy.setEnabled(false);					
					repaint();
				}
			}
			@Override
			public void mouseReleased(MouseEvent me)
			{
				slParent.requestFocus();
				if(me.getButton()==MouseEvent.BUTTON1)
				{
					stopDragScroll();
					
					String sNewHighlightText=l.getHighlightText();
					if(!sNewHighlightText.equals(sHighlightText))
					{
						sHighlightText=sNewHighlightText;
						slParent.fireChangeEvent();
					}
							
					aCopy.setEnabled(!l.getHighlightText().equals(""));
				}
				if(me.isPopupTrigger()) triggerPopup(me);				
			}
		});
		addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent me)
			{
				if((me.getModifiers() & MouseEvent.BUTTON1_DOWN_MASK)!=0) return;
				moveHighlight(me.getX(),me.getY());
				if(me.getY() < 0)
				  startDragScroll(me.getY());
				else if(me.getY() > getHeight())
				  startDragScroll(me.getY()-getHeight());
				else
				  stopDragScroll();
			}

			@Override
			public void mouseMoved(MouseEvent me)
			{
				try
				{
					LayoutInline.NodePos np=getResolvedNodePos(me);
					if(np!=null && slParent.isAction(np.getNode(),np.getPos()))
						setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					else
					  setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
				}
				catch(BugException e)
				{
					e.printStackTrace();
				}
			}

		});    
		addMouseWheelListener(new MouseWheelListener() 
		{
			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				int amount;
				int direction=e.getWheelRotation()<0 ? -1 : 1;
				JScrollBar sb=slParent.getScrollBar();
				if(e.getScrollType()==MouseWheelEvent.WHEEL_BLOCK_SCROLL)
				{
					amount=sb.getBlockIncrement(direction);
				}
				else
				{
					amount=sb.getUnitIncrement(direction)*direction*e.getScrollAmount();
				}
				int newValue=sb.getValue()+amount;
				newValue=Math.max(sb.getMinimum(),Math.min(newValue,sb.getMaximum()));
				sb.setValue(newValue);
			}
		});
  }
  
	void setMargins(int leftMargin,int rightMargin)
	{
		this.leftMargin=leftMargin;
		this.rightMargin=rightMargin;
	}
  
	private void triggerPopup(MouseEvent me)
	{
		Node n=null;
		int nodePos=0;
		try
		{
			LayoutInline.NodePos np=getResolvedNodePos(me);
			if(np!=null) 
			{
				n=np.getNode();
				nodePos=np.getPos();
			}
		}
		catch(BugException be)
		{
			be.printStackTrace();
		}
		JPopupMenu pm=slParent.buildMenu(n,nodePos);
		if(pm.getSubElements().length>0)
			pm.addSeparator();
		pm.add(aCopy);
		pm.show(this,me.getX(),me.getY());
	}
	
	/** Copy action */
	private Action aCopy=new CopyAction();	
	class CopyAction extends AbstractAction
	{
		CopyAction()
		{
			putValue(NAME,"Copy");
			putValue(MNEMONIC_KEY,new Integer(KeyEvent.VK_C));
			putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_C,PlatformUtils.getActionKeyMask()));
			setEnabled(false);
		}
		@Override
		public void actionPerformed(ActionEvent ae)
		{
			copy();
			clearHighlight();
		}		
	}
	
	/** @return True if there's something to copy */
	boolean hasHighlight()
	{
		return !sHighlightText.equals("");
	}
	
	/** Copy to clipboard */
	public void copy()
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
			new StringSelection(l.getHighlightText()),LayoutViewer.this);
	}
	
	/** Clear highlighted text */
	public void clearHighlight()
	{
		l.clearHighlight();
		repaint();
	}
	
	public void highlightAll()
	{
		l.highlightAll();
	}
	
	/**
	 * Begin highlighting.
	 * @param iX Start X
	 * @param iY Start Y
	 */
	private void startHighlight(int iX,int iY)
	{
		// When you select something, if you have anything selected in a linked
		// JTextField, deselect it
		JComponent linked = slParent.getLinked();
		if(linked != null && linked instanceof JTextField)
		{
			JTextField linkedText = (JTextField)linked;
			int caret = linkedText.getCaretPosition();
			linkedText.setSelectionStart(caret);
			linkedText.setSelectionEnd(caret);
		}

		iHighlightStartX=iX;
		iHighlightStartY=iY+iStartY;
	}

	/**
	 * Update highlight as user drags mouse.
	 * @param iX New X
	 * @param iY New Y
	 */
	private void moveHighlight(int iX,int iY)
	{
		l.setHighlight(iHighlightStartX-leftMargin,iHighlightStartY,iX-leftMargin,iY+iStartY);
		repaint();
	}
	
	class DragScrollThread extends Thread
	{
		int iDelta;
		boolean bFinished=false;
		
		DragScrollThread(int iDelta)
		{
			super("Drag-scroll thread");
			this.iDelta=iDelta;
			start();
		}		
			
		@Override
		public void run()
		{
			while(!bFinished)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						if(bFinished) return;
						slParent.scroll(iDelta);
					}
				});
				
				try
				{
					Thread.sleep(ScrollingLayout.DRAGSCROLL_DELAY);
				}
				catch (InterruptedException ie)
				{
				}
			}
		}
		
		void end()
		{
			bFinished=true;
		}
		
	}
	
	private void startDragScroll(int iDelta)
	{
		if(dst==null)
		{
			dst=new DragScrollThread(iDelta);
		}
		else
		  dst.iDelta=iDelta;
	}
	
	private void stopDragScroll()
	{
		// No need to synchronize this as all relevant changes occur in Swing thread
		if(dst!=null)
		{
			dst.end();
			dst=null;
		}
	}

  void updateScrollbar(JScrollBar sb)
  {
    int
      iLayoutHeight=l.getHeight(getWidth()-leftMargin-rightMargin),
      iHeight=getHeight();
    if(iHeight==0) return;

    if(sb.getValue() > iLayoutHeight-iHeight)
      sb.setValue( (iLayoutHeight-iHeight)<0 ? 0 : (iLayoutHeight-iHeight));

    sb.setMaximum(iLayoutHeight);
    sb.setVisibleAmount(iHeight);
    sb.setBlockIncrement(iHeight-20);
    sb.setUnitIncrement(20);

    if(sb.getValue()>=0)
    {
      if(iStartY!=sb.getValue())
      {
        iStartY=sb.getValue();
        repaint();
      }
    }
  }
  
  void reflow()
  {
	  l.reflow(getWidth()-leftMargin-rightMargin);
  }
  
  private int beforeHeight=-1,beforeInnerWidth=-1;
  private boolean beforeAtEnd=false;
  
  @Override
	public void paintComponent(Graphics g)
	{
 		g.setColor(getBackground());
 		g.fillRect(0,0,getWidth(),getHeight());
 		
 		// Check if width or height has changed
	  int innerWidth=getWidth()-leftMargin-rightMargin;
	  if((innerWidth!=beforeInnerWidth || getHeight()!=beforeHeight))
	  {
	  	// Currently at the end?
	  	if(beforeAtEnd)
	  	{
	  		// OK, make sure we're still at the end after resize
	  		l.reflow(innerWidth);
	  		iStartY=l.getHeight()-getHeight();
	  		slParent.fixScrollbar(iStartY);
	  	}	  		  
	  }
 		
 		slParent.paintBehind(g,getWidth(),getHeight(),iStartY); 		
		l.paint((Graphics2D)g,leftMargin,0,innerWidth,iStartY,getHeight());
	  beforeAtEnd=iStartY==l.getHeight()-getHeight();
	  beforeHeight=getHeight();
	  beforeInnerWidth=innerWidth;
	}

  /**
   * Ignored.
   * @param arg0 Clipbaoard
   * @param arg1 Transferable
   */
	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1)
	{
	}
}