/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * A letter Tile to play on a crossword Board.
 */

package com.jot.crossword;

//**************************************************************************************************

public class Square
{
	static final char EMPTY = ' ';
	static final char NEED = '@';

	private Tile m_tile;
	private Bonus m_eBonus;
	private int m_iRow;
	private int m_iColumn;
	private Bitmap m_bitmap;

	//**********************************************************************************************
	public
	Square(
		Bonus eBonus,
		int iRow,
		int iColumn,
		Bitmap bitmap)
	{
		m_eBonus = eBonus;
		m_iRow = iRow;
		m_iColumn = iColumn;
		m_bitmap = bitmap;
		m_tile = null;
	}

	//**********************************************************************************************
	public
	Square(
		byte iBonus,
		int iRow,
		int iColumn,
		Bitmap bitmap)
	{
		this(Bonus.values()[iBonus], iRow, iColumn, bitmap);
	}

	//**********************************************************************************************
	public Bitmap
	getBitmap()
	{
		if (m_tile != null)
		{
			return m_tile.getBitmap();
		}
		return m_bitmap;
	}

	//**********************************************************************************************
	public Bonus
	getBonus()
	{
		return m_eBonus;
	}

	//**********************************************************************************************
	public char
	getLetter()
	{
		if (m_tile != null)
		{
			m_tile.getLetter();
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
	setBonus(Bonus eBonus)
	{
		m_eBonus = eBonus;
	}

	//**********************************************************************************************
	public void
	setTile(Tile tile)
	{
		if (tile == null)
		{
			if (m_tile != null)
			{
				m_tile.setSquare(null);
			}
		}
		else
		{
			tile.setSquare(this);
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
		return m_eBonus.toString();
	}
}

//**************************************************************************************************
