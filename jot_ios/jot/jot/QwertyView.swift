//**************************************************************************************************
//
//  QwertyView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//  View to allow user to select letter for blank tile.
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class QwertyView: UIView
{
	let s_aLetters: [UInt8] =
	[
		//"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
		0x51, 0x57, 0x45, 0x52, 0x54, 0x59, 0x55, 0x49, 0x4F, 0x50,
		//"A", "S", "D", "F", "G", "H", "J", "K", "L",
		0x41, 0x53, 0x44, 0x46, 0x47, 0x48, 0x4A, 0x4B, 0x4C,
		//"Z", "X", "C", "V", "B", "N", "M"
		0x5A, 0x58, 0x43, 0x56, 0x42, 0x4E, 0x4D
	]

	let s_aImages: [UIImage] =
	[
		UIImage(named: "jot_white_qwerty.png")!,
		UIImage(named: "jot_black_qwerty")!
	]
	
	var m_gameView: GameView? = nil
	var m_width = 0
	var m_height = 0
	
	//**********************************************************************************************
	init(
		main: Main,
		gameView: GameView)
	{
		m_gameView = gameView

		m_width = main.getWidth()
		m_height = main.getHeight() / 4
		let rect = CGRect(x: 0, y: 2 * m_height, width: m_width, height: m_height)
        super.init(frame: rect)
		self.frame = rect
	}
	
	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
    
	//**********************************************************************************************
    
    override func
    touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?)
    {
        if let touch = touches.first
        {
            let location = touch.location(in: self)
        
			let keyWidth = Int(Double(m_width) / 10.2)
			let keyHeight = Int(Double(m_height) / 3.9)

			var x = Int(location.x - (CGFloat(keyWidth) / 7.5))
			let y = Int(location.y - (CGFloat(keyHeight) * 0.75))

			// Determine which, if any, key has been touched.
			if (x >= 0 && y >= 0)
			{
				var iLetter = 0
				if (y >= keyHeight)
				{
					if (y < (keyHeight << 1))
					{
						x -= keyWidth >> 2
						iLetter += 10
					}
					else
					{
						iLetter += 19
						x -= keyWidth >> 1
					}
				}

				if (x >= (m_width >> 1))
				{
					iLetter += 5
					x -= m_width >> 1
				}
				while (x >= keyWidth)
				{
					x -= keyWidth
					iLetter += 1
				}

				m_gameView!.setBlankLetter(cLetter: s_aLetters[iLetter])
			}
		}
	}

	//**********************************************************************************************
	func
	setPlayer(iPlayer: Int)
	{
        backgroundColor = UIColor(patternImage: s_aImages[iPlayer])
	}
}

//**************************************************************************************************
