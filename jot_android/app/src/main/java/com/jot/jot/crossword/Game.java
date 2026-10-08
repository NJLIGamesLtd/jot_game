/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.crossword;

import com.jot.decomp.Words;
import com.jot.jot.android.MainActivity;
import com.jot.jot.db.DbGame;
import com.jot.jot.db.DbUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//**************************************************************************************************

public class Game
{
	public enum State
	{
		DRAW_TILES,
		FIRST_WORD,
		PLACE_TILES,
		PLAY_TILES,
		PLAY_READY,
		MOVE_ROBOT,
		ROBOT_PASSED,
		QUIT,
		OVER;

		public boolean
		atLeast(State eState)
		{
			return ordinal() >= eState.ordinal();
		}

		public String
		toString()
		{
			return name();
		}
	}

	public static int s_bmDefault = 0x0000;
	public static int s_bmChallengeMode = 0x0001;
	public static int s_bmTimeLimit = 0x0002;

	// An enumeration set of bonus squares.
	static final byte NA = (byte)Bonus.NA.ordinal();
	static final byte L2 = (byte)Bonus.L2.ordinal();
	static final byte L21 = (byte)Bonus.L21.ordinal();
	static final byte L22 = (byte)Bonus.L22.ordinal();
	static final byte L23 = (byte)Bonus.L23.ordinal();
	static final byte L24 = (byte)Bonus.L24.ordinal();
	static final byte L3 = (byte)Bonus.L3.ordinal();
	static final byte L4 = (byte)Bonus.L4.ordinal();
	static final byte W2 = (byte)Bonus.W2.ordinal();
	static final byte W3 = (byte)Bonus.W3.ordinal();
	static final byte W4 = (byte)Bonus.W4.ordinal();

	private final MainActivity m_mainActivity;

	private final byte[][] m_aaBonuses;
	private final char[][] m_aaDefaultTiles;
	private final char[] m_aTileBag;
	private final char[] m_aVowelBag;
	private final byte[] m_aTileScores;
	private final char[] m_aTileRack;
	private final int m_nBingoPrize;

	private final Board m_board;
	private Board m_boardBest;

	private final List<Rack> m_lRacks;
	private final Rack m_rackBackup;
	private final List<Tiles> m_lTileBags;
	private List<Tiles> m_lVowelBags;
	private int m_nTilesTotal;
	private int m_nTilesRobot;

	private int m_iPlayerTurn;
	private final int m_nPlayers;
	private final int m_iPlayerRobot;
	private final int[] m_aPlayerScores;
	private int m_nLastScore;
	private final int m_nRackSize;
	private final int m_nBoardSize;
	private final boolean m_bBagShared;

	private State m_eState;
	private int m_bmSettings;

	private boolean m_bRackedTiles;

	private final Words m_words;
	private final BestWord m_bestWord;

	private boolean m_bFlingTop;
	private boolean m_bFlingVertical;
	private int m_iFlingDelta;
	private int m_iFlingLast;

	private boolean m_bBestWorking;
	private boolean m_bRobotWorking;
	private boolean m_bGameReset;
	private boolean m_bGameSwitched;	// A different or new game is in use.
	private boolean m_bNoPlay;
	private boolean m_bSawInvalids;
	private int m_iRackFilled;

	private DbGame m_dbGame;

	private String m_strBoard;
	private String m_strMessage;

	private int m_nOpponentTiles;

	//**********************************************************************************************
	public
	Game(
		final byte[][] aaBonuses,
		final char[][] aaDefaultTiles,
		final char[] aTileBag,
		final byte[] aTileScores,
		final char[] aTileRack,
		Words words,
		MainActivity mainActivity,
		int nPlayers)
	{
		this(
			aaBonuses,
			aaDefaultTiles,
			aTileBag,
			null,
			aTileScores,
			aTileRack,
			50,
			words,
			mainActivity,
			nPlayers,
			1,
			true);
	}

	//**********************************************************************************************
	public
	Game(
		final byte[][] aaBonuses,
		final char[][] aaDefaultTiles,
		final char[] aTileBag,
		final char[] aVowelBag,
		final byte[] aTileScores,
		final char[] aTileRack,
		int nBingoPrize,
		Words words,
		MainActivity mainActivity,
		int nPlayers,
		int iPlayerRobot,
		boolean bBagShared)
	{
		m_aaBonuses = aaBonuses;
		m_aaDefaultTiles = aaDefaultTiles;
		m_aTileBag = aTileBag;
		m_aVowelBag = aVowelBag;
		m_aTileScores = aTileScores;
		m_aTileRack = aTileRack;
		m_nBingoPrize = nBingoPrize;

		// Store a count of the total tiles per player.
		m_nTilesTotal = aTileBag.length;
		if (aVowelBag != null)
		{
			m_nTilesTotal += aVowelBag.length;
		}
		m_nTilesRobot = m_nTilesTotal;
		
		m_board = new Board(
			aaBonuses,
			nBingoPrize,
			aTileRack.length,
			words,
			aaDefaultTiles,
			aTileScores);
		m_nBoardSize = m_board.getSize();
		m_nPlayers = nPlayers;
		m_iPlayerRobot = iPlayerRobot;
		m_bBagShared = bBagShared;
		m_words = words;
		m_mainActivity = mainActivity;

		// Create an array player scores.
		m_aPlayerScores = new int[nPlayers];
		m_nLastScore = 0;

		m_bBestWorking = false;
		m_bRobotWorking = false;
		m_bGameReset = false;
		m_bGameSwitched = false;
		m_bNoPlay = false;
		m_bSawInvalids = false;
		m_iRackFilled = -1;
		m_nOpponentTiles = 0;

		// Create a list of tile racks.
		m_nRackSize = aTileRack.length;
		m_lRacks = new ArrayList<>();
		for (int iPlayer = 0; iPlayer < nPlayers; ++iPlayer)
		{
			m_lRacks.add(new Rack(m_nRackSize));
		}
		m_rackBackup = new Rack(m_nRackSize);

		// Create a list of bags (separate or to the same reference) for each player.
		m_lTileBags = new ArrayList<>();

		// If we are separating vowels from normal tiles, place them in this list of bags.
		if (aVowelBag != null)
		{
			m_lVowelBags = new ArrayList<>();
		}

		// Add each bag of tiles to our lists.
		Tiles lTiles = null;
		Tiles lVowels = null;
		for (int iPlayer = 0; iPlayer < nPlayers; ++iPlayer)
		{
			if (iPlayer == 0 || !bBagShared)
			{
				lTiles = new Tiles();
				for (int iTile = 0; iTile < m_aTileBag.length; ++iTile)
				{
					char cTile = m_aTileBag[iTile];
					lTiles.add(new Tile(cTile, aTileScores[cTile], iPlayer));
				}

				// If needed separate the vowels.
				if (m_lVowelBags != null)
				{
					lVowels = new Tiles();
					for (int iVowel = 0; iVowel < aVowelBag.length; ++iVowel)
					{
						char cVowel = aVowelBag[iVowel];
						lVowels.add(new Tile(cVowel, aTileScores[cVowel], iPlayer));
					}
				}
			}
			m_lTileBags.add(lTiles);
			if (m_lVowelBags != null)
			{
				m_lVowelBags.add(lVowels);
			}
		}

		// Remove tiles that have already been played.
		if (aaDefaultTiles != null)
		{
			lTiles = m_lTileBags.get(0);
			lVowels = null;
			if (m_lVowelBags != null)
			{
				lVowels = m_lVowelBags.get(0);
			}
			for (int i = 0; i < m_nBoardSize; ++i)
			{
				for (int j = 0; j < m_nBoardSize; ++j)
				{
					char cTile = aaDefaultTiles[i][j];
					if (cTile != 0)
					{
						Tile tile;
						if (lVowels != null)
						{
							if ((tile = lVowels.grabTile(cTile)) != null)
							{
								m_board.setTile(i, j, tile);
								tile.setState(Tile.State.CONFIRMED);
								continue;
							}
						}
						if ((tile = lTiles.grabTile(cTile)) != null)
						{
							m_board.setTile(i, j, tile);
							tile.setState(Tile.State.CONFIRMED);
						}

						// If our sample tile is WILD, mark it as 'K' for testing.
						if (tile != null && cTile == Tile.WILD)
						{
							tile.setLetter('K');
						}
					}
				}
			}
		}

		// Get a string encoding of our game board.
		m_strBoard = m_board.getEncoding();

		// Add the default tiles to our rack.
		Rack rack = m_lRacks.get(0);
		lTiles = m_lTileBags.get(0);
		lVowels = null;
		if (m_lVowelBags != null)
		{
			lVowels = m_lVowelBags.get(0);
		}
		for (int i = 0; i < aTileRack.length; ++i)
		{
			char cTile = aTileRack[i];
			if (cTile != 0)
			{
				Tile tile;
				if (lVowels != null)
				{
					if ((tile = lVowels.grabTile(cTile)) != null)
					{
						rack.add(tile);
						continue;
					}
				}
				if ((tile = lTiles.grabTile(cTile)) != null)
				{
					rack.add(tile);
				}
			}
		}

		// Create our BestWord engine for finding the highest scoring words.
		m_bestWord = new BestWord(this);

		setState(State.DRAW_TILES);
	}

