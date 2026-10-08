/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword game board.
 */

package com.jot.crossword;

import com.jot.decomp.Words;

//**************************************************************************************************

public class Board
{
	protected class Placement
	{
		protected int m_xFirst;
		protected int m_yFirst;
		protected int m_xLast;
		protected int m_yLast;

		protected
		Placement()
		{
			m_xFirst = -1;
		}
	}
	
	protected Square[][] m_aaSquares;
	protected Bitmap m_bitmap;
	protected int m_nBingoPrize;
	protected int m_nBingoLength;
	protected boolean m_bEmpty;
	protected Words m_words;
	protected boolean m_bFlipped;
	private char[] m_aWord;

	//**********************************************************************************************
	public
	Board(
		final byte[][] aaBonuses,
		int nBingoPrize,
		int nBingoLength)
	{
		this(aaBonuses, nBingoPrize, nBingoLength, null, null, null, null);
	}

	//**********************************************************************************************
	public
	Board(
		final byte[][] aaBonuses,
		int nBingoPrize,
		int nBingoLength,
		Words words,
		final char[][] aaDefaultTiles,
		final byte[] aTileScores,
		final Bitmap[] aBitmaps)
	{
		// Save the bingo details for scoring.
		m_nBingoPrize = nBingoPrize;
		m_nBingoLength = nBingoLength;
		m_bEmpty = true;
		m_words = words;

		// Create an array for testing words.
		m_aWord = new char[aaBonuses.length + 1];

		// Create the matrix of squares.
		m_aaSquares = new Square[aaBonuses.length][aaBonuses[0].length];
		for (int i = 0; i < m_aaSquares.length; ++i)
		{
			for (int j = 0; j < m_aaSquares[0].length; ++j)
			{
				// Create this square based on its score bonus and bitmap.
				byte iBonus = aaBonuses[i][j];
				Square square = m_aaSquares[i][j] =
					new Square(
						iBonus,
						i, j,
						aBitmaps != null ? aBitmaps[iBonus] : null);

				// If we have default letter tiles on board, include them.
				if (aaDefaultTiles != null)
				{
					char cTile = aaDefaultTiles[i][j];
					if (cTile != 0)
					{
						Tile tile =
							new Tile(
								cTile,
								aTileScores != null ? aTileScores[cTile] : 0,
								aBitmaps != null ? aBitmaps[cTile] : null);
						square.setTile(tile);
						tile.setState(Tile.State.SCORED);
						m_bEmpty = false;
					}
				}
			}
		}

		// Get the pixel width and height of the game board.
		if (aBitmaps != null)
		{
			int width = m_aaSquares[0].length * aBitmaps[0].getWidth();
			int height = m_aaSquares.length * aBitmaps[0].getHeight();

			// Create a bitmap image to represent our game board.
			m_bitmap = Bitmap.createBitMap(width, height, Bitmap.Config.ARGB_8888);
		}
	}

	//**********************************************************************************************
	public boolean
	areValidAllWords()
	{
		// Make sure we have a word list.
		if (m_words == null)
		{
			return false;
		}

		// Check for all horizontal words.
		for (int iRow = 0; iRow < m_aaSquares.length; ++iRow)
		{
			int iLen = 0;
			for (int iColumn = 0; iColumn < m_aaSquares.length; ++iColumn)
			{
				Tile tile = getTile(iRow, iColumn);
				if (tile != null)
				{
					m_aWord[iLen++] = tile.getLetter();
					continue;
				}

				if (iLen >= 2)
				{
					m_aWord[iLen] = 0;
					if (!m_words.isWord(m_aWord))
					{
						return false;
					}
				}
				iLen = 0;
			}

			if (iLen >= 2)
			{
				m_aWord[iLen] = 0;
				if (!m_words.isWord(m_aWord))
				{
					return false;
				}
				iLen = 0;
			}
		}

		// Check for all horizontal words.
		for (int iColumn = 0; iColumn < m_aaSquares.length; ++iColumn)
		{
			int iLen = 0;
			for (int iRow = 0; iRow < m_aaSquares.length; ++iRow)
			{
				Tile tile = getTile(iRow, iColumn);
				if (tile != null)
				{
					m_aWord[iLen++] = tile.getLetter();
					continue;
				}

				if (iLen >= 2)
				{
					m_aWord[iLen] = 0;
					if (!m_words.isWord(m_aWord))
					{
						return false;
					}
				}
				iLen = 0;
			}

			if (iLen >= 2)
			{
				m_aWord[iLen] = 0;
				if (!m_words.isWord(m_aWord))
				{
					return false;
				}
				iLen = 0;
			}
		}

		return true;
	}

