package textlayout;

import java.awt.Color;
import java.io.*;

import org.w3c.dom.*;

import util.PlatformUtils;
import util.xml.*;

/**
	 * User stylesheet
	 */
public class UserStylesheet
{
	/**
	 * Single shared element (so we don't get confused and edit different docs)
	 */
	private static UserStylesheet usShared=null;
	
	/**
	 * Document representing stylesheet
	 */
	private Document d;
	
	/**
	 * File used for user stylesheet
	 */
	private File f;
	
	/**
	 * Element matching !normal
	 */
	private Element eNormal;

	/**
	 * Loads or creates stylesheet.
	 * @throws XMLException 
	 */	
	private UserStylesheet() throws XMLException
	{
		// Set file
		f=new File(PlatformUtils.getUserFolder(),"userstyle.xml");
		
		// Open file or create document
		if(f.exists())
		{
			d=XML.parse(f);
		}
		else
		{
			d=XML.newDocument("styles");
		}
			
		// Find or create style element
		Element[] aeStyles=XML.getChildren(
		  d.getDocumentElement(),"style");
		for(int i=0;i<aeStyles.length;i++)
		{
		  if("!normal".equals(aeStyles[i].getAttribute("match")))
		  {
				eNormal=aeStyles[i]; 
				break;			
		  }		
		}
		if(eNormal==null)
		{
			eNormal=d.createElement("style");
			eNormal.setAttribute("match","!normal");
			d.getDocumentElement().appendChild(eNormal);		
		}
	}
	
	/**
	 * Saves stylesheet.
	 * @throws XMLException 
	 */
	public void save() throws XMLException
	{
		XML.save(f,d);
	}
	
	/**
	 * @return Document element
	 */
	public Element getDocumentElement()
	{
		return d.getDocumentElement();
	}
	
	/**
	 * @return Shared user stylesheet 
	 * @throws XMLException 
	 */
	public static UserStylesheet get() throws XMLException
	{
		if(usShared==null)
		{
			usShared=new UserStylesheet();
		}
		return usShared;
	}
	
	/**
	 * @return Style element matching !normal 
	 */
	public Element getNormalMatch()
	{
		return eNormal;
	}
	
	/**
	 * Obtains a given value element within the !normal match.
	 * @param name Name
	 * @param create If true, creates it when it doesn't exist
	 * @return Value element
	 */
	public Element findValue(String name,boolean create)
	{
		Element[] aeValues=XML.getChildren(eNormal,"value");
		for(int i=0;i<aeValues.length;i++)
		{
			if(name.equals(aeValues[i].getAttribute("name")))
			  return aeValues[i];
		}
		if(create)
		{
			Element e=d.createElement("value");
			e.setAttribute("name",name);
			eNormal.appendChild(e);
			return e;
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * @param name Property name
	 * @return Value or null if none
	 */
	public String getStringValue(String name)
	{
		Element e=findValue(name,false);
		if(e==null) return null;
		return e.getAttribute("string");
	}
	
	/**
	 * Sets a string value.
	 * @param name Property name
	 * @param value New value
	 */
	public void setStringValue(String name, String value)
	{
		findValue(name,true).setAttribute("string",value);
	}
	
	/**
	 * @param name Property name
	 * @return Value or null if none
	 */
	public String getNumberValue(String name)
	{
		Element e=findValue(name,false);
		if(e==null) return null;
		return e.getAttribute("number");
	}
	
	/**
	 * Set a number value.
	 * @param name Property name
	 * @param number New number value
	 */
	public void setNumberValue(String name, String number)
	{
		findValue(name,true).setAttribute("number",number);
	}
	
	/**
	 * Deletes a value.
	 * @param name Property name
	 */
	public void deleteValue(String name)
	{
		Element e=findValue(name,false);
		if(e!=null) e.getParentNode().removeChild(e);
	}
	
	/**
	 * Sets a colour value in stylesheet.
	 * @param id Colour id
	 * @param c New colour
	 */
	public void setColour(String id, Color c)
	{
		deleteColour(id);
		
		Element eColour=d.createElement("colour");
		eColour.setAttribute("id",id);
		eColour.setAttribute("rgb",c.getRed()+","+c.getGreen()+","+c.getBlue());
		d.getDocumentElement().appendChild(eColour);
	}
	
	/**
	 * Removes a colour value from stylesheet.
	 * @param id Colour id
	 */
	public void deleteColour(String id)
	{
		Element[] aeColours=XML.getChildren(d.getDocumentElement(),"colour");
		for(int i=0;i<aeColours.length;i++)
		{
			String s=(String)aeColours[i].getAttribute("id");
			if(s!=null && s.equals(id))
			{
				aeColours[i].getParentNode().removeChild(aeColours[i]);
			}
		}
	}
	
}
