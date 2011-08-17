/*
This file is part of leafdigital textlayout.

textlayout is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

textlayout is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with textlayout. If not, see <http://www.gnu.org/licenses/>.

Copyright 2011 Samuel Marshall.
*/
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
