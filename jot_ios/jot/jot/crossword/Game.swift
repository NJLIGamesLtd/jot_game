//**************************************************************************************************
//
//  Game.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

class Game
{
	enum State
	{
		case DRAW_TILES
		case FIRST_WORD
		case PLACE_TILES
		case PLAY_TILES
		case PLAY_READY
		case MOVE_ROBOT
		case ROBOT_PASSED
		case QUIT
		case OVER
	}

	static let s_bmDefault = 0x0000
	static let s_bmChallengeMode = 0x0001
	static let s_bmTimeLimit = 0x0002

	var m_main: Main

	var m_aaBonuses: [[UInt8]]
	var m_aaDefaultTiles: [[UInt8]]? = nil
	var m_aTileBag: [UInt8]
	var m_aVowelBag: [UInt8]? = nil
	var m_aTileScores: [UInt8]
	var m_aTileRack: [UInt8]
	var m_nBingoPrize = 0

	var m_board: Board
	var m_boardBest: Board?

	var m_aRacks: [Rack] = []
	var m_rackBackup: Rack
	var m_aTileBags: [Tiles] = []
	var m_aVowelBags: [Tiles] = []
	var m_nTilesTotal = 0
	var m_nTilesRobot = 0

	var m_iPlayerTurn = 0
	var m_nPlayers = 0
	var m_iPlayerRobot = 0
	var m_aPlayerScores: [Int] = []
	var m_nLastScore = 0
	var m_nRackSize = 0
	var m_nBoardSize = 0
	var m_bBagShared = false

    var m_eState = State.DRAW_TILES
	var m_bmSettings = 0

	var m_bRackedTiles = false

	var m_words: Words
	var m_bestWord: BestWord

	var m_bFlingTop = false
	var m_bFlingVertical = false
	var m_iFlingDelta = 0
	var m_iFlingLast = 0

	var m_bBestWorking = false
	var m_bRobotWorking = false
	var m_bGameReset = false
	var m_bGameSwitched	= false  // A different or new game is in use.
	var m_bNoPlay = false
	var m_bSawInvalids = false
	var m_iRackFilled = 0

	var m_dbGame: DbGame? = nil

	var m_strBoard: String
	var m_strMessage: String? = nil

	var m_nOpponentTiles = 0

	//**********************************************************************************************
	convenience
	init(game: Game)
	{
		self.init(
			aaBonuses: game.m_aaBonuses,
			aaDefaultTiles: game.m_aaDefaultTiles,
			aTileBag: game.m_aTileBag,
			aVowelBag: game.m_aVowelBag,
			aTileScores: game.m_aTileScores,
			aTileRack: game.m_aTileRack,
			nBingoPrize: game.m_nBingoPrize,
			words: game.m_words,
			main: game.m_main,
			nPlayers: game.m_nPlayers,
			iPlayerRobot: game.m_iPlayerRobot,
			bBagShared: game.m_bBagShared)
	}

	//**********************************************************************************************
	convenience
	init(
		aaBonuses: [[UInt8]],
		aaDefaultTiles: [[UInt8]]?,
		aTileBag: [UInt8],
		aTileScores: [UInt8],
		aTileRack: [UInt8],
		words: Words,
		main: Main,
		nPlayers: Int)
	{
		self.init(
			aaBonuses: aaBonuses,
			aaDefaultTiles: aaDefaultTiles,
			aTileBag: aTileBag,
			aVowelBag: nil,
			aTileScores: aTileScores,
			aTileRack: aTileRack,
			nBingoPrize: 50,
			words: words,
			main: main,
			nPlayers: nPlayers,
			iPlayerRobot: 1,
			bBagShared: true)
	}

	//**********************************************************************************************
	init(
		aaBonuses: [[UInt8]],
		aaDefaultTiles: [[UInt8]]?,
		aTileBag: [UInt8],
		aVowelBag: [UInt8]?,
		aTileScores: [UInt8],
		aTileRack: [UInt8],
		nBingoPrize: Int,
		words: Words,
		main: Main,
		nPlayers: Int,
		iPlayerRobot: Int,
		bBagShared: Bool)
	{
		m_aaBonuses = aaBonuses
		m_aaDefaultTiles = aaDefaultTiles
		m_aTileBag = aTileBag
		m_aVowelBag = aVowelBag
		m_aTileScores = aTileScores
		m_aTileRack = aTileRack
		m_nBingoPrize = nBingoPrize

		// Store a count of the total tiles per player.
		m_nTilesTotal = aTileBag.count
		if let aVowelBag = aVowelBag
		{
			m_nTilesTotal += aVowelBag.count
		}
		m_nTilesRobot = m_nTilesTotal
		
		m_board = Board(
			aaBonuses: aaBonuses,
			nBingoPrize: nBingoPrize,
			nBingoLength: aTileRack.count,
			words: words,
			aaDefaultTiles: aaDefaultTiles,
			aTileScores: aTileScores)
		m_nBoardSize = m_board.getSize()
		m_nPlayers = nPlayers
		m_iPlayerRobot = iPlayerRobot
		m_bBagShared = bBagShared
		m_words = words
		m_main = main

		m_nLastScore = 0
		m_bBestWorking = false
		m_bRobotWorking = false
		m_bGameReset = false
		m_bGameSwitched = false
		m_bNoPlay = false
		m_iRackFilled = -1

		// Create a list of tile racks.
		m_nRackSize = aTileRack.count
		for iPlayer in 0..<nPlayers
		{
			m_aRacks[iPlayer] = Rack(nMaxTiles: m_nRackSize)
		}
		m_rackBackup = Rack(nMaxTiles: m_nRackSize)

		// Add each bag of tiles to our lists.
		var lTiles = Tiles()
		var lVowels = Tiles()
		for iPlayer in 0..<nPlayers
		{
			if (iPlayer == 0 || !bBagShared)
			{
				for iTile in 0..<m_aTileBag.count
				{
					let cLetter = m_aTileBag[iTile]
					_ = lTiles.add(tile: Tile(
						cLetter: cLetter, nScore: aTileScores[Int(cLetter)], iPlayer: iPlayer))
				}

				// If needed separate the vowels.
				if let aVowelBag = m_aVowelBag
				{
					for iVowel in 0..<aVowelBag.count
					{
						let cVowel = aVowelBag[iVowel]
						_ = lVowels.add(tile: Tile(
							cLetter: cVowel, nScore: aTileScores[Int(cVowel)], iPlayer: iPlayer))
					}
				}
			}
			m_aTileBags.append(lTiles)
			m_aVowelBags.append(lVowels)
		}

		// Remove tiles that have already been played.
		if let aaDefaultTiles = aaDefaultTiles
		{
			lTiles = m_aTileBags[0]
			lVowels = m_aVowelBags[0]

			for i in 0..<m_nBoardSize
			{
				for j in 0..<m_nBoardSize
				{
					let cLetter = aaDefaultTiles[i][j]
					if (cLetter != 0)
					{
						if let tile = lVowels.grabTile(cLetter: cLetter)
						{
							m_board.setTile(iRow: i, iColumn: j, tile: tile)
							tile.setState(eState: .CONFIRMED)

							// If our sample tile is WILD, mark it as 'K' for testing.
							if (cLetter == Tile.WILD)
							{
								tile.setLetter(cLetter: 0x4B /* 'K' */)
							}
							continue
						}

						if let tile = lTiles.grabTile(cLetter: cLetter)
						{
							m_board.setTile(iRow: i, iColumn: j, tile: tile)
							tile.setState(eState: .CONFIRMED)
						}
					}
				}
			}
		}

		// Get a string encoding of our game board.
		m_strBoard = m_board.getEncoding()

		// Add the default tiles to our rack.
		let rack: Rack = m_aRacks[0]
		lTiles = m_aTileBags[0]
		lVowels = m_aVowelBags[0]

		for i in 0..<aTileRack.count
		{
			let cLetter = aTileRack[i]
			if (cLetter != 0)
			{
				if let tile = lVowels.grabTile(cLetter: cLetter)
				{
					_ = rack.add(tile: tile)
					continue
				}
				if let tile = lTiles.grabTile(cLetter: cLetter)
				{
					_ = rack.add(tile: tile)
				}
			}
		}

		// Create our BestWord engine for finding the highest scoring words.
		m_bestWord = BestWord()

		setState(eState: .DRAW_TILES)
        
        m_bestWord.setGame(game: self)
	}

