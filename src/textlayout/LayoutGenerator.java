package textlayout;

import java.util.*;

import org.w3c.dom.*;

import textlayout.stylesheet.*;
import util.StringUtils;
import util.xml.XML;

/**
 * Static class with utilities to create a layout based on XML.
 */
public abstract class LayoutGenerator
{
  /**
	 * Converts XML output tag to actual output blocks.
   * @param sc Context
   * @param e XML element
   * @return Blocks
   * @throws LayoutException 
	 */
	public static LayoutBlock[] getLayout(StyleContext sc,Element e) throws LayoutException
  {
    LayoutBlock lb=convertBlock(sc,e);
    if(lb==null)
      return new LayoutBlock[0];
    else
      return new LayoutBlock[]
      {
        lb
      };
  }

  /**
	 * Converts an XML block to a LayoutBlock.
   * @param sc Context
   * @param e Element
   * @return Single block
   * @throws LayoutException 
	 */
  private static LayoutBlock convertBlock(StyleContext sc,Element e) throws LayoutException
  {
    String[] context=new String[] {getContextString(e)};
    return processBlock(sc,fixParagraphs(sc,e,context,true),context,true, false);
  }
  
  /**
	 * Converts a block of data by adding &lt;para&gt; where necessary.
   * @param sc Context
   * @param eParent Parent tag
   * @param context Tag stack context
   * @param bTopLevel True if top level
   * @return Converted XML
   * @throws LayoutException 
	 */
  private static Element fixParagraphs(StyleContext sc,Element eParent,String[] context,boolean bTopLevel) throws LayoutException
  {
  	Element eNewParent = (Element)eParent.cloneNode(false);
  	boolean bAfterP=false;
	  	
		List<Node> lUncontained=new LinkedList<Node>();
		
		NodeList nl=eParent.getChildNodes();
		for(int iNode=0;iNode<nl.getLength();iNode++)
		{
			Node n=nl.item(iNode);
			if(n instanceof Text)
			{
				lUncontained.add(n.cloneNode(true));
			}
			else if(n instanceof Element)
			{
				Element e=(Element)n;
				String[] newContext=StringUtils.addLast(context,getContextString(e));

				String sType=e.getTagName();
				if(sc.isInline(newContext))
				{
					lUncontained.add(e.cloneNode(true));
				}
				else
				{
					if(!lUncontained.isEmpty())
					{
						if(sType.equals("p") || bTopLevel || bAfterP)
						{
							Element ePara=eParent.getOwnerDocument().createElement("para");
							for(Iterator<Node> i=lUncontained.iterator();i.hasNext();)
							{
								ePara.appendChild(i.next());							
							}
							lUncontained.clear();
							eNewParent.appendChild(ePara);
						}
						else
						{
							for(Iterator<Node> i=lUncontained.iterator();i.hasNext();)
							{
								eNewParent.appendChild(i.next());							
							}							
							lUncontained.clear();
						}
					}
					
					if(sType.equals("p"))
					{
						bAfterP=true;
					}
					else // apart from <p>
					{
						eNewParent.appendChild(fixParagraphs(sc,e,newContext,false));						
					}
				}
			}
			else if(n instanceof Comment)
			{
			}
			else throw new LayoutException(
				"Unexpected node type: "+n.getClass().getName());
		}
		if(!lUncontained.isEmpty())
		{
			if(bTopLevel || bAfterP)
			{
				Element ePara=eParent.getOwnerDocument().createElement("para");
				for(Iterator<Node> i=lUncontained.iterator();i.hasNext();)
				{
					ePara.appendChild(i.next());							
				}
				lUncontained.clear();
				eNewParent.appendChild(ePara);
			}
			else
			{
				for(Iterator<Node> i=lUncontained.iterator();i.hasNext();)
				{
					eNewParent.appendChild(i.next());							
				}							
			}
		}

		
		return eNewParent;  	
  }

