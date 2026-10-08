/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.android;

 import com.jot.jot.crossword.Board;
import com.jot.jot.crossword.Game;
import com.jot.jot.crossword.Tile;
import com.jot.jot.crossword.Tiles;

import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

//**************************************************************************************************

public class GameView extends ViewGroup
{
	private final MainActivity m_mainActivity;
	
	private Game m_game;

	private int m_nHeight;

	private final BoardView m_boardView;
	private final QwertyView m_qwertyView;
	private final RackView m_rackView;
	private TileView m_tileView;
	private SquareView m_squareView;

	private final MessageView m_msgView;
	private boolean m_bMessage;

	private final List<TileView> m_lTilesRange;

	private boolean m_bQwerty;

	private int m_iRackTile;

	private int m_iPlayer = 0;
	private final int m_nPlayers;

	//**********************************************************************************************
	public
	GameView(
		MainActivity mainActivity,
		int nPlayers,
		int nBoardWidth,
		int nBoardHeight,
		int nRackSize
	) throws Exception
	{
		super(mainActivity);
		m_mainActivity = mainActivity;
		m_nPlayers = nPlayers;

		// Set the layout parameters.
		setLayoutParams(new WindowManager.LayoutParams());

		// Create the viewable board.
		m_boardView = new BoardView(mainActivity, nPlayers, nBoardWidth, nBoardHeight);
		addView(m_boardView);

		// Create the user's tile rack.
		m_rackView = new RackView(mainActivity, nRackSize);
		addView(m_rackView);

		// Need a letter selection screen when a blank tile is played.
		m_bQwerty = false;
		m_qwertyView = new QwertyView(mainActivity, this);

		// Need a message screen to allow user to send another a message.
		m_msgView = new MessageView(mainActivity);
		m_bMessage = false;

		// Allow user to move a range of tiles.
		m_lTilesRange = new ArrayList<>();
	}

	//**********************************************************************************************
	public boolean
	blockFocus()
	{
		return m_boardView.blockFocus();
	}

	//**********************************************************************************************
	public int
	getDisplayHeight()
	{
		return m_nHeight;
	}

	//**********************************************************************************************
	public List<TileView>
	getTilesRange()
	{
		return m_lTilesRange;
	}

	//**********************************************************************************************
	public TileView
	getTileSelect()
	{
		return m_tileView;
	}

	//**********************************************************************************************
	public boolean
	isBoardFocused()
	{
		m_boardView.updateFocus(-1, -1);
		return m_boardView.isFocused();
	}

	//**********************************************************************************************
	public boolean
	isPlayerSetup()
	{
		return m_boardView.isPlayerSetup();
	}

	//**********************************************************************************************
	public boolean
	isQwertyActive()
	{
		return m_bQwerty;
	}

	//**********************************************************************************************
	public boolean
	isRackFilled()
	{
		if (m_game != null)
		{
			return m_game.isRackFilled(m_iPlayer);
		}
		return false;
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
		// Save the next location for a view.
		int yNext = yStart;
		int yQwerty = yEnd - ((yEnd - yStart) >> 2);

		int nChildren = getChildCount();
		for (int i = 0; i < nChildren; ++i)
		{
			View child = getChildAt(i);
			LayoutParams params = (LayoutParams)child.getLayoutParams();
			if (!(params instanceof WindowManager.LayoutParams))
			{
				continue;
			}

			// If we have a blank tile to choose a letter for, display a qwerty keyboard.
			if (m_bQwerty)
			{
				// Display our qwerty screen.
				if (child instanceof QwertyView)
				{
					int width = xEnd - xStart;
					int height = (yEnd - yStart) / 4;
					int x = xStart + (((xEnd - xStart) - width) / 4);
					child.measure(width, height);
					child.layout(x, yQwerty, x + width, yEnd);
					continue;
				}
			}

			// If the opponent sent a message, display it.
			if (child == m_msgView)
			{
				int nWidth = m_msgView.getMessageWidth();
				int nHeight = m_msgView.getMessageHeight();
				int x = xStart + ((xEnd - xStart) - nWidth) / 2;
				int y = m_boardView.getTopY() - yStart;
				// Have it point to the opponent.
				if (m_iPlayer == 0)
				{
					y -= y / 8;
				}
				else
				{
					y = 3 * y / 8;
				}
				child.measure(nWidth, nHeight);
				child.layout(x, y, x + nWidth, y + nHeight);
			}
			else if (child instanceof BoardView)
			{
				child.measure(xEnd, yEnd);
				int width = child.getMeasuredWidth();
				int height = child.getMeasuredHeight();
				child.layout(xStart, yNext, xStart + width, yNext + height);
				height = child.getMeasuredHeight();
				yNext += height;
			}
			else if (child instanceof TileView)
			{
				TileView tileView = (TileView)child;
				child.layout(
					(int)tileView.getTileX(),
					(int)tileView.getTileY(),
					(int)(tileView.getTileX() + tileView.getTileWidth()),
					(int)(tileView.getTileY() + tileView.getTileHeight()));
			}
			else
			{
				if (child instanceof RackView)
				{
					yQwerty = yNext;
					yNext += 10;
				}
				child.measure(xEnd, yEnd);
				int width = child.getMeasuredWidth();
				int height = child.getMeasuredHeight();

				child.layout(xStart, yNext, xStart + width, yNext + height);
				yNext += height + 10;
			}
		}
		m_nHeight = yNext - yStart;
	}

