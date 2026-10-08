/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * A bonus value for a Board Square.
 */

package com.jot.jot.crossword;

import com.jot.jot.R;

//**************************************************************************************************

public enum Bonus
{
	NA,
	L2,
	L21,
	L22,
	L23,
	L24,
	L3,
	L4,
	W2,
	W3,
	W4;

	static final int[] s_aColors =
	{
		R.color.white,
		R.color.yellow,
		R.color.yellow,
		R.color.yellow,
		R.color.yellow,
		R.color.yellow,
		R.color.aqua,
		R.color.orange,
		R.color.green,
		R.color.blue,
		R.color.red
	};

	static final String[] s_aLabels =
	{
		"   ",
		"L2 ",
		"L2 ",
		"L2 ",
		"L2 ",
		"L2 ",
		"L3 ",
		"L4 ",
		"W2 ",
		"W3 ",
		"W4 "
	};

	public static Bonus
	GetEnum(int iBonus)
	{
		if (iBonus > W4.ordinal())
		{
			return null;
		}
		return Bonus.values()[iBonus];
	}

	public int
	getColor()
	{
		return s_aColors[ordinal()];
	}

	public int
	getValue()
	{
		return ordinal();
	}

	public String
	toString()
	{
		return s_aLabels[ordinal()];
	}
}

//**************************************************************************************************
