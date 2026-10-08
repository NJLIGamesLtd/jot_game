//**************************************************************************************************
//
//  BoardView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//  Platform specific view of a crossword board.
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class BoardView: UIView
{
	enum BoardViewError: Error
	{
		case ERROR(String)
	}

	let s_nBasesWidth = 17
	let s_nBasesHeight = 31

	var m_main: Main? = nil

	var m_board: Board? = nil
	
	var m_aaSquareViews: [[SquareView]] = [[]]
	var m_aScoreViews: [ScoreView] = []
	var m_nScoreHeight = 0

	var m_aPlayerNames: [[String?]] = [[]]

	var m_bFocusBlock = false
	var m_bFocusForce = false
	var m_iFocusStartX = 0
	var m_iFocusStartY = 0
	var m_iFocusEndX = 0
	var m_iFocusEndY = 0
	var m_nFocusSquares = 0

	var m_xTouchLast = 0.0
	var m_yTouchLast = 0.0

	var m_xStart = 0
	var m_yStart = 0
	var m_yTop = 0

	var m_nHeight = 0
	var m_fBaseWidth = 0.0
	var m_fBaseHeight = 0.0
	var m_fSquareWidth = 0.0
	var m_fSquareHeight = 0.0

	var m_nPlayers = 0
	var m_nSquares = 0

	var m_xLast: [Int] = []
	var m_yLast: [Int] = []
	var m_aLastScores: [Int] = []
	var m_aLastSquares: [Int] = []
	var m_viewLastScore: [UILabel] = []

	var m_aPlayerViews: [UILabel] = []
	var m_bPlayerMessage = false
	var m_bPlayerSetup = false

	var m_bRotated = false

	var m_placement = Placement()
	var m_dFontMultiplier = 1.0

	var m_iPlayer = 0

	//**********************************************************************************************
	init(
		main: Main,
		nPlayers: Int,
		nSquaresWidth: Int,
		nSquaresHeight: Int) throws
	{
		// Save our parameters to members.
		m_main = main
		m_nPlayers = nPlayers
		m_nSquares = nSquaresWidth
		if (nSquaresWidth != nSquaresHeight)
		{
			throw BoardViewError.ERROR("BoardView assumes a square board layout.")
		}
		if (nSquaresWidth + 1 != s_nBasesWidth)
		{
			throw BoardViewError.ERROR("BoardView assumes a different board square count.")
		}

		// Calculate the board size based on the screen size and board layout.
		let width = main.getWidth();
		let height = main.getHeight();

		// Get the base size of each square on the board.
		m_fBaseWidth = Double(width) / Double(s_nBasesWidth)
		m_fBaseHeight = Double(height) / Double(s_nBasesHeight)

		// Unless focused on a region, use the base size for our squares.
		m_fSquareWidth = m_fBaseWidth
		m_fSquareHeight = m_fBaseHeight

		// The height of each player's name and score is twice the size of a base square.
		m_nScoreHeight = Int(2 * m_fBaseHeight)

		// Create a border around the board which is half the width/height of a square.
		let nBorderWidth = Int((m_fBaseWidth + 0.5) / 2)
		let nBorderHeight = Int((m_fBaseHeight + 0.5) / 2)

		// Update the board height to include all squares, border and score region.
		m_nHeight = Int(m_fBaseHeight * Double(1 + (2 * nPlayers) + nSquaresHeight))

        let cgRect = CGRect(x: 0, y: 0, width: width, height: height)
        super.init(frame: cgRect)

		m_placement = Placement()

		// Get the font multiplier
		m_dFontMultiplier = main.getFontMultiplier()

		// Update our x,y to determine location of our subviews.
		var x = 0.0
		var y = 0.0

		// Add the scores.
		for iPlayer in 0..<nPlayers
		{
			// Create a label for our player's name.
			let label = UILabel()
		 	m_aPlayerViews[iPlayer] = label

			// Position this on the screen.
			label.frame.origin = CGPoint(x: Int(x), y: Int(y))

			// Initialize the font color and size.
            label.textColor = UIColor.black
			label.font = UIFont(name: label.font.fontName, size: 30)

			// Add this as a subview of our board view.
			addSubview(label)

			// Create a tap function for setup or messaging this player.
			var selector: Selector
			if (iPlayer == 0)
			{
				selector = Selector(("tapPlayer1:"))
			}
			else
			{
				selector = Selector(("tapPlayer2:"))
			}
			let tap = UITapGestureRecognizer(target: self, action: selector)
        	label.addGestureRecognizer(tap)
			label.isUserInteractionEnabled = true

			// Create a score view for this player.
			let scoreView = ScoreView(nScore: -1, iPlayer: iPlayer, nScoreHeight: m_nScoreHeight)
			m_aScoreViews[iPlayer] = scoreView

			// Right justify after the player name.
			label.frame.origin = CGPoint(x: width - 4 * m_nScoreHeight, y: Int(y))

			// Add this as a subview of our board view.
			addSubview(scoreView)

			// Move position for the next player.
			y += Double(m_nScoreHeight);
		}
		y += Double(nBorderHeight)

		// Create the matrix of squares.
		for i in 0..<nSquaresHeight
		{
			x = Double(nBorderWidth)
			for j in 0..<nSquaresWidth
			{
				let squareView = SquareView()
				m_aaSquareViews[i][j] = squareView
				
				// Provide a location for this square.
				squareView.frame.origin = CGPoint(x: Int(x), y: Int(y))

				// Add this as a subview of our board view.
				addSubview(squareView)
				x += m_fBaseWidth
			}
			y += m_fBaseHeight
		}

		// Create a view to display the last score.
		for iPlayer in 0..<nPlayers
		{
			let label = UILabel()
			m_viewLastScore[iPlayer] = label
			label.textColor = UIColor.white
			label.font = UIFont(name: label.font.fontName, size: 9)
			addSubview(m_viewLastScore[iPlayer])
		}

		_ = blockFocus()
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
    
	//**********************************************************************************************
	func
	allowFocus()
	{
		m_bFocusBlock = false
		m_bFocusForce = false
	}

	//**********************************************************************************************
	func
	blockFocus() -> Bool
	{
		if (isFocused())
		{
			m_bFocusBlock = true
			m_bFocusForce = false
			m_iFocusStartX = 0
			m_iFocusStartY = 0
			m_iFocusEndX = m_nSquares
			m_iFocusEndY = m_nSquares
			m_nFocusSquares = m_nSquares
			return true
		}
		return false
	}

	//**********************************************************************************************
	func
	bumpScore(
		iPlayer: Int,
		nScore: Int)
	{
		let scoreView = m_aScoreViews[iPlayer]
		scoreView.bumpScore(nScore: nScore)
	}

	//**********************************************************************************************
	// Clear the last score displayed on the board.
	func
	clearLastScore(iPlayer: Int)
	{
		m_aLastScores[iPlayer] = 0
	}

	//**********************************************************************************************
	func
	clearPlayerNames()
	{
		for i in 0..<m_nPlayers
		{
			m_aPlayerNames[i][0] = nil
			m_aPlayerNames[i][1] = nil
		}
	}

	//**********************************************************************************************
	func
	decrementScore(iPlayer: Int)
	{
		m_aScoreViews[iPlayer].decrementScore()
	}

	//**********************************************************************************************
	func
	eraseScore(iPlayer: Int)
	{
		m_aScoreViews[iPlayer].eraseScore()
	}

	//**********************************************************************************************
	func
	getHeight() -> Int
	{
		return m_nHeight
	}

	//**********************************************************************************************
	func
	getScore(iPlayer: Int) -> Int
	{
		return m_aScoreViews[iPlayer].getScore()
	}

	//**********************************************************************************************
	func
	getSize() -> Int
	{
		return m_nSquares
	}

	//**********************************************************************************************
	func
	getStartX() -> Int
	{
		return m_xStart
	}
	
	//**********************************************************************************************
	func
	getStartY() -> Int
	{
		return m_yStart
	}
	
	//**********************************************************************************************
	func
	getTopY() -> Int
	{
		return m_yTop
	}
	
	//**********************************************************************************************
	func
	getSquareHeight() -> Int
	{
		return Int(m_fSquareHeight) - 1
	}

	//**********************************************************************************************
	func
	getSquareWidth() -> Int
	{
		return Int(m_fSquareWidth) - 1
	}

	//**********************************************************************************************
	func
	getSquareView(square: Square?) -> SquareView?
	{
		if let square = square
		{
			return getSquareViewByIndex(iRow: square.getRow(), iColumn: square.getColumn())
		}
		return nil
	}

	//**********************************************************************************************
	func
	getSquareViewByIndex(
		iRow: Int,
		iColumn: Int
	) -> SquareView
	{
		return m_aaSquareViews[iRow][iColumn]
	}

	//**********************************************************************************************
	func
	getSquareViewByLocation(
		x: Int,
		y: Int,
		bWantEmpty: Bool
	) -> SquareView?
	{
		var svFound: SquareView? = nil
		var svClose: SquareView? = nil
		var bWantEmpty = bWantEmpty
		
		for child in subviews
		{
			if let squareView = child as? SquareView
			{
				if (svFound == nil && squareView.isWithin(x: x, y: y, bMatchBelow: false))
				{
					svFound = squareView
				}
				else if (bWantEmpty && squareView.isClose(x: x, y: y))
				{
					svClose = squareView
					bWantEmpty = false
				}
			}
		}
		
		// If we're looking for an empty square, allow it to look for the closest
		// bordering square which is empty when this has a tile.
		if (svFound != nil &&
			svClose != nil &&
			svFound!.getTileView() != nil)
		{
			return svClose
		}
		return svFound
	}

	//**********************************************************************************************
	func
	incrementScore(iPlayer: Int)
	{
		m_aScoreViews[iPlayer].incrementScore()
	}

	//**********************************************************************************************
	func
	isAnonymous() -> Bool
	{
		if let strName = m_aPlayerNames[m_iPlayer ^ 1][0]
		{
			return strName.isEmpty
		}
		return true
	}

	//**********************************************************************************************
	func
	isFocused() -> Bool
	{
		return m_nFocusSquares < m_nSquares
	}

	//**********************************************************************************************
	func
	isRotated() -> Bool
	{
		return m_bRotated
	}

	//**********************************************************************************************
	func
	isPlayerMessage() -> Bool
	{
		let bMessage = m_bPlayerMessage
		m_bPlayerMessage = false
		return bMessage
	}

	//**********************************************************************************************
	func
	isPlayerSetup() -> Bool
	{
		let bSetup = m_bPlayerSetup
		m_bPlayerSetup = false
		return bSetup
	}

	//**********************************************************************************************
	/*
	func
	onTouchEvent(event: MotionEvent) -> Bool
	{
		if (isFocused())
		{
			switch (event.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					m_xTouchLast = event.getRawX()
					m_yTouchLast = event.getRawY()
					break

				case MotionEvent.ACTION_MOVE:
					let x = event.getRawX()
					let y = event.getRawY()
					var dx: Int = (m_xTouchLast - x) / m_fSquareWidth
					var dy: Int = (m_yTouchLast - y) / m_fSquareHeight
					if (dx != 0 || dy != 0)
					{
						// If the board is rotated, swap dx and dy.
						if (m_bRotated)
						{
							dx ^= dy
							dy ^= dx
							dx ^= dy
						}

						m_bFocusForce = true
						m_iFocusStartX += dx
						if (m_iFocusStartX < 0)
						{
							m_iFocusStartX = 0
						}
						m_iFocusEndX = m_iFocusStartX + m_nFocusSquares
						if (m_iFocusEndX > m_nSquares)
						{
							m_iFocusEndX = m_nSquares
							m_iFocusStartX = m_nSquares - m_nFocusSquares
						}
						m_iFocusStartY += dy
						if (m_iFocusStartY < 0)
						{
							m_iFocusStartY = 0
						}
						m_iFocusEndY = m_iFocusStartY + m_nFocusSquares
						if (m_iFocusEndY > m_nSquares)
						{
							m_iFocusEndY = m_nSquares
							m_iFocusStartY = m_nSquares - m_nFocusSquares
						}
						m_xTouchLast = x
						m_yTouchLast = y
					}
					break
			}
		}
				
		return true
	}
	*/
	
	//**********************************************************************************************
	func
	removeTileView(tileView: TileView?)
	{
		if let tileView = tileView
		{
			for child in subviews
			{
				if let squareView = child as? SquareView
				{
					if (squareView.getTileView() == tileView)
					{
						squareView.setTileView(tileView: nil, bPlaced: false)
						break
					}
				}
			}
		}
	}
	
	//**********************************************************************************************
	func
	reset()
	{
		// Erase scores.
		for iPlayer in 0..<m_nPlayers
		{
			m_aScoreViews[iPlayer].eraseScore()
			clearLastScore(iPlayer: iPlayer)
		}

		// Default back to no rotation.
		m_bRotated = false

		// By default, show the whole board.  No focus around one square.
		m_bFocusBlock = false
		m_bFocusForce = false
		_ = updateFocus(x: -1, y: -1)
	}
	
	//**********************************************************************************************
	func
	rotateBoard()
	{
		// If we have a focused region, unzoom the board first.
		if (isFocused())
		{
			_ = blockFocus()
		}
		else
		{
			m_bRotated = !m_bRotated
		}
	}

	//**********************************************************************************************
	@IBAction func
	scalePiece(_ gestureRecognizer : UIPinchGestureRecognizer)
	{
		guard gestureRecognizer.view != nil else
		{
			return
		}

		if	gestureRecognizer.state == .began ||
			gestureRecognizer.state == .changed
		{
			if (gestureRecognizer.scale < 1.0)
			{
				_ = blockFocus();
			}
			else
			{
				allowFocus();
				_ = updateFocus(x: Int(gestureRecognizer.scale), y: Int(gestureRecognizer.scale));
				gestureRecognizer.view?.transform =
					(gestureRecognizer.view?.transform
                    	.scaledBy(x: gestureRecognizer.scale, y: gestureRecognizer.scale))!
			}
  			gestureRecognizer.scale = 1.0
   		}
	}

	//**********************************************************************************************
	func
	setBlankLetter(cLetter: UInt8)
	{
		for child in subviews
		{
			if let squareView = child as? SquareView
			{
				if let tileView = squareView.getTileView()
				{
					if (tileView.getState() == .PLACED &&
						tileView.getLetter() == Tile.WILD)
					{
						tileView.setLetter(cLetter: cLetter)
						return
					}
				}
			}
		}
	}

	//**********************************************************************************************
	func
	setBoard(board: Board) throws
	{
		m_board = board

		// Make sure the board is the right size.
		if (m_nSquares != board.getSize())
		{
			throw BoardViewError.ERROR("Game board size is not in line with BoardView.")
		}

		// Reset the view of the board.
		reset()

		// Assign the board squares to our views.
		for i in 0..<m_nSquares
		{
			for j in 0..<m_nSquares
			{
                m_aaSquareViews[i][j].setSquare(square: board.getSquare(iRow: i, iColumn: j)!)
			}
		}
	}

	//**********************************************************************************************
	func
	setBoardRotation(bRotated: Bool)
	{
		m_bRotated = bRotated

		// Rotate the background image of the center square views.
		var i = (m_nSquares - 1) >> 1
		while (i <= m_nSquares >> 1)
		{
			var j = (m_nSquares - 1) >> 1
			while (j <= m_nSquares >> 1)
			{
				m_aaSquareViews[i][j].setRotation(bRotated: bRotated)
				j += 1
			}
			i += 1
		}
	}

	//**********************************************************************************************
	func
	setPlayer(iPlayer: Int)
	{
		m_iPlayer = iPlayer
	}

	//**********************************************************************************************
	func
	setPlayerName(
		iPlayer: Int,
		strPlayerName: String?,
		bForce: Bool)
	{
		if let strPlayerName = strPlayerName
		{
			if (bForce || m_aPlayerNames[iPlayer][0] == nil)
			{
				if (iPlayer == m_iPlayer)
				{
					m_aPlayerNames[iPlayer][0] = String("⚙" + strPlayerName)
					m_aPlayerNames[iPlayer][1] = String("⚙" + "⭓" + strPlayerName)
				}
				else
				{
					m_aPlayerNames[iPlayer][0] = String("✏" + strPlayerName)
					m_aPlayerNames[iPlayer][1] = String("✏" + "⭓" + strPlayerName)
				}
			}
        	m_aPlayerViews[iPlayer].text = strPlayerName
		}
	}

	//**********************************************************************************************
	func
	setPlayerTurn(iTurn: Int)
	{
        setPlayerName(iPlayer: iTurn, strPlayerName: m_aPlayerNames[iTurn][1]!, bForce: false)
        setPlayerName(iPlayer: iTurn ^ 1, strPlayerName: m_aPlayerNames[iTurn ^ 1][0]!, bForce: false)
	}

	//**********************************************************************************************
	func
	setScore(
		iPlayer: Int,
		nScore: Int)
	{
        m_aScoreViews[iPlayer].setScore(nScore: nScore)
	}

	//**********************************************************************************************
	func
	tapPlayer1() -> Bool
	{
		if (m_iPlayer == 0)
		{
			m_bPlayerSetup = true
		}
		else
		{
			m_bPlayerMessage = true
		}
        m_main!.updateState(bAnimate: false)
		return false
	}

	//**********************************************************************************************
	func
	tapPlayer2() -> Bool
	{
		if (m_iPlayer == 1)
		{
			m_bPlayerSetup = true
		}
		else
		{
			m_bPlayerMessage = true
		}
        m_main!.updateState(bAnimate: false)
		return false
	}

	//**********************************************************************************************
	func
	toString() -> String
	{
        if let board = m_board
		{
			return board.toString()
		}
		return ""
	}

	//**********************************************************************************************
	func
	updateFocus(x: Int, y: Int) -> Bool
	{
		var nFocusSquares = m_nSquares
		var placement: Placement? = nil

		// Don't zoom if blocked.
		if (!m_bFocusBlock && m_board != nil)
		{
			if (x > 0)
			{
                if let squareView = getSquareViewByLocation(x: x, y: y, bWantEmpty: false)
                {
                    m_placement.setPlacement(square: squareView.getSquare()!)
                    placement = m_placement
                    m_bFocusForce = true
                }
                else
				{
					return false
				}
			}
			else
			{
				if (m_bFocusForce)
				{
					return false
				}
				// Get board exposure of our placed tiles.
                placement = m_board!.getTilePlacement(eState: .PLACED)
			}
		}

		// If we have an exposure, see if we can zoom to show this.
		var nWidth = 0
		var nHeight = 0
		if let placement2 = placement
		{
			// Zoom in to show 5/8th of the board.
			nFocusSquares = 5 * m_nSquares / 8
			if (nFocusSquares < 8)
			{
				nFocusSquares = 8
			}

			nWidth = placement2.m_xLast - placement2.m_xFirst + 1
			nHeight = placement2.m_yLast - placement2.m_yFirst + 1
			if (nWidth >= nFocusSquares || nHeight >= nFocusSquares)
			{
				placement = nil
			}
		}

		// If we still have a placement, focus in on this.
		if let placement = placement
		{
			m_iFocusStartX = placement.m_xFirst - ((nFocusSquares - nWidth) >> 1)
			if (m_iFocusStartX < 0)
			{
				m_iFocusStartX = 0
			}
			m_iFocusStartY = placement.m_yFirst - ((nFocusSquares - nHeight) >> 1)
			if (m_iFocusStartY < 0)
			{
				m_iFocusStartY = 0
			}
		}
		else
		{
			nFocusSquares = m_nSquares
			m_iFocusStartX = 0
			m_iFocusStartY = 0
		}

		// Generate the ending square locations.
		m_iFocusEndX = m_iFocusStartX + nFocusSquares
		if (m_iFocusEndX > m_nSquares)
		{
			m_iFocusEndX = m_nSquares
			m_iFocusStartX = m_nSquares - nFocusSquares
		}
		m_iFocusEndY = m_iFocusStartY + nFocusSquares
		if (m_iFocusEndY > m_nSquares)
		{
			m_iFocusEndY = m_nSquares
			m_iFocusStartY = m_nSquares - nFocusSquares
		}

		if (m_nFocusSquares != nFocusSquares)
		{
			m_nFocusSquares = nFocusSquares
			return true
		}
		return false
	}

	//**********************************************************************************************
	func
	updateLastScore(
		board: Board,
		iPlayer: Int)
	{
		m_aLastScores[iPlayer] = 0
		
		let nLastScore = board.getLastScore(iPlayer: iPlayer)
		if (nLastScore > 0)
		{
			m_aLastScores[iPlayer] = nLastScore
			m_aLastSquares[iPlayer] = board.getLastSquare(iPlayer: iPlayer)

            m_viewLastScore[iPlayer].backgroundColor =
                UIColor(patternImage: UIImage(named: "score.png")!)
			m_viewLastScore[iPlayer].text = String(nLastScore)
		}
	}

	//**********************************************************************************************
	func
	updatePlayers(iTurn: Int)
	{
		setPlayerTurn(iTurn: iTurn)
	}

	//**********************************************************************************************
	func
	updateView(iPlayer: Int) -> Bool
	{
		if let board = m_board
		{
			return updateView(board: board, iPlayer: iPlayer)
		}
		return false
	}

	//**********************************************************************************************
	func
	updateView(
		board: Board,
		iPlayer: Int
	) -> Bool
	{
		// Update the last score for this board.
		updateLastScore(board: board, iPlayer: iPlayer)
		updateLastScore(board: board, iPlayer: iPlayer ^ 1)

		var x = 0
		var y = 0
		for child in subviews
		{
			if let squareView = child as? SquareView
			{
				let tileView: TileView? = squareView.getTileView()
				let tile: Tile? = board.getTile(iRow: x, iColumn: y)

				if let tileView = tileView
				{
					if let tile = tile
					{
						if (!tile.isEquals(tile: tileView.getTile()))
						{
							squareView.setTileView(
								tileView: TileView.GetTileView(main: m_main!, tile: tile),
								bPlaced: false,
								bPromote: false)
								return true
						}
						else if (tile.getLetter() != tileView.getLetter())
						{
							tileView.setLetter(cLetter: tile.getLetter())
							return true
						}
					}
					else
					{
						squareView.setTileView(tileView: nil, bPlaced: false, bPromote: false)
						return true
					}
				}
				else
				{
					if let tile = tile
					{
						squareView.setTileView(
							tileView: TileView.GetTileView(main: m_main!, tile: tile),
							bPlaced: false, bPromote: false)
						return true
					}
				}

				y += 1
				if (y >= m_nSquares)
				{
					y = 0
					x += 1
				}
			}
		}

		// Update the last score for this board.
		updateLastScore(board: board, iPlayer: iPlayer)

		return false
	}
}

//**************************************************************************************************
