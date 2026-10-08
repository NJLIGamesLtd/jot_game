//**************************************************************************************************
//
//  BestWord.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

class BestWord: ScanWords
{
	//**********************************************************************************************

	enum Orientation: CaseIterable
	{
		case ACROSS		// Horizontal word across an existing tile(s).
		case DOWN		// Vertical word across an existing tile(s).
		case ABOVE		// Horizontal word above an existing tile(s).
		case BEFORE		// Vertical word before an existing tile(s).
		case BELOW		// Horizontal word below an existing tile(s).
		case AFTER		// Vertical word after an existing tile(s).
	}

	let EMPTY: UInt8 = 0x20 //" "
	let NEED: UInt8 = 0x40 //"@"

	var m_topWords: TopWords
	var m_nWorstTopScore = 0
	var m_iWorstTopWord = 0
	
	var m_game: Game?
	var m_boardBase: Board?
	var m_board: Board?
	var m_words: Words?
	var m_nBoardSize = 0

	var m_bBestVertical = false
	var m_bVertical = false
	var m_iRow = 0
	var m_iColumn = 0
	var m_iBestRow = 0
	var m_iBestColumn = 0
	var m_nBestScore = 0
	var m_nBestLimit = 0

	var m_aBestWord: [UInt8]

	var m_aPattern: [UInt8]
	var m_rack: Rack?

	var m_iStartRow = 0
	var m_iStartColumn = 0
	var m_iNeedRow = 0
	var m_iNeedColumn = 0
	
	//**********************************************************************************************
	init()
	{
//		m_game = game
		
		m_aBestWord = Array(repeating: 0, count: m_nBoardSize + 1)
		m_aPattern = Array(repeating: 0, count: m_nBoardSize + 1)
		m_topWords = TopWords(nBoardSize: m_nBoardSize)
		
		m_nWorstTopScore = 0
		m_iWorstTopWord = 0
	}
    
	//**********************************************************************************************
    func
	setGame(game: Game)
    {
        m_game = game
        m_boardBase = game.getBoard()
        m_board = Board(board: m_boardBase!)
        m_words = game.getWords().clone()
        m_nBoardSize = game.getBoardSize()
        m_rack = game.getRack(iPlayer: 0)
    }

	//**********************************************************************************************
	// Create a pattern of letters and blank squares to build our tiles across.
	func
	createPattern(
		aPattern: inout [UInt8],
		iStart: Int,
		iRow: Int,
		iColumn: Int)
	{
		var iStart = iStart
		var	len = 0

		repeat
		{
			// If we have a tile, mark our pattern with its letter.
			var ch: UInt8 = EMPTY

			// Remove markers on exposed wildcards.
            if let tile = m_board!.getTile(iRow: iRow, iColumn: iStart)
			{
				ch = tile.getLetter()
			}
			// Else, if this is a required square, mark it as NEED.
			else if (iStart == iColumn)
			{
				ch = NEED
			}
			aPattern[len] = ch
			len += 1
			iStart += 1
		} while (iStart < m_nBoardSize)

		while (len < aPattern.count)
		{
			aPattern[len] = 0
			len += 1
		}
	}

	//**********************************************************************************************
	func
	getBestWord() -> String
	{
		return String(m_aBestWord)
	}

	//**********************************************************************************************
	// Search the entire board for the best word that can be made with these letter tiles.  The
	// word and its location is returned via parameters with its Scrabble score as the return value.
	func
	getBestWord(
		iPlayer: Int,
		rack: Rack,
		nRanking: Int,
		nScore: Int
	) throws -> Int
	{
		// Make sure we have a rack of tiles to work with.
		m_rack = rack

		// In order to avoid overplaying a user, put a limit of word scores based on ranking.
		m_nBestLimit = 10000
		if (nRanking > 0)
		{
			if (nRanking < 200)
			{
				m_nBestLimit = 30
			}
			else if (nRanking < 300)
			{
				m_nBestLimit = 40
			}
			else if (nRanking < 400)
			{
				m_nBestLimit = 50
			}
			else if (nRanking < 450)
			{
				m_nBestLimit = 60
			}
			else if (nRanking < 500)
			{
				m_nBestLimit = 100
			}
			else if (nRanking < 550)
			{
				m_nBestLimit = 150
			}
		}
		else if (nScore != 0)
		{
			m_nBestLimit = nScore + 5
		}

		// Copy the updated tiles from the base board.
        m_board!.copyTiles(board: m_boardBase!)

		m_iRow = 0
		m_iColumn = 0
		m_bVertical = false

		m_bBestVertical = false
		m_iBestRow = 0
		m_iBestColumn = 0

		m_nBestScore = 0
		m_nWorstTopScore = 0
		m_iWorstTopWord = 0
		m_topWords.m_nWords = 0
		
		try searchBoardForBestWord()

		return m_nBestScore
	}

