/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Place is an Item that can have Drone items directed towards and away.
 */

package com.jot.item;

import com.jot.simulation.Simulation;
import com.jot.vector.Location;
import com.jot.vector.Position;
import com.jot.vector.Size;

//An Place is an Item that can have Drone items directed towards and away.

public class PlaceImpl extends ItemImpl implements Place
{
	/***********************************************************************************************
	 * Place constructor based on its mass, size and position.
	 *
	 * @param mass
	 *		The kg mass of this item.
	 * @param size
	 *	    The vector size of this item.
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 */
	public PlaceImpl(int mass, Size size, Position position) throws Exception
	{
		super(mass, size, position);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void add(Drone drone)
	{
		// Tell the drone it is in the control of the lobby;
		drone.setPlace(this);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getFloorLevelID()
	{
		Location location = getLocation();
		return location.m_z / 4000;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getFloorGroupID()
	{
		Location location = getLocation();
		return location.m_z / (10 * 4000);
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(Drone drone) throws Exception
	{
		return false;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Drone poll()
	{
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
	}
}
