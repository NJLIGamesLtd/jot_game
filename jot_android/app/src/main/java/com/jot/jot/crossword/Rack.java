/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.crossword;

//**************************************************************************************************

public class Rack
{
	private final Square[] m_aSquares;
	private final int m_size;

	//**********************************************************************************************
	public
	Rack(int nMaxTiles)
	{
		m_size = nMaxTiles;
		m_aSquares = new Square[m_size];

		for (int i = 0; i < m_size; ++i)
		{
			m_aSquares[i] = new Square(i);
		}
	}

	//**********************************************************************************************
	public
	Rack(Rack rack)
	{
		m_size = rack.m_size;
		m_aSquares = new Square[m_size];

		for (int i = 0; i < m_size; ++i)
		{
			m_aSquares[i] = new Square(rack.m_aSquares[i]);
		}
	}

	//**********************************************************************************************
	public boolean
	add(Tile tile)
	{
		for (int i = 0; i < m_size; ++i)
		{
			if (m_aSquares[i].getTile() == null)
			{
				m_aSquares[i].setTile(tile, false);
				tile.setState(Tile.State.RACKED);
				
				// If we're returning a blank to our rack, reset its letter to WILD.
				if (tile.getScore() == 0)
				{
					tile.setLetter(Tile.WILD);
				}
				return true;
			}
		}
		return false;
	}

	//**********************************************************************************************
	public void
	copyTiles(Rack rack)
	{
		for (int i = 0; i < m_size; ++i)
		{
			Tile tile = rack.m_aSquares[i].m_tile;
			if (tile != null)
			{
				tile = new Tile(tile);
			}
			m_aSquares[i].m_tile = tile;
		}
	}

	//**********************************************************************************************
	public Tile
	get(int iTile)
	{
		if (iTile >= 0 && iTile < m_size)
		{
			return m_aSquares[iTile].getTile();
		}
		return null;
	}

	//**********************************************************************************************
	public String
	getEncoding()
	{
		StringBuilder sbRack = new StringBuilder();
		for (int iTile = 0; iTile < m_size; ++iTile)
		{
			Tile tile = get(iTile);
			if (tile != null)
			{
				sbRack.append(tile.getLetter());
			}
		}
		return sbRack.toString();
	}

	//**********************************************************************************************
	public char
	getLetter(int iTile)
	{
		Tile tile = get(iTile);
		if (tile != null)
		{
			return tile.getLetter();
		}
		return 0;
	}

	//**********************************************************************************************
	public int
	getPoints()
	{
		int nPoints = 0;
		for (int i = 0; i < m_size; ++i)
		{
			Tile tile = m_aSquares[i].getTile();
			if (tile != null)
			{
				nPoints += tile.getScore();
			}
		}
		return nPoints;
	}

	//**********************************************************************************************
	public int
	getRackSize()
	{
		return m_size;
	}

	//**********************************************************************************************
	public Square
	getSquare(int iSquare)
	{
		if (iSquare < m_size)
		{
			return m_aSquares[iSquare];
		}
		return null;
	}

	//**********************************************************************************************
	public int
	getTileCount()
	{
		int nTiles = 0;
		for (int i = 0; i < m_size; ++i)
		{
			if (m_aSquares[i].getTile() != null)
			{
				++nTiles;
			}
		}

		return nTiles;
	}

	//**********************************************************************************************
	public int
	getVowelCount()
	{
		int nVowels = 0;
		for (int i = 0; i < m_size; ++i)
		{
			Tile tile = m_aSquares[i].getTile();
			if (tile != null)
			{
				if (tile.isVowel())
				{
					++nVowels;
				}
			}
		}
		return nVowels;
	}

	//**********************************************************************************************
	public Tile
	grabTile(char cTile)
	{
		for (int i = 0; i < m_size; ++i)
		{
			Tile tile = m_aSquares[i].getTile();
			if (tile != null && tile.getLetter() == cTile)
			{
				remove(tile);
				return tile;
			}
		}
		return null;
	}

