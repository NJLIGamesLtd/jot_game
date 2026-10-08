//**************************************************************************************************
//
//  Square.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

class Square
{
	var m_tile: Tile? = nil
	var m_bonus: Bonus
	var m_iRow = 0
	var m_iColumn = 0

	//**********************************************************************************************
    convenience
	init(
		eBonus: Bonus.Mode,
		iRow: Int,
		iColumn: Int)
	{
		self.init(
			bonus: Bonus(eMode: eBonus),
			iRow: iRow,
			iColumn: iColumn)
	}

	//**********************************************************************************************
    convenience
	init(
		iBonus: UInt8,
		iRow: Int,
		iColumn: Int)
	{
		self.init(
			bonus: Bonus(iMode: iBonus),
			iRow: iRow,
			iColumn: iColumn)
	}

	//**********************************************************************************************
	init(
		bonus: Bonus,
		iRow: Int,
		iColumn: Int)
	{
		m_bonus = bonus
		m_iRow = iRow
		m_iColumn = iColumn
		m_tile = nil
	}

	//**********************************************************************************************
	convenience
	init(iSlot: Int)
	{
		self.init(
			eBonus: .NA,
			iRow: -1,
			iColumn: iSlot)
	}

	//**********************************************************************************************
	init(square: Square)
	{
		m_tile = square.m_tile
		m_bonus = square.m_bonus
		m_iRow = square.m_iRow
		m_iColumn = square.m_iColumn
	}

	//**********************************************************************************************
	func
	getBonus() -> Bonus.Mode
	{
		return m_bonus.getMode()
	}

	//**********************************************************************************************
	func
	getLetter() -> UInt8
	{
        return m_tile?.getLetter() ?? 0
	}

	//**********************************************************************************************
	func
	getRow() -> Int
	{
		return m_iRow
	}

	//**********************************************************************************************
	func
	getColumn() -> Int
	{
		return m_iColumn
	}

	//**********************************************************************************************
	func
	getTile() -> Tile?
	{
		return m_tile
	}

	//**********************************************************************************************
	func
	setBonus(bonus: Bonus)
	{
		m_bonus = bonus
	}

	//**********************************************************************************************
	func
	setTile(
		tile: Tile?,
		bPlaced: Bool)
	{
		// If nil, clear this square of any tile.
		if (tile == nil)
		{
			// If there was a tile here, remove it.
			m_tile?.setSquare(square: nil, bPlaced: false)
		}
		else
		{
			// Place our tile.
			tile?.setSquare(square: self, bPlaced: bPlaced)
		}
		m_tile = tile
	}

	//**********************************************************************************************
	func
	toString() -> String
	{
		if let tile = m_tile
		{
			return tile.toString()
		}
		return m_bonus.toString()
	}
}

//**************************************************************************************************
