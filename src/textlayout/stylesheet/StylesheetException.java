package textlayout.stylesheet;

import textlayout.LayoutException;

/** Errors that occur due to invalid stylesheet data */
public class StylesheetException extends LayoutException
{
	StylesheetException(int lineNumber,String message)
	{
		super(lineNumber+":"+message);
	}
}
