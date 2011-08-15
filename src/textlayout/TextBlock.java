package textlayout;

import java.awt.Graphics2D;
import java.util.*;

import org.w3c.dom.*;

import textlayout.LayoutInline.NodePos;
import textlayout.stylesheet.*;
import util.StringUtils;
import util.xml.XML;

/**
 * Layout block that displays wrapped text.
 */
class TextBlock implements LayoutBlock
{
	private boolean current;
	
	private String[] context;
	
  /**
	 * Words
	 */
  private LayoutInline[] words;

  /**
	 * Current set width and calculated height
	 */
  private int width=-1,height=-1;

  /**
	 * Space between each line
	 */
  private int iLineSpacing;

  /**
	 * Calculated lines
	 */
  private Line[] lines;
  
  /**
   * Text alignment 
   */
  private int textAlign;
  
  /**
	 * First line indent
	 */
  private int firstLineIndent;
  
  /**
	 * Indent of other lines
	 */
  private int otherLineIndent;
  
  /**
	 * Text alignment constants
	 */
  public final static int 
    ALIGN_LEFT = 0,
    ALIGN_CENTER = 1,
    ALIGN_RIGHT = 2;

  /**
	 * Creates text block with the given block style.
   * @param context Tag stack
   * @param words Word inlines
   * @param sc Style context
   * @throws LayoutException 
	 */
  TextBlock(String[] context,LayoutInline[] words,StyleContext sc) throws LayoutException
  {
  		this.context=context;
    this.words=words;
    resolveStyle(sc);
  }
  
  @Override
	public String debugDisplay(String indent)
  {
  	  StringBuffer sb=new StringBuffer();
  	  sb.append(indent+"TextBlock ["+StringUtils.join("/",context)+"]\n");
  	  for(int i=0;i<words.length;i++)
		{
			sb.append(words[i].debugDisplay(indent+"  "));
		}
  	  return sb.toString();
  }
  
  @Override
	public void buildXML(Node parent,Map<LayoutInline, NodePos> translation)
  {
  		Element thisElement=SurroundedBlock.buildXML(parent,translation,context);
		Document d=thisElement.getOwnerDocument();
  	  Node lastElement=thisElement,lastText=null;
  	  String[] lastContext=context;
  	  List<LayoutInline> previous=new LinkedList<LayoutInline>();
	  for(int i=0;i<words.length;i++)
		{
	  		String[] thisContext=words[i].getContext();
  			
	  		// Check how much context is in common
  			int commonContext=0;
  			for(;commonContext<thisContext.length && commonContext<lastContext.length;commonContext++)
  			{
  				if(!thisContext[commonContext].equals(lastContext[commonContext]))
  					break;
  			}
  			
  			if(commonContext==thisContext.length && thisContext.length==lastContext.length)
  			{
				// Add to existing text...
  				if(lastText!=null)
  				{
	  	  			Node modified=d.createTextNode(
	    					lastText.getNodeValue()+words[i].getText()	);
	  	  			int pos=lastText.getNodeValue().length();
	  	  			lastText.getParentNode().appendChild(modified);
	  	  			XML.remove(lastText);
	  	  			lastText=modified;
  					translation.put(words[i],new LayoutInline.NodePos(lastText,pos));
  					
  					// Update Node pointed to by previous words
  					for(Iterator<LayoutInline> previousWords=previous.iterator();previousWords.hasNext();)
  					{
  						LayoutInline previousWord = previousWords.next();
  						LayoutInline.NodePos previousPos= translation.get(previousWord);
  						translation.put(previousWord,new LayoutInline.NodePos(lastText,previousPos.getPos()));  						
  					}  					
  					previous.add(words[i]);
  				}
  				else
  				{
  					lastText=d.createTextNode(words[i].getText()	);
  					lastElement.appendChild(lastText);
  					translation.put(words[i],new LayoutInline.NodePos(lastText,0));
  					previous.clear();
  					previous.add(words[i]);
  				}
  			}
  			else
  			{
  				// Need a new element... go up until we get back to common root
	  			for(int up=0;up<lastContext.length-commonContext;up++)
	  				lastElement=lastElement.getParentNode();
	  			
	  			// Now go down adding nodes until we get to right place
	  			for(int down=commonContext;down<thisContext.length;down++)
	  				lastElement=SurroundedBlock.addElement(lastElement,thisContext[down]);
	  			
	  			// Now add text node
	  			lastText=d.createTextNode(words[i].getText());
	  			lastElement.appendChild(lastText);
				translation.put(words[i],new LayoutInline.NodePos(lastText,0));
				previous.clear();
				previous.add(words[i]);
	  		}
	  		
	  		lastContext=thisContext;
		}
  }
  
