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

public class Tiles extends ArrayList<Tile>
{
	//**********************************************************************************************
	public
	Tiles()
	{
		super();
	}

	//**********************************************************************************************
	public boolean
	add(Tile tile)
	{
		// Reset the tile's state.
		tile.setState(Tile.State.BAGGED);

		// If wild, reset its letter to wild.
		if (tile.getScore() == 0)
		{
			tile.setLetter(Tile.WILD);
		}
		return super.add(tile);
	}

	//**********************************************************************************************
	public int
	getPoints()
	{
		int nPoints = 0;
		for (Tile tile : this)
		{
			nPoints += tile.getScore();
		}
		return nPoints;
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
			if (tile != null && cTile == tile.getLetter())
			{
				remove(tile);
				return tile;
			}
		}
		return null;
	}

	//**********************************************************************************************
	public boolean
	grab(Rack rack)
	{
		if (size() > 0 && !rack.isFull())
		{
			int iTile = (int)(size() * Math.random());
			return rack.add(remove(iTile));
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
			if (tile != null && tile.getLetter() == cTile)
			{
				return i;
			}
		}
		return -1;
	}
}

//**************************************************************************************************
