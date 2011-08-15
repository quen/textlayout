package textlayout;


/** Exception probably indicates a bug in the system */
public class BugException extends LayoutException
{
	/**
	 * 
	 */
	public BugException()
	{
		super();
	}

	/**
	 * @param message
	 */
	public BugException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public BugException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public BugException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
