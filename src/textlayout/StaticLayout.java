package textlayout;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.w3c.dom.*;

import textlayout.stylesheet.StyleContext;
import util.xml.*;

/**
 * Static (non-scrolling) text layout, similar to {@link JLabel}.
 */
public class StaticLayout extends JComponent implements ClipboardOwner
{
  private Layout l;
  
  private int preferredWidth,iCurrentPreferredWidth;
  
	private LayoutBlock lastXMLBlock=null;
	private org.w3c.dom.Document lastXML=null;
	private HashMap<LayoutInline, LayoutInline.NodePos> lastXMLTranslation=null;

  private String thisText=null; 

  @Override
	public void setBounds(int x,int y,int width,int height)
  {
  		super.setBounds(x,y,width,height);
  		resized();
  }

  /**
   * @param text Text contenxt (XML)
   * @param preferredWidth Preferred width
   */
	public StaticLayout(String text,int preferredWidth)
  {
    setOpaque(false);

    l=new Layout();
    
    Insets i=getInsets();
    
    this.preferredWidth=preferredWidth;    
    if(preferredWidth!=-1)
    {
	    l.setWidth(preferredWidth-i.left-i.right);
    }
    else
    {
  	  l.setWidth(200);
    }

    setText(text);
    
    addComponentListener(new ComponentAdapter()
    {
      @Override
			public void componentResized(ComponentEvent ce)
      {
        resized();
      }
    });

    addHierarchyBoundsListener(new HierarchyBoundsAdapter()
    {
      @Override
			public void ancestorResized(HierarchyEvent he)
      {
        resized();
      }
    });
    
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
						if(np!=null) doAction(np.getNode(),np.getPos(),me);
					}
					catch(BugException e)
					{
						e.printStackTrace();
					}
					l.clearHighlight();
					repaint();
					aCopy.setEnabled(false);
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent me)
			{
				if(me.getButton()==MouseEvent.BUTTON1)
					aCopy.setEnabled(!l.getHighlightText().equals(""));		
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
    	}
    	
			@Override
			public void mouseMoved(MouseEvent me)
			{
				try
				{
					LayoutInline.NodePos np=getResolvedNodePos(me);
					if(np!=null && isAction(np.getNode(),np.getPos()))
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
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(KeyEvent.VK_C,InputEvent.CTRL_MASK),"copy");
		getActionMap().put("copy",aCopy);
  }
	
	/**
	 * Return a resolved (with XML data) node position in document.
	 * @param me Mouse position or null if not on a word
	 * @return Position
	 * @throws BugException 
	 */
	private LayoutInline.NodePos getResolvedNodePos(MouseEvent me) throws BugException
	{
		// Get position in layout objects
		LinkedList<LayoutThing> ll=new LinkedList<LayoutThing>();
		LayoutInline.NodePos np=l.getNodePos(me.getX(),me.getY(),ll);
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
		
		return np;
	}
	
	/**
	 * @param preferredWidth New preferred width
	 */
  public void setPreferredWidth(int preferredWidth)
  {
  		this.preferredWidth = preferredWidth;
  }
  
	/**
	 * Shows the context menu.
	 * @param me Mouse event
	 */
	private void triggerPopup(MouseEvent me)
	{
		JPopupMenu pm=new JPopupMenu();
		pm.add(aCopy);
		pm.show(this,me.getX(),me.getY());
	}
	
	/**
	 * Copy action
	 */
	private Action aCopy=new CopyAction();	
	class CopyAction extends AbstractAction
	{
		CopyAction()
		{
			putValue(NAME,"Copy");
			putValue(MNEMONIC_KEY,new Integer(KeyEvent.VK_C));
			putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_C,InputEvent.CTRL_MASK));
			setEnabled(false);
		}
		@Override
		public void actionPerformed(ActionEvent ae)
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
				new StringSelection(l.getHighlightText()),StaticLayout.this);
		}		
	}
	
  private int 
    highlightStartX,highlightStartY;
  
  /**
	 * Begins highlighting.
   * @param x Start X
   * @param y Start Y
	 */
  private void startHighlight(int x,int y)
  {
	  	highlightStartX=x;
	  	highlightStartY=y;
  }

	/**
	 * Updates highlight.
	 * @param x New end X
	 * @param y New end Y
	 */  
  private void moveHighlight(int x ,int y)
  {
  		Insets i=getInsets();
  		l.setHighlight(highlightStartX-i.left,highlightStartY-i.top,x-i.left,y-i.top);
  		repaint();
  }

  void setWidth(int iWidth)
  {
		Insets i=getInsets();
    l.setWidth(iWidth-i.left-i.right);
  }
  
  /**
   * @return Preferred width
   */
  public int getPreferredWidth()
  {
	  if(preferredWidth!=-1)
  		return preferredWidth;
	  else
  		return iCurrentPreferredWidth;
  }
  
  /**
   * @param iWidth Width
   * @return Preferred height at that width
   */
  public int getPreferredHeight(int iWidth)
  {
		Insets i=getInsets();
  		return l.getHeight(iWidth-i.left-i.right)+i.top+i.bottom;
  }
  
  /**
   * @return Initial line's baseline
   */
  public int getFirstBaseline()
  {
		Insets i=getInsets();
  		return l.getFirstBaseline()+i.top;
  }

  /**
   * Sets text.
   * @param text New text
   */
	public void setText(String text)
  {
		if(text.equals(thisText)) return;		
    try
    {
	    	StyleContext sc=StyleContext.getDefault(false);
	    	// TODO Set up colours to be appropriate for labels etc here?
      setText(text,sc);
    }
    catch(Exception e)
    {
      throw new Error("Unexpected error",e);
    }
  }

	/**
	 * Sets text of layout.
	 * @param text New text
	 * @param sc Style context
	 */
	public void setText(String text, StyleContext sc)
  {
    try
    {
  		if(preferredWidth==-1)
  		{
    	  l.setWidth(200);
  		}

      l.clear();
      LayoutBlock[] alb = LayoutGenerator.getLayout(sc,
      	XML.parse("<output>" + text + "</output>").getDocumentElement());
      for(int i=0;i<alb.length;i++)
      {
        l.addBlock(sc,alb[i]);
      }
      l.resolveStyle(sc);

      if(preferredWidth == -1)
      {
        Insets i = getInsets();
    	  iCurrentPreferredWidth = l.getUsedWidth() + i.left + i.right;
  	    l.setWidth(iCurrentPreferredWidth - i.left - i.right);
      }

      revalidate();
      repaint();
    }
    catch(Exception e)
    {
      throw new Error("Invalid layout string: " + text, e);
    }
  }
	
  @Override
	public Dimension getPreferredSize()
  {
    // In a viewport, always resize to the actual width of viewport
    if(getParent()!=null && getParent() instanceof JViewport)
    {
      setWidth(getParent().getWidth());
    }

		Insets i=getInsets();
    return new Dimension(l.getWidth()+i.left+i.right,l.getHeight(l.getWidth())+i.top+i.bottom);
  }

  private void resized()
  {
		Insets i=getInsets();
		int availableWidth=getWidth()-i.left-i.right;
		
    if(l.getWidth()!=availableWidth && availableWidth>0)
    {
      int iOldHeight=l.getHeight(l.getWidth());
      l.setWidth(availableWidth);
      int iNewHeight=l.getHeight(l.getWidth());

      if(iOldHeight!=iNewHeight)
      {
        revalidate();
      }
    }
  }

  @Override
	public void paintComponent(Graphics g)
  {
  		Insets i=getInsets();
    l.paint((Graphics2D)g,i.left,i.top,getWidth()-i.left-i.right,0,getHeight()-i.top-i.bottom);
  }

  @Override
	public boolean isOpaque() { return false; }

	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1)
	{
		// Don't care
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
}
