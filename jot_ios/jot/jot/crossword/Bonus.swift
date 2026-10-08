//**************************************************************************************************
//
//  Bonus.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class Bonus
{
	enum Mode: UInt8
	{
		case NA = 0
		case L2
		case L21
		case L22
		case L23
		case L24
		case L3
		case L4
		case W2
		case W3
		case W4
	}

	let s_aColors: [UIColor] =
	[
		UIColor.white,
		UIColor.yellow,
		UIColor.yellow,
		UIColor.yellow,
		UIColor.yellow,
		UIColor.yellow,
		UIColor.systemTeal,
		UIColor.orange,
		UIColor.green,
		UIColor.blue,
		UIColor.red
	]

	let s_aLabels: [String] =
	[
		"   ",
		"L2 ",
		"L2 ",
		"L2 ",
		"L2 ",
		"L2 ",
		"L3 ",
		"L4 ",
		"W2 ",
		"W3 ",
		"W4 "
	]

	var m_eMode: Mode = .NA

	init(eMode: Mode)
	{
		m_eMode = eMode
	}

	init(iMode: UInt8)
	{
        m_eMode = Mode(rawValue: iMode) ?? .NA
	}

	func
	getColor() -> UIColor
	{
		return s_aColors[Int(m_eMode.rawValue)]
	}

	func
	getMode() -> Mode
	{
		return m_eMode
	}

	func
	toString() -> String
	{
		return s_aLabels[Int(m_eMode.rawValue)]
	}
}

//**************************************************************************************************
