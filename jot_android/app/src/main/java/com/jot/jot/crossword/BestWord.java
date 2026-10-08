/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Best Word searches and returns the highest scoring Scrabble word that can be made from the given
 * letters and board arrangement.
 */

package com.jot.jot.crossword;

import com.jot.decomp.ScanWords;
import com.jot.decomp.Words;

//**************************************************************************************************

public class BestWord implements ScanWords
{
	//**********************************************************************************************

	enum Orientation
	{
		NONE,
		ACROSS,		// Horizontal word across an existing tile(s).
		DOWN,		// Vertical word across an existing tile(s).
		ABOVE,		// Horizontal word above an existing tile(s).
		BEFORE,		// Vertical word before an existing tile(s).
		BELOW,		// Horizontal word below an existing tile(s).
		AFTER,		// Vertical word after an existing tile(s).
		END;

		public Orientation
		getNext()
		{
			return values()[ordinal() + 1];
		}
	}

	static final char EMPTY = ' ';
	static final char NEED = '@';

	private final TopWords m_topWords;
	private int m_nWorstTopScore;
	private int m_iWorstTopWord;
	
	private final Game m_game;
	private final Board m_boardBase;
	private final Board m_board;
	private final Words m_words;
	private final int m_nBoardSize;

	private boolean m_bBestVertical;
	private boolean m_bVertical;
	private int m_iRow;
	private int m_iColumn;
	private int m_iBestRow;
	private int m_iBestColumn;
	private int m_nBestScore;
	private int m_nBestLimit;

	private final char[] m_aBestWord;

	private final char[] m_aPattern;
	private Rack m_rack;

	private int m_iStartRow;
	private int m_iStartColumn;
	private int m_iNeedRow;
	private int m_iNeedColumn;
	
	//**********************************************************************************************
	public
	BestWord(Game game)
	{
		m_game = game;
		m_boardBase = game.getBoard();
		m_board = new Board(m_boardBase);
		m_words = game.getWords().clone();
		m_nBoardSize = game.getBoardSize();
		m_aPattern = new char[m_nBoardSize + 1];
		m_aBestWord = new char[m_nBoardSize + 1];
		m_topWords = new TopWords(m_nBoardSize);
		m_nWorstTopScore = m_iWorstTopWord = 0;
	}

	//**********************************************************************************************
	// Create a pattern of letters and blank squares to build our tiles across.
	private void
	createPattern(
		char[] aPattern,
		int iStart,
		int iRow,
		int iCol)
	{
		int	len = 0;

		do
		{
			// Remove markers on exposed wildcards.
			Tile tile = m_board.getTile(iRow, iStart);

			// If we have a tile, mark our pattern with its letter.
			char ch;
			if (tile != null)
			{
				ch = tile.getLetter();
			}
			// Else, if this is a required square, mark it as NEED.
			else if (iStart == iCol)
			{
				ch = NEED;
			}
			else
			{
				ch = EMPTY;
			}
			aPattern[len++] = ch;
		} while (++iStart < m_nBoardSize);

		while (len < aPattern.length)
		{
			aPattern[len++] = 0;
		}
	}

	//**********************************************************************************************
	public String
	getBestWord()
	{
		return new String(m_aBestWord);
	}

	//**********************************************************************************************
	// Search the entire board for the best word that can be made with these letter tiles.  The
	// word and its location is returned via parameters with its score as the return value.
	public int
	getBestWord(
		int iPlayer,
		Rack rack,
		int nRanking,
		int nScore
	) throws Exception
	{
		// Make sure we have a rack of tiles to work with.
		if ((m_rack = rack) == null)
		{
			m_rack = m_game.getRack(iPlayer);
		}

		// In order to avoid overplaying a user, put a limit of word scores based on ranking.
		m_nBestLimit = 10000;
		if (nRanking > 0)
		{
			if (nRanking < 200)
			{
				m_nBestLimit = 20;
			}
			else if (nRanking < 300)
			{
				m_nBestLimit = 30;
			}
			else if (nRanking < 400)
			{
				m_nBestLimit = 40;
			}
			else if (nRanking < 450)
			{
				m_nBestLimit = 50;
			}
			else if (nRanking < 500)
			{
				m_nBestLimit = 75;
			}
			else if (nRanking < 600)
			{
				m_nBestLimit = 150;
			}
		}
		else if (nScore != 0)
		{
			m_nBestLimit = nScore;
		}

		// Copy the updated tiles from the base board.
		m_board.copyTiles(m_boardBase);

		m_iRow = m_iColumn = 0;
		m_bVertical = false;

		m_bBestVertical = false;
		m_iBestRow = m_iBestColumn = 0;

		m_nBestScore = 0;
		m_nWorstTopScore = m_iWorstTopWord = 0;
		m_topWords.m_nWords = 0;
		
		searchBoardForBestWord();

		return m_nBestScore;
	}

