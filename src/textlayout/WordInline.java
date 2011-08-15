package textlayout;

import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.image.*;
import java.util.*;

import org.w3c.dom.Node;

import textlayout.stylesheet.*;
import util.*;

/**
	 * Represents a single word, usually including a terminating space.
	 */
class WordInline implements LayoutInline
{
  /**
	 * Styles for this word
	 */
  private String[] context=null;

  /**
	 * Actual word
	 */
  private String word;

  /**
	 * Font
	 */
  private Font f;

  /**
	 * Colour
	 */
  private Color c,bg;
  
  /**
	 * Outline (requires prerendering)
	 */
  private Color outline;
  
  /**
	 * Underline
	 */
  private boolean underline;

  /**
	 * Whether word has whitespace at the ends
	 */
  private boolean bAllowBreakAfter,bAllowBreakBefore,bSkipAtLineStart;

  /**
	 * If true, we're allowed to wrap on letters when it won't fit on a line
	 */
	private boolean allowWrap;
	
	/**
	 * If true, this word is currently wrapped
	 */
	private boolean wrapped;
	
	/**
	 * If non-null, this word was generated from another word that wrapped
	 */
	private WordInline wrappedParent;
	
	private String wrappedWord;
	
	/**
	 * Offset from the start of the real LayoutInline, used when wrapping
	 */
	private int wrappedOffset;
	
  /**
	 * Wrapped second part of word
	 */
  private WordInline wrappedOverflow;
  
  private int wrappedWidth,wrappedLineWidth;

  /**
	 * Stored size details
	 */
  private int ascent,descent,width;
  
  /**
	 * Line-final width (different if the word ends in a space)
	 */
  private int widthLineFinal;
  
  /**
   * @param word Word
   * @param f Font
   * @param c Colour
	 */
  WordInline(String word,Font f,Color c)
  {
    this.word=word;
    this.f=f;
    this.c=c;
    updateBreaking();

    LineMetrics lm=f.getLineMetrics(word,GraphicsUtils.getFontRenderContext());
    ascent= (int)(lm.getAscent() + 0.5);
    descent= (int)(lm.getDescent() + 0.5);

		updateWidth(f);    
  }
    
  private void updateBreaking()
  {
    this.bAllowBreakAfter=
      Character.isWhitespace(
        word.charAt(word.length()-1));
    this.bAllowBreakBefore=
      Character.isWhitespace(
        word.charAt(0));
    bSkipAtLineStart=word.trim().equals("");
  }

  /**
   * @param word Word
   * @param style Styles (tag stack)
   * @param sc Style context
   * @throws LayoutException 
	 */
  WordInline(String word,String[] style,StyleContext sc) throws LayoutException
  {
    this.word=word;
    this.context=style;
    updateBreaking();

    resolveStyle(sc);
  }
  
  WordInline(WordInline parent,int wrapPos)
  {
  	  wrappedParent=parent;
  	  
  		word=parent.word.substring(wrapPos);
  		context=parent.context;
  		wrappedOffset=parent.wrappedOffset+wrapPos;
  		updateBreaking();
  		
  		if(parent.highlightSize>0)
  		{
  			highlightStart=parent.highlightStart-wrapPos;
  			if(highlightStart<0)
  			{
  				highlightSize=Math.max(0,parent.highlightSize+highlightStart);
  				highlightStart=0;
  			}
  			else
  				highlightSize=parent.highlightSize;
  		}
  		
    f=parent.f;
	  c=parent.c;
	  bg=parent.bg;
    ascent=parent.ascent;
    descent=parent.descent;
		updateWidth(f);
	  allowWrap=true;
		underline=parent.underline;  
	}

	/**
	 * Update style using a new OutputConverter
	 */
	@Override
	public void updateStyle(StyleContext sc) throws LayoutException
	{
		resolveStyle(sc);		
	}
	
