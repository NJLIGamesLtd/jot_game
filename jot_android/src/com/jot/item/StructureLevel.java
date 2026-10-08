/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A StructureLevel is a Place within a single level of a Structure.
 */

package com.jot.item;

public interface StructureLevel extends Place
{
	/***********************************************************************************************
	 * Build connections to the lift after they have been instantiated.
	 */
	public void setLiftConnections() throws Exception;
}
