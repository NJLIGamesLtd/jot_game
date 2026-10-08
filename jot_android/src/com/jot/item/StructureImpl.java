/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * An implentation of Structure, which is a Place with multiple StructureLevel.
 */

package com.jot.item;

import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.*;

public class StructureImpl extends StructureLevelImpl implements Structure
{
	/***********************************************************************************************
	 * Structure constructor based on its mass, size and position.
	 *
	 * @param mass
	 *	    The mass of the structure.
	 * @param size
	 *	    The vector size of the structure.
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 */
	public StructureImpl(int mass, Size size, Position position) throws Exception
	{
		super(mass, size, position);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public StructureLevel getStructureLevel(int iLevel)
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.trivial("Structure.update();");	
	}
}
