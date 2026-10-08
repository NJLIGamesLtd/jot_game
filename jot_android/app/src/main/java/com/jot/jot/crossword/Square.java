/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * A Square is a place holder for a Tile either on the Board or Rack.
 */

package com.jot.jot.crossword;

//**************************************************************************************************

public class Square
{
	protected Tile m_tile;
	private Bonus m_bonus;
	private final int m_iRow;
	private final int m_iColumn;

	//**********************************************************************************************
	public
	Square(
		Bonus bonus,
		int iRow,
		int iColumn)
	{
		m_bonus = bonus;
		m_iRow = iRow;
		m_iColumn = iColumn;
		m_tile = null;
	}

	//**********************************************************************************************
	public
	Square(
		byte iBonus,
		int iRow,
		int iColumn)
	{
		this(
			Bonus.values()[iBonus],
			iRow,
			iColumn);
	}

	//**********************************************************************************************
	public
	Square(int iSlot)
	{
		this(
			(byte)0,
			-1,
			iSlot);
	}

	//**********************************************************************************************
	public
	Square(Square square)
	{
		m_tile = square.m_tile;
		m_bonus = square.m_bonus;
		m_iRow = square.m_iRow;
		m_iColumn = square.m_iColumn;
	}

	//**********************************************************************************************
	public Bonus
	getBonus()
	{
		return m_bonus;
	}

	//**********************************************************************************************
	public char
	getLetter()
	{
		if (m_tile != null)
		{
			return m_tile.getLetter();
		}
		return 0;
	}

	//**********************************************************************************************
	public int
	getRow()
	{
		return m_iRow;
	}

	//**********************************************************************************************
	public int
	getColumn()
	{
		return m_iColumn;
	}

	//**********************************************************************************************
	public Tile
	getTile()
	{
		return m_tile;
	}

	//**********************************************************************************************
	public void
	setBonus(Bonus bonus)
	{
		m_bonus = bonus;
	}

	//**********************************************************************************************
	public void
	setTile(
		Tile tile,
		boolean bPlaced)
	{
		// If null, clear this square of any tile.
		if (tile == null)
		{
			// If there was a tile here, remove it.
			if (m_tile != null)
			{
				m_tile.setSquare(null, false);
			}
		}
		else
		{
			// Place our tile.
			tile.setSquare(this, bPlaced);
		}
		m_tile = tile;
	}

	//**********************************************************************************************
	public String
	toString()
	{
		if (m_tile != null)
		{
			return m_tile.toString();
		}
		return m_bonus.toString();
	}
}

//**************************************************************************************************