	//**********************************************************************************************
	public boolean
	areValidWordsAcross(
		int iColWord,
		int iRowStart,
		int iRowEnd)
	{
		// Make sure we have a word list.
		if (m_words == null)
		{
			return false;
		}

		for (int iRow = iRowStart; iRow < iRowEnd; ++iRow)
		{
			// Skip over check for tiles that have already been scored.
			Tile tile = getTile(iRow, iColWord);
			if (tile.getState() == Tile.State.SCORED)
			{
				continue;
			}

			// Scan back until we reach the start of potential word.
			int iCol = iColWord;
			while (--iCol >= 0 && getTile(iRow, iCol) != null)
			{
			}

			// Parse the word into a string.
			int iLen = 0;
			while ((tile = getTile(iRow, ++iCol)) != null)
			{
				m_aWord[iLen++] = tile.getLetter();
			}
			m_aWord[iLen] = 0;

			// If our word is at least two characters and is not a valid word, return false.
			if (iLen >= 2 && !m_words.isWord(m_aWord))
			{
				return false;
			}
		}

		return true;
	}

	//**********************************************************************************************
	public boolean
	areValidWordsDown(
		int iRowWord,
		int iColStart,
		int iColEnd)
	{
		// Make sure we have a word list.
		if (m_words == null)
		{
			return false;
		}

		for (int iCol = iColStart; iCol < iColEnd; ++iCol)
		{
			// Skip over check for tiles that have already been scored.
			Tile tile = getTile(iRowWord, iCol);
			if (tile.getState().atLeast(Tile.State.CROSSED))
			{
				continue;
			}

			// Scan back until we reach the start of potential word.
			int iRow = iRowWord;
			while (--iRow >= 0 && getTile(iRow, iCol) != null)
			{
			}

			// Parse the word into a string.
			int iLen = 0;
			while ((tile = getTile(++iRow, iCol)) != null)
			{
				m_aWord[iLen++] = tile.getLetter();
			}
			m_aWord[iLen] = 0;

			// If our word is at least two characters and is not a valid word, return false.
			if (iLen >= 2 && !m_words.isWord(m_aWord))
			{
				return false;
			}
		}

		return true;
	}

	//**********************************************************************************************
	public boolean
	areValidWords(Tile.State eState)
	{
		// Find the placement of the placed/played word.
		Placement placement = getWordPlacement(eState);
		if (placement == null)
		{
			return false;
		}

		// Check for a horizontal and vertical words.
		return
			areValidWordsAcross(placement.m_xFirst, placement.m_yFirst, placement.m_yLast + 1) &&
			areValidWordsDown(placement.m_yFirst, placement.m_xFirst, placement.m_xLast + 1);
	}

	//**********************************************************************************************
	public Bitmap
	getBitmap()
	{
		if (m_bitmap != null)
		{
			int y = 0;
			for (int i = 0; i < m_aaSquares.length; ++i)
			{
				Bitmap bmSquare = null;
				int x = 0;
				for (int j = 0; j < m_aaSquares[0].length; ++j)
				{
					bmSquare = m_aaSquares[i][j].getBitmap();
					if (bmSquare != null)
					{
						m_bitmap.drawBitmap(bmSquare, x, y, null);
						x += bmSquare.getWidth();
					}
				}
				if (bmSquare != null)
				{
					y += bmSquare.getHeight();
				}
			}
		}
		return m_bitmap;
	}

