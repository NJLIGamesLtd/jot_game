/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Stores an arrangement of a crossword com.jot.game board.
 */

package com.jot.jot.crossword;

import com.jot.decomp.Words;

import java.util.Set;
import java.util.TreeSet;

//**************************************************************************************************

public class Board
{
	private boolean m_bEmpty;

	private final Square[][] m_aaSquares;
	private final int m_nBingoPrize;
	private final int m_nBingoLength;
	private final Words m_words;
	private boolean m_bRotated;
	private final char[] m_aWord;

	private final int m_size;

	private PlayStatus m_eStatus;

	private int m_nPlacedScore;
	private int m_iPlacedSquare;
	private int m_iPlacedPlayer;
	private final int[] m_aLastScores;
	private final int[] m_aLastSquares;

	private final Placement m_placement = new Placement();
	private final StringBuilder m_sbBoard = new StringBuilder();
	private final Set<String> m_sInvalidWords = new TreeSet<>();

	//**********************************************************************************************
	public
	Board(final Board board)
	{
		m_size = board.m_size;
		m_aWord = new char[m_size + 1];
		m_aaSquares = new Square[m_size][m_size];

		m_nBingoPrize = board.m_nBingoPrize;
		m_nBingoLength = board.m_nBingoLength;
		m_bEmpty = board.m_bEmpty;
		m_words = board.m_words;

		m_eStatus = board.m_eStatus;
		m_bRotated = false;

		for (int i = 0; i < m_size; ++i)
		{
			for (int j = 0; j < m_size; ++j)
			{
				Square square = new Square(board.m_aaSquares[i][j]);
				m_aaSquares[i][j] = square;

				// If our square has a blank, make a copy of it so we save the used letter.
				Tile tile = square.getTile();
				if (tile != null && tile.getScore() == 0)
				{
					square.setTile(new Tile(tile), false);
				}
			}
		}

		m_aLastScores = new int[2];
		m_aLastSquares = new int[2];
		m_aLastScores[0] = board.m_aLastScores[0];
		m_aLastScores[1] = board.m_aLastScores[1];
		m_aLastSquares[0] = board.m_aLastSquares[0];
		m_aLastSquares[1] = board.m_aLastSquares[1];

		m_iPlacedPlayer = board.m_iPlacedPlayer;
		m_iPlacedSquare = board.m_iPlacedSquare;
		
		if ((m_nPlacedScore = getWordScore(Tile.State.PLACED)) > 0)
		{
			m_aLastScores[m_iPlacedPlayer] = m_nPlacedScore;
			m_aLastSquares[m_iPlacedPlayer] = m_iPlacedSquare;
		}
	}

	//**********************************************************************************************
	public
	Board(
		final byte[][] aaBonuses,
		int nBingoPrize,
		int nBingoLength)
	{
		this(aaBonuses, nBingoPrize, nBingoLength, null, null, null);
	}

