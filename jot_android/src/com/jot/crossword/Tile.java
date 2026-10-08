/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * A letter Tile to play on a crossword Board.
 */

package com.jot.crossword;

//import android.util.*;

//**************************************************************************************************

public class Tile //extends ShapeDrawable
{
	public enum State
	{
		BAGGED,
		RACKED,
		MOVED,
		PLACED,
		PLAYED,
		CROSSED,
		SCORED;

		public boolean
		atLeast(State eState)
		{
			return ordinal() >= eState.ordinal();
		}
	}
	
	static final char WILD = '@';
	static final char ALPHA = 'A';
	static final char WILD_MASK = 0x20;

	private char m_cLetter;
	private int m_nScore;
	private State m_eState;
	private Square m_square;
	private Bitmap m_bitmap;

	//**********************************************************************************************
	public
	Tile(
		char cLetter,
		int nScore,
		Bitmap bitmap)
	{
		m_cLetter = cLetter;
		m_nScore = nScore;
		m_bitmap = bitmap;
		m_eState = State.BAGGED;
		m_square = null;
	}

	//**********************************************************************************************
	public Bitmap
	getBitmap()
	{
		return m_bitmap;
	}

	//**********************************************************************************************
	public char
	getLetter()
	{
		return m_cLetter;
	}

	//**********************************************************************************************
	public int
	getScore()
	{
		return m_nScore;
	}

	//**********************************************************************************************
	public Square
	getSquare()
	{
		return m_square;
	}

	//**********************************************************************************************
	public State
	getState()
	{
		return m_eState;
	}

	//**********************************************************************************************
	public boolean
	isVowel()
	{
		switch (m_cLetter)
		{
			case WILD:
			case 'A':
			case 'E':
			case 'I':
			case 'O':
			case 'U':
				return true;
			default:
				return false;
		}
	}

	//**********************************************************************************************
	public void
	setLetter(char cLetter)
	{
		m_cLetter = cLetter;
	}

	//**********************************************************************************************
	public void
	setScore(byte nScore)
	{
		m_nScore = nScore;
	}

	//**********************************************************************************************
	public void
	setSquare(Square square)
	{
		if ((m_square = square) != null)
		{
			setState(Tile.State.PLACED);
		}
	}

	//**********************************************************************************************
	public void
	setState(State eState)
	{
		m_eState = eState;
	}

	//**********************************************************************************************
	public String
	toString()
	{
		char aLabel[] = { '[', ' ', ']' };
		aLabel[1] = m_cLetter;

		switch (m_eState)
		{
			case BAGGED:
			case RACKED:
				return new String(aLabel, 1, 1);

			case MOVED:
				aLabel[0] = '<';
				aLabel[2] = '>';
				break;

			case PLACED:
				aLabel[0] = '{';
				aLabel[2] = '}';
				break;

			case PLAYED:
				aLabel[0] = '|';
				aLabel[2] = '|';
				break;

			case CROSSED:
			case SCORED:
			default:
				if (m_nScore == 0)
				{
					aLabel[0] = '(';
					aLabel[2] = ')';
				}
				break;
		}
		return new String(aLabel);
	}
}

//**************************************************************************************************
