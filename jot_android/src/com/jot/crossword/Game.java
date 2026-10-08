/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword game board.
 */

package com.jot.crossword;

import com.jot.decomp.Words;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

//**************************************************************************************************

public class Game //implements ImageObserver
{
	public class GameKey implements KeyListener
	{
		int m_key;
		
		public
		GameKey()
		{
			JTextField textField = new JTextField(100);
			textField.addKeyListener(this);
			m_key = 0;
		}

		@Override
		public void
		keyTyped(KeyEvent e)
		{
			m_key = e.getKeyCode();
		}

		@Override
		public void
		keyPressed(KeyEvent e)
		{
			m_key = e.getKeyCode();
		}

		public int
		getKey()
		{
			int key = m_key;
			m_key = 0;
			return key;
		}

		@Override
		public void keyReleased(KeyEvent e)
		{
		}
	}
	
	// An enumeration set of bonus squares.
	static final byte NA = (byte)Bonus.NA.ordinal();
	static final byte L2 = (byte)Bonus.L2.ordinal();
	static final byte L3 = (byte)Bonus.L3.ordinal();
	static final byte L4 = (byte)Bonus.L4.ordinal();
	static final byte W2 = (byte)Bonus.W2.ordinal();
	static final byte W3 = (byte)Bonus.W3.ordinal();
	static final byte W4 = (byte)Bonus.W4.ordinal();

	protected Board m_board;

	protected List<Tiles> m_lTileRacks;
	protected List<Tiles> m_lTileBags;
	protected List<Tiles> m_lVowelBags;
	protected byte[] m_aTileScores;

	protected int m_iPlayer;
	protected int m_nPlayers;
	protected int[] m_aPlayerScores;
	protected int m_nLastScore;
	protected int m_nRackSize;
	protected int m_nBoardSize;
	protected boolean m_bBagShared;
	protected boolean m_bOver;

	protected Tile m_tileSelect;

	protected GameKey m_keyInput;

	protected Bitmap m_bitmap;

	protected Words m_words;
	protected BestWord m_bestWord;

	// TODO - support bingo prize amount.
	protected int m_nBingoPrize = 50;

	//**********************************************************************************************
	public
	Game(
		final Bitmap[] aBitmaps,
		final byte[][] aaBonuses,
		final char[][] aaDefaultTiles,
		final char[] aTileBag,
		final byte[] aTileScores,
		final char[] aTileRack,
		Words words,
		int nPlayers
	) throws Exception
	{
		this(
			aBitmaps,
			aaBonuses,
			aaDefaultTiles,
			aTileBag,
			null,
			aTileScores,
			aTileRack,
			50,
			words,
			nPlayers,
			true);
	}

