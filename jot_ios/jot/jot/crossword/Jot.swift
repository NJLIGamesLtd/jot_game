//**************************************************************************************************
//
//  Jot.swift
//  jot
//
//  Created by Stephen Kennedy on 5/2/20.
//  Copyright © 2020 Stephen Kennedy. All rights reserved.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

class Jot: Game
{
	static let L2: UInt8 = Bonus.Mode.L2.rawValue
	static let L21: UInt8 = Bonus.Mode.L21.rawValue
	static let L22: UInt8 = Bonus.Mode.L22.rawValue
	static let L23: UInt8 = Bonus.Mode.L23.rawValue
	static let L24: UInt8 = Bonus.Mode.L24.rawValue
	static let L3: UInt8 = Bonus.Mode.L3.rawValue
	static let W2: UInt8 = Bonus.Mode.W2.rawValue
	static let W3: UInt8 = Bonus.Mode.W3.rawValue
	static let WILD: UInt8 = 0x40
	static let A: UInt8 = 0x41
	static let B: UInt8 = 0x42
	static let C: UInt8 = 0x43
	static let D: UInt8 = 0x44
	static let E: UInt8 = 0x45
	static let F: UInt8 = 0x46
	static let G: UInt8 = 0x47
	static let H: UInt8 = 0x48
	static let I: UInt8 = 0x49
	static let J: UInt8 = 0x4A
	static let K: UInt8 = 0x4B
	static let L: UInt8 = 0x4C
	static let M: UInt8 = 0x4D
	static let N: UInt8 = 0x4E
	static let O: UInt8 = 0x4F
	static let P: UInt8 = 0x50
	static let Q: UInt8 = 0x51
	static let R: UInt8 = 0x52
	static let S: UInt8 = 0x53
	static let T: UInt8 = 0x54
	static let U: UInt8 = 0x55
	static let V: UInt8 = 0x56
	static let W: UInt8 = 0x57
	static let X: UInt8 = 0x58
	static let Y: UInt8 = 0x59
	static let Z: UInt8 = 0x5A
	
	// Location of bonus scores on board.
	let s_aaBoardBonuses: [[UInt8]] =
	[
		[  0,  0,  0,  0, W3,  0,  0,  0,  0,  0,  0, W3,  0,  0,  0,  0 ],
		[  0, L3,  0, L3,  0, W2,  0,  0,  0,  0, W2,  0, L3,  0, L3,  0 ],
		[  0,  0, W3,  0,  0,  0, L2,  0,  0, L2,  0,  0,  0, W3,  0,  0 ],
		[  0, L3,  0, L3,  0,  0,  0, W2, W2,  0,  0,  0, L3,  0, L3,  0 ],
		[ W3,  0,  0,  0, L3,  0, L2,  0,  0, L2,  0, L3,  0,  0,  0, W3 ],
		[  0, W2,  0,  0,  0, W2,  0,  0,  0,  0, W2,  0,  0,  0, W2,  0 ],
		[  0,  0, L2,  0, L2,  0, L2,  0,  0, L2,  0, L2,  0, L2,  0,  0 ],
		[  0,  0,  0, W2,  0,  0,  0,L21,L22,  0,  0,  0, W2,  0,  0,  0 ],
		[  0,  0,  0, W2,  0,  0,  0,L23,L24,  0,  0,  0, W2,  0,  0,  0 ],
		[  0,  0, L2,  0, L2,  0, L2,  0,  0, L2,  0, L2,  0, L2,  0,  0 ],
		[  0, W2,  0,  0,  0, W2,  0,  0,  0,  0, W2,  0,  0,  0, W2,  0 ],
		[ W3,  0,  0,  0, L3,  0, L2,  0,  0, L2,  0, L3,  0,  0,  0, W3 ],
		[  0, L3,  0, L3,  0,  0,  0, W2, W2,  0,  0,  0, L3,  0, L3,  0 ],
		[  0,  0, W3,  0,  0,  0, L2,  0,  0, L2,  0,  0,  0, W3,  0,  0 ],
		[  0, L3,  0, L3,  0, W2,  0,  0,  0,  0, W2,  0, L3,  0, L3,  0 ],
		[  0,  0,  0,  0, W3,  0,  0,  0,  0,  0,  0, W3,  0,  0,  0,  0 ],
	]

