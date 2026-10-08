//**************************************************************************************************
//
//  ScoreView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//  
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class ScoreView: UIView
{
	var m_nScore = 0
	var m_iPlayer = 0
	var m_nScoreHeight = 0
	var m_nDigitWidth = 0

	//**********************************************************************************************
	init(
		nScore: Int,
		iPlayer: Int,
		nScoreHeight: Int)
	{
		m_iPlayer = iPlayer
		m_nScoreHeight = nScoreHeight
		m_nDigitWidth = nScoreHeight

		let cgRect = CGRect(x: 0, y: 0, width: 4 * m_nDigitWidth, height: nScoreHeight)
        super.init(frame: cgRect)

		// Force the displayed score to be updated when we set it.
		m_nScore = -1
        self.setScore(nScore: nScore)
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
    
	//**********************************************************************************************
	func
	bumpScore(nScore: Int)
	{
		var nDigit = 1000
		while (nDigit > 0)
		{
			if (nScore >= m_nScore + nDigit)
			{
				m_nScore += nDigit
				break
			}
 			nDigit /= 10
 		}
		updateScore()
	}

	//**********************************************************************************************
	func
	getScore() -> Int
	{
		return m_nScore
	}

	//**********************************************************************************************
	func
	decrementScore()
	{
		// Decrement the score.
		m_nScore -= 1
		updateScore()
	}

	//**********************************************************************************************
	func
	eraseScore()
	{
		// Erase the score.
        subviews.forEach({ $0.removeFromSuperview() })
		m_nScore = -1
        setScore(nScore: 0)
	}

	//**********************************************************************************************
	func
	incrementScore()
	{
		// Increment the score.
		m_nScore += 1
		updateScore()
	}

	//**********************************************************************************************
	func
	setScore(nScore: Int)
	{
		if (m_nScore != nScore)
		{
			m_nScore = nScore
			updateScore()
		}
	}

	//**********************************************************************************************
	func
	updateScore()
	{
		// Don't display the initial pre-game score of -1.
		if (m_nScore < 0)
		{
			return
		}

		// Get the score digits.
		let strDigits = String(m_nScore)
		let nDigits = strDigits.count
		var x = 1000

		// Update the digit views to include each within the score.
		var i = 0
		var nChildren = subviews.count
		while (i < nChildren || i < nDigits)
		{
			if (i < nChildren)
			{
                let child = subviews[i]
                if let digitView = child as? DigitView
                {
                    if (i < nDigits)
                    {
                        let cDigit = strDigits[String.Index(encodedOffset: i)].asciiValue
                        digitView.setDigit(cDigit: cDigit!, iPlayer: m_iPlayer)
                        i += 1
                    }
                    else
                    {
                        digitView.removeFromSuperview()
                        nChildren -= 1
                    }
                }
            }
            else
            {
                let cDigit = strDigits[String.Index(encodedOffset: i)].asciiValue
				let digitView = DigitView(
                    cDigit: cDigit!, iPlayer: m_iPlayer, nDigitHeight: m_nScoreHeight)
				x -= m_nDigitWidth
				digitView.frame = CGRect(x: x, y: 0, width: m_nDigitWidth, height: m_nScoreHeight)
                addSubview(digitView)
				i += 1
			}
		}
	}
}

//**************************************************************************************************
