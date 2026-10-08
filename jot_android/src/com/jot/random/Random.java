/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Random number generator.
 */

package com.jot.random;

public class Random
{
	private static int s_nSeed = 0;

	/***********************************************************************************************
	 * Return a number between zero and this range minus one.
	 *
	 * @param nRange
	 *		Return a number between zero and this range minus one.
	 * @return
	 *		Return a number between zero and this range minus one.
	 */
	public static int GetInt(int nRange)
	{
		// return (int)(nRange * Math.random());
		if (++s_nSeed >= nRange)
		{
			s_nSeed = 0;
		}
		return s_nSeed;
	}
}