	/**
	 * Updates font and colour based on styles (called by superclass constructor)
	 * @param sc New style context
	 * @throws LayoutException 
	 */
	void resolveStyle(StyleContext sc) throws LayoutException
	{
		String sTextAlign=sc.getString(Property.TEXT_ALIGN,context);
		if(sTextAlign.equals(Property.V_TEXT_ALIGN_RIGHT)) 
		  textAlign=ALIGN_RIGHT;
		else if(sTextAlign.equals(Property.V_TEXT_ALIGN_CENTRE)) 
		  textAlign=ALIGN_CENTER;
		else
		  textAlign=ALIGN_LEFT;
		
		otherLineIndent=sc.getNumber(Property.TEXT_INDENT,context);
		firstLineIndent=otherLineIndent+sc.getNumber(Property.TEXT_FIRST_INDENT,context);
	}

	/**
	 * Update style using a new OutputConverter
	 */
	@Override
	public void updateStyle(StyleContext sc) throws LayoutException
	{
		for(int i=0;i<words.length;i++)
		  words[i].updateStyle(sc);
		
		width=-1;
		resolveStyle(sc);
	}

  /**
	 * Recalculate block for given width
	 */
  @Override
	public void reflow(int width)
  {
    if(this.width==width || width<=0) return;
    this.width=width;

    List<Line> linesList=new LinkedList<Line>();
    List<LayoutInline> currentLine=new LinkedList<LayoutInline>();
    int x=firstLineIndent,maxAscent=0,maxDescent=0;
    boolean lineStart=true,firstLine=true,justBroke=true;
    
    // Build array of words, after splitting any that want to be split due to width
    LinkedList<LayoutInline> newWordsList=new LinkedList<LayoutInline>(Arrays.asList(words));
    
    for(int wordIndex=0;wordIndex<newWordsList.size();wordIndex++)
    {
      LayoutInline currentWord = newWordsList.get(wordIndex);
      currentWord.setWrappedIfNecessary(LayoutInline.UNWRAPPED);
            
			if(currentWord.skipAtLineStart() && lineStart)
      	{
      		lineStart=false;
      		continue;
      	}
      	lineStart=false;

      int
        w=currentWord.getWidth(),
        a=currentWord.getAscent(),
        d=currentWord.getDescent();

      // If we're not allowed to break newWords, count following in with width
      for(int subWord=wordIndex+1;subWord<newWordsList.size();subWord++)
      {
        if((newWordsList.get(subWord-1)).breakAfter()) break;
        LayoutInline subWordItem = newWordsList.get(subWord);
        if(subWordItem.breakBefore()) break;
        w+=subWordItem.getWidth();
      }

      if(x+w > width && !justBroke)
      {
        // Go on to next line
        linesList.add(new Line(firstLine ? firstLineIndent : otherLineIndent,
          currentLine.toArray(new LayoutInline[currentLine.size()]),
          maxAscent,maxDescent));
        currentLine.clear();

        // Forget this word; clear details and repeat it
        wordIndex--;
        maxAscent=0;
        maxDescent=0;
        x=otherLineIndent; 
        lineStart=true;
        firstLine=false;
        justBroke=true;
      }
      else
      {
        // Add word, update maxes
	    		currentWord.setWrappedIfNecessary(
	    			firstLine ? width-firstLineIndent : width-otherLineIndent);
        x+=currentWord.getWidth();
        if(a > maxAscent) maxAscent=a;
        if(d > maxDescent) maxDescent=d;
        currentLine.add(currentWord);
        justBroke=false;
        
        // Add any wrapped words
        LayoutInline wrapped=currentWord.getWrapped();
        while(wrapped!=null)
        {
      		newWordsList.add(wordIndex+1,wrapped);
      		wrapped=wrapped.getWrapped();
        }
      }
    }
    // Final line
    linesList.add(new Line(firstLine ? firstLineIndent : otherLineIndent,
      currentLine.toArray(new LayoutInline[currentLine.size()]),
      maxAscent,maxDescent));

    // Convert to array
    lines = linesList.toArray(new Line[linesList.size()]);

    // Calculate height
    height=0;
    for(int i=0;i<lines.length;i++)
    {
      height+=lines[i].getHeight();
    }
  }
  
