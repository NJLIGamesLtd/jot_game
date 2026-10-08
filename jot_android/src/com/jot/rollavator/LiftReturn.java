/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator LiftReturn carries a single Box up and down, and can marry its Box to a BoxLoop or
 * Port for the purpose of transfering a Car between them.  This marriage joins the Track within
 * each Box until the Car is transfered.
 */

package com.jot.rollavator;

import com.jot.item.Drone;
import com.jot.item.StructureLevel;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.Return;
import com.jot.vector.Location;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

public class LiftReturn extends Return
{
	protected static final int s_mass = 1000;
	protected static final Size s_size = new Size(1800, 2000, 4000);

	private static final Location[] s_aLocPerFloor =
	{
		new Location(0, 0,    0),
		new Location(0, 0,   50),
		new Location(0, 0,  100),
		new Location(0, 0,  150),
		new Location(0, 0,  200),
		new Location(0, 0,  250),
		new Location(0, 0,  300),
		new Location(0, 0,  350),
		new Location(0, 0,  400),
		new Location(0, 0,  450),
		new Location(0, 0,  500),
		new Location(0, 0,  550),
		new Location(0, 0,  600),
		new Location(0, 0,  650),
		new Location(0, 0,  700),
		new Location(0, 0,  750),
		new Location(0, 0,  800),
		new Location(0, 0,  850),
		new Location(0, 0,  900),
		new Location(0, 0,  950),
		new Location(0, 0, 1000),
		new Location(0, 0, 1050),
		new Location(0, 0, 1100),
		new Location(0, 0, 1150),
		new Location(0, 0, 1200),
		new Location(0, 0, 1250),
		new Location(0, 0, 1300),
		new Location(0, 0, 1350),
		new Location(0, 0, 1400),
		new Location(0, 0, 1450),
		new Location(0, 0, 1500),
		new Location(0, 0, 1550),
		new Location(0, 0, 1600),
		new Location(0, 0, 1650),
		new Location(0, 0, 1700),
		new Location(0, 0, 1750),
		new Location(0, 0, 1800),
		new Location(0, 0, 1850),
		new Location(0, 0, 1900),
		new Location(0, 0, 1950),
		new Location(0, 0, 2000),
		new Location(0, 0, 2050),
		new Location(0, 0, 2100),
		new Location(0, 0, 2150),
		new Location(0, 0, 2200),
		new Location(0, 0, 2250),
		new Location(0, 0, 2300),
		new Location(0, 0, 2350),
		new Location(0, 0, 2400),
		new Location(0, 0, 2450),
		new Location(0, 0, 2500),
		new Location(0, 0, 2550),
		new Location(0, 0, 2600),
		new Location(0, 0, 2650),
		new Location(0, 0, 2700),
		new Location(0, 0, 2750),
		new Location(0, 0, 2800),
		new Location(0, 0, 2850),
		new Location(0, 0, 2900),
		new Location(0, 0, 2950),
		new Location(0, 0, 3000),
		new Location(0, 0, 3050),
		new Location(0, 0, 3100),
		new Location(0, 0, 3150),
		new Location(0, 0, 3200),
		new Location(0, 0, 3250),
		new Location(0, 0, 3300),
		new Location(0, 0, 3350),
		new Location(0, 0, 3400),
		new Location(0, 0, 3450),
		new Location(0, 0, 3500),
		new Location(0, 0, 3550),
		new Location(0, 0, 3600),
		new Location(0, 0, 3650),
		new Location(0, 0, 3700),
		new Location(0, 0, 3750),
		new Location(0, 0, 3800),
		new Location(0, 0, 3850),
		new Location(0, 0, 3900),
		new Location(0, 0, 3950),
	};

	private StructureLevel[] m_aLevels;

	/***********************************************************************************************
	 * LiftReturn constructor based on its position and structure levels it connects with.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param aLevels
	 *	    The structure levels this lift connects with.
	 * @param rollavator
	 *	    The Rollavator this is owned by.
	 * @param iLane
	 *		The port lane number.
	 */
	public LiftReturn(
		Position position,
		StructureLevel[] aLevels,
		Rollavator rollavator,
		int iLane
	) throws Exception
	{
		super(s_mass, s_size, position, null);
		
		Trace.startOf("rollavator.LiftReturn()");

		// Fix the size to account for each level in our structure.
		int nLevels = aLevels.length;
		m_size = new Size(s_size.m_x, s_size.m_y, nLevels * s_size.m_z);

		// Save a reference to our tower levels.
		m_aLevels = aLevels;

		// Build a list of positions for our track.
		int nPositions = nLevels * s_aLocPerFloor.length;
		Position aPosTrack[] = new Position[nPositions];
		addLevelPositions(aPosTrack, 0, nLevels, s_aLocPerFloor);
		
		// Lay the return track.
		setTrack(aPosTrack, position);

		// SPK: Mark each quadrant based on floor groups.
		//markQuadrant(?);

		// Create a lift box and add it to our track.
		BoxReturn box = new BoxReturn(getSectionFirst(), rollavator, this, iLane);
		add(0, box);

		Trace.endOf("rollavator.LiftReturn()");
	}

	/***********************************************************************************************
	 * Internal method for adding track positions to our lift.
	 *
	 * @param posList
	 *	    Return track positions list that should eventually form a complete loop.
	 * @param iLevelFirst
	 *	    Initial structure level for the new set of track positions.
	 * @param iLevelLast
	 *	    Ending structure level for the new set of track positions.
	 * @param locList
	 *	    Add this partial set of track positions.
	 */
	private void addLevelPositions(
		Position[] posList,
		int iLevelFirst,
		int iLevelLast,
		final Location[] locList)
	{
		int iPos = 0;
		Orientation orientation = new Orientation();
		for (int iLevel = iLevelFirst; iLevel < iLevelLast; ++iLevel)
		{
			for (int iLoc = 0; iLoc < locList.length; ++iLoc)
			{
				posList[iPos++] = new Position(
					locList[iLoc].m_x,
					locList[iLoc].m_y,
					(4000 * iLevel) + locList[iLoc].m_z,
					orientation);				
			}
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void pushDestination(Drone person)
	{
		BoxReturn box = (BoxReturn)m_car;
		try
		{
			box.pushDestination(person);
		}
		catch (Exception e)
		{
		}
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("rollavator.LiftReturn.update()");

		// Run an update internal to the box.
		m_car.update(sim);

		Trace.endOf("rollavator.LiftReturn.update()");
	}
}