	//**********************************************************************************************
	public boolean
	placeTile(
		TileView tileView,
		long msTouch,
		int x,
		int y
	) throws Exception
	{
		// Check to see if we have a range of tiles.
		if (m_tileView == null)
		{
			// Return the range tiles to the rack.
			rackRangeTiles();
			return false;
		}
		
		if (m_tileView != tileView)
		{
			throw new Exception("Floating a different tile.");
		}
		m_tileView = null;

		// Remove this tile from our view control.
		removeView(tileView);

		// By default, we are moving tiles around its rack.
		boolean bPlaced = false;

		// By default, no updates made on the screen.
		boolean bUpdated = false;

		// Determine where the tile is being placed.
		SquareView squareView;
		do
		{
			// See if this tile is being dropped back onto the rack.
			if ((squareView = m_rackView.getSquareView(x, y)) != null)
			{
				// Clear the last score.
				m_boardView.clearLastScore(m_iPlayer);

				// See if we have a blank tile.
				if (tileView.getScore() == 0)
				{
					tileView.setLetter(Tile.WILD);
					removeView(m_qwertyView);
					m_bQwerty = false;
				}
				break;
			}

			// Don't allow tiles to be placed on the board until the player's turn.
			if (m_iPlayer != m_game.getPlayer())
			{
				m_mainActivity.setAlert("Sorry, it's not your turn.");
				break;
			}
			
			// If there are still more tiles to draw, alert the user to do so.
			if (!isRackFilled())
			{
				m_mainActivity.setAlert("Please draw more tiles.");
				break;
			}
			
			// If we have a quick fling, try to build on an existing placement.
			if (msTouch < 400)
			{
				squareView = m_boardView.getSquareView(
					m_game.getFlingSquare(
						tileView.getTile(),
						m_iRackTile,
						m_boardView.isRotated()));
			}
			else
			{
				m_game.setRackTileIndex(m_iRackTile);
			}

			// Else, try to place the letter at the touch location.
			if (squareView == null)
			{
				squareView = m_boardView.getSquareViewByLocation(x, y, !m_boardView.isFocused());
			}

			// If we have a board square, we will need to mark the tile as placed.
			if (squareView != null)
			{
				bPlaced = true;
			}
		} while (false);

		// If we have a tile to be placed on the board, allow focus on this tile.
		if (bPlaced)
		{
			m_boardView.allowFocus();
		}

		// If found, drop it onto this square.
		if (squareView != null)
		{
			TileView tileView2 = squareView.getTileView();

			// Try to transpose tiles.
			if (tileView2 != null &&
				tileView2.getState() == Tile.State.PLACED &&
				m_squareView != null &&
				m_squareView.getParent() instanceof BoardView)
			{
				m_squareView.setTileView(tileView2, bPlaced);
				tileView2 = null;
			}
			
			if (tileView2 == null)
			{
				squareView.setTileView(tileView, bPlaced);

				// If we have a board square to place our letter, see if it is blank.
				if (bPlaced)
				{
					// See if we have a blank tile.
					if (!m_bQwerty && tileView.getLetter() == Tile.WILD)
					{
						addView(m_qwertyView);
						m_bQwerty = true;
					}
				}

				// Ask the player to place or play a tile if their rack is full.
				if (isRackFilled())
				{
					m_game.setState(Game.State.PLACE_TILES);
				}
				bUpdated = true;
			}
			else
			{
				// Shift tiles over to fit this one.
				bUpdated = m_rackView.shiftToFit(tileView, squareView);
			}
		}
		else
		{
			// Bad placement. Place it back onto its rack, instead.
			m_rackView.add(tileView);

			// If our rack is full, ask player to place a tile.
			if (isRackFilled())
			{
				m_game.setState(Game.State.PLACE_TILES);
			}
		}

		m_mainActivity.updateState(false);
		return bUpdated;
	}