	//**********************************************************************************************
	private void
	bagTile(Tile tile)
	{
		int iPlayer = tile.getPlayer();
		if (m_lVowelBags != null && tile.isVowel())
		{
			m_lVowelBags.get(iPlayer).add(tile);
		}
		else
		{
			m_lTileBags.get(iPlayer).add(tile);
		}
	}

	//**********************************************************************************************
	public void
	clear()
	{
		// Bag all the tiles from the board.
		for (int i = 0; i < m_nBoardSize; ++i)
		{
			for (int j = 0; j < m_nBoardSize; ++j)
			{
				Tile tile = getTile(i, j);
				if (tile != null)
				{
					bagTile(tile);
				}
			}
		}

		// Reset the board.
		m_board.reset();
		m_boardBest = null;
	
		// Remove all tiles from the racks.
		for (Rack rack : m_lRacks)
		{
			int nTiles = rack.getRackSize();
			for (int iTile = 0; iTile < nTiles; ++iTile)
			{
				Tile tile = rack.remove(iTile);
				if (tile != null)
				{
					bagTile(tile);
				}
			}
		}

		// Clear the scores.
		m_iPlayerTurn = 0;
		for (int iPlayer = 0; iPlayer < m_nPlayers; ++iPlayer)
		{
			m_aPlayerScores[iPlayer] = 0;
		}
		m_nLastScore = 0;
		m_nTilesRobot = m_nTilesTotal;
	}

	//**********************************************************************************************
	public void
	clearOpponentScore()
	{
		if (m_dbGame != null)
		{
			int iOpponent = m_dbGame.getPlayer() ^ 1;
			m_aPlayerScores[iOpponent] = 0;
			m_dbGame.setScore(iOpponent, 0);
		}
	}

	//**********************************************************************************************
	public void
	confirmWord()
	{
		m_board.markTiles(Tile.State.SCORED, Tile.State.CONFIRMED);
	}

	//**********************************************************************************************
	public void
	delete()
	{
		clear();

		// Delete the database document, if we have one.
		if (m_dbGame != null)
		{
			m_dbGame.delete();
			m_dbGame = null;
		}
	}

	//**********************************************************************************************
	private void
	drawAllTiles()
	{
		// Make sure we don't have any placed tiles on the board.
		withdrawAllTiles();

		// Get access to our tile rack.
		Rack rack = getRack(m_iPlayerTurn);

		// Count up the number of vowels we have.
		int nRackVowels = rack.getVowelCount();

		// If there is room on our player's rack, see if we have letters to pick from.
		while (!isRackFilled(m_iPlayerTurn))
		{
			int nTiles = getTileCount(m_iPlayerTurn);
			int nVowels = getVowelCount(m_iPlayerTurn);

			// If there are no letters left to grab, we're done.
			if (nTiles == 0 && nVowels == 0)
			{
				break;
			}

			// If our vowel count is less than half, try to pick one.
			if (nVowels > 0)
			{
				if (nTiles == 0 || nRackVowels < (rack.getTileCount() >> 1))
				{
					++nRackVowels;
					grabVowel();
					continue;
				}
			}

			// Pick a normal tile.
			grabTile();
		}
	}

	//**********************************************************************************************
	private Tile
	drawTile(
		int iPlayer,
		char cLetter)
	{
		Tile tile = null;

		// If we have a vowel bag, try to grab it from this first.
		if (m_lVowelBags != null)
		{
			tile = m_lVowelBags.get(iPlayer).grabTile(cLetter);
		}

		// Else, try to grab it from the main bag.
		if (tile == null)
		{
			tile = m_lTileBags.get(iPlayer).grabTile(cLetter);
		}

		// If we still don't have this tile, try the user's rack.
		if (tile == null)
		{
			Rack rack = getRack(iPlayer);
			tile = rack.grabTile(cLetter);
		}

		return tile;
	}

