//**************************************************************************************************
//
//  TopWords.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

class TopWords
{
	static let N_TOPWORDS = 10

	var m_aTopWords: [TopWord] = []
	var m_nWords = 0

	//**********************************************************************************************
	init(nBoardSize: Int)
	{
        for i in 0..<TopWords.N_TOPWORDS
		{
            m_aTopWords[i] = TopWord(nBoardSize: nBoardSize)
		}
	}

	//**********************************************************************************************
	func
	getTopWord(iWord: Int) -> TopWord?
	{
		if (iWord < m_nWords)
		{
			return m_aTopWords[iWord]
		}
		return nil
	}

	//**********************************************************************************************
	func
	isStart(square: Square) -> Bool
	{
		for i in 0..<m_nWords
		{
            if (m_aTopWords[i].isStart(square: square))
			{
				return true
			}
		}
		return false
	}
}

//**************************************************************************************************
