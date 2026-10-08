/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * A letter Tile to play on a crossword Board.
 */

package com.jot.jot.android;

import com.jot.jot.crossword.*;
import com.jot.jot.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.appcompat.widget.AppCompatImageView;

//**************************************************************************************************

public class TileView extends AppCompatImageView
{
	private static final Map<Tile, TileView> s_mTileViews = new HashMap<>();
	private static long s_msTouch;
	private static int s_nYDelta = 0;

	public static TileView
	GetTileView(
		MainActivity mainActivity,
		Tile tile)
	{
		// If needed, get the font Y delta to raise a tile on top of a finger touch.
		if (s_nYDelta == 0)
		{
			int nTileHeight = 3 * mainActivity.getHeight() / 31;
			if ((s_nYDelta = (int)(mainActivity.getFontMultiplier() * nTileHeight))
				> nTileHeight)
			{
				s_nYDelta = nTileHeight;
			}
		}

		TileView tileView = s_mTileViews.get(tile);
		if (tileView == null)
		{
			tileView = new TileView(mainActivity, tile);
			s_mTileViews.put(tile, tileView);
		}
		return tileView;
	}

	private static final int[][] s_idTiles =
	{
		{
			R.drawable.jot_white_wild,
			R.drawable.jot_white_a,
			R.drawable.jot_white_b,
			R.drawable.jot_white_c,
			R.drawable.jot_white_d,
			R.drawable.jot_white_e,
			R.drawable.jot_white_f,
			R.drawable.jot_white_g,
			R.drawable.jot_white_h,
			R.drawable.jot_white_i,
			R.drawable.jot_white_j,
			R.drawable.jot_white_k,
			R.drawable.jot_white_l,
			R.drawable.jot_white_m,
			R.drawable.jot_white_n,
			R.drawable.jot_white_o,
			R.drawable.jot_white_p,
			R.drawable.jot_white_q,
			R.drawable.jot_white_r,
			R.drawable.jot_white_s,
			R.drawable.jot_white_t,
			R.drawable.jot_white_u,
			R.drawable.jot_white_v,
			R.drawable.jot_white_w,
			R.drawable.jot_white_x,
			R.drawable.jot_white_y,
			R.drawable.jot_white_z
		},
		{
			R.drawable.jot_black_wild,
			R.drawable.jot_black_a,
			R.drawable.jot_black_b,
			R.drawable.jot_black_c,
			R.drawable.jot_black_d,
			R.drawable.jot_black_e,
			R.drawable.jot_black_f,
			R.drawable.jot_black_g,
			R.drawable.jot_black_h,
			R.drawable.jot_black_i,
			R.drawable.jot_black_j,
			R.drawable.jot_black_k,
			R.drawable.jot_black_l,
			R.drawable.jot_black_m,
			R.drawable.jot_black_n,
			R.drawable.jot_black_o,
			R.drawable.jot_black_p,
			R.drawable.jot_black_q,
			R.drawable.jot_black_r,
			R.drawable.jot_black_s,
			R.drawable.jot_black_t,
			R.drawable.jot_black_u,
			R.drawable.jot_black_v,
			R.drawable.jot_black_w,
			R.drawable.jot_black_x,
			R.drawable.jot_black_y,
			R.drawable.jot_black_z
		}
	};

	private static final int[][] s_idTilesWild =
	{
		{
			R.drawable.jot_white_wild,
			R.drawable.jot_white_wild_a,
			R.drawable.jot_white_wild_b,
			R.drawable.jot_white_wild_c,
			R.drawable.jot_white_wild_d,
			R.drawable.jot_white_wild_e,
			R.drawable.jot_white_wild_f,
			R.drawable.jot_white_wild_g,
			R.drawable.jot_white_wild_h,
			R.drawable.jot_white_wild_i,
			R.drawable.jot_white_wild_j,
			R.drawable.jot_white_wild_k,
			R.drawable.jot_white_wild_l,
			R.drawable.jot_white_wild_m,
			R.drawable.jot_white_wild_n,
			R.drawable.jot_white_wild_o,
			R.drawable.jot_white_wild_p,
			R.drawable.jot_white_wild_q,
			R.drawable.jot_white_wild_r,
			R.drawable.jot_white_wild_s,
			R.drawable.jot_white_wild_t,
			R.drawable.jot_white_wild_u,
			R.drawable.jot_white_wild_v,
			R.drawable.jot_white_wild_w,
			R.drawable.jot_white_wild_x,
			R.drawable.jot_white_wild_y,
			R.drawable.jot_white_wild_z
		},
		{
			R.drawable.jot_black_wild,
			R.drawable.jot_black_wild_a,
			R.drawable.jot_black_wild_b,
			R.drawable.jot_black_wild_c,
			R.drawable.jot_black_wild_d,
			R.drawable.jot_black_wild_e,
			R.drawable.jot_black_wild_f,
			R.drawable.jot_black_wild_g,
			R.drawable.jot_black_wild_h,
			R.drawable.jot_black_wild_i,
			R.drawable.jot_black_wild_j,
			R.drawable.jot_black_wild_k,
			R.drawable.jot_black_wild_l,
			R.drawable.jot_black_wild_m,
			R.drawable.jot_black_wild_n,
			R.drawable.jot_black_wild_o,
			R.drawable.jot_black_wild_p,
			R.drawable.jot_black_wild_q,
			R.drawable.jot_black_wild_r,
			R.drawable.jot_black_wild_s,
			R.drawable.jot_black_wild_t,
			R.drawable.jot_black_wild_u,
			R.drawable.jot_black_wild_v,
			R.drawable.jot_black_wild_w,
			R.drawable.jot_black_wild_x,
			R.drawable.jot_black_wild_y,
			R.drawable.jot_black_wild_z
		}
	};