	//**********************************************************************************************
	public void
	endGame(int iUser)
	{
		// Loop through the players to see if someone went out.
		int iOut = -1;
		int nTotalPoints = 0;
		for (int iPlayer = 0; iPlayer < m_nPlayers; ++iPlayer)
		{
			// Get the rack details.
			Rack rack = getRack(iPlayer);
			int nPoints = rack.getPoints();

			// If this player has no tiles in their rack or bag, they went out first.
			if (rack.getTileCount() == 0 &&
				getTileCount(iPlayer) + getVowelCount(iPlayer) == 0)
			{
				iOut = iPlayer;
			}

			// If each player has their own tile bag, get their points.
			if (!m_bBagShared)
			{
				nPoints += getTilePoints(iPlayer) + getVowelPoints(iPlayer);
			}

			// Subtract leftover points from the player's score.
			if ((m_aPlayerScores[iPlayer] -= nPoints) < 0)
			{
				m_aPlayerScores[iPlayer] = 0;
			}
			nTotalPoints += nPoints;
		}

		// Add the points leftover to the score of the player who when out.
		if (iOut >= 0)
		{
			m_aPlayerScores[iOut] += nTotalPoints;
		}

		// Update the player's highest and average game score.
		setGameEndScore(iUser);

		// Mark the game as over.
		setState(State.OVER);
	}

	//**********************************************************************************************
	public void
	findBestBoard(int iPlayer)
	{
		// Don't look for a new best word until it's own turn.
		if (iPlayer != m_iPlayerTurn || m_bBestWorking)
		{
			return;
		}

		// Avoid triggering a redundant search.
		m_bBestWorking = true;
		m_bNoPlay = false;
		m_boardBest = null;

		new Thread(new Runnable()
		{
			public void
			run()
			{
				try
				{
					// Play the next word.
					m_boardBest = getBestBoard();
					if (m_boardBest == null)
					{
						m_bNoPlay = true;
					}
				}
				catch(Exception e)
				{
					m_boardBest = null;
				}
				m_bBestWorking = false;
			}
		}).start();
	}

	//**********************************************************************************************
	public Board
	getBestBoard() throws Exception
	{
		// Make a copy of the rack.
		Rack rack = getRack(m_iPlayerTurn);
		m_rackBackup.copyTiles(rack);

		// Save the last score.
		int nLastScore = m_board.getLastScore(m_iPlayerTurn);
		
		// Find our BestWord.
		int nBestScore = m_bestWord.getBestWord(m_iPlayerTurn, m_rackBackup, 0, 0);
		if (nBestScore == 0)
		{
			return null;
		}

		// Output the best word to the console.
		//SPK - check for DEBUG mode: System.out.println("BESTWORD: " + m_bestWord.getBestWord());

		// Place our BestWord on the board.
		Board boardBest = new Board(m_board);
		m_bestWord.placeBestWord(m_iPlayerTurn, m_rackBackup, boardBest);

		// Restore the last score.
		m_board.setLastScore(m_iPlayerTurn, nLastScore);

		// Update last score to reflex the best word.
		boardBest.setLastScore(m_iPlayerTurn, nBestScore);
		boardBest.setLastScore(m_iPlayerTurn ^ 1, 0);

		return boardBest;
	}

	//**********************************************************************************************
	public Board
	getBoard()
	{
		return m_board;
	}

	//**********************************************************************************************
	public Board
	getBoardBest()
	{
		return m_boardBest;
	}

	//**********************************************************************************************
	public int
	getBoardHeight()
	{
		return m_nBoardSize;
	}

	//**********************************************************************************************
	public int
	getBoardSize()
	{
		return m_nBoardSize;
	}

	//**********************************************************************************************
	public int
	getBoardWidth()
	{
		return m_nBoardSize;
	}

	//**********************************************************************************************
	public DbGame
	getDbGame()
	{
		return m_dbGame;
	}

	//**********************************************************************************************
	public Square
	getFlingSquare(
		Tile tile,
		int iRackTile,
		boolean bRotated)
	{
		// Don't fling tiles onto the board while the computer is searching for the best word.
		if (m_bBestWorking)
		{
			return null;
		}

		// Get the placement of other tiles.
		Placement placement = m_board.getWordPlacement(Tile.State.PLACED);

		// If this is the first tile to be placed, then see if the rack matches the start
		// or end letter of the arrangement of our best word.
		if (placement == null)
		{
			// By default, we are not flinging letters in the placement of the best word.
			m_bFlingTop = false;
			m_iFlingDelta = 0;

			// Record the tile's rack location to compare with the next.
			m_iFlingLast = iRackTile;

			// See if we have a collection of best words.
			TopWords topWords = m_bestWord.getTopWords();
			if (topWords != null)
			{
				// Get the player's rack to see if the word the user has arranged is the same
				// as one of the top words.
				Rack rack = getRack(tile.getPlayer());
				char cLetter = tile.getLetter();

				for (int i = 0; i < topWords.m_nWords; ++i)
				{
					TopWord topWord = topWords.getTopWord(i);

					// Check for a forward match.
					if (isTopMatch(topWord, cLetter, rack, iRackTile, 1))
					{
						m_bFlingTop = true;
						return m_board.getSquare(
							topWord.m_placement.m_yFirst,
							topWord.m_placement.m_xFirst);
					}

					// Check for a reverse match.
					if (isTopMatch(topWord, cLetter, rack, iRackTile, -1))
					{
						m_bFlingTop = true;
						return m_board.getSquare(
							topWord.m_placement.m_yLast,
							topWord.m_placement.m_xLast);
					}
				}
			}

			// Without a hint on where to fling this tile, have it drop at their finger.
			return null;
		}

		// If we have at least two tiles in the placement, use its orientation.
		if (placement.m_xFirst != placement.m_xLast ||
			placement.m_yFirst != placement.m_yLast)
		{
			m_bFlingTop = false;
			m_iFlingDelta = 0;
		}

		// If we're flinging the word to match that of the best word, use its placement.
		else if (m_bFlingTop)
		{
			// Break from best word unless they fling the letters in order.
			if (iRackTile != m_iFlingLast + m_iFlingDelta)
			{
				m_bFlingTop = false;
				m_iFlingDelta = 0;
			}
		}

		// If we have no delta, determine this and the orientation.
		if (m_iFlingDelta == 0)
		{
			m_iFlingDelta = (iRackTile >= m_iFlingLast) ? 1 : -1;
			if (placement.m_yFirst != placement.m_yLast)
			{
				m_bFlingVertical = true;
			}
			else if (placement.m_xFirst != placement.m_xLast)
			{
				m_bFlingVertical = false;
			}
			else
			{
				// Test the board limits.
				int i, j;
				if ((m_bFlingVertical = bRotated))
				{
					i = placement.m_yLast + m_iFlingDelta;
					j = placement.m_xLast + m_iFlingDelta;
				}
				else
				{
					i = placement.m_xLast + m_iFlingDelta;
					j = placement.m_yLast + m_iFlingDelta;
				}

				// Switch the delta or verticalization if the fling would push past the edge.
				if (i < 0 || i >= m_nBoardSize)
				{
					// If we're on the wrong corner, then switch the delta.
					if (j < 0 || j >= m_nBoardSize)
					{
						m_iFlingDelta *= -1;
					}
					else
					{
						m_bFlingVertical ^= true;
					}
				}
			}
		}

		// Record the tile's rack location to compare with the next.
		m_iFlingLast = iRackTile;

		// Determine the next square location based on our placement and delta.
		int xNext, yNext;
		if (m_iFlingDelta > 0)
		{
			xNext = placement.m_xLast;
			yNext = placement.m_yLast;
		}
		else
		{
			xNext = placement.m_xFirst;
			yNext = placement.m_yFirst;
		}

		// If this is the first tile flung backward from the rack, override the default
		// horizonal orientation if we would have to jump over a played tile.
		if (!m_bFlingVertical &&
			m_board.getTilesPlaced(Tile.State.PLACED) == 1)
		{
			Square square = m_board.getSquare(yNext, xNext - 1);
			if (square != null && square.getTile() != null)
			{
				// If this tile does not start a top word, switch to vertical.
				TopWords topWords = m_bestWord.getTopWords();
				if (topWords == null || !topWords.isStart(square))
				{
					m_bFlingVertical = true;
					m_bFlingTop = false;
				}
			}
			else
			{
				square = m_board.getSquare(yNext, xNext + 1);
				if (square != null && square.getTile() != null)
				{
					m_bFlingVertical = true;
					m_bFlingTop = false;
				}
			}
		}

		// Attempt a vertical or horizontal word extending fling.
		while (true)
		{
			if (m_bFlingVertical)
			{
				yNext += m_iFlingDelta;
			}
			else
			{
				xNext += m_iFlingDelta;
			}

			// If we have an empty square, place the tile here.
			Square square = m_board.getSquare(yNext, xNext);
			if (square == null)
			{
				break;
			}
			if (square.getTile() == null)
			{
				return square;
			}
		}

		return null;
	}

