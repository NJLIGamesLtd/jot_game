//**************************************************************************************************
//
//  TileView.swift
//  jot
//
//  Created by Stephen Kennedy on 4/7/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
// A letter Tile to play on a crossword Board.
//**************************************************************************************************

import Foundation
import UIKit

//**************************************************************************************************

class TileView: UIImageView, UIDropInteractionDelegate, UIDragInteractionDelegate
{
	static var s_mTileViews: [Tile: TileView?] = [:]
	static var s_msTouch = CFAbsoluteTimeGetCurrent()
	static var s_nYDelta = 0

	static func
	GetTileView(
		main: Main,
		tile: Tile
	) -> TileView
	{
		// If needed, get the font Y delta to raise a tile on top of a finger touch.
		if (s_nYDelta == 0)
		{
			let nTileHeight = 3 * main.getHeight() / 31;
            s_nYDelta = Int(main.getFontMultiplier() * Float(nTileHeight))
			if (s_nYDelta > nTileHeight)
			{
				s_nYDelta = nTileHeight
			}
		}

		if let tileView = s_mTileViews[tile]
		{
            return tileView!
		}

		let tileView = TileView(main: main, tile: tile)
		s_mTileViews[tile] = tileView
		return tileView
	}

	let s_idTiles: [[UIImage]] =
	[
		[
			UIImage(named: "jot_white_wild.png")!,
			UIImage(named: "jot_white_a.png")!,
			UIImage(named: "jot_white_b.png")!,
			UIImage(named: "jot_white_c.png")!,
			UIImage(named: "jot_white_d.png")!,
			UIImage(named: "jot_white_e.png")!,
			UIImage(named: "jot_white_f.png")!,
			UIImage(named: "jot_white_g.png")!,
			UIImage(named: "jot_white_h.png")!,
			UIImage(named: "jot_white_i.png")!,
			UIImage(named: "jot_white_j.png")!,
			UIImage(named: "jot_white_k.png")!,
			UIImage(named: "jot_white_l.png")!,
			UIImage(named: "jot_white_m.png")!,
			UIImage(named: "jot_white_n.png")!,
			UIImage(named: "jot_white_o.png")!,
			UIImage(named: "jot_white_p.png")!,
			UIImage(named: "jot_white_q.png")!,
			UIImage(named: "jot_white_r.png")!,
			UIImage(named: "jot_white_s.png")!,
			UIImage(named: "jot_white_t.png")!,
			UIImage(named: "jot_white_u.png")!,
			UIImage(named: "jot_white_v.png")!,
			UIImage(named: "jot_white_w.png")!,
			UIImage(named: "jot_white_x.png")!,
			UIImage(named: "jot_white_y.png")!,
			UIImage(named: "jot_white_z.png")!
		],
		[
			UIImage(named: "jot_black_wild.png")!,
			UIImage(named: "jot_black_a.png")!,
			UIImage(named: "jot_black_b.png")!,
			UIImage(named: "jot_black_c.png")!,
			UIImage(named: "jot_black_d.png")!,
			UIImage(named: "jot_black_e.png")!,
			UIImage(named: "jot_black_f.png")!,
			UIImage(named: "jot_black_g.png")!,
			UIImage(named: "jot_black_h.png")!,
			UIImage(named: "jot_black_i.png")!,
			UIImage(named: "jot_black_j.png")!,
			UIImage(named: "jot_black_k.png")!,
			UIImage(named: "jot_black_l.png")!,
			UIImage(named: "jot_black_m.png")!,
			UIImage(named: "jot_black_n.png")!,
			UIImage(named: "jot_black_o.png")!,
			UIImage(named: "jot_black_p.png")!,
			UIImage(named: "jot_black_q.png")!,
			UIImage(named: "jot_black_r.png")!,
			UIImage(named: "jot_black_s.png")!,
			UIImage(named: "jot_black_t.png")!,
			UIImage(named: "jot_black_u.png")!,
			UIImage(named: "jot_black_v.png")!,
			UIImage(named: "jot_black_w.png")!,
			UIImage(named: "jot_black_x.png")!,
			UIImage(named: "jot_black_y.png")!,
			UIImage(named: "jot_black_z.png")!
		]
	]

