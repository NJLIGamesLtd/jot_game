//**************************************************************************************************
//
//  PlayStatus.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

class PlayStatus
{
	enum State: Int
	{
		 case VALID
		 case NO_PLACEMENT
		 case NOT_ALIGNED
		 case NOT_START
		 case ISOLATED
		 case SINGLE_LETTER
		 case INVALID_WORD
	}
	
	static let s_aLabels: [String] =
	[
		"Valid placement",
		"Place tiles first",
		"Tiles must be aligned",
		"Must cross a center square",
		"Must cross a played word",
		"Words must be two or more letters",
		"Not all words valid"
	]

	var m_eState: State = .VALID

	init(eState: State)
	{
		m_eState = eState
	}

	func
	isValid() -> Bool
	{
		return m_eState == .VALID
	}

	func
	setState(eState: State)
	{
		m_eState = eState
	}

	func
	toString() -> String
	{
		return PlayStatus.s_aLabels[m_eState.rawValue]
	}

	static func
	ToString(eState: State) -> String
	{
		return PlayStatus.s_aLabels[eState.rawValue]
	}
}

//**************************************************************************************************