	//**********************************************************************************************
	public int
	indexOf(Tile tile)
	{
		for (int i = 0; i < m_size; ++i)
		{
			if (m_aSquares[i].getTile() == tile)
			{
				return i;
			}
		}
		return -1;
	}

	//**********************************************************************************************
	public int
	indexOf(char cTile)
	{
		for (int i = 0; i < m_size; ++i)
		{
			Tile tile = m_aSquares[i].getTile();
			if (tile != null && tile.getLetter() == cTile)
			{
				return i;
			}
		}
		return -1;
	}

	//**********************************************************************************************
	public boolean
	isEmpty()
	{
		for (int i = 0; i < m_size; ++i)
		{
			if (m_aSquares[i].getTile() != null)
			{
				return false;
			}
		}
		return true;
	}

	//**********************************************************************************************
	public boolean
	isFull()
	{
		for (int i = 0; i < m_size; ++i)
		{
			if (m_aSquares[i].getTile() == null)
			{
				return false;
			}
		}
		return true;
	}

	//**********************************************************************************************
	public void
	jumble()
	{
		Tiles tiles = new Tiles();

		int nTiles = 0;
		for (int i = 0; i < m_size; ++i)
		{
			Tile tile = m_aSquares[i].getTile();
			if (tile != null)
			{
				m_aSquares[i].setTile(null, false);
				tiles.add(tile);
				++nTiles;
			}
		}

		while (nTiles-- > 0)
		{
			m_aSquares[nTiles].setTile(
				tiles.remove((int)((nTiles + 1) * Math.random())),
				false);
		}
	}

	//**********************************************************************************************
	public Tile
	remove(int iTile)
	{
		Tile tile = m_aSquares[iTile].getTile();
		m_aSquares[iTile].setTile(null, false);

		return tile;
	}

	//**********************************************************************************************
	public boolean
	remove(Tile tile)
	{
		for (int i = 0; i < m_size; ++i)
		{
			if (m_aSquares[i].getTile() == tile)
			{
				remove(i);
				return true;
			}
		}
		return false;
	}

	//**********************************************************************************************
	// Shift tiles over to fit this one.
	public boolean
	shiftToFit(
		Tile tile,
		Square square
	) throws Exception
	{
		// Get the location of destination square in our rack.
		int iEmpty = -1;
		int iSquare = -1;
		for (int i = 0; i < m_size; ++i)
		{
			if (m_aSquares[i] == square)
			{
				iSquare = i;
			}
			else if (iEmpty < 0 && m_aSquares[i].getTile() == null)
			{
				iEmpty = i;
			}
		}

		// There should be an empty square to push tiles towards.
		if (iEmpty < 0)
		{
			throw new Exception("No empty square on rack.");
		}

		// If the square is on the board, just return the tile to an empty spot.
		if (iSquare < 0)
		{
			iSquare = iEmpty;
		}

		// If we're returning a blank to our rack, reset its letter to WILD.
		if (tile.getScore() == 0)
		{
			tile.setLetter(Tile.WILD);
		}

		// Push towards the empty square.
		if (iEmpty < iSquare)
		{
			do
			{
				Tile tileMove = m_aSquares[iEmpty + 1].getTile();
				m_aSquares[iEmpty + 1].setTile(null, false);
				m_aSquares[iEmpty].setTile(tileMove, false);
			} while (++iEmpty < iSquare);
		}
		else if (iEmpty > iSquare)
		{
			do
			{
				Tile tileMove = m_aSquares[iEmpty - 1].getTile();
				m_aSquares[iEmpty - 1].setTile(null, false);
				m_aSquares[iEmpty].setTile(tileMove, false);
			} while (--iEmpty > iSquare);
		}

		// Now we can add the tile to this square.
		m_aSquares[iSquare].setTile(tile, false);
		return true;
	}
}

//**************************************************************************************************
