/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Represents the board layout and tile scores for this game.
 */

package com.jot.crossword;

import com.jot.decomp.Words;

//**************************************************************************************************

public class Scrabble extends Game
{
	// Location of bonus scores on board.
	private static final byte s_aaBoardBonuses[][] =
	{
		{ W3,  0,  0, L2,  0,  0,  0, W3,  0,  0,  0, L2,  0,  0, W3 },
		{  0, W2,  0,  0,  0, L3,  0,  0,  0, L3,  0,  0,  0, W2,  0 },
		{  0,  0, W2,  0,  0,  0, L2,  0, L2,  0,  0,  0, W2,  0,  0 },
		{ L2,  0,  0, W2,  0,  0,  0, L2,  0,  0,  0, W2,  0,  0, L2 },
		{  0,  0,  0,  0, W2,  0,  0,  0,  0,  0, W2,  0,  0,  0,  0 },
		{  0, L3,  0,  0,  0, L3,  0,  0,  0, L3,  0,  0,  0, L3,  0 },
		{  0,  0, L2,  0,  0,  0, L2,  0, L2,  0,  0,  0, L2,  0,  0 },
		{ W3,  0,  0, L2,  0,  0,  0, W2,  0,  0,  0, L2,  0,  0, W3 },
		{  0,  0, L2,  0,  0,  0, L2,  0, L2,  0,  0,  0, L2,  0,  0 },
		{  0, L3,  0,  0,  0, L3,  0,  0,  0, L3,  0,  0,  0, L3,  0 },
		{  0,  0,  0,  0, W2,  0,  0,  0,  0,  0, W2,  0,  0,  0,  0 },
		{ L2,  0,  0, W2,  0,  0,  0, L2,  0,  0,  0, W2,  0,  0, L2 },
		{  0,  0, W2,  0,  0,  0, L2,  0, L2,  0,  0,  0, W2,  0,  0 },
		{  0, W2,  0,  0,  0, L3,  0,  0,  0, L3,  0,  0,  0, W2,  0 },
		{ W3,  0,  0, L2,  0,  0,  0, W3,  0,  0,  0, L2,  0,  0, W3 },
	};

	// Sample board in play for testing.
	private static final char s_aaBoardDefault[][] =
	{
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 'K', 0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0, 'W','H','E','N', 0,  0,  0,  0 },
		{ 0,  0,  0,  0, 'R','U','N','E', 0,  0, 'O', 0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 'C', 0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0, '@', 0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
		{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },
	};

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	private static final char s_aTileBag[] =
	{
		'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
		'B', 'B',
		'C', 'C',
		'D', 'D', 'D', 'D',
		'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E',
		'F', 'F',
		'G', 'G', 'G',
		'H', 'H',
		'I', 'I', 'I', 'I', 'I', 'I', 'I', 'I', 'I',
		'J',
		'K',
		'L', 'L', 'L', 'L',
		'M', 'M',
		'N', 'N', 'N', 'N', 'N', 'N',
		'O', 'O', 'O', 'O', 'O', 'O', 'O', 'O',
		'P', 'P',
		'Q',
		'R', 'R', 'R', 'R', 'R', 'R',
		'S', 'S', 'S', 'S',
		'T', 'T', 'T', 'T', 'T', 'T',
		'U', 'U', 'U', 'U',
		'V', 'V',
		'W', 'W',
		'X',
		'Y', 'Y',
		'Z',
		Tile.WILD, Tile.WILD
	};

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	private static final byte s_aTileScores[] =
	{
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 1, 3, 3, 2, 1, 4, 2, 4, 1, 8, 5, 1, 3, 1, 1,
		3,10, 1, 1, 1, 1, 4, 4, 8, 4,10, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	};

	// Sample letters for testing.
	private static final char[] s_aRackDefault =
	{
		'Q', 'W', 'E', 'R', 'T', 'I', 'U'
	};

	//**********************************************************************************************
	public
	Scrabble(Words words) throws Exception
	{
		super(
			null,
			s_aaBoardBonuses,
			s_aaBoardDefault,
			s_aTileBag,
			null,
			s_aTileScores,
			s_aRackDefault,
			50,
			words,
			2,
			true);
	}
}

//**************************************************************************************************