	//**********************************************************************************************
	func
	getTopWords() -> TopWords
	{
		return m_topWords
	}

	//**********************************************************************************************
	// Get the potential starting poInt and required build square for a new word.  Return false
	// if there is no starting square.
	func
	getStartingSquare(
		iRowStart: Int,
		iColStart: Int,
		eOrient: Orientation
	) -> Bool
	{
		var iRow = iRowStart
		var iCol = iColStart
		m_bVertical = false
		m_iStartRow = iRow
		m_iNeedRow = iRow
		m_iStartColumn = iRow
		m_iNeedColumn = iCol

		switch (eOrient)
		{
			// Trying to build a horizontal word on this or an adjoining row.
			case .ABOVE:
				iRow -= 1
				if (iRow < 0)
				{
					return false
				}
				// fallthru

			case .BELOW:
				if (eOrient == .BELOW)
				{
					iRow += 1
					if (iRow >= m_nBoardSize)
					{
						return false
					}
				}
				m_iNeedRow = iRow
				// fallthru

			case .ACROSS:
				// Find the start of our usable pattern squares.
				while (iCol > 0)
				{
					iCol -= 1
                    if (m_board!.getTile(iRow: iRow, iColumn: iCol) != nil)
					{
						iCol += 2
						if (iCol >= m_nBoardSize)
						{
							return false
						}
						break
					}
				}
				m_iStartColumn = iCol
				break

				// Trying to build a vertical word on this or an adjoining column.
			case .BEFORE:
				iCol -= 1
				if (iCol < 0)
				{
					return false
				}
				// fallthru

			case .AFTER:
				if (eOrient == .AFTER)
				{
					iCol += 1
					if (iCol >= m_nBoardSize)
					{
						return false
					}
				}
				m_iNeedColumn = iCol
				// fallthru

			case .DOWN:
				// Find the start of our useable pattern squares.
				while (iRow > 0)
				{
					iRow -= 1
                    if (m_board!.getTile(iRow: iRow, iColumn: iCol) != nil)
					{
						iRow += 2
						if (iRow >= m_nBoardSize)
						{
							return false
						}
						break
					}
				}
				m_iStartRow = iRow
				m_bVertical = true
				break
		}
		return true
	}

	//**********************************************************************************************
	func
	isVertical() -> Bool
	{
		return m_bBestVertical
	}

	//**********************************************************************************************
	// Place our BestWord.
	func
	placeBestWord(
		iPlayer: Int,
		rack: Rack?,
		board: Board
	) -> Bool
	{
		// Make sure we have a best word.
		if (m_nBestScore == 0)
		{
			return false
		}

		// Record the number of tiles on our rack, in case we get a bingo.
		var rack = rack
		if (rack == nil)
		{
            rack = m_game!.getRack(iPlayer: iPlayer)
		}
		if let rack = rack
		{
			// If we have a vertical word, flip the board.
			if (m_bBestVertical)
			{
				board.setOrientation(bRotate: true)
			}

			// Place our word tiles.
			let iRow = m_iBestRow
			var iColumn = m_iBestColumn
			var i = -1
			var cWord: UInt8 = 0x20 //" "
			while (true)
			{
				i += 1
				cWord = m_aBestWord[i]
				if (cWord == 0)
				{
					break
				}
				var tile = board.getTile(iRow: iRow, iColumn: iColumn)
				if (tile == nil)
				{
					tile = rack.grabTile(cLetter: cWord)
					if (tile == nil)
					{
						tile = rack.grabTile(cLetter: Tile.WILD)
						if (tile == nil)
						{
							// SPK: Find out if this could happen.
							//throw "BestWord: Can't find letter for robot generated word."
						}
						if let tile = tile
						{
							tile.setLetter(cLetter: cWord)
						}
					}
					board.setTile(iRow: iRow, iColumn: iColumn, tile: tile)
				}
				if let tile = tile
				{
					if (cWord != tile.getLetter())
					{
						//throw "BestWord: Mismatch of robot generated word across com.jot.game word."
					}
				}

				// Move to the next square.
				iColumn += 1
			}

			board.setOrientation(bRotate: false)
		}
		return true
	}

