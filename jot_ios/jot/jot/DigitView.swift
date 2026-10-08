//**************************************************************************************************
//
//  DigitView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class DigitView: UIView
{
	let s_idDigits: [[UIImage]] =
	[
		[
			UIImage(named: "jot_white_0")!,
			UIImage(named: "jot_white_1")!,
			UIImage(named: "jot_white_2")!,
			UIImage(named: "jot_white_3")!,
			UIImage(named: "jot_white_4")!,
			UIImage(named: "jot_white_5")!,
			UIImage(named: "jot_white_6")!,
			UIImage(named: "jot_white_7")!,
			UIImage(named: "jot_white_8")!,
			UIImage(named: "jot_white_9")!
		],
		[
			UIImage(named: "jot_black_0")!,
			UIImage(named: "jot_black_1")!,
			UIImage(named: "jot_black_2")!,
			UIImage(named: "jot_black_3")!,
			UIImage(named: "jot_black_4")!,
			UIImage(named: "jot_black_5")!,
			UIImage(named: "jot_black_6")!,
			UIImage(named: "jot_black_7")!,
			UIImage(named: "jot_black_8")!,
			UIImage(named: "jot_black_9")!
		]
	]

	//**********************************************************************************************
	init(
		cDigit: UInt8,
		iPlayer: Int,
		nDigitHeight: Int)
	{	
        let cgRect = CGRect(x: 0, y: 0, width: nDigitHeight, height: nDigitHeight)
        super.init(frame: cgRect)

		setDigit(cDigit: cDigit, iPlayer: iPlayer)
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}

	//**********************************************************************************************
    func
	setDigit(
		cDigit: UInt8,
		iPlayer: Int)
	{
		let iDigit: Int = Int(cDigit - 0x30)
        backgroundColor = UIColor(patternImage: s_idDigits[iPlayer][iDigit])
	}
}

//**************************************************************************************************