  /**
	 * Updates font and colour based on styles.
   * @param sc Style context
   * @throws LayoutException 
	 */
  void resolveStyle(StyleContext sc) throws LayoutException
  {
    f=sc.getFont(context);
    c=sc.getRGB(Property.TEXT_RGB,context);
    bg=sc.getRGB(Property.TEXT_BACKGROUND_RGB,context);

    // Get font metrics [Adding Aj is just to make words take the full ascent/descent even if they have no caps/descenders]
    LineMetrics lm=f.getLineMetrics(word+"Aj",GraphicsUtils.getFontRenderContext());
		ascent= (int)(lm.getAscent() + 0.5);
    descent= (int)(lm.getDescent() + 0.5);

    // If we're supposed to be matching some other baseline, get that
    int matchBaseline=sc.getNumber(Property.MATCH_BASELINE,context);
    if(matchBaseline!=0)
    {
    		int matchAscent=(int)(f.deriveFont((float)matchBaseline).getLineMetrics(
    			word,GraphicsUtils.getFontRenderContext()).getAscent()+0.5);
    		ascent=Math.max(ascent,matchAscent);
    }

    width=sc.getNumber(Property.WIDTH,context);	    
    if(width==-1)
    {
			updateWidth(f);
			allowWrap=sc.getString(Property.WRAP_STYLE,context).equals(Property.V_WRAP_STYLE_NORMAL);
    }
		else
		{
		  widthLineFinal=width;
		  allowWrap=false;
		}
		  
		String sUnderline=sc.getString(Property.FONT_UNDERLINE,context);
		underline=sUnderline.equals(Property.V_FONT_UNDERLINE_SOLID);
		
		outline=sc.getRGB(Property.OUTLINE,context);
		if(outline.getAlpha()==0) outline=null;
		
		useBackground=outline!=null;
		putBackgroundImage(null);
  }
  
  private boolean useBackground=false;

  // Background image cache (designed to reduce memory use over storing all of them)  
  //////////////////////////////////////////////////////////////////////////////////
  
  /**
	 * Time after which background cache items expire
	 */
  private final static int BACKGROUNDCACHEEXPIRE=60000;  
  
  /**
	 * Approx. frequency with which the cache expiry check run happens
	 */
  private final static int BACKGROUNDCACHEEXPIRYRUN=30000;
  
  /**
	 * Last expiry run time
	 */
  private static long backgroundLastExpiryRun=0;
  
  /**
	 * Map of Object (actually WordInline) to BackgroundImage
	 */
  private static Map<WordInline, BackgroundImage> backgroundImages=new HashMap<WordInline, BackgroundImage>();
  
  /**
	 * Stores a background image and tracks the time it was last used
	 */
  private static class BackgroundImage
  {
		/**
		 * Image
		 */
		BufferedImage bi;
		
		/**
		 * Time of use
		 */
		long lastUsed;

		public BackgroundImage(BufferedImage bi)
		{
			this.bi=bi;
			poke();
		}

		/**
		 * Updates use time
		 */
		void poke()
		{
			lastUsed=System.currentTimeMillis();
		}
  }
  
  /**
   * Gets the background image for this item. Also handles the cache if necessary
   * to reduce it etc.
   * @return The image or null if not in cache
   */
  private BufferedImage getBackgroundImage()
  {
  	synchronized(backgroundImages)
  	{
  		BackgroundImage background=(BackgroundImage)backgroundImages.get(this);
  		if(background!=null) 
  			background.poke();
  		
  		// Is it time to shrink the cache?
  		long now=System.currentTimeMillis();
  		if(now-backgroundLastExpiryRun > BACKGROUNDCACHEEXPIRYRUN)
  		{
  			backgroundLastExpiryRun=now;
  			for(Iterator<BackgroundImage> i=backgroundImages.values().iterator();i.hasNext();)
  			{
				BackgroundImage old=(BackgroundImage)i.next();
				if(now - old.lastUsed > BACKGROUNDCACHEEXPIRE)
					i.remove();
  			}
  		}
			
  		return background==null ? null : background.bi;
		}
  }
  
  /**
   * Saves the background image for this item.
   * @param bi New image to put into cache, or null to clear cached image.
   */
  private void putBackgroundImage(BufferedImage bi)
  {
		if(bi==null)
			backgroundImages.remove(this);
		else
			backgroundImages.put(this,new BackgroundImage(bi));
  }
  
  private final static int IMAGEBORDER=3;
  
  private static int OUTLINEBLURPASSES = 2;

