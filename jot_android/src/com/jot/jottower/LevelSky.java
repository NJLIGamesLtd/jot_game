/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A LevelSky is a TowerLevel that resides at the highest floor reachable by a specific lift.
 */

package com.jot.jottower;

import com.jot.item.*;
import com.jot.lift.*;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.*;

/***************************************************************************************************
 * An LevelSky is a StructureLevel at the top of a structure for people to congregate.
 */
public class LevelSky extends StructureLevelImpl
{
	private static final int s_mass = 0;
	private static final Size s_size = new Size(106000, 122400, 4000);

	private Lift[] m_aLifts;
	private CollisionGrid m_collisionGrid;

	/***********************************************************************************************
	 * LevelSky constructor based on its position and lifts.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param towerLifts
	 *	    An array of lifts that can be used to access this level.
	 */
	public LevelSky(Position position, Lift[] towerLifts) throws Exception
	{
		super(s_mass, s_size, position);

		Trace.startOf("LevelSky()");

		m_aLifts = towerLifts;
		m_collisionGrid = new CollisionGrid(s_size.m_x, s_size.m_y);

		Trace.endOf("LevelSky()");
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.trivial("LevelSky.update();");	
	}
}

/**************************************************************************************************/