	private final Tile m_tile;
	private int m_x;
	private int m_y;
	
	private final WindowManager.LayoutParams m_layoutParams;

	private char m_cLetter;

	private final MainActivity m_mainActivity;

	//**********************************************************************************************
	public
	TileView(
		MainActivity mainActivity,
		Tile tile)
	{
		super(mainActivity);
		m_mainActivity = mainActivity;

		m_tile = tile;

		// Set the layout parameters.
		m_layoutParams = new WindowManager.LayoutParams();
		setLayoutParams(m_layoutParams);

		setLetter(tile.getLetter());
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
		return m_tile.getScore();
	}

	//**********************************************************************************************
	public Tile.State
	getState()
	{
		return m_tile.getState();
	}

	//**********************************************************************************************
	public Tile
	getTile()
	{
		return m_tile;
	}

	//**********************************************************************************************
	public double
	getTileX()
	{
		return m_layoutParams.x;
	}

	//**********************************************************************************************
	public double
	getTileY()
	{
		return m_layoutParams.y - s_nYDelta;
	}

	//**********************************************************************************************
	public double
	getTileWidth()
	{
		return m_layoutParams.width;
	}

	//**********************************************************************************************
	public double
	getTileHeight()
	{
		return m_layoutParams.height;
	}

	//**********************************************************************************************
	@Override
	public float
	getX()
	{
		return m_x;
	}

	//**********************************************************************************************
	@Override
	public float
	getY()
	{
		return m_y;
	}

	//**********************************************************************************************
	public boolean
	handleDropEvent(MotionEvent event) throws Exception
	{
		// This tile's parent should be the game object.
		ViewGroup parent = (ViewGroup)getParent();
		if (!(parent instanceof GameView))
		{
			return false;
		}
		GameView gameView = (GameView)parent;

		// Check to see if we have a range of tiles to drop.
		List<TileView> lTiles = gameView.getTilesRange();
		if (lTiles != null && !lTiles.isEmpty())
		{
			return gameView.placeTiles();
		}

		// Get the duration of the tile drag.
		long msTime = System.currentTimeMillis() - s_msTouch;

		// Place this single tile.
		return gameView.placeTile(
			this,
			msTime,
			m_x + (getWidth() >> 1),
			m_y + getHeight());
	}

	//**********************************************************************************************
	public boolean
	handleMotionEvent(
		MotionEvent event,
		float xPos,
		float yPos,
		boolean bPrimary
	) throws Exception
	{
		m_layoutParams.x = (int)xPos - (getWidth() >> 1);
		m_layoutParams.y = (int)yPos - (getHeight() >> 2);
		setLayoutParams(m_layoutParams);
		requestLayout();

		// If this is the primary motion tile, see if it is part of a range.
		if (bPrimary)
		{
			// If this tile is being moved, it should be owned by the game object.
			ViewGroup parent = (ViewGroup)getParent();
			if (parent instanceof GameView)
			{
				GameView gameView = (GameView)parent;

				// If we have a range of tiles, make sure they all move together.
				List<TileView> lTiles = gameView.getTilesRange();
				if (lTiles != null && !lTiles.isEmpty() && lTiles.get(0) == this)
				{
					int nTiles = lTiles.size();
					for (int iTile = 1; iTile < nTiles; ++iTile)
					{
						xPos += getWidth();
						lTiles.get(iTile).handleMotionEvent(event, xPos, yPos, false);
					}
				}
			}
		}
		return true;
	}