  /**
	 * Get height at last-specified width
	 */
  @Override
	public int getHeight()
  {
    if(height==-1) 
  		throw new Error("Must reflow before calling getHeight");
    return height;
  }
  
  @Override
	public int getUsedWidth()
  {
    if(height==-1) throw new Error("Must reflow before calling getUsedWidth");
    int usedWidth=0;
    for(int i=0;i<lines.length;i++)
    {
  	  usedWidth=Math.max(usedWidth,lines[i].getWidth());
    }
    return usedWidth;
  }

  /**
	 * Paint into given graphics context at given start location
	 */
  @Override
	public void paint(Graphics2D g,int x,int y)
  {
    if(height==-1) throw new Error("Must reflow before calling paint");

    for(int iLine=0;iLine<lines.length;iLine++)
    {
      lines[iLine].paint(g,x,y);
      y+=lines[iLine].getHeight();
    }
  }
  
  @Override
	public int getFirstBaseline()
  {
		if(lines==null || lines.length==0) return 0;
		return lines[0].getBaseline();
  }


	@Override
	public void clearHighlight()
	{
		if(height==-1) throw new Error("Must reflow before calling clearHighlight");

		for(int iLine=0;iLine<lines.length;iLine++)
		{
			lines[iLine].clearHighlight();
		}
	}

	@Override
	public void setHighlight(int lowX, int lowY, int highX, int highY)
	{
		if(height==-1) throw new Error("Must reflow before calling setHighlight");

		int y=0;
		for(int iLine=0;iLine<lines.length;iLine++)
		{
			Line l=lines[iLine];
			int newY=y+l.getHeight();
			
			if(y > highY || newY < lowY)
			  l.clearHighlight();
			else
			{
				if(y<lowY)
				{
					if(highY<newY)
					{
						l.setHighlight(lowX,highX);						
					}
					else
					{
						l.setHighlight(lowX,Layout.HIGHLIGHT_TOEND);
					}
				}
				else if(highY<newY)
				{
					l.setHighlight(Layout.HIGHLIGHT_TOSTART,highX);
				}
				else
				{
					l.setHighlight(Layout.HIGHLIGHT_TOSTART,Layout.HIGHLIGHT_TOEND);
				}				
			}

			y=newY;
		}
	}

	/** @return The currently highlighted string from this block */
	@Override
	public String getHighlightText()
	{
		StringBuffer sb=new StringBuffer();
		
		for(int iLine=0;iLine<lines.length;iLine++)
		{
			Line l=lines[iLine];
			sb.append(l.getHighlightText());
		}
		return sb.toString();
	}


	@Override
	public LayoutInline.NodePos getNodePos(int targetX, int targetY, List<LayoutThing> blocks)
	{
		if(height==-1) throw new Error("Must reflow before calling getNodePos");

		int y=0;
		for(int iLine=0;iLine<lines.length;iLine++)
		{
			Line l=lines[iLine];
			int newY=y+l.getHeight();
			
			if(targetY>=y  && targetY <newY)
			{
				return l.getNodePos(targetX,blocks);
			}

			y=newY;
		}
		return null;
	}
  
	
	/**
	 * A single line that's been laid out
	 */
	class Line
	{
		int indent,ascent,descent;
		LayoutInline[] items;

