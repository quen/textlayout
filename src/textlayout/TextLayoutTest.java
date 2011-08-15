package textlayout;

import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.JFrame;

import org.w3c.dom.*;

import textlayout.stylesheet.*;
import util.PlatformUtils;
import util.xml.XML;

/**
 * Test class for checking layout without using another application.
 */
public class TextLayoutTest extends JFrame
{
	private ScrollingLayout scroll;

	TextLayoutTest() throws Exception
	{
		super("Test");
		PlatformUtils.setUserFolder("textlayout");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		StaticLayout sl=new StaticLayout(
/*			"<head>Heading</head><para>Here's a bit of text that should run over into " +
			"two lines at <em>least</em>. I mean, it's pretty long.</para>" +
			"<line>Line 1</line><line>Line 2</line><line>A <mono>monospaced bit</mono> " +
			"and then a veryverylongwordwhichwon'tfitonthelineandneedstobesplitintoseveral</line>" +
			"<line>anotherveryverylongwordthatstartsatthebeginningofthelinethistimejust" +
			"toseeifthatworksandohhowwelaughedisn'tthisfunyeahtherearenobreaksandmore" +
			"linesthistimewhichshouldtakeuplotsofspace</line>" +
			"<line>Test an <unknown>unknown inline tag</unknown></line>" +*/
			"<unknown><line>And an unknown block tag</line></unknown>",300);
/*		StaticLayout sl=new StaticLayout(
			"<line>Test an <unknown>unknown inline tag</unknown></line>",300);*/
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(sl,BorderLayout.NORTH);

		StyleContext sc=new StyleContext(StyleContext.getDefault(true));
		sc.addStylesheet(new Stylesheet(
			"output > line { gap-left:33; }" +
			"line > timestamp { 	type:margin-block; margin-width:33; 	font-size:0.8f; match-baseline:1.25f; outline:#ff0; }" +
			"line > timestamp2 { 	type:margin-block; margin-width:33; 	font-size:0.8f; match-baseline:1.25f; outline:#ff0 50%; }" +
			"stupid {font-size:0.8f;} @rgb bg 'sfasf' #800;"));
		scroll=new ScrollingLayout(300,250,sc);
		getContentPane().add(scroll,BorderLayout.CENTER);
		for(int i=0;i<100;i++)
		{
			//addText("<line><mono>Line number</mono> <em>"+i+"</em></line>");
		}
		addText(sc,
			"<line>Lined <stupid>small type</stupid> before</line>" +
			"<line><timestamp>08:30</timestamp>Rest of line</line>" +
			"<line><timestamp2>08:30</timestamp2>Rest of line</line>" +
			"<line>Block after</line>");
		/*
		addText(sc,"<line>This is some <mono id='frog'>mono text</mono> right here</line>");
		addText(sc,"<line blue='red'>" +
				"<line id='tadpole'>Text</line>" +
				"<line>Some <mono>mono text</mono> and a url <url>http://www.google.com/</url> here</line>" +
				"<line><margin>Margin text</margin>Rest of line</line>"+
				"</line>");
				*/

		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	private void addText(StyleContext sc,String xml)
	{
		try
		{
			LayoutBlock[] alb=LayoutGenerator.getLayout(sc,XML.parse(
				"<output>"+xml+"</output>"
				).getDocumentElement());
			System.err.println(alb[0].debugDisplay(""));
			Document d=XML.newDocument();
			alb[0].buildXML(d,new HashMap<LayoutInline, LayoutInline.NodePos>());
			debugOutput(d.getDocumentElement());
			for(int i=0;i<alb.length;i++)
				scroll.addBlock(alb[i]);
		}
		catch(Exception le)
		{
			throw new Error(le);
		}
	}
	
	/**
	 * @param eNewParent
	 */
	public static void debugOutput(Element eNewParent)
	{
		try
		{
			Document d=XML.newDocument("test");
			d.getDocumentElement().appendChild(d.importNode(eNewParent,true));
			System.err.println(XML.saveString(d).replaceAll("(^(.|[\\n\\r])*<test>)|(</test>$)",""));
		}
		catch(Exception e) {}
	}

	/**
	 * Main method for testing only.
	 * @param args Arguments (ignored)
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		new TextLayoutTest();
	}
	

}
