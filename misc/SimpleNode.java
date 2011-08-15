/* Generated By:JJTree: Do not edit this line. SimpleNode.java */
package textlayout.grammar;

import java.util.*;

public class SimpleNode implements Node {
  protected SimpleNode parent;
  protected SimpleNode[] children;
  protected int id;
  protected CSSGrammar parser;

  public SimpleNode(int i) {
    id = i;
  }

  public SimpleNode(CSSGrammar p, int i) {
    this(i);
    parser = p;
  }

  public void jjtOpen() {
  }

  public void jjtClose() {
  }
  
  public void jjtSetParent(Node n) { parent = (SimpleNode)n; }
  public Node jjtGetParent() { return parent; }

  public void jjtAddChild(Node n, int i) {
    if (children == null) {
      children = new SimpleNode[i + 1];
    } else if (i >= children.length) {
      SimpleNode c[] = new SimpleNode[i + 1];
      System.arraycopy(children, 0, c, 0, children.length);
      children = c;
    }
    children[i] = (SimpleNode)n;
  }

  public Node jjtGetChild(int i) {
    return children[i];
  }

  public int jjtGetNumChildren() {
    return (children == null) ? 0 : children.length;
  }

  /* You can override these two methods in subclasses of SimpleNode to
     customize the way the node appears when the tree is dumped.  If
     your output uses more than one line you should override
     toString(String), otherwise overriding toString() is probably all
     you need to do. */

  public String toString() 
  { 
  		return CSSGrammarTreeConstants.jjtNodeName[id]+
  			(sName==null ? "" : " ["+sName+ 
  				(sType==null || sValue==null ? "" : sType+sValue) + "]"); 
  	}
  public String toString(String prefix) { return prefix + toString(); }

  /* Override this method if you want to customize how the node dumps
     out its children. */

  public void dump(String prefix) {
    System.out.println(toString(prefix));
    if (children != null) {
      for (int i = 0; i < children.length; ++i) {
	SimpleNode n = (SimpleNode)children[i];
	if (n != null) {
	  n.dump(prefix + " ");
	}
      }
    }
  }
  
  // sam bits
  private String sName=null,sType=null,sValue=null;
  private int line=-1,column=-1;
  public void setName(String sName) 
  {
  		this.sName=sName;
  }
  public String getName()
  {
  		return sName;
  }
  public void setType(String sType)
  {
  		this.sType=sType;
  }
  public String getValue()
  {
  		return sValue;
  }
  public void setValue(String sValue)
  {
  		this.sValue=sValue;
  }
  public String getType()
  {
  		return sType;
  }
  public String getRule()
  {
  		return CSSGrammarTreeConstants.jjtNodeName[id];
  }
  public void setPos(int line,int column)
  {
  		this.line=line;
  		this.column=column;
  }
  public int getLine() 
  { 
  		if(line==-1) return parent.getLine();
  		return line; 	
  	}
  public int getColumn() 
  { 
  		if(column==-1) return parent.getLine();
  		return column; 	
  	}
  public SimpleNode[] getChildren()
  {
  		return children;
  }
  public SimpleNode[] getChildren(Set sRules)
  {
  		LinkedList l=new LinkedList();
  		getChildren(sRules,l);
  		return (SimpleNode[]) l.toArray(new SimpleNode[l.size()]);
  }
  private void getChildren(Set sRules,List l)
  {
  		if(sRules.contains(getRule()))
  			l.add(this);
  		if(children==null) return;
  		for(int i=0;i<children.length;i++)
  		{
  			children[i].getChildren(sRules,l);
  		}
  }
  
}
