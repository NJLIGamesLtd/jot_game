//**************************************************************************************************
//
//  TopWord.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

public class TopWord
{
	var m_placement: Placement
	var m_aWord: [UInt8]
	var m_nLength = 0
	var m_nScore = 0

	//**********************************************************************************************
	init(nBoardSize: Int)
	{
		m_placement = Placement()
        m_aWord = [UInt8](repeating: 0, count: nBoardSize)
	}
	
	//**********************************************************************************************
	func
	charAt(iChar: Int) -> UInt8
	{
		if (iChar < m_nLength)
		{
			return m_aWord[iChar]
		}
		return 0
	}

	//**********************************************************************************************
	func
	getLength() -> Int
	{
		return m_nLength
	}

	//**********************************************************************************************
	func
	getPlacement() -> Placement
	{
		return m_placement
	}

	//**********************************************************************************************
	func
	isEqual(aWord: [UInt8]) -> Bool
	{
		var iWord = -1
		while (true)
		{
			iWord += 1
			let cWord = aWord[iWord]
			if (cWord != aWord[iWord])
			{
				return false
			}
			if (cWord == 0)
			{
				break
			}
		}
		return true
	}

	//**********************************************************************************************
	func
	isStart(square: Square) -> Bool
	{
		let cLetter = square.getLetter()
		return
			cLetter == m_aWord[0] &&
			square.getRow() == m_placement.m_yFirst &&
			square.getColumn() == m_placement.m_xFirst - 1
	}

	//**********************************************************************************************
	func
	set(
		aWord: [UInt8],
		nScore: Int,
		xFirst: Int,
		yFirst: Int,
		xLast: Int,
		yLast: Int)
	{
		m_nLength = 0;
        while (true)
        {
            m_aWord[m_nLength] = aWord[m_nLength]
            if (m_aWord[m_nLength] == 0)
            {
                break;
            }
			m_nLength += 1
		}
		m_nScore = nScore
		m_placement.m_xFirst = xFirst
		m_placement.m_yFirst = yFirst
		m_placement.m_xLast = xLast
		m_placement.m_yLast = yLast
	}
}

//**************************************************************************************************
