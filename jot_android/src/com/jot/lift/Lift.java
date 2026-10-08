/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Lift is a generic elevator that is directed as a Return Track.
 */

package com.jot.lift;

import com.jot.item.Drone;
import com.jot.item.StructureLevel;
import com.jot.track.Return;
import com.jot.vector.Position;
import com.jot.vector.Size;

public class Lift extends Return
{
	protected StructureLevel[] m_aLevels;

	/***********************************************************************************************
	 * Lift constructor based on its position and structure levels it connects with.
	 *
	 * @param mass
	 *		The kg mass of this item.
	 * @param size
	 *	    The vector size of this item.
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param aPosTrack
	 *	    An array of positions that defines the path of the track.
	 * @param aLevels
	 *	    The structure levels this lift connects with.
	 */
	public Lift(
		int mass,
		Size size,
		Position position,
		Position[] aPosTrack,
		StructureLevel[] aLevels) throws Exception
	{
		super(mass, size, position, aPosTrack);

		// Save a reference to the StructureLevel list that can be accessed from this Lift.
		m_aLevels = aLevels;
	}

	/***********************************************************************************************
	 * Push a new destination for our lift to pick up this person.
	 *
	 * @param person
	 *		A directed drone.
	 */
	public void pushDestination(Drone person)
	{
	}
}
