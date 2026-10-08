//**************************************************************************************************
//
//  Board.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

class Board
{
	var m_bEmpty = false

	var m_aaSquares: [[Square]] = [[]]
	var m_nBingoPrize = 0
	var m_nBingoLength = 0
	var m_words: Words
	var m_bRotated = false
	var m_aWord: [UInt8]

	var m_size = 0

	var m_eStatus = PlayStatus.State.VALID

	var m_nPlacedScore = 0
	var m_iPlacedSquare = 0
	var m_iPlacedPlayer = 0
	var m_aLastScores: [Int] = []
	var m_aLastSquares: [Int] = []

	var m_placement = Placement()
	var m_strBoard = String()
	var m_sInvalidWords = Set()

	//**********************************************************************************************
	init(board: Board)
	{
		m_size = board.m_size
        m_aWord = [UInt8](repeating: 0, count: m_size + 1)

		m_nBingoPrize = board.m_nBingoPrize
		m_nBingoLength = board.m_nBingoLength
		m_bEmpty = board.m_bEmpty
		m_words = board.m_words

		m_eStatus = board.m_eStatus
		m_bRotated = false

		for i in 0..<m_size
		{
			for j in 0..<m_size
			{
                let square: Square = Square(square: board.m_aaSquares[i][j])
				m_aaSquares[i][j] = square

				// If our square has a blank, make a copy of it so we save the used letter.
				if let tile = square.getTile()
				{
					if (tile.getScore() == 0)
					{
						square.setTile(tile: Tile(tile: tile), bPlaced: false)
					}
				}
			}
		}

		m_aLastScores = board.m_aLastScores
		m_aLastSquares = board.m_aLastSquares

		m_iPlacedPlayer = board.m_iPlacedPlayer
		m_iPlacedSquare = board.m_iPlacedSquare

		m_nPlacedScore = getWordScore(eState: Tile.State.PLACED)
        if (m_nPlacedScore > 0)
		{
			m_aLastScores[m_iPlacedPlayer] = m_nPlacedScore
			m_aLastSquares[m_iPlacedPlayer] = m_iPlacedSquare
		}
	}

	//**********************************************************************************************
	init(
		aaBonuses: [[UInt8]],
		nBingoPrize: Int,
		nBingoLength: Int,
		words: Words,
		aaDefaultTiles: [[UInt8]]?,
		aTileScores: [UInt8])
	{
		// Save the bingo details for scoring.
		m_nBingoPrize = nBingoPrize
		m_nBingoLength = nBingoLength
		m_bEmpty = true
		m_words = words

		// Create an array for testing words.
		m_size = aaBonuses.count
		m_aWord = [UInt8](repeating: 0, count: m_size + 1)

		// Create the matrix of squares.
		for i in 0..<m_size
		{
			for j in 0..<m_size
			{
				// Create this square based on its score bonus.
				let iBonus = aaBonuses[i][j]
				m_aaSquares[i][j] = Square(iBonus: iBonus, iRow: i, iColumn: j)
				let square = m_aaSquares[i][j]

				// If we have default letter tiles on board, include them.
				if let aaDefaultTiles = aaDefaultTiles
				{
					let cLetter = aaDefaultTiles[i][j]
					if (cLetter != 0)
					{
						let tile =
							Tile(
								cLetter: cLetter,
								nScore: aTileScores[Int(cLetter)],
								iPlayer: 0)
						square.setTile(tile: tile, bPlaced: true)
						tile.setState(eState: Tile.State.CONFIRMED)
						m_bEmpty = false
					}
				}
			}
		}
	}

	//**********************************************************************************************
	func
	areValidWordsAcross(
		iColWord: Int,
		iRowStart: Int,
		iRowEnd: Int) throws -> Bool
	{
		for iRow in iRowStart..<iRowEnd
		{
			// Skip over check for tiles that have already been scored.
			if let tile = getTile(iRow: iRow, iColumn: iColWord)
			{
				if (tile.getState() == Tile.State.CONFIRMED)
				{
					continue
				}
			}
			else
			{
				continue;
			}

			// Scan back until we reach the start of potential word.
			var iCol = iColWord
			while (iCol > 0 && getTile(iRow: iRow, iColumn: iCol - 1) != nil)
			{
				iCol -= 1
			}

			// Parse the word into a string.
			var iLen = 0
			while (true)
			{
				if let tile = getTile(iRow: iRow, iColumn: iCol)
				{
					m_aWord[iLen] = tile.getLetter()
					iLen += 1
					iCol += 1
				}
				else
				{
					break
				}
			}
			m_aWord[iLen] = 0

			// If our word is at least two characters and is not a valid word, return false.
			if (iLen >= 2)
			{
				if (try !m_words.isWord(aWord: m_aWord))
				{
					return false
				}
			}
		}

		return true
	}