	//**********************************************************************************************
	public Bonus
	getBonus(
		int iRow,
		int iColumn
	) throws Exception
	{
		if (iRow < 0 || iRow >= m_aaSquares.length ||
			iColumn < 0 || iColumn >= m_aaSquares.length)
		{
			throw new Exception("Overflow of board index.");
		}

		if (m_bFlipped)
		{
			return m_aaSquares[iColumn][iRow].getBonus();
		}
		return m_aaSquares[iRow][iColumn].getBonus();
	}

	//**********************************************************************************************
	public char
	getLetter(
		int iRow,
		int iColumn
	) throws Exception
	{
		if (iRow < 0 || iRow >= m_aaSquares.length ||
			iColumn < 0 || iColumn >= m_aaSquares.length)
		{
			throw new Exception("Overflow of board index.");
		}

		if (m_bFlipped)
		{
			return m_aaSquares[iColumn][iRow].getLetter();
		}
		return m_aaSquares[iRow][iColumn].getLetter();
	}

	//**********************************************************************************************
	// Fibonacci scan out from the center for the first open square.
	public Square
	getOpenSquare() throws Exception
	{
		int iRow = getSize() >> 1;
		int iColumn = iRow;
		
		int iStep = 0;
		int nStage = 0;
		int nShift = 2;

		while (getTile(iRow, iColumn) != null)
		{
			switch (nStage)
			{
				case 0:
					if (iStep == 0)
					{
						--iRow;
						break;
					}
					++iColumn;
					break;
				case 1:
					++iRow;
					break;
				case 2:
					--iColumn;
					break;
				case 3:
					--iRow;
					break;
				default:
					break;
			}

			if (++iStep >= nShift)
			{
				iStep = 0;
				nShift += 2;
				if (++nStage >= 4)
				{
					nStage = 0;
				}
			}
		}

		return getSquare(iRow, iColumn);
	}

	//**********************************************************************************************
	public int
	getSize()
	{
		return m_aaSquares.length;
	}

	//**********************************************************************************************
	public Square
	getSquare(
		int iRow,
		int iColumn
	) throws Exception
	{
		if (iRow < 0 || iRow >= m_aaSquares.length ||
			iColumn < 0 || iColumn >= m_aaSquares.length)
		{
			throw new Exception("Overflow of board index.");
		}

		if (m_bFlipped)
		{
			return m_aaSquares[iColumn][iRow];
		}
		return m_aaSquares[iRow][iColumn];
	}

	//**********************************************************************************************
	public Tile
	getTile(
		int iRow,
		int iColumn)
	{
		if (iRow >= 0 && iRow < m_aaSquares.length &&
			iColumn >= 0 && iColumn < m_aaSquares.length)
		{
			if (m_bFlipped)
			{
				return m_aaSquares[iColumn][iRow].getTile();
			}
			return m_aaSquares[iRow][iColumn].getTile();
		}
		return null;
	}