  private static void doOutline(BufferedImage img,Color c)
  {
		int[]data=((DataBufferInt)img.getRaster().getDataBuffer()).getData();
		int[]original=new int[data.length];
		System.arraycopy(data,0,original,0,original.length);
		
		int w=img.getWidth(),h=img.getHeight();
		  		
		// Offset so that we start in the other direction if needed
		int passOffset=(OUTLINEBLURPASSES&1)==1 ? 0 : 1;
		
		int colour=(c.getRed()<<16) | (c.getGreen()<<8) | c.getBlue(); 
		int alpha=c.getAlpha();
		if(alpha==255) alpha=256; // Lets me do >>8 instead of /255 later - woo!
		
		// Loop through a number of blur passes
		for(int pass=0;pass<OUTLINEBLURPASSES;pass++)
		{
			int[] from,to;
			if(((pass+passOffset)&1)==1)
			{
				from=data;
				to=original;
			}
			else
			{
				from=original;
				to=data;
			}
	
  		for(int y=1;y<h-1;y++)
  		{
  			int ofs=y*w+1; // Because we start at position 1, not 0
  			int upto=ofs+w-2;
  			for(;ofs<upto;ofs++)
  			{
				int blur=0;
				blur=4*(from[ofs]>>>24);
				blur+=4*(from[ofs-1]>>>24); // Left
				blur+=4*(from[ofs+1]>>>24); // Right
				blur+=4*(from[ofs-w]>>>24); // Up
				blur+=4*(from[ofs+w]>>>24); // Down
				blur+=2*(from[ofs-w-1]>>>24); // Up left
				blur+=2*(from[ofs-w+1]>>>24); // Up right
				blur+=2*(from[ofs+w-1]>>>24); // Down left
				blur+=2*(from[ofs+w+1]>>>24); // Down right
				blur>>>=3; // Divide by 16
				if((blur&0xffffff00)!=0) blur=0xff;
				to[ofs]=(((blur*alpha)>>>8)<<24) | colour;
  			}
  			// Because we skip last position
  			ofs++;
  		}
		}
  }
  
  private static int getWidth(String s,Font f)
  {
	  return (int)(f.getStringBounds(s,GraphicsUtils.getFontRenderContext()).getWidth()+0.5f);
//  		if(s.length()==0) 
//  			return 0;
//  		else
//  			return (int)((new TextLayout(s,f,GraphicsUtils.getFontRenderContext())).getAdvance()+0.5f);
  }

  private static Rectangle getBounds(String s,Font f)
  {
	  return f.getStringBounds(s,GraphicsUtils.getFontRenderContext()).getBounds();
//  		TextLayout tl=new TextLayout(s,f,GraphicsUtils.getFontRenderContext());
//  		Rectangle r=new Rectangle();
//  		r.width=(int)(tl.getAdvance()+0.5f);
//  		r.height=(int)(tl.getAscent()+tl.getDescent()+0.5f);
//  		r.y=-(int)(tl.getAscent()+0.5f);
//  		return r;
  }
  
  private void updateWidth(Font f)
  {
		// Calculate width
  		width=getWidth(word,f);
		setWrappedIfNecessary(-1); // Forget wrapped data
    
		// Line-final width (width after trimming whitespace from right)
		int iNewLength=word.length();
		for(;iNewLength>0;iNewLength--)
		{
			if(!Character.isWhitespace(word.charAt(iNewLength-1)))
				break;
		}
		if(iNewLength==word.length())
		{ 
			widthLineFinal=width;
		}
		else
		{
			widthLineFinal=getWidth(word.substring(0,iNewLength),f);
		}    	
  }

  /**
	 * Gets next word from plaintext stringbuffer.
   * @param sb Buffer
   * @param f Font
   * @param c Colour
   * @return Next word, or null if none remain
	 */
  static WordInline nextWord(StringBuffer sb,Font f,Color c)
  {
    if(sb.length()==0) return null;

    for(int i=0;i<sb.length();i++)
    {
      if(Character.isWhitespace(sb.charAt(i)))
      {
        // Create word based on the buffer up to here
        WordInline wiResult=new WordInline(
          sb.substring(0,i+1),f,c);
        sb.delete(0,i+1);
        return wiResult;
      }
    }

    // Return whole buffer
    WordInline wiResult=new WordInline(sb.toString(),f,c);
    sb.setLength(0);
    return wiResult;
  }

  /**
	 * Gets next word from plaintext stringbuffer.
   * @param sb Buffer
   * @param asStyle Style (tag stack)
   * @param sc Style context
   * @param n Node (not sure what this is for)
   * @param iPos Pos within node (ditto)
   * @return Next word, or null if none remain
   * @throws LayoutException 
	 */
  static WordInline nextWord(StringBuffer sb,String[] asStyle,StyleContext sc,Node n,int iPos) throws LayoutException
  {
    if(sb.length()==0) return null;

    for(int i=0;i<sb.length();i++)
    {
      if(Character.isWhitespace(sb.charAt(i)))
      {
        // Create word based on the buffer up to here
        WordInline wiResult=new WordInline(
          sb.substring(0,i+1),asStyle,sc);
        sb.delete(0,i+1);
        return wiResult;
      }
    }

    // Return whole buffer
    WordInline wiResult=new WordInline(sb.toString(),asStyle,sc);
    sb.setLength(0);
    return wiResult;
  }