	//**********************************************************************************************
	func
	areValidWordsDown(
		iRowWord: Int,
		iColStart: Int,
		iColEnd: Int) throws -> Bool
	{
		for iCol in iColStart..<iColEnd
		{
			// Skip over check for tiles that have already been scored.
			if let tile = getTile(iRow: iRowWord, iColumn: iCol)
			{
				if (tile.isStateAtLeast(eState: .CROSSED))
				{
					continue
				}
			}

			// Scan back until we reach the start of potential word.
			var iRow = iRowWord
			while (iRow > 0 && getTile(iRow: iRow - 1, iColumn: iCol) != nil)
			{
				iRow -= 1
			}

			// Parse the word into a string.
			var iLen = 0
			while (true)
			{
				let tile = getTile(iRow: iRow, iColumn: iCol)
				if let tile = tile
				{
					m_aWord[iLen] = tile.getLetter()
					iLen += 1
					iRow += 1
				}
				else
				{
					break
				}
			}
			m_aWord[iLen] = 0

			// If our word is at least two characters and is not a valid word, return false.
			if (iLen >= 2)
			{
				if (try !m_words.isWord(aWord: m_aWord))
				{
					return false
				}
			}
		}

		return true
	}

	//**********************************************************************************************
	func
	areValidWords(eState: Tile.State) throws -> Bool
	{
		// Find the placement of the placed/played word.
		if let placement = getWordPlacement(eState: eState)
		{
			// Check for a horizontal and vertical words.
			return
				try areValidWordsAcross(
					iColWord: placement.m_xFirst,
					iRowStart: placement.m_yFirst,
					iRowEnd: placement.m_yLast + 1) &&
				areValidWordsDown(
					iRowWord: placement.m_yFirst,
					iColStart: placement.m_xFirst,
					iColEnd: placement.m_xLast + 1)
		}
		return false
	}

	//**********************************************************************************************
	func
	clearPlacedScore()
	{
		m_nPlacedScore = 0
	}

	//**********************************************************************************************
	func
	copyTiles(board: Board)
	{
		for i in 0..<m_size
		{
			for j in 0..<m_size
			{
				setTile(iRow: i, iColumn: j, tile: board.getTile(iRow: i, iColumn: j))
			}
		}
		m_bEmpty = board.m_bEmpty
	}

	//**********************************************************************************************
	func
	getEncoding() -> String
	{
		m_strBoard = ""
		for iRow in 0..<m_size
		{
			for iColumn in 0..<m_size
			{
				m_strBoard.append(Tile.GetEncoding(tile: m_aaSquares[iRow][iColumn].getTile()))
			}
			m_strBoard.append("\n")
		}
		return m_strBoard
	}

	//**********************************************************************************************
	func
	getInvalidWords() throws -> Set<String>
	{
		// Clear our potential list of invalid words.
		m_sInvalidWords.removeAll(keepingCapacity: true)

		// Check for all horizontal words.
		for iRow in 0..<m_size
		{
			var iLen = 0
			var bPlayed = false
			for iColumn in 0..<m_size
			{
				if let tile = getTile(iRow: iRow, iColumn: iColumn)
				{
					m_aWord[iLen] = tile.getLetter()
					iLen += iLen
					if (!tile.isStateAtLeast(eState: .CROSSED))
					{
						bPlayed = true
					}
					continue
				}

				if (bPlayed && iLen >= 2)
				{
					m_aWord[iLen] = 0
					if (try !m_words.isWord(aWord: m_aWord))
					{
						if let strWord = String(bytes: m_aWord, encoding: .isoLatin1)
						{
							m_sInvalidWords.insert(strWord)
						}
					}
				}
				iLen = 0
			}

			if (bPlayed && iLen >= 2)
			{
				m_aWord[iLen] = 0
				if (try !m_words.isWord(aWord: m_aWord))
				{
					if let strWord = String(bytes: m_aWord, encoding: .isoLatin1)
					{
						m_sInvalidWords.insert(strWord)
					}
				}
				iLen = 0
			}
		}

		// Check for all horizontal words.
		for iColumn in 0..<m_size
		{
			var iLen = 0
			var bPlayed = false
			for iRow in 0..<m_size
			{
				if let tile = getTile(iRow: iRow, iColumn: iColumn)
				{
					m_aWord[iLen] = tile.getLetter()
					iLen += 1
					if (!tile.isStateAtLeast(eState: .CROSSED))
					{
						bPlayed = true
					}
					continue
				}

				if (bPlayed && iLen >= 2)
				{
					m_aWord[iLen] = 0
					if (try !m_words.isWord(aWord: m_aWord))
					{
						if let strWord = String(bytes: m_aWord, encoding: .isoLatin1)
						{
							m_sInvalidWords.insert(strWord)
						}
					}
				}
				iLen = 0
			}

			if (bPlayed && iLen >= 2)
			{
				m_aWord[iLen] = 0
				if (try !m_words.isWord(aWord: m_aWord))
				{
					if let strWord = String(bytes: m_aWord, encoding: .isoLatin1)
					{
						m_sInvalidWords.insert(strWord)
					}
				}
				iLen = 0
			}
		}

		return m_sInvalidWords
	}

