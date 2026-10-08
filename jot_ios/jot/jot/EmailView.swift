//**************************************************************************************************
//
//  EmailView.swift
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

class EmailView: UIView
{
	var m_etEmail: UITextField? = nil
	var m_etPassword: UITextField? = nil
	var m_etNickname: UITextField? = nil

	//**********************************************************************************************
	init(main: Main)
	{
		// Calculate the board size based on the screen size and board layout.
		var width = main.getWidth();
		var height = main.getHeight();
		let x = width / 8
		let y = height / 8
		width = 3 * width / 4
		height = 3 * height / 4

		let email_prompt = UILabel()
		email_prompt.text = "Email address:"
		email_prompt.font = UIFont(name: email_prompt.font.fontName, size: 16)

		m_etEmail = UITextField()
        m_etEmail!.font = UIFont(name: m_etEmail!.font!.fontName, size: 16)

		let pwd_prompt = UILabel()
		pwd_prompt.text = "Jot password:"
		pwd_prompt.font = UIFont(name: pwd_prompt.font.fontName, size: 16)
		
		m_etPassword = UITextField()
		m_etPassword!.font = UIFont(name: m_etPassword!.font!.fontName, size: 16)
		
		let nick_prompt = UILabel()
		nick_prompt.text = "Nickname (for new users):"
		nick_prompt.font = UIFont(name: nick_prompt.font.fontName, size: 16)
		
		m_etNickname = UITextField()
		m_etNickName!.font = UIFont(name: m_etNickname!.font!.fontName, size: 16)
		
        let cgRect = CGRect(x: x, y: y, width: width, height: height)
        super.init(frame: cgRect)
		
		// Set the background screen.
		backgroundColor = UIColor(patternImage: UIImage(named: "jot_login.png")!)

		addSubview(email_prompt)
		addSubview(m_etEmail!)
		addSubview(pwd_prompt)
		addSubview(m_etPassword!)
		addSubview(nick_prompt)
		addSubview(m_etNickname!)
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
    
	//**********************************************************************************************
	func
	getUserLogin() -> [String: Any]
	{
		var mLogin: [String: Any] = [:]
        mLogin["strEmail"] = m_etEmail!.text.lowercaseString
		mLogin["strPassword"] = m_etPassword!.text
		mLogin["strNickname"] = m_etNickname!.text

		return mLogin
	}
}

//**************************************************************************************************
