//**************************************************************************************************
//
//  Tiles.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

class Tiles
{
	var m_aTiles: [Tile] = []

	//**********************************************************************************************
	func
	add(tile: Tile) -> Bool
	{
		// Reset the tile's state.
		tile.setState(eState: .BAGGED)

		// If wild, reset its letter to wild.
		if (tile.getScore() == 0)
		{
			tile.setLetter(cLetter: Tile.WILD)
		}
		m_aTiles.append(tile)
		return true
	}

	//**********************************************************************************************
	func
	getPoints() -> Int
	{
		var nPoints = 0
		for tile in m_aTiles
		{
			nPoints += tile.getScore()
		}
		return nPoints
	}

	//**********************************************************************************************
	func
	getCount() -> Int
	{
		return m_aTiles.count
	}

	//**********************************************************************************************
	func
	getVowelCount() -> Int
	{
		var nVowels = 0
		for tile in m_aTiles
		{
			if (tile.isVowel())
			{
				nVowels += 1
			}
		}
		return nVowels
	}

	//**********************************************************************************************
	func
	grabTile(cLetter: UInt8) -> Tile?
	{
		for i in 0..<m_aTiles.count
		{
			let tile = m_aTiles[i]
			if (cLetter == tile.getLetter())
			{
				return remove(iTile: i)
			}
		}
		return nil
	}

	//**********************************************************************************************
	func
	grab(rack: Rack) -> Bool
	{
		if (m_aTiles.count > 0 && !rack.isFull())
		{
			let iTile = Int.random(in: 1...m_aTiles.count)
            return rack.add(tile: remove(iTile: iTile))
		}
		
		return false
	}

	//**********************************************************************************************
	func
	indexOf(cLetter: UInt8) -> Int
	{
		for i in 0..<m_aTiles.count
		{
			let tile = m_aTiles[i]
			if (tile.getLetter() == cLetter)
			{
				return i
			}
		}
		return -1
	}

	//**********************************************************************************************
	func
	remove(iTile: Int) -> Tile
	{
		return m_aTiles.remove(at: iTile)
	}
}

//**************************************************************************************************
