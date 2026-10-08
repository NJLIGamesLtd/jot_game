/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * ScanWords is an interface for providing a pruning method over a word list scan in
 * conjunction with the word list's scan() method.
 */

package com.jot.decomp;

//**************************************************************************************************

public interface ScanWords
{
	public static final int SCAN_LEVEL_QUIT = -1;		// Exit the scan.
	public static final int SCAN_LEVEL_RESET = -2;		// Reset to the start of the list.
	public static final int SCAN_LEVEL_RANDOM = -3;		// Randomize our position.
	public static final int SCAN_LEVEL_CONTINUE = -4;	// No pruning, just continue.

	public static final int SCAN_FLAG_NORMAL = 0x00;	// Normal search: no flags.
	public static final int SCAN_FLAG_RANDOM = 0x01;	// Start scan from a random location.
	public static final int SCAN_FLAG_CONTINUE = 0x02;	// From and including current word.
	public static final int SCAN_FLAG_NEXT = 0x04;		// Search from next word.
	public static final int SCAN_FLAG_QUICK = 0x08;		// Turn off word numbering.

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
	public int
	prune(
		char[] aWord,
		int iWord
	) throws Exception;
}

//**************************************************************************************************