  /**
	 * Obtain width of item
	 */
  @Override
	public int getWidth()
  {
  		return wrapped ? wrappedWidth : width;
  }
  
  @Override
	public void setWrappedIfNecessary(int lineWidth)
  {
  	  if(wrappedLineWidth==lineWidth) return;

  		if(!allowWrap || lineWidth>=width || lineWidth==-1)
  		{
  			wrappedLineWidth=lineWidth; // Just to stop us recalculating...
  			wrapped=false;
  			wrappedOverflow=null;  			
  			wrappedWord=null;
  		}
  		else
  		{
  			// Split the word
  			int lastWidth=-1;
  			for(int character=1;character<word.length();character++)
  			{
  				int widthSoFar=getWidth(word.substring(0,character+1),f);
  				
  				if(lastWidth!=-1 && widthSoFar>lineWidth)
  				{
  					wrapped=true;
  					wrappedLineWidth=lineWidth;
  					wrappedWord=word.substring(0,character-1);
  					wrappedWidth=lastWidth;
  					wrappedOverflow=new WordInline(this,character-1);
  					break;
  				}
  				lastWidth=widthSoFar;
  			}
  		}
  }
  
  @Override
	public LayoutInline getWrapped()
  {
		return wrappedOverflow;
  }

	/**
	 * Obtain width of item when used at end of line (excluding final space)
	 */
	@Override
	public int getWidthLineFinal()
	{
		return widthLineFinal;
	}

  /**
	 * Obtain ascent above baseline
	 */
  @Override
	public int getAscent()
  {
    return ascent;
  }

  /**
	 * Obtain descent below baseline
	 */
  @Override
	public int getDescent()
  {
    return descent;
  }
  
  /**
	 * Paint into given context
	 */
  @Override
	public void paint(Graphics2D g2,int iX,int iBaselineY)
  {
		if(useBackground)
		{
			BufferedImage backgroundImage=getBackgroundImage();
			if(backgroundImage==null)
			{
				backgroundImage=new BufferedImage(widthLineFinal+2*IMAGEBORDER,ascent+descent+2*IMAGEBORDER,BufferedImage.TYPE_INT_ARGB);
				if(outline!=null)
				{
  				render(backgroundImage.createGraphics(),IMAGEBORDER,ascent+IMAGEBORDER,true);
  				doOutline(backgroundImage,outline);
				}
				putBackgroundImage(backgroundImage);
			}
			g2.drawImage(backgroundImage,iX-IMAGEBORDER,iBaselineY-ascent-IMAGEBORDER,null);
		}
		render(g2,iX,iBaselineY,false);
  }

  static boolean first=true,first2=true;
	private void render(Graphics2D g2,int iX,int iBaselineY,boolean prerender)
	{
		g2.setFont(f);
    
		if(bg!=RGBPropertyData.TRANSPARENT)
		{
			g2.setColor(bg);
			g2.fillRect(iX,iBaselineY-ascent,wrapped ? wrappedWidth : width,ascent+descent);
		}
		
		g2.setColor(c);
		if(underline)
			g2.drawLine(iX,iBaselineY+2,iX+(wrapped ? wrappedWidth : width),iBaselineY+2);
		
		String currentWord=wrapped?wrappedWord:word;
		int currentHighlightSize;
		if(wrapped)
		{
		 if(highlightStart >= wrappedWord.length())
			 currentHighlightSize=0;
		 else
			 currentHighlightSize=Math.min(highlightSize,wrappedWord.length()-highlightStart);
		}
		else
			currentHighlightSize=highlightSize;

    if(currentHighlightSize==0)
    {
			g2.setColor(c);
			g2.drawString(currentWord,iX,iBaselineY);			
    }
    else
    {
    	if(highlightStart>0)
    	{
			// Draw string before highlight
			g2.setColor(c);
			g2.drawString(currentWord.substring(0,highlightStart),iX,iBaselineY);
			iX+=getWidth(currentWord.substring(0,highlightStart),f);
    	}
    	
    	// Draw highlight
			g2.setColor(SystemColor.textHighlight);
	    	Rectangle r=getBounds(currentWord.substring(highlightStart,highlightStart+currentHighlightSize),f);
			r.translate(iX,iBaselineY);
			g2.fill(r);			
			g2.setColor(SystemColor.textHighlightText);
			g2.drawString(
			  currentWord.substring(highlightStart,highlightStart+currentHighlightSize),
			  iX,iBaselineY);
			iX+=r.width;
			
			// Draw string after highlight
			if(highlightStart+currentHighlightSize < word.length())
			{
				g2.setColor(c);
				g2.drawString(currentWord.substring(highlightStart+currentHighlightSize),
				  iX,iBaselineY);				
			}
			
    }
	}
  
