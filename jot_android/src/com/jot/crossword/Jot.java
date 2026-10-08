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

public class Jot extends Game
{
	// Location of bonus scores on board.
	private static final byte s_aaBoardBonuses[][] =
	{
		{  0,  0,  0,  0, W3,  0,  0,  0,  0,  0,  0, W3,  0,  0,  0,  0 },
		{  0, L3,  0, L3,  0, W2,  0,  0,  0,  0, W2,  0, L3,  0, L3,  0 },
		{  0,  0, W3,  0,  0,  0, L2,  0,  0, L2,  0,  0,  0, W3,  0,  0 },
		{  0, L3,  0, L3,  0,  0,  0, W2, W2,  0,  0,  0, L3,  0, L3,  0 },
		{ W3,  0,  0,  0, L3,  0, L2,  0,  0, L2,  0, L3,  0,  0,  0, W3 },
		{  0, W2,  0,  0,  0, W2,  0,  0,  0,  0, W2,  0,  0,  0, W2,  0 },
		{  0,  0, L2,  0, L2,  0, L2,  0,  0, L2,  0, L2,  0, L2,  0,  0 },
		{  0,  0,  0, W2,  0,  0,  0, L2, L2,  0,  0,  0, W2,  0,  0,  0 },
		{  0,  0,  0, W2,  0,  0,  0, L2, L2,  0,  0,  0, W2,  0,  0,  0 },
		{  0,  0, L2,  0, L2,  0, L2,  0,  0, L2,  0, L2,  0, L2,  0,  0 },
		{  0, W2,  0,  0,  0, W2,  0,  0,  0,  0, W2,  0,  0,  0, W2,  0 },
		{ W3,  0,  0,  0, L3,  0, L2,  0,  0, L2,  0, L3,  0,  0,  0, W3 },
		{  0, L3,  0, L3,  0,  0,  0, W2, W2,  0,  0,  0, L3,  0, L3,  0 },
		{  0,  0, W3,  0,  0,  0, L2,  0,  0, L2,  0,  0,  0, W3,  0,  0 },
		{  0, L3,  0, L3,  0, W2,  0,  0,  0,  0, W2,  0, L3,  0, L3,  0 },
		{  0,  0,  0,  0, W3,  0,  0,  0,  0,  0,  0, W3,  0,  0,  0,  0 },
	};

	// Location of bonus scores on board.
	private static final byte s_aaBoardBonuses_test[][] =
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
	private static final char s_aaBoardDefault[][] =
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
	private static final char s_aaBoardSample_test[][] =
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
	private static final char s_aTileBag[] =
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
	private static final char s_aVowelBag[] =
	{
		'A', 'A', 'A', 'A', 'A',
		'E', 'E', 'E', 'E', 'E', 'E', 'E', 'E',
		'I', 'I', 'I', 'I', 'I',
		'O', 'O', 'O', 'O', 'O',
		'U', 'U', 'U',
		Tile.WILD
	};

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	private static final byte s_aTileScores[] =
	{
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 1, 3, 3, 2, 1, 4, 2, 4, 1, 8, 6, 1, 3, 1, 1,
		3,10, 1, 1, 1, 1, 5, 4, 7, 4, 9, 0, 0, 0, 0, 0,
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

	static final Bitmap[] s_bitmaps =
	{
		Bitmap.createBitMap(SquareBitmap.EMPTY, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(SquareBitmap.LETx2, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(SquareBitmap.LETx3, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(SquareBitmap.LETx4, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(SquareBitmap.WORDx2, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(SquareBitmap.WORDx3, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(SquareBitmap.WORDx4, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		null, null, null, null, null, null, null, null, null, 
		/* 10 */	null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 
		/* 20 */	null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 
		/* 30 */	null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 
		Bitmap.createBitMap(TileBitmap.WILD, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.A, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.B, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.C, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.D, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.E, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.F, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.G, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.H, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.I, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.J, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.K, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.L, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.M, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.N, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.O, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.P, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.Q, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.R, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.S, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.T, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.U, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.V, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.W, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.X, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.Y, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
		Bitmap.createBitMap(TileBitmap.Z, SquareBitmap.WIDTH, SquareBitmap.HEIGHT, Bitmap.Config.ARGB_8888),
	};

	//**********************************************************************************************
	public
	Jot(Words words) throws Exception
	{
		super(
			s_bitmaps,
			s_aaBoardBonuses_test,
			s_aaBoardSample_test,
			s_aTileBag,
			s_aVowelBag,
			s_aTileScores,
			s_aRackSample,
			100,
			words,
			1,
			false);
/*
		super(
			s_bitmaps,
			s_aaBoardBonuses,
			s_aaBoardSample,
			s_aTileBag,
			s_aVowelBag,
			s_aTileScores,
			s_aRackSample,
			words,
			1,
			false);
*/
	}

	//**********************************************************************************************
	public
	Jot(
		Words words,
		int nPlayers
	) throws Exception
	{
		super(
			s_bitmaps,
			s_aaBoardBonuses,
			s_aaBoardDefault,
			s_aTileBag,
			s_aVowelBag,
			s_aTileScores,
			s_aRackDefault,
			100,
			words,
			nPlayers,
			false);
	}
}

//**************************************************************************************************