	//**********************************************************************************************
	func
	getLastScore(iPlayer: Int) -> Int
	{
		m_nPlacedScore = getWordScore(eState: .PLACED)
		if (m_nPlacedScore > 0 &&
			m_iPlacedPlayer == iPlayer)
		{
			return m_nPlacedScore
		}
		return m_aLastScores[iPlayer]
	}

	//**********************************************************************************************
	func
	getLastSquare(iPlayer: Int) -> Int
	{
		if (m_nPlacedScore > 0 &&
			m_iPlacedPlayer == iPlayer)
		{
			return m_iPlacedSquare
		}
		if (m_aLastScores[iPlayer] > 0)
		{
			return m_aLastSquares[iPlayer]
		}
		return 0
	}

	//**********************************************************************************************
	func
	getLetter(
		iRow: Int,
		iColumn: Int
	) -> UInt8
	{
		if (m_bRotated)
		{
			return m_aaSquares[iColumn][iRow].getLetter()
		}
		return m_aaSquares[iRow][iColumn].getLetter()
	}

	//**********************************************************************************************
	// Fibonacci scan out from the center for the first open square.
	func
	getOpenSquare() -> Square?
	{
		var iRow = m_size >> 1
		var iColumn = iRow
		
		var iStep = 0
		var nStage = 0
		var nShift = 2

		while (getTile(iRow: iRow, iColumn: iColumn) != nil)
		{
			switch (nStage)
			{
				case 0:
					if (iStep == 0)
					{
						iRow -= 1
						break
					}
					iColumn += 1
					break
				case 1:
					iRow += 1
					break
				case 2:
					iColumn -= 1
					break
				case 3:
					iRow -= 1
					break
				default:
					break
			}

			iStep += 1
			if (iStep >= nShift)
			{
				iStep = 0
				nShift += 2
				nStage += 1
				if (nStage >= 4)
				{
					nStage = 0
				}
			}
		}

		return getSquare(iRow: iRow, iColumn: iColumn)
	}

