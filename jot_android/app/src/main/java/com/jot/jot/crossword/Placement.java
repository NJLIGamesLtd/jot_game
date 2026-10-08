/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores a placement coordinates of a word on the game board.
 */

package com.jot.jot.crossword;

//**************************************************************************************************

public class Placement
{
	public int m_xFirst;
	public int m_yFirst;
	public int m_xLast;
	public int m_yLast;

	public
	Placement()
	{
		m_xFirst = -1;
	}

	public
	Placement(
		int xFirst,
		int yFirst,
		int xLast,
		int yLast)
	{
		m_xFirst = xFirst;
		m_yFirst = yFirst;
		m_xLast = xLast;
		m_yLast = yLast;
	}

	public
	Placement(Square square)
	{
		m_xFirst = m_xLast = square.getColumn();
		m_yFirst = m_yLast = square.getRow();
	}

	public void
	setPlacement(Square square)
	{
		m_xFirst = m_xLast = square.getColumn();
		m_yFirst = m_yLast = square.getRow();
	}
}

//**************************************************************************************************
