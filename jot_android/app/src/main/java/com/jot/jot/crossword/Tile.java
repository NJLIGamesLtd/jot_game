/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * A letter Tile to play on a crossword Board.
 */

package com.jot.jot.crossword;

import java.util.ArrayList;

//**************************************************************************************************

public class Tile
{
	public enum State
	{
		BAGGED,
		RACKED,
		PLACED,
		PLAYED,
		CROSSED,
		SCORED,
		CONFIRMED;

		public boolean
		atLeast(State eState)
		{
			return ordinal() >= eState.ordinal();
		}
	}

	public static final int ENCODING_SIZE = 2;

	public static final char WILD = '@';
	public static final char ALPHA = 'A';
	static final char WILD_MASK = 0x20;

	public static final char FLAG_PLAYER	= 0x03;
	public static final char FLAG_PLACED	= 0x04;
	public static final char FLAG_PLAYED	= 0x08;
	public static final char FLAG_WILDCARD	= 0x10;
	public static final char FLAG_SCORED	= 0x20;
	public static final char FLAG_CONFIRMED	= 0x40;
	public static final char FLAG_MASK		= 0x80;

	private char m_cLetter;
	private int m_nScore;
	private State m_eState;
	private Square m_square;
	private boolean m_bFound;

	private final int m_iPlayer;

	//**********************************************************************************************
	public static String
	GetEncoding(Tile tile)
	{
		char[] aLabel = { FLAG_MASK, ' ' };
		if (tile != null)
		{
			// Generate and stores the flags defined the state of our tile.
			char flags = (char)((int)FLAG_MASK + tile.m_iPlayer);

			// Mark if it's a wildcard.
			if (tile.m_nScore == 0)
			{
				flags |= FLAG_WILDCARD;
			}

			// Mark if it's placed, scored or played.
			switch (tile.m_eState)
			{
				case PLACED:
					flags |= FLAG_PLACED;
					break;

				case PLAYED:
					flags |= FLAG_PLAYED;
					break;

				case SCORED:
					flags |= FLAG_SCORED;
					break;

				case CONFIRMED:
					flags |= FLAG_CONFIRMED;
					break;

				default:
					break;
			}
			aLabel[0] = flags;
			
			// Store the tile character in the second slot.
			aLabel[1] = tile.m_cLetter;
		}
		return new String(aLabel);
	}

	//**********************************************************************************************
	public static boolean
	IsEncodedWildcard(char cFlags)
	{
		return (cFlags & FLAG_WILDCARD) != 0;
	}

	//**********************************************************************************************
	public static int
	GetEncodedPlayer(char cFlags)
	{
		return cFlags & FLAG_PLAYER;
	}

	//**********************************************************************************************
	public
	Tile(
		char cLetter,
		int nScore,
		int iPlayer)
	{
		m_nScore = nScore;
		m_eState = State.BAGGED;
		m_square = null;
		m_iPlayer = iPlayer;

		setLetter(cLetter);
	}

	//**********************************************************************************************
	public
	Tile(Tile tile)
	{
		m_nScore = tile.m_nScore;
		m_eState = tile.m_eState;
		m_square = tile.m_square;
		m_iPlayer = tile.m_iPlayer;
		m_cLetter = tile.m_cLetter;
	}

	//**********************************************************************************************
	public char
	getLetter()
	{
		return m_cLetter;
	}

	//**********************************************************************************************
	public int
	getPlayer()
	{
		return m_iPlayer;
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
	isEquals(Tile tile)
	{
		if (tile != null)
		{
			if (m_nScore == tile.m_nScore)
			{
				// If we have the same score and letter, the tiles match.
				if (m_cLetter == tile.m_cLetter)
				{
					return true;
				}
				// If we have a blank, then they can also match.
				if (m_nScore == 0)
				{
					return true;
				}
			}
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isFound()
	{
		return m_bFound;
	}

	//**********************************************************************************************
	public boolean
	isStateAtLeast(State eState)
	{
		return m_eState.atLeast(eState);
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
				if (m_nScore == 0)
				{
					return true;
				}
				return false;
		}
	}

	//**********************************************************************************************
	public void
	markFound(boolean bFound)
	{
		m_bFound = bFound;
	}

	//**********************************************************************************************
	public void
	setLetter(char cLetter)
	{
		m_cLetter = cLetter;
	}

	//**********************************************************************************************
	public void
	setScore(char nScore)
	{
		m_nScore = nScore;
	}

	//**********************************************************************************************
	public void
	setSquare(
		Square square,
		boolean bPlaced)
	{
		if ((m_square = square) != null &&
			bPlaced &&
			!isStateAtLeast(State.CROSSED))
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
		char[] aLabel = { '[', ' ', ']' };
		aLabel[1] = m_cLetter;

		switch (m_eState)
		{
			case BAGGED:
			case RACKED:
				return new String(aLabel, 1, 1);

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
				aLabel[0] = '<';
				aLabel[2] = '>';
				break;

			case CONFIRMED:
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
