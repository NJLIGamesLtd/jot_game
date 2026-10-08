//**************************************************************************************************
//
//  Placement.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

class Placement
{
	var m_xFirst = 0
	var m_yFirst = 0
	var m_xLast = 0
	var m_yLast = 0

	init()
	{
		m_xFirst = -1
	}

	init(
		xFirst: Int,
		yFirst: Int,
		xLast: Int,
		yLast: Int)
	{
		m_xFirst = xFirst
		m_yFirst = yFirst
		m_xLast = xLast
		m_yLast = yLast
	}

	init(square: Square)
	{
		setPlacement(square: square)
	}

	func
	setPlacement(square: Square)
	{
		m_xFirst = square.getColumn()
		m_yFirst = square.getRow()
		m_xLast = m_xFirst
		m_yLast = m_yFirst
	}
}

//**************************************************************************************************