	//**********************************************************************************************
	public TopWords
	getTopWords()
	{
		return m_topWords;
	}

	//**********************************************************************************************
	// Get the potential starting point and required build square for a new word.  Return false
	// if there is no starting square.
	private boolean
	getStartingSquare(
		int iRow,
		int iCol,
		Orientation eOrient)
	{
		m_bVertical = false;
		m_iStartRow = m_iNeedRow = iRow;
		m_iStartColumn = m_iNeedColumn = iCol;

		switch (eOrient)
		{
				// Trying to build a horizontal word on this or an adjoining row.
			case ABOVE:
				if (--iRow < 0)
				{
					return false;
				}
				// fallthru;

			case BELOW:
				if (eOrient == Orientation.BELOW)
				{
					if (++iRow >= m_nBoardSize)
					{
						return false;
					}
				}
				m_iNeedRow = iRow;
				// fallthru;

			case ACROSS:
				// Find the start of our usable pattern squares.
				while (iCol > 0)
				{
					if (m_board.getTile(iRow, --iCol) != null)
					{
						if ((iCol += 2) >= m_nBoardSize)
						{
							return false;
						}
						break;
					}
				}
				m_iStartColumn = iCol;
				break;

				// Trying to build a vertical word on this or an adjoining column.
			case BEFORE:
				if (--iCol < 0)
				{
					return false;
				}
				// fallthru;

			case AFTER:
				if (eOrient == Orientation.AFTER)
				{
					if (++iCol >= m_nBoardSize)
					{
						return false;
					}
				}
				m_iNeedColumn = iCol;
				// fallthru;

			case DOWN:
				// Find the start of our useable pattern squares.
				while (iRow > 0)
				{
					if (m_board.getTile(--iRow, iCol) != null)
					{
						if ((iRow += 2) >= m_nBoardSize)
						{
							return false;
						}
						break;
					}
				}
				m_iStartRow = iRow;
				m_bVertical = true;
				break;

			default:
				break;
		}
		return true;
	}

	//**********************************************************************************************
	public boolean
	isVertical()
	{
		return m_bBestVertical;
	}

	//**********************************************************************************************
	// Place our BestWord.
	public boolean
	placeBestWord(
		int iPlayer,
		Rack rack,
		Board board
	) throws Exception
	{
		// Make sure we have a best word.
		if (m_nBestScore == 0)
		{
			return false;
		}

		// If we don't have a specific board to use, use the default.
		if (board == null)
		{
			board = m_board;
		}

		// Record the number of tiles on our rack, in case we get a bingo.
		if (rack == null)
		{
			rack = m_game.getRack(iPlayer);
		}
		int nRackTiles = rack.getTileCount();

		// If we have a vertical word, flip the board.
		if (m_bBestVertical)
		{
			board.setOrientation(true);
		}

		// Place our word tiles.
		int iRow = m_iBestRow;
		int iColumn = m_iBestColumn;
		char cWord;
		for (int i = 0; (cWord = m_aBestWord[i]) != 0; ++i)
		{
			Tile tile;
			if ((tile = board.getTile(iRow, iColumn)) == null)
			{
				tile = rack.grabTile(cWord);
				if (tile == null)
				{
					if ((tile = rack.grabTile(Tile.WILD)) == null)
					{
						// SPK: Find out if this could happen.
						throw new Exception(
							"BestWord: Can't find letter for robot generated word.");
					}
					tile.setLetter(cWord);
				}
				board.setTile(iRow, iColumn, tile);
			}
			if (cWord != tile.getLetter())
			{
				throw new Exception(
					"BestWord: Mismatch of robot generated word across com.jot.game word.");
			}

			// Move to the next square.
			++iColumn;
		}

		board.setOrientation(false);
		return true;
	}