	//**********************************************************************************************
	func
	bagTile(tile: Tile)
	{
		let iPlayer = tile.getPlayer()
		if (tile.isVowel())
		{
			_ = m_aVowelBags[iPlayer].add(tile: tile)
		}
		else
		{
			_ = m_aTileBags[iPlayer].add(tile: tile)
		}
	}

	//**********************************************************************************************
	func
	clear()
	{
		// Bag all the tiles from the board.
		for i in 0..<m_nBoardSize
		{
			for j in 0..<m_nBoardSize
			{
				if let tile = getTile(iRow: i, iColumn: j)
				{
					bagTile(tile: tile)
				}
			}
		}

		// Reset the board.
		m_board.reset()
		m_boardBest = nil
	
		// Remove all tiles from the racks.
		for rack in m_aRacks
		{
			let nTiles = rack.getRackSize()
			for iTile in 0..<nTiles
			{
				if let tile = rack.remove(iTile: iTile)
				{
					bagTile(tile: tile)
				}
			}
		}

		// Clear the scores.
		m_iPlayerTurn = 0
		for iPlayer in 0..<m_nPlayers
		{
			m_aPlayerScores[iPlayer] = 0
		}
		m_nLastScore = 0
		m_nTilesRobot = m_nTilesTotal
	}

	//**********************************************************************************************
	func
	clearOpponentScore()
	{
		if let dbGame = m_dbGame
		{
			let iOpponent = dbGame.getPlayer() ^ 1
			m_aPlayerScores[iOpponent] = 0
			dbGame.setScore(iOpponent, 0)
		}
	}

	//**********************************************************************************************
	func
	confirmWord()
	{
		m_board.markTiles(eStateFrom: .SCORED, eStateTo: .CONFIRMED)
	}

	//**********************************************************************************************
	func
	delete()
	{
		clear()

		// Delete the database document, if we have one.
		if let dbGame = m_dbGame
		{
			dbGame.delete()
			m_dbGame = nil
		}
	}

	//**********************************************************************************************
	func
	drawAllTiles()
	{
		// Make sure we don't have any placed tiles on the board.
		_ = withdrawAllTiles()

		// Get access to our tile rack.
		let rack: Rack = getRack(iPlayer: m_iPlayerTurn)

		// Count up the number of vowels we have.
		var nRackVowels = rack.getVowelCount()

		// If there is room on our player's rack, see if we have letters to pick from.
		while (!isRackFilled(iPlayer: m_iPlayerTurn))
		{
			let nTiles = getTileCount(iPlayer: m_iPlayerTurn)
			let nVowels = getVowelCount(iPlayer: m_iPlayerTurn)

			// If there are no letters left to grab, we're done.
			if (nTiles == 0 && nVowels == 0)
			{
				break
			}

			// If our vowel count is less than half, try to pick one.
			if (nVowels > 0)
			{
				if (nTiles == 0 || nRackVowels < (rack.getTileCount() >> 1))
				{
					nRackVowels += 1
					_ = grabVowel()
					continue
				}
			}

			// Pick a normal tile.
			_ = grabTile()
		}
	}

	//**********************************************************************************************
	func
	drawTile(
		iPlayer: Int,
		cLetter: UInt8
	) -> Tile?
	{
		// If we have a vowel bag, try to grab it from this first.
		var tile = m_aVowelBags[iPlayer].grabTile(cLetter: cLetter)

		// Else, try to grab it from the main bag.
		if (tile == nil)
		{
			tile = m_aTileBags[iPlayer].grabTile(cLetter: cLetter)
		}

		// If we still don't have this tile, try the user's rack.
		if (tile == nil)
		{
			let rack: Rack = getRack(iPlayer: iPlayer)
			tile = rack.grabTile(cLetter: cLetter)
		}

		return tile
	}

	//**********************************************************************************************
	func
	endGame(iPlayer: Int)
	{
		// Loop through the players to see if someone went out.
		var iOut = -1
		var nTotalPoints = 0
		for i in 0..<m_nPlayers
		{
			// Get the rack details.
			let rack: Rack = getRack(iPlayer: i)
			var nPoints = rack.getPoints()

			// If this player has no tiles in their rack or bag, they went out first.
			if (rack.getTileCount() == 0 &&
				getTileCount(iPlayer: i) + getVowelCount(iPlayer: i) == 0)
			{
				iOut = i
			}

			// If each player has their own tile bag, get their points.
			if (!m_bBagShared)
			{
				nPoints += getTilePoints(iPlayer: i) + getVowelPoints(iPlayer: i)
			}

			// Subtract leftover points from the player's score.
			m_aPlayerScores[i] -= nPoints
			if (m_aPlayerScores[i] < 0)
			{
				m_aPlayerScores[i] = 0
			}
			nTotalPoints += nPoints
		}

		// Add the points leftover to the score of the player who when out.
		if (iOut >= 0)
		{
			m_aPlayerScores[iOut] += nTotalPoints
		}

		// Update the player's highest and average game score.
		setGameEndScore(iPlayer: iPlayer)

		// Mark the game as over.
		setState(eState: .OVER)
	}

	//**********************************************************************************************
	func
	findBestBoard(iPlayer: Int)
	{
		// Don't look for a new best word until it's own turn.
		if (iPlayer != m_iPlayerTurn || m_bBestWorking)
		{
			return
		}

		// Avoid triggering a redundant search.
		m_bBestWorking = true
		m_bNoPlay = false
		m_boardBest = null

		let queue = DispatchQueue(label: "Finding best...");
		queue.async
		{
			// Play the next word.
			do
			{
				self.m_boardBest = try self.getBestBoard()
				if (self.m_boardBest == nil)
				{
					m_bNoPlay = true
				}
			}
			catch
			{
				self.m_boardBest = nil
			}
			self.m_bBestWorking = false
		}
	}