	//**********************************************************************************************
	// Get the x1,y1/x2,y2 first/last tile placement of a word with the given tile state.
	// If not found, return a null placement.
	private Placement
	getWordPlacement(Tile.State eState)
	{
		Placement placement = new Placement();

		// Don't allow a null tile in between our row/column of placed tiles.
		int xNull = -1;
		int yNull = -1;
		
		// Make two passes through the squares.  The first to find the location of the new word.
		for (int y = 0; y < m_aaSquares.length; ++y)
		{
			for (int x = 0; x < m_aaSquares[0].length; ++x)
			{
				// If we have a null tile, see if we've started our placed row/column.
				Tile tile = getTile(y, x);
				if (tile == null)
				{
					if (placement.m_xFirst >= 0)
					{
						if (placement.m_xFirst == placement.m_xLast)
						{
							if (x == placement.m_xFirst ||
								placement.m_yFirst == placement.m_yLast)
							{
								xNull = x;
								yNull = y;
							}
						}
						else
						{
							if (y == placement.m_yFirst)
							{
								xNull = x;
								yNull = y;
							}
						}
					}
					continue;
				}

				// If this tile matches our state, extend our row/column to fit it.
				if (tile.getState().equals(eState))
				{
					// If this is our first placed tile, the record its spot and continue.
					if (placement.m_xFirst < 0)
					{
						placement.m_xFirst = placement.m_xLast = x;
						placement.m_yFirst = placement.m_yLast = y;
						continue;
					}

					// If this tile is not inline with the placement, return null.
					if (x != placement.m_xFirst &&
						y != placement.m_yFirst)
					{
						return null;
					}

					// Based on the vertical/horizontal placement, allow this tile to extend
					// only in this direction.
					if (placement.m_xFirst == placement.m_xLast)
					{
						if (placement.m_yFirst != placement.m_yLast &&
							x != placement.m_xFirst)
						{
							return null;
						}

						// If we have a null tile placed between our first and last, return null.
						if (xNull == placement.m_xFirst &&
							yNull < y)
						{
							return null;
						}
					}
					else
					{
						if (y != placement.m_yFirst)
						{
							return null;
						}

						// If we have a null tile placed between our first and last, return null.
						if (yNull == placement.m_yFirst &&
							xNull < x)
						{
							return null;
						}
					}

					// Record the new last placed tile.
					placement.m_xLast = x;
					placement.m_yLast = y;
				}
			}
		}

		// If we have placed word, return null.
		if (placement.m_xFirst < 0)
		{
			return null;
		}
		return placement;
	}

	//**********************************************************************************************
	public int
	getWordScore(Tile.State eState) throws Exception
	{
		// Find the placement of the placed/played word.
		Placement placement = getWordPlacement(eState);
		if (placement == null)
		{
			return 0;
		}

		// Mark all tiles that cross the placed/played word as CROSSED.
		markCrossedTiles(eState);

		// Tally the total score.
		int nScoreTotal = 0;

		// Total the score of each horizontal word.
		for (int i = 0; i < m_aaSquares.length; ++i)
		{
			int	nScoreMultiply = 1;
			int nScoreWord = 0;
			int nWordLength = 0;
			int nBingoLength = 0;
			for (int j = 0; j < m_aaSquares[0].length; ++j)
			{
				Square square = getSquare(i, j);
				Tile tile = square.getTile();
				if (tile != null)
				{
					int	nScoreTile = tile.getScore();

					switch (tile.getState())
					{
						case CROSSED:
							++nWordLength;
							break;

						case PLACED:
						case PLAYED:
							// Handle bonus tiles.
							switch (square.getBonus())
							{
								case L2:
									nScoreTile *= 2;
									break;

								case L3:
									nScoreTile *= 3;
									break;

								case L4:
									nScoreTile *= 4;
									break;

								case W2:
									nScoreMultiply *= 2;
									break;

								case W3:
									nScoreMultiply *= 3;
									break;

								case W4:
									nScoreMultiply *= 4;
									break;

								default:
									break;
							}
							++nWordLength;
							++nBingoLength;
							break;

						default:
							nScoreTile = 0;
							break;
					}
					nScoreWord += nScoreTile;
				}
			}
			if (nWordLength > 1)
			{
				nScoreTotal += nScoreWord * nScoreMultiply;
				if (nBingoLength == m_nBingoLength)
				{
					nScoreTotal += m_nBingoPrize;
				}
			}
		}

		// Total the score of each vertical word.
		for (int j = 0; j < m_aaSquares[0].length; ++j)
		{
			int	nScoreMultiply = 1;
			int nScoreWord = 0;
			int nWordLength = 0;
			int nBingoLength = 0;
			for (int i = 0; i < m_aaSquares.length; ++i)
			{
				Square square = getSquare(i, j);
				Tile tile = square.getTile();
				if (tile != null)
				{
					int	nScoreTile = tile.getScore();

					switch (tile.getState())
					{
						case CROSSED:
							++nWordLength;
							tile.setState(Tile.State.SCORED);
							break;

						case PLACED:
						case PLAYED:
							// Handle bonus tiles.
							switch (square.getBonus())
							{
								case L2:
									nScoreTile *= 2;
									break;

								case L3:
									nScoreTile *= 3;
									break;

								case L4:
									nScoreTile *= 4;
									break;

								case W2:
									nScoreMultiply *= 2;
									break;

								case W3:
									nScoreMultiply *= 3;
									break;

								case W4:
									nScoreMultiply *= 4;
									break;

								default:
									break;
							}
							++nWordLength;
							++nBingoLength;
							break;

						default:
							nScoreTile = 0;
							break;
					}
					nScoreWord += nScoreTile;
				}
			}
			if (nWordLength > 1)
			{
				nScoreTotal += nScoreWord * nScoreMultiply;
				if (nBingoLength == m_nBingoLength)
				{
					nScoreTotal += m_nBingoPrize;
				}
			}
		}

		return nScoreTotal;
	}