	//**********************************************************************************************
	// Return true if the played letters have been placed as a single row or column of tiles that
	// either connect with a scored tile or make use of a center tile.
	func
	getPlayStatus() -> PlayStatus.State
	{		
		var nPlayed = 0

		// Find the placement of the new word.
		if let placement = getWordPlacement(eState: .PLACED)
		{
			// Check for an adjoining letter on the board.
			for y in 0..<m_size
			{
				for x in 0..<m_size
				{
					if let tile = getTile(iRow: y, iColumn: x)
					{
						switch (tile.getState())
						{
							case .PLAYED,
								 .CROSSED,
								 .SCORED,
								 .CONFIRMED:
								nPlayed += 1
								if (placement.m_xFirst == placement.m_xLast)
								{
									if (x == placement.m_xFirst)
									{
										if (y == placement.m_yFirst - 1 ||
											y == placement.m_yLast + 1)
										{
											return .VALID
										}
									}
									else if (
										x == placement.m_xFirst - 1 ||
										x == placement.m_xFirst + 1)
									{
										if (y >= placement.m_yFirst && y <= placement.m_yLast)
										{
											return .VALID
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
											return .VALID
										}
									}
									else if (
										y == placement.m_yFirst - 1 ||
										y == placement.m_yFirst + 1)
									{
										if (x >= placement.m_xFirst &&
											x <= placement.m_xLast)
										{
											return .VALID
										}
									}
								}
								break

							default:
								break
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
					return .SINGLE_LETTER
				}

				// Return true if we're on a starting position.
				if (isStart(
					iRowFirst: placement.m_xFirst,
					iRowLast: placement.m_xLast,
					iColumnFirst: placement.m_yFirst,
					iColumnLast: placement.m_yLast))
				{
					return .VALID
				}
				return .NOT_START
			}

			// No adjoining played letters.
			return .ISOLATED
		}

		// If we have no placed tiles, then we have no valid move.
		return m_eStatus
	}

	//**********************************************************************************************
	func
	getSize() -> Int
	{
		return m_size
	}

	//**********************************************************************************************
	func
	getSquare(
		iRow: Int,
		iColumn: Int
	) -> Square?
	{
		if (iRow < 0 || iRow >= m_size ||
			iColumn < 0 || iColumn >= m_size)
		{
			return nil
		}

		if (m_bRotated)
		{
			return m_aaSquares[iColumn][iRow]
		}
		return m_aaSquares[iRow][iColumn]
	}

	//**********************************************************************************************
	func
	getTile(
		iRow: Int,
		iColumn: Int
	) -> Tile?
	{
		if (iRow >= 0 && iRow < m_size &&
			iColumn >= 0 && iColumn < m_size)
		{
			if (m_bRotated)
			{
				return m_aaSquares[iColumn][iRow].getTile()
			}
			return m_aaSquares[iRow][iColumn].getTile()
		}
		return nil
	}

	//**********************************************************************************************
	func
	getTileIndex(eState: Tile.State) -> Int
	{
		var iTile = 0
		for iRow in 0..<m_size
		{
			for iColumn in 0..<m_size
			{
				if let tile = getTile(iRow: iRow, iColumn: iColumn)
				{
					if (tile.getState() == eState)
					{
						return iTile
					}
				}
				iTile += 1
			}
		}
		return -1
	}

	//**********************************************************************************************
	// Return the number of tiles on the board that have the given state.
	func
	getTilesPlaced(eState: Tile.State) -> Int
	{
		var nTiles = 0
		for y in 0..<m_size
		{
			for x in 0..<m_size
			{
				// If we have a nil tile, see if we've started our placed row/column.
				if let tile = getTile(iRow: y, iColumn: x)
				{
					if (tile.getState() == eState)
					{
						nTiles += 1
					}
				}
			}
		}
		return nTiles
	}

	//**********************************************************************************************
	// Return the tile placement exposure on the board.  Return nil if there are no placed tiles.
	func
	getTilePlacement(eState: Tile.State) -> Placement?
	{
		// Force our first values to the end.
		m_placement.m_xFirst = m_size
		m_placement.m_yFirst = m_size

		// Loop forward through the board.
		for y in 0..<m_size
		{
			for x in 0..<m_size
			{
				// If we have a nil tile, see if we've started our placed row/column.
				if let tile = getTile(iRow: y, iColumn: x)
				{
					if (tile.getState() == eState)
					{
						if (x < m_placement.m_xFirst)
						{
							m_placement.m_xFirst = x
						}
						if (y < m_placement.m_yFirst)
						{
							m_placement.m_yFirst = y
						}
						if (x > m_placement.m_xLast)
						{
							m_placement.m_xLast = x
						}
						if (y > m_placement.m_yLast)
						{
							m_placement.m_yLast = y
						}
					}
				}
			}
		}

		// If no tile placement was found in this state, return nil.
		if (m_placement.m_xFirst == m_size)
		{
			return nil
		}
		return m_placement
	}

	//**********************************************************************************************
	// Get the x1,y1/x2,y2 first/last tile placement of a word with the given tile state.
	// If not found, return a nil placement.
	func
	getWordPlacement(eState: Tile.State) -> Placement?
	{
		m_placement.m_xFirst = -1

		// Don't allow a nil tile in between our row/column of placed tiles.
		var xNil = -1
		var yNil = -1

		// By default, mark our status as valid.
		m_eStatus = .VALID

		// Make two passes through the squares.  The first to find the location of the new word.
		var iPlaced = -1
		for y in 0..<m_size
		{
			for x in 0..<m_size
			{
				iPlaced += 1
				
				// If we have a nil tile, see if we've started our placed row/column.
				if let tile = getTile(iRow: y, iColumn: x)
				{
					// If this tile matches our state, extend our row/column to fit it.
					if (tile.getState() == eState)
					{
						// If this is our first placed tile, the record its spot and continue.
						if (m_placement.m_xFirst < 0)
						{
							m_iPlacedSquare = iPlaced
							m_iPlacedPlayer = tile.getPlayer()
							m_placement.m_xFirst = x
							m_placement.m_xLast = x
							m_placement.m_yFirst = y
							m_placement.m_yLast = y
							continue
						}

						// If this tile is not inline with the placement, return nil.
						if (x != m_placement.m_xFirst &&
							y != m_placement.m_yFirst)
						{
							m_eStatus = .NOT_ALIGNED
							return nil
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
								m_eStatus = .NOT_ALIGNED;
								return nil
							}

							if (m_placement.m_yFirst != m_placement.m_yLast &&
								x != m_placement.m_xFirst)
							{
								m_eStatus = .NOT_ALIGNED
								return nil
							}

							// If we have a nil tile placed between our first and last, return nil.
							if (xNil == m_placement.m_xFirst &&
								yNil < y)
							{
								m_eStatus = .NOT_ALIGNED
								return nil
							}
						}
						else
						{
							if (y != m_placement.m_yFirst)
							{
								m_eStatus = .NOT_ALIGNED
								return nil
							}

							// If we have a nil tile placed between our first and last, return nil.
							if (yNil == m_placement.m_yFirst &&
								xNil < x)
							{
								m_eStatus = .NOT_ALIGNED
								return nil
							}
						}

						// Record the new last placed tile.
						m_placement.m_xLast = x
						m_placement.m_yLast = y
					}
				}
				else
				{
					if (m_placement.m_xFirst >= 0)
					{
						if (m_placement.m_xFirst == m_placement.m_xLast)
						{
							if (x == m_placement.m_xFirst ||
								m_placement.m_yFirst == m_placement.m_yLast)
							{
								xNil = x
								yNil = y
							}
						}
						else
						{
							if (y == m_placement.m_yFirst)
							{
								xNil = x
								yNil = y
							}
						}
					}
				}
			}
		}

		// If we have no placed word, return nil.
		if (m_placement.m_xFirst < 0)
		{
			m_eStatus = .NO_PLACEMENT
			return nil
		}
		return m_placement
	}

	//**********************************************************************************************
	func
	getWordScore(eState: Tile.State) -> Int
	{
		// Find the placement of the placed/played word.
		if (getWordPlacement(eState: eState) == nil)
		{
			return 0
		}

		// Mark all tiles that cross the placed/played word as CROSSED.
		markCrossedTiles(eState: eState)

		// Tally the total score.
		var nScoreTotal = 0

		// Total the score of each horizontal word.
		for i in 0..<m_size
		{
			var	nScoreMultiply = 1
			var nScoreWord = 0
			var nWordLength = 0
			var nBingoLength = 0
			var nPlayed = 0
			for j in 0..<m_size
			{
				if let square = getSquare(iRow: i, iColumn: j)
				{
					if let tile = square.getTile()
					{
						var	nScoreTile = tile.getScore()

						switch (tile.getState())
						{
							case .CROSSED:
								nWordLength += 1
								break

							case .PLACED,
								 .PLAYED:
								// Handle bonus tiles.
								nPlayed += 1
								switch (square.getBonus())
								{
									case .L2,
										 .L21,
										 .L22,
									 	 .L23,
									 	 .L24:
										nScoreTile *= 2
										break

									case .L3:
										nScoreTile *= 3
										break

									case .L4:
										nScoreTile *= 4
										break

									case .W2:
										nScoreMultiply *= 2
										break

									case .W3:
										nScoreMultiply *= 3
										break

									case .W4:
										nScoreMultiply *= 4
										break

									default:
										break
								}
								nWordLength += 1
								nBingoLength += 1
								break

							default:
								nScoreTile = 0
								break
						}
						nScoreWord += nScoreTile
					}
				}
			}
			if (nWordLength > 1 && nPlayed > 0)
			{
				nScoreTotal += nScoreWord * nScoreMultiply
				if (nBingoLength == m_nBingoLength)
				{
					nScoreTotal += m_nBingoPrize
				}
			}
		}

		// Total the score of each vertical word.
		for j in 0..<m_size
		{
			var	nScoreMultiply = 1
			var nScoreWord = 0
			var nWordLength = 0
			var nBingoLength = 0
			var nPlayed = 0
			for i in 0..<m_size
			{
				if let square = getSquare(iRow: i, iColumn: j)
				{
					if let tile = square.getTile()
					{
						var	nScoreTile = tile.getScore()

						switch (tile.getState())
						{
							case .CROSSED:
								nWordLength += 1
								tile.setState(eState: .CONFIRMED)
								break

							case .PLACED,
								 .PLAYED:
								nPlayed += 1
								// Handle bonus tiles.
								switch (square.getBonus())
								{
									case .L2,
										 .L21,
										 .L22,
									 	 .L23,
									 	 .L24:
										nScoreTile *= 2
										break

									case .L3:
										nScoreTile *= 3
										break

									case .L4:
										nScoreTile *= 4
										break

									case .W2:
										nScoreMultiply *= 2
										break

									case .W3:
										nScoreMultiply *= 3
										break

									case .W4:
										nScoreMultiply *= 4
										break

									default:
										break
								}
								nWordLength += 1
								nBingoLength += 1
								break

							default:
								nScoreTile = 0
								break
						}
						nScoreWord += nScoreTile
					}
				}
			}
			if (nWordLength > 1 && nPlayed > 0)
			{
				nScoreTotal += nScoreWord * nScoreMultiply
				if (nBingoLength == m_nBingoLength)
				{
					nScoreTotal += m_nBingoPrize
				}
			}
		}

		return nScoreTotal
	}

	//**********************************************************************************************
	// Return true if the board is currently empty of any played words.
	func
	isEmpty() -> Bool
	{
		return m_bEmpty
	}

	//**********************************************************************************************
	// Return true the given position falls within the center of the board.
	func
	isStart(
		iRow: Int,
		iColumn: Int
	) -> Bool
	{
		// If the board already contains a scored word, return false.
		if (!m_bEmpty)
		{
			return false
		}
		
		// If we have an even board, there are four center squares.
		if ((m_size & 1) == 0)
		{
			let center = m_size >> 2
			return
				center == ((iRow + 1) >> 1) &&
				center == ((iColumn + 1) >> 1)
		}

		// Odd sized boards have to make use of the single center square.
		let center = m_size >> 1
		return
			center == iRow &&
			center == iColumn
	}

	//**********************************************************************************************
	// Return true the given position range falls within the center of the board.
	func
	isStart(
		iRowFirst: Int,
		iRowLast: Int,
		iColumnFirst: Int,
		iColumnLast: Int
	) -> Bool
	{
		// If the board already contains a scored word, return false.
		if (!m_bEmpty)
		{
			return false
		}

		// If we have an even board, there are four center squares.
		if ((m_size & 1) == 0)
		{
			let center = m_size >> 2
			return
				center >= ((iRowFirst + 1) >> 1) &&
				center <= ((iRowLast + 1) >> 1) &&
				center >= ((iColumnFirst + 1) >> 1) &&
				center <= ((iColumnLast + 1) >> 1)
		}

		// Odd sized boards have to make use of the single center square.
		let center = m_size >> 1
		return
			center >= iRowFirst &&
			center <= iRowLast &&
			center >= iColumnFirst &&
			center <= iColumnLast
	}

	//**********************************************************************************************
	// Return true if there is a PLACED or PLAYED letter on the board.
	func
	isTilePlacedOrPlayed(iPlayer: Int) -> Bool
	{
		// Cycle through all the tiles.
		for y in 0..<m_size
		{
			for x in 0..<m_size
			{
				if let tile = getTile(iRow: x, iColumn: y)
				{
					if ((iPlayer < 0 || iPlayer == tile.getPlayer()) &&
						(tile.getState() == Tile.State.PLACED ||
						 tile.getState() == Tile.State.PLAYED))
					{
						return true
					}
				}
			}
		}
		return false
	}

	//**********************************************************************************************
	func
	isRotated() -> Bool
	{
		return m_bRotated
	}

	//**********************************************************************************************
	// Run a horizontal/verticle scan for this scored tile to connect with the played word.
	// Those tile(s) that do extend to the played word will be marked as crossed.
	func
	markCrossedWord(
		eState: Tile.State,
		x: Int,
		y: Int,
		bHorizontal: Bool)
	{
		var bFoundPlayed = false
		var x = x
		var y = y

		if (bHorizontal)
		{
			// Check the previous square for a played tile.
			if let tile = getTile(iRow: x - 1, iColumn: y)
			{
				if (tile.getState() == eState)
				{
					bFoundPlayed = true
				}
			}

			// And then to the end of the connected tiles.
			x += 1
			while (true)
			{
				if let tile = getTile(iRow: x, iColumn: y)
				{
					if (tile.getState() == eState)
					{
						bFoundPlayed = true
					}
				}
				else
				{
					break
				}
				x += 1
			}

			// If we are connected to a played tile, mark these all as crossed.
			if (bFoundPlayed)
			{
				x -= 1
				while (true)
				{
					if let tile = getTile(iRow: x, iColumn: y)
					{
						if (tile.isStateAtLeast(eState: .SCORED))
						{
							tile.setState(eState: .CROSSED)
						}
					}
					else
					{
						break
					}
					x -= 1
				}
			}
		}
		else
		{
			// Check the previous square for a played tile.
			if let tile = getTile(iRow: x, iColumn: y - 1)
			{
				if (tile.getState() == eState)
				{
					bFoundPlayed = true
				}
			}

			// And then to the end of the connected tiles.
			y += 1
			while (true)
			{
				if let tile = getTile(iRow: x, iColumn: y)
				{
					if (tile.getState() == eState)
					{
						bFoundPlayed = true
					}
				}
				else
				{
					break
				}
				y += 1
			}

			// If we are connected to a played tile, mark these all as crossed.
			if (bFoundPlayed)
			{
				y -= 1
				while (true)
				{
					if let tile = getTile(iRow: x, iColumn: y)
					{
						if (tile.isStateAtLeast(eState: .SCORED))
						{
							tile.setState(eState: .CROSSED)
						}
					}
					else
					{
						break
					}
					y -= 1
				}
			}
		}
	}

	//**********************************************************************************************
	// Scan the board for scored tiles that connect with the newly played tiles, and mark them as
	// crossed so they can be rescored.
	func
	markCrossedTiles(eState: Tile.State)
	{
		// Make sure there is an adjoining letter.
		for y in 0..<m_size
		{
			for x in 0..<m_size
			{
				if let tile = getTile(iRow: x, iColumn: y)
				{
					if (tile.isStateAtLeast(eState: .SCORED))
					{
						// Run a horizontal and vertical scan for tiles to mark as CROSSED.
						markCrossedWord(eState: eState, x: x, y: y, bHorizontal: true)
						markCrossedWord(eState: eState, x: x, y: y, bHorizontal: false)
					}
				}
			}
		}
	}

	//**********************************************************************************************
	// Scan the board for scored tiles that connect with the newly played tiles, and mark them as
	// crossed so they can be rescored.
	func
	markTiles(
		eStateFrom: Tile.State,
		eStateTo: Tile.State)
	{
		// Cycle through all the tiles.
		for y in 0..<m_size
		{
			for x in 0..<m_size
			{
				if let tile = getTile(iRow: x, iColumn: y)
				{
					if (tile.getState() == eStateFrom)
					{
						tile.setState(eState: eStateTo)
					}
				}
			}
		}
	}

	//**********************************************************************************************
	func
	markTileFound(
		iRow: Int,
		iColumn: Int)
	{
		if (iRow >= 0 && iRow < m_size &&
			iColumn >= 0 && iColumn < m_size)
		{
			if let tile = getTile(iRow: iRow, iColumn: iColumn)
			{
				if (!tile.isFound())
				{
					tile.markFound(bFound: true)
					markTileFound(iRow: iRow - 1, iColumn: iColumn)
					markTileFound(iRow: iRow, iColumn: iColumn - 1)
					markTileFound(iRow: iRow + 1, iColumn: iColumn)
					markTileFound(iRow: iRow, iColumn: iColumn + 1)
				}
			}
		}
	}
	
	//**********************************************************************************************
	func
	markConnectedTiles()
	{
		markTilesFound(bFound: false)

		// Mark all tiles and their connected tiles from the center squares.
		var y = ((m_size - 1) >> 1) - 1
		while (y < m_size >> 1)
		{
			y += 1
			var x = ((m_size - 1) >> 1) - 1
			while (x < m_size >> 1)
			{
				x += 1
				markTileFound(iRow: x, iColumn: y)
			}
		}
	}

	//**********************************************************************************************
	func
	markTilesFound(bFound: Bool)
	{
		for y in 0..<m_size
		{
			for x in 0..<m_size
			{
				if let tile = getTile(iRow: x, iColumn: y)
				{
					tile.markFound(bFound: bFound)
				}
			}
		}
	}

	/***********************************************************************************************
	 * If they form a valid placement, mark placed tiles as played.
	 */
	func
	playWord() -> PlayStatus.State
	{
		let eStatus = getPlayStatus()
		if (eStatus == .VALID)
		{
			for y in 0..<m_size
			{
				for x in 0..<m_size
				{
					if let tile = getTile(iRow: x, iColumn: y)
					{
						if (tile.getState() == .PLACED)
						{
							tile.setState(eState: .PLAYED)
						}
					}
				}
			}
		}

		return eStatus
	}

	//**********************************************************************************************
	func
	removePlacedTile(
		iRow: Int,
		iColumn: Int
	) -> Tile?
	{
		if let tile = getTile(iRow: iRow, iColumn: iColumn)
		{
			if (tile.getState() == Tile.State.PLACED)
			{
				setTile(iRow: iRow, iColumn: iColumn, tile: nil)
				return tile
			}
		}
		return nil
	}

	//**********************************************************************************************
	func
	reset()
	{
		// Remove all tiles from the board.
		for y in 0..<m_size
		{
			for x in 0..<m_size
			{
				setTile(iRow: y, iColumn: x, tile: nil)
			}
		}
		m_bRotated = false
		m_bEmpty = true
	}

	//**********************************************************************************************
	func
	scoreWord() -> Int
	{
		// Get the score of the played word.  If the score is negative, must be an invalid play.
		let nScoreWord = getWordScore(eState: .PLAYED)
		if (nScoreWord >= 0)
		{
			m_aLastSquares[m_iPlacedPlayer] = getTileIndex(eState: .PLAYED)
			m_aLastScores[m_iPlacedPlayer] = nScoreWord

			// Mark PLAYED tiles as SCORED.
			markTiles(eStateFrom: .PLAYED, eStateTo: .SCORED)
			m_bEmpty = false
		}

		return nScoreWord
	}

	//**********************************************************************************************
	func
	setBlankLetter(cLetter: UInt8)
	{
		for y in 0..<m_size
		{
			for x in 0..<m_size
			{
				if let tile = getTile(iRow: x, iColumn: y)
				{
					if (tile.getState() == .PLACED &&
						tile.getLetter() == Tile.WILD)
					{
						tile.setLetter(cLetter: cLetter)
						return
					}
				}
			}
		}
	}

	//**********************************************************************************************
	func
	setEmpty(bEmpty: Bool)
	{
		m_bEmpty = bEmpty
	}

	//**********************************************************************************************
	func
	setLastScore(
		iPlayer: Int,
		nLastScore: Int)
	{
		m_aLastScores[iPlayer] = nLastScore
	}

	//**********************************************************************************************
	func
	setLastSquare(
		iPlayer: Int,
		iLastSquare: Int)
	{
		m_aLastSquares[iPlayer] = iLastSquare
	}

	//**********************************************************************************************
	func
	setOrientation(bRotate: Bool)
	{
		m_bRotated = bRotate
	}

	//**********************************************************************************************
	func
	rotate()
	{
		m_bRotated = m_bRotated ? false : true
	}

	//**********************************************************************************************
	func
	setTile(
		iRow: Int,
		iColumn: Int,
		tile: Tile?)
	{
		// If we're placing a viewed tile, then clear the last score.
		if let tile = tile
		{
			m_aLastScores[tile.getPlayer()] = 0
		}

		// Assign the tile to the associated square.
		if (m_bRotated)
		{
			m_aaSquares[iColumn][iRow].setTile(tile: tile, bPlaced: true)
		}
		else
		{
			m_aaSquares[iRow][iColumn].setTile(tile: tile, bPlaced: true)
		}
	}

	//**********************************************************************************************
	func
	toString() -> String
	{
		var iRow = -1
		m_strBoard = ""
		while (true)
		{
			// Output a row divider.
			for _ in 0..<m_size
			{
				m_strBoard.append("----")
			}
			m_strBoard.append("-\n")

			iRow += 1
			if (iRow >= m_size)
			{
				break
			}

			// Output each column.
			for iCol in 0..<m_size
			{
				// Output a column divider.
				m_strBoard.append("|")
				m_strBoard.append(m_aaSquares[iRow][iCol].toString())
			}
			m_strBoard.append("|\n")
		}
		return m_strBoard
	}
}

//**************************************************************************************************
