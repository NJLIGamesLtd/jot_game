/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Words is a word list decompression interface.
 */

package com.jot.decomp;

//**************************************************************************************************

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Taboo
{
	//**********************************************************************************************

	private static final String[] s_aTaboos =
	{
		"dick",
		"cock",
		"cunt",
		"fuck",
		"nigger",
		"pussy",
		"suck",
		"vagina",
	};
	private static final Set<String> s_sTaboos = new HashSet<>(Arrays.asList(s_aTaboos));

	public static boolean
	IsTaboo(String strWord)
	{
		return s_sTaboos.contains(strWord.toLowerCase());
	}

	public static boolean
	ContainsTaboo(String strMessage)
	{
		strMessage = strMessage.toLowerCase();

		for (String strFragment: s_sTaboos)
		{
			if (strMessage.contains(strFragment))
			{
				return true;
			}
		}
		return false;
	}
}