	//**********************************************************************************************
	// Return true the given position falls within the center of the board.
	public boolean
	isStart(
		int iRow,
		int iColumn)
	{
		// If the board already contains a scored word, return false.
		if (!m_bEmpty)
		{
			return false;
		}
		
		// If we have an even board, there are four center squares.
		if ((m_aaSquares.length & 1) == 0)
		{
			int center = m_aaSquares.length >> 2;
			return
				center == ((iRow + 1) >> 1) &&
				center == ((iColumn + 1) >> 1);
		}

		// Odd sized boards have to make use of the single center square.
		int center = m_aaSquares.length >> 1;
		return
			center == iRow &&
			center == iColumn;
	}

	//**********************************************************************************************
	// Return true the given position range falls within the center of the board.
	public boolean
	isStart(
		int iRowFirst,
		int iRowLast,
		int iColumnFirst,
		int iColumnLast)
	{
		// If the board already contains a scored word, return false.
		if (!m_bEmpty)
		{
			return false;
		}

		// If we have an even board, there are four center squares.
		if ((m_aaSquares.length & 1) == 0)
		{
			int center = m_aaSquares.length >> 2;
			return
				center >= ((iRowFirst + 1) >> 1) &&
				center <= ((iRowLast + 1) >> 1) &&
				center >= ((iColumnFirst + 1) >> 1) &&
				center <= ((iColumnLast + 1) >> 1);
		}

		// Odd sized boards have to make use of the single center square.
		int center = m_aaSquares.length >> 1;
		return
			center >= iRowFirst &&
			center <= iRowLast &&
			center >= iColumnFirst &&
			center <= iColumnLast;
	}

