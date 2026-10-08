//**************************************************************************************************
//
//  LoginView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//  View screen for user login via email
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class LoginView: UIView
{
	//**********************************************************************************************
	init(main: Main)
	{
		// Calculate the board size based on the screen size and board layout.
		let width = 8 * main.getWidth() / 10;
		let height = main.getHeight() / 2;
		yStart += height >> 4;
		let x = 0
		let y = height / 16

		let prompt = UILabel()
		prompt.text =
			"Please pick a method of login\n" +
			"to play against others, or\n" +
			"press play to verse the computer.";
		prompt.font = UIFont(name: prompt.font.fontName, size: 16)

        let cgRect = CGRect(x: x, y: y, width: width, height: height)
        super.init(frame: cgRect)
		
		addSubview(prompt)
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
}

//**************************************************************************************************
