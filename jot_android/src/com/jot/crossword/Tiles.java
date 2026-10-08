/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * A letter Tile to play on a crossword Board.
 */

package com.jot.crossword;

import java.util.ArrayList;

//**************************************************************************************************

public class Tiles extends ArrayList<Tile>
{
	private Bitmap m_bitmap;
	private int m_nMaxTiles;

	//**********************************************************************************************
	public
	Tiles(int nMaxTiles)
	{
		super();
		m_nMaxTiles = nMaxTiles;
	}

	//**********************************************************************************************
	public boolean
	add(Tile tile)
	{
		// Reset the tile's state.
		if (m_nMaxTiles == 0)
		{
			tile.setState(Tile.State.BAGGED);
		}
		else
		{
			tile.setState(Tile.State.RACKED);
		}

		// If wild, reset its letter to wild.
		if (tile.getScore() == 0)
		{
			tile.setLetter(Tile.WILD);
		}
		return super.add(tile);
	}

	//**********************************************************************************************
	public Bitmap
	getBitmap() throws Exception
	{
		// If needed, create the initial bitmap image.
		if (m_bitmap == null)
		{
			if (size() == 0)
			{
				return null;
			}
			Tile tile = get(0);
			if (tile == null)
			{
				throw new Exception("No initial tiles for rack image.");
			}

			// Get the dimensions of a tile.
			Bitmap bmTile = tile.getBitmap();
			if (bmTile == null)
			{
				return null;
			}
			int width = bmTile.getWidth();
			int height = bmTile.getHeight();

			// Create our rack bitmap image.
			m_bitmap = Bitmap.createBitMap(
				bmTile.getWidth() * size(),
				bmTile.getHeight(),
				Bitmap.Config.ARGB_8888);
		}

		// Fill in each tile.
		int x = 0;
		for (int i = 0; i < size(); ++i)
		{
			Bitmap bmTile = get(i).getBitmap();
			m_bitmap.drawBitmap(bmTile, x, 0, null);
			x += bmTile.getWidth();
		}

		// If we are missing tiles, clear the remaining part of the rack.
		if (x < m_bitmap.getWidth())
		{
			m_bitmap.clear(x, 0, m_bitmap.getWidth(), m_bitmap.getHeight());
		}

		return m_bitmap;
	}

	//**********************************************************************************************
	public int
	getMaxTiles()
	{
		return m_nMaxTiles;
	}

	//**********************************************************************************************
	public int
	getVowelCount()
	{
		int nVowels = 0;
		for (Tile tile : this)
		{
			if (tile.isVowel())
			{
				++nVowels;
			}
		}
		return nVowels;
	}

	//**********************************************************************************************
	public Tile
	grabTile(char cTile)
	{
		for (Tile tile : this)
		{
			if (cTile == tile.getLetter())
			{
				remove(tile);
				return tile;
			}
		}
		return null;
	}

	//**********************************************************************************************
	public boolean
	grab(Tiles tiles)
	{
		if (size() > 0 &&
			tiles.size() < tiles.m_nMaxTiles)
		{
			int iTile = (int)(size() * Math.random());
			tiles.add(remove(iTile));
			return true;
		}
		
		return false;
	}

	//**********************************************************************************************
	public int
	indexOf(char cTile)
	{
		for (int i = 0; i < size(); ++i)
		{
			Tile tile = get(i);
			if (tile.getLetter() == cTile)
			{
				return i;
			}
		}
		return -1;
	}

	//**********************************************************************************************
	public boolean
	isFull()
	{
		return size() == m_nMaxTiles;
	}

	//**********************************************************************************************
	public void
	jumble()
	{
		Tiles tiles = new Tiles(this.m_nMaxTiles);

		while (size() > 0)
		{
			tiles.add(remove((int)(size() * Math.random())));
		}
		while (tiles.size() > 0)
		{
			add(tiles.remove(0));
		}
	}

	//**********************************************************************************************
	public boolean
	moveTileLeft(Tile tile)
	{
		int iTile = indexOf(tile);
		if (iTile < 0)
		{
			return false;
		}
		remove(iTile);
		if (iTile-- == 0)
		{
			iTile = size() - 1;
		}
		add(iTile, tile);
		return true;
	}

	//**********************************************************************************************
	public boolean
	moveTileRight(Tile tile)
	{
		int iTile = indexOf(tile);
		if (iTile < 0)
		{
			return false;
		}
		remove(iTile);
		if (++iTile >= size())
		{
			iTile = 0;
		}
		add(iTile, tile);
		return true;
	}
}

//**************************************************************************************************