	//**********************************************************************************************
	// Check the next word to see if it uses our tiles and fits across the board.  If we find a
	// letter not in our tile list, prune over all words at this level for a different letter.
	public int
	prune(
		char[] aWord,
		int iWord
	)
	{
		// Remember the highest level into the word that we reached.
		int iLevel = 0;

		// Initializing these here to appease the compiler.
		int xLast = 0;
		int yFirst = 0;
		int yLast = 0;

		// Loop through starting locations for our word in the board pattern.
		int iStart = 0;
		do
		{
			int xFirst = -1;

			// Start at the next letter into the pattern.
			int iLen = -1;
			int iPat = iStart;
			char cPat = 0;
			boolean bAllEmpty = true;

			// Try to match the word to this pattern.
			char cWord;
			while ((cWord = aWord[++iLen]) != 0)
			{
				// Remember the highest level into the word that we reached.
				if (iLen > iLevel)
				{
					iLevel = iLen;
				}

				// Get the next square in the pattern.  If we reached the end, prune to the
				// next word.
				if ((cPat = m_aPattern[iPat++]) == 0)
				{
					break;
				}

				// If the next square in the pattern is a letter, force a match.
				if (cPat >= Tile.ALPHA)
				{
					bAllEmpty = false;
					if (cWord == cPat)
					{
						continue;
					}
					break;
				}

				// If we're at the square we need to reach, mark off the empty flag.
				if (cPat == NEED)
				{
					bAllEmpty = false;
				}

				// Try to match this letter to those in our tile rack.
				Tile tile;
				int iTile = m_rack.indexOf(cWord);
				if (iTile >= 0)
				{
					tile = m_rack.remove(iTile);
				}
				else
				{
					if ((iTile = m_rack.indexOf(Tile.WILD)) < 0)
					{
						// No match.  Try the next iteration.
						break;
					}
					tile = m_rack.remove(iTile);
					tile.setLetter(cWord);
				}

				// Remove this letter from our tile rack and cycle back for the next letter
				// in the word.
				m_board.setTile(m_iRow, m_iColumn + iStart + iLen, tile);

				// Record the placement of these tiles.
				if (xFirst < 0)
				{
					xFirst = m_iColumn + iStart + iLen;
					yFirst = m_iRow;
				}
				xLast = m_iColumn + iStart + iLen;
				yLast = m_iRow;
			}

			// If we got to the end of our word, all for its extension.
			if (cWord == 0)
			{
				iLevel = iLen;

				// If this word fits next to a played letter or a needed square,
				// and we have not hit the end of the pattern,
				// and the next pattern square is empty or the end, check it.
				if (!bAllEmpty &&
					cPat != 0 &&
					m_aPattern[iPat] < Tile.WILD)
				{
					// Make sure our cross words are also valid.
					if (m_board.areValidWordsDown(m_iRow, m_iColumn + iStart, m_iColumn + iPat))
					{
						int nScore = m_board.getWordScore(Tile.State.PLACED);
						if (nScore > m_nBestScore && nScore < m_nBestLimit)
						{
							m_nBestScore = nScore;
							m_bBestVertical = m_bVertical;
							m_iBestRow = m_iRow;
							m_iBestColumn = m_iColumn + iStart;
							System.arraycopy(aWord, 0, m_aBestWord, 0, iLen + 1);
						}

						// If we have a scorable word, try to add it as one of our top words.
						if (nScore > m_nWorstTopScore)
						{
							// Overwrite our current worst.
							TopWord topWord = m_topWords.m_aTopWords[m_iWorstTopWord];

							// Find a new worst.
							int nWorst = nScore;
							int iWorst = m_iWorstTopWord;
							for (int i = 0; i < m_topWords.m_nWords; ++i)
							{
								if (i == m_iWorstTopWord)
								{
									continue;
								}

								TopWord worstWord = m_topWords.getTopWord(i);
								if (worstWord.isEqual(aWord))
								{
									if (nScore <= worstWord.m_nScore)
									{
										topWord = null;
									}
									else
									{
										topWord = worstWord;
										iWorst = m_iWorstTopWord;
										nWorst = m_nWorstTopScore;
									}
									break;
								}

								if (worstWord.m_nScore < nWorst)
								{
									iWorst = i;
									nWorst = worstWord.m_nScore;
								}
							}

							// If needed, save the location of the new worst word.
							if (topWord != null)
							{
								if (m_topWords.m_nWords < TopWords.N_TOPWORDS)
								{
									if (iWorst < m_topWords.m_nWords &&
										nScore <= topWord.m_nScore)
									{
										iWorst = m_topWords.m_nWords;
									}
									topWord = m_topWords.m_aTopWords[m_topWords.m_nWords++];
								}
								else
								{
									m_nWorstTopScore = nWorst;
								}
								m_iWorstTopWord = iWorst;

								// If we make the cut (for now), save it.
								if (m_bVertical)
								{
									topWord.set(aWord, nScore, yFirst, xFirst, yLast, xLast);
								}
								else
								{
									topWord.set(aWord, nScore, xFirst, yFirst, xLast, yLast);
								}
							}
						}
					}
				}
			}

			// If we have placed tiles, withdraw them.
			if (xFirst >= 0)
			{
				while (xFirst <= xLast)
				{
					for (int y = yFirst; y <= yLast; ++y)
					{
						Tile tile = m_board.removePlacedTile(y, xFirst);
						if (tile != null)
						{
							m_rack.add(tile);
						}
					}
					++xFirst;
				}
			}

			// If we hit the end of the pattern, prune at this level.
			if (cPat == 0)
			{
				break;
			}
		} while (m_aPattern[iStart++] == EMPTY);

		return (iLevel);
	}

