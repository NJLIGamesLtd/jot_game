/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.android;

import com.jot.jot.crossword.*;
import com.jot.jot.R;

import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

//**************************************************************************************************

public class BoardView extends ViewGroup
{
	private class FocusListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
	{
		@Override
		public boolean
		onScale(ScaleGestureDetector scaleGestureDetector)
		{
			if (scaleGestureDetector.getScaleFactor() < 1.0)
			{
				blockFocus();
			}
			else
			{
				allowFocus();
				updateFocus(
					(int)scaleGestureDetector.getFocusX(),
					(int)scaleGestureDetector.getFocusY());
			}
			m_mainActivity.updateState(false);
			return true;
		}
	}
	
	private static final int s_nBasesWidth = 17;
	private static final int s_nBasesHeight = 31;

	private final MainActivity m_mainActivity;

	private Board m_board;
	
	private final SquareView[][] m_aaSquareViews;
	private final ScoreView[] m_aScoreViews;
	private int m_nScoreHeight;

	private final String[][] m_aPlayerNames;

	private final ScaleGestureDetector m_focusDetector;
	
	private boolean m_bFocusBlock;
	private boolean m_bFocusForce;
	private int m_iFocusStartX;
	private int m_iFocusStartY;
	private int m_iFocusEndX;
	private int m_iFocusEndY;
	private int m_nFocusSquares;

	private float m_xTouchLast;
	private float m_yTouchLast;

	private int m_xStart;
	private int m_yStart;
	private int m_yTop;

	private float m_fBaseWidth;
	private float m_fBaseHeight;
	private float m_fSquareWidth;
	private float m_fSquareHeight;

	private final int m_nPlayers;
	private final int m_nSquares;

	private final int[] m_xLast;
	private final int[] m_yLast;
	private final int[] m_aLastScores;
	private final int[] m_aLastSquares;
	private final TextView[] m_viewLastScore;

	private final TextView[] m_aPlayerViews;
	private boolean m_bPlayerMessage;
	private boolean m_bPlayerSetup;

	private boolean m_bRotated;

	private final Placement m_placement;
	private final float m_dFontMultiplier;

	private int m_iPlayer = 0;

	//**********************************************************************************************
	public
	BoardView(
		MainActivity mainActivity,
		int nPlayers,
		int nBoardWidth,
		int nBoardHeight
	) throws Exception
	{
		super(mainActivity);

		m_mainActivity = mainActivity;
		m_nSquares = nBoardWidth;
		m_nPlayers = nPlayers;

		m_placement = new Placement();

		if (nBoardWidth != nBoardHeight)
		{
			throw new Exception("BoardView assumes a square board layout.");
		}

		m_aPlayerNames = new String[nPlayers][2];

		// Set this view object's layout parameters.
		WindowManager.LayoutParams layout = new WindowManager.LayoutParams();
		setLayoutParams(layout);

		// Add the score.
		m_aScoreViews = new ScoreView[nPlayers];
		m_aPlayerViews = new TextView[nPlayers];

		// Get the font multiplier
		m_dFontMultiplier = mainActivity.getFontMultiplier();
		
		for (int iPlayer = 0; iPlayer < nPlayers; ++iPlayer)
		{
			TextView view = new TextView(mainActivity);
		 	m_aPlayerViews[iPlayer] = view;
			view.setLayoutParams(new WindowManager.LayoutParams());
			view.setTextColor(Color.BLACK);
			view.setTextSize(25 * m_dFontMultiplier);
			addView(view);

			view.setOnTouchListener
			(
				(view1, motionEvent) ->
				{
					if (view1 == m_aPlayerViews[m_iPlayer])
					{
						m_bPlayerSetup = true;
					}
					else
					{
						m_bPlayerMessage = true;
					}
					m_mainActivity.updateState(false);
					return false;
				}
			);

			m_aScoreViews[iPlayer] = new ScoreView(mainActivity, -1, iPlayer);
			addView(m_aScoreViews[iPlayer]);
		}

		// Create the matrix of squares.
		m_aaSquareViews = new SquareView[nBoardHeight][nBoardWidth];
		for (int i = 0; i < nBoardHeight; ++i)
		{
			for (int j = 0; j < nBoardWidth; ++j)
			{
				SquareView squareView = new SquareView(mainActivity);
				m_aaSquareViews[i][j] = squareView;
				addView(squareView);
			}
		}

		// Create arrays to display last played word scores.
		m_xLast = new int[nPlayers];
		m_yLast = new int[nPlayers];
		m_aLastScores = new int[nPlayers];
		m_aLastSquares = new int[nPlayers];
		m_viewLastScore = new TextView[nPlayers];

		// Create a view to display the last score.
		for (int iPlayer = 0; iPlayer < nPlayers; ++iPlayer)
		{
			m_viewLastScore[iPlayer] = new TextView(mainActivity);
			m_viewLastScore[iPlayer].setTextColor(Color.WHITE);
			m_viewLastScore[iPlayer].setSingleLine();
			addView(m_viewLastScore[iPlayer]);
		}

		m_focusDetector = new ScaleGestureDetector(mainActivity, new FocusListener());

		blockFocus();
	}