	//**********************************************************************************************
	func
	getBestBoard() throws -> Board?
	{
		// Make a copy of the rack.
		let rack: Rack = getRack(iPlayer: m_iPlayerTurn)
		m_rackBackup.copyTiles(rack: rack)

		// Save the last score.
		let nLastScore = m_board.getLastScore(iPlayer: m_iPlayerTurn)
		
		// Find our BestWord.
		let nBestScore = try m_bestWord.getBestWord(
			iPlayer: m_iPlayerTurn,
			rack: m_rackBackup,
			nRanking: 0,
			nScore: 0)
		if (nBestScore == 0)
		{
			return nil
		}

		// Place our BestWord on the board.
		let boardBest = Board(board: m_board)
		_ = m_bestWord.placeBestWord(iPlayer: m_iPlayerTurn, rack: m_rackBackup, board: boardBest)

		// Restore the last score.
		m_board.setLastScore(iPlayer: m_iPlayerTurn, nLastScore: nLastScore)

		// Update last score to reflex the best word.
		boardBest.setLastScore(iPlayer: m_iPlayerTurn, nLastScore: nBestScore)
		boardBest.setLastScore(iPlayer: m_iPlayerTurn ^ 1, nLastScore: 0)

		return boardBest
	}

	//**********************************************************************************************
	func
	getBoard() -> Board
	{
		return m_board
	}

	//**********************************************************************************************
	func
	getBoardBest() -> Board?
	{
		return m_boardBest
	}

	//**********************************************************************************************
	func
	getBoardHeight() -> Int
	{
		return m_nBoardSize
	}

	//**********************************************************************************************
	func
	getBoardSize() -> Int
	{
		return m_nBoardSize
	}

	//**********************************************************************************************
	func
	getBoardWidth() -> Int
	{
		return m_nBoardSize
	}

	//**********************************************************************************************
	func
	getDbGame() -> DbGame?
	{
		return m_dbGame
	}

	//**********************************************************************************************
	func
	getFlingSquare(
		tile: Tile,
		iRackTile: Int,
		bRotated: Bool
	) -> Square?
	{
		// Don't fling tiles onto the board while the computer is searching for the best word.
		if (m_bBestWorking)
		{
			return nil
		}

		// Get the placement of other tiles.
		if let placement = m_board.getWordPlacement(eState: .PLACED)
		{
			// If we have at least two tiles in the placement, use its orientation.
			if (placement.m_xFirst != placement.m_xLast ||
				placement.m_yFirst != placement.m_yLast)
			{
				m_bFlingTop = false
				m_iFlingDelta = 0
			}

			// If we're flinging the word to match that of the best word, use its placement.
			else if (m_bFlingTop)
			{
				// Break from best word unless they fling the letters in order.
				if (iRackTile != m_iFlingLast + m_iFlingDelta)
				{
					m_bFlingTop = false
					m_iFlingDelta = 0
				}
			}

			// If we have no delta, determine this and the orientation.
			if (m_iFlingDelta == 0)
			{
				m_iFlingDelta = (iRackTile >= m_iFlingLast) ? 1 : -1
				if (placement.m_yFirst != placement.m_yLast)
				{
					m_bFlingVertical = true
				}
				else if (placement.m_xFirst != placement.m_xLast)
				{
					m_bFlingVertical = false
				}
				else
				{
					// Test the board limits.
					var i = 0
					var j = 0
					m_bFlingVertical = bRotated
					if (bRotated)
					{
						i = placement.m_yLast + m_iFlingDelta
						j = placement.m_xLast + m_iFlingDelta
					}
					else
					{
						i = placement.m_xLast + m_iFlingDelta
						j = placement.m_yLast + m_iFlingDelta
					}

					// Switch the delta or verticalization if the fling would push past the edge.
					if (i < 0 || i >= m_nBoardSize)
					{
						// If we're on the wrong corner, then switch the delta.
						if (j < 0 || j >= m_nBoardSize)
						{
							m_iFlingDelta *= -1
						}
						else
						{
							m_bFlingVertical = m_bFlingVertical ? false : true
						}
					}
				}
			}

			// Record the tile's rack location to compare with the next.
			m_iFlingLast = iRackTile

			// Determine the next square location based on our placement and delta.
			var xNext = 0
			var yNext = 0
			if (m_iFlingDelta > 0)
			{
				xNext = placement.m_xLast
				yNext = placement.m_yLast
			}
			else
			{
				xNext = placement.m_xFirst
				yNext = placement.m_yFirst
			}

			// If this is the first tile flung backward from the rack, override the default
			// horizonal orientation if we would have to jump over a played tile.
			if (!m_bFlingVertical &&
				m_board.getTilesPlaced(eState: .PLACED) == 1)
			{
				var bFound = false
				if let square = m_board.getSquare(iRow: yNext, iColumn: xNext - 1)
				{
					if (square.getTile() != nil)
					{
						bFound = true
						// If this tile does not start a top word, switch to vertical.
						let topWords = m_bestWord.getTopWords()
						if (!topWords.isStart(square: square))
						{
							m_bFlingVertical = true
							m_bFlingTop = false
						}
					}
				}
				if (!bFound)
				{
					if let square = m_board.getSquare(iRow: yNext, iColumn: xNext + 1)
					{
						if (square.getTile() != nil)
						{
							m_bFlingVertical = true
							m_bFlingTop = false
						}
					}
				}
			}

			// Attempt a vertical or horizontal word extending fling.
			while (true)
			{
				if (m_bFlingVertical)
				{
					yNext += m_iFlingDelta
				}
				else
				{
					xNext += m_iFlingDelta
				}

				// If we have an empty square, place the tile here.
				if let square = m_board.getSquare(iRow: yNext, iColumn: xNext)
				{
					if (square.getTile() == nil)
					{
						return square
					}
				}
				else
				{
					break
				}
			}
		}
		else
		{
			// This is the first tile to be placed, see if the rack matches the start
			// or end letter of the arrangement of our best word.

			// By default, we are not flinging letters in the placement of the best word.
			m_bFlingTop = false
			m_iFlingDelta = 0

			// Record the tile's rack location to compare with the next.
			m_iFlingLast = iRackTile

			// See if we have a collection of best words.
			let topWords = m_bestWord.getTopWords()

			// Get the player's rack to see if the word the user has arranged is the same
			// as one of the top words.
			let rack = getRack(iPlayer: tile.getPlayer())
			let cLetter = tile.getLetter()

			for i in 0..<topWords.m_nWords
			{
                let topWord: TopWord = topWords.getTopWord(iWord: i)!

				// Check for a forward match.
				if (isTopMatch(topWord: topWord, cLetter: cLetter, rack: rack, iRackTile: iRackTile, iDelta: 1))
				{
					m_bFlingTop = true
					return m_board.getSquare(
						iRow: topWord.m_placement.m_yFirst,
						iColumn: topWord.m_placement.m_xFirst)
				}

				// Check for a reverse match.
				if (isTopMatch(topWord: topWord, cLetter: cLetter, rack: rack, iRackTile: iRackTile, iDelta: -1))
				{
					m_bFlingTop = true
					return m_board.getSquare(
						iRow: topWord.m_placement.m_yLast,
						iColumn: topWord.m_placement.m_xLast)
				}
			}

			// Without a hint on where to fling this tile, have it drop at their finger.
			return nil
		}

		return nil
	}

