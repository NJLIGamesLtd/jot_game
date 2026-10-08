//**************************************************************************************************
//
//  SquareView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//  The view of a square for placing a tile.
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class SquareView: UIView
{
	@IBOutlet weak var m_image: UIImageView!
	var m_square: Square? = nil
	var m_tileView: TileView? = nil

	var m_xStart = 0
	var m_yStart = 0
	var m_xEnd = 0
	var m_yEnd = 0

	//**********************************************************************************************
	init()
	{
        let cgRect = CGRect(x: 0, y: 0, width: 100, height: 100)
        super.init(frame: cgRect)
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
    
	//**********************************************************************************************
	func
	isClose(
		x: Int,
		y: Int
	) -> Bool
	{
		if (m_tileView == nil && x >= m_xStart && x <= m_xEnd)
		{
			if ((y >= m_yStart - ((m_yEnd - m_yStart) >> 1) && y <= m_yEnd) ||
				(y >= m_yStart && y <= m_yEnd + ((m_yEnd - m_yStart) >> 1)))
			{
				return true
			}
		}
		return false
	}
	
	//**********************************************************************************************
	func
	isWithin(
		x: Int,
		y: Int,
		bMatchBelow: Bool
	) -> Bool
	{
		return
			x >= m_xStart && x < m_xEnd &&
			y >= m_yStart && (bMatchBelow || y < m_yEnd)
	}
	
	//**********************************************************************************************
	// Handler when a tile is dropped onto this square.
	func
	onDrop(view: UIView)
	{
		// IF this tile is a child of another, remove it from its former parent.
		view.removeFromSuperview()

		// Remove all existing views and add this.
		subviews.forEach({ $0.removeFromSuperview() })
		addSubview(view)

		// Assign the size and location to fill our square.
		view.frame = CGRect(x: 0, y: 0, width: 100, height: 100)
	}
	
	//**********************************************************************************************
	func
	onLayout(
		bChanged: Bool,
		xStart: Int,
		yStart: Int,
		xEnd: Int,
		yEnd: Int)
	{
		// Ignore if we are not visible.
		if (xStart == xEnd)
		{
			return
		}

		// Have the rack squares a little taller.
		var yDelta = 0

		// Get the parent so we can determine our location on the screen.
		if let boardView = superview as? BoardView
		{
			m_xStart = boardView.getStartX()
			m_yStart = boardView.getStartY()
		}
		else if let rackView = superview as? RackView
		{
			m_xStart = rackView.getStartX()
			m_yStart = rackView.getStartY() - 10
			yDelta = 50
		}			

		// TODO: Does this assume xStart,yStart is zero?
		m_xEnd = m_xStart + xEnd
		m_yEnd = m_yStart + yEnd + yDelta
		m_xStart += xStart
		m_yStart += yStart

		// Assign the size and location to fill our square.
		
		frame = CGRect(x: m_xStart, y: m_yStart, width: m_xEnd - m_xStart, height: m_yEnd - m_yStart)

		// If our bonus has been removed by a temporary tile placement, add it back.
		for child in subviews
		{
			child.frame = CGRect(x: 0, y: 0, width: 100, height: 100)
		}
	}

	//**********************************************************************************************
	func
	getSquare() -> Square?
	{
		return m_square
	}

	//**********************************************************************************************
	func
	setLocation(
		xStart: Int,
		yStart: Int,
		xEnd: Int,
		yEnd: Int)
	{
		m_xStart = xStart
		m_yStart = yStart
		m_xEnd = xEnd
		m_yEnd = yEnd
		
		// Ignore if we are not visible.
		if (xStart == xEnd)
		{
			return
		}

		// Get the parent so we can determine our location on the screen.
		if let boardView = superview as? BoardView
		{
			m_xStart = boardView.getStartX()
			m_yStart = boardView.getStartY()
		}
		else if let rackView = superview as? RackView
		{
			m_xStart = rackView.getStartX()
			m_yStart = rackView.getStartY()
		}			

		// TODO: Does this assume xStart,yStart is zero?
		m_xEnd = m_xStart + xEnd
		m_yEnd = m_yStart + yEnd
		m_xStart += xStart
		m_yStart += yStart
	}

	//**********************************************************************************************
	func
	getTileView() -> TileView?
	{
		return m_tileView
	}

	//**********************************************************************************************
	func
	setSquare(square: Square)
	{
		m_square = square

		// Set the background image.
		switch (square.getBonus())
		{
			case .NA:
				m_image.image = UIImage(named: "jot_square.png")!
				break
			case .L2:
				m_image.image = UIImage(named: "jot_square_dl.png")!
				break
			case .L21:
				m_image.image = UIImage(named: "jot_square_dl_s1.png")!
				break
			case .L22:
				m_image.image = UIImage(named: "jot_square_dl_s2.png")!
				break
			case .L23:
				m_image.image = UIImage(named: "jot_square_dl_s3.png")!
				break
			case .L24:
				m_image.image = UIImage(named: "jot_square_dl_s4.png")!
				break
			case .L3,
				 .L4:
				m_image.image = UIImage(named: "jot_square_tl.png")!
				break
			case .W2:
				m_image.image = UIImage(named: "jot_square_dw.png")!
				break
			case .W3,
				 .W4:
				m_image.image = UIImage(named: "jot_square_tw.png")!
				break
			default:
				return
		}
	}

	//**********************************************************************************************
	func
	setRotation(bRotated: Bool)
	{
		// Set the background image.
		switch (m_square!.getBonus())
		{
			case .L22:
				if (bRotated)
				{
					m_image.image = UIImage(named: "jot_square_dl_s3")
				}
				else
				{
					m_image.image = UIImage(named: "jot_square_dl_s2")
				}
				break;
			case .L23:
				if (bRotated)
				{
					m_image.image = UIImage(named: "jot_square_dl_s2")
				}
				else
				{
					m_image.image = UIImage(named: "jot_square_dl_s3")
				}
				break;
			default:
				return;
		}
	}

	//**********************************************************************************************
	func
	setTileView(
		tileView: TileView?,
		bPlaced: Bool)
	{
		setTileView(tileView: tileView, bPlaced: bPlaced, bPromote: true)
	}

	//**********************************************************************************************
	func
	setTileView(
		tileView: TileView?,
		bPlaced: Bool,
		bPromote: Bool)
	{
		// Remove all existing views.
		subviews.forEach({ $0.removeFromSuperview() })

		if let square = m_square
		{
			// If we have a new view, give it a position.
			if (tileView != nil)
			{
                onDrop(view: tileView!)
				if (bPromote)
				{
                    square.setTile(tile: tileView!.getTile(), bPlaced: bPlaced)
				}
			}
			else if (bPromote)
			{
				square.setTile(tile: nil, bPlaced: bPlaced)
			}
			m_tileView = tileView
		}
	}

	//**********************************************************************************************
	func
	updateView() -> Bool
	{
		if let tileView = m_tileView
		{
			return tileView.updateView()
		}
		return false
	}
}

//**************************************************************************************************
