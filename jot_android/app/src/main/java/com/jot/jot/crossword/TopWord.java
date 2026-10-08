/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Place for saving a top scoring word.
 */

package com.jot.jot.crossword;

//**************************************************************************************************

public class TopWord
{
	public Placement m_placement;
	public char[] m_aWord;
	public int m_nLength;
	public int m_nScore;

	//**********************************************************************************************
	public
	TopWord(int nBoardSize)
	{
		m_placement = new Placement();
		m_aWord = new char[nBoardSize + 1];
	}

	//**********************************************************************************************
	public char
	charAt(int iChar)
	{
		if (iChar < m_nLength)
		{
			return m_aWord[iChar];
		}
		return 0;
	}

	//**********************************************************************************************
	public int
	getLength()
	{
		return m_nLength;
	}

	//**********************************************************************************************
	public Placement
	getPlacement()
	{
		return m_placement;
	}

	//**********************************************************************************************
	public boolean
	isEqual(char[] aWord)
	{
		char cWord;
		for (int i = 0; (cWord = m_aWord[i]) != 0; ++i)
		{
			if (cWord != aWord[i])
			{
				return false;
			}
		}
		return true;
	}

	//**********************************************************************************************
	public boolean
	isStart(Square square)
	{
		char cLetter = square.getLetter();
		return
			cLetter == m_aWord[0] &&
			square.getRow() == m_placement.m_yFirst &&
			square.getColumn() == m_placement.m_xFirst - 1;
	}

	//**********************************************************************************************
	public void
	set(
		char[] aWord,
		int nScore,
		int xFirst,
		int yFirst,
		int xLast,
		int yLast)
	{
		m_nLength = 0;
		while ((m_aWord[m_nLength] = aWord[m_nLength]) != 0)
		{
			++m_nLength;
		}
		m_nScore = nScore;
		m_placement.m_xFirst = xFirst;
		m_placement.m_yFirst = yFirst;
		m_placement.m_xLast = xLast;
		m_placement.m_yLast = yLast;
	}
}

//**************************************************************************************************