	//**********************************************************************************************
	// Return true if the played letters have been placed as a single row or column of tiles that
	// either connect with a scored tile or make use of a center tile.
	public boolean
	isValidMove()
	{
		int nPlayed = 0;

		// Find the placement of the new word.
		Placement placement = getWordPlacement(Tile.State.PLACED);

		// If we have no placed tiles, then we have no valid move.
		if (placement == null)
		{
			return false;
		}

		// Check for an adjoining letter on the board.
		for (int y = 0; y < m_aaSquares.length; ++y)
		{
			for (int x = 0; x < m_aaSquares[0].length; ++x)
			{
				Tile tile = getTile(y, x);
				if (tile != null)
				{
					switch (tile.getState())
					{
						case PLAYED:
						case CROSSED:
						case SCORED:
							++nPlayed;
							if (placement.m_xFirst == placement.m_xLast)
							{
								if (x == placement.m_xFirst)
								{
									if (y == placement.m_yFirst - 1 ||
										y == placement.m_yLast + 1)
									{
										return true;
									}
								}
								else if (
									x == placement.m_xFirst - 1 ||
									x == placement.m_xFirst + 1)
								{
									if (y >= placement.m_yFirst && y <= placement.m_yLast)
									{
										return true;
									}
								}
							}
							else
							{
								if (y == placement.m_yFirst)
								{
									if (x == placement.m_xFirst - 1 ||
										x == placement.m_xLast + 1)
									{
										return true;
									}
								}
								else if (
									y == placement.m_yFirst - 1 ||
									y == placement.m_yFirst + 1)
								{
									if (x >= placement.m_xFirst &&
										x <= placement.m_xLast)
									{
										return true;
									}
								}
							}
							break;

						default:
							break;
					}
				}
			}
		}

		// If this is our first word, make sure it hits the center.
		if (nPlayed == 0)
		{
			// The initial word needs to be at least two letters.
			if (placement.m_xFirst == placement.m_xLast &&
				placement.m_yFirst == placement.m_yLast)
			{
				return false;
			}

			// Return true if we're on a starting position.
			return isStart(
				placement.m_xFirst,
				placement.m_xLast,
				placement.m_yFirst,
				placement.m_yLast);
		}

		// No adjoining played letters.
		return false;
	}

	//**********************************************************************************************
	// Run a horizontal/verticle scan for this scored tile to connect with the played word.
	// Those tile(s) that do extend to the played word will be marked as crossed.
	private void
	markCrossedWord(
		Tile.State eState,
		int x,
		int y,
		boolean bHorizontal)
	{
		boolean bFoundPlayed = false;
		
		if (bHorizontal)
		{
			Tile tile;

			// Check the previous square for a played tile.
			if ((tile = getTile(x - 1, y)) != null &&
				tile.getState() == eState)
			{
				bFoundPlayed = true;
			}

			// And then to the end of the connected tiles.
			while ((tile = getTile(++x, y)) != null)
			{
				if (tile.getState() == eState)
				{
					bFoundPlayed = true;
				}
			}

			// If we are connected to a played tile, mark these all as crossed.
			if (bFoundPlayed)
			{
				while ((tile = getTile(--x, y)) != null)
				{
					if (tile.getState() == Tile.State.SCORED)
					{
						tile.setState(Tile.State.CROSSED);
					}
				}
			}
		}
		else
		{
			Tile tile;

			// Check the previous square for a played tile.
			if ((tile = getTile(x, y - 1)) != null &&
				tile.getState() == eState)
			{
				bFoundPlayed = true;
			}

			// And then to the end of the connected tiles.
			while ((tile = getTile(x, ++y)) != null)
			{
				if (tile.getState() == eState)
				{
					bFoundPlayed = true;
				}
			}

			// If we are connected to a played tile, mark these all as crossed.
			if (bFoundPlayed)
			{
				while ((tile = getTile(x, --y)) != null)
				{
					if (tile.getState() == Tile.State.SCORED)
					{
						tile.setState(Tile.State.CROSSED);
					}
				}
			}
		}
	}

	//**********************************************************************************************
	// Scan the board for scored tiles that connect with the newly played tiles, and mark them as
	// crossed so they can be rescored.
	private boolean
	markCrossedTiles(Tile.State eState)
	{
		// Find the placement of the new word.
		Placement placement = getWordPlacement(eState);

		// If we have no placed tiles, then we have no valid move.
		if (placement == null)
		{
			return false;
		}

		// Make sure there is an adjoining letter.
		for (int y = 0; y < m_aaSquares.length; ++y)
		{
			for (int x = 0; x < m_aaSquares[0].length; ++x)
			{
				Tile tile = getTile(x, y);
				if (tile != null &&
					tile.getState() == Tile.State.SCORED)
				{
					// Run a horizontal and vertical scan for tiles to mark as CROSSED.
					markCrossedWord(eState, x, y, true);
					markCrossedWord(eState, x, y, false);
				}
			}
		}

		return true;
	}