	//**********************************************************************************************
	// Search the area for the best word based on its orientation around a square.
	private void
	searchAreaForBestWord(
		int iRow,
		int iCol,
		Orientation eOrient) throws Exception
	{
		// Find starting point and required build square.
		if (getStartingSquare(iRow, iCol, eOrient))
		{
			// Search for the best word across this square.
			searchSquareForBestWord(
				m_iStartRow,
				m_iStartColumn,
				m_iNeedRow,
				m_iNeedColumn);
		}
	}

	//**********************************************************************************************
	// Search the entire board for the best word that can be made with these letter tiles.  The
	// word and its location is returned via parameters with its Scrabble score as the return value.
	private void
	searchBoardForBestWord() throws Exception
	{
		m_bVertical = false;

		// Search for the best scoring word.
		for (int iRow = 0; iRow < m_nBoardSize; ++iRow)
		{
			for (int iCol = 0; iCol < m_nBoardSize; ++iCol)
			{
				Tile tile = m_board.getTile(iRow, iCol);

				// If we have no letter to build across, see if we're in center.
				if (tile == null)
				{
					if (m_board.isStart(iRow, iCol))
					{
						searchRowForBestWord(
							0,
							iRow,
							iCol);
					}
					continue;
				}

				// We have a letter.  Try to build across, down or next to.
				Orientation eOrient = Orientation.NONE;
				while ((eOrient = eOrient.getNext()) != Orientation.END)
				{
					if (!skipArea(iRow, iCol, eOrient))
					{
						searchAreaForBestWord(iRow, iCol, eOrient);
					}
				}
			}
		}
	}

	//**********************************************************************************************
	// Search across a row for the best word.
	private void
	searchRowForBestWord(
		int iStart,
		int iRow,
		int iCol
	) throws Exception
	{
		// Create a single pattern row to build across.
		createPattern(m_aPattern, iStart, iRow, iCol);

		// Fill in our search structure.
		m_iRow = iRow;
		m_iColumn = iStart;

		// Run the search for the highest scoring word across this row.
		m_words.scan(this, ScanWords.SCAN_FLAG_QUICK);
	}

	//**********************************************************************************************
	// Search down or across through a square for the best word.
	private void
	searchSquareForBestWord(
		int startRow,
		int startCol,
		int needRow,
		int needCol
	) throws Exception
	{
		// If we're not building across, flip the board and coordinates to make it look like we are.
		if (m_bVertical)
		{
			m_board.setOrientation(true);
			searchRowForBestWord(
				startRow,
				needCol,
				needRow);
			m_board.setOrientation(false);
			m_bVertical = false;
		}
		else
		{
			searchRowForBestWord(
				startCol,
				needRow,
				needCol);
		}
	}

	//**********************************************************************************************
	// Return true if we can skip an area based on the orientation around a square.
	private boolean
	skipArea(
		int iRow,
		int iCol,
		Orientation eOrient)
	{
		switch (eOrient)
		{
			case ACROSS:
				// Only attempt an across build when it is the first letter in a group.
				if (iCol > 0 &&
					m_board.getTile(iRow, iCol - 1) != null)
				{
					return true;
				}
				break;

			case DOWN:
				// Attempt building down if this is the top letter in a group.
				if (iRow > 0 &&
					m_board.getTile(iRow - 1, iCol) != null)
				{
					return true;
				}
				break;

			case ABOVE:
				// If there is space, try a word above.
				if (iRow == 0 ||
					m_board.getTile(iRow - 1, iCol) != null ||
					(iRow > 1 && m_board.getTile(iRow - 2, iCol) != null) ||
					(iCol > 0 && m_board.getTile(iRow - 1, iCol - 1) != null))
				{
					return true;
				}
				break;

			case BEFORE:
				// If there is space, try a word before.
				if (iCol == 0 ||
					m_board.getTile(iRow, iCol - 1) != null ||
					(iCol > 1 && m_board.getTile(iRow, iCol - 2) != null) ||
					(iRow > 0 && m_board.getTile(iRow - 1, iCol - 1) != null))
				{
					return true;
				}
				break;

			case BELOW:
				// If there is space, try a word below.
				if (iRow + 1 >= m_nBoardSize ||
					m_board.getTile(iRow + 1, iCol) != null)
				{
					return true;
				}
				break;
                  
			case AFTER:
				// If there is space, try a word above.
				if (iCol + 1 >= m_nBoardSize ||
					m_board.getTile(iRow, iCol + 1) != null)
				{
					return true;
				}
				break;

			default:
				return true;
		}
		return false;
	}
}

//**************************************************************************************************