	//**********************************************************************************************
	public int
	getHighScore(int iPlayer)
	{
		if (m_dbGame != null)
		{
			int iScore = m_dbGame.getHighScore(iPlayer);
			if (iScore > 0)
			{
				return iScore;
			}
		}
		return getScore(iPlayer);
	}

	//**********************************************************************************************
	public Set<String>
	getInvalidWords()
	{
		return m_board.getInvalidWords();
	}

	//**********************************************************************************************
	public String
	getOpponentInvalids()
	{
		if (m_dbGame != null && !m_bSawInvalids)
		{
			String strInvalids = m_dbGame.getInvalids();
			if (strInvalids != null)
			{
				m_bSawInvalids = true;
				return strInvalids;
			}
		}
		return null;
	}

	//**********************************************************************************************
	public String
	getOpponentMessage()
	{
		if (m_strMessage == null && m_dbGame != null)
		{
			m_strMessage = m_dbGame.getOpponentMessage();
		}
		String strMessage = m_strMessage;
		m_strMessage = null;
		return strMessage;
	}

	//**********************************************************************************************
	public String
	getOpponentName()
	{
		if (m_dbGame != null)
		{
			return m_dbGame.getOpponentName();
		}
		return null;
	}

	//**********************************************************************************************
	public int
	getPlayer()
	{
		return m_iPlayerTurn;
	}

	//**********************************************************************************************
	public int
	getPlayerCount()
	{
		return m_nPlayers;
	}

	//**********************************************************************************************
	public String
	getPlayerName(int iPlayer)
	{
		if (m_dbGame != null)
		{
			return m_dbGame.getUserName(iPlayer);
		}
		if (isPlayerRobot(iPlayer))
		{
			return DbUser.GetRobotUserName();
		}
		return DbUser.GetDefaultUserName();
	}

	//**********************************************************************************************
	public PlayStatus
	getPlayStatus()
	{
		return m_board.getPlayStatus();
	}

	//**********************************************************************************************
	public Rack
	getRack(int iPlayer)
	{
		return m_lRacks.get(iPlayer);
	}

	//**********************************************************************************************
	public int
	getRackSize()
	{
		return m_nRackSize;
	}

	//**********************************************************************************************
	public int
	getRackTileCount(int iPlayer)
	{
		return m_lRacks.get(iPlayer).getTileCount();
	}

	//**********************************************************************************************
	public int
	getRackVowelCount(int iPlayer)
	{
		return m_lRacks.get(iPlayer).getVowelCount();
	}

	//**********************************************************************************************
	public int
	getRanking(int iPlayer)
	{
		if (m_dbGame != null)
		{
			return m_dbGame.getRanking(iPlayer);
		}
		return 0;
	}

	//**********************************************************************************************
	public int
	getScore(int iPlayer)
	{
		return m_aPlayerScores[iPlayer];
	}

	//**********************************************************************************************
	public State
	getState()
	{
		if (!m_mainActivity.isInThread() && m_eState == State.ROBOT_PASSED)
		{			
			// Skipping the robot turn.
			m_mainActivity.setAlert("Passing my turn");
			setState(State.DRAW_TILES);
		}
		return m_eState;
	}

	//**********************************************************************************************
	public Tile
	getTile(
		int iRow,
		int iColumn)
	{
		return m_board.getTile(iRow, iColumn);
	}

	//**********************************************************************************************
	public Tiles
	getTileBag(int iPlayer)
	{
		return m_lTileBags.get(iPlayer);
	}

	//**********************************************************************************************
	public int
	getTileCount(int iPlayer)
	{
		return m_lTileBags.get(iPlayer).size();
	}

	//**********************************************************************************************
	public int
	getTilePoints(int iPlayer)
	{
		return m_lTileBags.get(iPlayer).getPoints();
	}

	//**********************************************************************************************
	public int
	getTilesLeft(int iPlayer)
	{
		return getTileCount(iPlayer) + getVowelCount(iPlayer) + getRack(iPlayer).getTileCount();
	}

	//**********************************************************************************************
	public String
	getTilesLeftAlert(int iPlayer)
	{
		int nTiles = getTilesLeft(iPlayer);
		if (nTiles != m_nOpponentTiles)
		{
			m_nOpponentTiles = nTiles;
			if (nTiles > 0 && nTiles <= getRack(iPlayer).getRackSize())
			{
				return m_dbGame.getOpponentName() + " has " + nTiles + " tiles left.";
			}
		}
		return null;
	}

	//**********************************************************************************************
	public Tiles
	getVowelBag(int iPlayer)
	{
		if (m_lVowelBags != null)
		{
			return m_lVowelBags.get(iPlayer);
		}
		return null;
	}

	//**********************************************************************************************
	public int
	getVowelCount(int iPlayer)
	{
		if (m_lVowelBags != null)
		{
			return m_lVowelBags.get(iPlayer).size();
		}
		return 0;
	}

	//**********************************************************************************************
	public int
	getVowelPoints(int iPlayer)
	{
		if (m_lVowelBags != null)
		{
			return m_lVowelBags.get(iPlayer).getPoints();
		}
		return 0;
	}

