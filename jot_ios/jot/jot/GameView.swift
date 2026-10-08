//**************************************************************************************************
//
//  GameView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//  Provides the view of the Game object.
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class GameView: UIView
{
	enum GameViewError: Error
	{
		case ERROR(String)
	}
	
	var m_main: Main? = nil
	
	var m_game: Game? = nil

	var m_nHeight = 0

	var m_boardView: BoardView? = nil
	var m_qwertyView: QwertyView? = nil
	var m_rackView: RackView? = nil
	var m_tileView: TileView? = nil
	var m_squareView: SquareView? = nil

	var m_msgView: MessageView? = nil
	var m_bMessage = false

	var m_lTilesRange: [TileView] = []

	var m_bQwerty = false

	var m_iRackTile = 0

	var m_iPlayer = 0
	var m_nPlayers = 0

	//**********************************************************************************************
	init(
		main: Main,
		nPlayers: Int,
		nBoardWidth: Int,
		nBoardHeight: Int,
		nRackSize: Int) throws
	{
		m_main = main
		m_nPlayers = nPlayers

		// Calculate the board size based on the screen size and board layout.
		let width = main.getWidth();
		let height = main.getHeight();

		// Create the viewable board.
		try m_boardView = BoardView(
			main: main,
			nPlayers: nPlayers,
			nSquaresWidth: nBoardWidth,
			nSquaresHeight: nBoardHeight)

		let y = m_boardView!.getHeight();

		// Create the user's tile rack.
		m_rackView = RackView(main: main, nSize: nRackSize)
		m_rackView!.frame.origin = CGPoint(x: 0, y: y)

		// Need a message screen to allow user to send another a message.
		m_msgView = MessageView(main: main)
		m_bMessage = false

        let cgRect = CGRect(x: 0, y: 0, width: width, height: height)
        super.init(frame: cgRect)
		
		addSubview(m_boardView!)
		addSubview(m_rackView!)

		// Need a letter selection screen when a blank tile is played.
		m_bQwerty = false
		m_qwertyView = QwertyView(main: main, gameView: self)
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
    
	//**********************************************************************************************
	func
	blockFocus() -> Bool
	{
		return m_boardView!.blockFocus()
	}

	//**********************************************************************************************
	func
	getDisplayHeight() -> Int
	{
		return m_nHeight
	}

	//**********************************************************************************************
	func
	getTilesRange() -> [TileView]
	{
		return m_lTilesRange
	}

	//**********************************************************************************************
	func
	getTileSelect() -> TileView?
	{
		return m_tileView
	}

	//**********************************************************************************************
	func
	isBoardFocused() -> Bool
	{
		_ = m_boardView!.updateFocus(x: -1, y: -1)
		return m_boardView!.isFocused()
	}

	//**********************************************************************************************
	func
	isPlayerSetup() -> Bool
	{
		return m_boardView!.isPlayerSetup()
	}

	//**********************************************************************************************
	func
	isQwertyActive() -> Bool
	{
		return m_bQwerty
	}

	//**********************************************************************************************
	func
	isRackFilled() -> Bool
	{
		if let game = m_game
		{
			return game.isRackFilled(iPlayer: m_iPlayer)
		}
		return false
	}

	//**********************************************************************************************
	func 
	placeTile(
		tileView: TileView,
		msTouch: Int,
		x: Int,
		y: Int
	) throws -> Bool
	{
		if let game = m_game
		{
			// Check to see if we have a range of tiles.
			if (m_tileView == nil)
			{
				if (m_lTilesRange.isEmpty)
				{
					throw GameViewError.ERROR("No single or tile range selection.")
				}

				// Return the range tiles to the rack.
				rackRangeTiles()
				return false
			}
		
			if (m_tileView != tileView)
			{
				throw GameViewError.ERROR("Floating a different tile.")
			}
			m_tileView = nil

			// Remove this tile from our view control.
			tileView.removeFromSuperview()

			// By default, we are moving tiles around its rack.
			var bPlaced = false

			// By default, no updates made on the screen.
			var bUpdated = false

			// Determine where the tile is being placed.
			var squareView: SquareView? = nil
			repeat
			{
				// See if this tile is being dropped back onto the rack.
				squareView = m_rackView!.getSquareView(x: x, y: y)
				if (squareView != nil)
				{
					// Clear the last score.
					m_boardView!.clearLastScore(iPlayer: m_iPlayer)

					// See if we have a blank tile.
					if (tileView.getScore() == 0)
					{
						tileView.setLetter(cLetter: Tile.WILD)
						m_qwertyView!.removeFromSuperview()
						m_bQwerty = false
					}
					break
				}

				// Don't allow tiles to be placed on the board until the player's turn.
				if (m_iPlayer != game.getPlayer())
				{
					m_main!.setAlert(strAlert: "Sorry, it's not your turn.")
					break
				}
			
				// If there are still more tiles to draw, alert the user to do so.
				if (!isRackFilled())
				{
					m_main!.setAlert(strAlert: "Please draw more tiles.")
					break
				}
			
				// If we have a quick fling, try to build on an existing placement.
				if (msTouch < 400)
				{
					squareView = m_boardView!.getSquareView(
						square: game.getFlingSquare(
							tile: tileView.getTile(),
							iRackTile: m_iRackTile,
							bRotated: m_boardView!.isRotated()))
				}
				else
				{
					game.setRackTileIndex(iRackTile: m_iRackTile)
				}

				// Else, try to place the letter at the touch location.
				if (squareView == nil)
				{
					squareView = m_boardView!.getSquareViewByLocation(
						x: x, y: y,
						bWantEmpty: !m_boardView!.isFocused())
				}

				// If we have a board square, we will need to mark the tile as placed.
				if (squareView != nil)
				{
					bPlaced = true
				}
			} while (false)

			// If we have a tile to be placed on the board, allow focus on this tile.
			if (bPlaced)
			{
				m_boardView!.allowFocus()
			}

			// If found, drop it onto this square.
			if let squareView = squareView
			{
				if (squareView.getTileView() == nil)
				{
					squareView.setTileView(tileView: tileView, bPlaced: bPlaced)

					// If we have a board square to place our letter, see if it is blank.
					if (bPlaced)
					{
						// See if we have a blank tile.
						if (!m_bQwerty && tileView.getLetter() == Tile.WILD)
						{
							addSubview(m_qwertyView!)
							m_bQwerty = true
						}
					}

					// Ask the player to place or play a tile if their rack is full.
					if (isRackFilled())
					{
						game.setState(eState: .PLACE_TILES)
					}
					bUpdated = true
				}
				else
				{
					// Shift tiles over to fit this one.
					bUpdated = m_rackView!.shiftToFit(tileView: tileView, squareView: squareView)
				}
			}
			else
			{
				// Bad placement. Place it back onto its rack, instead.
				_ = m_rackView!.add(tileView: tileView)

				// If our rack is full, ask player to place a tile.
				if (isRackFilled())
				{
					game.setState(eState: .PLACE_TILES)
				}
			}

			m_main!.updateState(bAnimate: false)
			return bUpdated
		}
		return false
	}

	//**********************************************************************************************
	func
	placeTiles() -> Bool
	{
		// Make sure we have tiles to place.
		let nTiles = m_lTilesRange.count
		if (nTiles == 0)
		{
			return false
		}

		// Don't allow tiles to be placed on the board until the player's turn.
		if (m_iPlayer != m_game!.getPlayer())
		{
			// Return the range tiles to the rack.
			rackRangeTiles();

			m_main!.setAlert(strAlert: "Sorry, it's not your turn.")
			return false
		}
			
		// Get our x,y based on the first tile.
		var tileView = m_lTilesRange[0]
		var x: Int = tileView.getX() + tileView.getWidth() / 2
		let y: Int = tileView.getY() + tileView.getHeight()

		// Need to have our drop onto the board.
		var squareView = m_boardView!.getSquareViewByLocation(x: x, y: y, bWantEmpty: false)
		if (squareView != nil)
		{
			// Make sure the initial tile square is empty.
			if (squareView!.getTileView() != nil)
			{
				squareView = nil
			}
		}

		// If we have a blank tile, make sure we ask for the letter.
		var tileBlank: TileView? = nil

		// Place each. If any fail, they should all revert to the rack.
		var bPlaced = false
		for iTile in 0..<nTiles
		{
			tileView = m_lTilesRange[iTile]

			// Remove this tile from our view control.
			tileView.removeFromSuperview()

			// If we have square to place this, put it back on the rack.
			if (squareView == nil)
			{
				_ = m_rackView!.add(tileView: tileView)
				continue
			}

			// Remember our blank tile.
			if (tileView.getLetter() == Tile.WILD)
			{
				tileBlank = tileView
			}

			// Place the tile on the square.
			squareView!.setTileView(tileView: tileView, bPlaced: true)
			bPlaced = true

			// Find the next open square.
			let width = m_boardView!.getSquareWidth()
			while (squareView != nil)
			{
				x += width
				squareView = m_boardView!.getSquareViewByLocation(x: x, y: y, bWantEmpty: false)
				if (squareView != nil)
				{
					if (squareView!.getTileView() == nil)
					{
						break
					}
				}
			}
		}

		// Remove all tiles from our range list.
        m_lTilesRange.removeAll()

		// If we have a blank, ask for its letter.
		if (tileBlank != nil)
		{
			addSubview(m_qwertyView!)
			m_bQwerty = true
		}

		if (bPlaced)
		{
			m_game!.setState(eState: .PLAY_TILES)
		}

		m_main!.updateState(bAnimate: false)
		return true
	}

	//**********************************************************************************************
	// Return the range tiles to the rack.
	func
	rackRangeTiles()
	{
		var i = m_lTilesRange.count
		while (i > 0)
		{
			i -= 1
			let tileView = m_lTilesRange.remove(at: i)
			_ = m_rackView!.add(tileView: tileView)
		}
	}

	//**********************************************************************************************
	func
	reset()
	{
		m_boardView!.reset()
	}

	//**********************************************************************************************
	func
	setBlankLetter(cLetter: UInt8)
	{
		m_boardView!.setBlankLetter(cLetter: cLetter)
		m_qwertyView!.removeFromSuperview();
		m_bQwerty = false
		m_main!.updateState(bAnimate: false)
	}

	//**********************************************************************************************
	func
	setBoardRotation(bRotated: Bool)
	{
		m_boardView!.setBoardRotation(bRotated: bRotated)
	}

	//**********************************************************************************************
	func
	setGame(
		game: Game?,
		iPlayer: Int) throws
	{
		if let game = game
		{
			if (game !== m_game || game.isGameReset())
			{
				m_game = game
				m_iPlayer = iPlayer
				game.setGameReset(bReset: false)
				game.setGameSwitched(bSwitched: true)

				// Assign the game board to our view of it.
				m_boardView!.setPlayer(iPlayer: iPlayer)
				try m_boardView!.setBoard(board: game.getBoard())

				// Assign the user's rack to our view of it.
				m_rackView!.setRack(rack: game.getRack(iPlayer: iPlayer))

				// Set the player color of our qwerty keyboard.
				m_qwertyView!.setPlayer(iPlayer: iPlayer)

				m_boardView!.clearPlayerNames()
				for iPlayer in 0..<m_nPlayers
				{
					setPlayerName(
						iPlayer: iPlayer,
						strPlayerName: game.getPlayerName(iPlayer: iPlayer))
				}
			}
			else if (m_boardView!.isAnonymous())
			{
				for iPlayer in 0..<m_nPlayers
				{
					setPlayerName(
						iPlayer: iPlayer,
						strPlayerName: game.getPlayerName(iPlayer: iPlayer))
				}
			}
		}
	}

	//**********************************************************************************************
	func
	setPlayerName(
		iPlayer: Int,
		strPlayerName: String)
	{
		m_boardView!.setPlayerName(iPlayer: iPlayer, strPlayerName: strPlayerName, bForce: true)
	}

	//**********************************************************************************************
	func
	setTileSelect(
		tileView: TileView,
		squareView: SquareView,
		iRackTile: Int)
	{
		if (m_tileView != nil)
		{
			// Remove this tile from our view control.
			m_tileView!.removeFromSuperview()

			// Place it back onto its rack.
			_ = m_rackView!.add(tileView: m_tileView!)
		}
		m_squareView = squareView
		m_tileView = tileView
		//tileView.measure(m_rackView!.getSquareWidth(), m_rackView!.getSquareHeight())
		addSubview(tileView)

		// Remember the direction on the rack we are flinging tiles.
		if (iRackTile >= 0)
		{
			m_iRackTile = iRackTile
		}
	}

	//**********************************************************************************************
	func
	setTileSelectRange(tileView: TileView?)
	{
		// Need a primary and range tile.
		if var tileView = tileView
		{
			if (m_tileView == nil)
			{
				return
			}

			// Select the range of tiles on the rack.
			var iRackTile = m_rackView!.removeTileView(tileView: tileView)
			if (iRackTile < 0 || iRackTile == m_iRackTile)
			{
				return
			}

			// Move the range of tiles to our list.
			if (iRackTile < m_iRackTile)
			{
				// Reinsert the primary tile based on order.
				m_tileView!.removeFromSuperview()

				addSubview(tileView)
                m_lTilesRange.append(tileView)
				while (iRackTile + 1 < m_iRackTile)
				{
					iRackTile += 1
					if let tileView2 = m_rackView!.removeTileView(iSquare: iRackTile)
					{
						addSubview(tileView2)
						m_lTilesRange.append(tileView2)
					}
				}
				tileView = m_tileView!
			}
			else
			{
				m_lTilesRange.append(m_tileView!)
				while (m_iRackTile + 1 < iRackTile)
				{
					m_iRackTile += 1
					if let tileView2 = m_rackView!.removeTileView(iSquare: m_iRackTile)
					{
						addSubview(tileView2)
						m_lTilesRange.append(tileView2)
					}
				}
			}
			addSubview(tileView)
			m_lTilesRange.append(tileView)
		
			m_tileView = nil
			m_squareView = nil
		}
	}

	//**********************************************************************************************
	// Assign a label based on the tile availability.
	func
	setTileButtonLabel(button: UIButton)
	{
		if let game = m_game
		{
			if (game.isBagFull(iPlayer: m_iPlayer))
			{
				button.setTitle("", for: .normal)
			}
			else
			{
				let tiles = game.getTileBag(iPlayer: m_iPlayer)
            	button.setTitle(String(tiles.getCount()), for: .normal)
			}
		}
	}

	//**********************************************************************************************
	// Assign a label based on the tile availability.
	func
	setVowelButtonLabel(button: UIButton)
	{
		if let game = m_game
		{
			if (game.isBagFull(iPlayer: m_iPlayer))
			{
				button.setTitle("", for: .normal)
			}
			else
			{
				if let tiles = game.getVowelBag(iPlayer: m_iPlayer)
				{
					button.setTitle(String(tiles.getCount()), for: .normal)
				}
			}
		}
	}

	//**********************************************************************************************
	func
	toString() -> String
	{
		if let game = m_game
		{
			return game.toString()
		}
		return ""
	}

	//**********************************************************************************************
	func
	updateScore(iPlayer: Int) -> Bool
	{
		if let game = m_game
		{
			let nScore = game.getScore(iPlayer: iPlayer)
			let nBoardScore = m_boardView!.getScore(iPlayer: iPlayer)
			if (nBoardScore != nScore)
			{
				if (nScore == 0)
				{
					m_boardView!.eraseScore(iPlayer: iPlayer)
				}
				else if (nScore < nBoardScore)
				{
					if (game.isGameSwitched())
					{
						m_boardView!.eraseScore(iPlayer: iPlayer)
					}
					else
					{
						m_boardView!.decrementScore(iPlayer: iPlayer)
					}
				}
				else
				{
					if (game.isGameSwitched())
					{
						m_boardView!.bumpScore(iPlayer: iPlayer, nScore: nScore)
					}
					else
					{
						m_boardView!.incrementScore(iPlayer: iPlayer)
					}
				}
				m_main!.playTone()
				return true
			}
		}
		return false
	}

	//**********************************************************************************************
	func
	updateBest() -> Bool
	{
		if let boardBest = m_game!.getBoardBest()
		{
			return m_boardView!.updateView(board: boardBest, iPlayer: m_iPlayer)
		}
		return false
	}

	//**********************************************************************************************
	func
	updateFocus() -> Bool
	{
		return m_boardView!.updateFocus(x: -1, y: -1)
	}
	
	//**********************************************************************************************
	func
	updatePlayers()
	{
		if let game = m_game
		{
			m_boardView!.updatePlayers(iTurn: game.getPlayer())
		}
	}
	
	//**********************************************************************************************
	func
	updateScore() -> Bool
	{
		for iPlayer in 0..<m_nPlayers
		{
			if (updateScore(iPlayer: iPlayer))
			{
				return true
			}
		}
		return false
	}

	//**********************************************************************************************
	func
	updateView() -> Bool
	{
		if let game = m_game
		{
			// Get rid of the message window if done.
			if (m_bMessage)
			{
				if let strMessage = m_msgView!.getComposeMessage()
				{
					if (!strMessage.isEmpty)
					{
						game.sendMessage(strMessage: strMessage)
					}
				}
				if (m_msgView!.isDone())
				{
					m_msgView!.removeFromSuperview()
					m_bMessage = false
				}
			}

			// Check to see if the user wants to send a message.
			if (!m_bMessage && m_boardView!.isPlayerMessage())
			{
				m_msgView!.setCompose()
				addSubview(m_msgView!)
				m_bMessage = true
			}
		
			repeat
			{
				// If we have a stranded tile, place it back on the rack.
				if (game.testRackedTiles() && m_tileView != nil)
				{
					_ = m_rackView!.add(tileView: m_tileView!)
					m_tileView = nil
					m_squareView =  nil
				}

				do
				{
					// Update player's rack quickly.
					if (m_rackView!.updateView())
					{
						break
					}

					// If we are showing a completely different game, update board before score.
					if (!game.isGameSwitched())
					{
						// Make sure the player's score is updated before we display the opponent word.
						if (updateScore(iPlayer: m_iPlayer))
						{
							break
						}
					}

					// If the robot is working, don't update the board.
					if (game.isWorking())
					{
						break
					}

					// Update the board.
					if (m_boardView!.updateView(iPlayer: m_iPlayer))
					{
						// For a the return of a game, update the board quickly.
						if (game.isGameSwitched())
						{
							while (m_boardView!.updateView(iPlayer: m_iPlayer))
							{
							}
						}
						break
					}

					// Update the score.
					if (updateScore())
					{
						break
					}
				}
/*
				catch let error
				{
					m_main!.setAlert(strAlert: error.localizedDescription)
					return false
				}
*/
				// The new game is now visible.  Switch to animated updates.
				game.setGameSwitched(bSwitched: false)

				// Update the player names, if needed.
				updatePlayers()

				// If we have a new message, display it.
				if (!m_bMessage)
				{
					if let strInvalids = game.getOpponentInvalids()
					{
						if (!strInvalids.isEmpty)
						{
							m_msgView!.setReceiveMessage(strMessage:
								game.getOpponentName()! + " played invalid words:\n" + strInvalids);
							addSubview(m_msgView!)
							m_bMessage = true
						}
					}
				}
				if (!m_bMessage)
				{
					if let strMessage = game.getOpponentMessage()
					{
						if (!strMessage.isEmpty)
						{
							m_msgView!.setReceiveMessage(strMessage: strMessage)
							addSubview(m_msgView!)
							m_bMessage = true
						}
					}
				}
			
				// Done updating.
				return false

			} while (false)

			return true
		}
		return false
	}
}

//**************************************************************************************************