	//**********************************************************************************************
	func
	getHighScore(iPlayer: Int) -> Int
	{
		if let dbGame = m_dbGame
		{
			let iScore = dbGame.getHighScore(iPlayer: iPlayer)
			if (iScore > 0)
			{
				return iScore
			}
		}
		return getScore(iPlayer: iPlayer)
	}

	//**********************************************************************************************
	func
	getInvalidWords() -> String?
	{
		do
		{
			return try m_board.getInvalidWords()
		}
		catch
		{
			return nil
		}
	}

	//**********************************************************************************************
	func
	getOpponentInvalids() -> String?
	{
		if let dbGame = m_dbGame
		{
			if (!m_bSawInvalids)
			{
				if let strInvalids = dbGame.getInvalids()
				{
					m_bSawInvalids = true
					return strInvalids
				}
			}
		}
		return nil
	}

	//**********************************************************************************************
	func
	getOpponentMessage() -> String?
	{
		if (m_strMessage == nil)
		{
			if let dbGame = m_dbGame
			{
				m_strMessage = dbGame.getOpponentMessage()
			}
		}
		let strMessage = m_strMessage
		m_strMessage = nil
		return strMessage
	}

	//**********************************************************************************************
	func
	getOpponentName() -> String?
	{
		if let dbGame = m_dbGame
		{
			return dbGame.getOpponentName()
		}
		return nil
	}

	//**********************************************************************************************
	func
	getPlayer() -> Int
	{
		return m_iPlayerTurn
	}

	//**********************************************************************************************
	func
	getPlayerCount() -> Int
	{
		return m_nPlayers
	}

	//**********************************************************************************************
	func
	getPlayerName(iPlayer: Int) -> String
	{
		if let dbGame = m_dbGame
		{
			return dbGame.getUserName(iPlayer: iPlayer)
		}
		if (isPlayerRobot(iPlayer: iPlayer))
		{
			return DbUser.GetRobotUserName()
		}
		return DbUser.GetDefaultUserName()
	}

	//**********************************************************************************************
	func
	getPlayStatus() -> PlayStatus.State
	{
		return m_board.getPlayStatus()
	}

	//**********************************************************************************************
	func
	getRack(iPlayer: Int) -> Rack
	{
		return m_aRacks[iPlayer]
	}

	//**********************************************************************************************
	func
	getRackSize() -> Int
	{
		return m_nRackSize
	}

	//**********************************************************************************************
	func
	getRanking(iPlayer: Int) -> Int
	{
		if let dbGame = m_dbGame
		{
			return dbGame.getRanking(iPlayer: iPlayer)
		}
		return 0
	}

	//**********************************************************************************************
	func
	getScore(iPlayer: Int) -> Int
	{
		return m_aPlayerScores[iPlayer]
	}

	//**********************************************************************************************
	func
	getState() -> State
	{
		if (!m_mainActivity.isInThread() && m_eState == .ROBOT_PASSED)
		{			
			// Skipping the robot turn.
			m_mainActivity.setAlert(strAlert: "Passing my turn")
			setState(eState: .DRAW_TILES)
		}
		return m_eState
	}

	//**********************************************************************************************
	func
	getTile(
		iRow: Int,
		iColumn: Int
	) -> Tile?
	{
		return m_board.getTile(iRow: iRow, iColumn: iColumn)
	}

	//**********************************************************************************************
	func
	getTileBag(iPlayer: Int) -> Tiles
	{
		return m_aTileBags[iPlayer]
	}

	//**********************************************************************************************
	func
	getTileCount(iPlayer: Int) -> Int
	{
		return m_aTileBags[iPlayer].getCount()
	}

	//**********************************************************************************************
	func
	getTilePoints(iPlayer: Int) -> Int
	{
		return m_aTileBags[iPlayer].getPoints()
	}

	//**********************************************************************************************
	func
	getTilesLeft(iPlayer: Int) -> Int
	{
		return
			getTileCount(iPlayer: iPlayer) +
			getVowelCount(iPlayer: iPlayer) +
			getRack(iPlayer: iPlayer).getTileCount()
	}

	//**********************************************************************************************
	func
	getTilesLeftAlert(iPlayer: Int) -> String
	{
		let nTiles = getTilesLeft(iPlayer)
		if (nTiles != m_nOpponentTiles)
		{
			m_nOpponentTiles = nTiles
			if (nTiles > 0 && nTiles <= getRack(iPlayer).getRackSize())
			{
				return m_dbGame!.getOpponentName() + " has " + nTiles + " tiles left."
			}
		}
		return nil
	}

	//**********************************************************************************************
	func
	getVowelBag(iPlayer: Int) -> Tiles?
	{
		return m_aVowelBags[iPlayer]
	}

	//**********************************************************************************************
	func
	getVowelCount(iPlayer: Int) -> Int
	{
		return m_aVowelBags[iPlayer].getCount()
	}

	//**********************************************************************************************
	func
	getVowelPoints(iPlayer: Int) -> Int
	{
		return m_aVowelBags[iPlayer].getPoints()
	}

	//**********************************************************************************************
	func
	getWords() -> Words
	{
		return m_words
	}

	//**********************************************************************************************
	func
	grabTile() -> Bool
	{
		return grabTile(iPlayer: m_iPlayerTurn)
	}

	//**********************************************************************************************
	func
	grabTile(iPlayer: Int) -> Bool
	{
		return grabTile(bag: m_aTileBags[iPlayer], iPlayer: iPlayer)
	}

	//**********************************************************************************************
	func
	grabTile(
		bag: Tiles,
		iPlayer: Int
	) -> Bool
	{
		// Can't grab more tiles when a tile has been placed on the board.
		if (!m_board.isTilePlacedOrPlayed(iPlayer: iPlayer))
		{
			// If we grabbed a tile, check to see if there are still more to grab.
			let rack = getRack(iPlayer: iPlayer)
			if (bag.grab(rack: rack))
			{
				// For non-robots, update our state and rack db table.
				if (!isPlayerRobot(iPlayer: iPlayer))
				{
					// If the rack is full, switch states.
					if (isRackFilled(iPlayer: iPlayer))
					{
						// Save the updated rack to the database.
						if let dbGame = m_dbGame
						{
							_ = dbGame.pushRack(iPlayer: iPlayer, strRack: rack.getEncoding())
						}

						// Switch the state only if it is our turn.
						if (iPlayer == m_iPlayerTurn)
						{
							if (m_board.isEmpty())
							{
								setState(eState: .FIRST_WORD)
							}
							else
							{
								setState(eState: .PLACE_TILES)
							}
						}
					}
				}
				return true
			}
		}
		return false
	}

