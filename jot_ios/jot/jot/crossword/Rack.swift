//
//  Rack.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//

import Foundation

//**************************************************************************************************

public class Rack
{
	enum RackError: Error
	{
		case ERROR(String)
	}

	var m_aSquares: [Square] = []
	var m_size = 0

	//**********************************************************************************************
	init(nMaxTiles: Int)
	{
		m_size = nMaxTiles
		for i in 0..<m_size
		{
			m_aSquares[i] = Square(iSlot: i)
		}
	}

	//**********************************************************************************************
	init(rack: Rack)
	{
		m_size = rack.m_size
		for i in 0..<m_size
		{
			m_aSquares[i] = Square(square: rack.m_aSquares[i])
		}
	}

	//**********************************************************************************************
	func
	add(tile: Tile) -> Bool
	{
		for i in 0..<m_size
		{
			if (m_aSquares[i].getTile() == nil)
			{
				m_aSquares[i].setTile(tile: tile, bPlaced: false)
				tile.setState(eState: .RACKED)
				
				// If we're returning a blank to our rack, reset its letter to WILD.
				if (tile.getScore() == 0)
				{
					tile.setLetter(cLetter: Tile.WILD)
				}
				return true
			}
		}
		return false
	}

	//**********************************************************************************************
	func
	copyTiles(rack: Rack)
	{
		for i in 0..<m_size
		{
			var tile: Tile?  = rack.m_aSquares[i].getTile()
			if let t = tile
			{
				tile = Tile(tile: t)
			}
			m_aSquares[i].m_tile = tile
		}
	}

	//**********************************************************************************************
	func
	getEncoding() -> String
	{
		var strRack = String()
		for iTile in 0..<m_size
		{
			if let tile = getTile(iTile: iTile)
			{
                strRack.append(Character(Unicode.Scalar(tile.getLetter())))
			}
		}
		return strRack
	}

	//**********************************************************************************************
	func
	getLetter(iTile: Int) -> UInt8
	{
		if let tile = getTile(iTile: iTile)
		{
			return tile.getLetter()
		}
		return 0
	}

	//**********************************************************************************************
	func
	getPoints() -> Int
	{
		var nPoints = 0
		for iTile in 0..<m_size
		{
			if let tile = m_aSquares[iTile].getTile()
			{
				nPoints += tile.getScore()
			}
		}
		return nPoints
	}

	//**********************************************************************************************
	func
	getRackSize() -> Int
	{
		return m_size
	}

	//**********************************************************************************************
	func
	getSquare(iSquare: Int) -> Square?
	{
		if (iSquare < m_size)
		{
			return m_aSquares[iSquare]
		}
		return nil
	}

	//**********************************************************************************************
	func
	getTile(iTile: Int) -> Tile?
	{
		if (iTile >= 0 && iTile < m_size)
		{
			return m_aSquares[iTile].getTile()
		}
		return nil
	}

	//**********************************************************************************************
	func
	getTileCount() -> Int
	{
		var nTiles = 0
		for i in 0..<m_size
		{
			if (m_aSquares[i].getTile() != nil)
			{
				nTiles += 1
			}
		}

		return nTiles
	}

	//**********************************************************************************************
	func
	getVowelCount() -> Int
	{
		var nVowels = 0
		for i in 0..<m_size
		{
			if let tile = m_aSquares[i].getTile()
			{
				if (tile.isVowel())
				{
					nVowels += 1
				}
			}
		}
		return nVowels
	}

	//**********************************************************************************************
	func
	grabTile(cLetter: UInt8) -> Tile?
	{
		for i in 0..<m_size
		{
			if let tile = m_aSquares[i].getTile()
			{
				if (tile.getLetter() == cLetter)
				{
					_ = remove(tile: tile)
					return tile
				}
			}
		}
		return nil
	}

	//**********************************************************************************************
	func
	indexOf(tile: Tile) -> Int
	{
		for i in 0..<m_size
		{
			if (m_aSquares[i].getTile() === tile)
			{
				return i
			}
		}
		return -1
	}

