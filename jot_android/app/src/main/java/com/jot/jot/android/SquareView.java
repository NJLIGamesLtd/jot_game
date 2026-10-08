/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * A SquareView is an Android View of a Square.
 */

package com.jot.jot.android;

import com.jot.jot.crossword.*;
import com.jot.jot.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;

//**************************************************************************************************

public class SquareView extends ViewGroup
{
	private Square m_square;
	private TileView m_tileView;

	private int m_xStart;
	private int m_yStart;
	private int m_xEnd;
	private int m_yEnd;
	
	//**********************************************************************************************
	public
	SquareView(Context context)
	{
		super(context);

		// By default, this square has no tile.
		m_tileView = null;

		// Set this view object's layout parameters.
		setLayoutParams(new WindowManager.LayoutParams());
	}

	//**********************************************************************************************
	public boolean
	isClose(
		int x,
		int y)
	{
		if (m_tileView == null && x >= m_xStart && x <= m_xEnd)
		{
			if ((y >= m_yStart - ((m_yEnd - m_yStart) >> 1) && y <= m_yEnd) ||
				(y >= m_yStart && y <= m_yEnd + ((m_yEnd - m_yStart) >> 1)))
			{
				return true;
			}
		}
		return false;
	}
	
	//**********************************************************************************************
	public boolean
	isWithin(
		int x,
		int y,
		boolean bMatchBelow)
	{
		return
			x >= m_xStart && x <= m_xEnd &&
			y >= m_yStart && (bMatchBelow || y <= m_yEnd);
	}
	
	//**********************************************************************************************
	// Handler when a tile is dropped onto this square.
	public void
	onDrop(View view)
	{
		// Remove all existing views.
		removeAllViews();

		// IF this tile is a child of another, remove it from its former parent.
		ViewGroup vg = (ViewGroup)view.getParent();
		if (vg != null)
		{
			vg.removeView(view);
		}

		WindowManager.LayoutParams lp =
			(WindowManager.LayoutParams)view.getLayoutParams();
		lp.x = 0;
		lp.y = 0;
		lp.width = getWidth();
		lp.height = getHeight();
		view.setLayoutParams(lp);

		addView(view);
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
		// Ignore if we are not visible.
		if (xStart == xEnd)
		{
			return;
		}

		// Have the rack squares a little taller.
		int yDelta = 0;

		// Get the parent so we can determine our location on the screen.
		ViewParent parent = getParent();
		if (parent instanceof BoardView)
		{
			BoardView board = (BoardView)parent;
			m_xStart = board.getStartX();
			m_yStart = board.getStartY();
		}
		else if (parent instanceof RackView)
		{
			RackView rack = (RackView)parent;
			m_xStart = rack.getStartX();
			m_yStart = rack.getStartY() - 10;
			yDelta = 50;
		}			

		m_xEnd = m_xStart + xEnd;
		m_yEnd = m_yStart + yEnd + yDelta;
		m_xStart += xStart;
		m_yStart += yStart;

		// If our bonus has been removed by a temporary tile placement, add it back.
		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);
			child.measure(getWidth(), getHeight());
			child.layout(0, 0, getWidth(), getHeight());
		}
	}

	//**********************************************************************************************
	@Override
	protected void
	onMeasure(
		int nWidth,
		int nHeight)
	{
		setMeasuredDimension(nWidth, nHeight);
	}	

	//**********************************************************************************************
	public Square
	getSquare()
	{
		return m_square;
	}

	//**********************************************************************************************
	public void
	setLocation(
		int xStart,
		int yStart,
		int xEnd,
		int yEnd)
	{
		m_xStart = xStart;
		m_yStart = yStart;
		m_xEnd = xEnd;
		m_yEnd = yEnd;
		
		// Ignore if we are not visible.
		if (xStart == xEnd)
		{
			return;
		}

		// Get the parent so we can determine our location on the screen.
		ViewParent parent = getParent();
		if (parent instanceof BoardView)
		{
			BoardView board = (BoardView)parent;
			m_xStart = board.getStartX();
			m_yStart = board.getStartY();
		}
		else if (parent instanceof RackView)
		{
			RackView rack = (RackView)parent;
			m_xStart = rack.getStartX();
			m_yStart = rack.getStartY();
		}			

		m_xEnd = m_xStart + xEnd;
		m_yEnd = m_yStart + yEnd;
		m_xStart += xStart;
		m_yStart += yStart;
	}

	//**********************************************************************************************
	public TileView
	getTileView()
	{
		return m_tileView;
	}

	//**********************************************************************************************
	public void
	setSquare(Square square)
	{
		m_square = square;

		// Set the background image.
		int idSquare = 0;
		switch (square.getBonus())
		{
			case NA:
				idSquare = R.drawable.jot_square;
				break;
			case L2:
				idSquare = R.drawable.jot_square_dl;
				break;
			case L21:
				idSquare = R.drawable.jot_square_dl_s1;
				break;
			case L22:
				idSquare = R.drawable.jot_square_dl_s2;
				break;
			case L23:
				idSquare = R.drawable.jot_square_dl_s3;
				break;
			case L24:
				idSquare = R.drawable.jot_square_dl_s4;
				break;
			case L3:
			case L4:
				idSquare = R.drawable.jot_square_tl;
				break;
			case W2:
				idSquare = R.drawable.jot_square_dw;
				break;
			case W3:
			case W4:
				idSquare = R.drawable.jot_square_tw;
				break;
		}
		setBackgroundResource(idSquare);
	}

	//**********************************************************************************************
	public void
	setRotation(boolean bRotated)
	{
		// Set the background image.
		int idSquare = 0;
		switch (m_square.getBonus())
		{
			case L22:
				if (bRotated)
				{
					idSquare = R.drawable.jot_square_dl_s3;
				}
				else
				{
					idSquare = R.drawable.jot_square_dl_s2;
				}
				break;
			case L23:
				if (bRotated)
				{
					idSquare = R.drawable.jot_square_dl_s2;
				}
				else
				{
					idSquare = R.drawable.jot_square_dl_s3;
				}
				break;
			default:
				return;
		}
		setBackgroundResource(idSquare);
	}

	//**********************************************************************************************
	public void
	setTileView(
		TileView tileView,
		boolean bPlaced)
	{
		setTileView(tileView, bPlaced, true);
	}

	//**********************************************************************************************
	public void
	setTileView(
		TileView tileView,
		boolean bPlaced,
		boolean bPromote)
	{
		// Remove all existing views.
		removeAllViews();

		if (m_square != null)
		{
			// If we have a new view, give it a position.
			if (tileView != null)
			{
				onDrop(tileView);
				if (bPromote)
				{
					m_square.setTile(tileView.getTile(), bPlaced);
				}
			}
			else if (bPromote)
			{
				m_square.setTile(null, bPlaced);
			}
			m_tileView = tileView;
		}
	}

	//**********************************************************************************************
	public boolean
	updateView() throws Exception
	{
		if (m_tileView != null)
		{
			return m_tileView.updateView();
		}
		return false;
	}
}

//**************************************************************************************************
