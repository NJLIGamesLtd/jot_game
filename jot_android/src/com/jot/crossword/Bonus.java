/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * A bonus value for a Board Square.
 */

package com.jot.crossword;

//**************************************************************************************************

public enum Bonus
{
	NA,
	L2,
	L3,
	L4,
	W2,
	W3,
	W4;

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
	getValue()
	{
		return ordinal();
	}

	public String
	toString()
	{
		switch (this)
		{
			case L2:	return "l-2";
			case L3:	return "l-3";
			case L4:	return "l-4";
			case W2:	return "w-2";
			case W3:	return "w-3";
			case W4:	return "w-4";
			case NA:
			default:	return "   ";
		}
	}
}

//**************************************************************************************************