	//**********************************************************************************************
	// Scan the board for scored tiles that connect with the newly played tiles, and mark them as
	// crossed so they can be rescored.
	private void
	markTiles(
		Tile.State eStateFrom,
		Tile.State eStateTo)
	{
		// Cycle through all the tiles.
		for (int y = 0; y < m_aaSquares.length; ++y)
		{
			for (int x = 0; x < m_aaSquares[0].length; ++x)
			{
				Tile tile = getTile(x, y);
				if (tile != null &&
					tile.getState() == eStateFrom)
				{
					tile.setState(eStateTo);
				}
			}
		}
	}

	//**********************************************************************************************
	// Move the given tile north on the board.
	public boolean
	moveTileUp(Tile tile) throws Exception
	{
		return moveTileUpDown(tile, true);
	}

	//**********************************************************************************************
	// Move the given tile south on the board.
	public boolean
	moveTileDown(Tile tile) throws Exception
	{
		return moveTileUpDown(tile, false);
	}

	//**********************************************************************************************
	// Move the given tile north on the board.
	public boolean
	moveTileUpDown(Tile tile, boolean bUp) throws Exception
	{
		Square square = tile.getSquare();
		if (square == null)
		{
			return false;
		}

		// Remove this tile from the square.
		square.setTile(null);

		// Get the tiles current location.
		int iRow = square.getRow();
		int iColumn = square.getColumn();

		// Search for a row to the north.
		do
		{
			// Try the previous or next row.
			if (bUp)
			{
				if (--iRow < 0)
				{
					iRow = getSize() - 1;
				}
			}
			else
			{
				if (++iRow >= getSize())
				{
					iRow = 0;
				}
			}
			
			// Look for an open spot on this row.
			int iColumnLeft = iColumn;
			int iColumnRight = iColumn + 1;
			while (iColumnLeft >= 0 || iColumnRight < getSize())
			{
				if (iColumnLeft >= 0)
				{
					square = getSquare(iRow, iColumnLeft--);
					if (square.getTile() == null)
					{
						break;
					}
				}
				if (iColumnRight < getSize())
				{
					square = getSquare(iRow, iColumnRight++);
					if (square.getTile() == null)
					{
						break;
					}
				}
			}
		} while (square.getTile() != null);

		// Assign the tile to our new square.
		square.setTile(tile);

		return true;
	}

	//**********************************************************************************************
	// Move the given tile north on the board.
	public boolean
	moveTileRight(Tile tile) throws Exception
	{
		return moveTileRightLeft(tile, true);
	}

	//**********************************************************************************************
	// Move the given tile south on the board.
	public boolean
	moveTileLeft(Tile tile) throws Exception
	{
		return moveTileRightLeft(tile, false);
	}

	//**********************************************************************************************
	// Move the given tile north on the board.
	public boolean
	moveTileRightLeft(Tile tile, boolean bRight) throws Exception
	{
		Square square = tile.getSquare();
		if (square == null)
		{
			return false;
		}

		// Remove this tile from the square.
		square.setTile(null);

		// Get the tiles current location.
		int iRow = square.getRow();
		int iColumn = square.getColumn();

		// Search for a row to the north.
		do
		{
			// Try the previous or next row.
			if (bRight)
			{
				if (++iColumn >= getSize())
				{
					iColumn = 0;
				}
			}
			else
			{
				if (--iColumn < 0)
				{
					iColumn = getSize() - 1;
				}
			}
			
			// Look for an open spot on this row.
			int iRowUp = iRow;
			int iRowDown = iRow + 1;
			while (iRowUp >= 0 || iRowDown < getSize())
			{
				if (iRowUp >= 0)
				{
					square = getSquare(iRowUp--, iColumn);
					if (square.getTile() == null)
					{
						break;
					}
				}
				if (iRowDown < getSize())
				{
					square = getSquare(iRowDown++, iColumn);
					if (square.getTile() == null)
					{
						break;
					}
				}
			}
		} while (square.getTile() != null);

		// Assign the tile to our new square.
		square.setTile(tile);

		return true;
	}

