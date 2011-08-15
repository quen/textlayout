package textlayout.stylesheet;

abstract class PropertyData
{
	/**
	 * @return True if this value is the special 'inherit' one
	 */
	public boolean inherit()
	{
		return false;
	}
}