	//**********************************************************************************************
	public boolean
	handleTouchEvent(MotionEvent event) throws Exception
	{
		// Can't move tiles which are already played.
		if (m_tile.isStateAtLeast(Tile.State.PLAYED))
		{
			return false;
		}
		
		/* Record current settings in case we need to jump back to them */
		ViewGroup parent = (ViewGroup)getParent();

		// Initial touches should occur from a board or rack square.
		if (!(parent instanceof SquareView))
		{
			return false;
		}
		SquareView squareView = (SquareView)parent;

		// Get the parent of the square (rack or board).
		parent = (ViewGroup)parent.getParent();

		/* Need to add view to window so it can move over all ViewGroups */
		m_layoutParams.x = (int)event.getRawX() - (getWidth() >> 1);
		m_layoutParams.y = (int)event.getRawY() - (getHeight() >> 2);
		setLayoutParams(m_layoutParams);

		// If the square's parent is our game board, try to place the letter there.
		if (parent instanceof BoardView)
		{
			// Only placed tiles can be moved.
			if (m_tile.getState() != Tile.State.PLACED)
			{
				return false;
			}

			BoardView boardView = (BoardView)parent;
			GameView gameView = (GameView)boardView.getParent();

			// SPK: Allow multitouch tiles from the game board?
			if (gameView.getTileSelect() != null)
			{
				return false;
			}

			// Remove the tile from the board.
			boardView.removeTileView(this);

			// Add this tile as a floating tile over the game screen.
			gameView.setTileSelect(this, squareView, -1);
		}

		// Must have a rack.
		else if (parent instanceof RackView)
		{
			RackView rackView = (RackView)parent;
			GameView gameView = (GameView)rackView.getParent();

			// If the qwerty screen is up, don't interfere with the touch of the background
			// rack tiles.
			if (gameView.isQwertyActive())
			{
				return false;
			}
			
			// If we have a multitouch, force the touches to be around the same time.
			boolean bMultitouch = false;
			if (gameView.getTileSelect() != null)
			{
				long msTime = System.currentTimeMillis() - s_msTouch;
				if (msTime >= 200)
				{
					return false;
				}
				bMultitouch = true;
			}

			if (bMultitouch)
			{				
				// Add this tile as a floating tile over the game screen.
				gameView.setTileSelectRange(this);
			}
			else
			{
				// Record the time of the touch.
				s_msTouch = System.currentTimeMillis();

				// Remove the tile from the rack.
				int iRackTile = rackView.removeTileView(this);

				// Add this tile as a floating tile over the game screen.
				gameView.setTileSelect(this, squareView, iRackTile);
			}
		}

		return true;
	}

	//**********************************************************************************************
	@Override
	protected void
	onAttachedToWindow()
	{
		super.onAttachedToWindow();
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
		m_layoutParams.x = m_x = xStart;
		m_layoutParams.y = m_y = yStart;
		m_layoutParams.width = xEnd - xStart;
		m_layoutParams.height = yEnd - yStart;
		setLayoutParams(m_layoutParams);
	}

	//**********************************************************************************************
	@Override
	public void
	onMeasure(
		int nWidth,
		int nHeight)
	{
		setMeasuredDimension(nWidth, nHeight);

		m_layoutParams.width = nWidth;
		m_layoutParams.height = nHeight;
		setLayoutParams(m_layoutParams);
	}

	//**********************************************************************************************
	@Override
    public boolean
	onTouchEvent(MotionEvent event)
	{
		try
		{
			switch (event.getAction())
			{
				case MotionEvent.ACTION_UP:
					return handleDropEvent(event);

				case MotionEvent.ACTION_DOWN:
					return handleTouchEvent(event);

				case MotionEvent.ACTION_MOVE:
					return handleMotionEvent(event, event.getRawX(), event.getRawY(), true);
			}
		}
		catch (Exception e)
		{
			m_mainActivity.failMessage("onTouchEvent() error:" + e.getMessage());
			return false;
		}
		return true;
	}

	//**********************************************************************************************
	public void
	setLetter(char cLetter)
	{
		m_cLetter = cLetter;
		m_tile.setLetter(cLetter);

		int idTile = 0;
		int iPlayer = m_tile.getPlayer();
		
		// If this tile is scoreless, it must be wild.
		if (m_tile.getScore() == 0)
		{
			idTile = s_idTilesWild[iPlayer][(int)(cLetter - Tile.WILD)];
		}
		else
		{
			idTile = s_idTiles[iPlayer][(int)(cLetter - Tile.WILD)];
		}

		setBackgroundResource(idTile);
	}

	//**********************************************************************************************
	public String
	toString()
	{
		return m_tile.toString();
	}

	//**********************************************************************************************
	public boolean
	updateView() throws Exception
	{
		if (m_tile != null && m_cLetter != m_tile.getLetter())
		{
			setLetter(m_tile.getLetter());
			return true;
		}
		return false;
	}
}

//**************************************************************************************************