	//**********************************************************************************************
	public
	Game(
		final Bitmap[] aBitmaps,
		final byte[][] aaBonuses,
		final char[][] aaDefaultTiles,
		final char[] aTileBag,
		final char[] aVowelBag,
		final byte[] aTileScores,
		final char[] aTileRack,
		int nBingoPrize,
		Words words,
		int nPlayers,
		boolean bBagShared
	) throws Exception
	{
		m_board = new Board(
			aaBonuses,
			nBingoPrize,
			aTileRack.length,
			words,
			aaDefaultTiles,
			aTileScores,
			aBitmaps);
		m_nBoardSize = m_board.getSize();
		m_nPlayers = nPlayers;
		m_bBagShared = bBagShared;
		m_aTileScores = aTileScores;
		m_nBingoPrize = nBingoPrize;
		m_words = words;
		
		// Create an array player scores.
		m_aPlayerScores = new int[nPlayers];
		m_nLastScore = 0;
	
		// Create a list of tile racks.
		m_nRackSize = aTileRack.length;
		m_lTileRacks = new ArrayList<>();
		for (int iPlayer = 0; iPlayer < nPlayers; ++iPlayer)
		{
			m_lTileRacks.add(new Tiles(m_nRackSize));
		}

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
				lTiles = new Tiles(0);
				for (int iTile = 0; iTile < aTileBag.length; ++iTile)
				{
					char cTile = aTileBag[iTile];
					Bitmap bitmap = null;
					if (aBitmaps != null)
					{
						bitmap = aBitmaps[cTile];
					}
					lTiles.add(new Tile(cTile, aTileScores[cTile], bitmap));
				}

				// If needed separate the vowels.
				if (m_lVowelBags != null)
				{
					lVowels = new Tiles(0);
					for (int iVowel = 0; iVowel < aVowelBag.length; ++iVowel)
					{
						char cVowel = aVowelBag[iVowel];
						lVowels.add(new Tile(cVowel, aTileScores[cVowel], aBitmaps[cVowel]));
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
								tile.setState(Tile.State.SCORED);
								continue;
							}
						}
						if ((tile = lTiles.grabTile(cTile)) != null)
						{
							m_board.setTile(i, j, tile);
							tile.setState(Tile.State.SCORED);
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

		// Add the default tiles to our rack.
		Tiles rack = m_lTileRacks.get(0);
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

		m_bOver = false;

		// Create a bitmap image to represent our game board.
		Bitmap bmBoard = m_board.getBitmap();
		Bitmap bmRack = m_lTileRacks.get(0).getBitmap();
		if (bmBoard != null && bmRack != null)
		{
			m_bitmap = Bitmap.createBitMap(
				bmBoard.getWidth(),
				bmBoard.getHeight() + bmRack.getHeight(),
				Bitmap.Config.ARGB_8888);
		}

		m_keyInput = new GameKey();

		// Create our BestWord engine for finding the highest scoring words.
		m_bestWord = new BestWord(this);
	}

	//**********************************************************************************************
	public boolean
	areValidAllWords()
	{
		return m_board.areValidAllWords();
	}

	//**********************************************************************************************
	private void
	bagTile(Tile tile)
	{
		if (m_lVowelBags != null && tile.isVowel())
		{
			m_lVowelBags.get(m_iPlayer).add(tile);
		}
		else
		{
			m_lTileBags.get(m_iPlayer).add(tile);
		}
	}

	//**********************************************************************************************
	public int
	getBingoPrize()
	{
		return m_nBingoPrize;
	}

	//**********************************************************************************************
	public Bitmap
	getBitmap() throws Exception
	{
		Bitmap bmBoard = m_board.getBitmap();
		Bitmap bmRack = m_lTileRacks.get(m_iPlayer).getBitmap();
		m_bitmap.drawBitmap(bmBoard, 0, 0, null);
		m_bitmap.drawBitmap(bmRack, 0, bmBoard.getHeight(), null);

		return m_bitmap;
	}

	//**********************************************************************************************
	public Image
	getImage()
	{
		return null;
	}

	//**********************************************************************************************
	public int
	getBestWord(int iPlayer) throws Exception
	{
		return m_bestWord.getBestWord(iPlayer);
	}

	//**********************************************************************************************
	public Board
	getBoard()
	{
		return m_board;
	}

	//**********************************************************************************************
	public int
	getBoardSize()
	{
		return m_nBoardSize;
	}

	//**********************************************************************************************
	public Bonus
	getBonus(
		int iRow,
		int iColumn
	) throws Exception
	{
		return m_board.getBonus(iRow, iColumn);
	}

	//**********************************************************************************************
	public int
	getPlayerCount()
	{
		return m_nPlayers;
	}

	//**********************************************************************************************
	public Tiles
	getRack(int iPlayer)
	{
		return m_lTileRacks.get(iPlayer);
	}

	//**********************************************************************************************
	public int
	getRackTileCount(int iPlayer)
	{
		return m_lTileRacks.get(iPlayer).size();
	}

	//**********************************************************************************************
	public int
	getRackSize()
	{
		return m_nRackSize;
	}

	//**********************************************************************************************
	public Square
	getSquare(
		int iRow,
		int iColumn
	) throws Exception
	{
		return m_board.getSquare(iRow, iColumn);
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
	public int
	getTileCount(int iPlayer)
	{
		return m_lTileBags.get(iPlayer).size();
	}

	//**********************************************************************************************
	public int
	getTileScore(char cTile)
	{
		return m_aTileScores[cTile];
	}

	//**********************************************************************************************
	public int
	getTileScore(Tile tile)
	{
		return tile.getScore();
	}

	//**********************************************************************************************
	public int
	getVowelCount(int iPlayer)
	{
		return m_lVowelBags.get(iPlayer).size();
	}

	//**********************************************************************************************
	public Words
	getWords()
	{
		return m_words;
	}

	//**********************************************************************************************
	public boolean
	grabTile(int iPlayer)
	{
		return m_lTileBags.get(iPlayer).grab(m_lTileRacks.get(iPlayer));
	}

	//**********************************************************************************************
	public boolean
	grabVowel(int iPlayer)
	{
		if (m_lVowelBags != null)
		{
			return m_lVowelBags.get(iPlayer).grab(m_lTileRacks.get(iPlayer));
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isRackFull(int iPlayer)
	{
		return m_lTileRacks.get(iPlayer).isFull();
	}

	//**********************************************************************************************
	public boolean
	isOver()
	{
		return m_bOver;
	}

	//**********************************************************************************************
	public void
	jumbleRack(int iPlayer)
	{
		m_lTileRacks.get(iPlayer).jumble();
	}

	//**********************************************************************************************
	private boolean
	moveTileUp() throws Exception
	{
		Tiles rack = m_lTileRacks.get(m_iPlayer);
		if (rack.remove(m_tileSelect))
		{
			Square square = m_board.getOpenSquare();
			square.setTile(m_tileSelect);
			m_tileSelect.setState(Tile.State.MOVED);
			return true;
		}

		return m_board.moveTileUp(m_tileSelect);
	}
	
	//**********************************************************************************************
	private boolean
	moveTileDown() throws Exception
	{
		Tiles rack = m_lTileRacks.get(m_iPlayer);
		if (rack.remove(m_tileSelect))
		{
			Square square = m_board.getOpenSquare();
			square.setTile(m_tileSelect);
			m_tileSelect.setState(Tile.State.MOVED);
			return true;
		}

		return m_board.moveTileDown(m_tileSelect);
	}
	
	//**********************************************************************************************
	private boolean
	moveTileRight() throws Exception
	{
		Tiles rack = m_lTileRacks.get(m_iPlayer);
		if (rack.moveTileRight(m_tileSelect))
		{
			return true;
		}

		return m_board.moveTileRight(m_tileSelect);
	}

	//**********************************************************************************************
	private boolean
	moveTileLeft() throws Exception
	{
		Tiles rack = m_lTileRacks.get(m_iPlayer);
		if (rack.moveTileLeft(m_tileSelect))
		{
			return true;
		}

		return m_board.moveTileLeft(m_tileSelect);
	}

	//**********************************************************************************************
	private boolean
	placeTile() throws Exception
	{
		if (m_tileSelect.getSquare() != null)
		{
			m_tileSelect.setState(Tile.State.PLACED);
			Tiles rack = m_lTileRacks.get(m_iPlayer);
			if (rack.size() > 0)
			{
				m_tileSelect = rack.get(0);
				m_tileSelect.setState(Tile.State.MOVED);
			}
			return true;
		}
		return moveTileUp();
	}
	
	//**********************************************************************************************
	private boolean
	nextTile() throws Exception
	{
		Tiles rack = m_lTileRacks.get(m_iPlayer);
		int iTile = rack.indexOf(m_tileSelect);
		if (iTile < 0)
		{
			placeTile();
		}
		else
		{
			m_tileSelect.setState(Tile.State.RACKED);
		}
		if (++iTile >= rack.size())
		{
			iTile = 0;
		}
		if ((m_tileSelect = rack.get(iTile)) != null)
		{
			m_tileSelect.setState(Tile.State.MOVED);
		}
		return true;
	}
	
	//**********************************************************************************************
	private boolean
	withdrawTile()
	{
		Square square;
		if ((square = m_tileSelect.getSquare()) != null)
		{
			square.setTile(null);
			m_tileSelect.setState(Tile.State.RACKED);
			Tiles rack = m_lTileRacks.get(m_iPlayer);
			rack.add(m_tileSelect);
		}
		m_tileSelect = null;
		return true;
	}
	
	//**********************************************************************************************
	private boolean
	withdrawAllTiles() throws Exception
	{
		Tiles rack = m_lTileRacks.get(m_iPlayer);
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
							tile.setState(Tile.State.RACKED);
							square.setTile(null);
							rack.add(tile);
							break;
						default:
							break;
					}
				}
			}
		}
		m_tileSelect = null;
		return true;
	}