	//**********************************************************************************************
	func
	grabVowel() -> Bool
	{
		return grabVowel(iPlayer: m_iPlayerTurn)
	}

	//**********************************************************************************************
	func
	grabVowel(iPlayer: Int) -> Bool
	{
		// Make sure we have a vowel tile bag to draw from.
		return grabTile(bag: m_aVowelBags[iPlayer], iPlayer: iPlayer)
	}

	//**********************************************************************************************
	func
	isBagFull(iPlayer: Int) -> Bool
	{
		var nTiles = m_lTileBags[iPlayer].size()
		if let lVowelBags = m_lVowelBags
		{
			nTiles += lVowelBags[iPlayer].size()
		}
		
		return nTiles >= m_nTilesTotal
	}

	//**********************************************************************************************
	func
	isBoardEmpty() -> Bool
	{
		return m_board.isEmpty()
	}

	//**********************************************************************************************
	func
	isEmpty() -> Bool
	{
		return m_board.isEmpty()
	}

	//**********************************************************************************************
	func
	isGameReset() -> Bool
	{
		return m_bGameReset
	}

	//**********************************************************************************************
	func
	isGameSwitched() -> Bool
	{
		return m_bGameSwitched
	}

	//**********************************************************************************************
	func
	isMyTurn() -> Bool
	{
		if let dbGame = m_dbGame
		{
			return dbGame.isMyTurn()
		}
		return m_iPlayerTurn != m_iPlayerRobot
	}

	//**********************************************************************************************
	func
	isNoPlay() -> Bool
	{
		if (m_bNoPlay)
		{
			m_bNoPlay = false
			return true
		}
		return false
	}

	//**********************************************************************************************
	func
	isQuit() -> Bool
	{
		return m_eState == .QUIT
	}

	//**********************************************************************************************
	func
	isOver() -> Bool
	{
		return m_eState == .OVER || m_eState == .QUIT
	}

	//**********************************************************************************************
	func
	isPlayerRobot() -> Bool
	{
		return isPlayerRobot(iPlayer: m_iPlayerTurn)
	}

	//**********************************************************************************************
	func
	isPlayerRobot(iPlayer: Int) -> Bool
	{
		return iPlayer == m_iPlayerRobot
	}

	//**********************************************************************************************
	func
	isRackEmpty(iPlayer: Int) -> Bool
	{
		// The rack wasn't empty if we placed a tile on the board.
		if (m_board.isTilePlacedOrPlayed(iPlayer: iPlayer))
		{
			return false
		}

		return m_aRacks[iPlayer].isEmpty()
	}

	//**********************************************************************************************
	func
	isRackFilled(iPlayer: Int) -> Bool
	{
		// While a letter is being played, it might look like our rack has room.
		if (iPlayer == m_iRackFilled)
		{
			return true
		}

		repeat
		{
			// Can't grab more tiles when a tile has been placed on the board.
			if (m_board.isTilePlacedOrPlayed(iPlayer: iPlayer))
			{
				break
			}

			// If the rack is really full, then true.
			if (m_aRacks[iPlayer].isFull())
			{
				break
			}

			// If we have no more tiles to grab, then true as well.
			if (m_aTileBags[iPlayer].getCount() != 0)
			{
				return false
			}
			if (m_aVowelBags[iPlayer].getCount() != 0)
			{
				return false
			}
		} while (false)

		m_iRackFilled = iPlayer
		return true
	}

	//**********************************************************************************************
	func
	isRobot() -> Bool
	{
		if let dbGame = m_dbGame
		{
			return dbGame.isRobot()
		}
		return true
	}

	//**********************************************************************************************
	func
	isRobotWorking() -> Bool
	{
		return m_bRobotWorking
	}

	//**********************************************************************************************
	func
	isTilePlaced() -> Bool
	{
		if (!isWorking())
		{
			for i in 0..<m_nBoardSize
			{
				for j in 0..<m_nBoardSize
				{
					if let square = m_board.getSquare(iRow: i, iColumn: j)
					{
						if let tile = square.getTile()
						{
							switch (tile.getState())
							{
								case .PLACED:
									return true
								default:
									break
							}
						}
					}
				}
			}
		}
		return false
	}

	//**********************************************************************************************
	func
	isTilesLow(iPlayer: Int) -> Bool
	{
		let rack = getRack(iPlayer: iPlayer)
		return
			getTileCount(iPlayer: iPlayer) + getVowelCount(iPlayer: iPlayer) + rack.getTileCount()
			<= rack.getRackSize()
	}

	//**********************************************************************************************
	// Return true if the arranged word on the rack matches our best word.
	func
	isTopMatch(
		topWord: TopWord,
		cLetter: UInt8,
		rack: Rack,
		iRackTile: Int,
		iDelta: Int
	) -> Bool
	{
		var iRackTile = iRackTile
		var cLetter = cLetter
		var nLength = topWord.getLength()
		if (nLength > 0)
		{
			m_iFlingDelta = iDelta

			// Determine the alignment of the top word.
			m_bFlingVertical = false
			let placement = topWord.getPlacement()
			if (placement.m_yFirst != placement.m_yLast)
			{
				m_bFlingVertical = true
			}

			// By default, assume we are doing a forward compare.
			var iWord = 0
			var x = placement.m_xFirst
			var y = placement.m_yFirst

			// For negative detals, it's a reverse compare.
			if (iDelta < 0)
			{
				iWord = nLength - 1
				x = placement.m_xLast
				y = placement.m_yLast
			}

			// Run the comparison.
			while (nLength > 0)
			{
				nLength -= 1
				// Check for a mismatch.
				if (cLetter != Tile.WILD && cLetter != topWord.charAt(iChar: iWord))
				{
					// Allow tiles on the board to be built across.
					if (getTile(iRow: y, iColumn: x) == nil)
					{
						return false
					}
				}
				else
				{
					iRackTile += iDelta
					cLetter = rack.getLetter(iTile: iRackTile)
				}

				// Jump to the next board sqaure.
				if (m_bFlingVertical)
				{
					y += iDelta
				}
				else
				{
					x += iDelta
				}
				iWord += iDelta
			}
			return true
		}
		return false
	}