	//**********************************************************************************************
	public boolean
	placeTiles()
	{
		// Make sure we have tiles to place.
		int nTiles = m_lTilesRange.size();
		if (nTiles == 0)
		{
			return false;
		}

		// Don't allow tiles to be placed on the board until the player's turn.
		if (m_iPlayer != m_game.getPlayer())
		{
			// Return the range tiles to the rack.
			rackRangeTiles();
			
			m_mainActivity.setAlert("Sorry, it's not your turn.");
			return false;
		}
			
		// Get our x,y based on the first tile.
		TileView tileView = m_lTilesRange.get(0);
		if (tileView == null)
		{
			return false;
		}
		int x = (int)tileView.getX() + tileView.getWidth() / 2;
		int y = (int)tileView.getY() + tileView.getHeight();

		// Need to have our drop onto the board.
		SquareView squareView = m_boardView.getSquareViewByLocation(x, y, false);
		if (squareView != null)
		{
			// Make sure the initial tile square is empty.
			if (squareView.getTileView() != null)
			{
				squareView = null;
			}
		}

		// If we have a blank tile, make sure we ask for the letter.
		TileView tileBlank = null;

		// Place each. If any fail, they should all revert to the rack.
		boolean bPlaced = false;
		for (int iTile = 0; iTile < nTiles; ++iTile)
		{
			tileView = m_lTilesRange.get(iTile);

			// Remove this tile from our view control.
			removeView(tileView);

			// If we have square to place this, put it back on the rack.
			if (squareView == null)
			{
				m_rackView.add(tileView);
				continue;
			}

			// Remember our blank tile.
			if (tileView.getLetter() == Tile.WILD)
			{
				tileBlank = tileView;
			}

			// Place the tile on the square.
			squareView.setTileView(tileView, true);
			bPlaced = true;

			// Find the next open square.
			int width = m_boardView.getSquareWidth();
			while (squareView != null)
			{
				x += width;
				if ((squareView = m_boardView.getSquareViewByLocation(x, y, false)) != null)
				{
					if (squareView.getTileView() == null)
					{
						break;
					}
				}
			}
		}

		// Remove all tiles from our range list.
		m_lTilesRange.clear();

		// If we have a blank, ask for its letter.
		if (tileBlank != null)
		{
			addView(m_qwertyView);
			m_bQwerty = true;
		}

		if (bPlaced)
		{
			m_game.setState(Game.State.PLAY_TILES);
		}

		m_mainActivity.updateState(false);
		return true;
	}

	//**********************************************************************************************
	// Return the range tiles to the rack.
	public void
	rackRangeTiles()
	{
		int i = m_lTilesRange.size();
		while (--i >= 0)
		{
			m_rackView.add(m_lTilesRange.remove(i));
		}
	}

	//**********************************************************************************************
	public void
	reset()
	{
		m_boardView.reset();
	}

	//**********************************************************************************************
	public void
	setBlankLetter(char cTile)
	{
		m_boardView.setBlankLetter(cTile);
		removeView(m_qwertyView);
		m_bQwerty = false;
		m_mainActivity.updateState(false);
	}

	//**********************************************************************************************
	public void
	setBoardRotation(boolean bRotated)
	{
		m_boardView.setBoardRotation(bRotated);
	}