	//**********************************************************************************************
	public boolean
	drawTiles() throws Exception
	{
		// Don't draw letters while in selection mode.
		if (m_tileSelect != null)
		{
			return false;
		}
		
		// If there is room on our player's rack, see if we have letters to pick from.
		while (!isRackFull(m_iPlayer))
		{
			int nTiles = getTileCount(m_iPlayer);
			int nVowels = getVowelCount(m_iPlayer);

			// If there are no letters left to grab, fall through to the next function.
			if (nTiles == 0 && nVowels == 0)
			{
				break;
			}
			
			// If the bag has only enough letters for the rack, or there is only
			// one bag to grab from, pull them automatically.
			int nEmpty = m_nRackSize - getRackTileCount(m_iPlayer);
			if (nEmpty >= nTiles + nVowels ||
				nTiles == 0 ||
				nVowels == 0)
			{
				while (--nEmpty >= 0)
				{
					if (!grabTile(m_iPlayer) &&
						!grabVowel(m_iPlayer))
					{
						break;
					}
				}
				break;
			}

			// Have the player pick their vowels and consonants.
			System.out.print("Pick a (V)owel or (C)onsonant: ");

			while (true)
			{
				// Get the next key command.
				int key = m_keyInput.getKey();
				switch (key)
				{
					case 'q':
					case 'Q':
						m_bOver = true;
						return true;

					case 'C':
					case 'c':
						grabTile(m_iPlayer);
						return true;

					case 'V':
					case 'v':
						grabVowel(m_iPlayer);
						return true;

					default:
						break;
				}
			}
		}

		// Done drawing letters.  Select the first.
		if ((m_tileSelect = m_lTileRacks.get(m_iPlayer).get(0)) == null)
		{
			m_bOver = true;
		}
		else
		{
			m_tileSelect.setState(Tile.State.MOVED);
		}
		return true;
	}