	//**********************************************************************************************
	public
	Board(
		final byte[][] aaBonuses,
		int nBingoPrize,
		int nBingoLength,
		Words words,
		final char[][] aaDefaultTiles,
		final byte[] aTileScores)
	{
		// Save the bingo details for scoring.
		m_nBingoPrize = nBingoPrize;
		m_nBingoLength = nBingoLength;
		m_bEmpty = true;
		m_words = words;

		// Create an array for testing words.
		m_size = aaBonuses.length;
		m_aWord = new char[m_size + 1];

		// Create the matrix of squares.
		m_aaSquares = new Square[m_size][m_size];
		for (int i = 0; i < m_size; ++i)
		{
			for (int j = 0; j < m_size; ++j)
			{
				// Create this square based on its score bonus.
				byte iBonus = aaBonuses[i][j];
				Square square = m_aaSquares[i][j] =
					new Square(iBonus, i, j);

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
								0);
						square.setTile(tile, true);
						tile.setState(Tile.State.CONFIRMED);
						m_bEmpty = false;
					}
				}
			}
		}

		// SPK: Get the number of players instead of using 2.
		m_aLastScores = new int[2];
		m_aLastSquares = new int[2];
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
			if (tile.getState() == Tile.State.CONFIRMED)
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
			if (tile != null && tile.getState().atLeast(Tile.State.CROSSED))
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
	public void
	clearPlacedScore()
	{
		m_nPlacedScore = 0;
	}

	//**********************************************************************************************
	public void
	copyTiles(Board board)
	{
		for (int i = 0; i < m_size; ++i)
		{
			for (int j = 0; j < m_size; ++j)
			{
				setTile(i, j, board.getTile(i, j));
			}
		}
		m_bEmpty = board.m_bEmpty;
	}

	//**********************************************************************************************
	public String
	getEncoding()
	{
		m_sbBoard.setLength(0);
		for (int iRow = 0; iRow < m_size; ++iRow)
		{
			for (int iColumn = 0; iColumn < m_size; ++iColumn)
			{
				m_sbBoard.append(Tile.GetEncoding(m_aaSquares[iRow][iColumn].getTile()));
			}
			m_sbBoard.append('\n');
		}
		return m_sbBoard.toString();
	}

	//**********************************************************************************************
	public Set<String>
	getInvalidWords()
	{
		// Make sure we have a word list.
		if (m_words == null)
		{
			return null;
		}

		// Clear our potential list of invalid words.
		m_sInvalidWords.clear();

		// Check for all vertical words.
		for (int iRow = 0; iRow < m_size; ++iRow)
		{
			int iLen = 0;
			boolean bPlayed = false;
			for (int iColumn = 0; iColumn < m_size; ++iColumn)
			{
				Tile tile = getTile(iRow, iColumn);
				if (tile != null)
				{
					m_aWord[iLen++] = tile.getLetter();
					if (!tile.isStateAtLeast(Tile.State.CROSSED))
					{
						bPlayed = true;
					}
					continue;
				}

				if (bPlayed && iLen >= 2)
				{
					m_aWord[iLen] = 0;
					if (!m_words.isWord(m_aWord))
					{
						m_sInvalidWords.add(new String(m_aWord, 0, iLen));
					}
				}
				bPlayed = false;
				iLen = 0;
			}

			if (bPlayed && iLen >= 2)
			{
				m_aWord[iLen] = 0;
				if (!m_words.isWord(m_aWord))
				{
					m_sInvalidWords.add(new String(m_aWord, 0, iLen));
				}
				iLen = 0;
			}
		}

		// Check for all horizontal words.
		for (int iColumn = 0; iColumn < m_size; ++iColumn)
		{
			int iLen = 0;
			boolean bPlayed = false;
			for (int iRow = 0; iRow < m_size; ++iRow)
			{
				Tile tile = getTile(iRow, iColumn);
				if (tile != null)
				{
					m_aWord[iLen++] = tile.getLetter();
					if (!tile.isStateAtLeast(Tile.State.CROSSED))
					{
						bPlayed = true;
					}
					continue;
				}

				if (bPlayed && iLen >= 2)
				{
					m_aWord[iLen] = 0;
					if (!m_words.isWord(m_aWord))
					{
						m_sInvalidWords.add(new String(m_aWord, 0, iLen));
					}
				}
				bPlayed = false;
				iLen = 0;
			}

			if (bPlayed && iLen >= 2)
			{
				m_aWord[iLen] = 0;
				if (!m_words.isWord(m_aWord))
				{
					m_sInvalidWords.add(new String(m_aWord, 0, iLen));
				}
				iLen = 0;
			}
		}

		return m_sInvalidWords;
	}

	//**********************************************************************************************
	public int
	getLastScore(int iPlayer)
	{
		if ((m_nPlacedScore = getWordScore(Tile.State.PLACED)) > 0 &&
			m_iPlacedPlayer == iPlayer)
		{
			return m_nPlacedScore;
		}
		return m_aLastScores[iPlayer];
	}

	//**********************************************************************************************
	public int
	getLastSquare(int iPlayer)
	{
		if (m_nPlacedScore > 0 &&
			m_iPlacedPlayer == iPlayer)
		{
			return m_iPlacedSquare;
		}
		if (m_aLastScores[iPlayer] > 0)
		{
			return m_aLastSquares[iPlayer];
		}
		return 0;
	}

	//**********************************************************************************************
	public char
	getLetter(
		int iRow,
		int iColumn)
	{
		if (m_bRotated)
		{
			return m_aaSquares[iColumn][iRow].getLetter();
		}
		return m_aaSquares[iRow][iColumn].getLetter();
	}

	//**********************************************************************************************
	// Fibonacci scan out from the center for the first open square.
	public Square
	getOpenSquare()
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
	// Return true if the played letters have been placed as a single row or column of tiles that
	// either connect with a scored tile or make use of a center tile.
	public PlayStatus
	getPlayStatus()
	{		
		int nPlayed = 0;

		// Find the placement of the new word.
		Placement placement = getWordPlacement(Tile.State.PLACED);

		// If we have no placed tiles, then we have no valid move.
		if (placement == null)
		{
			return m_eStatus;
		}

		// Check for an adjoining letter on the board.
		for (int y = 0; y < m_size; ++y)
		{
			for (int x = 0; x < m_size; ++x)
			{
				Tile tile = getTile(y, x);
				if (tile != null)
				{
					switch (tile.getState())
					{
						case PLAYED:
						case CROSSED:
						case SCORED:
						case CONFIRMED:
							++nPlayed;
							if (placement.m_xFirst == placement.m_xLast)
							{
								if (x == placement.m_xFirst)
								{
									if (y == placement.m_yFirst - 1 ||
										y == placement.m_yLast + 1)
									{
										return PlayStatus.VALID;
									}
								}
								else if (
									x == placement.m_xFirst - 1 ||
									x == placement.m_xFirst + 1)
								{
									if (y >= placement.m_yFirst && y <= placement.m_yLast)
									{
										return PlayStatus.VALID;
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
										return PlayStatus.VALID;
									}
								}
								else if (
									y == placement.m_yFirst - 1 ||
									y == placement.m_yFirst + 1)
								{
									if (x >= placement.m_xFirst &&
										x <= placement.m_xLast)
									{
										return PlayStatus.VALID;
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
				return PlayStatus.SINGLE_LETTER;
			}

			// Return true if we're on a starting position.
			if (isStart(
				placement.m_xFirst,
				placement.m_xLast,
				placement.m_yFirst,
				placement.m_yLast))
			{
				return PlayStatus.VALID;
			}
			return PlayStatus.NOT_START;
		}

		// No adjoining played letters.
		return PlayStatus.ISOLATED;
	}

	//**********************************************************************************************
	public int
	getSize()
	{
		return m_size;
	}

	//**********************************************************************************************
	public Square
	getSquare(
		int iRow,
		int iColumn)
	{
		if (iRow < 0 || iRow >= m_size ||
			iColumn < 0 || iColumn >= m_size)
		{
			return null;
		}

		if (m_bRotated)
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
		if (iRow >= 0 && iRow < m_size &&
			iColumn >= 0 && iColumn < m_size)
		{
			if (m_bRotated)
			{
				return m_aaSquares[iColumn][iRow].getTile();
			}
			return m_aaSquares[iRow][iColumn].getTile();
		}
		return null;
	}

	//**********************************************************************************************
	public int
	getTileIndex(Tile.State eState)
	{
		int iTile = 0;
		for (int iRow = 0; iRow < m_size; ++iRow)
		{
			for (int iColumn = 0; iColumn < m_size; ++iColumn)
			{
				Tile tile = getTile(iRow, iColumn);
				if (tile != null && tile.getState() == eState)
				{
					return iTile;
				}
				++iTile;
			}
		}
		return -1;
	}

	//**********************************************************************************************
	// Return the number of tiles on the board that have the given state.
	public int
	getTilesPlaced(Tile.State eState)
	{
		int nTiles = 0;
		for (int y = 0; y < m_size; ++y)
		{
			for (int x = 0; x < m_size; ++x)
			{
				// If we have a null tile, see if we've started our placed row/column.
				Tile tile = getTile(y, x);
				if (tile != null)
				{
					if (tile.getState() == eState)
					{
						++nTiles;
					}
				}
			}
		}
		return nTiles;
	}

	//**********************************************************************************************
	// Return the tile placement exposure on the board.  Return null if there are no placed tiles.
	public Placement
	getTilePlacement(Tile.State eState)
	{
		// Force our first values to the end.
		m_placement.m_xFirst = m_placement.m_yFirst = m_size;

		// Loop forward through the board.
		for (int y = 0; y < m_size; ++y)
		{
			for (int x = 0; x < m_size; ++x)
			{
				// If we have a null tile, see if we've started our placed row/column.
				Tile tile = getTile(y, x);
				if (tile != null && tile.getState() == eState)
				{
					if (x < m_placement.m_xFirst)
					{
						m_placement.m_xFirst = x;
					}
					if (y < m_placement.m_yFirst)
					{
						m_placement.m_yFirst = y;
					}
					if (x > m_placement.m_xLast)
					{
						m_placement.m_xLast = x;
					}
					if (y > m_placement.m_yLast)
					{
						m_placement.m_yLast = y;
					}
				}
			}
		}

		// If no tile placement was found in this state, return null.
		if (m_placement.m_xFirst == m_size)
		{
			return null;
		}
		return m_placement;
	}

	//**********************************************************************************************
	// Get the x1,y1/x2,y2 first/last tile placement of a word with the given tile state.
	// If not found, return a null placement.
	public Placement
	getWordPlacement(Tile.State eState)
	{
		m_placement.m_xFirst = -1;

		// Don't allow a null tile in between our row/column of placed tiles.
		int xNull = -1;
		int yNull = -1;

		// By default, mark our status as valid.
		m_eStatus = PlayStatus.VALID;

		// Make two passes through the squares.  The first to find the location of the new word.
		int iPlaced = 0;
		for (int y = 0; y < m_size; ++y)
		{
			for (int x = 0; x < m_size; ++x, ++iPlaced)
			{
				// If we have a null tile, see if we've started our placed row/column.
				Tile tile = getTile(y, x);
				if (tile == null)
				{
					if (m_placement.m_xFirst >= 0)
					{
						if (m_placement.m_xFirst == m_placement.m_xLast)
						{
							if (x == m_placement.m_xFirst ||
								m_placement.m_yFirst == m_placement.m_yLast)
							{
								xNull = x;
								yNull = y;
							}
						}
						else
						{
							if (y == m_placement.m_yFirst)
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
					if (m_placement.m_xFirst < 0)
					{
						m_iPlacedSquare = iPlaced;
						m_iPlacedPlayer = tile.getPlayer();
						m_placement.m_xFirst = m_placement.m_xLast = x;
						m_placement.m_yFirst = m_placement.m_yLast = y;
						continue;
					}

					// If this tile is not inline with the placement, return null.
					if (x != m_placement.m_xFirst &&
						y != m_placement.m_yFirst)
					{
						m_eStatus = PlayStatus.NOT_ALIGNED;
						return null;
					}

					// Based on the vertical/horizontal placement, allow this tile to extend
					// only in this direction.
					if (m_placement.m_xFirst == m_placement.m_xLast)
					{
						// Check for two separately placed tiles on the same row.
						if (m_placement.m_yFirst == m_placement.m_yLast &&
							xNull > m_placement.m_xFirst &&
							xNull < x)
						{
							m_eStatus = PlayStatus.NOT_ALIGNED;
							return null;
						}
						
						if (m_placement.m_yFirst != m_placement.m_yLast &&
							x != m_placement.m_xFirst)
						{
							m_eStatus = PlayStatus.NOT_ALIGNED;
							return null;
						}

						// If we have a null tile placed between our first and last, return null.
						if (xNull == m_placement.m_xFirst &&
							yNull < y)
						{
							m_eStatus = PlayStatus.NOT_ALIGNED;
							return null;
						}
					}
					else
					{
						if (y != m_placement.m_yFirst)
						{
							m_eStatus = PlayStatus.NOT_ALIGNED;
							return null;
						}

						// If we have a null tile placed between our first and last, return null.
						if (yNull == m_placement.m_yFirst &&
							xNull < x)
						{
							m_eStatus = PlayStatus.NOT_ALIGNED;
							return null;
						}
					}

					// Record the new last placed tile.
					m_placement.m_xLast = x;
					m_placement.m_yLast = y;
				}
			}
		}

		// If we have no placed word, return null.
		if (m_placement.m_xFirst < 0)
		{
			m_eStatus = PlayStatus.NO_PLACEMENT;
			return null;
		}
		return m_placement;
	}

	//**********************************************************************************************
	public int
	getWordScore(Tile.State eState)
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
		for (int i = 0; i < m_size; ++i)
		{
			int	nScoreMultiply = 1;
			int nScoreWord = 0;
			int nWordLength = 0;
			int nBingoLength = 0;
			int nPlayed = 0;
			for (int j = 0; j < m_size; ++j)
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
							++nPlayed;
							switch (square.getBonus())
							{
								case L2:
								case L21:
								case L22:
								case L23:
								case L24:
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
			if (nWordLength > 1 && nPlayed > 0)
			{
				nScoreTotal += nScoreWord * nScoreMultiply;
				if (nBingoLength == m_nBingoLength)
				{
					nScoreTotal += m_nBingoPrize;
				}
			}
		}

		// Total the score of each vertical word.
		for (int j = 0; j < m_size; ++j)
		{
			int	nScoreMultiply = 1;
			int nScoreWord = 0;
			int nWordLength = 0;
			int nBingoLength = 0;
			int nPlayed = 0;
			for (int i = 0; i < m_size; ++i)
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
							tile.setState(Tile.State.CONFIRMED);
							break;

						case PLACED:
						case PLAYED:
							++nPlayed;
							// Handle bonus tiles.
							switch (square.getBonus())
							{
								case L2:
								case L21:
								case L22:
								case L23:
								case L24:
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
			if (nWordLength > 1 && nPlayed > 0)
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
	// Return true if the board is currently empty of any played words.
	public boolean
	isEmpty()
	{
		return m_bEmpty;
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
		if ((m_size & 1) == 0)
		{
			int center = m_size >> 2;
			return
				center == ((iRow + 1) >> 1) &&
				center == ((iColumn + 1) >> 1);
		}

		// Odd sized boards have to make use of the single center square.
		int center = m_size >> 1;
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
		if ((m_size & 1) == 0)
		{
			int center = m_size >> 2;
			return
				center >= ((iRowFirst + 1) >> 1) &&
				center <= ((iRowLast + 1) >> 1) &&
				center >= ((iColumnFirst + 1) >> 1) &&
				center <= ((iColumnLast + 1) >> 1);
		}

		// Odd sized boards have to make use of the single center square.
		int center = m_size >> 1;
		return
			center >= iRowFirst &&
			center <= iRowLast &&
			center >= iColumnFirst &&
			center <= iColumnLast;
	}

	//**********************************************************************************************
	// Return true if there is a PLACED or PLAYED letter on the board.
	public boolean
	isTilePlacedOrPlayed(int iPlayer)
	{
		// Cycle through all the tiles.
		for (int y = 0; y < m_size; ++y)
		{
			for (int x = 0; x < m_size; ++x)
			{
				Tile tile = getTile(x, y);
				if (tile != null &&
					(iPlayer < 0 || iPlayer == tile.getPlayer()) &&
					(tile.getState() == Tile.State.PLACED ||
					 tile.getState() == Tile.State.PLAYED))
				{
					return true;
				}
			}
		}
		return false;
	}

	//**********************************************************************************************
	public boolean
	isRotated()
	{
		return m_bRotated;
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
					if (tile.getState().atLeast(Tile.State.SCORED))
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
					if (tile.getState().atLeast(Tile.State.SCORED))
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
	private void
	markCrossedTiles(Tile.State eState)
	{
		// Make sure there is an adjoining letter.
		for (int y = 0; y < m_size; ++y)
		{
			for (int x = 0; x < m_size; ++x)
			{
				Tile tile = getTile(x, y);
				if (tile != null &&
					tile.getState().atLeast(Tile.State.SCORED))
				{
					// Run a horizontal and vertical scan for tiles to mark as CROSSED.
					markCrossedWord(eState, x, y, true);
					markCrossedWord(eState, x, y, false);
				}
			}
		}
	}

	//**********************************************************************************************
	// Scan the board for scored tiles that connect with the newly played tiles, and mark them as
	// crossed so they can be rescored.
	public void
	markTiles(
		Tile.State eStateFrom,
		Tile.State eStateTo)
	{
		// Cycle through all the tiles.
		for (int y = 0; y < m_size; ++y)
		{
			for (int x = 0; x < m_size; ++x)
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
	public void
	markTileFound(int iRow, int iColumn)
	{
		if (iRow >= 0 && iRow < m_size &&
			iColumn >= 0 && iColumn < m_size)
		{
			Tile tile = getTile(iRow, iColumn);
			if (tile != null)
			{
				if (!tile.isFound())
				{
					tile.markFound(true);
					markTileFound(iRow - 1, iColumn);
					markTileFound(iRow, iColumn - 1);
					markTileFound(iRow + 1, iColumn);
					markTileFound(iRow, iColumn + 1);
				}
			}
		}
	}
	
	//**********************************************************************************************
	public void
	markConnectedTiles()
	{
		markTilesFound(false);

		// Mark all tiles and their connected tiles from the center squares.
		for (int y = (m_size - 1) >> 1; y <= m_size >> 1; ++y)
		{
			for (int x = (m_size - 1) >> 1; x <= m_size >> 1; ++x)
			{
				markTileFound(x, y);
			}
		}
	}

	//**********************************************************************************************
	public void
	markTilesFound(boolean bFound)
	{
		for (int y = 0; y < m_size; ++y)
		{
			for (int x = 0; x < m_size; ++x)
			{
				Tile tile = getTile(x, y);
				if (tile != null)
				{
					tile.markFound(bFound);
				}
			}
		}
	}

	/***********************************************************************************************
	 * If they form a valid placement, mark placed tiles as played.
	 */
	public PlayStatus
	playWord()
	{
		PlayStatus eStatus = getPlayStatus();
		if (eStatus.isValid())
		{
			for (int i = 0; i < m_size; ++i)
			{
				for (int j = 0; j < m_size; ++j)
				{
					Tile tile = getTile(i, j);
					if (tile != null &&
						tile.getState() == Tile.State.PLACED)
					{
						tile.setState(Tile.State.PLAYED);
					}
				}
			}
		}

		return eStatus;
	}

	//**********************************************************************************************
	public Tile
	removePlacedTile(
		int iRow,
		int iCol)
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
	public void
	reset()
	{
		// Remove all tiles from the board.
		for (int i = 0; i < m_size; ++i)
		{
			for (int j = 0; j < m_size; ++j)
			{
				setTile(i, j, null);
			}
		}
		m_bRotated = false;
		m_bEmpty = true;
	}

	//**********************************************************************************************
	public int
	scoreWord()
	{
		// Get the score of the played word.  If the score is negative, must be an invalid play.
		int nScoreWord = getWordScore(Tile.State.PLAYED);
		if (nScoreWord >= 0)
		{
			m_aLastSquares[m_iPlacedPlayer] = getTileIndex(Tile.State.PLAYED);
			m_aLastScores[m_iPlacedPlayer] = nScoreWord;

			// Mark PLAYED tiles as SCORED.
			markTiles(Tile.State.PLAYED, Tile.State.SCORED);
			m_bEmpty = false;
		}

		return nScoreWord;
	}

	//**********************************************************************************************
	public void
	setBlankLetter(char cTile)
	{
		for (int i = 0; i < m_size; ++i)
		{
			for (int j = 0; j < m_size; ++j)
			{
				Tile tile = getTile(i, j);
				if (tile != null &&
					tile.getState() == Tile.State.PLACED &&
					tile.getLetter() == Tile.WILD)
				{
					tile.setLetter(cTile);
					return;
				}
			}
		}
	}

	//**********************************************************************************************
	public void
	setEmpty(boolean bEmpty)
	{
		m_bEmpty = bEmpty;
	}

	//**********************************************************************************************
	public void
	setLastScore(
		int iPlayer,
		int nLastScore)
	{
		m_aLastScores[iPlayer] = nLastScore;
	}

	//**********************************************************************************************
	public void
	setLastSquare(
		int iPlayer,
		int iLastSquare)
	{
		m_aLastSquares[iPlayer] = iLastSquare;
	}

	//**********************************************************************************************
	public void
	setOrientation(boolean bRotate)
	{
		m_bRotated = bRotate;
	}

	//**********************************************************************************************
	public void
	rotate()
	{
		m_bRotated ^= true;
	}

	//**********************************************************************************************
	public void
	setTile(
		int iRow,
		int iColumn,
		Tile tile)
	{
		// If we're placing a viewed tile, then clear the last score.
		if (tile != null)
		{
			m_aLastScores[tile.getPlayer()] = 0;
		}

		// Assign the tile to the associated square.
		if (m_bRotated)
		{
			m_aaSquares[iColumn][iRow].setTile(tile, true);
		}
		else
		{
			m_aaSquares[iRow][iColumn].setTile(tile, true);
		}
	}

	//**********************************************************************************************
	public String
	toString()
	{
		int iRow = -1;
		m_sbBoard.setLength(0);
		while (true)
		{
			// Output a row divider.
			for (int iCol = 0; iCol < m_size; ++iCol)
			{
				m_sbBoard.append("----");
			}
			m_sbBoard.append("-\n");

			if (++iRow >= m_size)
			{
				break;
			}

			// Output each column.
			for (int iCol = 0; iCol < m_size; ++iCol)
			{
				// Output a column divider.
				m_sbBoard.append('|');
				m_sbBoard.append(m_aaSquares[iRow][iCol].toString());
			}
			m_sbBoard.append("|\n");
		}
		return m_sbBoard.toString();
	}
}

//**************************************************************************************************