	//**********************************************************************************************
	public void
	setGame(
		Game game,
		int iPlayer
	) throws Exception
	{
		if (game != null)
		{
			if (game != m_game || game.isGameReset())
			{
				m_game = game;
				m_iPlayer = iPlayer;
				m_game.setGameReset(false);
				m_game.setGameSwitched(true);

				// Assign the game board to our view of it.
				m_boardView.setPlayer(iPlayer);
				m_boardView.setBoard(game.getBoard());

				// Assign the user's rack to our view of it.
				m_rackView.setRack(game.getRack(iPlayer));

				// Set the player color of our qwerty keyboard.
				m_qwertyView.setPlayer(iPlayer);

				m_boardView.clearPlayerNames();
				for (iPlayer = 0; iPlayer < m_nPlayers; ++iPlayer)
				{
					setPlayerName(iPlayer, m_game.getPlayerName(iPlayer));
				}
			}
			else if (m_boardView.isAnonymous())
			{
				for (iPlayer = 0; iPlayer < m_nPlayers; ++iPlayer)
				{
					setPlayerName(iPlayer, m_game.getPlayerName(iPlayer));
				}
			}
		}
	}

	//**********************************************************************************************
	public void
	setPlayerName(
		int iPlayer,
		String strPlayerName)
	{
		m_boardView.setPlayerName(iPlayer, strPlayerName, true);
	}

	//**********************************************************************************************
	public void
	setTileSelect(
		TileView tileView,
		SquareView squareView,
		int iRackTile)
	{
		if (m_tileView != null)
		{
			// Remove this tile from our view control.
			removeView(m_tileView);

			// Place it back onto its rack.
			m_rackView.add(m_tileView);
		}
		m_squareView = squareView;
		m_tileView = tileView;
		if (tileView != null)
		{
			tileView.measure(m_rackView.getSquareWidth(), m_rackView.getSquareHeight());
			addView(tileView);

			// Remember the direction on the rack we are flinging tiles.
			if (iRackTile >= 0)
			{
				m_iRackTile = iRackTile;
			}
		}
	}

	//**********************************************************************************************
	public void
	setTileSelectRange(TileView tileView)
	{
		// Need a primary and range tile.
		if (tileView == null || m_tileView == null)
		{
			return;
		}

		// Select the range of tiles on the rack.
		int iRackTile = m_rackView.removeTileView(tileView);
		if (iRackTile < 0 || iRackTile == m_iRackTile)
		{
			return;
		}

		// Move the range of tiles to our list.
		if (iRackTile < m_iRackTile)
		{
			// Reinsert the primary tile based on order.
			removeView(m_tileView);

			addView(tileView);
			m_lTilesRange.add(tileView);
			while (++iRackTile < m_iRackTile)
			{
				if ((tileView = m_rackView.removeTileView(iRackTile)) != null)
				{
					addView(tileView);
					m_lTilesRange.add(tileView);
				}
			}
			tileView = m_tileView;
		}
		else
		{
			m_lTilesRange.add(m_tileView);
			while (++m_iRackTile < iRackTile)
			{
				TileView tileView2 = m_rackView.removeTileView(m_iRackTile);
				if (tileView2 != null)
				{
					addView(tileView2);
					m_lTilesRange.add(tileView2);
				}
			}
		}
		addView(tileView);
		m_lTilesRange.add(tileView);

		m_tileView = null;
		m_squareView = null;
	}

	//**********************************************************************************************
	// Assign a label based on the tile availability.
	public void
	setTileButtonLabel(
		Button button,
		float dFontMultiplier)
	{
		if (m_game != null)
		{
			if (m_game.isBagFull(m_iPlayer))
			{
				button.setText("");
			}
			else
			{
				Tiles tiles = m_game.getTileBag(m_iPlayer);
				button.setText(Integer.toString(tiles.size()));
				button.setTextSize(15 * dFontMultiplier);
			}
		}
	}

	//**********************************************************************************************
	// Assign a label based on the tile availability.
	public void
	setVowelButtonLabel(
		Button button,
		float dFontMultiplier)
	{
		if (m_game != null)
		{
			if (m_game.isBagFull(m_iPlayer))
			{
				button.setText("");
			}
			else
			{
				Tiles tiles = m_game.getVowelBag(m_iPlayer);
				if (tiles != null)
				{
					button.setText(Integer.toString(tiles.size()));
					button.setTextSize(15 * dFontMultiplier);
				}
			}
		}
	}

	//**********************************************************************************************
	public String
	toString()
	{
		if (m_game != null)
		{
			return m_game.toString();
		}
		return "";
	}