	//**********************************************************************************************
	public Words
	getWords()
	{
		return m_words;
	}

	//**********************************************************************************************
	public boolean
	grabTile()
	{
		return grabTile(m_iPlayerTurn);
	}

	//**********************************************************************************************
	public boolean
	grabTile(int iPlayer)
	{
		return grabTile(m_lTileBags.get(iPlayer), iPlayer);
	}

	//**********************************************************************************************
	public boolean
	grabTile(
		Tiles bag,
		int iPlayer)
	{
		// Can't grab more tiles when a tile has been placed on the board.
		if (!m_board.isTilePlacedOrPlayed(iPlayer))
		{
			// If we grabbed a tile, check to see if there are still more to grab.
			Rack rack = getRack(iPlayer);
			if (bag.grab(rack))
			{
				// For non-robots, update our state and rack db table.
				if (!isPlayerRobot(iPlayer))
				{
					// If the rack is full, switch states.
					if (isRackFilled(iPlayer))
					{
						// Save the updated rack to the database.
						if (m_dbGame != null)
						{
							m_dbGame.pushRack(iPlayer, rack.getEncoding());
						}

						// Switch the state only if it is our turn.
						if (iPlayer == m_iPlayerTurn)
						{
							if (m_board.isEmpty())
							{
								setState(State.FIRST_WORD);
							}
							else
							{
								setState(State.PLACE_TILES);
							}
						}
					}
				}
				return true;
			}
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	grabVowel()
	{
		return grabVowel(m_iPlayerTurn);
	}

	//**********************************************************************************************
	public boolean
	grabVowel(int iPlayer)
	{
		// Make sure we have a vowel tile bag to draw from.
		if (m_lVowelBags != null)
		{
			return grabTile(m_lVowelBags.get(iPlayer), iPlayer);
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isBagFull(int iPlayer)
	{
		int nTiles = m_lTileBags.get(iPlayer).size();
		if (m_lVowelBags != null)
		{
			nTiles += m_lVowelBags.get(iPlayer).size();
		}
		
		return nTiles >= m_nTilesTotal;
	}

	//**********************************************************************************************
	public boolean
	isBoardEmpty()
	{
		return m_board.isEmpty();
	}

	//**********************************************************************************************
	public boolean
	isEmpty()
	{
		return m_board.isEmpty();
	}

	//**********************************************************************************************
	public boolean
	isGameReset()
	{
		return m_bGameReset;
	}

	//**********************************************************************************************
	public boolean
	isGameSwitched()
	{
		return m_bGameSwitched;
	}

	//**********************************************************************************************
	public boolean
	isMyTurn()
	{
		if (m_dbGame != null)
		{
			return m_dbGame.isMyTurn();
		}
		return m_iPlayerTurn != m_iPlayerRobot;
	}

	//**********************************************************************************************
	public boolean
	isNoPlay()
	{
		if (m_bNoPlay)
		{
			m_bNoPlay = false;
			return true;
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isQuit()
	{
		return m_eState == State.QUIT;
	}

	//**********************************************************************************************
	public boolean
	isOver()
	{
		return m_eState == State.OVER || m_eState == State.QUIT;
	}

	//**********************************************************************************************
	public boolean
	isPlayerRobot()
	{
		return isPlayerRobot(m_iPlayerTurn);
	}

	//**********************************************************************************************
	public boolean
	isPlayerRobot(int iPlayer)
	{
		return iPlayer == m_iPlayerRobot;
	}

	//**********************************************************************************************
	public boolean
	isRackEmpty(int iPlayer)
	{
		// The rack wasn't empty if we placed a tile on the board.
		if (m_board.isTilePlacedOrPlayed(iPlayer))
		{
			return false;
		}

		return m_lRacks.get(iPlayer).isEmpty();
	}

	//**********************************************************************************************
	public boolean
	isRackFilled(int iPlayer)
	{
		// While a letter is being played, it might look like our rack has room.
		if (iPlayer == m_iRackFilled)
		{
			return true;
		}

		do
		{
			// Can't grab more tiles when a tile has been placed on the board.
			if (m_board.isTilePlacedOrPlayed(iPlayer))
			{
				break;
			}

			// If the rack is really full, then true.
			if (m_lRacks.get(iPlayer).isFull())
			{
				break;
			}

			// If we have no more tiles to grab, then true as well.
			if (m_lTileBags.get(iPlayer).size() != 0)
			{
				return false;
			}
			if (m_lVowelBags != null &&
				m_lVowelBags.get(iPlayer).size() != 0)
			{
				return false;
			}
		} while (false);

		m_iRackFilled = iPlayer;
		return true;
	}

	//**********************************************************************************************
	public boolean
	isRobot()
	{
		return m_dbGame == null || m_dbGame.isRobot();
	}

	//**********************************************************************************************
	public boolean
	isRobotWorking()
	{
		return m_bRobotWorking;
	}

	//**********************************************************************************************
	public boolean
	isTilePlaced()
	{
		if (!isWorking())
		{
			for (int i = 0; i < m_nBoardSize; ++i)
			{
				for (int j = 0; j < m_nBoardSize; ++j)
				{
					Square square = m_board.getSquare(i, j);
					Tile tile = square.getTile();
					if (tile != null)
					{
						switch (tile.getState())
						{
							case PLACED:
								return true;
						}
					}
				}
			}
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isTilesLow(int iPlayer)
	{
		Rack rack = getRack(iPlayer);
		return
			getTileCount(iPlayer) + getVowelCount(iPlayer) + rack.getTileCount()
			<= rack.getRackSize();
	}

	//**********************************************************************************************
	// Return true if the arranged word on the rack matches our best word.
	private boolean
	isTopMatch(
		TopWord topWord,
		char cLetter,
		Rack rack,
		int iRackTile,
		int iDelta)
	{
		int nLength = topWord.getLength();
		if (nLength > 0)
		{
			m_iFlingDelta = iDelta;

			// Determine the alignment of the top word.
			m_bFlingVertical = false;
			Placement placement = topWord.getPlacement();
			if (placement.m_yFirst != placement.m_yLast)
			{
				m_bFlingVertical = true;
			}

			// By default, assume we are doing a forward compare.
			int iWord = 0;
			int x = placement.m_xFirst;
			int y = placement.m_yFirst;

			// For negative detals, it's a reverse compare.
			if (iDelta < 0)
			{
				iWord = nLength - 1;
				x = placement.m_xLast;
				y = placement.m_yLast;
			}

			// Run the comparison.
			while (nLength-- > 0)
			{
				// Check for a mismatch.
				if (cLetter != Tile.WILD && cLetter != topWord.charAt(iWord))
				{
					// Allow tiles on the board to be built across.
					if (getTile(y, x) == null)
					{
						return false;
					}
				}
				else
				{
					cLetter = rack.getLetter(iRackTile += iDelta);
				}

				// Jump to the next board sqaure.
				if (m_bFlingVertical)
				{
					y += iDelta;
				}
				else
				{
					x += iDelta;
				}
				iWord += iDelta;
			}
			return true;
		}
		return false;
	}

	//**********************************************************************************************
	// Return true if the arranged word on the rack matches our best word.
	private boolean
	isTopMatch(
		Rack rack,
		int yFirst,
		int xFirst,
		int yLast,
		int xLast,
		int iRack,
		int iDelta)
	{
		m_bFlingVertical = (yFirst != yLast);
		m_iFlingDelta = iDelta;
		
		while (true)
		{
			// Move to the next tile.
			if (m_bFlingVertical)
			{
				yFirst += iDelta;
			}
			else
			{
				xFirst += iDelta;
			}
			iRack += iDelta;

			// See if we have a tile match.
			Tile tile = rack.get(iRack);
			if (tile == null)
			{
				break;
			}
			if (!tile.isEquals(m_boardBest.getTile(yFirst, xFirst)))
			{
				break;
			}

			// If that was our last tile in the pattern, we have a match.
			if (xFirst == xLast && yFirst == yLast)
			{
				return true;
			}
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isWorking()
	{
		return m_bBestWorking || m_bRobotWorking;
	}

	//**********************************************************************************************
	public void
	jumbleRack(int iPlayer)
	{
		m_lRacks.get(iPlayer).jumble();
	}

	//**********************************************************************************************
	public void
	moveRobot() throws Exception
	{
		m_bRobotWorking = true;

		// Make sure our rack is full with available tiles.
		drawAllTiles();

		// If this is the user's first game, try to even the score.
		int nScore = m_aPlayerScores[m_iPlayerRobot ^ 1] - m_aPlayerScores[m_iPlayerRobot];
		if (nScore < 20)
		{
			nScore = 20;
		}

		// If available, get the player's ranking.
		int nRanking = 0;
		if (m_dbGame != null)
		{
			nRanking = m_dbGame.getRanking(m_iPlayerRobot ^ 1);
		}

		// Find our BestWord.
		Rack rack = getRack(m_iPlayerTurn);
		int nBestScore = m_bestWord.getBestWord(m_iPlayerTurn, rack, nRanking, nScore);

		// If there's no move, try to trade in letters.
		if (nBestScore == 0)
		{
			// Skip the robot's turn, but avoid repeated skips.
			if (m_nLastScore <= 0)
			{
				if (--m_nLastScore <= - m_nPlayers)
				{
					m_bRobotWorking = false;
					endGame(m_iPlayerTurn ^ 1);
					return;
				}
			}

			// Record this as a zero score.
			m_nLastScore = 0;

			// Switch back to the human player.
			switchPlayer();

			// Turn off race condition blocker.
			m_bRobotWorking = false;

			// Set the state so an alert message can be posted on the loop thread.
			setState(State.ROBOT_PASSED);
			return;
		}

		// Place our BestWord on the board.
		m_bestWord.placeBestWord(m_iPlayerTurn, rack, m_board);

		// Play and score the word.
		if (!playWord(m_iPlayerTurn ^ 1).isValid())
		{
			throw new Exception("Placed BestWord could not be played.");
		}

		m_bRobotWorking = false;
	}

	//**********************************************************************************************
	public PlayStatus
	playWord(int iUser)
	{
		// Try to play the word.
		PlayStatus eStatus = m_board.playWord();

		do
		{
			// Break out if the arrangement is invalid.
			if (!eStatus.isValid())
			{
				break;
			}
			
			// Check to see if only valid words can be played.
			if ((m_bmSettings & s_bmChallengeMode) == 0)
			{
				Set<String> sInvalidWords = getInvalidWords();
				if (sInvalidWords != null && !sInvalidWords.isEmpty())
				{
					eStatus = PlayStatus.INVALID_WORD;
					if (m_dbGame != null)
					{
						m_dbGame.setInvalids(sInvalidWords);
					}
					break;
				}
			}

			// Have a valid play.
			int iPlayer = m_iPlayerTurn;
			scoreWord(iUser);

			if (m_dbGame != null &&
				iPlayer == m_iPlayerRobot &&
				!isOver())
			{
				String strMessage = m_dbGame.getUserMessage();
				if (strMessage != null && !strMessage.isEmpty())
				{
					// Build a dataset of responses.
					m_dbGame.setMessage("Thanks for showing this robot a little love.", iPlayer);
				}
			}
			
			postMove(iPlayer);
		} while (false);

		// If it's an invalid play, withdraw all the letters.
		if (!eStatus.isValid())
		{
			withdrawAllTiles();
		}

		// Reset the fling direction.
		m_iFlingDelta = 0;

		return eStatus;
	}

	//**********************************************************************************************
	public void
	postMove(int iPlayer)
	{
		if (m_dbGame != null)
		{
			// Check to make sure we didn't lose sync.  Don't reduce the other player's
			// score.
			if (!isRobot() &&
				!isOver() &&
				m_dbGame.getScore(iPlayer ^ 1) != getScore(iPlayer ^ 1))
			{
				m_mainActivity.setAlert("Sync failure. Please try again.");
				updateGame(false);
				return;
			}
			
			m_dbGame.setLastScore(iPlayer, m_board.getLastScore(iPlayer));
			m_dbGame.setLastSquare(iPlayer, m_board.getLastSquare(iPlayer));
			m_dbGame.setMove(m_strBoard = m_board.getEncoding());
			m_dbGame.setRack(iPlayer, getRack(iPlayer).getEncoding());
			m_dbGame.setScore(iPlayer, getScore(iPlayer));
			m_dbGame.setScore(iPlayer ^ 1, getScore(iPlayer ^ 1));

			// If the game is over, post the negative player turn to indicate this.
			if (!isRobot() && isOver())
			{
				m_dbGame.setPlayerTurn(-1 - iPlayer);
			}
			else
			{
				m_dbGame.setPlayerTurn(m_iPlayerTurn);
			}
			
			m_dbGame.pushData();

			// Mark saw-invalids flag to false so we present them, but only once.
			m_bSawInvalids = false;
		}
	}

	//**********************************************************************************************
	public void
	postReset()
	{
		if (m_dbGame != null)
		{
			m_dbGame.setLastScore(0, 0);
			m_dbGame.setLastScore(1, 0);
			m_dbGame.setLastSquare(0, 0);
			m_dbGame.setLastSquare(1, 0);
			m_dbGame.setMove("");
			m_dbGame.setRack(0, getRack(0).getEncoding());
			m_dbGame.setRack(1, getRack(1).getEncoding());
			m_dbGame.setScore(0, 0);
			m_dbGame.setScore(1, 0);
			m_dbGame.setPlayerTurn(m_iPlayerTurn);
			m_dbGame.clearInvalids();
			m_dbGame.pushData();
			m_bSawInvalids = false;
		}
	}

	//**********************************************************************************************
	public void
	reset()
	{
		// Clear the board, rack and score.
		clear();
		
		// Let the GUI know that a new game has been reset.
		setGameReset(true);

		if (m_dbGame != null)
		{
			if (m_dbGame.isRobot())
			{
				// Save the reset game to the database.
				postReset();
			}
			else
			{
				String strGameId = DbGame.PostGame(m_dbGame);
				DbUser dbUser1 = m_dbGame.getUser();
				DbUser dbUser2 = m_dbGame.getOpponent();
				m_dbGame.delete();
				m_dbGame = new DbGame(strGameId, dbUser1, dbUser2, null);
			}
		}

		// Game is reset.
		setState(State.DRAW_TILES);
	}

	//**********************************************************************************************
	public int
	scoreWord(int iUser)
	{
		// Score our word and add its value to the player's score total.
		m_nLastScore = m_board.scoreWord();
		m_aPlayerScores[m_iPlayerTurn] += m_nLastScore;

		// If the player is out of tiles, then mark the game as over.
		if (getRack(m_iPlayerTurn).getTileCount() == 0 &&
			getTileCount(m_iPlayerTurn) + getVowelCount(m_iPlayerTurn) == 0)
		{
			endGame(iUser);
		}
		else
		{
			// Switch the player turn.
			switchPlayer();
		}

		// By default, we do not have a filled rack.
		m_iRackFilled = -1;

		// Return the last score.
		return m_nLastScore;
	}

	//**********************************************************************************************
	public void
	sendMessage(String strMessage)
	{
		if (m_dbGame != null)
		{
			m_dbGame.pushMessage(strMessage);
		}
	}

	//**********************************************************************************************
	public void
	setDbGame(DbGame dbGame)
	{
	    m_dbGame = dbGame;

		// If we're resuming a game, see if there are tiles to put on our rack.
		if (dbGame != null)
		{
			// Let the GUI know that our game has a connection to the database.
			setGameReset(true);

			int iPlayer = dbGame.getPlayer();
			String strRack = dbGame.getRack();
			if (strRack != null)
			{
				Rack rack = getRack(iPlayer);
				if (strRack.length() > rack.getTileCount())
				{
					// Bag all tiles from this rack.
					for (int iTile = 0; iTile < rack.getRackSize(); ++iTile)
					{
						Tile tile = rack.remove(iTile);
						if (tile != null)
						{
							bagTile(tile);
						}
					}

					// Grab a tile for each letter in the game rack and put it on our rack.
					for (int iTile = 0; iTile < strRack.length(); ++iTile)
					{
						Tile tile = drawTile(iPlayer, strRack.charAt(iTile));
						if (tile != null)
						{
							rack.add(tile);
						}
					}
				}
			}

			// If the game has been quit, indicate this.
			if (dbGame.getPlayerTurn() < 0)
			{
				setState(State.OVER);
				if (dbGame.getScoreOpponent() == 0)
				{
					setState(State.QUIT);
				}
			}
		}
	}

	//**********************************************************************************************
	public void
	setGameEndScore(int iUser)
	{
		if (m_dbGame != null)
		{
			// Update the player's highest and average game score.
			DbUser dbUser = m_dbGame.getUser();
			if (dbUser != null)
			{
				dbUser.setFinalGameScore(m_aPlayerScores[iUser]);
			}
		}
	}

	//**********************************************************************************************
	public void
	setGameReset(boolean bReset)
	{
		m_bGameReset = bReset;
	}

	//**********************************************************************************************
	public void
	setGameSwitched(boolean bSwitched)
	{
		m_bGameSwitched = bSwitched;
	}

	//**********************************************************************************************
	public void
	setRackTileIndex(int iRackTile)
	{
		m_iFlingLast = iRackTile;
		m_iFlingDelta = 0;
	}

	//**********************************************************************************************
	public void
	setScore(
		int iPlayer,
		int nScore)
	{
		m_aPlayerScores[iPlayer] = nScore;
	}

	//**********************************************************************************************
	public void
	setSettings()
	{
		m_bmSettings = 0;
		if (m_dbGame != null)
		{
			m_bmSettings = m_dbGame.getSettings();
		}
	}

	//**********************************************************************************************
	public void
	setState(State eState)
	{
		m_eState = eState;
		switch (eState)
		{
			case DRAW_TILES:
			case MOVE_ROBOT:
			case OVER:
				m_iRackFilled = -1;
				break;

			case PLACE_TILES:
				if (isPlayerRobot())
				{
					setState(State.MOVE_ROBOT);
					break;
				}
				if (m_board.getPlayStatus() == PlayStatus.VALID)
				{
					m_eState = State.PLAY_READY;
				}
				//fallthru;
			case PLAY_READY:
				m_iRackFilled = m_iPlayerTurn;
				break;

			case PLAY_TILES:
				m_iRackFilled = m_iPlayerTurn;
				if (m_board.getPlayStatus() == PlayStatus.VALID)
				{
					m_eState = State.PLAY_READY;
				}
				break;
		}
	}

	//**********************************************************************************************
	public void
	skipTurn()
	{
		withdrawAllTiles();

		if (m_nLastScore == 0)
		{
			if (--m_nLastScore <= - m_nPlayers)
			{
				m_bRobotWorking = false;
				endGame(m_iPlayerTurn ^ 1);
			}
			else
			{
				switchPlayer();
			}
		}
		else
		{
			switchPlayer();
			m_nLastScore = 0;
		}
		
		if (m_dbGame != null)
		{
			m_dbGame.setPlayerTurn(m_iPlayerTurn);
			m_dbGame.pushData();
			m_bSawInvalids = false;
		}
	}

	//**********************************************************************************************
	public void
	switchPlayer()
	{
		// Switch the player turn.
		if (++m_iPlayerTurn >= m_nPlayers)
		{
			m_iPlayerTurn = 0;
		}

		// Update the state.
		if (isPlayerRobot())
		{
			setState(State.MOVE_ROBOT);
		}
		else if (isRackFilled(m_iPlayerTurn))
		{
			setState(State.PLACE_TILES);
		}
		else
		{
			setState(State.DRAW_TILES);
		}
	}

	//**********************************************************************************************
	public boolean
	testRackedTiles()
	{
		boolean bRackedTiles = m_bRackedTiles;
		m_bRackedTiles = false;
		return bRackedTiles;
	}

	//**********************************************************************************************
	public String
	toString()
	{
		StringBuilder sbBoard = new StringBuilder();

		sbBoard.append(m_board.toString());

		// If we have a last score, display this.
		if (m_nLastScore > 0)
		{
			sbBoard.append("Last score: ");
			sbBoard.append(Integer.toString(m_nLastScore));
			sbBoard.append('\n');
		}

		// Generate a score, rack and bag tile count for each player.
		for (int iPlayer = 0; iPlayer < m_nPlayers; ++iPlayer)
		{
			sbBoard.append("Player(");
			sbBoard.append(Integer.toString(iPlayer + 1));
			sbBoard.append(") Score(");
			sbBoard.append(Integer.toString(m_aPlayerScores[iPlayer]));
			if (m_lVowelBags == null)
			{
				sbBoard.append(") Tiles(");
			}
			else
			{
				sbBoard.append(") Vowels(");
				sbBoard.append(Integer.toString(m_lVowelBags.get(iPlayer).size()));
				sbBoard.append(") Consonants(");
			}
			sbBoard.append(Integer.toString(m_lTileBags.get(iPlayer).size()));
			sbBoard.append(") Rack(");
			sbBoard.append(m_lRacks.get(iPlayer).toString());
			sbBoard.append(")\n");
		}

		return sbBoard.toString();
	}

	//**********************************************************************************************
	public void
	updateGame(boolean bAlert)
	{
		// Count up the number of opponent tiles so we can alert the user when the
		// count is a rack size or less.
		int iUser = m_dbGame.getPlayer();
		int iOpponent = iUser ^ 1;
		int nTiles = 0;

		// If we have an updated move in the database, set this to our board.
		String strBoard = m_dbGame.getMove();
		if (strBoard != null &&
			strBoard.length() ==
			m_nBoardSize + (Tile.ENCODING_SIZE * m_nBoardSize * m_nBoardSize) &&
			!m_strBoard.equals(strBoard))
		{
			// Avoid updating again.
			m_strBoard = strBoard;

			// Update our game board.
			int iBoard = 0;
			for (int iRow = 0; iRow < m_nBoardSize; ++iRow)
			{
				for (int iColumn = 0; iColumn < m_nBoardSize; ++iColumn)
				{
					char cFlags = strBoard.charAt(iBoard++);
					char cLetter = strBoard.charAt(iBoard++);
					if (cLetter < Tile.ALPHA)
					{
						continue;
					}

					// Get the tile's player so we can count the opponent tiles.
					int iPlayer = Tile.GetEncodedPlayer(cFlags);
					if (iPlayer == iOpponent)
					{
						++nTiles;
					}

					// Skip over squares that are already in sync.
					if (cLetter == m_board.getLetter(iRow, iColumn))
					{
						continue;
					}

					if (m_board.getLetter(iRow, iColumn) != 0)
					{
						//throw new Exception("Board out of sync with database.");
						return;
					}

					// Draw the tile from the player's rack and play it on the board.
					boolean bWildcard = Tile.IsEncodedWildcard(cFlags);
					Tile tile = drawTile(iPlayer, bWildcard ? Tile.WILD : cLetter);
					if (tile == null)
					{
						continue;
					}

					// If we have a wildcard, set the board letter to this.
					if (bWildcard)
					{
						tile.setLetter(cLetter);
					}

					// Set the board tile
					m_board.setTile(iRow, iColumn, tile);
					tile.setState(Tile.State.CONFIRMED);
					m_board.setEmpty(false);
				}
				++iBoard;
			}

			// If there is a connection issue to the database which caused placed tiles to be
			// lost, then remove those tiles that are potentially disconnected from the others.
			withdrawStrayTiles();
		}
		
		// If we have a robot game, count the tiles from its rack and bags.
		if (isRobot())
		{
			nTiles =
				m_lRacks.get(m_iPlayerRobot).getTileCount() +
				getTileCount(m_iPlayerRobot) +
				getVowelCount(m_iPlayerRobot);
			if (nTiles < m_nTilesRobot)
			{
				m_nTilesRobot = nTiles;
				nTiles = m_nTilesTotal - nTiles;
			}
			else
			{
				nTiles = 0;
			}
		}
		else
		{
			// Get the active player.
			if ((m_iPlayerTurn = m_dbGame.getPlayerTurn()) < 0)
			{
				m_iPlayerTurn = 0;
			}
		}

		// Update scores.
		for (int iPlayer = 0; iPlayer < m_nPlayers; ++iPlayer)
		{
			setScore(iPlayer, m_dbGame.getScore(iPlayer));

			// Set the score of the last played word.
			m_board.setLastScore(iPlayer, m_dbGame.getLastScore(iPlayer));
			m_board.setLastSquare(iPlayer, m_dbGame.getLastSquare(iPlayer));
		}

		// If we are actually viewing this game, make sure to provide alerts.
		if (bAlert)
		{
			// If there are no more tiles, the game is over.
			Rack rack = m_lRacks.get(0);
			if (!isOver())
			{
				if (!m_board.isTilePlacedOrPlayed(-1) &&
					(rack.getTileCount() + getTileCount(0) + getVowelCount(0) == 0 ||
					 m_lRacks.get(1).getTileCount() + getTileCount(1) + getVowelCount(1) == 0))
				{
					setGameEndScore(iUser);
					setState(State.OVER);
				}
			}
		}
	}

	//**********************************************************************************************
	public void
	updateState()
	{
		if (m_dbGame != null && m_dbGame.isUpdated())
		{
			updateGame(true);
		}
	}

	//**********************************************************************************************
	public boolean
	withdrawAllTiles()
	{
		boolean bFound = false;
		for (int i = 0; i < m_nBoardSize; ++i)
		{
			for (int j = 0; j < m_nBoardSize; ++j)
			{
				Square square = m_board.getSquare(i, j);
				Tile tile = square.getTile();
				if (tile != null)
				{
					switch (tile.getState())
					{
						case PLACED:
						case PLAYED:
							square.setTile(null, false);
							m_lRacks.get(tile.getPlayer()).add(tile);
							bFound = true;
							break;
						default:
							break;
					}
				}
			}
		}
		m_bRackedTiles = true;

		// Change the state.
		setState(State.PLACE_TILES);

		// Clear any placed score.
		m_board.clearPlacedScore();

		// Reset the fling direction.
		m_iFlingDelta = 0;
		
		return bFound;
	}
	
	//**********************************************************************************************
	public boolean
	withdrawStrayTiles()
	{
		// Mark all tiles as found which are connected to tiles from the start location.
		m_board.markConnectedTiles();

		boolean bFound = false;
		for (int i = 0; i < m_nBoardSize; ++i)
		{
			for (int j = 0; j < m_nBoardSize; ++j)
			{
				Square square = m_board.getSquare(i, j);
				Tile tile = square.getTile();
				if (tile != null)
				{
					if (!tile.isFound())
					{
						square.setTile(null, false);
						m_lRacks.get(tile.getPlayer()).add(tile);
						bFound = true;
					}
				}
			}
		}
		return bFound;
	}
}

//**************************************************************************************************