	let s_idTilesWild: [[UIImage]] =
	[
		[
			UIImage(named: "jot_white_wild.png")!,
			UIImage(named: "jot_white_wild_a.png")!,
			UIImage(named: "jot_white_wild_b.png")!,
			UIImage(named: "jot_white_wild_c.png")!,
			UIImage(named: "jot_white_wild_d.png")!,
			UIImage(named: "jot_white_wild_e.png")!,
			UIImage(named: "jot_white_wild_f.png")!,
			UIImage(named: "jot_white_wild_g.png")!,
			UIImage(named: "jot_white_wild_h.png")!,
			UIImage(named: "jot_white_wild_i.png")!,
			UIImage(named: "jot_white_wild_j.png")!,
			UIImage(named: "jot_white_wild_k.png")!,
			UIImage(named: "jot_white_wild_l.png")!,
			UIImage(named: "jot_white_wild_m.png")!,
			UIImage(named: "jot_white_wild_n.png")!,
			UIImage(named: "jot_white_wild_o.png")!,
			UIImage(named: "jot_white_wild_p.png")!,
			UIImage(named: "jot_white_wild_q.png")!,
			UIImage(named: "jot_white_wild_r.png")!,
			UIImage(named: "jot_white_wild_s.png")!,
			UIImage(named: "jot_white_wild_t.png")!,
			UIImage(named: "jot_white_wild_u.png")!,
			UIImage(named: "jot_white_wild_v.png")!,
			UIImage(named: "jot_white_wild_w.png")!,
			UIImage(named: "jot_white_wild_x.png")!,
			UIImage(named: "jot_white_wild_y.png")!,
			UIImage(named: "jot_white_wild_z.png")!
		],
		[
			UIImage(named: "jot_black_wild.png")!,
			UIImage(named: "jot_black_wild_a.png")!,
			UIImage(named: "jot_black_wild_b.png")!,
			UIImage(named: "jot_black_wild_c.png")!,
			UIImage(named: "jot_black_wild_d.png")!,
			UIImage(named: "jot_black_wild_e.png")!,
			UIImage(named: "jot_black_wild_f.png")!,
			UIImage(named: "jot_black_wild_g.png")!,
			UIImage(named: "jot_black_wild_h.png")!,
			UIImage(named: "jot_black_wild_i.png")!,
			UIImage(named: "jot_black_wild_j.png")!,
			UIImage(named: "jot_black_wild_k.png")!,
			UIImage(named: "jot_black_wild_l.png")!,
			UIImage(named: "jot_black_wild_m.png")!,
			UIImage(named: "jot_black_wild_n.png")!,
			UIImage(named: "jot_black_wild_o.png")!,
			UIImage(named: "jot_black_wild_p.png")!,
			UIImage(named: "jot_black_wild_q.png")!,
			UIImage(named: "jot_black_wild_r.png")!,
			UIImage(named: "jot_black_wild_s.png")!,
			UIImage(named: "jot_black_wild_t.png")!,
			UIImage(named: "jot_black_wild_u.png")!,
			UIImage(named: "jot_black_wild_v.png")!,
			UIImage(named: "jot_black_wild_w.png")!,
			UIImage(named: "jot_black_wild_x.png")!,
			UIImage(named: "jot_black_wild_y.png")!,
			UIImage(named: "jot_black_wild_z.png")!
		]
	]

	var m_tile: Tile?
    var m_x = 0
    var m_y = 0
    var m_width = 0
    var m_height = 0
    var m_location = CGPoint(x: 0, y: 0)
	
	var m_cLetter: UInt8 = 0x20

	//**********************************************************************************************
	init(tile: Tile)
	{
        super.init(image: s_idTiles[tile.getPlayer()][Int(tile.getLetter() - Tile.WILD)])
		m_tile = tile
		setLetter(cLetter: tile.getLetter())
        isUserInteractionEnabled = true
	}

	//**********************************************************************************************
	required
	init?(coder aDecoder: NSCoder)
	{
		super.init(coder: aDecoder)
	}
    
	//**********************************************************************************************
    //override func
    //touchesBegan(touches: NSSet!, withEvent event: UIEvent!)
    //{
    //    var touch: UITouch! = touches.anyObject() as UITouch
    //    m_location = touch.locationInView(self.view)
    //}

	// *********************************************************************************************
	func
	dragInteraction(
        _ interaction: UIDragInteraction,
		itemsForBeginning session: UIDragSession) -> [UIDragItem]
	{
		let touchedPoint = session.location(in: self)
		if let touchedImageView = self.hitTest(touchedPoint, with: nil) as? UIImageView
		{
			let touchedImage = touchedImageView.image
            let itemProvider = NSItemProvider(object: touchedImage!)
			let dragItem = UIDragItem(itemProvider: itemProvider)
			dragItem.localObject = touchedImageView
			return [dragItem]
		}
		return []
	}

