/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Best Word searches and returns the highest scoring Scrabble word that can be made from the given
 * letters and board arrangement.
 */

package com.jot.crossword;

import com.jot.decomp.ScanWords;
import com.jot.decomp.Words;

//**************************************************************************************************

public class BestWord implements ScanWords
{
	//**********************************************************************************************

	enum Orientation
	{
		NONE,
		ACROSS,
		DOWN,
		ABOVE,
		BEFORE,
		BELOW,
		AFTER,
		END;

		public Orientation
		getNext()
		{
			return values()[ordinal() + 1];
		}
	}

	private Game m_game;
	private Board m_board;
	private Words m_words;
	private int m_nBoardSize;

	private boolean m_bBestVertical;
	private boolean m_bVertical;
	private int m_iRow;
	private int m_iColumn;
	private int m_iBestRow;
	private int m_iBestColumn;
	private int m_nBestScore;

	private char[] m_aBestWord;
	private int m_nBestWordLength;

	private char[] m_aPattern;
	private Tiles m_tiles;

	private int m_iStartRow;
	private int m_iStartColumn;
	private int m_iNeedRow;
	private int m_iNeedColumn;
	
	//**********************************************************************************************
	public
	BestWord(Game game)
	{
		m_game = game;
		m_board = game.getBoard();
		m_words = game.getWords().clone();
		m_nBoardSize = game.getBoardSize();
		m_aPattern = new char[m_nBoardSize + 1];
		m_aBestWord = new char[m_nBoardSize + 1];
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
				ch = Square.NEED;
			}
			else
			{
				ch = Square.EMPTY;
			}
			aPattern[len++] = ch;
		} while (++iStart < m_nBoardSize);

		while (len < aPattern.length)
		{
			aPattern[len++] = 0;
		}
	}

	//**********************************************************************************************
	// Search the entire board for the best word that can be made with these letter tiles.  The
	// word and its location is returned via parameters with its Scrabble score as the return value.
	public int
	getBestWord(int iPlayer) throws Exception
	{
		m_tiles = m_game.getRack(iPlayer);

		m_iRow = m_iColumn = 0;
		m_bVertical = false;

		m_bBestVertical = false;
		m_iBestRow = m_iBestColumn = 0;
		m_nBestWordLength = 0;

		m_nBestScore = 0;
		searchBoardForBestWord();

		return m_nBestScore;
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
				// falthru;

			case ACROSS:
				// Find the start of our usable pattern squares.
				while (iCol > 0)
				{
					if (m_board.getTile(iRow, --iCol) != null)
					{
						iCol += 2;
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
						iRow += 2;
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
	placeBestWord(int iPlayer) throws Exception
	{
		// Make sure we have a best word.
		if (m_nBestScore == 0)
		{
			return false;
		}
		
		// Record the number of tiles on our rack, in case we get a bingo.
		Tiles rack = m_game.getRack(iPlayer);
		int nRackTiles = rack.size();

		// If we have a vertical word, flip the board.
		if (m_bBestVertical)
		{
			m_board.setOrientation(true);
		}

		// Place our word tiles.
		int iRow = m_iBestRow;
		int iColumn = m_iBestColumn;
		char cWord;
		for (int i = 0; (cWord = m_aBestWord[i]) != 0; ++i)
		{
			Tile tile;
			if ((tile = m_board.getTile(iRow, iColumn)) == null)
			{
				tile = rack.grabTile(cWord);
				if (tile == null)
				{
					if ((tile = rack.grabTile(Tile.WILD)) == null)
					{
						throw new Exception("BestWord: Can't find letter for robot generated word.");
					}
					tile.setLetter(cWord);
				}
				m_board.setTile(iRow, iColumn, tile);
			}
			if (cWord != tile.getLetter())
			{
				throw new Exception("BestWord: Mismatch of robot generated word across game word.");
			}

			// Move to the next square.
			++iColumn;
		}

		m_board.setOrientation(false);

		return true;
	}

	//**********************************************************************************************
	// Check the next word to see if it uses our tiles and fits across the board.  If we find a
	// letter not in our tile list, prune over all words at this level for a different letter.
	public int
	prune(
		char[] aWord,
		int iWord
	) throws Exception
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
				if (cPat == Square.NEED)
				{
					bAllEmpty = false;
				}

				// Try to match this letter to those in our tile rack.
				Tile tile;
				int iTile = m_tiles.indexOf(cWord);
				if (iTile >= 0)
				{
					tile = m_tiles.remove(iTile);
				}
				else
				{
					if ((iTile = m_tiles.indexOf(Tile.WILD)) < 0)
					{
						// No match.  Try the next iteration.
						break;
					}
					tile = m_tiles.remove(iTile);
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
						if (nScore > m_nBestScore)
						{
							m_nBestScore = nScore;
							m_nBestWordLength = iLen;
							m_bBestVertical = m_bVertical;
							m_iBestRow = m_iRow;
							m_iBestColumn = m_iColumn + iStart;
							System.arraycopy(aWord, 0, m_aBestWord, 0, iLen + 1);
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
							m_tiles.add(tile);
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
		} while (m_aPattern[iStart++] == Square.EMPTY);

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
				if (iRow + 1 < m_nBoardSize ||
					m_board.getTile(iRow + 1, iCol) != null)
				{
					return true;
				}
				break;
                  
			case AFTER:
				// If there is space, try a word above.
				if (iCol + 1 < m_nBoardSize ||
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
