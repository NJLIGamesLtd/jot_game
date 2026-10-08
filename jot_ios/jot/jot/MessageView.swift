//**************************************************************************************************
//
//  MessageView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//  View of a message from the other player.
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class MessageView: UIView
{
	var m_main: Main? = nil

	var m_etCompose: UITextField? = nil
	var m_bCompose: Bool = false
	var m_strCompose: String = ""

	var m_butReceive: UIButton? = nil
	var m_bReceive = false

    var m_x: Int = 0
    var m_y: Int = 0
	var m_nWidth: Int = 0
	var m_nHeight: Int = 0

	//**********************************************************************************************
	init(main: Main)
	{
		m_main = main
        
        let width = main.getWidth()
        let height = main.getHeight()
        m_nWidth = Int(0.9 * Double(width))
        m_nHeight = height / 4
        m_x = (width - m_nWidth) >> 1
        m_y = (height - m_nHeight) >> 1

		let rect = CGRect(x: m_x, y: m_y, width: m_nWidth, height: m_nHeight)
        super.init(frame: rect)

		// Create a edit text window for sending messages.
		m_etCompose = UITextField()
		m_etCompose!.frame = rect
		m_etCompose!.backgroundColor = UIColor(patternImage: UIImage(named: "message.png")!)
        m_etCompose!.addTarget(self, action: #selector(composeEditAction), for: .editingChanged)
        m_etCompose!.addTarget(self, action: #selector(composeTouchAction), for: .touchUpInside)

		// Create simple button for the receive message.
		m_butReceive = UIButton()
        m_butReceive!.frame = rect
		m_butReceive!.backgroundColor = UIColor(patternImage: UIImage(named: "message.png")!)
        m_butReceive!.addTarget(self, action: #selector(receiveTouchAction), for: .touchUpInside)
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
    
	//**********************************************************************************************
	@objc func
	composeEditAction(sender: UITextField!)
	{
        m_strCompose = sender.text!
        if (m_strCompose.hasSuffix("\n"))
        {
			// Perform action on key press.
			m_main!.updateState(bAnimate: false)
		}
	}
    
	//**********************************************************************************************
	@objc func
	composeTouchAction(sender: UITextField!)
	{
        m_etCompose!.text = ""
		m_etCompose!.removeFromSuperview()
		m_bCompose = false
		m_main!.updateState(bAnimate: false)
	}
    
	//**********************************************************************************************
	@objc func
	receiveTouchAction(sender: UIButton!)
	{
		m_butReceive!.removeFromSuperview()
		m_bReceive = false
		m_main!.updateState(bAnimate: false)
	}
    
	//**********************************************************************************************
	func
	getComposeMessage() -> String?
	{
		if (m_bCompose)
		{
			if (m_strCompose == "")
			{
				m_etCompose!.removeFromSuperview()
				m_bCompose = false
				return m_strCompose
			}
		}
		return nil
	}

	//**********************************************************************************************
	func
	getMessageHeight() -> Int
	{
		return m_nHeight
	}

	//**********************************************************************************************
	func
	getMessageWidth() -> Int
	{
		return m_nWidth
	}

	//**********************************************************************************************
	func
	isDone() -> Bool
	{
		return !(m_bCompose || m_bReceive)
	}

	//**********************************************************************************************
	func
	setCompose()
	{
		addSubview(m_etCompose!)
		m_strCompose = ""
		m_bCompose = true
	}

	//**********************************************************************************************
	func
	setReceiveMessage(strMessage: String)
	{
        m_butReceive!.setTitle("\n" + strMessage, for: .normal)
		addSubview(m_butReceive!)
		m_bReceive = true
	}
}

//**************************************************************************************************
