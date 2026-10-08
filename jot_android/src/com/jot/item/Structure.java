/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Structure is a Place that can be defined by multiple StructureLevel.
 */

package com.jot.item;

public interface Structure extends StructureLevel
{
	/***********************************************************************************************
	 * Return the Nth level, if available.
	 *
	 * @param iLevel
	 *		The Nth level, starting with 0 for the lobby.
	 * @return
	 *		The Nth level, if available.
	 */
	public StructureLevel getStructureLevel(int iLevel);
}
