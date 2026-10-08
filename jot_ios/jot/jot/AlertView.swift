//**************************************************************************************************
//
//  AlertView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//  Text widget to alert user of game status update.
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class AlertView: UIView
{
	var m_main: Main? = nil

	var m_butAlert: UIButton? = nil
	var m_bAlert = false

	var m_nWidth: Int = 0
	var m_nHeight: Int = 0

	//**********************************************************************************************
	init(main: Main)
	{        
		m_main = main

		// Get the font multiplier
		//let dFontMultiplier = main.getFontMultiplier()

		// Base the size of the alert window on the screen size.
		m_nWidth = Int(0.9 * Double(main.getWidth()))
		m_nHeight = Int(Double(main.getHeight()) / 8)
		let x = main.getWidth() - m_nWidth
		let y = main.getHeight() - m_nHeight

        let cgRect = CGRect(x: x, y: y, width: m_nWidth, height: m_nHeight)
        super.init(frame: cgRect)
        
		// Create simple button for the receive message.
        m_butAlert = UIButton(type: .custom)
        if let butAlert = m_butAlert
        {
            butAlert.frame = cgRect
            butAlert.setImage(
				UIImage(named: "alert.png"),
            	for: UIControl.State())
            butAlert.addTarget(self, action: Selector(("onClick:")), for: .touchUpInside)
        }
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
    
    //**********************************************************************************************
    func
    onClick()
    {
		m_bAlert = false
        m_butAlert!.removeFromSuperview()
        m_main!.updateState(bAnimate: false)
    }
    
	//**********************************************************************************************
	func
	getAlertHeight() -> Int
	{
		return m_nHeight
	}

	//**********************************************************************************************
	func
	getAlertWidth() -> Int
	{
		return m_nWidth
	}

	//**********************************************************************************************
	func
	setAlert(strAlert: String)
	{
		if (!m_bAlert)
		{
			m_bAlert = true
        	m_butAlert!.removeFromSuperview()			
        	m_butAlert!.setTitle(strAlert, for: UIControl.State())
			addSubview(m_butAlert!)
		}
	}
}

//**************************************************************************************************