	//**********************************************************************************************
	// Return true if the arranged word on the rack matches our best word.
	func
	isTopMatch(
		rack: Rack,
		yFirst: Int,
		xFirst: Int,
		yLast: Int,
		xLast: Int,
		iRack: Int,
		iDelta: Int
	) -> Bool
	{
		var xFirst = xFirst
		var yFirst = yFirst
		var iRack = iRack
		
		m_bFlingVertical = (yFirst != yLast)
		m_iFlingDelta = iDelta

		while (true)
		{
			// Move to the next tile.
			if (m_bFlingVertical)
			{
				yFirst += iDelta
			}
			else
			{
				xFirst += iDelta
			}
			iRack += iDelta

			// See if we have a tile match.
			if let tile = rack.getTile(iTile: iRack)
			{
				if let boardBest = m_boardBest
				{
					if (!tile.isEquals(tile: boardBest.getTile(iRow: yFirst, iColumn: xFirst)))
					{
						break
					}
				}
			}
			else
			{
				break
			}

			// If that was our last tile in the pattern, we have a match.
			if (xFirst == xLast && yFirst == yLast)
			{
				return true
			}
		}
		return false
	}

	//**********************************************************************************************
	func
	isWorking() -> Bool
	{
		return m_bBestWorking || m_bRobotWorking
	}

	//**********************************************************************************************
	func
	jumbleRack(iPlayer: Int)
	{
		m_aRacks[iPlayer].jumble()
	}

	//**********************************************************************************************
	func
	moveRobot() throws
	{
		m_bRobotWorking = true

		// Make sure our rack is full with available tiles.
		drawAllTiles()

		// If this is the user's first game, try to even the score.
		var nScore = m_aPlayerScores[m_iPlayerRobot ^ 1] - m_aPlayerScores[m_iPlayerRobot];
		if (nScore < 20)
		{
			nScore = 20
		}

		// If available, get the player's ranking.
		var nRanking = 0
		if let dbGame = m_dbGame
		{
			nRanking = dbGame.getRanking(iPlayer: m_iPlayerRobot ^ 1)
		}

		// Find our BestWord.
		let rack: Rack = getRack(iPlayer: m_iPlayerTurn)
		let nBestScore = try m_bestWord.getBestWord(
			iPlayer: m_iPlayerTurn, rack: rack, nRanking: nRanking, nScore: nScore)

		// If there's no move, try to trade in letters.
		if (nBestScore == 0)
		{
			// Skip the robot's turn, but avoid repeated skips.
			if (m_nLastScore <= 0)
			{
				m_nLastScore -= 1
				if (m_nLastScore <= -m_nPlayers)
				{
					m_bRobotWorking = false
					endGame(iPlayer: m_iPlayerTurn ^ 1)
					return
				}
			}

			// Record this as a zero score.
			m_nLastScore = 0

			// Switch back to the human player.
			switchPlayer()

			// Turn off race condition blocker.
			m_bRobotWorking = false

			// Set the state so an alert message can be posted on the loop thread.
			setState(State.ROBOT_PASSED);
			return
		}

		// Place our BestWord on the board.
		_ = m_bestWord.placeBestWord(iPlayer: m_iPlayerTurn, rack: m_rackBackup, board: m_board)

		// Play and score the word.
		if (playWord(iPlayer: m_iPlayerTurn ^ 1) != .VALID)
		{
			//throw Exception("Placed BestWord could not be played.")
			return
		}

		m_bRobotWorking = false
	}

	//**********************************************************************************************
	func
	playWord(iUser: Int) -> PlayStatus.State
	{
		// Try to play the word.
		var eStatus = m_board.playWord()

		repeat
		{
			// Break out if the arrangement is invalid.
			if (eStatus != .VALID)
			{
				break
			}
			
			// Check to see if only valid words can be played.
			if ((m_bmSettings & Game.s_bmChallengeMode) == 0)
			{
				if let sInvalidWords = getInvalidWords()
				{
					if (!sInvalidWords.isEmpty())
					{
						eStatus = .INVALID_WORD
						if let dbGame = m_dbGame
						{
							dbGame.setInvalids(sInvalidWords)
						}
						break
					}
				}
			}

			// Have a valid play.
			let iPlayer = m_iPlayerTurn
			_ = scoreWord(iPlayer: iUser)

			if let dbGame = m_dbGame
			{
				if (i == m_iPlayerRobot &&
					!isOver())
				{
					if let strMessage = dbGame.getUserMessage()
					{
						if (!strMessage.isEmpty)
						{
							// Build a dataset of responses.
							dbGame.setMessage(
								strMsg: "Thanks for showing this robot a little love.",
								iPlayer: iPlayer)
						}
					}
				}
			}
			
			postMove(iPlayer: iPlayer)
		} while (false)

		// If it's an invalid play, withdraw all the letters.
		if (eStatus != .VALID)
		{
			_ = withdrawAllTiles()
		}

		// Reset the fling direction.
		m_iFlingDelta = 0

		return eStatus
	}

	//**********************************************************************************************
	func
	postMove(iPlayer: Int)
	{
		if let dbGame = m_dbGame
		{
			// Check to make sure we didn't lose sync.  Don't reduce the other player's
			// score.
			if (!isRobot() &&
				!isOver() &&
				dbGame.getScore(iPlayer ^ 1) != getScore(iPlayer ^ 1))
			{
				m_mainActivity.setAlert(strAlert: "Sync failure. Please try again.")
				updateGame(false)
				return;
			}

			dbGame.setLastScore(
				iPlayer: iPlayer,
				nLastScore: m_board.getLastScore(iPlayer: iPlayer))
			dbGame.setLastSquare(
				iPlayer: iPlayer,
				iLastSquare: m_board.getLastSquare(iPlayer: iPlayer))
			m_strBoard = m_board.getEncoding()
			dbGame.setMove(strMove: m_strBoard)
			dbGame.setRack(
				iPlayer: iPlayer,
				strRack: getRack(iPlayer: iPlayer).getEncoding())
			dbGame.setScore(
				iPlayer: iPlayer,
				nScore: getScore(iPlayer: iPlayer))
			dbGame.setScore(
				iPlayer: iPlayer ^ 1,
				nScore: getScore(iPlayer: iPlayer ^ 1))
			dbGame.setPlayerTurn(iTurn: m_iPlayerTurn)
			_ = dbGame.pushData()

			dbGame.clearInvalids()
		}
	}

	//**********************************************************************************************
	func
	postReset()
	{
		if let dbGame = m_dbGame
		{
			dbGame.setLastScore(iPlayer: 0, nLastScore: 0)
			dbGame.setLastScore(iPlayer: 1, nLastScore: 0)
			dbGame.setLastSquare(iPlayer: 0, iLastSquare: 0)
			dbGame.setLastSquare(iPlayer: 1, iLastSquare: 0)
			dbGame.setMove(strMove: "")
			dbGame.setRack(iPlayer: 0, strRack: getRack(iPlayer: 0).getEncoding())
			dbGame.setRack(iPlayer: 1, strRack: getRack(iPlayer: 1).getEncoding())
			dbGame.setScore(iPlayer: 0, nScore: 0)
			dbGame.setScore(iPlayer: 1, nScore: 0)
			dbGame.setPlayerTurn(iTurn: m_iPlayerTurn)
			_ = dbGame.pushData()
		}
	}