  /** 
   * Returns a layout block based on the given element.
   * @param sc Style context
   * @param eParent Element to examine
   * @param context Position in element tree
   * @param bAlwaysPara True if we always put paragraphs in (I think?)
   * @param inMarginBlock If we've already added a margin block
   * @return New layout block
   * @throws LayoutException Any error
   */ 
  private static LayoutBlock processBlock(StyleContext sc,
    Element eParent,String[] context,boolean bAlwaysPara, boolean inMarginBlock)
    throws LayoutException
  {
    List<LayoutBlock> lBlocks=new LinkedList<LayoutBlock>();
    List<WordInline> lInlines=new LinkedList<WordInline>();
    
    // If this is a margin block, we always return one of those
    if(!inMarginBlock && sc.isMarginBlock(context))
    {
    		return new MarginBlock(context,processBlock(sc,eParent,context,bAlwaysPara,true),sc);
    }

    // True if the next inline item should have its front trimmed
    boolean bInFirstPath=true;
    
    NodeList nl=eParent.getChildNodes();
    for(int i=0;i<nl.getLength();i++)
    {
      Node n=nl.item(i);
      if(n instanceof Element)
      {
        Element e=(Element)n;
				String[] newContext=StringUtils.addLast(context,getContextString(e));

				boolean inline=sc.isInline(newContext);
				if(sc.isUnknown(newContext))
				{
					// Guess whether it should be inline - if it has any sibling text (as opposed to elements)
					boolean forward=false;
					for(Node test=e;;test=forward ? test.getNextSibling() : test.getPreviousSibling())
					{
						// Switch direction - after checking backward, go forward
						if(test==null)
						{
							if(forward) break; // Done both ways
							test=e;
							forward=true;
							continue;
						}
						if(test instanceof Text) 
						{
							inline=true;
							break;
						}
					}
				}
        if(inline)
        {
          processInlineOnly(sc,e,newContext,lInlines,bInFirstPath && i==0);
          bInFirstPath=false;
        }
        else
        {
          // Add new textblock with any inlines up to now
          if(lInlines.size()>0)
          {
            lBlocks.add(new TextBlock(
              context,
              lInlines.toArray(new LayoutInline[lInlines.size()]),sc));
            lInlines.clear();
            bInFirstPath=true;
          }
          
          LayoutBlock lb=processBlock(sc,e,newContext,false, inMarginBlock);
          if(lb!=null) lBlocks.add(lb);
        }
      }
      else if(n instanceof Text)
      {
        // Make up stringbuffer from normalised text
        StringBuffer sb=new StringBuffer(
          XML.normaliseText(((Text)n).getData()));
        if(bInFirstPath && i==0 && Character.isWhitespace(sb.charAt(0)))
          sb.delete(0,1);

        int iPos=0;
        while(true)
        {
          WordInline wi=WordInline.nextWord(sb,context,sc,n,iPos);
          if(wi==null) break;
          lInlines.add(wi);
          iPos+=wi.getText().length();
        }
      }
      else if(n instanceof Comment)
      {
      }
      else throw new LayoutException(
        "Unexpected node type: "+n.getClass().getName());
    }

    // Add new textblock with any inlines up to now
    if(lInlines.size()>0)
    {
      lBlocks.add(new TextBlock(
        context,
        lInlines.toArray(new LayoutInline[lInlines.size()]),sc));
    }

    // Things actually marked as block always occupy a block; things left as
    // unknown do not.
    if(lBlocks.size()==1)
    	{
    		LayoutBlock lbInner = lBlocks.get(0);
    		return new SurroundedBlock(lbInner,context,sc);
    	}
    else if(lBlocks.size()==0)
    {    	
      return null;
    }

    return new SurroundedBlock(
    		new VerticalHolderBlock(lBlocks,context),context,sc);
  }
  
  /**
   * Obtains a 'context string' for an XML element, a string representation of
   * that element and its attributes. Basically it's the element name followed
   * by a 1-separated list of name=value for attributes.
   * @param e Element to convert
   * @return String
   */
  private static String getContextString(Element e)
  {
  		StringBuffer sb=new StringBuffer();
  		sb.append(e.getTagName());
  		NamedNodeMap nnm=e.getAttributes();
  		for(int i=0;i<nnm.getLength();i++)
  		{
  			sb.append('\u0001');
  			sb.append(nnm.item(i).getNodeName());
  			sb.append('=');
  			sb.append(nnm.item(i).getNodeValue());
  		}
  		return sb.toString();
  }

  /**
	 * Returns (in variable 'inlines') a bunch of inline items.
   * @param sc Context
   * @param eParent Parent tag
   * @param context Tag context stack
   * @param inlines List where inline items will be added
   * @param inFirstPath True if this is in the first path (what?)
   * @throws LayoutException 
	 */
  private static void processInlineOnly(
    StyleContext sc,Element eParent,String[] context,List<WordInline> inlines,boolean inFirstPath)
    throws LayoutException
  {
		NodeList nl=eParent.getChildNodes();
    for(int i=0;i<nl.getLength();i++)
    {
      Node n=nl.item(i);
      if(n instanceof Element)
      {
        Element e=(Element)n;
				String[] newContext=StringUtils.addLast(context,getContextString(e));

        if(sc.isBlock(newContext))
        {
          throw new LayoutException(
            "Invalid: block type <"+e.getTagName()+"> may not be included in inline element");
        }

        processInlineOnly(sc,e,newContext,inlines,inFirstPath && i==0);
      }
      else if(n instanceof Text)
      {
        // Make up stringbuffer from normalised text
        StringBuffer sb=new StringBuffer(
          XML.normaliseText(((Text)n).getData()));
        if(inFirstPath && i==0 && Character.isWhitespace(sb.charAt(0)))
          sb.delete(0,1);

        int pos=0;
        while(true)
        {
          WordInline wi=WordInline.nextWord(sb,context,sc,n,pos);
          if(wi==null) break;
          inlines.add(wi);
          pos+=wi.getText().length();
        }
      }
      else if(n instanceof Comment)
      {
      }
      else throw new LayoutException(
        "Unexpected node type: "+n.getClass().getName());
    }
  }
}
