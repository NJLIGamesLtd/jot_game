//**************************************************************************************************
//
//  Tile.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

class Tile: Hashable
{
	enum State: Int
	{
		case BAGGED
		case RACKED
		case PLACED
		case PLAYED
		case CROSSED
		case SCORED
		case CONFIRMED
	}

	static let ENCODING_SIZE = 2

	static let WILD: UInt8				= 0x40 // "@"
	static let ALPHA: UInt8				= 0x41 // "A"
	static let WILD_MASK: UInt8			= 0x20

	static let FLAG_PLAYER: UInt8		= 0x03
	static let FLAG_PLACED: UInt8		= 0x04
	static let FLAG_PLAYED: UInt8		= 0x08
	static let FLAG_WILDCARD: UInt8		= 0x10
	static let FLAG_SCORED: UInt8		= 0x20
	static let FLAG_CONFIRMED: UInt8	= 0x40
	static let FLAG_MASK: UInt8			= 0x80

	var m_cLetter: UInt8 = 0
	var m_nScore: Int = 0
	var m_eState: State
	var m_square: Square? = nil
	var m_bFound = false

	var m_iPlayer = 0

	var hashValue: Int
	{
		return ObjectIdentifier(self).hashValue
	}

	//**********************************************************************************************
	static func
	== (lhs: Tile, rhs: Tile) -> Bool
	{
		return lhs.isEquals(tile: rhs)
	}
	
	//**********************************************************************************************
	static func
	GetEncoding(tile: Tile?) -> String
	{
		var aLabel: [Character] = [ Character(UnicodeScalar(FLAG_MASK)), " " ]
		if let tile = tile
		{
			// Generate and stores the flags defined the state of our tile.
			var flags: UInt8 = FLAG_MASK + UInt8(tile.m_iPlayer)

			// Mark if it's a wildcard.
			if (tile.m_nScore == 0)
			{
				flags |= FLAG_WILDCARD
			}

			// Mark if it's placed, scored or played.
			switch (tile.m_eState)
			{
				case .PLACED:
					flags |= FLAG_PLACED
					break

				case .PLAYED:
					flags |= FLAG_PLAYED
					break

				case .SCORED:
					flags |= FLAG_SCORED
					break

				case .CONFIRMED:
					flags |= FLAG_CONFIRMED
					break

				default:
					break
			}
			aLabel[0] = Character(UnicodeScalar(flags))
			
			// Store the tile character in the second slot.
			aLabel[1] = Character(UnicodeScalar(tile.m_cLetter))
		}
		return String(aLabel)
	}

	//**********************************************************************************************
	static func
	IsEncodedWildcard(cFlags: UInt8) -> Bool
	{
		return (cFlags & FLAG_WILDCARD) != 0
	}

	//**********************************************************************************************
	static func
	GetEncodedPlayer(cFlags: UInt8) -> Int
	{
		return Int(cFlags & FLAG_PLAYER)
	}

	//**********************************************************************************************
	init(
		cLetter: UInt8,
		nScore: UInt8,
		iPlayer: Int)
	{
		m_nScore = Int(nScore)
		m_eState = State.BAGGED
		m_square = nil
		m_iPlayer = iPlayer

		setLetter(cLetter: cLetter)
	}

	//**********************************************************************************************
	init(tile: Tile)
	{
		m_nScore = tile.m_nScore
		m_eState = tile.m_eState
		m_square = tile.m_square
		m_iPlayer = tile.m_iPlayer
		m_cLetter = tile.m_cLetter
	}

	//**********************************************************************************************
	func
	getLetter() -> UInt8
	{
		return m_cLetter
	}

	//**********************************************************************************************
	func
	getPlayer() -> Int
	{
		return m_iPlayer
	}

	//**********************************************************************************************
	func
	getScore() -> Int
	{
		return m_nScore
	}

	//**********************************************************************************************
	func
	getSquare() -> Square?
	{
		return m_square
	}

	//**********************************************************************************************
	func
	getState() -> State
	{
		return m_eState
	}

	//**********************************************************************************************
	func
	isEquals(tile: Tile?) -> Bool
	{
		if let tile = tile
		{
			if (m_nScore == tile.m_nScore)
			{
				// If we have the same score and letter, the tiles match.
				if (m_cLetter == tile.m_cLetter)
				{
					return true
				}
				// If we have a blank, then they can also match.
				if (m_nScore == 0)
				{
					return true
				}
			}
		}
		return false
	}

	//**********************************************************************************************
	func
	isFound() -> Bool
	{
		return m_bFound
	}

	//**********************************************************************************************
	func
	isStateAtLeast(eState: State) -> Bool
	{
		return m_eState.rawValue >= eState.rawValue
	}

	//**********************************************************************************************
	func
	isVowel() -> Bool
	{
		switch (m_cLetter)
		{
			case Tile.WILD,
				 0x41, //"A",
				 0x45, //"E",
				 0x49, //"I",
				 0x4F, //"O",
				 0x55: //"U":
				return true

			default:
				if (m_nScore == 0)
				{
					return true
				}
				return false
		}
	}

	//**********************************************************************************************
	func
	markFound(bFound: Bool)
	{
		m_bFound = bFound
	}

	//**********************************************************************************************
	func
	setLetter(cLetter: UInt8)
	{
		m_cLetter = cLetter
	}

	//**********************************************************************************************
	func
	setScore(nScore: UInt8)
	{
		m_nScore = Int(nScore)
	}

	//**********************************************************************************************
	func
	setSquare(
		square: Square?,
		bPlaced: Bool)
	{
		m_square = square
		if (m_square != nil &&
			bPlaced &&
			!isStateAtLeast(eState: .CROSSED))
		{
			setState(eState: .PLACED)
		}
	}

	//**********************************************************************************************
	func
	setState(eState: State)
	{
		m_eState = eState
	}

	//**********************************************************************************************
	func
	toString() -> String
	{
		var aLabel: [Character] = [ "[", " ", "]" ]
		aLabel[1] = Character(UnicodeScalar(m_cLetter))

		switch (m_eState)
		{
			case .BAGGED,
				 .RACKED:
				return " "

			case .PLACED:
				aLabel[0] = "{"
				aLabel[2] = "}"
				break

			case .PLAYED:
				aLabel[0] = "|"
				aLabel[2] = "|"
				break

			case .CROSSED,
				 .SCORED:
				aLabel[0] = "<"
				aLabel[2] = ">"
				break

			//case .CONFIRMED:
			default:
				if (m_nScore == 0)
				{
					aLabel[0] = "("
					aLabel[2] = ")"
				}
				break
		}
		return String(aLabel)
	}
}

//**************************************************************************************************