	/***********************************************************************************************
	 * If they form a valid placement, mark placed tiles as played.
	 */
	public void
	playWord()
	{
		if (isValidMove())
		{
			for (int i = 0; i < m_aaSquares.length; ++i)
			{
				for (int j = 0; j < m_aaSquares[0].length; ++j)
				{
					Tile tile = getTile(i, j);
					if (tile != null && tile.getState() == Tile.State.PLACED)
					{
						tile.setState(Tile.State.PLAYED);
					}
				}
			}
		}
	}

	//**********************************************************************************************
	public Tile
	removePlacedTile(
		int iRow,
		int iCol
	) throws Exception
	{
		Tile tile = getTile(iRow, iCol);
		if (tile != null)
		{
			if (tile.getState() == Tile.State.PLACED)
			{
				setTile(iRow, iCol, null);
			}
			else
			{
				tile = null;
			}
		}
		return tile;
	}

	//**********************************************************************************************
	public int
	scoreWord() throws Exception
	{
		// Get the score of the played word.  If the score is negative, must be an invalid play.
		int nScoreWord = getWordScore(Tile.State.PLAYED);
		if (nScoreWord >= 0)
		{
			// Mark PLAYED tiles as SCORED.
			markTiles(Tile.State.PLAYED, Tile.State.SCORED);
			m_bEmpty = false;
		}

		return nScoreWord;
	}

	//**********************************************************************************************
	public void
	setOrientation(boolean bFlip)
	{
		m_bFlipped = bFlip;
	}

	//**********************************************************************************************
	public void
	setSquare(
		int iRow,
		int iColumn,
		Square square
	) throws Exception
	{
		if (iRow < 0 || iRow >= m_aaSquares.length ||
			iColumn < 0 || iColumn >= m_aaSquares.length)
		{
			throw new Exception("Overflow of board index.");
		}

		if (m_bFlipped)
		{
			m_aaSquares[iColumn][iRow] = square;
		}
		else
		{
			m_aaSquares[iRow][iColumn] = square;
		}
	}

	//**********************************************************************************************
	public void
	setTile(
		int iRow,
		int iColumn,
		Tile tile
	) throws Exception
	{
		if (iRow < 0 || iRow >= m_aaSquares.length ||
			iColumn < 0 || iColumn >= m_aaSquares.length)
		{
			throw new Exception("Overflow of board index.");
		}

		if (m_bFlipped)
		{
			m_aaSquares[iColumn][iRow].setTile(tile);
		}
		else
		{
			m_aaSquares[iRow][iColumn].setTile(tile);
		}
	}

	//**********************************************************************************************
	public String
	toString()
	{
		StringBuilder sbBoard = new StringBuilder();

		int iRow = -1;
		while (true)
		{
			// Output a row divider.
			for (int iCol = 0; iCol < m_aaSquares.length; ++iCol)
			{
				sbBoard.append("----");
			}
			sbBoard.append("-\n");

			if (++iRow >= m_aaSquares.length)
			{
				break;
			}

			// Output each column.
			for (int iCol = 0; iCol < m_aaSquares.length; ++iCol)
			{
				// Output a column divider.
				sbBoard.append('|');
				sbBoard.append(m_aaSquares[iRow][iCol].toString());
			}
			sbBoard.append("|\n");
		}
		return sbBoard.toString();
	}
}

//**************************************************************************************************