	//**********************************************************************************************
	public void
	allowFocus()
	{
		m_bFocusBlock = false;
		m_bFocusForce = false;
	}

	//**********************************************************************************************
	public boolean
	blockFocus()
	{
		if (isFocused())
		{
			m_bFocusBlock = true;
			m_bFocusForce = false;
			m_iFocusStartX = m_iFocusStartY = 0;
			m_nFocusSquares = m_iFocusEndX = m_iFocusEndY = m_nSquares;
			return true;
		}
		return false;
	}

	//**********************************************************************************************
	public void
	bumpScore(int iPlayer, int nScore)
	{
		m_aScoreViews[iPlayer].bumpScore(nScore);
	}

	//**********************************************************************************************
	// Clear the last score displayed on the board.
	public void
	clearLastScore(int iPlayer)
	{
		m_aLastScores[iPlayer] = 0;
	}

	//**********************************************************************************************
	public void
	clearPlayerNames()
	{
		for (int i = 0; i < m_nPlayers; ++i)
		{
			m_aPlayerNames[i][0] = null;
			m_aPlayerNames[i][1] = null;
		}
	}

	//**********************************************************************************************
	public void
	decrementScore(int iPlayer)
	{
		m_aScoreViews[iPlayer].decrementScore();
	}

	//**********************************************************************************************
	public void
	eraseScore(int iPlayer)
	{
		m_aScoreViews[iPlayer].eraseScore();
	}

	//**********************************************************************************************
	public int
	getScore(int iPlayer)
	{
		return m_aScoreViews[iPlayer].getScore();
	}

	//**********************************************************************************************
	public int
	getSize()
	{
		return m_nSquares;
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
	getTopY()
	{
		return m_yTop;
	}

	//**********************************************************************************************
	public int
	getSquareHeight()
	{
		return (int)m_fSquareHeight - 1;
	}

	//**********************************************************************************************
	public int
	getSquareWidth()
	{
		return (int)m_fSquareWidth - 1;
	}

	//**********************************************************************************************
	public SquareView
	getSquareView(Square square)
	{
		if (square != null)
		{
			return getSquareViewByIndex(square.getRow(), square.getColumn());
		}
		return null;
	}

	//**********************************************************************************************
	public SquareView
	getSquareViewByIndex(
		int iRow,
		int iColumn)
	{
		return m_aaSquareViews[iRow][iColumn];
	}

	//**********************************************************************************************
	public SquareView
	getSquareViewByLocation(
		int x,
		int y,
		boolean bWantEmpty)
	{
		SquareView svFound = null;
		SquareView svClose = null;
		
		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);
			if (child instanceof SquareView)
			{
				SquareView squareView = (SquareView)child;
				if (svFound == null && squareView.isWithin(x, y, false))
				{
					svFound = squareView;
				}
				else if (bWantEmpty && squareView.isClose(x, y))
				{
					svClose = squareView;
					bWantEmpty = false;
				}
			}
		}
		