	//**********************************************************************************************
	func
	indexOf(cLetter: UInt8) -> Int
	{
		for i in 0..<m_size
		{
			if let tile = m_aSquares[i].getTile()
			{
				if (tile.getLetter() == cLetter)
				{
					return i
				}
			}
		}
		return -1
	}

	//**********************************************************************************************
	func
	isEmpty() -> Bool
	{
		for i in 0..<m_size
		{
			if (m_aSquares[i].getTile() != nil)
			{
				return false
			}
		}
		return true
	}

	//**********************************************************************************************
	func
	isFull() -> Bool
	{
		for i in 0..<m_size
		{
			if (m_aSquares[i].getTile() == nil)
			{
				return false
			}
		}
		return true
	}

	//**********************************************************************************************
	func
	jumble()
	{
		let tiles = Tiles()
		var nTiles = 0
		for i in 0..<m_size
		{
			if let tile = m_aSquares[i].getTile()
			{
				m_aSquares[i].setTile(tile: nil, bPlaced: false)
				_ = tiles.add(tile: tile)
				nTiles += 1
			}
		}

		while (nTiles > 0)
		{
			nTiles -= 1
			m_aSquares[nTiles].setTile(
				tile: tiles.remove(iTile: Int.random(in: 0...nTiles)),
				bPlaced: false)
		}
	}

	//**********************************************************************************************
	func
	remove(iTile: Int) -> Tile?
	{
		let tile: Tile? = m_aSquares[iTile].getTile()
		m_aSquares[iTile].setTile(tile: nil, bPlaced: false)

		return tile
	}

	//**********************************************************************************************
	func
	remove(tile: Tile) -> Bool
	{
		for i in 0..<m_size
		{
			if (m_aSquares[i].getTile() === tile)
			{
				_ = remove(iTile: i)
				return true
			}
		}
		return false
	}

	//**********************************************************************************************
	// Shift tiles over to fit this one.
	func
	shiftToFit(
		tile: Tile,
		square: Square
	) throws -> Bool
	{
		// Get the location of destination square in our rack.
		var iEmpty = -1
		var iSquare = -1
		for i in 0..<m_size
		{
			if (m_aSquares[i] === square)
			{
				iSquare = i
			}
			else if (iEmpty < 0 && m_aSquares[i].getTile() == nil)
			{
				iEmpty = i
			}
		}

		// There should be an empty square to push tiles towards.
		if (iEmpty < 0)
		{
			throw RackError.ERROR("No empty square on rack.")
		}

		// If the square is on the board, just return the tile to an empty spot.
		if (iSquare < 0)
		{
			iSquare = iEmpty
		}

		// If we're returning a blank to our rack, reset its letter to WILD.
		if (tile.getScore() == 0)
		{
			tile.setLetter(cLetter: Tile.WILD)
		}

		// Push towards the empty square.
		if (iEmpty < iSquare)
		{
			repeat
			{
				let tileMove: Tile? = m_aSquares[iEmpty + 1].getTile()
				m_aSquares[iEmpty + 1].setTile(tile: nil, bPlaced: false)
				m_aSquares[iEmpty].setTile(tile: tileMove, bPlaced: false)
				iEmpty += 1
			} while (iEmpty < iSquare)
		}
		else if (iEmpty > iSquare)
		{
			repeat
			{
				let tileMove: Tile? = m_aSquares[iEmpty - 1].getTile()
				m_aSquares[iEmpty - 1].setTile(tile: nil, bPlaced: false)
				m_aSquares[iEmpty].setTile(tile: tileMove, bPlaced: false)
				iEmpty -= 1
			} while (iEmpty > iSquare)
		}

		// Now we can add the tile to this square.
		m_aSquares[iSquare].setTile(tile: tile, bPlaced: false)
		return true
	}

	//**********************************************************************************************
	func
	toString() -> String
	{
		return getEncoding()
	}
}

//**************************************************************************************************
