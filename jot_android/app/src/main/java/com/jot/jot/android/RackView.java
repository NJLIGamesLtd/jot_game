/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.android;

import com.jot.jot.crossword.*;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

//**************************************************************************************************

public class RackView extends ViewGroup
{
	private static int s_nSquareWidth;
	
	private final MainActivity m_mainActivity;
	private Rack m_rack;
	private final int m_size;
	private int m_xStart;
	private int m_yStart;

	//**********************************************************************************************
	public
	RackView(
		Context context,
		int nSize)
	{
		super(context);

		m_mainActivity = (MainActivity)context;
		m_size = nSize;

		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		setLayoutParams(lp);

		for (int i = 0; i < m_size; ++i)
		{
			SquareView square = new SquareView(context);
			addView(square);
		}
	}
	
	//**********************************************************************************************
	public boolean
	add(TileView tile)
	{
		return m_rack.add(tile.getTile());
	}

	//**********************************************************************************************
	@Override
	protected void
	onLayout(
		boolean changed,
		int xStart,
		int yStart,
		int xEnd,
		int yEnd)
	{
		m_xStart = xStart;
		m_yStart = yStart;

		int nChildren = getChildCount();

		// If we haven't already, get the size of each square.
		if (s_nSquareWidth == 0)
		{
			s_nSquareWidth = (xEnd - xStart) / nChildren;
		}
		
		int x = 0;
		int y = 0;
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);
			child.measure(s_nSquareWidth, s_nSquareWidth);
			child.layout(
				x,
				y,
				x + s_nSquareWidth,
				y + s_nSquareWidth);
			
			x += s_nSquareWidth;
		}
	}
	
	//**********************************************************************************************
	@Override
    protected void
	onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		WindowManager.LayoutParams params = (WindowManager.LayoutParams)getLayoutParams();
		params.width = widthMeasureSpec;
		params.height = widthMeasureSpec / m_size;
		params.y = widthMeasureSpec;
		setLayoutParams(params);
		setMeasuredDimension(params.width, params.height);
    }

	//**********************************************************************************************
	public int
	getStartX()
	{
		return m_xStart;
	}
	
	//**********************************************************************************************
	public int
	getStartY()
	{
		return m_yStart;
	}

	//**********************************************************************************************
	public int
	getSquareWidth()
	{
		return s_nSquareWidth;
	}

	//**********************************************************************************************
	public int
	getSquareHeight()
	{
		return s_nSquareWidth;
	}

	//**********************************************************************************************
	public SquareView
	getSquareView(
		int x,
		int y)
	{
		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			SquareView child = (SquareView)getChildAt(i);
			if (child.isWithin(x, y, true))
			{
				return child;
			}
		}
		return null;
	}

	//**********************************************************************************************
	public TileView
	removeTileView(int iSquare)
	{
		int nChildren = getChildCount();
		if (iSquare < nChildren)
		{
			SquareView square = (SquareView)getChildAt(iSquare);
			TileView tile = square.getTileView();
			if (tile != null)
			{
				square.setTileView(null, false);
				return tile;
			}
		}
		return null;
	}

	//**********************************************************************************************
	public int
	removeTileView(TileView tile)
	{
		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			SquareView child = (SquareView)getChildAt(i);
			if (child.getTileView() == tile)
			{
				int iRack = m_rack.indexOf(tile.getTile());
				child.setTileView(null, false);
				return iRack;
			}
		}
		return -1;
	}

	//**********************************************************************************************
	public void
	setRack(Rack rack)
	{
		m_rack = rack;
		for (int i = 0; i < m_size; ++i)
		{
			SquareView squareView = (SquareView)getChildAt(i);
			squareView.setSquare(rack.getSquare(i));
		}
	}

	//**********************************************************************************************
	// Shift tiles over to fit this one.
	public boolean
	shiftToFit(
		TileView tileView,
		SquareView squareView
	) throws Exception
	{
		if (m_rack.shiftToFit(tileView.getTile(), squareView.getSquare()))
		{
			while (updateView())
			{
			}
			return true;
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	updateView() throws Exception
	{
		// Don't update the display until we have an underlying rack.
		if (m_rack == null)
		{
			return false;
		}
		
		boolean bUpdated = false;

		// Check to see if there are tiles that are not in our view.
		int numChildren = getChildCount();
		for (int i = 0; i < numChildren; ++i)
		{
			View child = getChildAt(i);
			if (!(child instanceof SquareView))
			{
				throw new Exception("RackView contains a non TileView child.");
			}

			// Get the raw tile on the game rack.
			Tile tileGame = m_rack.get(i);

			// Get the tile on the view rack.
			SquareView squareView = (SquareView)child;
			TileView tileView = squareView.getTileView();

			// If we have no view tile, see if we need to create one.
			if (tileView == null)
			{
				if (tileGame != null)
				{
					// Set the tile view that contains this tile.
					squareView.setTileView(
						TileView.GetTileView(m_mainActivity, tileGame),
						false);
					bUpdated = true;
					continue;
				}
			}
			else if (tileGame == null)
			{
				squareView.setTileView(null, false);
			}
			else
			{
				Tile tile = tileView.getTile();
				if (tile != tileGame)
				{
					// Set the tile view that contains this tile.
					squareView.setTileView(
						TileView.GetTileView(m_mainActivity, tileGame),
						false);
					bUpdated = true;
					continue;
				}
			}

			if (squareView.updateView())
			{
				bUpdated = true;
				continue;
			}

			// Just in case.
			if (tileGame != null)
			{
				squareView.setTileView(
					TileView.GetTileView(m_mainActivity, tileGame),
					false);
			}
		}
		return bUpdated;
	}
}

//**************************************************************************************************
