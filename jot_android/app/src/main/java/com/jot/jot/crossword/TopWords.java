/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Place for storing a collection of top scoring words.
 */

package com.jot.jot.crossword;

//**************************************************************************************************

public class TopWords
{
	static final int N_TOPWORDS = 10;
	public TopWord[] m_aTopWords;
	public int m_nWords = 0;

	//**********************************************************************************************
	public
	TopWords(int nBoardSize)
	{
		m_aTopWords = new TopWord[N_TOPWORDS];
		for (int i = 0; i < N_TOPWORDS; ++i)
		{
			m_aTopWords[i] = new TopWord(nBoardSize);
		}
	}

	//**********************************************************************************************
	public TopWord
	getTopWord(int iWord)
	{
		if (iWord < m_nWords)
		{
			return m_aTopWords[iWord];
		}
		return null;
	}

	//**********************************************************************************************
	public boolean
	isStart(Square square)
	{
		for (int i = 0; i < m_nWords; ++i)
		{
			if (m_aTopWords[i].isStart(square))
			{
				return true;
			}
		}
		return false;
	}
}

//**************************************************************************************************