	//**********************************************************************************************
	public boolean
	updateScore(int iPlayer)
	{
		if (m_game != null)
		{
			int nScore = m_game.getScore(iPlayer);
			int nBoardScore = m_boardView.getScore(iPlayer);
			if (nBoardScore != nScore)
			{
				if (nScore == 0)
				{
					m_boardView.eraseScore(iPlayer);
				}
				else if (nScore < nBoardScore)
				{
					if (m_game.isGameSwitched())
					{
						m_boardView.eraseScore(iPlayer);
					}
					else
					{
						m_boardView.decrementScore(iPlayer);
					}
				}
				else
				{
					if (m_game.isGameSwitched())
					{
						m_boardView.bumpScore(iPlayer, nScore);
					}
					else
					{
						m_boardView.incrementScore(iPlayer);
					}
				}
				m_mainActivity.playTone();
				return true;
			}
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	updateBest() throws Exception
	{
		Board boardBest = m_game.getBoardBest();
		if (boardBest != null)
		{
			return m_boardView.updateView(boardBest, m_iPlayer);
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	updateFocus()
	{
		return m_boardView.updateFocus(-1, -1);
	}
	
	//**********************************************************************************************
	public void
	updatePlayers()
	{
		if (m_game != null)
		{
			m_boardView.updatePlayers(m_game.getPlayer());
		}
	}
	
	//**********************************************************************************************
	public boolean
	updateScore()
	{
		for (int iPlayer = 0; iPlayer < m_nPlayers; ++iPlayer)
		{
			if (updateScore(iPlayer))
			{
				return true;
			}
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	updateView()
	{
		// Get rid of the message window if done.
		if (m_bMessage)
		{
			String strMessage = m_msgView.getComposeMessage();
			if (strMessage != null && !strMessage.isEmpty())
			{
				m_game.sendMessage(strMessage);
			}
			if (m_msgView.isDone())
			{
				removeView(m_msgView);
				m_bMessage = false;
			}
		}

		// Check to see if the user wants to send a message.
		if (!m_bMessage && m_boardView.isPlayerMessage())
		{
			m_msgView.setCompose();
			addView(m_msgView);
			m_bMessage = true;
		}
		
		do
		{
			// If we have a stranded tile, place it back on the rack.
			if (m_game != null && m_game.testRackedTiles() && m_tileView != null)
			{
				m_rackView.add(m_tileView);
				m_tileView = null;
				m_squareView = null;
			}

			try
			{
				// Update player's rack quickly.
				if (m_rackView.updateView())
				{
					break;
				}

				// If we are showing a completely different game, update board before score.
				if (m_game != null && !m_game.isGameSwitched())
				{
					// Make sure the player's score is updated before we display the opponent word.
					if (updateScore(m_iPlayer))
					{
						break;
					}
				}

				// If the robot is working, don't update the board.
				if (m_game != null && m_game.isWorking())
				{
					break;
				}

				// Update the board.
				if (m_boardView.updateView(m_iPlayer))
				{
					// For a the return of a game, update the board quickly.
					if (m_game != null && m_game.isGameSwitched())
					{
						while (m_boardView.updateView(m_iPlayer))
						{
						}
					}
					break;
				}

				// Update the score.
				if (updateScore())
				{
					break;
				}
			}
			catch (Exception e)
			{
				m_mainActivity.setAlert(e.getMessage());
				return false;
			}

			// The new game is now visible.  Switch to animated updates.
			if (m_game != null)
			{
				m_game.setGameSwitched(false);
			}

			// Update the player names, if needed.
			updatePlayers();

			// If we have a new message, display it.
			if (!m_bMessage)
			{
				String strInvalids = m_game.getOpponentInvalids();
				if (strInvalids != null && !strInvalids.isEmpty())
				{
					m_msgView.setReceiveMessage(
						m_game.getOpponentName() + " played invalid words:\n" + strInvalids);
					addView(m_msgView);
					m_bMessage = true;
				}
				else
				{
					String strMessage = m_game.getOpponentMessage();
					if (strMessage != null && !strMessage.isEmpty())
					{
						m_msgView.setReceiveMessage(strMessage);
						addView(m_msgView);
						m_bMessage = true;
					}
				}
			}
			
			// Done updating.
			return false;

		} while (false);

		// Force continued updates until the robot is done.
		requestLayout();
		return true;
	}
}

//**************************************************************************************************