	//**********************************************************************************************
	// Check the next word to see if it uses our tiles and fits across the board.  If we find a
	// letter not in our tile list, prune over all words at this level for a different letter.
	func
	prune(
		aWord: [UInt8],
		iWord: Int
	) throws -> Int
	{
		// Remember the highest level into the word that we reached.
		var iLevel = 0

		// Initializing these here to appease the compiler.
		var xLast = 0
		var yFirst = 0
		var yLast = 0

		// Loop through starting locations for our word in the board pattern.
		var iStart = -1
		repeat
		{
			iStart += 1
			var xFirst = -1

			// Start at the next letter into the pattern.
			var iLen = -1
			var iPat = iStart
			var cPat: UInt8 = 0
			var bAllEmpty = true

			// Try to match the word to this pattern.
			var cWord: UInt8 = 0
			while (true)
			{
				iLen += 1
				cWord = aWord[iLen]
				if (cWord == 0)
				{
					break
				}
				// Remember the highest level into the word that we reached.
				if (iLen > iLevel)
				{
					iLevel = iLen
				}

				// Get the next square in the pattern.  If we reached the end, prune to the
				// next word.
				cPat = m_aPattern[iPat]
				if (cPat == 0)
				{
					iPat += 1
					break
				}
				iPat += 1

				// If the next square in the pattern is a letter, force a match.
				if (cPat >= Tile.ALPHA)
				{
					bAllEmpty = false
					if (cWord == cPat)
					{
						continue
					}
					break
				}

				// If we're at the square we need to reach, mark off the empty flag.
				if (cPat == NEED)
				{
					bAllEmpty = false
				}

				// Try to match this letter to those in our tile rack.
				var tile: Tile?
                var iTile = m_rack!.indexOf(cLetter: cWord)
				if (iTile >= 0)
				{
                    tile = m_rack!.remove(iTile: iTile)
				}
				else
				{
                    iTile = m_rack!.indexOf(cLetter: Tile.WILD)
					if (iTile < 0)
					{
						// No match.  Try the next iteration.
						break
					}
                    tile = m_rack!.remove(iTile: iTile)
					if let tile = tile
					{
						tile.setLetter(cLetter: cWord)
					}
				}

				// Remove this letter from our tile rack and cycle back for the next letter
				// in the word.
                m_board!.setTile(iRow: m_iRow, iColumn: m_iColumn + iStart + iLen, tile: tile)

				// Record the placement of these tiles.
				if (xFirst < 0)
				{
					xFirst = m_iColumn + iStart + iLen
					yFirst = m_iRow
				}
				xLast = m_iColumn + iStart + iLen
				yLast = m_iRow
			}

			// If we got to the end of our word, all for its extension.
			if (cWord == 0)
			{
				iLevel = iLen

				// If this word fits next to a played letter or a needed square,
				// and we have not hit the end of the pattern,
				// and the next pattern square is empty or the end, check it.
				if (!bAllEmpty &&
					cPat != 0 &&
					m_aPattern[iPat] < Tile.WILD)
				{
					// Make sure our cross words are also valid.
                    if (try m_board!.areValidWordsDown(
						iRowWord: m_iRow,
						iColStart: m_iColumn + iStart,
						iColEnd: m_iColumn + iPat))
					{
                        let nScore = m_board!.getWordScore(eState: .PLACED)
						if (nScore > m_nBestScore && nScore < m_nBestLimit)
						{
							m_nBestScore = nScore
							m_bBestVertical = m_bVertical
							m_iBestRow = m_iRow
							m_iBestColumn = m_iColumn + iStart
							m_aBestWord = aWord
						}

						// If we have a scorable word, try to add it as one of our top words.
						if (nScore > m_nWorstTopScore)
						{
							// Overwrite our current worst.
							var topWord: TopWord? = m_topWords.m_aTopWords[m_iWorstTopWord]

							// Find a new worst.
							var nWorst = nScore
							var iWorst = m_iWorstTopWord
							var i = -1
							while (true)
							{
								i += 1
								if (i >= m_topWords.m_nWords)
								{
									break
								}
								if (i == m_iWorstTopWord)
								{
									continue
								}

                                let worstWord: TopWord = m_topWords.getTopWord(iWord: i)!
								if (worstWord.isEqual(aWord: aWord))
								{
									if (nScore <= worstWord.m_nScore)
									{
										topWord = nil
									}
									else
									{
										topWord = worstWord
										iWorst = m_iWorstTopWord
										nWorst = m_nWorstTopScore
									}
									break
								}

								if (worstWord.m_nScore < nWorst)
								{
									iWorst = i
									nWorst = worstWord.m_nScore
								}
							}

							// If needed, save the location of the new worst word.
							if var topWord = topWord
							{
								if (m_topWords.m_nWords < TopWords.N_TOPWORDS)
								{
									if (iWorst < m_topWords.m_nWords &&
										nScore <= topWord.m_nScore)
									{
										iWorst = m_topWords.m_nWords
									}
									topWord = m_topWords.m_aTopWords[m_topWords.m_nWords]
									m_topWords.m_nWords += 1
								}
								else
								{
									m_nWorstTopScore = nWorst
								}
								m_iWorstTopWord = iWorst

								// If we make the cut (for now), save it.
								if (m_bVertical)
								{
									topWord.set(
										aWord: aWord,
										nScore: nScore,
										xFirst: yFirst,
										yFirst: xFirst,
										xLast: yLast,
										yLast: xLast);
								}
								else
								{
									topWord.set(
										aWord: aWord,
										nScore: nScore,
										xFirst: xFirst,
										yFirst: yFirst,
										xLast: xLast,
										yLast: yLast);
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
					for y in yFirst...yLast
					{
                        if let tile = m_board!.removePlacedTile(iRow: y, iColumn: xFirst)
						{
                            _ = m_rack!.add(tile: tile)
						}
					}
					xFirst += 1
				}
			}

			// If we hit the end of the pattern, prune at this level.
			if (cPat == 0)
			{
				break
			}
		} while (m_aPattern[iStart] == EMPTY)

		return iLevel
	}

	//**********************************************************************************************
	// Search the area for the best word based on its orientation around a square.
	func
	searchAreaForBestWord(
		iRow: Int,
		iCol: Int,
		eOrient: Orientation) throws
	{
		// Find starting poInt and required build square.
		if (getStartingSquare(iRowStart: iRow, iColStart: iCol, eOrient: eOrient))
		{
			// Search for the best word across this square.
			try searchSquareForBestWord(
				iStartRow: m_iStartRow,
				iStartCol: m_iStartColumn,
				iNeedRow: m_iNeedRow,
				iNeedCol: m_iNeedColumn)
		}
	}

	//**********************************************************************************************
	// Search the entire board for the best word that can be made with these letter tiles.  The
	// word and its location is returned via parameters with its score as the return value.
	func
	searchBoardForBestWord() throws
	{
		m_bVertical = false

		// Search for the best scoring word.
		for iRow in 0..<m_nBoardSize
		{
			for iCol in 0..<m_nBoardSize
			{
                let tile: Tile? = m_board!.getTile(iRow: iRow, iColumn: iCol)

				// If we have no letter to build across, see if we're in center.
				if (tile == nil)
				{
                    if (m_board!.isStart(iRow: iRow, iColumn: iCol))
					{
						try searchRowForBestWord(
							iStart: 0,
							iRow: iRow,
							iColumn: iCol)
					}
					continue
				}

				// We have a letter.  Try to build across, down or next to.
				for eOrient in Orientation.allCases
				{
					if (!skipArea(iRow: iRow, iColumn: iCol, eOrient: eOrient))
					{
						try searchAreaForBestWord(iRow: iRow, iCol: iCol, eOrient: eOrient)
					}
				}
			}
		}
	}

	//**********************************************************************************************
	// Search across a row for the best word.
	func
	searchRowForBestWord(
		iStart: Int,
		iRow: Int,
		iColumn: Int) throws
	{
		// Create a single pattern row to build across.
        createPattern(aPattern: &m_aPattern, iStart: iStart, iRow: iRow, iColumn: iColumn)

		// Fill in our search structure.
		m_iRow = iRow
		m_iColumn = iStart

		// Run the search for the highest scoring word across this row.
        try m_words!.scan(scanWords: self, bmScanFlags: ScanWordsConstants.SCAN_FLAG_QUICK)
	}

	//**********************************************************************************************
	// Search down or across through a square for the best word.
	func
	searchSquareForBestWord(
		iStartRow: Int,
		iStartCol: Int,
		iNeedRow: Int,
		iNeedCol: Int) throws
	{
		// If we're not building across, flip the board and coordinates to make it look like we are.
		if (m_bVertical)
		{
            m_board!.setOrientation(bRotate: true)
			try searchRowForBestWord(
				iStart: iStartRow,
				iRow: iNeedCol,
				iColumn: iNeedRow)
            m_board!.setOrientation(bRotate: false)
			m_bVertical = false
		}
		else
		{
			try searchRowForBestWord(
				iStart: iStartCol,
				iRow: iNeedRow,
				iColumn: iNeedCol)
		}
	}

	//**********************************************************************************************
	// Return true if we can skip an area based on the orientation around a square.
	func
	skipArea(
		iRow: Int,
		iColumn: Int,
		eOrient: Orientation
	) -> Bool
	{
		switch (eOrient)
		{
			case .ACROSS:
				// Only attempt an across build when it is the first letter in a group.
				if (iColumn > 0 &&
                    m_board!.getTile(iRow: iRow, iColumn: iColumn - 1) != nil)
				{
					return true
				}
				break

			case .DOWN:
				// Attempt building down if this is the top letter in a group.
				if (iRow > 0 &&
                    m_board!.getTile(iRow: iRow - 1, iColumn: iColumn) != nil)
				{
					return true
				}
				break

			case .ABOVE:
				// If there is space, try a word above.
				if (iRow == 0 ||
                    m_board!.getTile(iRow: iRow - 1, iColumn: iColumn) != nil ||
                    (iRow > 1 && m_board!.getTile(iRow: iRow - 2, iColumn: iColumn) != nil) ||
                    (iColumn > 0 && m_board!.getTile(iRow: iRow - 1, iColumn: iColumn - 1) != nil))
				{
					return true
				}
				break

			case .BEFORE:
				// If there is space, try a word before.
				if (iColumn == 0 ||
                    m_board!.getTile(iRow: iRow, iColumn: iColumn - 1) != nil ||
                    (iColumn > 1 && m_board!.getTile(iRow: iRow, iColumn: iColumn - 2) != nil) ||
                    (iRow > 0 && m_board!.getTile(iRow: iRow - 1, iColumn: iColumn - 1) != nil))
				{
					return true
				}
				break

			case .BELOW:
				// If there is space, try a word below.
				if (iRow + 1 >= m_nBoardSize ||
                    m_board!.getTile(iRow: iRow + 1, iColumn: iColumn) != nil)
				{
					return true
				}
				break
                  
			case .AFTER:
				// If there is space, try a word above.
				if (iColumn + 1 >= m_nBoardSize ||
                    m_board!.getTile(iRow: iRow, iColumn: iColumn + 1) != nil)
				{
					return true
				}
				break
		}
		return false
	}
}

//**************************************************************************************************
