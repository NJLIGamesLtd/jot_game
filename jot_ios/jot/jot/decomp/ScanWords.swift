//**************************************************************************************************
//
//  ScanWords.swift
//  
//
//  Created by Stephen Kennedy on 5/7/20.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

protocol ScanWords
{
	/***********************************************************************************************
	 * Prune method needed to parse words and to return which level we can skip over.
	 *
	 * @param aWord
	 *		The character array of letters within a tree word.
	 * @param iWord
	 *		The enumeration number of this word.
	 * @return
	 *		The level 
	 */
	func
	prune(
		aWord: [UInt8],
		iWord: Int
	) throws -> Int
}

//**************************************************************************************************

struct ScanWordsConstants
{
	static let SCAN_LEVEL_QUIT = -1		// Exit the scan.
	static let SCAN_LEVEL_RESET = -2		// Reset to the start of the list.
	static let SCAN_LEVEL_RANDOM = -3		// Randomize our position.
	static let SCAN_LEVEL_CONTINUE = -4	// No pruning, just continue.

	static let SCAN_FLAG_NORMAL = 0x00	// Normal search: no flags.
	static let SCAN_FLAG_RANDOM = 0x01	// Start scan from a random location.
	static let SCAN_FLAG_CONTINUE = 0x02	// From and including current word.
	static let SCAN_FLAG_NEXT = 0x04		// Search from next word.
	static let SCAN_FLAG_QUICK = 0x08		// Turn off word numbering.
}

//**************************************************************************************************