	//**********************************************************************************************
	func
	reset()
	{
		// Clear the board, rack and score.
		clear()
		
		// Let the GUI know that a new game has been reset.
		setGameReset(bReset: true)

		if let dbGame = m_dbGame
		{
			if (dbGame.isRobot())
			{
				// Save the reset game to the database.
				postReset()
			}
			else
			{
				let strGameId = DbGame.PostGame(dbGame: dbGame)
				dbGame.delete()
				let dbUser1 = dbGame.getUser()
				let dbUser2 = dbGame.getOpponent()
				m_dbGame = DbGame(
					strGameId: strGameId, dbUser: dbUser1, dbOpponent: dbUser2, mFields: nil)
			}
		}

		// Game is reset.
		setState(eState: .DRAW_TILES)
	}

	//**********************************************************************************************
	func
	scoreWord(iUser: Int) -> Int
	{
		// Score our word and add its value to the player's score total.
		m_nLastScore = m_board.scoreWord()
		m_aPlayerScores[m_iPlayerTurn] += m_nLastScore

		// If the player is out of tiles, then mark the com.jot.game as over.
		if (getRack(iPlayer: m_iPlayerTurn).getTileCount() == 0 &&
			getTileCount(iPlayer: m_iPlayerTurn) + getVowelCount(iPlayer: m_iPlayerTurn) == 0)
		{
			endGame(iPlayer: iUser)
		}
		else
		{
			// Switch the player turn.
			switchPlayer()
		}

		// By default, we do not have a filled rack.
		m_iRackFilled = -1

		// Return the last score.
		return m_nLastScore
	}

	//**********************************************************************************************
	func
	sendMessage(strMessage: String)
	{
		if let dbGame = m_dbGame
		{
			dbGame.pushMessage(strMsg: strMessage)
		}
	}

	//**********************************************************************************************
	func
	setDbGame(dbGame: DbGame?)
	{
	    m_dbGame = dbGame

		// If we're resuming a game, see if there are tiles to put on our rack.
		if let dbGame = m_dbGame
		{
			// Let the GUI know that our game has a connection to the database.
			setGameReset(bReset: true)

			let strRack = dbGame.getRack()
			let iPlayer = dbGame.getPlayer()
			let rack = getRack(iPlayer: iPlayer)
			if (strRack.count > rack.getTileCount())
			{
				// Bag all tiles from this rack.
				for iTile in 0..<rack.getRackSize()
				{
					if let tile = rack.remove(iTile: iTile)
					{
						bagTile(tile: tile)
					}
				}

				// Grab a tile for each letter in the game rack and put it on our rack.
				for iTile in 0..<strRack.count
				{
					var cLetter: UInt8 = 0
					if let ch = strRack[String.Index(encodedOffset: iTile)].asciiValue
					{
						cLetter = ch
					}
					if let tile = drawTile(iPlayer: iPlayer, cLetter: cLetter)
					{
						_ = rack.add(tile: tile)
					}
				}
			}

			// If the game has been quit, indicate this.
			if (dbGame.getPlayerTurn() < 0)
			{
				setState(eState: .OVER)
				if (dbGame.getScoreOpponent() == 0)
				{
					setState(eState: .QUIT)
				}
			}
		}
	}

	//**********************************************************************************************
	func
	setGameEndScore(iPlayer: Int)
	{
		if let dbGame = m_dbGame
		{
			// Update the player's highest and average game score.
			let dbUser = dbGame.getUser()
			dbUser.setFinalGameScore(nScore: m_aPlayerScores[iPlayer])
		}
	}

	//**********************************************************************************************
	func
	setGameReset(bReset: Bool)
	{
		m_bGameReset = bReset
	}

	//**********************************************************************************************
	func
	setGameSwitched(bSwitched: Bool)
	{
		m_bGameSwitched = bSwitched
	}

	//**********************************************************************************************
	func
	setRackTileIndex(iRackTile: Int)
	{
		m_iFlingLast = iRackTile
		m_iFlingDelta = 0
	}

	//**********************************************************************************************
	func
	setScore(
		iPlayer: Int,
		nScore: Int)
	{
		m_aPlayerScores[iPlayer] = nScore
	}

	//**********************************************************************************************
	func
	setSettings()
	{
		m_bmSettings = 0
		if let dbGame = m_dbGame
		{
			m_bmSettings = dbGame.getSettings()
		}
	}

	//**********************************************************************************************
	func
	setState(eState: State)
	{
		m_eState = eState
		switch (eState)
		{
			case .DRAW_TILES,
				 .MOVE_ROBOT,
				 .OVER:
				m_iRackFilled = -1
				break

			case .PLACE_TILES:
				if (isPlayerRobot())
				{
					setState(eState: .MOVE_ROBOT)
					break
				}
				if (m_board.getPlayStatus() == .VALID)
				{
					m_eState = .PLAY_READY
				}
				//fallthru
			case .PLAY_READY:
				m_iRackFilled = m_iPlayerTurn
				break

			case .PLAY_TILES:
				m_iRackFilled = m_iPlayerTurn
				if (m_board.getPlayStatus() == .VALID)
				{
					m_eState = .PLAY_READY
				}
				break

			default:
				break
		}
	}

	//**********************************************************************************************
	func
	skipTurn()
	{
		_ = withdrawAllTiles()

		if (m_nLastScore == 0)
		{
			m_nLastScore -= 1
			if (m_nLastScore <= -m_nPlayers)
			{
				m_bRobotWorking = false;
				endGame(m_iPlayerTurn ^ 1)
			}
			else
			{
				switchPlayer()
			}
		}
		else
		{
			switchPlayer()
			m_nLastScore = 0;
		}

		if let dbGame = m_dbGame
		{
			dbGame.setPlayerTurn(iPlayer: m_iPlayerTurn)
			_ = dbGame.pushData()
			m_bSawInvalids = false
		}
	}

	//**********************************************************************************************
	func
	switchPlayer()
	{
		// Switch the player turn.
		m_iPlayerTurn += 1
		if (m_iPlayerTurn >= m_nPlayers)
		{
			m_iPlayerTurn = 0
		}

		// Update the state.
		if (isPlayerRobot())
		{
			setState(eState: .MOVE_ROBOT)
		}
		else if (isRackFilled(iPlayer: m_iPlayerTurn))
		{
			setState(eState: .PLACE_TILES)
		}
		else
		{
			setState(eState: .DRAW_TILES)
		}
	}

	//**********************************************************************************************
	func
	testRackedTiles() -> Bool
	{
		let bRackedTiles = m_bRackedTiles
		m_bRackedTiles = false
		return bRackedTiles
	}

