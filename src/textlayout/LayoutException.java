package textlayout;

/**
 * Any exceptions encountered during layout handling.
 */
public class LayoutException extends Exception
{
	/**
	 * Default constructor.
	 */
	public LayoutException()
	{
		super();
	}

	/**
	 * @param message Message
	 * @param t Error
	 */
	public LayoutException(String message,Throwable t)
	{
		super(message,t);
	}

	/**
	 * @param message Message
	 */
	public LayoutException(String message)
	{
		super(message);
	}

	/**
	 * @param t Error
	 */
	public LayoutException(Throwable t)
	{
		super(t);
	}
}
