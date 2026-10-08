/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * A PlayStatus defines the validity of a word placement.
 */

package com.jot.jot.crossword;

//**************************************************************************************************

public enum PlayStatus
{
	VALID,
	NO_PLACEMENT,
	NOT_ALIGNED,
	NOT_START,
	ISOLATED,
	SINGLE_LETTER,
	INVALID_WORD;

	static final String s_aLabels[] =
	{
		"Valid placement",
		"Place tiles first",
		"Tiles must be aligned",
		"Must cross a center square",
		"Must cross a played word",
		"Words must be two or more letters",
		"Not all words valid"
	};

	public boolean
	isValid()
	{
		return this == VALID;
	}

	public String
	toString()
	{
		return s_aLabels[ordinal()];
	}
}

//**************************************************************************************************