  @Override
	public boolean breakAfter()
  {
    return bAllowBreakAfter || wrapped;
  }

  @Override
	public boolean breakBefore()
  {
    return bAllowBreakBefore || wrappedParent!=null;
  }

  @Override
	public String toString()
  {
    return "["+word+"]";
  }

  /**
	 * True if this shouldn't be displayed at line starts
	 */
  @Override
	public boolean skipAtLineStart()
  {
    return bSkipAtLineStart;
  }
  
  /**
	 * Start position of highlight
	 */
  private int highlightStart=0;
  
  /**
	 * Length in characters of highlight
	 */
  private int highlightSize=0;

	/**
	 * Remove highlighting from this item
	 */
	@Override
	public void clearHighlight()
	{
		highlightSize=0;
	}
  
	/** Set highlight on this item
	 * @param iStartX Begining (relative to this item) or Layout.HIGHLIGHT_TOSTART
	 * @param iEndX End (relative to this item) or Layout.HIGHLIGHT_TOEND
	 */
	@Override
	public void setHighlight(int iStartX,int iEndX)
	{
		// Deal with entire word selections
		String currentWord=wrapped ? wrappedWord : word;
		if(iStartX<=0 && iEndX>=(wrapped ? wrappedWidth : width))
		{
			highlightStart=0;
			highlightSize=currentWord.length();
			informWrappedParentHighlight();
			return;
		}

		// Find out the character position of selection
		highlightStart=-1; highlightSize=0;
		for(int iCharacter=0;iCharacter<currentWord.length();iCharacter++)
		{
			int iWidthSoFar=getWidth(currentWord.substring(0,iCharacter+1),f);
			
			// Check if highlight should have started yet  
			if(highlightStart==-1 && iWidthSoFar>iStartX)
			{
				highlightStart=iCharacter;
				highlightSize=currentWord.length()-highlightStart;
			}
			
			// End of highlight?
			if(highlightStart!=-1 && iWidthSoFar>iEndX) 
			{
				highlightSize=iCharacter-highlightStart;
				break;				
			} 
		}
		
		informWrappedParentHighlight();
	}
	
	/**
	 * Used to inform the wrapped parent about the highlight. 
	 * <p>
	 * Wrapped parents
	 * actually maintain their own information about the full range of highlight
	 * (because the wrapped-child words may be discarded any time during resize)
	 * so this is how we keep them informed. 
	 * <p>
	 * Note that it only works because
	 * setHighlight is always called in document order i.e. the parent (which
	 * is placed before) will have already received the setHighlight.
	 */
	private void informWrappedParentHighlight()
	{
		if(wrappedParent!=null)
		{
			if(wrappedParent.highlightSize==0)
			{
				// Parent doesn't have a highlight yet, so give it the whole lot
				wrappedParent.highlightStart=highlightStart+wrappedParent.wrappedWord.length();
				wrappedParent.highlightSize=highlightSize;				
			}
			else
			{
				// Parent does include the highlight, so extend it to include this
				wrappedParent.highlightSize=wrappedParent.wrappedWord.length()-wrappedParent.highlightStart+highlightSize;
			}
			// And it should tell its parent too
			wrappedParent.informWrappedParentHighlight();
		}
	}
	
	/** @return The currently highlighted string from this inline */
	@Override
	public String getHighlightText()
	{
		if(highlightSize==0 || wrappedParent!=null) return "";
		return word.substring(highlightStart,highlightStart+highlightSize);
	}
	
	@Override
	public String getText()
	{
		return wrappedParent==null ? word : "";
	}
	
	public WordInline getWrappedRoot()
	{
		return wrappedParent!=null ? wrappedParent.getWrappedRoot() : this;
	}
	
	@Override
	public NodePos getNodePos(int iX)
	{
		String currentWord=wrapped ? wrappedWord : word;
		// Find out the character position of selection
		for(int character=0;character<currentWord.length();character++)
		{
			int widthSoFar=getWidth(currentWord.substring(0,character+1),f);
			
			// Are we there yet?  
			if(widthSoFar>iX)
			{
				return new NodePos(getWrappedRoot(),character+wrappedOffset);
			}
		}
		return null;
	}

	@Override
	public String debugDisplay(String indent)
	{
		return indent+"WordInline ["+StringUtils.join("/",context)+"]: "+word+"\n";
	}

	@Override
	public String[] getContext()
	{
		return context;
	}
	
}
