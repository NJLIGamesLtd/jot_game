//**************************************************************************************************
//
//  SetupView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//  Screen view to allow user to control personal settings.
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class SetupView: UIView
{
	var m_tvHeader: UILabel? = nil
	var m_etNickname: UITextField? = nil

	//**********************************************************************************************
	init(main: Main)
	{
        let width = Int(0.9 * Double(main.getWidth()))
        let height = main.getHeight() / 4
        let x = (width - main.getWidth()) >> 1
        let y = (height - main.getHeight()) >> 1

		let rect = CGRect(x: m_x, y: m_y, width: m_nWidth, height: m_nHeight)
        super.init(frame: rect)

		// Set the background screen.
		backgroundColor = UIColor(patternImage: UIImage(named: "jot_login.png")!)

		// Get the font multiplier
		let dFontMultiplier = main.getFontMultiplier()

		m_tvHeader = UILabel()
		m_tvHeader!.frame = rect

		let prompt = UILabel()
		prompt.setText("Nickname:")
		prompt.frame = rect

		m_etNickname = UITextField()
		m_etNickname!.frame = rect
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
    
	//**********************************************************************************************
	func
	getUserSetup() -> [String: Any]
	{
		let mSetup: [String: Any] = [:]
		mSetup["strNickname"] = m_etNickname.getText().toString().trim()

		return mSetup
	}

	//**********************************************************************************************
	func
	setDetails(main: Main)
	{
		let strHeader = "Account: " + main.getUserEmail()
		m_tvHeader.setText(strHeader)

		if let strName = main.getUserName()
		{
			if (!strName.isEmpty() &&
				strName != "Anonymous")
		{
			m_etNickname.setText(strName)
		}
	}
}

//**************************************************************************************************