	//**********************************************************************************************
	private void
	drawAllTiles() throws Exception
	{
		// Make sure we don't have any placed tiles on the board.
		withdrawAllTiles();

		// Get access to our tile rack.
		Tiles rack = getRack(m_iPlayer);
		
		// Count up the number of vowels we have.
		int nRackVowels = rack.getVowelCount();

		// If there is room on our player's rack, see if we have letters to pick from.
		while (!isRackFull(m_iPlayer))
		{
			int nTiles = getTileCount(m_iPlayer);
			int nVowels = getVowelCount(m_iPlayer);

			// If there are no letters left to grab, we're done.
			if (nTiles == 0 && nVowels == 0)
			{
				break;
			}

			// If our vowel count is less than half, try to pick one.
			if (nVowels > 0)
			{
				if (nTiles == 0 || nRackVowels < (rack.size() >> 1))
				{
					grabVowel(m_iPlayer);
					++nRackVowels;
					continue;
				}
			}

			// Pick a normal tile.
			grabTile(m_iPlayer);
		}
	}

	//**********************************************************************************************
	private boolean
	playMoveTile() throws Exception
	{
		System.out.print("Move: Next/Up/Down/Left/Right/Place/Jumble/Withdraw/All: ");

		while (m_tileSelect != null)
		{
			// Get the next key command.
			int key = m_keyInput.getKey();

			switch (key)
			{
				case 'q':
				case 'Q':
					m_bOver = true;
					return true;

				case 'a':
				case 'A':
					return withdrawAllTiles();

				case 'J':
				case 'j':
					withdrawAllTiles();
					jumbleRack(m_iPlayer);
					return true;
					
				case 'w':
				case 'W':
					return withdrawTile();

				case 'n':
				case 'N':
					return nextTile();

				case 'P':
				case 'p':
				case KeyEvent.VK_ENTER:
					return placeTile();

				case 'U':
				case 'u':
				case KeyEvent.VK_UP:
					return moveTileUp();

				case 'D':
				case 'd':
				case KeyEvent.VK_DOWN:
					return moveTileDown();

				case 'L':
				case 'l':
				case KeyEvent.VK_LEFT:
					return moveTileLeft();

				case 'R':
				case 'r':
				case KeyEvent.VK_RIGHT:
					return moveTileRight();

				default:
					break;
			}
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isValidMove() throws Exception
	{
		return m_board.isValidMove();
	}

	//**********************************************************************************************
	public boolean
	playWord() throws Exception
	{
		// If it's an invalid play, withdraw all the letters.
		if (!isValidMove())
		{
			withdrawAllTiles();
			return false;
		}

		m_board.playWord();
		return true;
	}

	//**********************************************************************************************
	public void
	play() throws Exception
	{
		while (!m_bOver)
		{
			// Update the game board.
			System.out.println("Player's turn: " + Integer.toString(m_iPlayer + 1));
			System.out.println(toString());

			// If there is room on our player's rack, see if we have letters to pick from.
			if (drawTiles())
			{
				continue;
			}

			// If we're in select mode, allow the user to move the tile.
			if (playMoveTile())
			{
				continue;
			}
		}
	}

	//**********************************************************************************************
	public void
	moveRobot(int iPlayer) throws Exception
	{
		m_iPlayer = iPlayer;
		Tiles rack = getRack(iPlayer);

		// Make sure our rack is full with available tiles.
		drawAllTiles();

		// Find our BestWord.
		int nBestScore = m_bestWord.getBestWord(iPlayer);

		// If there's no move, try to trade in letters.
		if (nBestScore == 0)
		{
			// Make sure there are letters left in the bag(s).
			int nTiles = getTileCount(iPlayer) + getVowelCount(iPlayer);
			if (nTiles > 0)
			{
				int iTile = 0;
				while (iTile < rack.size())
				{
					Tile tile = rack.get(iTile);
					switch (tile.getLetter())
					{
						case Tile.WILD:	// Keep these letters.
						case 'J':
						case 'K':
						case 'Q':
						case 'S':
						case 'X':
						case 'Z':
							break;
						default:
							rack.remove(iTile);
							bagTile(tile);
							continue;
					}
					++iTile;
				}
			}
			else
			{
				if (m_nLastScore <= 0)
				{
					if (--m_nLastScore <= - m_nPlayers)
					{
						m_bOver = true;
					}
					return;
				}
			}

			// Record this as a zero score.
			m_nLastScore = 0;
			return;
		}

		// Place our BestWord on the board.
		m_bestWord.placeBestWord(iPlayer);

		// Play and score the word.
		if (!playWord())
		{
			throw new Exception("Placed BestWord could not be played.");
		}

		// Score the word.  If the score does not match what our search engine calculated
		// it to be, throw an exception.
		if (scoreWord(iPlayer) != nBestScore)
		{
			throw new Exception(
				"BestScore (" + nBestScore + ") not equal to played score (" + m_nLastScore + ")");
		}

		// Draw more tiles.
		drawAllTiles();
	}

	//**********************************************************************************************
	public int
	scoreWord(int iPlayer) throws Exception
	{
		// Score our word and add its value to the player's score total.
		m_nLastScore = m_board.scoreWord();
		m_aPlayerScores[iPlayer] += m_nLastScore;

		// If the player is out of tiles, then mark the game as over.
		if (getRack(iPlayer).size() == 0 &&
			getTileCount(iPlayer) + getVowelCount(iPlayer) == 0)
		{
			m_bOver = true;
		}

		// Return the last score.
		return m_nLastScore;
	}

	//**********************************************************************************************
	public void
	setTile(
		int iRow,
		int iColumn,
		Tile tile
	) throws Exception
	{
		m_board.setTile(iRow, iColumn, tile);
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
			sbBoard.append(m_lTileRacks.get(iPlayer).toString());
			sbBoard.append(")\n");
		}

		return sbBoard.toString();
	}

	//**********************************************************************************************
	public void
	update(Object obj)
	{
	}
}

//**************************************************************************************************