	// *********************************************************************************************
	func
	dragInteraction(
		_ interaction: UIDragInteraction,
		willAnimationLiftWith animator: UIDragAnimating,
		session: UIDragSession)
	{
		session.items.forEach
		{
			(dragItem) in
			if let touchedImageView = dragItem.localObject as? UIView
			{
				touchedImageView.removeFromSuperview()
			}
		}
	}
	
	// *********************************************************************************************
// TODO: James Folk
//	func
//	dragInteraction(
//		_ interaction: UIDragInteraction,
//		previewForLifting item: UIDragItem,
//		session: UIDragSession) -> UITargetDragPreview?
//	{
//		return IUTargetedDragPreview(view: item.localObject as! UIView)
//	}
	
	// *********************************************************************************************
	func
	dragInteraction(
		_ interaction: UIDragInteraction,
		item: UIDragItem,
		willAnimateCancelWith animator: UIDragAnimating)
	{
		addSubview(item.localObject as! UIView)
	}

	//**********************************************************************************************
	func
	getLetter() -> UInt8
	{
		return m_cLetter
	}

	//**********************************************************************************************
	func
	getWidth() -> Int
	{
		return m_width
	}

	//**********************************************************************************************
	func
	getHeight() -> Int
	{
		return m_height
	}

	//**********************************************************************************************
	func
	getScore() -> Int
	{
        return m_tile!.getScore()
	}

	//**********************************************************************************************
	func
	getTile() -> Tile
	{
        return m_tile!
	}

	//**********************************************************************************************
	func
	getTileX() -> Int
	{
		return m_x
	}

	//**********************************************************************************************
	func
	getTileY() -> Int
	{
		return m_y - 200 // 216
	}

	//**********************************************************************************************
	func
	getTileWidth() -> Int
	{
        return m_width
	}

	//**********************************************************************************************
	func
	getTileHeight() -> Int
	{
		return m_height
	}

	//**********************************************************************************************
	func
	getX() -> Int
	{
		return m_x
	}

	//**********************************************************************************************
	func
	getY() -> Int
	{
		return m_y
	}

	//**********************************************************************************************
// TODO: James Folk
//	func
//	handleDropEvent(event: MotionEvent) throws -> Bool
//	{
//		// This tile's parent should be the game object.
//        if (superview!.isKind(of: GameView.self))
//		{
//            let gameView = superview! as! GameView
//
//            // Check to see if we have a range of tiles to drop.
//            let lTiles = gameView.getTilesRange()
//            if (!lTiles.isEmpty)
//            {
//                return gameView.placeTiles()
//            }
//
//            // Get the duration of the tile drag.
//            var msTime = Int(CFAbsoluteTimeGetCurrent() - TileView.s_msTouch)
//
//            // Place this single tile.
//            return try gameView.placeTile(
//                tileView: self,
//                msTouch: msTime,
//                x: m_x + (getTileWidth() >> 1),
//                y: m_y + getTileHeight())
//        }
//	}

	//**********************************************************************************************
// TODO: James Folk
//	func
//	handleMotionEvent(
//		event: MotionEvent,
//		xPos: Int,
//		yPos: Int,
//		bPrimary: Bool
//	) -> Bool
//	{
//		m_x = xPos - (m_width >> 1)
//		m_y = yPos - (m_height >> 2)
//        var xPos = xPos
//
//		// If this is the primary motion tile, see if it is part of a range.
//		if (bPrimary)
//		{
//			// If this tile is being moved, it should be owned by the game object.
//			if (superview!.isKind(of: GameView.self))
//			{
//				let gameView = superview! as! GameView
//
//				// If we have a range of tiles, make sure they all move together.
//				let lTiles = gameView.getTilesRange()
//                if (lTiles.count > 0 && lTiles[0] === self)
//                {
//                    for tile in lTiles
//                    {
//                        xPos += m_width
//                        tile.handleMotionEvent(event, xPos, yPos, false)
//                    }
//				}
//			}
//		}
//		return true
//	}

