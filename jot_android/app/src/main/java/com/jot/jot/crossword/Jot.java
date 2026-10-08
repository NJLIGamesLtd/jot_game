/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Represents the board layout and tile scores for this com.jot.game.
 */

package com.jot.jot.crossword;

import com.jot.decomp.Words;
import com.jot.jot.android.MainActivity;

//**************************************************************************************************

public class Jot extends Game
{
	// Location of bonus scores on board.
	private static final byte[][] s_aaBoardBonuses =
	{
		{  0,  0,  0,  0, W3,  0,  0,  0,  0,  0,  0, W3,  0,  0,  0,  0 },
		{  0, L3,  0, L3,  0, W2,  0,  0,  0,  0, W2,  0, L3,  0, L3,  0 },
		{  0,  0, W3,  0,  0,  0, L2,  0,  0, L2,  0,  0,  0, W3,  0,  0 },
		{  0, L3,  0, L3,  0,  0,  0, W2, W2,  0,  0,  0, L3,  0, L3,  0 },
		{ W3,  0,  0,  0, L3,  0, L2,  0,  0, L2,  0, L3,  0,  0,  0, W3 },
		{  0, W2,  0,  0,  0, W2,  0,  0,  0,  0, W2,  0,  0,  0, W2,  0 },
		{  0,  0, L2,  0, L2,  0, L2,  0,  0, L2,  0, L2,  0, L2,  0,  0 },
		{  0,  0,  0, W2,  0,  0,  0,L21,L22,  0,  0,  0, W2,  0,  0,  0 },
		{  0,  0,  0, W2,  0,  0,  0,L23,L24,  0,  0,  0, W2,  0,  0,  0 },
		{  0,  0, L2,  0, L2,  0, L2,  0,  0, L2,  0, L2,  0, L2,  0,  0 },
		{  0, W2,  0,  0,  0, W2,  0,  0,  0,  0, W2,  0,  0,  0, W2,  0 },
		{ W3,  0,  0,  0, L3,  0, L2,  0,  0, L2,  0, L3,  0,  0,  0, W3 },
		{  0, L3,  0, L3,  0,  0,  0, W2, W2,  0,  0,  0, L3,  0, L3,  0 },
		{  0,  0, W3,  0,  0,  0, L2,  0,  0, L2,  0,  0,  0, W3,  0,  0 },
		{  0, L3,  0, L3,  0, W2,  0,  0,  0,  0, W2,  0, L3,  0, L3,  0 },
		{  0,  0,  0,  0, W3,  0,  0,  0,  0,  0,  0, W3,  0,  0,  0,  0 },
	};

	// Location of bonus scores on board.
	private static final byte[][] s_aaBoardBonuses_test =
	{
		{ L3,  0, L2,  0,  0, L2,  0, L3 },
		{  0, W2,  0,  0,  0,  0, W2,  0 },
		{ L2,  0, L2,  0,  0, L2,  0, L2 },
		{  0,  0,  0, L2, L2,  0,  0,  0 },
		{  0,  0,  0, L2, L2,  0,  0,  0 },
		{ L2,  0, L2,  0,  0, L2,  0, L2 },
		{  0, W2,  0,  0,  0,  0, W2,  0 },
		{ L3,  0, L2,  0,  0, L2,  0, L3 },
	};

	// Default board in play for testing.
	private static final char[][] s_aaBoardDefault =
	{
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
	};

	// Sample board in play for testing.
	private static final char s_aaBoardSample[][] =
	{
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 'K', 0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0, 'W','H','E','N', 0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0, 'R','U','N','E', 0,  0, 'O', 0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 'C', 0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, '@', 0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
	};

	// Sample board in play for testing.
	private static final char[][] s_aaBoardSample_test =
	{
		{ 0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0 },
	};

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	private static final char[] s_aTileBag =
	{
		'B', 'B',
		'C',
		'D', 'D',
		'F',
		'G', 'G',
		'H',
		'J',
		'K',
		'L', 'L', 'L',
		'M',
		'N', 'N', 'N',
		'P', 'P',
		'Q',
		'R', 'R', 'R', 'R',
		'S', 'S', 'S',
		'T', 'T', 'T', 'T',
		'V',
		'W',
		'X',
		'Y',
		'Z'
	};

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	private static final char[] s_aTileBag_test =
	{
		'B', 'D', 'L', 'R', 'S'
	};

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	private static final char[] s_aVowelBag =
	{
		'A', 'A', 'A', 'A', 'A',
		'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E',
		'I', 'I', 'I', 'I', 'I',
		'O', 'O', 'O', 'O', 'O',
		'U', 'U', 'U',
		Tile.WILD
	};

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	private static final char[] s_aVowelBag_test =
	{
		'A', 'E', 'I', 'O',
		Tile.WILD
	};

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	private static final byte[] s_aTileScores =
	{
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 1, 3, 4, 2, 1, 4, 2, 3, 1, 8, 6, 1, 3, 1, 1,
		3,10, 1, 1, 1, 2, 5, 4, 7, 4, 9, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	};

	// Default letters tiles.
	private static final char[] s_aRackDefault =
	{
		0, 0, 0, 0, 0, 0, 0, 0
	};

	// Sample letters for testing.
	private static final char[] s_aRackSample =
	{
		'Q', 'W', 'E', 'R', 'T', 'I', 'U', 'P'
	};

	//**********************************************************************************************
	public
	Jot(
		Words words,
		MainActivity mainActivity)
	{
		super(
			s_aaBoardBonuses_test,
			s_aaBoardSample_test,
			s_aTileBag_test,
			s_aVowelBag_test,
			s_aTileScores,
			s_aRackSample,
			100,
			words,
			mainActivity,
			2,
			1,
			false);
	}

	//**********************************************************************************************
	public
	Jot(
		Words words,
		MainActivity mainActivity,
		int nPlayers,
		int iPlayerRobot)
	{
		super(
			s_aaBoardBonuses,
			s_aaBoardDefault,
			s_aTileBag,
			s_aVowelBag,
			s_aTileScores,
			s_aRackDefault,
			100,
			words,
			mainActivity,
			nPlayers,
			iPlayerRobot,
			false);
	}

	//**********************************************************************************************
	private
	Jot(
		Words words,
		MainActivity mainActivity,
		int nPlayers)
	{
		this(words, mainActivity, nPlayers, 1);
	}
}

//**************************************************************************************************