		Line(int indent,LayoutInline[] items,int ascent,int descent)
		{
			this.items=items;
			this.indent=indent;
			this.ascent=ascent;
			this.descent=descent;
		}

		/**
		 * @param targetX Target X-coordinate
		 * @param blocks List of blocks
		 * @return Actual position
		 */
		public LayoutInline.NodePos getNodePos(int targetX,List<LayoutThing> blocks)
		{
			int x=getStartOffset();      
			for(int i=0;i<items.length;i++)
			{
				LayoutInline li=items[i];
				int newX=x+li.getWidth();

				if(targetX>=x && targetX<newX)
				{
					blocks.add(li);
					return li.getNodePos(targetX-x);
				}
				
				x=newX;
			}
			return null;
		}

		int getHeight() { return ascent+descent+iLineSpacing; }
		
		int getWidth()
		{
			int x=getStartOffset();      
			for(int i=0;i<items.length;i++)
			{
				x+=items[i].getWidth();
			}
			
			return x;			
		}

		void paint(Graphics2D g2,int x,int y)
		{
			int baselineY=getBaseline()+y;

			x+=getStartOffset();      

			// Paint starting at correct initial X
			for(int i=0;i<items.length;i++)
			{
				items[i].paint(g2,x,baselineY);
				x+=items[i].getWidth();
			}      
		}
		
		int getBaseline()
		{
			return ascent+iLineSpacing/2;
		}
		
		/** @return Start X offset because of centreing or right-justification */
		private int getStartOffset()
		{
			if(textAlign!=ALIGN_LEFT)
			{
				// Precalculate width
				int totalWidth=0;
				for(int i=0;i<items.length;i++)
				{
					if(i==items.length-1)
						totalWidth+=items[i].getWidthLineFinal();
					else 
						totalWidth+=items[i].getWidth();
				}
				
				// Add on appropriate initial width
				switch(textAlign)
				{
					case ALIGN_CENTER:
						return (width-totalWidth)/2 + indent;
					case ALIGN_RIGHT:
						return (width-totalWidth) + indent;
					default:
						throw new Error("Unexpected align constant: "+textAlign);
				}				
			}
			else
			  return indent;
		}
		
		void clearHighlight()
		{
			for(int i=0;i<items.length;i++)
			{
				items[i].clearHighlight();
			}
		}
		
		void setHighlight(int startX,int endX)
		{
			int lowX=Math.min(startX,endX),highX=Math.max(startX,endX);
			int x=getStartOffset();      
			for(int i=0;i<items.length;i++)
			{
				LayoutInline li=items[i];
				int newX=x+li.getWidth();

				if(newX<lowX || x>highX)
				  li.clearHighlight();
				else
				{
					if(x<lowX) // Start of highlight in here
					{
						if(newX>highX) // End too
						{
							li.setHighlight(lowX-x,highX-x);
						}
						else
						{
						  li.setHighlight(lowX-x,Layout.HIGHLIGHT_TOEND);
						}
					}
					else if(newX > highX) // End of highlight in here
					{
						li.setHighlight(Layout.HIGHLIGHT_TOSTART,highX-x);
					}
					else
					{
						li.setHighlight(Layout.HIGHLIGHT_TOSTART,Layout.HIGHLIGHT_TOEND);
					}
				}			
				
				x=newX;	
			}      
		}
		
		String getHighlightText()
		{
			StringBuffer sb=new StringBuffer();
			for(int i=0;i<items.length;i++)
			{
				sb.append(items[i].getHighlightText());
			}
			return sb.toString();
		}
		
	}

	@Override
	public int getFlowCategory()
	{
		return FLOWCATEGORY_NORMAL;
	}

	@Override
	public int getMaxX()
	{
		return width;
	}

	@Override
	public int getMinX()
	{
		return 0;
	}

	@Override
	public boolean setCurrent(StyleContext sc,boolean current) throws LayoutException
	{
    if(this.current==current) return false;
    this.current=current;
    resolveStyle(sc);
    return true;
  }

}