	//**********************************************************************************************
// TODO: James Folk
//	func
//	handleTouchEvent(event: MotionEvent) -> Bool
//	{
//		// Can't move tiles which are already played.
//        if (m_tile.isStateAtLeast(eState: .PLAYED))
//		{
//			return false
//		}
//
//		// Initial touches should occur from a board or rack square.
//		if (!superview!.isKind(of: SquareView.self))
//		{
//			return false
//		}
//
//		// Get the parent of the square (rack or board).
//        let parent = superview!.superview!
//
//		/* Need to add view to window so it can move over all ViewGroups */
//		m_x = event.getRawX() - (m_width >> 1)
//		m_y = event.getRawY() - (m_height >> 2)
//
//		// If the square's parent is our game board, try to place the letter there.
//		if (parent.isKind(of: BoardView.self))
//		{
//			// Only placed tiles can be moved.
//			if (m_tile.getState() != Tile.State.PLACED)
//			{
//				return false
//			}
//
//            let boardView = parent as! BoardView
//			let gameView = boardView.superview! as! GameView
//
//			// TODO: Allow multitouch tiles from the game board.
//			if (gameView.getTileSelect() != nil)
//			{
//				return false
//			}
//
//			// Remove the tile from the board.
//            boardView.removeTileView(tileView: self)
//
//			// Add this tile as a Floating tile over the game screen.
//            gameView.setTileSelect(tileView: self, iRackTile: -1)
//		}
//
//		// Must have a rack.
//		else if (parent.isKind(of: RackView.self))
//		{
//            let rackView = parent as! RackView
//			let gameView = parent.superview as! GameView
//
//			// If the qwerty screen is up, don't interfere with the touch of the background
//			// rack tiles.
//			if (gameView.isQwertyActive())
//			{
//				return false
//			}
//
//			// If we have a multitouch, force the touches to be around the same time.
//			var bMultitouch = false
//			if (gameView.getTileSelect() != nil)
//			{
//                let msTime = CFAbsoluteTimeGetCurrent() - TileView.s_msTouch
//				if (msTime >= 200)
//				{
//					return false
//				}
//				bMultitouch = true
//			}
//
//			if (bMultitouch)
//			{
//				// Add this tile as a Floating tile over the game screen.
//                gameView.setTileSelectRange(tileView: self)
//			}
//			else
//			{
//				// Record the time of the touch.
//				TileView.s_msTouch = CFAbsoluteTimeGetCurrent()
//
//				// Remove the tile from the rack.
//                let iRackTile = rackView.removeTileView(tileView: self)
//
//				// Add this tile as a Floating tile over the game screen.
//                gameView.setTileSelect(tileView: self, iRackTile: iRackTile)
//			}
//		}
//
//		return true
//	}

	//**********************************************************************************************
	func
	onLayout(
		bChanged: Bool,
		xStart: Int,
		yStart: Int,
		xEnd: Int,
		yEnd: Int)
	{
        m_x = xStart
        m_y = yStart
        frame = CGRect(x: xStart, y: yStart, width: xEnd - xStart, height: yEnd - yStart);
	}

	//**********************************************************************************************
// TODO: James Folk
//    func
//	onTouchEvent(event: MotionEvent) -> Bool
//	{
//		do
//		{
//			switch (event.getAction())
//			{
//				case MotionEvent.ACTION_UP:
//					return handleDropEvent(event)
//
//				case MotionEvent.ACTION_DOWN:
//					return handleTouchEvent(event)
//
//				case MotionEvent.ACTION_MOVE:
//					return handleMotionEvent(event, event.getRawX(), event.getRawY(), true)
//			}
//		}
//		catch
//		{
//			return false
//		}
//		return true
//	}

	//**********************************************************************************************
	func
	setLetter(cLetter: UInt8)
	{
		m_cLetter = cLetter
        m_tile!.setLetter(cLetter: cLetter)

        let iPlayer = m_tile!.getPlayer()
		
		// If this tile is scoreless, it must be wild.
        if (m_tile!.getScore() == 0)
		{
			backgroundColor =
				UIColor(patternImage: s_idTilesWild[iPlayer][Int(cLetter - Tile.WILD)])
		}
		else
		{
			backgroundColor =
				UIColor(patternImage: s_idTiles[iPlayer][Int(cLetter - Tile.WILD)])
		}
	}

	//**********************************************************************************************
	func
	getState() -> Tile.State
	{
        return m_tile!.getState()
	}

	//**********************************************************************************************
	func
	toString() -> String
	{
        return m_tile!.toString()
	}

	//**********************************************************************************************
	func
	updateView() -> Bool
	{
        if (m_cLetter != m_tile!.getLetter())
		{
            setLetter(cLetter: m_tile!.getLetter())
			return true
		}
		return false
	}
}

//**************************************************************************************************