	// Location of bonus scores on board.
	let s_aaBoardBonuses_test: [[UInt8]] =
	[
		[ L3,  0, L2,  0,  0, L2,  0, L3 ],
		[  0, W2,  0,  0,  0,  0, W2,  0 ],
		[ L2,  0, L2,  0,  0, L2,  0, L2 ],
		[  0,  0,  0, L2, L2,  0,  0,  0 ],
		[  0,  0,  0, L2, L2,  0,  0,  0 ],
		[ L2,  0, L2,  0,  0, L2,  0, L2 ],
		[  0, W2,  0,  0,  0,  0, W2,  0 ],
		[ L3,  0, L2,  0,  0, L2,  0, L3 ],
	]

	// Default board in play for testing.
	let s_aaBoardDefault: [[UInt8]] =
	[
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
	]

	// Sample board in play for testing.
	let s_aaBoardDefault_test: [[UInt8]] =
	[
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
	]

	// Sample board in play for testing.
	let s_aaBoardSample: [[UInt8]] =
	[
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  K,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  W,  H,  E,  N,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  R,  U,  N,  E,  0,  0,  O,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  C,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, WILD,0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 ],
	]

	// Sample board in play for testing.
	let s_aaBoardSample_test: [[UInt8]] =
	[
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
		[ 0,  0,  0,  0,  0,  0,  0,  0 ],
	]

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	let s_aTileBag: [UInt8] =
	[
		B, B,
		C,
		D, D,
		F,
		G, G,
		H,
		J,
		K,
		L, L, L,
		M,
		N, N, N,
		P, P,
		Q,
		R, R, R, R,
		S, S, S,
		T, T, T, T,
		V,
		W,
		X,
		Y,
		Z
	]

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	let s_aTileBag_test: [UInt8] =
	[
		B, D, L, R, S
	]

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	let s_aVowelBag: [UInt8] =
	[
		A, A, A, A, A,
		E, E, E, E, E, E, E, E,
		I, I, I, I, I,
		O, O, O, O, O,
		U, U, U,
		WILD
	]

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	let s_aVowelBag_test: [UInt8] =
	[
		A, E, I, O,
		WILD
	]

	// Point value for each letter tile.  Letters marked with the WILD mask have no score.
	let s_aTileScores: [UInt8] =
	[
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 1, 3, 4, 2, 1, 4, 2, 3, 1, 8, 6, 1, 3, 1, 1,
		3,10, 1, 1, 1, 2, 5, 4, 7, 4, 9, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	]

	// Default letters tiles.
	let s_aRackDefault: [UInt8] =
	[
		0, 0, 0, 0, 0, 0, 0, 0
	]

	// Sample letters for testing.
	let s_aRackSample: [UInt8] =
	[
		Q, W, E, R, T, I, U, P
	]

	//**********************************************************************************************
	init(
		words: Words,
		main: Main)
	{
		super.init(
			aaBonuses: s_aaBoardBonuses_test,
			aaDefaultTiles: s_aaBoardSample_test,
			aTileBag: s_aTileBag_test,
			aVowelBag: s_aVowelBag_test,
			aTileScores: s_aTileScores,
			aTileRack: s_aRackSample,
			nBingoPrize: 100,
			words: words,
			main: main,
			nPlayers: 2,
			iPlayerRobot: 1,
			bBagShared: false)
	}

	//**********************************************************************************************
	init(
		words: Words,
		main: Main,
		nPlayers: Int,
		iPlayerRobot: Int)
	{
		super.init(
			aaBonuses: s_aaBoardBonuses,
			aaDefaultTiles: s_aaBoardDefault,
			aTileBag: s_aTileBag,
			aVowelBag: s_aVowelBag,
			aTileScores: s_aTileScores,
			aTileRack: s_aRackDefault,
			nBingoPrize: 100,
			words: words,
			main: main,
			nPlayers: nPlayers,
			iPlayerRobot: iPlayerRobot,
			bBagShared: false)
	}
}

//**************************************************************************************************