	//**********************************************************************************************
	func
	toString() -> String
	{
		var strBoard = String()

		strBoard.append(m_board.toString())

		// If we have a last score, display this.
		if (m_nLastScore > 0)
		{
			strBoard.append("Last score: ")
			strBoard.append(String(m_nLastScore))
			strBoard.append("\n")
		}

		// Generate a score, rack and bag tile count for each player.
		for iPlayer in 0..<m_nPlayers
		{
			strBoard.append("Player(")
			strBoard.append(String(iPlayer + 1))
			strBoard.append(") Score(")
			strBoard.append(String(m_aPlayerScores[iPlayer]))
			strBoard.append(") Vowels(")
			strBoard.append(String(m_aVowelBags[iPlayer].getCount()))
			strBoard.append(") Consonants(")
			strBoard.append(String(m_aTileBags[iPlayer].getCount()))
			strBoard.append(") Rack(")
			strBoard.append(m_aRacks[iPlayer].toString())
			strBoard.append(")\n")
		}

		return strBoard
	}

	//**********************************************************************************************
	func
	updateGame(bAlert: Bool)
	{
		if let dbGame = m_dbGame
		{
			// Count up the number of opponent tiles so we can alert the user when the
			// count is a rack size or less.
			let iUser = dbGame.getPlayer()
			let iOpponent = iUser ^ 1
			var nTiles = 0

			// If we have an updated move in the database, set this to our board.
			let strBoard = dbGame.getMove()
			if (strBoard.count ==
				m_nBoardSize + (Tile.ENCODING_SIZE * m_nBoardSize * m_nBoardSize) &&
				m_strBoard != strBoard)
			{
				// Avoid updating again.
				m_strBoard = strBoard

				// Update our game board.
				var iBoard = 0
				for iRow in 0..<m_nBoardSize
				{
					for iColumn in 0..<m_nBoardSize
					{
						var cFlags: UInt8 = 0
						if let ch = strBoard[iBoard].asciiValue
						{
							cFlags = ch
						}
						iBoard = strBoard.index(iBoard, offsetBy: 1)

						var cLetter: UInt8 = 0
						if let ch = strBoard[iBoard].asciiValue
						{
							cLetter = ch
						}
						iBoard = strBoard.index(iBoard, offsetBy: 1)
						
						if (cLetter < Tile.ALPHA)
						{
							continue
						}

						// Get the tile's player so we can count the opponent tiles.
						let iPlayer = Tile.GetEncodedPlayer(cFlags: cFlags)
						if (iPlayer == iOpponent)
						{
							nTiles += 1
						}

						// Skip over squares that are already in sync.
						if (cLetter == m_board.getLetter(iRow: iRow, iColumn: iColumn))
						{
							continue
						}

						if (m_board.getLetter(iRow: iRow, iColumn: iColumn) != 0)
						{
							//throw new Exception("Board out of sync with database.")
							return
						}

						// Draw the tile from the player's rack and play it on the board.
						let bWildcard = Tile.IsEncodedWildcard(cFlags: cFlags)
						if let tile = drawTile(iPlayer: iPlayer, cLetter: bWildcard ? Tile.WILD : cLetter)
						{
							// If we have a wildcard, set the board letter to this.
							if (bWildcard)
							{
								tile.setLetter(cLetter: cLetter)
							}

							// Set the board tile
							m_board.setTile(iRow: iRow, iColumn: iColumn, tile: tile)
							tile.setState(eState: .CONFIRMED)
							m_board.setEmpty(bEmpty: false)
						}
						else
						{
							continue
						}
					}
					iBoard = strBoard.index(iBoard, offsetBy: 1)
				}

				// If there is a connection issue to the database which caused placed tiles to be
				// lost, then remove those tiles that are potentially disconnected from the others.
				_ = withdrawStrayTiles()
			}

			// If we have a robot game, count the tiles from its rack and bags.
			if (isRobot())
			{
				nTiles =
					m_lRacks.get(m_iPlayerRobot).getTileCount() +
					getTileCount(m_iPlayerRobot) +
					getVowelCount(m_iPlayerRobot)
				if (nTiles < m_nTilesRobot)
				{
					m_nTilesRobot = nTiles
					nTiles = m_nTilesTotal - nTiles
				}
				else
				{
					nTiles = 0
				}
			}
			else
			{
				// Get the active player.
				m_iPlayerTurn = dbGame.getPlayerTurn()
				if (m_iPlayerTurn < 0)
				{
					m_iPlayerTurn = 0
				}
			}
			
			// Update scores.
			for iPlayer in 0..<m_nPlayers
			{
				setScore(iPlayer: iPlayer, nScore: dbGame.getScore(iPlayer: iPlayer))

				// Set the score of the last played word.
				m_board.setLastScore(
					iPlayer: iPlayer,
					nLastScore: dbGame.getLastScore(iPlayer: iPlayer))
				m_board.setLastSquare(
					iPlayer: iPlayer,
					iLastSquare: dbGame.getLastSquare(iPlayer: iPlayer))
			}

			// If we are actually viewing this game, make sure to provide alerts.
			if (bAlert)
			{
				// If there are no more tiles, the game is over.
				let rack = m_aRacks[0]
				if (!isOver())
				{
					if (!m_board.isTilePlacedOrPlayed(iPlayer: -1) &&
						(rack.getTileCount() + getTileCount(iPlayer: 0) + getVowelCount(iPlayer: 0) == 0 ||
						 m_aRacks[1].getTileCount() + getTileCount(iPlayer: 1) + getVowelCount(iPlayer: 1) == 0))
					{
						setGameEndScore(iPlayer: iUser)
						setState(eState: .OVER)
					}
				}
			}
		}
	}

	//**********************************************************************************************
	func
	updateState()
	{
		if let dbGame = m_dbGame
		{
			if (dbGame.isUpdated())
			{
				updateGame(bAlert: true)
			}
		}
	}

	//**********************************************************************************************
	func
	withdrawAllTiles() -> Bool
	{
		var bFound = false
		for i in 0..<m_nBoardSize
		{
			for j in 0..<m_nBoardSize
			{
				if let square = m_board.getSquare(iRow: i, iColumn: j)
				{
					if let tile = square.getTile()
					{
						switch (tile.getState())
						{
							case .PLACED,
								 .PLAYED:
								square.setTile(tile: nil, bPlaced: false)
								_ = m_aRacks[tile.getPlayer()].add(tile: tile)
								bFound = true
								break
							default:
								break
						}
					}
				}
			}
		}
		m_bRackedTiles = true

		// Change the state.
		setState(eState: .PLACE_TILES)

		// Clear any placed score.
		m_board.clearPlacedScore()

		// Reset the fling direction.
		m_iFlingDelta = 0
		
		return bFound
	}
	
	//**********************************************************************************************
	func
	withdrawStrayTiles() -> Bool
	{
		// Mark all tiles as found which are connected to tiles from the start location.
		m_board.markConnectedTiles()

		var bFound = false
		for i in 0..<m_nBoardSize
		{
			for j in 0..<m_nBoardSize
			{
				if let square = m_board.getSquare(iRow: i, iColumn: j)
				{
					if let tile = square.getTile()
					{
						if (!tile.isFound())
						{
							square.setTile(tile: nil, bPlaced: false)
							_ = m_aRacks[tile.getPlayer()].add(tile: tile)
							bFound = true
						}
					}
				}
			}
		}
		return bFound
	}
}

//**************************************************************************************************