		// If we're looking for an empty square, allow it to look for the closest
		// bordering square which is empty when this has a tile.
		if (svFound != null &&
			svClose != null &&
			svFound.getTileView() != null)
		{
			return svClose;
		}
		return svFound;
	}

	//**********************************************************************************************
	public void
	incrementScore(int iPlayer)
	{
		m_aScoreViews[iPlayer].incrementScore();
	}

	//**********************************************************************************************
	public boolean
	isAnonymous()
	{
		return
			m_aPlayerNames[m_iPlayer ^ 1][0] == null ||
			m_aPlayerNames[m_iPlayer ^ 1][0].isEmpty();
	}

	//**********************************************************************************************
	public boolean
	isFocused()
	{
		return m_nFocusSquares < m_nSquares;
	}

	//**********************************************************************************************
	public boolean
	isRotated()
	{
		return m_bRotated;
	}

	//**********************************************************************************************
	protected boolean
	isPlayerMessage()
	{
		boolean bMessage = m_bPlayerMessage;
		m_bPlayerMessage = false;
		return bMessage;
	}

	//**********************************************************************************************
	protected boolean
	isPlayerSetup()
	{
		boolean bSetup = m_bPlayerSetup;
		m_bPlayerSetup = false;
		return bSetup;
	}

	//**********************************************************************************************
	protected int
	layoutPlayerScores(
		int xStart,
		int yStart,
		int xEnd,
		int yEnd)
	{
		int xNameEnd = xEnd - 3 * m_nScoreHeight;
		int iPlayer = 0;
		int iScore = 0;

		// Display the score first.
		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);
			if (child instanceof ScoreView)
			{
				int width = child.getWidth();
				if (width == 0)
				{
					// Supports 4 digit score as well.
					width = 3 * m_nScoreHeight;
				}
				int x = xEnd - width;
				int y = yStart + iScore * m_nScoreHeight;
				
				child.layout(
					xStart, // x,
					y,
					xEnd,   // x + width,
					y + m_nScoreHeight);
				++iScore;
			}
		}

		// Display the player names.
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);
			if (child instanceof TextView)
			{
				if (iPlayer < 2)
				{
					int y = yStart + iPlayer * m_nScoreHeight;
					child.layout(0, y, xNameEnd, y + m_nScoreHeight);
					++iPlayer;
				}
			}
		}
		return yStart + 2 * m_nScoreHeight;
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
		// Save the top/left location of our layout to later determine individual square locations.
		m_xStart = xStart;
		m_yStart = yStart;

		// Have a 1/2 sized base tile sized border.
		int nBorderWidth = (int)((m_fBaseWidth + 0.5) / 2);
		int nBorderHeight = (int)((m_fBaseHeight + 0.5) / 2);

		// Unless focused on a region, use the base size for our squares.
		m_fSquareWidth = m_fBaseWidth;
		m_fSquareHeight = m_fBaseHeight;

		// Get the displayable size of each square.
		boolean bFocused = isFocused();
		if (bFocused)
		{
			nBorderWidth = nBorderHeight = 0;
			m_fSquareWidth = (float)getWidth() / m_nFocusSquares;
			m_fSquareHeight =
				((m_fSquareWidth - 1) * m_fBaseHeight) / m_fBaseWidth;
		}

		m_yTop = layoutPlayerScores(xStart, yStart, xEnd, yEnd);

		// Save a running float location of each square to avoid shifting.
		float xSquare = nBorderWidth;
		float ySquare = m_yTop + nBorderHeight;

		// Save a running float location of the squares when rotated.
		float xRotate = nBorderWidth;
		float yRotate = ySquare;

		// Record the location, if found, where the last played word is.
		for (int iPlayer = 0; iPlayer < m_nPlayers; ++iPlayer)
		{
			m_xLast[iPlayer] = m_yLast[iPlayer] = 0;
		}

		// Prepare a counter to know what square we are looking at.
		int iSquare = 0;
		int iSquareX = 0;
		int iSquareY = 0;

		// Prepare other counters.
		int iTextView = 0;
		int nColumns = 0;

		// Loop through the child views.
		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);

			if (child instanceof SquareView)
			{
				// Display only if the tile square is within our board focus.
				if (iSquareX >= m_iFocusStartX &&
					iSquareX < m_iFocusEndX &&
					iSquareY >= m_iFocusStartY &&
					iSquareY < m_iFocusEndY)
				{
					// Get the starting point for the square; swap if rotated.
					int xNew = (int)xSquare;
					int yNew = (int)ySquare;
					if (m_bRotated)
					{
						xNew = (int)xRotate;
						yNew = (int)yRotate;
					}

					// Set the location.
					((SquareView)child).setLocation(
						xNew,
						yNew,
						(int)(xNew + m_fSquareWidth),
						(int)(yNew + m_fSquareHeight));

					// Should be a SquareView.  Set it's size without our board.
					child.measure((int)m_fSquareWidth, (int)m_fSquareHeight);

					// If this is the first letter tile in our last played word, remember
					// its location to display the word's score.
					for (int iPlayer = 0; iPlayer < m_nPlayers; ++iPlayer)
					{
						if (iSquare == m_aLastSquares[iPlayer])
						{
							m_xLast[iPlayer] = xNew;
							m_yLast[iPlayer] = yNew;
						}
					}
					
					// Allow the background to stay in view unless zoomed or tiled.
					if (!bFocused &&
						((SquareView)child).getTileView() == null)
					{
						child.layout(0, 0, 0, 0);
					}
					else
					{
						child.layout(
							xNew,
							yNew,
							(int)(xNew + m_fSquareWidth),
							(int)(yNew + m_fSquareHeight));
					}

					xSquare += m_fSquareWidth;
					yRotate += m_fSquareHeight;
				}
				else
				{
					child.layout(0, 0, 0, 0);
					((SquareView)child).setLocation(-1, -1, -1, -1);
				}
				++iSquare;

				if (++nColumns >= m_nSquares)
				{
					nColumns = 0;
					xSquare = nBorderWidth;
					yRotate = m_yTop + nBorderHeight;

					// Update the y position if we are in focus.
					if (iSquareY >= m_iFocusStartY &&
						iSquareY < m_iFocusEndY)
					{
						xRotate += m_fSquareWidth;
						ySquare += m_fSquareHeight;
					}
				}

				// Update the x/y square index.
				if (++iSquareX >= m_nSquares)
				{
					iSquareX = 0;
					++iSquareY;
				}
			}
			else if (child instanceof TextView)
			{
				if (++iTextView > m_nPlayers)
				{
					TextView textView = (TextView)child;
					int iPlayer = iTextView - m_nPlayers - 1;
					if (m_aLastScores[iPlayer] > 0 && m_yLast[iPlayer] > 0)
					{
						int nSize = 3;
						if (m_aLastScores[iPlayer] > 100)
						{
							++nSize;
						}
						int nScoreWidth = (int)(nSize * m_fSquareWidth / 4);
						int xScore = m_xLast[iPlayer] + nScoreWidth;
						if (xScore + nScoreWidth > xEnd)
						{
							xScore -= nScoreWidth;
						}
						m_yLast[iPlayer] -= nScoreWidth / 2;

						if (bFocused)
						{
							textView.setTextSize(10 * m_dFontMultiplier);
							textView.setPadding(
								(int)(8 * m_dFontMultiplier), (int)(16 * m_dFontMultiplier), 0, 0);
						}
						else
						{
							textView.setTextSize((int)(7.5 * m_dFontMultiplier));
							textView.setPadding(
								(int)(4 * m_dFontMultiplier), (int)(8 * m_dFontMultiplier), 0, 0);
						}

						child.layout(
							xScore,
							m_yLast[iPlayer],
							xScore + nScoreWidth,
							m_yLast[iPlayer] + nScoreWidth);
					}
					else
					{
						child.layout(0, 0, 0, 0);
					}
				}
			}
		}
	}
	
	//**********************************************************************************************
	@Override
	protected void
	onMeasure(
		int nWidth,
		int nHeight)
	{
		if (m_nScoreHeight == 0)
		{
			// Detemined the size of each tile base as a ratio to that in the image.
			m_fBaseWidth = (float)m_mainActivity.getWidth() / s_nBasesWidth;
			m_fBaseHeight = (float)m_mainActivity.getHeight() / s_nBasesHeight;
			m_nScoreHeight = (int)(2 * m_fBaseHeight);
		}

		LayoutParams params = getLayoutParams();
		params.width = nWidth;
		params.height = 2 * m_nScoreHeight + (int)((m_nSquares + 1) * m_fBaseHeight);
		setLayoutParams(params);
		setMeasuredDimension(params.width, params.height);
	}

	//**********************************************************************************************
	@Override
    public boolean
	onTouchEvent(MotionEvent event)
	{
		m_focusDetector.onTouchEvent(event);

		if (isFocused())
		{
			switch (event.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					m_xTouchLast = event.getRawX();
					m_yTouchLast = event.getRawY();
					break;

				case MotionEvent.ACTION_MOVE:
					float x = event.getRawX();
					float y = event.getRawY();
					int dx = (int)((m_xTouchLast - x) / m_fSquareWidth);
					int dy = (int)((m_yTouchLast - y) / m_fSquareHeight);
					if (dx != 0 || dy != 0)
					{
						// If the board is rotated, swap dx and dy.
						if (m_bRotated)
						{
							dx ^= dy;
							dy ^= dx;
							dx ^= dy;
						}

						m_bFocusForce = true;
						if ((m_iFocusStartX += dx) < 0)
						{
							m_iFocusStartX = 0;
						}
						if ((m_iFocusEndX = m_iFocusStartX + m_nFocusSquares) > m_nSquares)
						{
							m_iFocusEndX = m_nSquares;
							m_iFocusStartX = m_nSquares - m_nFocusSquares;
						}
						if ((m_iFocusStartY += dy) < 0)
						{
							m_iFocusStartY = 0;
						}
						if ((m_iFocusEndY = m_iFocusStartY + m_nFocusSquares) > m_nSquares)
						{
							m_iFocusEndY = m_nSquares;
							m_iFocusStartY = m_nSquares - m_nFocusSquares;
						}
						m_xTouchLast = x;
						m_yTouchLast = y;

						requestLayout();
					}
					break;
			}
		}
				
		return true;
	}

	//**********************************************************************************************
	public void
	removeTileView(TileView tileView)
	{
		if (tileView != null)
		{
			int nChildren = getChildCount();
			for (int i = 0; i < nChildren; ++i)
			{
				View child = getChildAt(i);
				if (child instanceof SquareView)
				{
					SquareView squareView = (SquareView)child;
					if (squareView.getTileView() == tileView)
					{
						squareView.setTileView(null, false);
						break;
					}
				}
			}
		}
	}
	
	//**********************************************************************************************
	public void
	reset()
	{
		// Erase scores.
		for (int iPlayer = 0; iPlayer < 2; ++iPlayer)
		{
			m_aScoreViews[iPlayer].eraseScore();
			clearLastScore(iPlayer);
		}

		// Default back to no rotation.
		m_bRotated = false;

		// By default, show the whole board.  No focus around one square.
		m_bFocusBlock = false;
		m_bFocusForce = false;
		updateFocus(-1, -1);
	}

	//**********************************************************************************************
	public void
	setBlankLetter(char cTile)
	{
		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);
			if (child instanceof SquareView)
			{
				SquareView squareView = (SquareView)child;
				TileView tileView = squareView.getTileView();
				if (tileView != null &&
					tileView.getState() == Tile.State.PLACED &&
					tileView.getLetter() == Tile.WILD)
				{
					tileView.setLetter(cTile);
					return;
				}
			}
		}
	}

	//**********************************************************************************************
	public void
	setBoard(Board board) throws Exception
	{
		m_board = board;

		// Make sure the board is the right size.
		if (m_nSquares != board.getSize())
		{
			throw new Exception("Game board size is not in line with BoardView.");
		}

		// Reset the view of the board.
		reset();

		// Assign the board squares to our views.
		for (int i = 0; i < m_nSquares; ++i)
		{
			for (int j = 0; j < m_nSquares; ++j)
			{
				m_aaSquareViews[i][j].setSquare(board.getSquare(i, j));
			}
		}
	}

	//**********************************************************************************************
	public void
	setBoardRotation(boolean bRotated)
	{
		m_bRotated = bRotated;

		// Rotate the background image of the center square views.
		for (int i = (m_nSquares - 1) >> 1; i <= m_nSquares >> 1; ++i)
		{
			for (int j = (m_nSquares - 1) >> 1; j <= m_nSquares >> 1; ++j)
			{
				m_aaSquareViews[i][j].setRotation(bRotated);
			}
		}
	}

	//**********************************************************************************************
	public void
	setPlayer(int iPlayer)
	{
		m_iPlayer = iPlayer;
	}

	//**********************************************************************************************
	public void
	setPlayerName(
		int iPlayer,
		String strPlayerName,
		boolean bForce)
	{
		if ((bForce || m_aPlayerNames[iPlayer][0] == null) &&
			strPlayerName != null)
		{
			if (iPlayer == m_iPlayer)
			{
				m_aPlayerNames[iPlayer][0] = "⚙" + strPlayerName;
				m_aPlayerNames[iPlayer][1] = "⚙" + "⭓" + strPlayerName;
			}
			else
			{
				m_aPlayerNames[iPlayer][0] = "✏" + strPlayerName;
				m_aPlayerNames[iPlayer][1] = "✏" + "⭓" + strPlayerName;
			}
		}
		m_aPlayerViews[iPlayer].setText(strPlayerName);
	}

	//**********************************************************************************************
	public void
	setPlayerTurn(int iTurn)
	{
		setPlayerName(iTurn, m_aPlayerNames[iTurn][1], false);
		setPlayerName(iTurn ^ 1, m_aPlayerNames[iTurn ^ 1][0], false);
	}

	//**********************************************************************************************
	public void
	setScore(
		int iPlayer,
		int nScore)
	{
		m_aScoreViews[iPlayer].setScore(nScore);
	}

	//**********************************************************************************************
	public String
	toString()
	{
		if (m_board != null)
		{
			return m_board.toString();
		}
		return "";
	}

	//**********************************************************************************************
	public boolean
	updateFocus(int x, int y)
	{
		int nFocusSquares = m_nSquares;
		Placement placement = null;

		// Don't zoom if blocked.
		if (!m_bFocusBlock && m_board != null)
		{
			if (x > 0)
			{
				SquareView squareView = getSquareViewByLocation(x, y, false);
				if (squareView == null)
				{
					return false;
				}
				m_placement.setPlacement(squareView.getSquare());
				placement = m_placement;
				m_bFocusForce = true;
			}
			else
			{
				if (m_bFocusForce)
				{
					return false;
				}
				// Get board exposure of our placed tiles.
				placement = m_board.getTilePlacement(Tile.State.PLACED);
			}
		}

		// If we have an exposure, see if we can zoom to show this.
		int nWidth = 0;
		int nHeight = 0;
		if (placement != null)
		{
			// Zoom in to show 5/8th of the board.
			if ((nFocusSquares = 5 * m_nSquares / 8) < 8)
			{
				nFocusSquares = 8;
			}

			nWidth = placement.m_xLast - placement.m_xFirst + 1;
			nHeight = placement.m_yLast - placement.m_yFirst + 1;
			if (nWidth >= nFocusSquares || nHeight >= nFocusSquares)
			{
				placement = null;
			}
		}

		// If we still have a placement, focus in on this.
		if (placement != null)
		{
			if ((m_iFocusStartX = placement.m_xFirst - ((nFocusSquares - nWidth) >> 1)) < 0)
			{
				m_iFocusStartX = 0;
			}
			if ((m_iFocusStartY = placement.m_yFirst - ((nFocusSquares - nHeight) >> 1)) < 0)
			{
				m_iFocusStartY = 0;
			}
		}
		else
		{
			nFocusSquares = m_nSquares;
			m_iFocusStartX = 0;
			m_iFocusStartY = 0;
		}

		// Generate the ending square locations.
		if ((m_iFocusEndX = m_iFocusStartX + nFocusSquares) > m_nSquares)
		{
			m_iFocusEndX = m_nSquares;
			m_iFocusStartX = m_nSquares - nFocusSquares;
		}
		if ((m_iFocusEndY = m_iFocusStartY + nFocusSquares) > m_nSquares)
		{
			m_iFocusEndY = m_nSquares;
			m_iFocusStartY = m_nSquares - nFocusSquares;
		}

		if (placement == null)
		{
		    requestLayout();
		}

		if (m_nFocusSquares != nFocusSquares)
		{
			m_nFocusSquares = nFocusSquares;
			return true;
		}
		return false;
	}

	//**********************************************************************************************
	public void
	updateLastScore(
		Board board,
		int iPlayer)
	{
		m_aLastScores[iPlayer] = 0;
		
		int nLastScore = board.getLastScore(iPlayer);
		if (nLastScore > 0)
		{
			m_aLastScores[iPlayer] = nLastScore;
			m_aLastSquares[iPlayer] = board.getLastSquare(iPlayer);

			m_viewLastScore[iPlayer].setBackgroundResource(R.drawable.score);
			m_viewLastScore[iPlayer].setText(Integer.toString(nLastScore));
		}
	}

	//**********************************************************************************************
	public void
	updatePlayers(int iTurn)
	{
		setPlayerTurn(iTurn);
	}

	//**********************************************************************************************
	public boolean
	updateView(int iPlayer) throws Exception
	{
		if (m_board != null)
		{
			return updateView(m_board, iPlayer);
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	updateView(
		Board board,
		int iPlayer
	) throws Exception
	{
		// Update the last score for this board.
		updateLastScore(board, iPlayer);
		updateLastScore(board, iPlayer ^ 1);

		// Check to see if there are played tiles that are not in our view.
		int nChildren = getChildCount();

		int x = 0;
		int y = 0;
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);
			if (child instanceof SquareView)
			{
				SquareView squareView = (SquareView)child;
				TileView tileView = squareView.getTileView();
				Tile tile = board.getTile(x, y);
				
				if (tileView == null)
				{
					if (tile != null)
					{
						squareView.setTileView(
							TileView.GetTileView(m_mainActivity, tile), false, false);
						return true;
					}
				}
				else if (tile == null)
				{
					squareView.setTileView(null, false, false);
					return true;
				}
				else if (tile != tileView.getTile())
				{
					squareView.setTileView(
						TileView.GetTileView(m_mainActivity, tile), false, false);
					return true;
				}
				else if (tile.getLetter() != tileView.getLetter())
				{
					tileView.setLetter(tile.getLetter());
					return true;
				}

				if (++y >= m_nSquares)
				{
					y = 0;
					++x;
				}
			}
		}

		// Update the last score for this board.
		updateLastScore(board, iPlayer);

		return false;
	}
}

//**************************************************************************************************
