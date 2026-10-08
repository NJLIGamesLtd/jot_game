//**************************************************************************************************
//
//  RackView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/18/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//  View of a tile rack
//
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class RackView: UIView
{
	var m_rack: Rack? = nil
	var m_size = 0
	var m_x = 0
	var m_y = 0
	var m_nWidth = 0
	var m_nHeight = 0
	var m_nSquareSize = 0

	//**********************************************************************************************
	init(
		main: Main,
		nSize: Int)
	{
		m_size = nSize

		m_nWidth = main.getWidth()
		let height = main.getHeight()
		m_nHeight = m_nWidth / nSize
		m_x = 0
		m_y = 3 * height / 4

		m_nSquareSize = m_nWidth / nSize
		
		let rect = CGRect(x: m_x, y: m_y, width: m_nWidth, height: m_nHeight)
        super.init(frame: rect)
		self.frame = rect

		for _ in 0..<m_size
		{
			addSubview(SquareView())
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
	add(tileView: TileView) -> Bool
	{
        return m_rack!.add(tile: tileView.getTile())
	}

	//**********************************************************************************************
	func
	getStartX() -> Int
	{
		return m_x
	}
	
	//**********************************************************************************************
	func
	getStartY() -> Int
	{
		return m_y
	}

	//**********************************************************************************************
	func
	getSquareWidth() -> Int
	{
		return m_nSquareSize
	}

	//**********************************************************************************************
	func
	getSquareHeight() -> Int
	{
		return m_nSquareSize
	}

	//**********************************************************************************************
	func
	getSquareView(
		x: Int,
		y: Int) -> SquareView?
	{
		for child in subviews
		{
            if let square = child as? SquareView
            {
                if (square.isWithin(x: x, y: y, bMatchBelow: true))
                {
                    return square
                }
            }
		}
		return nil
	}

	//**********************************************************************************************
	func
	removeTileView(iSquare: Int) -> TileView?
	{
        if (iSquare < subviews.count)
        {
            let child = subviews[iSquare]
            if let squareView = child as? SquareView
            {
                if let tileView = squareView.getTileView()
                {
                    squareView.setTileView(tileView: nil, bPlaced: false)
                    return tileView
                }
			}
		}
		return nil
	}

	//**********************************************************************************************
	func
	removeTileView(tileView: TileView) -> Int
	{
		for child in subviews
		{
            if let squareView = child as? SquareView
            {
                if (squareView.getTileView() == tileView)
                {
                    let iRack = m_rack!.indexOf(tile: tileView.getTile())
                    squareView.setTileView(tileView: nil, bPlaced: false)
                    return iRack
                }
			}
		}
		return -1
	}

	//**********************************************************************************************
	func
	reset()
	{
		for child in subviews
		{
            if let squareView = child as? SquareView
            {
                if let _ = squareView.getTileView()
                {
                    squareView.setTileView(tileView: nil, bPlaced: false)
                }
			}
		}
	}

	//**********************************************************************************************
	func
	setRack(rack: Rack)
	{
		m_rack = rack
		for i in 0..<m_size
		{
            let child = subviews[i]
            if let squareView = child as? SquareView
            {
                squareView.setSquare(square: rack.getSquare(iSquare: i)!)
            }
		}
	}

	//**********************************************************************************************
	// Shift tiles over to fit this one.
	func 
	shiftToFit(
		tileView: TileView,
		squareView: SquareView
	) -> Bool
	{
        do
        {
            if (try m_rack!.shiftToFit(tile: tileView.getTile(), square: squareView.getSquare()!))
            {
                while (updateView())
                {
                }
                return true
            }
		}
        catch
        {
        }
		return false
	}

	//**********************************************************************************************
	func
	updateView() -> Bool
	{
		// Don't update the display until we have an underlying rack.
		if (m_rack == nil)
		{
			return false
		}
		
		var bUpdated = false

		// Check to see if there are tiles that are not in our view.
        var i = -1
        for child in subviews
		{
            if let squareView = child as? SquareView
            {
                i += 1
                
                // Get the raw tile on the game rack.
                let tileGame = m_rack!.getTile(iTile: i)

                // Get the tile on the view rack.
                let tileView = squareView.getTileView()

                // If we have no view tile, see if we need to create one.
                if (tileView == nil)
                {
                    if (tileGame != nil)
                    {
                        // Set the tile view that contains this tile.
                        squareView.setTileView(
                            tileView: TileView.GetTileView(tile: tileGame!),
                            bPlaced: false)
                        bUpdated = true
                        continue
                    }
                }
                else if (tileGame == nil)
                {
                    squareView.setTileView(tileView: nil, bPlaced: false)
                }
                else
                {
                    let tile = tileView!.getTile()
                    if (!tile.isEquals(tile: tileGame))
                    {
                        // Set the tile view that contains this tile.
                        squareView.setTileView(
                            tileView: TileView.GetTileView(tile: tileGame!),
                            bPlaced: false)
                        bUpdated = true
                        continue
                    }
                }

                if (squareView.updateView())
                {
                    bUpdated = true
                    continue
                }

                // Just in case.
                if (tileGame != nil)
                {
                    squareView.setTileView(
                        tileView: TileView.GetTileView(tile: tileGame!),
                        bPlaced: false)
                }
			}
		}
		return bUpdated
	}
}

//**************************************************************************************************
